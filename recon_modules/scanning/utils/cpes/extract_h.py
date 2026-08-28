import asyncio
import aiohttp
from scanning.utils.cpes.fingerpint import _FINGERPRINT_HEADERS, _PATTERNS


# Recieves an endpoint list and returns a set of CPEs
async def extract_cpes_from_headers(endpoints: list[str]) -> set[str]:
    
    seen_values: set[str] = set()   # deduplication: same header value, same CPE
    final_cpes: set[str] = set()

    connector = aiohttp.TCPConnector(ssl = False)
    timeout = aiohttp.ClientTimeout(total = 8)

    async with aiohttp.ClientSession(connector = connector, timeout = timeout) as session:
        # one task for each endpoint
        tasks = [
            _fingerprint_endpoint(session, url, seen_values)
            for url in endpoints
        ]
        # a broken endpoint doesn't affect the scan
        results = await asyncio.gather(*tasks, return_exceptions = True)

    for result in results:
        if isinstance(result, set):
            final_cpes |= result

    return final_cpes


async def _fingerprint_endpoint(session: aiohttp.ClientSession, url: str, seen_values: set[str]) -> set[str]:
    
    try:
        async with session.head(url, allow_redirects=True) as resp:
            return _headers_to_cpes(dict(resp.headers), seen_values)
    except (asyncio.TimeoutError, aiohttp.ClientError):
        return set()


def _headers_to_cpes(headers: dict, seen_values: set[str]) -> set[str]:
    
    cpes: set[str] = set()
    for header_name in _FINGERPRINT_HEADERS:
        value = headers.get(header_name, "").strip()
        if not value or value in seen_values:
            continue
        seen_values.add(value)
        cpes |= _value_to_cpes(value)
    return cpes


def _value_to_cpes(value: str) -> set[str]:
    
    cpes: set[str] = set()
    for pattern, template in _PATTERNS:
        match = pattern.search(value)
        if match:
            version = match.group(1) if match.lastindex and match.lastindex >= 1 else "unknown"
            cpes.add(template.replace("{v}", version))
    return cpes
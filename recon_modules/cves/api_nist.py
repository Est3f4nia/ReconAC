import asyncio
import aiohttp
from typing import List, Optional, Dict, Any
from datetime import datetime, timedelta

from cves.utils.parsing.p_normalize_cpes import normalize
from urllib.parse import quote
from yarl import URL
from models.api_result import ApiResult
from cves.utils.parsing.p_api_nist import parse_single_vuln

NVD_API_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0/"

_RESULTS_PER_PAGE = 30
_MAX_PAGES = 10

_RETRY_DELAY = 6
_MAX_RETRIES = 5
_SEMAPHORE = asyncio.Semaphore(8)

async def lookup_cves_for_cpes(cpes: List[str], api_key: str, max_cve_years: int, min_cvss_score: float) -> Dict[str, ApiResult]:
    
    headers = {"apiKey": api_key}

    async with aiohttp.ClientSession(headers=headers) as session:
        tasks = [_pipeline_cpe(session, cpe, max_cve_years, min_cvss_score) for cpe in cpes]
        results = await asyncio.gather(*tasks, return_exceptions=True)

    return {
        cpe: result
        for cpe, result in zip(cpes, results)
        if isinstance(result, ApiResult)
    }

async def _pipeline_cpe(session: aiohttp.ClientSession, cpe: str, max_cve_years: int, min_cvss_score: float) -> ApiResult:   
    
    normalized = normalize(cpe)

    if _is_too_generic(normalized):
        print(f"[!] Skipping generic CPE: {normalized}")
        return ApiResult(cpe_string=cpe, vulnerabilities=[])

    raw_vulns: List[Dict[str, Any]] = await _fetch_all_pages(session, normalized)

    if not raw_vulns:
        print(f"[-] No vulnerabilities found for {cpe}")
        return ApiResult(cpe_string=cpe, vulnerabilities=[])

    vulnerabilities = parse_single_vuln(raw_vulns)
    filtered_vulns = _apply_filters(vulnerabilities, max_cve_years, min_cvss_score)

    return ApiResult(
        cpe_string=cpe,
        vulnerabilities=filtered_vulns,
        last_checked=datetime.now()
    )

def _apply_filters(vulnerabilities: List[Any], max_cve_years: int, min_cvss_score: float) -> List[Any]:

    if not vulnerabilities:
        return []

    cutoff_date = None
    if max_cve_years > 0:
        cutoff_date = datetime.now() - timedelta(days=max_cve_years * 365)

    filtered = []
    for vuln in vulnerabilities:
        if vuln.cvss_score is None or vuln.cvss_score < min_cvss_score:
            continue

        if vuln.published_date and vuln.published_date < cutoff_date:
            continue

        filtered.append(vuln)

    return filtered

# --- FETCHS ---

async def _fetch_all_pages(session: aiohttp.ClientSession, cpe_normalized: str) -> List[Dict[str, Any]]:
    
    all_vulns: List[Dict[str, Any]] = []
    start_index = 0
    page = 0

    while page < _MAX_PAGES:

        query = (
            f"?virtualMatchString={quote(cpe_normalized, safe='')}"
            f"&resultsPerPage={_RESULTS_PER_PAGE}"
            f"&startIndex={start_index}"
        )

        url = URL(NVD_API_URL + query, encoded=True)
        data = await _fetch_page(session, url)

        if not data:
            print(f"[!] No data returned from NIST for page {page}")
            break

        vulns = data.get("vulnerabilities", [])
        total = data.get("totalResults", 0)

        if not vulns:
            print("[-] No vulnerabilities found")
            break

        all_vulns.extend(vulns)

        if (
            len(vulns) < _RESULTS_PER_PAGE
            or start_index + len(vulns) >= total
        ):
            break

        start_index += _RESULTS_PER_PAGE
        page += 1
        await asyncio.sleep(1.2)

    return all_vulns

async def _fetch_page(session: aiohttp.ClientSession, url: URL) -> Optional[dict]:
    
    for attempt in range(_MAX_RETRIES):
        try:
            async with _SEMAPHORE:
                async with session.get(
                    url,
                    timeout=aiohttp.ClientTimeout(total=45),
                ) as resp:
                    
                    if resp.status == 429:
                        delay = _RETRY_DELAY * (2 ** attempt)
                        await asyncio.sleep(delay)
                        continue

                    if resp.status != 200:
                        print(f"NIST API error {resp.status} for {url}")
                        if attempt == _MAX_RETRIES - 1:
                            return None
                        continue

                    return await resp.json()

        except Exception as e:
            print(f"[!] Error fetching page: {e}")
            if attempt == _MAX_RETRIES - 1:
                return None
            await asyncio.sleep(_RETRY_DELAY)
    
    return None

def _is_too_generic(cpe: str) -> bool:
    
    parts = cpe.split(":")
    if len(parts) < 6:
        return True
    version = parts[5]
    return version in ("*", "-", "")
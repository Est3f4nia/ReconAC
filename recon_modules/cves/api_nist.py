import aiohttp

from datetime import datetime
from typing import Dict, List

from models.api_result import (
    ApiResult,
    Vulnerability,
    CWE,
    Reference,
)


BACKEND_API_URL = "http://localhost:8080"


async def lookup_cves_for_cpes(
    escaneo_id: str,
    cpes: List[str],
    max_cve_years: int,
    min_cvss_score: float,
) -> Dict[str, ApiResult]:

    if not cpes:
        return {}

    payload = {
        "escaneoId": escaneo_id,
        "cpes": cpes,
        "maxCveYears": max_cve_years,
        "minCvssScore": min_cvss_score,
    }

    timeout = aiohttp.ClientTimeout(
        total=300
    )

    async with aiohttp.ClientSession(
        timeout=timeout
    ) as session:

        async with session.post(
            f"{BACKEND_API_URL}/api/internal/vulnerabilities/lookup",
            json=payload,
        ) as response:

            if response.status >= 400:
                try:
                    problem = await response.json(content_type=None)
                    detail = problem.get("detail") if isinstance(problem, dict) else None
                except (ValueError, aiohttp.ClientError):
                    detail = None
                raise RuntimeError(
                    f"Lookup de vulnerabilidades HTTP {response.status}: "
                    f"{str(detail)[:500] if detail else 'El backend no devolvió detalles'}"
                )

            data = await response.json()

    return _parse_response(data)


def _parse_response(
    data: dict,
) -> Dict[str, ApiResult]:

    results = {}

    for cpe, entry in data.get(
        "results",
        {}
    ).items():

        vulnerabilities = [
            _parse_vulnerability(raw)
            for raw in entry.get(
                "vulnerabilities",
                []
            )
        ]

        results[cpe] = ApiResult(
            cpe_string=cpe,
            vulnerabilities=vulnerabilities,
        )

    return results


def _parse_vulnerability(
    raw: dict,
) -> Vulnerability:

    cwes = [
        CWE(id=cwe)
        for cwe in raw.get(
            "cwes",
            []
        )
    ]

    references = [
        Reference(
            url=ref.get("url", ""),
            source=ref.get("source"),
            tags=ref.get("tags", []),
        )
        for ref in raw.get(
            "references",
            []
        )
    ]

    return Vulnerability(
        cve_id=_required_string(raw, "cveId"),
        description=_required_string(raw, "description"),
        severity=_required_string(raw, "severity"),
        cvss_score=raw.get("cvssScore"),
        cvss_vector=raw.get("cvssVector"),
        published_date=_parse_datetime(
            raw.get("publishedDate")
        ),
        last_modified=_parse_datetime(
            raw.get("lastModified")
        ),
        cwes=cwes,
        references=references,
        exploit_refs=raw.get(
            "exploitRefs",
            []
        ),
        mitigation=raw.get(
            "mitigation"
        ),
        fixed_version=raw.get(
            "fixedVersion"
        ),
        vulnerable_versions=raw.get(
            "vulnerableVersions"
        ),
        fix_type=raw.get(
            "fixType"
        ),
        nist_url=raw.get(
            "nistUrl"
        ),
    )


def _parse_datetime(
    value: str | None,
):

    if not value:
        return None

    try:
        return datetime.fromisoformat(
            value.replace(
                "Z",
                "+00:00"
            )
        )
    except ValueError:
        return None

def _required_string(raw: dict[str, object], key: str) -> str:
    value = raw.get(key)

    if not isinstance(value, str) or not value.strip():
        raise ValueError(f"Campo requerido inválido: {key}")

    return value

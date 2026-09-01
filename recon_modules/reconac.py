import asyncio
from config import Config
from scanning.port_scan import initial_scan
from scanning.service_scan import service_scan
from scanning.utils.cpes.get_cpes import get_cpes
from cves.api_nist import lookup_cves_for_cpes
from models.api_result import ApiResult
from models.scan_result import ScanResult, HostResult
from models.scan_report import ScanReport
from datetime import datetime

NMAP_CMD = "nmap"

PIPELINE_TIMEOUT = 1800


async def _run_pipeline(cfg: Config) -> ScanReport:
    target = cfg.targets[0]
    open_ports = await initial_scan(NMAP_CMD, target, cfg.timeout)

    if not open_ports:
        scan_result = ScanResult(
            start_time=datetime.now(),
            hosts=[HostResult(ip=target)],
        )
        return ScanReport(scan_result, ApiResult(cpe_string=target, vulnerabilities=[]))

    scan_result = await service_scan(NMAP_CMD, target, open_ports, cfg.timeout)
    all_cpes = get_cpes(scan_result)

    if not all_cpes:
        return ScanReport(scan_result, ApiResult(cpe_string=target, vulnerabilities=[]))

    if not cfg.nvd_api_key:
        print("[*] Sin NVD API key: se omite la fase de lookup de CVEs")
        return ScanReport(scan_result, ApiResult(cpe_string=target, vulnerabilities=[]))

    api_result = await lookup_cves_for_cpes(
        all_cpes, cfg.nvd_api_key, cfg.max_cve_years, cfg.min_cvss_score
    )

    combined = ApiResult(cpe_string=target, vulnerabilities=[])
    for result in api_result.values():
        combined.vulnerabilities.extend(result.vulnerabilities)

    return ScanReport(scan_result, combined)


def run_scan(cfg: Config) -> ScanReport:
    return asyncio.run(_run_pipeline(cfg))

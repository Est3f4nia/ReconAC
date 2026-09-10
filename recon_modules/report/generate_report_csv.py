from typing import Dict, List

from models.scan_result import ScanResult
from models.api_result import ApiResult
from report.utils.cves_score import get_severity_from_cvss


def build_csv_data(
    scan_result: ScanResult,
    api_results: Dict[str, ApiResult],
) -> Dict[str, List[dict]]:

    return {
        "hosts.csv": _build_hosts(scan_result),
        "ports.csv": _build_ports(scan_result),
        "cpes.csv": _build_cpes(scan_result, api_results),
        "vulnerabilities.csv": _build_vulnerabilities(
            scan_result,
            api_results,
        ),
        "metadata.csv": _build_scan_metadata(scan_result),
    }


def _build_hosts(scan: ScanResult) -> List[dict]:

    data = []

    for host in scan.hosts:
        data.append({
            "ip": host.ip,
            "hostname": host.hostname,
            "mac": host.mac,
            "os_name": host.os.name if host.os else None,
            "os_accuracy": host.os.accuracy if host.os else 0,
            "status": host.status,
            "ports_count": len(host.ports),
            "unique_cpes_count": len(host.unique_cpes),
        })

    return data


def _build_ports(scan: ScanResult) -> List[dict]:

    data = []

    for host in scan.hosts:
        for port in host.ports:
            data.append({
                "ip": host.ip,
                "port": port.port,
                "protocol": port.protocol,
                "state": port.estado,
                "service": port.service,
                "product": port.product,
                "version": port.version,
                "extrainfo": port.extrainfo,
                "cpes": "|".join(port.cpes)
                if port.cpes
                else None,
            })

    return data


def _build_cpes(
    scan: ScanResult,
    api_results: Dict[str, ApiResult],
) -> List[dict]:

    data = []

    for host in scan.hosts:
        for cpe in host.unique_cpes:

            api = api_results.get(cpe)

            data.append({
                "ip": host.ip,
                "cpe": cpe,
                "vulns_count": (
                    len(api.vulnerabilities)
                    if api
                    else 0
                ),
                "last_checked": (
                    api.last_checked
                    if api
                    else None
                ),
            })

    return data


def _build_vulnerabilities(
    scan: ScanResult,
    api_results: Dict[str, ApiResult],
) -> List[dict]:

    data = []

    for host in scan.hosts:

        for cpe in host.unique_cpes:

            api_result = api_results.get(cpe)

            if not api_result:
                continue

            for vuln in api_result.vulnerabilities:

                cwes = vuln.cwes or [None]

                severity = (
                    vuln.severity
                    if vuln.severity
                    not in (None, "UNKNOWN", "")
                    else get_severity_from_cvss(
                        vuln.cvss_score
                    )
                )

                for cwe in cwes:

                    data.append({
                        "ip": host.ip,
                        "cpe": cpe,
                        "cve_id": vuln.cve_id,
                        "severity": severity,
                        "cvss_score": vuln.cvss_score,
                        "cvss_vector": vuln.cvss_vector,
                        "cwe_id": cwe.id if cwe else None,
                        "cwe_name": cwe.name if cwe else None,
                        "is_exploitable": vuln.is_exploitable,
                        "published_date": vuln.published_date,
                        "has_mitigation": bool(
                            vuln.mitigation
                        ),
                        "fix_type": vuln.fix_type,
                        "nist_url": vuln.nist_url,
                    })

    return data


def _build_scan_metadata(
    scan: ScanResult,
) -> List[dict]:

    return [{
        "start_time": scan.start_time,
        "end_time": scan.end_time,
        "hosts_scanned": len(scan.hosts),
        "nmap_version": scan.nmap_version,
    }]
from pathlib import Path
from typing import Dict, List, Optional
from models.scan_result import ScanResult
from models.api_result import ApiResult
from report.utils.save_report import save_report_csv
from report.utils.cves_score import get_severity_from_cvss


def generate_csv(output_dir: Path, filename: str, scan_result: ScanResult, api_results: Dict[str, ApiResult]) -> List[Path]:

    files: List[Path] = []

    # dryyyyyyyyy

    for file_path in [
        _export_hosts(scan_result, output_dir, filename),
        _export_ports(scan_result, output_dir, filename),
        _export_cpes(scan_result, api_results, output_dir, filename),
        _export_vulnerabilities(scan_result, api_results, output_dir, filename),
        _export_scan_metadata(scan_result, output_dir, filename)
    ]:
        if file_path:
            files.append(file_path)

    return files


def _export_hosts(scan: ScanResult, output_dir: Path, filename: str) -> Optional[Path]:

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
            "unique_cpes_count": len(host.unique_cpes)
        })

    return save_report_csv(data, filename + "_hosts", output_dir)


def _export_ports(scan: ScanResult, output_dir: Path, filename: str) -> Optional[Path]:

    data = []
    for host in scan.hosts:
        for port in host.ports:
            data.append({
                "ip": host.ip,
                "port": port.port,
                "protocol": port.protocol,
                "service": port.service,
                "product": port.product,
                "version": port.version,
                "extrainfo": port.extrainfo,
                "cpes": "|".join(port.cpes) if port.cpes else None
            })

    return save_report_csv(data, filename + "_ports", output_dir)


def _export_cpes(scan: ScanResult, api_results: Dict[str, ApiResult], output_dir: Path, filename: str) -> Optional[Path]:

    data = []
    for host in scan.hosts:
        for cpe in host.unique_cpes:
            api = api_results.get(cpe)
            data.append({
                "ip": host.ip,
                "cpe": cpe,
                "vulns_count": len(api.vulnerabilities) if api else 0,
                "last_checked": api.last_checked if api else None
            })

    return save_report_csv(data, filename + "_cpes", output_dir)


def _export_vulnerabilities(scan: ScanResult, api_results: Dict[str, ApiResult], output_dir: Path, filename: str) -> Optional[Path]:

    data = []
    for host in scan.hosts:
        for cpe in host.unique_cpes:
            api_result = api_results.get(cpe)
            if not api_result or not api_result.vulnerabilities:
                continue

            for vuln in api_result.vulnerabilities:
                for cwe in vuln.cwes:
                    data.append({
                        "ip": host.ip,
                        "cpe": cpe,
                        "cve_id": vuln.cve_id,
                        "severity": vuln.severity if vuln.severity not in (None, "UNKNOWN", "") else get_severity_from_cvss(vuln.cvss_score),
                        "cvss_score": vuln.cvss_score,
                        "cvss_vector": vuln.cvss_vector,
                        "cwe_id": cwe.id,
                        "cwe_name": cwe.name,
                        "is_exploitable": vuln.is_exploitable,
                        "published_date": vuln.published_date,
                        "has_mitigation": bool(vuln.mitigation),
                        "fix_type": vuln.fix_type,
                        "nist_url": vuln.nist_url
                    })

    return save_report_csv(data, filename + "_vulnerabilities", output_dir)


def _export_scan_metadata(scan: ScanResult, output_dir: Path, filename: str) -> Optional[Path]:

    data = [{
        "start_time": scan.start_time,
        "end_time": scan.end_time,
        "hosts_scanned": len(scan.hosts),
        "nmap_version": scan.nmap_version,
    }]

    return save_report_csv(data, filename + "_metadata", output_dir)
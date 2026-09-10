from datetime import datetime
from typing import Any, Dict, List

from models.scan_result import (
    ScanResult,
    HostResult,
    PortInfo,
    OSInfo,
)
from models.api_result import (
    ApiResult,
    Vulnerability,
    CWE,
    Reference,
)


def parse_datetime(value: Any) -> datetime | None:
    if not value:
        return None

    if isinstance(value, datetime):
        return value

    return datetime.fromisoformat(value)


def deserialize_scan_result(data: Dict[str, Any]) -> ScanResult:
    start_time = parse_datetime(data.get("startTime"))

    if start_time is None:
        raise ValueError("startTime is required")

    hosts: List[HostResult] = []

    for host_data in data.get("hosts", []):
        # -------------------------
        # OS
        # -------------------------
        os_data = host_data.get("os")
        os_info = None

        if isinstance(os_data, dict):
            os_info = OSInfo(
                name=os_data.get("name"),
                accuracy=os_data.get("accuracy", 0),
            )
        elif isinstance(os_data, str):
            # El resultado actual de Spring puede enviar el SO
            # directamente como string.
            os_info = OSInfo(
                name=os_data,
                accuracy=host_data.get("soProbab", 0),
            )

        # -------------------------
        # PORTS
        # -------------------------
        ports: List[PortInfo] = []

        # El JSON de ReconAC utiliza "puertos".
        # Se mantiene "ports" como fallback por compatibilidad.
        ports_data = host_data.get("puertos", host_data.get("ports", []))

        for port_data in ports_data:
            ports.append(
                PortInfo(
                    port=port_data["numero"],
                    protocol=port_data["protocolo"],
                    estado=port_data.get("estado", ""),
                    service=port_data.get("servicio", ""),
                    product=port_data.get("producto", ""),
                    version=port_data.get("version", ""),
                    extrainfo=port_data.get("extrainfo", ""),
                    cpes=port_data.get("cpes", []),
                )
            )

        hosts.append(
            HostResult(
                ip=host_data["ip"],
                mac=host_data.get("mac"),
                hostname=host_data.get("hostname"),
                os=os_info,
                status=host_data.get("status", "up"),
                ports=ports,
            )
        )

    return ScanResult(
        start_time=start_time,
        end_time=parse_datetime(data.get("endTime")),
        hosts=hosts,
        nmap_version=data.get("nmapVersion"),
    )


def deserialize_api_results(
    data: Dict[str, Any],
) -> Dict[str, ApiResult]:

    results: Dict[str, ApiResult] = {}

    for cpe, api_data in data.items():
        vulnerabilities: List[Vulnerability] = []

        for vuln_data in api_data.get("vulnerabilities", []):
            # -------------------------
            # CWE
            # -------------------------
            cwes: List[CWE] = []

            for cwe_data in vuln_data.get("cwes", []):
                if isinstance(cwe_data, dict):
                    cwes.append(
                        CWE(
                            id=cwe_data.get("id", ""),
                            name=cwe_data.get("name"),
                            description=cwe_data.get("description"),
                        )
                    )

            # -------------------------
            # REFERENCES
            # -------------------------
            references: List[Reference] = []

            for ref_data in vuln_data.get("references", []):
                if isinstance(ref_data, dict):
                    references.append(
                        Reference(
                            url=ref_data.get("url", ""),
                            source=ref_data.get("source"),
                            tags=ref_data.get("tags", []),
                        )
                    )

            # -------------------------
            # EXPLOIT REFERENCES
            # -------------------------
            exploit_refs = vuln_data.get("exploit_refs", [])

            # Compatibilidad por si algún resultado antiguo
            # utilizó otro nombre.
            if exploit_refs is None:
                exploit_refs = []

            # -------------------------
            # VULNERABILITY
            # -------------------------
            vulnerabilities.append(
                Vulnerability(
                    cve_id=vuln_data.get("cve_id", ""),
                    description=vuln_data.get("description", ""),
                    severity=vuln_data.get("severity", "UNKNOWN"),
                    cvss_score=vuln_data.get("cvss_score"),
                    cvss_vector=vuln_data.get("cvss_vector"),
                    published_date=parse_datetime(
                        vuln_data.get("published_date")
                    ),
                    last_modified=parse_datetime(
                        vuln_data.get("last_modified")
                    ),
                    cwes=cwes,
                    references=references,
                    exploit_refs=exploit_refs,
                    mitigation=vuln_data.get("mitigation"),
                    fixed_version=vuln_data.get("fixed_version"),
                    vulnerable_versions=vuln_data.get(
                        "vulnerable_versions"
                    ),
                    fix_type=vuln_data.get("fix_type"),
                    nist_url=vuln_data.get("nist_url"),
                )
            )

        # ApiResult.last_checked NO acepta None.
        # Si el JSON no trae last_checked, usamos ahora.
        last_checked = parse_datetime(api_data.get("last_checked"))

        if last_checked is None:
            last_checked = datetime.now()

        results[cpe] = ApiResult(
            cpe_string=api_data.get("cpe_string", cpe),
            vulnerabilities=vulnerabilities,
            last_checked=last_checked,
        )

    return results
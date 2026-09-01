from models.scan_result import ScanResult


def get_cpes(scan_result: ScanResult) -> list[str]:
    """Recolecta los CPEs ya detectados por Nmap durante el service scan.

    `PortInfo.cpes` se popula en el parsing de nmap (ver p_service_scan), por lo
    que aquí solo se agregan de forma deduplicada.
    """
    cpes: set[str] = set()
    for host in scan_result.hosts:
        for port in host.ports:
            cpes.update(port.cpes)
    return sorted(cpes)

import xml.etree.ElementTree as ET
from typing import List, Optional
from datetime import datetime
from models.scan_result import (ScanResult, HostResult, PortInfo, OSInfo)
from scanning.utils.validations.nmap_xml_validation import validate

"""
    Parse Nmap service scan XML and build a structured scan result.

    - Extracts open ports with service, version and product info
    - Collects CPEs (Common Platform Enumeration) per port and globally (deduplicated)
    - Attempts OS detection parsing if available (-O)
    
    Returns a ScanResult object
"""

def parse_service_scan(xml: str) -> ScanResult:

    print("[+] Parseando resultado de escaneo...")

    root = validate(xml)

    hosts: List[HostResult] = []
    start_time = datetime.now()

    for host in root.findall("host"):

        # --- STATUS ---
        status_elem = host.find("status")
        if status_elem is None:
            status = "unknown"
        else:
            status = status_elem.get("state") or "unknown"

        # --- IP ---
        addr_elem = host.find("address[@addrtype='ipv4']")
        ip = addr_elem.get("addr") if addr_elem is not None else None

        if not ip:
            continue

        # --- HOSTNAME ---
        hostname_elem = host.find("hostnames/hostname")
        hostname = (
            hostname_elem.get("name")
            if hostname_elem is not None
            else None
        )

        # --- OS DETECTION ---
        os_data: Optional[OSInfo] = None
        osmatches = host.findall(".//osmatch")

        if osmatches:

            def get_accuracy(match):
                value = match.get("accuracy")
                return int(value) if value and value.isdigit() else 0

            best = max(osmatches, key=get_accuracy)

            os_data = OSInfo(
                name=best.get("name"),
                accuracy=get_accuracy(best)
            )

        # --- PORTS ---
        ports: List[PortInfo] = []

        for port in host.findall(".//ports/port"):

            state = port.find("state")

            if state is None:
                continue

            state_value = state.get("state")

            # ReconAC actualmente solo procesa puertos abiertos.
            if state_value != "open":
                continue

            port_id = port.get("portid")

            if not port_id or not port_id.isdigit():
                continue

            protocol = port.get("protocol", "tcp")
            service = port.find("service")

            # --- CPEs ---
            cpes: set[str] = set()

            if service is not None:
                for cpe_elem in service.findall("cpe"):
                    if cpe_elem.text:
                        cpe = cpe_elem.text.strip()
                        if cpe:
                            cpes.add(cpe)

            for cpe_elem in port.findall("cpe"):
                if cpe_elem.text:
                    cpe = cpe_elem.text.strip()
                    if cpe:
                        cpes.add(cpe)

            port_info = PortInfo(
                port=int(port_id),
                protocol=protocol,
                estado="OPEN",
                service=service.get("name", "") if service is not None else "",
                product=service.get("product", "") if service is not None else "",
                version=service.get("version", "") if service is not None else "",
                extrainfo=service.get("extrainfo", "") if service is not None else "",
                cpes=sorted(cpes)
            )

            ports.append(port_info)

        # --- HOST RESULT ---
        host_result = HostResult(
            ip=ip,
            hostname=hostname,
            status=status,
            os=os_data,
            ports=ports
        )

        hosts.append(host_result)

    return ScanResult(
        start_time=start_time,
        end_time=datetime.now(),
        hosts=hosts,
        nmap_version=root.get("version"),
    )
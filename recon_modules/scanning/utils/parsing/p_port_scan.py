import scanning.utils.validations.nmap_xml_validation as xml_validation

"""
    Parse Nmap XML raw output and extract open TCP ports.

    - Ignores non-open states (filtered, closed, etc.)
    - Deduplicates ports using a set
    
    Returns a sorted list of integers
"""

# Principal cuello de botella
def parse_open_ports(xml: str) -> list[int]:
    
    root = xml_validation.validate(xml)
    
    # set to avoid duplicates
    ports: set[int] = set() # maybe someday this can analyze multiple targets

    for port in root.findall(".//ports/port"):
        
        state = port.find("state")
        if state is None or state.get("state") != "open":
            continue

        port_id = port.get("portid")
        if not port_id:
            continue

        if port_id.isdigit():
            ports.add(int(port_id))

    return sorted(ports)

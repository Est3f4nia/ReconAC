import xml.etree.ElementTree as ET

def validate(xml: str) -> ET.Element:
    try:
        root = ET.fromstring(xml) # str XML to ElementTree for traversal
        return root
    except ET.ParseError as e:
        raise ValueError(f"[!] Invalid Nmap XML output: {e}") from e
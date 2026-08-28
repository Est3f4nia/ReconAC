import re

def normalize(cpe: str) -> str:

    if not cpe:
        return "cpe:2.3:a:*:*:*:*:*:*:*:*:*:*"

    cpe = cpe.strip()

    if cpe.startswith("cpe:/"):
        cpe = "cpe:2.3:" + cpe[5:]

    if not cpe.startswith("cpe:2.3:"):
        print("DEBUG: cpe invalido")
        return "cpe:2.3:a:*:*:*:*:*:*:*:*:*:*"

    parts = cpe.split(":")

    while len(parts) < 13:
        parts.append("*")

    parts = parts[:13]

    normalized = ":".join(parts)

    if not re.match(r"^cpe:2\.3:[aho]:", normalized):
        print("DEBUG: cpe invalido")
        return "cpe:2.3:a:*:*:*:*:*:*:*:*:*:*"

    return normalized
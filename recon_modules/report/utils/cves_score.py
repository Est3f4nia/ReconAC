def get_severity_from_cvss(cvss_score: float | None) -> str:
    if cvss_score is None:
        return "UNKNOWN"
    if cvss_score >= 9.0:
        return "CRITICAL"
    elif cvss_score >= 7.0:
        return "HIGH"
    elif cvss_score >= 4.0:
        return "MEDIUM"
    elif cvss_score >= 0.1:
        return "LOW"
    else:
        return "UNKNOWN"

# reutilizaría esa función en sort_key() y en la presentación para tener una única fuente de verdad.
def resolve_severity(vuln):
    if vuln.severity not in (None, "UNKNOWN", ""):
        return vuln.severity

    return get_severity_from_cvss(vuln.cvss_score)
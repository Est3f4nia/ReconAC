from datetime import datetime
from typing import List, Dict, Any, Optional
from models.api_result import Vulnerability, CWE, Reference

"""
    Parses raw vulnerability list from NVD API
"""

def parse_single_vuln(raw_vulns: List[Dict[str, Any]]) -> List[Vulnerability]:
    
    vulnerabilities: List[Vulnerability] = []

    for item in raw_vulns:
        cve_obj = item.get("cve") or item
        vuln = _parse_single_vulnerability(cve_obj)
        if vuln:
            vulnerabilities.append(vuln)

    return vulnerabilities


def _parse_single_vulnerability(cve_data: Dict[str, Any]) -> Optional[Vulnerability]:
    
    # description ---
    cve_id = cve_data.get("id")
    if not cve_id:
        return None

    descriptions = cve_data.get("descriptions", [])
    description = next(
        (d.get("value") for d in descriptions if d.get("lang") == "en"),
        descriptions[0].get("value") if descriptions else "[-] No description provided"
    )

    # cvss ---
    cvss_score: Optional[float] = None
    cvss_vector: Optional[str] = None
    severity: str = "UNKNOWN"

    metrics = cve_data.get("metrics", {})
    for version in ["cvssMetricV31", "cvssMetricV30", "cvssMetricV2"]:
        if version in metrics and metrics[version]:
            m = metrics[version][0]
            cvss_data = m.get("cvssData", {})
            cvss_score = cvss_data.get("baseScore")
            cvss_vector = cvss_data.get("vectorString")
            severity = m.get("baseSeverity", severity)
            break

    # dates ---
    def parse_date(date_str: Optional[str]) -> Optional[datetime]:
        if not date_str:
            return None
        try:
            return datetime.fromisoformat(date_str.replace("Z", "+00:00"))
        except ValueError as e:
            print(f"[!] Error parsing date: {e}")
            return None

    # references + mitigation ---

    # references --
    references: List[Reference] = []
    patch_refs = []
    mitigation_refs = []
    exploit_refs: List[str] = []

    for ref in cve_data.get("references", []):
        url = ref.get("url", "")
        if not url:
            continue
        
        tags = ref.get("tags", [])

        references.append(Reference(
            url=url,
            source=ref.get("source"),
            tags=ref.get("tags", [])
        ))

        # exploit detection
        if any(kw in url.lower() for kw in ["exploit-db", "github.com", "metasploit", "poc", "0day"]):
            exploit_refs.append(url)

        # fix/mitigation detection
        if any(tag in tags for tag in ["Patch", "VendorFix"]):
            patch_refs.append(url)

        if any(tag in tags for tag in ["Mitigation", "Workaround"]):
            mitigation_refs.append(url)

    # mitigation --
    mitigation: Optional[str] = None
    fix_type: Optional[str] = None

    if mitigation_refs:
        mitigation = f"Workaround/Mitigation: {mitigation_refs[0]}"
        fix_type = "Workaround/Mitigation"
    elif patch_refs:
        mitigation = f"Available patch: {patch_refs[0]}"
        fix_type = "Vendor Patch"

    # mitigation: versions

    fixed_version: Optional[str] = None
    vulnerable_versions: Optional[str] = None

    configurations = cve_data.get("configurations", [])
    if configurations and isinstance(configurations, list):
        for config in configurations:
            for node in config.get("nodes", []):
                for cpe_match in node.get("cpeMatch", []):
                    if cpe_match.get("vulnerable", False):
                        # fixed version
                        if cpe_match.get("versionEndExcluding"):
                            fixed_version = f"< {cpe_match['versionEndExcluding']}"
                        elif cpe_match.get("versionEndIncluding"):
                            fixed_version = f"<= {cpe_match['versionEndIncluding']}"
                        
                        # vulnerable range
                        start = cpe_match.get("versionStartIncluding") or cpe_match.get("versionStartExcluding")
                        end = cpe_match.get("versionEndIncluding") or cpe_match.get("versionEndExcluding")
                        if start or end:
                            vulnerable_versions = f"{start or '*'} - {end or '*'}"

                        if fixed_version:
                            break
                
                if fixed_version:
                    break

    # cwes ---

    cwes: List[CWE] = []
    for weakness in cve_data.get("weaknesses", []):
        for desc in weakness.get("description", []):
            if desc.get("lang") == "en":
                cwes.append(CWE(id=desc.get("value", "")))
                break

    return Vulnerability(
        cve_id = cve_id,
        description = description,
        severity = severity,
        cvss_score = cvss_score,
        cvss_vector = cvss_vector,
        published_date = parse_date(cve_data.get("published")),
        last_modified = parse_date(cve_data.get("lastModified")),
        cwes = cwes,
        references = references,
        exploit_refs = exploit_refs,
        nist_url = f"https://nvd.nist.gov/vuln/detail/{cve_id}",
        
        mitigation = mitigation,
        vulnerable_versions = vulnerable_versions,
        fix_type = fix_type,
        fixed_version = fixed_version
    )
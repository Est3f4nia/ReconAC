from datetime import datetime
from typing import Dict
from models.scan_result import ScanResult
from models.api_result import ApiResult
from report.utils.cves_score import get_severity_from_cvss


def generate_md(scan_result: ScanResult, api_results: Dict[str, ApiResult]) -> str:

    md = []
    md.append("# ReconAC Report\n")
    md.append(f"**Generated:** {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    md.append(f"**Nmap Version:** {scan_result.nmap_version or 'N/A'}")
    md.append(f"**Hosts scanned:** {len(scan_result.hosts)}")

    for host in scan_result.hosts:

        md.append(f"\n## Host: {host.ip}")
        
        if host.hostname:
            md.append(f"**Hostname:** {host.hostname}")
        if host.mac:
            md.append(f"**MAC:** {host.mac}")
        if host.os and host.os.name:
            md.append(f"**OS:** {host.os.name} (accuracy: {host.os.accuracy}%)")
        
        md.append("\n### Open Ports & Services\n")
        md.append("| Port | Protocol | Service | Product | Version |")
        md.append("|------|----------|---------|---------|---------|")

        for port in host.ports:
            service = f"{port.service} {port.product}".strip()
            version = port.version or port.extrainfo or "-"
            
            md.append(f"| {port.port}/{port.protocol} | {port.protocol} | {service} | {port.product} | {version} |")

        md.append("\n### Vulnerabilities\n")
        
        host_cpes = host.unique_cpes
        if not host_cpes:
            md.append("> *No CPEs detected.*")
            continue

        for cpe in host_cpes:
            api_result = api_results.get(cpe)
            if not api_result or not api_result.vulnerabilities:
                md.append(f'#### CPE: "{cpe}" \n')
                md.append("> *No vulnerabilities found matching the current filters.*\n")
                continue

            vulns = api_result.vulnerabilities

            # --- Sort ---

            def sort_key(v):
                # severity ---
                real_severity = v.severity if getattr(v, 'severity', None) not in (None, "UNKNOWN", "") else get_severity_from_cvss(v.cvss_score)
                sev_order: Dict[str, int] = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "UNKNOWN": 4}

                # year ---
                year = 0
                if v.cve_id and v.cve_id.startswith("CVE-"):
                    try:
                        year = int(v.cve_id.split("-")[1])
                    except (IndexError, ValueError):
                        year = 0

                return (
                    sev_order.get(real_severity, 99),               # severity
                    not getattr(v, 'is_exploitable', False),        # exploitable first
                    -(v.cvss_score or 0),                           # CVSS descending
                    -year                                           # year descending
                )

            sorted_vulns = sorted(vulns, key = sort_key)
            # Sorting: severity > exploitable > CVSS Score > year
            # ------
    
            exploitable = [v for v in vulns if v.is_exploitable]

            critical_high = [v for v in vulns if v.severity in ("CRITICAL", "HIGH")]
            md.append(f"#### CPE: {cpe} ({len(vulns)} vulnerabilities)")
            md.append(f"**Critical/High:** {len(critical_high)} | **Exploitable:** {len(exploitable)}")

            for vuln in sorted_vulns:
                display_severity = vuln.severity
                if display_severity in (None, "UNKNOWN", ""):
                    display_severity = get_severity_from_cvss(vuln.cvss_score)

                icon = "[!!!]" if display_severity == "CRITICAL" else "[!!]" if display_severity == "HIGH" else "[!]"

                exploitable_tag = " **EXPLOITABLE**" if vuln.is_exploitable else " N/A"
                
                cvss_display = f"{vuln.cvss_score:.1f}" if vuln.cvss_score is not None else "N/A"
                
                md.append(f"\n**{icon} {vuln.cve_id}**{exploitable_tag} - {display_severity} (CVSS: {cvss_display})")
                md.append(f"**Description:** {vuln.description[:400]}{'...' if len(vuln.description) > 400 else ''}")

                if vuln.fixed_version:
                    md.append(f"**Fixed Version:** {vuln.fixed_version}")
                if vuln.vulnerable_versions:
                    md.append(f"**Vulnerable Versions:** {vuln.vulnerable_versions}")
                if vuln.fix_type:
                    md.append(f"**Fix Type:** {vuln.fix_type}")
                if vuln.mitigation:
                    md.append(f"\n**Mitigation:** {vuln.mitigation}")
                if vuln.exploit_refs:
                    md.append(f"\n**Exploit References:**")
                    for ref in vuln.exploit_refs[:5]:
                        md.append(f"- {ref}")
                if vuln.nist_url:
                    md.append(f"\n[NIST Detail]({vuln.nist_url})")

    md.append("---\n*\nReport generated by ReconAC*")
    return "\n".join(md)
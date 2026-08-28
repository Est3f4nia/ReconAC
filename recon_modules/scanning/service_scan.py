import asyncio
from scanning.utils.parsing.p_service_scan import parse_service_scan
from models.scan_result import ScanResult

"""
    Run a heavy Nmap scan just against open ports
    
    - Performs service and version detection, default NSE script scanning and OS fingerprinting
      on specific ports
    
    Returns a ScanResult object
"""

async def service_scan(nmap_cmd: str, target: str, ports: list[int], timeout: int) -> ScanResult:
    """nmap -p<ports> -n -Pn -sC -sV -O -oX"""
    
    ports_str = ",".join(str(p) for p in ports)

    proc = await asyncio.create_subprocess_exec(
        nmap_cmd, f"-p{ports_str}", "-n", "-Pn", "-sC", "-sV", "-O",
        "-oX", "-", target,
        stdout = asyncio.subprocess.PIPE,
        stderr = asyncio.subprocess.PIPE,
    )

    try:
        stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout = timeout)
    except asyncio.TimeoutError:
        proc.kill()
        await proc.wait()
        raise RuntimeError("Nmap services scan timed out")

    out = stdout.decode("utf-8", errors="ignore")
    err = stderr.decode("utf-8", errors="ignore")

    if proc.returncode != 0:
        raise RuntimeError(f"Nmap services scan failed (code {proc.returncode}): {err.strip() or out.strip()}")

    print("[+] Parsing scan output...")
    return parse_service_scan(stdout.decode("utf-8", errors = "ignore"))
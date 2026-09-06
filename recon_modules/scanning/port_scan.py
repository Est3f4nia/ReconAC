import asyncio
from scanning.utils.parsing.p_port_scan import parse_open_ports

"""
    Run a full-port Nmap scan against a target and return open TCP ports

    - Executes a TCP SYN scan across all ports (-p-) with **aggressive timing**
    - Captures XML output from Nmap and parses it into structured data
    - Enforces a timeout to prevent hanging scans
    - Raises RuntimeError on scan failure or timeout
    
    Returns a sorted list of open ports as integers
"""

async def initial_scan(nmap_cmd: str, target: str, timeout: int) -> list[int]:
    """nmap -p- --open -n -Pn -sS --min-rate 5000 -oX"""

    # dev: -oX {output} -> asyncio.subprocess.DEVNULL -> parse_open_ports(output.xml)
    proc = await asyncio.create_subprocess_exec(
        nmap_cmd, "-p-", "--open", "-n", "-Pn", "-sS", "--min-rate", "5000",
        "-oX", "-", target,
        stdout = asyncio.subprocess.PIPE,
        stderr = asyncio.subprocess.PIPE,
    )

    try:
        stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout = timeout)
        print("[+] Ejecutando escaneo inicial")
    except asyncio.TimeoutError:
        proc.kill()
        await proc.wait()
        raise RuntimeError("Nmap general scan timed out")

    out = stdout.decode("utf-8", errors="ignore")
    err = stderr.decode("utf-8", errors="ignore")

    if proc.returncode != 0:
        raise RuntimeError(f"Nmap scan failed (code {proc.returncode}): {err.strip() or out.strip()}")
    
    print("[+] Parsing scan output...")
    return parse_open_ports(stdout.decode("utf-8", errors="ignore"))
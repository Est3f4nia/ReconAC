import asyncio
from scanning.utils.parsing.p_service_scan import parse_service_scan
from models.scan_result import ScanResult

"""
Ejecuta un escaneo detallado de servicios sobre los puertos abiertos.

- Detecta servicios y versiones.
- Ejecuta scripts NSE por defecto.
- Intenta detectar el sistema operativo.
- Procesa el resultado XML de Nmap.
- Devuelve un ScanResult estructurado.
"""

async def service_scan(nmap_cmd: str, target: str, ports: list[int], timeout: int) -> ScanResult:

    ports_str = ",".join(str(p) for p in ports)

    print(f"[*] Ejecutando detección de servicios contra {target}")

    """nmap -p<ports> -n -Pn -sC -sV -O -oX"""
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
        raise RuntimeError(f"[!] El escaneo de servicios de Nmap superó el tiempo máximo permitido para el objetivo: {target}")

    out = stdout.decode("utf-8", errors="ignore")
    err = stderr.decode("utf-8", errors="ignore")

    if proc.returncode != 0:
         raise RuntimeError(f"[!] El escaneo de servicios de Nmap falló para {target} (código {proc.returncode}): {err.strip() or out.strip()}")

    scan_result = parse_service_scan(out)

    print(
        f"[*] Análisis de servicios finalizado para: {target}"
    )

    return scan_result

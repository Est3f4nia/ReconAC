import asyncio
from scanning.utils.parsing.p_port_scan import parse_open_ports

"""
Ejecuta un escaneo inicial de todos los puertos TCP contra un objetivo.

- Ejecuta Nmap sobre todos los puertos (-p-).
- Solo considera puertos abiertos.
- Utiliza XML como salida para el procesamiento posterior.
- Aplica un tiempo máximo de ejecución.
- Devuelve una lista ordenada de puertos abiertos.
"""

async def initial_scan(nmap_cmd: str, target: str, timeout: int) -> list[int]:

    print(f"[*] Ejecutando escaneo inicial contra {target}")

    """nmap -p- --open -n -Pn -sS --min-rate 5000 -oX"""
    proc = await asyncio.create_subprocess_exec(
        nmap_cmd, "-p-", "--open", "-n", "-Pn", "-sS", "--min-rate", "5000",
        "-oX", "-", target,
        stdout = asyncio.subprocess.PIPE,
        stderr = asyncio.subprocess.PIPE,
    )

    try:
        stdout, stderr = await asyncio.wait_for(proc.communicate(), timeout = timeout)
    except asyncio.TimeoutError:
        proc.kill()
        await proc.wait()
        raise RuntimeError(f"[!] El escaneo inicial de Nmap superó el tiempo máximo permitido para el objetivo: {target}")

    out = stdout.decode("utf-8", errors="ignore")
    err = stderr.decode("utf-8", errors="ignore")

    if proc.returncode != 0:
        raise RuntimeError(f"[!] El escaneo inicial de Nmap falló para {target} (código {proc.returncode}): {err.strip() or out.strip()}")

    open_ports = parse_open_ports(out)

    print(f"[*] Escaneo inicial finalizado para {target}: {len(open_ports)} puerto(s) abierto(s)")

    return open_ports

    # print("[+] Parseando salida del escaneo general...")
    # return parse_open_ports(stdout.decode("utf-8", errors="ignore"))
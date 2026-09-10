import asyncio
import os
import shutil
from datetime import datetime
from typing import Callable, Optional

from config import Config
from scanning.port_scan import initial_scan
from scanning.service_scan import service_scan
from cves.api_nist import lookup_cves_for_cpes
from models.scan_result import ScanResult, HostResult
from models.scan_report import ScanReport

NMAP_CMD = os.getenv("NMAP_CMD", "nmap")

PIPELINE_TIMEOUT = 1800

ProgressCallback = Callable[[int], None]


async def _scan_target(
    target: str,
    cfg: Config,
    on_progress: Optional[ProgressCallback] = None
) -> ScanResult:

    print(f"[*] Iniciando escaneo del objetivo: {target}")

    open_ports = await initial_scan(NMAP_CMD, target, cfg.timeout)

    if on_progress:
        on_progress(30)

    if not open_ports:
        print(f"[!] No se encontraron puertos abiertos en el objetivo: {target}")

        now = datetime.now()

        return ScanResult(
            start_time=now,
            end_time=now,
            hosts=[
                HostResult(
                    ip=target
                )
            ]
        )

    print(f"[*] {target}: {len(open_ports)} puertos abiertos detectados")

    scan_result = await service_scan(
        NMAP_CMD,
        target,
        open_ports,
        cfg.timeout
    )

    if on_progress:
        on_progress(40)

    print(f"[*] Escaneo de servicios finalizado para el objetivo: {target}")

    return scan_result


async def _run_pipeline(
    cfg: Config,
    escaneo_id: str,
    on_progress: Optional[ProgressCallback] = None
) -> ScanReport:

    if shutil.which(NMAP_CMD) is None:
        raise RuntimeError(
            "[!] Nmap no instalado o no está disponible en PATH."
        )

    if not cfg.targets:
        raise ValueError(
            "[!] Debe especificarse al menos un objetivo para el escaneo."
        )

    print(f"[*] Iniciando escaneo de {len(cfg.targets)} objetivo(s)")
    print(f"[*] Escaneo ID: {escaneo_id}")

    if on_progress:
        on_progress(10)

    scan_results = await asyncio.gather(
        *[
            _scan_target(target, cfg)
            for target in cfg.targets
        ]
    )

    hosts = [
        host
        for result in scan_results
        for host in result.hosts
    ]

    start_times = [
        result.start_time
        for result in scan_results
        if result.start_time is not None
    ]

    nmap_versions = [
        result.nmap_version
        for result in scan_results
        if result.nmap_version
    ]

    scan_result = ScanResult(
        start_time=(
            min(start_times)
            if start_times
            else datetime.now()
        ),
        end_time=datetime.now(),
        hosts=hosts,
        nmap_version=(
            nmap_versions[0]
            if nmap_versions
            else None
        )
    )

    print(
        f"[*] Escaneo general finalizado: "
        f"{len(scan_result.hosts)} host(s) procesado(s)"
    )

    if on_progress:
        on_progress(50)

    all_cpes = sorted({
        cpe
        for host in scan_result.hosts
        for cpe in host.unique_cpes
    })

    print(f"[*] CPEs únicos detectados: {len(all_cpes)}")

    if not all_cpes:
        print(
            "[!] No se encontraron CPEs. "
            "Se omite la consulta de vulnerabilidades."
        )
        return ScanReport(scan_result, {})

    if on_progress:
        on_progress(65)

    max_cve_years = (
        cfg.max_cve_years
        if cfg.max_cve_years is not None
        else 0
    )

    min_cvss_score = (
        cfg.min_cvss_score
        if cfg.min_cvss_score is not None
        else 0.0
    )

    print(
        f"[*] Consultando vulnerabilidades para "
        f"{len(all_cpes)} CPE(s)"
    )

    if on_progress:
        on_progress(70)

    api_results = await asyncio.wait_for(
        lookup_cves_for_cpes(
            escaneo_id,
            all_cpes,
            max_cve_years,
            min_cvss_score
        ),
        timeout=PIPELINE_TIMEOUT
    )

    if on_progress:
        on_progress(90)

    print(
        "[*] Consulta de vulnerabilidades finalizada."
    )

    return ScanReport(scan_result, api_results)


def run_scan(
    cfg: Config,
    escaneo_id: str,
    on_progress: Optional[ProgressCallback] = None
) -> ScanReport:

    return asyncio.run(
        _run_pipeline(
            cfg,
            escaneo_id,
            on_progress
        )
    )
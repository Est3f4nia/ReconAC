import asyncio
import os
import shutil
from datetime import datetime
from typing import Callable, Optional

from config import Config
from scanning.port_scan import initial_scan
from scanning.service_scan import service_scan
from scanning.utils.comprobacion_icmp import check_icmp
from cves.api_nist import lookup_cves_for_cpes
from models.scan_result import (ScanResult, HostResult)
from models.scan_report import ScanReport


NMAP_CMD = os.getenv("NMAP_CMD", "nmap")
PIPELINE_TIMEOUT = 1800

ProgressCallback = Callable[[int], None]

# ============================================================
# Escaneo individual
# ============================================================

async def _scan_target(target: str, cfg: Config) -> ScanResult:

    print(f"[*] Iniciando escaneo del objetivo: {target}")

    # --------------------------------------------------------
    # 1. Comprobación ICMP
    # --------------------------------------------------------

    print(
        f"[*] Comprobando disponibilidad "
        f"ICMP de {target}."
    )

    check_icmp(target, cfg.icmp_timeout)

    print(
        f"[+] {target} respondió "
        f"a la comprobación ICMP."
    )

    # --------------------------------------------------------
    # 2. Descubrimiento de puertos
    # --------------------------------------------------------

    print(
        f"[*] Buscando puertos abiertos "
        f"en {target}."
    )

    open_ports = await initial_scan(
        NMAP_CMD,
        target,
        cfg.timeout,
    )

    if not open_ports:
        print(
            f"[!] No se encontraron "
            f"puertos abiertos en "
            f"el objetivo: {target}"
        )

        now = datetime.now()

        return ScanResult(
            start_time=now,
            end_time=now,
            hosts=[
                HostResult(
                    ip=target
                )
            ],
        )

    print(
        f"[*] {target}: "
        f"{len(open_ports)} "
        f"puertos abiertos detectados."
    )

    # --------------------------------------------------------
    # 3. Detección de servicios
    # --------------------------------------------------------

    print(
        f"[*] Analizando servicios "
        f"de {target}."
    )

    scan_result = await service_scan(
        NMAP_CMD,
        target,
        open_ports,
        cfg.timeout,
    )

    print(
        f"[+] Escaneo de servicios "
        f"finalizado para {target}."
    )

    return scan_result


# ============================================================
# Pipeline
# ============================================================

async def _run_pipeline(
    cfg: Config,
    escaneo_id: str,
    on_progress: Optional[
        ProgressCallback
    ] = None,
) -> ScanReport:

    # --------------------------------------------------------
    # Helper
    # --------------------------------------------------------

    last_progress = 0

    def progress(
        value: int,
    ):
        nonlocal last_progress

        value = max(
            0,
            min(
                99,
                int(value),
            ),
        )

        # Nunca permitir retroceso.
        if value <= last_progress:
            return

        last_progress = value

        if on_progress:
            on_progress(
                value
            )

    # --------------------------------------------------------
    # Validaciones
    # --------------------------------------------------------

    progress(5)

    if shutil.which(
        NMAP_CMD
    ) is None:
        raise RuntimeError(
            "Nmap no está instalado "
            "o no está disponible en PATH."
        )

    if not cfg.targets:
        raise ValueError(
            "Debe especificarse al menos "
            "un objetivo para el escaneo."
        )

    print(
        f"[*] Iniciando escaneo "
        f"de {len(cfg.targets)} "
        f"objetivo(s)."
    )

    print(
        f"[*] Escaneo ID: "
        f"{escaneo_id}"
    )

    progress(10)

    # ========================================================
    # Reconocimiento
    #
    # Rango de progreso:
    # 10% -> 55%
    # ========================================================

    total_targets = len(
        cfg.targets
    )

    completed_targets = 0

    async def scan_and_track(
        target: str,
    ):
        nonlocal completed_targets

        result = await _scan_target(
            target,
            cfg,
        )

        completed_targets += 1

        # Repartimos 45 puntos
        # entre todos los objetivos:
        #
        # 10 -> 55
        target_progress = (
            10
            + round(
                (
                    completed_targets
                    / total_targets
                )
                * 45
            )
        )

        progress(
            target_progress
        )

        return result

    scan_results = await asyncio.gather(
        *[
            scan_and_track(
                target
            )
            for target
            in cfg.targets
        ]
    )

    # Aseguramos cierre de etapa.
    progress(55)

    # ========================================================
    # Consolidación
    # ========================================================

    print(
        "[*] Consolidando resultados "
        "del reconocimiento."
    )

    hosts = [
        host
        for result
        in scan_results
        for host
        in result.hosts
    ]

    start_times = [
        result.start_time
        for result
        in scan_results
        if result.start_time
        is not None
    ]

    nmap_versions = [
        result.nmap_version
        for result
        in scan_results
        if result.nmap_version
    ]

    scan_result = ScanResult(
        start_time=(
            min(
                start_times
            )
            if start_times
            else datetime.now()
        ),

        end_time=datetime.now(),

        hosts=hosts,

        nmap_version=(
            nmap_versions[0]
            if nmap_versions
            else None
        ),
    )

    print(
        "[+] Escaneo general "
        "finalizado: "
        f"{len(scan_result.hosts)} "
        "activo(s) procesado(s)."
    )

    progress(60)

    # ========================================================
    # CPEs
    # ========================================================

    all_cpes = sorted({
        cpe
        for host
        in scan_result.hosts
        for cpe
        in host.unique_cpes
    })

    print(
        f"[*] CPEs únicos "
        f"detectados: "
        f"{len(all_cpes)}"
    )

    progress(65)

    # --------------------------------------------------------
    # Sin CPEs
    # --------------------------------------------------------

    if not all_cpes:
        print(
            "[!] No se encontraron CPEs. "
            "Se omite la consulta "
            "de vulnerabilidades."
        )

        # El pipeline terminó,
        # pero dejamos 100 para app.py,
        # después del callback exitoso.
        progress(95)

        return ScanReport(
            scan_result,
            {},
        )

    # ========================================================
    # Preparación consulta de vulnerabilidades
    # ========================================================

    max_cve_years = (
        cfg.max_cve_years
        if (
            cfg.max_cve_years
            is not None
        )
        else 0
    )

    min_cvss_score = (
        cfg.min_cvss_score
        if (
            cfg.min_cvss_score
            is not None
        )
        else 0.0
    )

    print(
        "[*] Preparando consulta "
        "de vulnerabilidades."
    )

    progress(70)

    print(
        f"[*] Consultando "
        f"vulnerabilidades para "
        f"{len(all_cpes)} CPE(s)."
    )

    # ========================================================
    # Consulta CVE
    #
    # Esta es normalmente la etapa más larga,
    # así que ocupa buena parte del progreso.
    # ========================================================

    progress(75)

    api_results = await asyncio.wait_for(
        lookup_cves_for_cpes(
            escaneo_id,
            all_cpes,
            max_cve_years,
            min_cvss_score,
        ),
        timeout=PIPELINE_TIMEOUT,
    )

    progress(90)

    print(
        "[+] Consulta de "
        "vulnerabilidades finalizada."
    )

    # --------------------------------------------------------
    # Serialización / finalización del módulo
    # --------------------------------------------------------

    print(
        "[*] Preparando resultado "
        "final del escaneo."
    )

    progress(95)

    return ScanReport(
        scan_result,
        api_results,
    )


# ============================================================
# Entrada síncrona
# ============================================================

def run_scan(
    cfg: Config,
    escaneo_id: str,
    on_progress: Optional[
        ProgressCallback
    ] = None,
) -> ScanReport:

    return asyncio.run(
        _run_pipeline(
            cfg,
            escaneo_id,
            on_progress,
        )
    )
import os
import threading
import json
import requests

from datetime import datetime, date, timedelta
from uuid import UUID
from dataclasses import asdict, is_dataclass
from io import BytesIO

from flask import (
    Flask,
    request,
    jsonify,
    send_file,
)

from config import Config
from reconac import run_scan

from scan_output import (
    install,
    capture,
    snapshot,
)

from report.generate_report_md import generate_md
from report.generate_report_csv import build_csv_data
from report.report_serializer import (
    deserialize_scan_result,
    deserialize_api_results,
)
from report.utils.save_report import create_csv_zip

from models.scan_report import ScanReport


# ============================================================
# App
# ============================================================

app = Flask(__name__)

install()

BACKEND_API_URL = os.getenv(
    "BACKEND_API_URL",
    "http://localhost:8080",
).rstrip("/")


# ============================================================
# Estados
# ============================================================

_BACKEND_STATUS = {
    "PENDIENTE": "PENDIENTE",
    "EN_PROCESO": "EN_PROCESO",
    "COMPLETADO": "COMPLETADO",
    "FALLO": "FALLO",
}


# ============================================================
# Estado local de trabajos
# ============================================================

_scans: dict[str, dict] = {}

_lock = threading.Lock()

_MAX_SCANS = 500

_SCAN_TTL = timedelta(
    hours=24
)


# ============================================================
# Serialización
# ============================================================

def _default_serializer(obj):
    if (
        is_dataclass(obj)
        and not isinstance(obj, type)
    ):
        return asdict(obj)

    if isinstance(
        obj,
        (datetime, date),
    ):
        return obj.isoformat()

    raise TypeError(
        f"Object of type "
        f"{type(obj).__name__} "
        f"is not JSON serializable"
    )


def _to_json_safe(obj):
    return json.loads(
        json.dumps(
            obj,
            default=_default_serializer,
        )
    )


# ============================================================
# Logs legibles
# ============================================================

def _error_message(
    error: Exception,
) -> str:
    """
    Convierte una excepción en un mensaje corto
    para backend y consola del frontend.
    """

    message = str(error).strip()

    if not message:
        return error.__class__.__name__

    return message


def _log_info(
    message: str,
):
    print(
        f"[*] {message}"
    )


def _log_warning(
    message: str,
):
    print(
        f"[!] {message}"
    )


def _log_success(
    message: str,
):
    print(
        f"[+] {message}"
    )


# ============================================================
# Estado local
# ============================================================

def _update_scan(
    scan_id: str,
    **kwargs,
):
    with _lock:
        scan = _scans.get(
            scan_id
        )

        if scan is not None:
            scan.update(
                kwargs
            )


def _get_scan_copy(
    scan_id: str,
):
    with _lock:
        scan = _scans.get(
            scan_id
        )

        if scan is None:
            return None

        return dict(scan)


def _cleanup_scans():
    with _lock:
        now = datetime.now()

        expired = [
            sid
            for sid, scan
            in _scans.items()
            if (
                scan.get("status")
                in (
                    "COMPLETADO",
                    "FALLO",
                )
                and scan.get(
                    "updated_at"
                ) is not None
                and (
                    now
                    - scan[
                        "updated_at"
                    ]
                )
                > _SCAN_TTL
            )
        ]

        for sid in expired:
            _scans.pop(
                sid,
                None,
            )

        if (
            len(_scans)
            > _MAX_SCANS
        ):
            excess = (
                len(_scans)
                - _MAX_SCANS
            )

            oldest = sorted(
                _scans.items(),
                key=lambda item:
                    item[1].get(
                        "created_at"
                    )
                    or datetime.min,
            )[:excess]

            for sid, _ in oldest:
                _scans.pop(
                    sid,
                    None,
                )


# ============================================================
# Comunicación con backend
# ============================================================

def _notify_status(
    scan_id: str,
    status: str,
    progress=None,
    error=None,
) -> bool:

    backend_status = (
        _BACKEND_STATUS.get(
            status
        )
    )

    if backend_status is None:
        _log_warning(
            f"Estado desconocido: "
            f"{status}"
        )

        return False

    payload = {
        "scanId": scan_id,
        "status": backend_status,
        "progress": progress,
        "error": error,
    }

    try:
        response = requests.post(
            (
                f"{BACKEND_API_URL}"
                f"/api/internal/scans/"
                f"{scan_id}/status"
            ),
            json=payload,
            timeout=15,
        )

        if not response.ok:
            _log_warning(
                "El backend rechazó "
                "la actualización de estado "
                f"({response.status_code})."
            )

            return False

        return True

    except requests.exceptions.Timeout:
        _log_warning(
            "El backend no respondió "
            "al actualizar el estado."
        )

    except requests.exceptions.ConnectionError:
        _log_warning(
            "No se pudo conectar con "
            "el backend para actualizar "
            "el estado."
        )

    except requests.exceptions.RequestException as error:
        _log_warning(
            "Error HTTP al actualizar "
            "el estado: "
            f"{_error_message(error)}"
        )

    except Exception as error:
        _log_warning(
            "No se pudo actualizar "
            "el estado en el backend: "
            f"{_error_message(error)}"
        )

    return False


def _notify_callback(
    scan_id: str,
    report: ScanReport,
) -> bool:

    scan_result = (
        report.scan_result
    )

    hosts = [
        {
            "ip": host.ip,
            "mac": host.mac,
            "hostname": (
                host.hostname
            ),
            "os": (
                host.os.name
                if host.os
                else None
            ),
            "soProbab": (
                host.os.accuracy
                if host.os
                else None
            ),
            "puertos": [
                {
                    "numero":
                        port.port,

                    "protocolo":
                        port.protocol,

                    "estado":
                        port.estado,

                    "servicio":
                        port.service,

                    "producto":
                        port.product,

                    "version":
                        port.version,

                    "extrainfo":
                        port.extrainfo,

                    "cpes":
                        port.cpes,
                }
                for port
                in host.ports
            ],
        }
        for host
        in scan_result.hosts
    ]

    payload = {
        "hosts": hosts,

        "apiResults":
            _to_json_safe(
                report.api_results
            ),

        "nmapVersion":
            scan_result.nmap_version,

        "startTime": (
            scan_result
            .start_time
            .isoformat()
            if scan_result.start_time
            else None
        ),

        "endTime": (
            scan_result
            .end_time
            .isoformat()
            if scan_result.end_time
            else None
        ),
    }

    try:
        response = requests.post(
            (
                f"{BACKEND_API_URL}"
                f"/api/internal/scans/"
                f"{scan_id}/callback"
            ),
            json=payload,
            timeout=15,
        )

        if not response.ok:
            body = (
                response.text[:200]
                if response.text
                else ""
            )

            _log_warning(
                "El backend rechazó "
                "el resultado "
                f"({response.status_code})"
                + (
                    f": {body}"
                    if body
                    else "."
                )
            )

            return False

        return True

    except requests.exceptions.Timeout:
        _log_warning(
            "El backend no respondió "
            "al recibir el resultado "
            "del escaneo."
        )

    except requests.exceptions.ConnectionError:
        _log_warning(
            "No se pudo conectar con "
            "el backend para enviar "
            "el resultado."
        )

    except requests.exceptions.RequestException as error:
        _log_warning(
            "Error HTTP al enviar "
            "el resultado: "
            f"{_error_message(error)}"
        )

    except Exception as error:
        _log_warning(
            "No se pudo enviar "
            "el resultado al backend: "
            f"{_error_message(error)}"
        )

    return False


# ============================================================
# Scan
# ============================================================

@app.route(
    "/scan",
    methods=["POST"],
)
def start_scan():

    body = request.get_json(
        silent=True
    )

    if not body:
        return jsonify({
            "error":
                "la consulta debe tener un cuerpo"
        }), 400

    # --------------------------------------------------------
    # job_id
    # --------------------------------------------------------

    job_id = body.get(
        "job_id"
    )

    if (
        not isinstance(
            job_id,
            str,
        )
        or not job_id.strip()
    ):
        return jsonify({
            "error":
                "job_id es obligatorio"
        }), 400

    job_id = (
        job_id.strip()
    )

    # --------------------------------------------------------
    # escaneo_id
    # --------------------------------------------------------

    escaneo_id = body.get(
        "escaneo_id"
    )

    if (
        not isinstance(
            escaneo_id,
            str,
        )
        or not escaneo_id.strip()
    ):
        return jsonify({
            "error":
                "escaneo_id es obligatorio "
                "y debe ser un UUID"
        }), 400

    try:
        escaneo_id = str(
            UUID(
                escaneo_id.strip()
            )
        )

    except (
        ValueError,
        AttributeError,
    ):
        return jsonify({
            "error":
                "escaneo_id debe ser "
                "un UUID válido"
        }), 400

    # --------------------------------------------------------
    # Targets
    # --------------------------------------------------------

    targets = body.get(
        "targets"
    )

    if (
        not isinstance(
            targets,
            list,
        )
        or not targets
    ):
        return jsonify({
            "error":
                "targets debe ser "
                "una lista no vacía"
        }), 400

    targets = [
        str(target).strip()
        for target in targets
        if (
            target is not None
            and str(
                target
            ).strip()
        )
    ]

    if not targets:
        return jsonify({
            "error":
                "targets no contiene "
                "objetivos válidos"
        }), 400

    # --------------------------------------------------------
    # Configuración
    # --------------------------------------------------------

    timeout = body.get(
        "timeout",
        600,
    )

    icmp_timeout = body.get(
        "icmp_timeout",
        5,
    )

    max_cve_years = body.get(
        "max_cve_years",
        2,
    )

    min_cvss_score = body.get(
        "min_cvss_score",
        0.0,
    )

    # --------------------------------------------------------
    # Registro local
    # --------------------------------------------------------

    now = datetime.now()

    with _lock:
        if job_id in _scans:
            return jsonify({
                "error":
                    "job_id ya existe"
            }), 409

        _scans[job_id] = {
            "scan_id":
                job_id,

            "escaneo_id":
                escaneo_id,

            "status":
                "PENDIENTE",

            "progress":
                0,

            "targets":
                targets,

            "result":
                None,

            "error":
                None,

            "created_at":
                now,

            "updated_at":
                now,
        }

    # IMPORTANTE:
    # Se ejecuta fuera del lock.
    _cleanup_scans()

    # ========================================================
    # Progress
    # ========================================================

    def _on_progress(
        progress,
    ):
        try:
            if progress is None:
                return

            progress = int(
                progress
            )

            progress = max(
                0,
                min(
                    100,
                    progress,
                ),
            )

            _update_scan(
                job_id,
                progress=progress,
                updated_at=datetime.now(),
            )

            _notify_status(
                job_id,
                "EN_PROCESO",
                progress=progress,
            )

        except Exception as error:
            # Un error reportando progreso
            # no debe cancelar el escaneo.
            _log_warning(
                "No se pudo actualizar "
                "el progreso: "
                f"{_error_message(error)}"
            )

    # ========================================================
    # Trabajo
    # ========================================================

    def _run():

        _update_scan(
            job_id,
            status="EN_PROCESO",
            progress=0,
            error=None,
            updated_at=datetime.now(),
        )

        _notify_status(
            job_id,
            "EN_PROCESO",
            progress=0,
        )

        try:
            _log_info(
                "Validando configuración "
                "del escaneo."
            )

            cfg = Config(
                targets=targets,
                timeout=timeout,
                icmp_timeout=icmp_timeout,
                max_cve_years=max_cve_years,
                min_cvss_score=min_cvss_score,
            )

            cantidad = len(
                targets
            )

            _log_info(
                "Iniciando reconocimiento "
                f"de {cantidad} "
                + (
                    "objetivo."
                    if cantidad == 1
                    else "objetivos."
                )
            )

            report = run_scan(
                cfg,
                escaneo_id=escaneo_id,
                on_progress=_on_progress,
            )

            if report is None:
                raise RuntimeError(
                    "El módulo terminó "
                    "sin generar un resultado."
                )

            _log_info(
                "Reconocimiento finalizado. "
                "Procesando resultado."
            )

            safe_report = (
                _to_json_safe(
                    report
                )
            )

            # Guardamos localmente el resultado,
            # pero todavía NO marcamos COMPLETADO.
            _update_scan(
                job_id,
                result=safe_report,
                updated_at=datetime.now(),
            )

            _log_info(
                "Enviando resultado "
                "al backend."
            )

            callback_ok = (
                _notify_callback(
                    job_id,
                    report,
                )
            )

            if not callback_ok:
                raise RuntimeError(
                    "El escaneo terminó, "
                    "pero el backend no pudo "
                    "guardar el resultado."
                )

            # El callback exitoso en Spring
            # es quien persiste el resultado
            # y deja el escaneo COMPLETADO.
            _update_scan(
                job_id,
                status="COMPLETADO",
                progress=100,
                error=None,
                updated_at=datetime.now(),
            )

            _log_success(
                "Escaneo completado "
                "correctamente."
            )

        except Exception as error:
            message = (
                _error_message(
                    error
                )
            )

            _update_scan(
                job_id,
                status="FALLO",
                error=message,
                updated_at=datetime.now(),
            )

            _log_warning(
                "Escaneo finalizado "
                f"con error: {message}"
            )

            _notify_status(
                job_id,
                "FALLO",
                error=message,
            )

        finally:
            try:
                _cleanup_scans()

            except Exception as error:
                _log_warning(
                    "No se pudo limpiar "
                    "el historial temporal: "
                    f"{_error_message(error)}"
                )

    # ========================================================
    # Captura de consola
    # ========================================================

    def _run_with_output():
        try:
            with capture(
                job_id
            ):
                _log_info(
                    "Trabajo recibido. "
                    "Preparando módulo "
                    "de reconocimiento."
                )

                _run()

                state = (
                    _get_scan_copy(
                        job_id
                    )
                    or {}
                )

                _log_info(
                    "Estado final: "
                    f"{state.get('status', 'DESCONOCIDO')}"
                )

        except Exception as error:
            # Última barrera:
            # evita un thread muerto silenciosamente.
            message = (
                _error_message(
                    error
                )
            )

            print(
                "[!] Error inesperado "
                "del trabajo: "
                f"{message}"
            )

            _update_scan(
                job_id,
                status="FALLO",
                error=message,
                updated_at=datetime.now(),
            )

            _notify_status(
                job_id,
                "FALLO",
                error=message,
            )

    thread = threading.Thread(
        target=_run_with_output,
        daemon=True,
        name=f"reconac-{job_id}",
    )

    thread.start()

    return jsonify({
        "scan_id":
            job_id,

        "status":
            "PENDIENTE",
    }), 202


# ============================================================
# Status
# ============================================================

@app.route(
    "/status/<scan_id>",
    methods=["GET"],
)
def get_status(
    scan_id: str,
):

    scan = (
        _get_scan_copy(
            scan_id
        )
    )

    if scan is None:
        return jsonify({
            "error":
                "escaneo no encontrado"
        }), 404

    return jsonify({
        "scan_id":
            scan["scan_id"],

        "status":
            scan["status"],

        "progress":
            scan.get(
                "progress",
                0,
            ),

        "error":
            scan.get(
                "error"
            ),
    })


# ============================================================
# Result
# ============================================================

@app.route(
    "/result/<scan_id>",
    methods=["GET"],
)
def get_result(
    scan_id: str,
):

    scan = (
        _get_scan_copy(
            scan_id
        )
    )

    if scan is None:
        return jsonify({
            "error":
                "escaneo no encontrado"
        }), 404

    if (
        scan["status"]
        != "COMPLETADO"
    ):
        return jsonify({
            "error":
                f"escaneo es "
                f"{scan['status']}"
        }), 409

    return jsonify(
        scan["result"]
    )


# ============================================================
# Logs
# ============================================================

@app.route(
    "/logs/<job_id>",
    methods=["GET"],
)
def get_logs(
    job_id: str,
):

    try:
        after = max(
            0,
            int(
                request.args.get(
                    "after",
                    "0",
                )
            ),
        )

    except (
        TypeError,
        ValueError,
    ):
        return jsonify({
            "error":
                "after debe ser "
                "un entero"
        }), 400

    try:
        return jsonify(
            snapshot(
                job_id,
                after,
            )
        )

    except Exception as error:
        return jsonify({
            "error":
                "no se pudieron obtener "
                "los logs",

            "message":
                _error_message(
                    error
                ),
        }), 500


# ============================================================
# Health
# ============================================================

@app.route(
    "/health",
    methods=["GET"],
)
def health():
    return jsonify({
        "status":
            "ok"
    })


# ============================================================
# Reportes
# ============================================================

@app.route(
    "/report",
    methods=["POST"],
)
def generate_report():

    body = request.get_json(
        silent=True
    )

    if not body:
        return jsonify({
            "error":
                "la consulta debe "
                "tener un cuerpo"
        }), 400

    formato = body.get(
        "formato"
    )

    if formato not in (
        "md",
        "csv",
    ):
        return jsonify({
            "error":
                "formato debe ser "
                "'md' o 'csv'"
        }), 400

    scan_id = body.get(
        "scan_id"
    )

    if not scan_id:
        return jsonify({
            "error":
                "scan_id es obligatorio"
        }), 400

    resultado = body.get(
        "resultado"
    )

    if not resultado:
        return jsonify({
            "error":
                "resultado es obligatorio"
        }), 400

    try:
        scan_result = (
            deserialize_scan_result(
                resultado
            )
        )

        api_results = (
            deserialize_api_results(
                resultado.get(
                    "apiResults",
                    {},
                )
            )
        )

        # ----------------------------------------------------
        # Markdown
        # ----------------------------------------------------

        if formato == "md":
            markdown = (
                generate_md(
                    scan_result,
                    api_results,
                )
            )

            response = (
                app.response_class(
                    markdown,
                    status=200,
                    mimetype="text/markdown",
                )
            )

            response.headers[
                "Content-Disposition"
            ] = (
                "attachment; "
                f'filename="reconac_'
                f'{scan_id}.md"'
            )

            return response

        # ----------------------------------------------------
        # CSV
        # ----------------------------------------------------

        csv_data = (
            build_csv_data(
                scan_result,
                api_results,
            )
        )

        zip_bytes = (
            create_csv_zip(
                csv_data
            )
        )

        return send_file(
            BytesIO(
                zip_bytes
            ),
            mimetype="application/zip",
            as_attachment=True,
            download_name=(
                f"reconac_"
                f"{scan_id}.zip"
            ),
        )

    except Exception as error:
        message = (
            _error_message(
                error
            )
        )

        print(
            "[!] No se pudo generar "
            f"el reporte: {message}"
        )

        return jsonify({
            "error":
                "reporte no generado",

            "message":
                message,
        }), 500


# ============================================================
# Main
# ============================================================

if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True,
        use_reloader=False,
    )
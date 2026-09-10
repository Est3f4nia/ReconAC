import os
import threading
import json
import requests
from datetime import datetime, date, timedelta
from uuid import UUID
from dataclasses import asdict, is_dataclass
from flask import Flask, request, jsonify
from config import Config
from reconac import run_scan
from scan_output import install, capture, snapshot
install()
from io import BytesIO
from flask import send_file

from report.generate_report_md import generate_md
from report.generate_report_csv import build_csv_data
from report.report_serializer import (deserialize_scan_result, deserialize_api_results)
from report.utils.save_report import create_csv_zip
from models.scan_report import ScanReport

app = Flask(__name__)

BACKEND_API_URL = os.getenv("BACKEND_API_URL", "http://localhost:8080").rstrip("/")

_BACKEND_STATUS = {
    "PENDIENTE": "PENDIENTE",
    "EN_PROCESO": "EN_PROCESO",
    "COMPLETADO": "COMPLETADO",
    "FALLO": "FALLO",
}

_scans: dict[str, dict] = {}
_lock = threading.Lock()
_MAX_SCANS = 500
_SCAN_TTL = timedelta(hours=24)


def _default_serializer(obj):
    if is_dataclass(obj) and not isinstance(obj, type):
        return asdict(obj)

    if isinstance(obj, (datetime, date)):
        return obj.isoformat()

    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")


def _to_json_safe(obj):
    return json.loads(json.dumps(obj, default=_default_serializer))


def _update_scan(scan_id: str, **kwargs):
    with _lock:
        if scan_id in _scans:
            _scans[scan_id].update(kwargs)


def _cleanup_scans():
    with _lock:
        now = datetime.now()

        expired = [
            sid
            for sid, s in _scans.items()
            if s.get("status") in ("COMPLETADO", "FALLO")
            and s.get("updated_at") is not None
            and (now - s["updated_at"]) > _SCAN_TTL
        ]

        for sid in expired:
            del _scans[sid]

        if len(_scans) > _MAX_SCANS:
            oldest = sorted(
                _scans.items(),
                key=lambda x: x[1].get("created_at") or datetime.min
            )[: len(_scans) - _MAX_SCANS]

            for sid, _ in oldest:
                del _scans[sid]


def _notify_status(scan_id: str, status: str, progress=None, error=None):
    payload = {
        "scanId": scan_id,
        "status": _BACKEND_STATUS[status],
        "progress": progress,
        "error": error
    }

    try:
        resp = requests.post(
            f"{BACKEND_API_URL}/api/internal/scans/{scan_id}/status",
            json=payload,
            timeout=15,
        )

        if resp.status_code != 200:
            print(f"[!] Notificación de estado rechazada ({resp.status_code}): {scan_id}")

    except requests.exceptions.Timeout:
        print(f"[!] Timeout al notificar estado al backend: {scan_id}")

    except requests.exceptions.ConnectionError:
        print(f"[!] Error de conexión al notificar estado al backend: {scan_id}")

    except Exception as e:
        print(f"[!] Fallo al notificar estado al backend ({scan_id}): {e}")


def _notify_callback(scan_id: str, report: ScanReport):
    sr = report.scan_result

    hosts = [
        {
            "ip": h.ip,
            "mac": h.mac,
            "hostname": h.hostname,
            "os": h.os.name if h.os else None,
            "soProbab": h.os.accuracy if h.os else None,
            "puertos": [
                {
                    "numero": port.port,
                    "protocolo": port.protocol,
                    "estado": port.estado,
                    "servicio": port.service,
                    "producto": port.product,
                    "version": port.version,
                    "extrainfo": port.extrainfo,
                    "cpes": port.cpes,
                }
                for port in h.ports
            ]
        }

        for h in sr.hosts
    ]

    payload = {
        "hosts": hosts,
        "apiResults": _to_json_safe(report.api_results),
        "nmapVersion": sr.nmap_version,
        "startTime": sr.start_time.isoformat()
        if sr.start_time
        else None,
        "endTime": sr.end_time.isoformat()
        if sr.end_time
        else None
    }

    try:
        resp = requests.post(f"{BACKEND_API_URL}/api/internal/scans/{scan_id}/callback", json=payload, timeout=15)

        if resp.status_code != 200:
            print(f"[!] Callback rechazado ({resp.status_code}): {scan_id} - {resp.text[:200]}")

    except requests.exceptions.Timeout:
        print(f"[!] Timeout al notificar resultado al backend: {scan_id}")

    except requests.exceptions.ConnectionError:
        print(f"[!] Error de conexión al notificar resultado al backend: {scan_id}")

    except Exception as e:
        print(f"[!] Fallo al notificar resultado al backend ({scan_id}): {e}")


# ===== Endpoints back ======


@app.route("/scan", methods=["POST"])
def start_scan():
    body = request.get_json(silent=True)

    if not body:
        return jsonify({"error": "la consulta debe tener un cuerpo"}), 400

    if "job_id" not in body:
        return jsonify({"error": "job_id es obligatorio"}), 400

    job_id = body["job_id"]

    if not isinstance(job_id, str) or not job_id.strip():
        return jsonify({"error": "job_id no puede ser una cadena vacía"}), 400

    escaneo_id = body.get("escaneo_id")
    if not isinstance(escaneo_id, str) or not escaneo_id.strip():
        return jsonify({"error": "escaneo_id es obligatorio y debe ser un UUID"}), 400
    try:
        escaneo_id = str(UUID(escaneo_id.strip()))
    except ValueError:
        return jsonify({"error": "escaneo_id debe ser un UUID válido"}), 400

    if "targets" not in body:
        return jsonify({"error": "targets es obligatorio"}), 400

    targets = body["targets"]

    if not isinstance(targets, list) or not targets:
        return jsonify({"error": "targets no puede ser una lista vacía"}), 400

    timeout = body.get("timeout", 600)
    icmp_timeout = body.get("icmp_timeout", 5)
    # ververver
    max_cve_years = body.get("max_cve_years", 2)
    min_cvss_score = body.get("min_cvss_score", 0.0)

    now = datetime.now()

    with _lock:
        # Evita sobrescribir accidentalmente un trabajo existente.
        if job_id in _scans:
            return jsonify({"error": "job_id ya existe"}), 409

        _scans[job_id] = {
            "scan_id": job_id,
            "escaneo_id": escaneo_id,
            "status": "PENDIENTE",
            "targets": targets,
            "result": None,
            "error": None,
            "created_at": now,
            "updated_at": now
        }

        if len(_scans) > _MAX_SCANS:
            _cleanup_scans()

    def _run():

        _update_scan(job_id, status="EN_PROCESO", updated_at=datetime.now())
        _notify_status(job_id, "EN_PROCESO", progress=0)

        try:
            cfg = Config(
                targets=targets,
                timeout=timeout,
                icmp_timeout=icmp_timeout,
                max_cve_years=max_cve_years,
                min_cvss_score=min_cvss_score
            )

            report = run_scan(cfg, escaneo_id=escaneo_id, on_progress=lambda progress: _notify_status(job_id, "EN_PROCESO", progress=progress))

            _update_scan(job_id, status="COMPLETADO", result=_to_json_safe(report), updated_at=datetime.now())
            _notify_callback(job_id, report)

        except Exception as e:
            _update_scan( job_id, status="FALLO", error=str(e), updated_at=datetime.now())
            _notify_status(job_id,"FALLO",error=str(e))

        finally:
            _cleanup_scans()


    def _run_with_output():
        with capture(job_id):
            print('[*] Trabajo recibido. Preparando módulo de reconocimiento.')
            _run()
            state = _scans.get(job_id, {})
            if state.get('error'):
                print('[!] ' + state['error'])
            print('[*] Estado final: ' + state.get('status', 'DESCONOCIDO'))

    thread = threading.Thread(target=_run_with_output, daemon=True)
    thread.start()

    return jsonify({"scan_id": job_id, "status": "PENDIENTE"}), 202


@app.route("/status/<scan_id>", methods=["GET"])
def get_status(scan_id: str):
    with _lock:
        scan = _scans.get(scan_id)

    if not scan:
        return jsonify({"error": "escaneo no encontrado"}), 404

    return jsonify({
        "scan_id": scan["scan_id"],
        "status": scan["status"],
        "error": scan["error"]
    })


@app.route("/result/<scan_id>", methods=["GET"])
def get_result(scan_id: str):
    with _lock:
        scan = _scans.get(scan_id)

    if not scan:
        return jsonify({"error": "escaneo no encontrado"}), 404

    if scan["status"] != "COMPLETADO":
        return jsonify({"error": f"escaneo es {scan['status']}"}), 409

    return jsonify(scan["result"])


@app.route('/logs/<job_id>', methods=['GET'])
def get_logs(job_id):
    try:
        after = max(0, int(request.args.get('after', '0')))
    except ValueError:
        return jsonify({'error': 'after debe ser un entero'}), 400
    return jsonify(snapshot(job_id, after))


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})

@app.route("/report", methods=["POST"])
def generate_report():

    app.logger.warning("========== ENTRE A /report ==========")

    body = request.get_json(silent=True)

    if not body:
        return jsonify({
            "error": "la consulta debe tener un cuerpo"
        }), 400

    formato = body.get("formato")

    if formato not in ("md", "csv"):
        return jsonify({
            "error": "formato debe ser 'md' o 'csv'"
        }), 400

    scan_id = body.get("scan_id")

    if not scan_id:
        return jsonify({
            "error": "scan_id es obligatorio"
        }), 400

    resultado = body.get("resultado")

    if not resultado:
        return jsonify({
            "error": "resultado es obligatorio"
        }), 400

    app.logger.warning(
        "REPORT FORMATO: %s",
        formato,
    )

    app.logger.warning(
        "REPORT SCAN ID: %s",
        scan_id,
    )

    app.logger.warning(
        "REPORT HOSTS: %s",
        len(resultado.get("hosts", [])),
    )

    for host in resultado.get("hosts", []):
        app.logger.warning(
            "REPORT HOST %s -> puertos=%s",
            host.get("ip"),
            len(host.get("puertos", [])),
        )

    try:

        scan_result = deserialize_scan_result(resultado)

        api_results = deserialize_api_results(
            resultado.get("apiResults", {})
        )

        app.logger.warning(
            "REPORT DESERIALIZED -> hosts=%s",
            len(scan_result.hosts),
        )

        for host in scan_result.hosts:
            app.logger.warning(
                "REPORT DESERIALIZED HOST %s -> ports=%s",
                host.ip,
                len(host.ports),
            )

        # =========================================================
        # MARKDOWN
        # =========================================================

        if formato == "md":

            markdown = generate_md(
                scan_result,
                api_results,
            )

            response = app.response_class(
                markdown,
                status=200,
                mimetype="text/markdown",
            )

            response.headers["Content-Disposition"] = (
                f'attachment; filename="reconac_{scan_id}.md"'
            )

            return response

        # =========================================================
        # CSV
        # =========================================================

        csv_data = build_csv_data(
            scan_result,
            api_results,
        )

        zip_bytes = create_csv_zip(csv_data)

        app.logger.warning(
            "REPORT CSV -> archivos=%s, bytes_zip=%s",
            list(csv_data.keys()),
            len(zip_bytes),
        )

        return send_file(
            BytesIO(zip_bytes),
            mimetype="application/zip",
            as_attachment=True,
            download_name=f"reconac_{scan_id}.zip",
        )

    except Exception as e:

        app.logger.exception(
            "Error generando el reporte para el escaneo %s",
            scan_id,
        )

        return jsonify({
            "error": "error: reporte no generado"
        }), 500

if __name__ == "__main__":
    app.run(
        host="0.0.0.0",
        port=5000,
        debug=True,
        use_reloader=False
    )


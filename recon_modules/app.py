import os
import uuid
import threading
import json
import requests
from datetime import datetime, date, timedelta
from dataclasses import asdict, is_dataclass
from flask import Flask, request, jsonify
from config import Config
from reconac import run_scan
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
    if is_dataclass(obj):
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
            sid for sid, s in _scans.items()
            if s.get("status") in ("COMPLETADO", "FALLO")
            and s.get("updated_at") is not None
            and (now - s["updated_at"]) > _SCAN_TTL
        ]
        for sid in expired:
            del _scans[sid]
        if len(_scans) > _MAX_SCANS:
            oldest = sorted(
                _scans.items(),
                key=lambda x: x[1].get("created_at") or datetime.min,
            )[: len(_scans) - _MAX_SCANS]
            for sid, _ in oldest:
                del _scans[sid]


def _notify_status(scan_id: str, status: str, progress=None, error=None):
    payload = {
        "scanId": scan_id,
        "status": _BACKEND_STATUS[status],
        "progress": progress,
        "error": error,
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
        }
        for h in sr.hosts
    ]
    payload = {
        "hosts": hosts,
        "apiResults": {"vulnerabilities": _to_json_safe(report.api_result.vulnerabilities)},
        "nmapVersion": sr.nmap_version,
        "startTime": sr.start_time.isoformat() if sr.start_time else None,
        "endTime": sr.end_time.isoformat() if sr.end_time else None,
    }
    try:
        resp = requests.post(
            f"{BACKEND_API_URL}/api/internal/scans/{scan_id}/callback",
            json=payload,
            timeout=15,
        )
        if resp.status_code != 200:
            print(f"[!] Callback rechazado ({resp.status_code}): {scan_id} - {resp.text[:200]}")
    except requests.exceptions.Timeout:
        print(f"[!] Timeout al notificar resultado al backend: {scan_id}")
    except requests.exceptions.ConnectionError:
        print(f"[!] Error de conexión al notificar resultado al backend: {scan_id}")
    except Exception as e:
        print(f"[!] Fallo al notificar resultado al backend ({scan_id}): {e}")


@app.route("/scan", methods=["POST"])
def start_scan():
    body = request.get_json(silent=True)
    if not body or "targets" not in body:
        return jsonify({"error": "targets is required"}), 400

    targets = body["targets"]
    if not isinstance(targets, list) or not targets:
        return jsonify({"error": "targets must be a non-empty list"}), 400

    # La key la incluye el backend solo cuando el usuario optó a enriquecimiento CVE.
    # No usar silenciosamente la key a nivel de módulo.
    nvd_api_key = body.get("nvd_api_key")
    timeout = body.get("timeout", 600)
    icmp_timeout = body.get("icmp_timeout", 5)
    max_cve_years = body.get("max_cve_years", 2)
    min_cvss_score = body.get("min_cvss_score", 0.0)

    scan_id = str(uuid.uuid4())
    now = datetime.now()

    with _lock:
        _scans[scan_id] = {
            "scan_id": scan_id,
            "status": "PENDIENTE",
            "targets": targets,
            "result": None,
            "error": None,
            "created_at": now,
            "updated_at": now,
        }
        if len(_scans) > _MAX_SCANS:
            _cleanup_scans()

    def _run():
        _update_scan(scan_id, status="EN_PROCESO", updated_at=datetime.now())
        _notify_status(scan_id, "EN_PROCESO", progress=10)
        try:
            cfg = Config(
                targets=targets,
                timeout=timeout,
                icmp_timeout=icmp_timeout,
                nvd_api_key=nvd_api_key,
                max_cve_years=max_cve_years,
                min_cvss_score=min_cvss_score,
            )
            report = run_scan(cfg)
            _update_scan(scan_id, status="COMPLETADO", result=_to_json_safe(report), updated_at=datetime.now())
            _notify_callback(scan_id, report)
            _notify_status(scan_id, "COMPLETADO", progress=100)
        except Exception as e:
            _update_scan(scan_id, status="FALLO", error=str(e), updated_at=datetime.now())
            _notify_status(scan_id, "FALLO", error=str(e))
        finally:
            _cleanup_scans()

    thread = threading.Thread(target=_run, daemon=True)
    thread.start()

    return jsonify({"scan_id": scan_id, "status": "PENDIENTE"}), 202


@app.route("/status/<scan_id>", methods=["GET"])
def get_status(scan_id: str):
    with _lock:
        scan = _scans.get(scan_id)

    if not scan:
        return jsonify({"error": "scan not found"}), 404

    return jsonify({
        "scan_id": scan["scan_id"],
        "status": scan["status"],
        "error": scan["error"],
    })


@app.route("/result/<scan_id>", methods=["GET"])
def get_result(scan_id: str):
    with _lock:
        scan = _scans.get(scan_id)

    if not scan:
        return jsonify({"error": "scan not found"}), 404

    if scan["status"] != "COMPLETADO":
        return jsonify({"error": f"scan is {scan['status']}"}), 409

    return jsonify(scan["result"])


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)
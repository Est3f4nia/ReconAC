import os
import uuid
import threading
import json
import requests
from datetime import datetime, date
from dataclasses import asdict
from flask import Flask, request, jsonify
from config import Config
from reconac import run_scan
from models.scan_report import ScanReport

app = Flask(__name__)

BACKEND_API_URL = os.getenv("BACKEND_API_URL", "http://localhost:8080").rstrip("/")

_scans: dict[str, dict] = {}
_lock = threading.Lock()


def _default_serializer(obj):
    if isinstance(obj, (datetime, date)):
        return obj.isoformat()
    raise TypeError(f"Object of type {type(obj)} is not JSON serializable")


def _to_json_safe(obj):
    return json.loads(json.dumps(asdict(obj), default=_default_serializer))


def _update_scan(scan_id: str, **kwargs):
    with _lock:
        if scan_id in _scans:
            _scans[scan_id].update(kwargs)


def _notify_status(scan_id: str, status: str, progress=None, error=None):
    payload = {"scanId": scan_id, "status": status, "progress": progress, "error": error}
    try:
        requests.post(
            f"{BACKEND_API_URL}/api/internal/scans/{scan_id}/status",
            json=payload,
            timeout=10,
        )
    except Exception as e:
        print(f"[!] Fallo al notificar estado al backend: {e}")


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
        requests.post(
            f"{BACKEND_API_URL}/api/internal/scans/{scan_id}/callback",
            json=payload,
            timeout=10,
        )
    except Exception as e:
        print(f"[!] Fallo al notificar resultado al backend: {e}")


@app.route("/scan", methods=["POST"])
def start_scan():
    body = request.get_json(silent=True)
    if not body or "targets" not in body:
        return jsonify({"error": "targets is required"}), 400

    targets = body["targets"]
    if not isinstance(targets, list) or not targets:
        return jsonify({"error": "targets must be a non-empty list"}), 400

    nvd_api_key = body.get("nvd_api_key") or os.getenv("NVD_API_KEY")
    timeout = body.get("timeout", 600)
    icmp_timeout = body.get("icmp_timeout", 5)
    max_cve_years = body.get("max_cve_years", 2)
    min_cvss_score = body.get("min_cvss_score", 0.0)

    scan_id = str(uuid.uuid4())

    with _lock:
        _scans[scan_id] = {
            "scan_id": scan_id,
            "status": "QUEUED",
            "targets": targets,
            "result": None,
            "error": None,
        }

    def _run():
        _update_scan(scan_id, status="RUNNING")
        _notify_status(scan_id, "RUNNING", progress=10)
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
            _update_scan(scan_id, status="COMPLETED", result=_to_json_safe(report))
            _notify_status(scan_id, "COMPLETED", progress=100)
            _notify_callback(scan_id, report)
        except Exception as e:
            _update_scan(scan_id, status="FAILED", error=str(e))
            _notify_status(scan_id, "FAILED", error=str(e))

    thread = threading.Thread(target=_run, daemon=True)
    thread.start()

    return jsonify({"scan_id": scan_id, "status": "QUEUED"}), 202


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

    if scan["status"] != "COMPLETED":
        return jsonify({"error": f"scan is {scan['status']}"}), 409

    return jsonify(scan["result"])


@app.route("/health", methods=["GET"])
def health():
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000, debug=True)

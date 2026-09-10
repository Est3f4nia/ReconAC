"""Salida por trabajo: los contextos asyncio conservan el ID sin mezclar hilos."""
from collections import deque
from contextlib import contextmanager
from contextvars import ContextVar
from datetime import datetime, timezone
from threading import RLock
import sys
import time

_job = ContextVar("scan_output_job", default=None)
_buffers = {}
_lock = RLock()
_limit = 500
_ttl = 24 * 60 * 60


def _prune():
    now = time.monotonic()
    for key in list(_buffers):
        if now - _buffers[key]["updated"] > _ttl:
            del _buffers[key]
    while len(_buffers) > 500:
        del _buffers[min(_buffers, key=lambda key: _buffers[key]["updated"])]


def _append(job, text):
    with _lock:
        state = _buffers.get(job)
        if state is None:
            return
        state["pending"] += text.replace("\r", "\n")
        while "\n" in state["pending"] or len(state["pending"]) > 2000:
            line, sep, rest = state["pending"].partition("\n")
            if not sep:
                line, rest = line[:2000], line[2000:]
            state["pending"] = rest
            if line.strip():
                state["cursor"] += 1
                state["lines"].append({"seq": state["cursor"],
                    "timestamp": datetime.now(timezone.utc).isoformat(), "text": line[:2000]})
        state["updated"] = time.monotonic()


class _Output:
    def __init__(self, original):
        self.original = original
    def write(self, text):
        result = self.original.write(text)
        job = _job.get()
        if job is not None:
            _append(job, text)
        return result
    def flush(self):
        self.original.flush()
    def __getattr__(self, name):
        return getattr(self.original, name)


def install():
    if not isinstance(sys.stdout, _Output):
        sys.stdout = _Output(sys.stdout)
    if not isinstance(sys.stderr, _Output):
        sys.stderr = _Output(sys.stderr)


@contextmanager
def capture(job):
    with _lock:
        _prune()
        _buffers[job] = {"lines": deque(maxlen=_limit), "pending": "", "cursor": 0,
                         "updated": time.monotonic()}
        _prune()
    token = _job.set(job)
    try:
        yield
    finally:
        _append(job, "\n")
        _job.reset(token)


def snapshot(job, after=0):
    with _lock:
        _prune()
        state = _buffers.get(job)
        if state is None:
            return {"available": False, "lines": [], "nextCursor": after, "truncated": False}
        lines = list(state["lines"])
        return {"available": True, "lines": [line for line in lines if line["seq"] > after],
                "nextCursor": state["cursor"],
                "truncated": bool(lines and after < lines[0]["seq"] - 1)}

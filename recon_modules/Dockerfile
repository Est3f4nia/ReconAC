# syntax=docker/dockerfile:1
FROM python:3.12-slim-bookworm
ENV PYTHONDONTWRITEBYTECODE=1 PYTHONUNBUFFERED=1 PIP_NO_CACHE_DIR=1
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends nmap iputils-ping ca-certificates \
    && rm -rf /var/lib/apt/lists/*
COPY requirements.txt requirements-docker.txt ./
RUN pip install -r requirements-docker.txt && pip check
COPY . .
# Nmap -sS / -O require raw sockets. Compose grants only NET_RAW to this process.
EXPOSE 5000
HEALTHCHECK --interval=15s --timeout=5s --start-period=15s --retries=5 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://127.0.0.1:5000/health', timeout=3)"
CMD ["gunicorn", "--config", "gunicorn.conf.py", "app:app"]

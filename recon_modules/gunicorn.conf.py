# Jobs and log buffers are process-local. Do not increase workers or enable preload.
bind = "0.0.0.0:5000"
workers = 1
worker_class = "gthread"
threads = 4
timeout = 120
graceful_timeout = 30
accesslog = "-"
errorlog = "-"
capture_output = False

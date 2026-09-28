import platform
import subprocess


def check_icmp(
    target: str,
    timeout: int,
) -> None:
    if timeout <= 0:
        raise ValueError(
            "El timeout ICMP debe ser mayor que 0."
        )

    system = platform.system().lower()

    if system == "windows":
        command = [
            "ping",
            "-n",
            "1",
            "-w",
            str(timeout * 1000),
            target,
        ]
    else:
        command = [
            "ping",
            "-c",
            "1",
            "-W",
            str(timeout),
            target,
        ]

    try:
        result = subprocess.run(
            command,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            timeout=timeout + 1,
            check=False,
        )
    except subprocess.TimeoutExpired as exc:
        raise RuntimeError(
            f"El objetivo {target} no respondió al ICMP "
            f"dentro de {timeout} segundos."
        ) from exc
    except OSError as exc:
        raise RuntimeError(
            f"No se pudo ejecutar la comprobación ICMP "
            f"para {target}."
        ) from exc

    if result.returncode != 0:
        raise RuntimeError(
            f"El objetivo {target} no respondió al ICMP "
            f"dentro de {timeout} segundos."
        )
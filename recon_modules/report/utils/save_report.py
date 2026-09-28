from pathlib import Path
from typing import Dict, List, Optional
from io import BytesIO
import csv
import zipfile

from models.scan_result import ScanResult
from models.api_result import ApiResult
from report.generate_report_md import generate_md


def save_report_md(
    scan_result: ScanResult,
    api_results: Dict[str, ApiResult],
    full_path: Path
) -> Optional[Path]:

    report = generate_md(scan_result, api_results)

    full_path.parent.mkdir(parents=True, exist_ok=True)

    try:
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(report)

        return full_path

    except PermissionError:
        print(f"[!] Error: couldn't save to {full_path}")

    except Exception as e:
        print(f"[!] Error while saving report: {e}")

    return None


def save_report_csv(
    data: List[dict],
    filename: str,
    output_dir: Path
) -> Optional[Path]:

    if not data:
        return None

    output_dir.mkdir(parents=True, exist_ok=True)

    path = output_dir / f"{filename}.csv"

    try:
        with path.open("w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(
                f,
                fieldnames=data[0].keys()
            )

            writer.writeheader()
            writer.writerows(data)

        return path

    except PermissionError:
        print(f"[!] Error: couldn't save to {output_dir}")

    except Exception as e:
        print(f"[!] Error while saving report: {e}")

    return None


def csv_to_bytes(data: List[dict]) -> bytes:
    if not data:
        return b""

    output = BytesIO()

    import io

    text_stream = io.TextIOWrapper(
        output,
        encoding="utf-8",
        newline=""
    )

    writer = csv.DictWriter(
        text_stream,
        fieldnames=list(data[0].keys())
    )

    writer.writeheader()
    writer.writerows(data)

    text_stream.flush()
    text_stream.detach()

    return output.getvalue()


def create_csv_zip(
    files: Dict[str, List[dict]]
) -> bytes:

    output = BytesIO()

    with zipfile.ZipFile(
        output,
        mode="w",
        compression=zipfile.ZIP_DEFLATED
    ) as zip_file:

        for filename, data in files.items():

            if not data:
                continue

            csv_content = csv_to_bytes(data)

            zip_file.writestr(
                filename,
                csv_content
            )

    return output.getvalue()
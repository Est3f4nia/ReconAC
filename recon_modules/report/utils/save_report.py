from pathlib import Path
from typing import Dict
from models.scan_result import ScanResult
from models.api_result import ApiResult
from report.generate_report_md import generate_md
import csv
from datetime import datetime
from pathlib import Path
from typing import List, Optional


TIMESTAMP = datetime.now().strftime("%Y%m%d_%H%M%S")

def save_report_md(scan_result: ScanResult, api_results: Dict[str, ApiResult], full_path: Path) -> Optional[Path]:
    
    if not scan_result and not api_results:
        return None

    report = generate_md(scan_result, api_results)

    full_path.parent.mkdir(parents = True, exist_ok = True)

    try:
        with open(full_path, "w", encoding="utf-8") as f:
            f.write(report)
    
    except PermissionError:
        print(f"[!] Error: couldn't save to {full_path}")
    
    except Exception as e:
        print(f"[!] Error while saving report: {e}")


def save_report_csv(data: List[dict], filename: str, output_dir: Path) -> Optional[Path]:
    
    if not data:
        return None

    output_dir.mkdir(parents=True, exist_ok=True)
 
    path = output_dir / f"{filename}.csv"

    try: 
        with path.open("w", newline="", encoding="utf-8") as f:
            writer = csv.DictWriter(f, fieldnames=data[0].keys())
            writer.writeheader()
            writer.writerows(data)
    
    except PermissionError:
        print(f"[!] Error: couldn't save to {output_dir}")
    
    except Exception as e:
        print(f"[!] Error while saving report: {e}")

    return path
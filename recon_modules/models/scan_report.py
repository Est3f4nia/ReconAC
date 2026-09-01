from dataclasses import dataclass

from models.api_result import ApiResult
from models.scan_result import ScanResult


@dataclass
class ScanReport:
    scan_result: ScanResult
    api_result: ApiResult

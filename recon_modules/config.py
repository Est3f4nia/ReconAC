from dataclasses import dataclass
from typing import Optional
import os


@dataclass
class Config:
    targets: list[str]
    timeout: int
    icmp_timeout: int
    nvd_api_key: Optional[str] = None
    max_cve_years: Optional[int] = None
    min_cvss_score: Optional[float] = None

    def __post_init__(self):
        if not self.nvd_api_key:
            self.nvd_api_key = os.getenv("NVD_API_KEY")

        # La NVD API key es opcional (ADR-011): sin ella se omite la fase
        # de lookup de CVEs en lugar de fallar el escaneo.
        if not self.targets:
            raise ValueError("At least one target must be specified")

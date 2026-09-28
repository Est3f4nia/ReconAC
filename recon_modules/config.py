from dataclasses import dataclass
from typing import Optional


@dataclass
class Config:
    targets: list[str]
    timeout: int
    icmp_timeout: int
    max_cve_years: Optional[int] = None
    min_cvss_score: Optional[float] = None

    def __post_init__(self):
        # La NVD API key es opcional (ADR-011): sin ella se omite la fase
        # de lookup de CVEs en lugar de fallar el escaneo.
        if not self.targets:
            raise ValueError("At least one target must be specified")

from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional


@dataclass(frozen=True)
class CWE:
    id: str
    name: Optional[str] = None
    description: Optional[str] = None


@dataclass(frozen=True)
class Reference:
    url: str
    source: Optional[str] = None
    tags: List[str] = field(default_factory=list)


@dataclass(frozen=True)
class Vulnerability:
    cve_id: str
    
    description: str
    severity: str
    cvss_score: Optional[float] = None
    cvss_vector: Optional[str] = None

    # epss y kev
    
    published_date: Optional[datetime] = None
    last_modified: Optional[datetime] = None
    
    cwes: List[CWE] = field(default_factory=list)
    
    references: List[Reference] = field(default_factory=list)
    exploit_refs: List[str] = field(default_factory=list)
    
    mitigation: Optional[str] = None
    fixed_version: Optional[str] = None
    vulnerable_versions: Optional[str] = None
    fix_type: Optional[str] = None
    
    nist_url: Optional[str] = None

    # posible y probable reemplazo por epss
    @property
    def is_exploitable(self) -> bool:   # public exploitation references
        return len(self.exploit_refs) > 0


# CPE + Vulns
@dataclass(frozen=True)
class ApiResult:
    cpe_string: str
    vulnerabilities: List[Vulnerability] = field(default_factory=list)
    last_checked: datetime = field(default_factory=datetime.now)
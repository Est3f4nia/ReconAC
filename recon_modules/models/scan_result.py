from dataclasses import dataclass, field
from datetime import datetime
from typing import List, Optional


@dataclass(frozen=True)
class PortInfo:
    port: int
    protocol: str
    estado: str
    service: str = ''
    product: str = ''
    version: str = ''
    extrainfo: str = ''
    cpes: List[str] = field(default_factory=list)

    
@dataclass(frozen=True)
class OSInfo:
    name: Optional[str] = None
    accuracy: int = 0 


@dataclass(frozen=True)
class HostResult:
    ip: str
    mac: Optional[str]  = None
    hostname: Optional[str] = None
    os: Optional[OSInfo] = None
    status: str = 'up'  # esto se va
    ports: List[PortInfo] = field(default_factory=list)
    
    unique_cpes: List[str] = field(init=False)

    def __post_init__(self):
        cpesS = {cpe for port in self.ports for cpe in port.cpes}
        object.__setattr__(self, 'unique_cpes', sorted(cpesS))


@dataclass(frozen=True)
class ScanResult:
    start_time: datetime
    end_time: Optional[datetime] = None
    hosts: List[HostResult] = field(default_factory=list) # multi-host support
    nmap_version: Optional[str] = None
    
    # DEBUGGING ---
    # xml_output: Optional[str] = None    # raw XML
    # error: Optional[str] = None     # partial failure cases
package com.tup.reconac.feature.puerto.models;

// Estados Nmap
public enum PuertoEstado {
    OPEN,
    CLOSED,
    FILTERED,
    UNFILTERED,
    OPEN_OR_FILTERED,
    CLOSED_OR_FILTERED
}

// vulnEnum solo toma puertos OPEN (p_service_scan -> parse_service_scan()),
// pero se implementa para futura mejora (filtrado por estado de puerto)
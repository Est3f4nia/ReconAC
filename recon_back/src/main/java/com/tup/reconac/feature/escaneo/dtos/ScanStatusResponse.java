package com.tup.reconac.feature.escaneo.dtos;

import java.util.Map;

public record ScanStatusResponse(
        String scanId,
        String status,
        String error
) {
    public static ScanStatusResponse fromExternal(Map<String, Object> external) {
        return new ScanStatusResponse(
                (String) external.get("scan_id"),
                (String) external.get("status"),
                (String) external.get("error")
        );
    }
}

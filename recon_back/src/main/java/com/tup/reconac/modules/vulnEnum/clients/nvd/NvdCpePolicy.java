package com.tup.reconac.modules.vulnEnum.clients.nvd;

import java.util.ArrayList;
import java.util.List;

public final class NvdCpePolicy {

    private NvdCpePolicy() {}

    /**
     * Determina si una CPE tiene precisión suficiente para consultar NVD.
     *
     * ReconAC conserva CPEs genéricas detectadas, pero no realiza
     * enriquecimiento CVE si no existe una versión concreta.
     */
    public static boolean isQueryable(String cpe) {
        if (cpe == null || cpe.isBlank()) {
            return false;
        }

        String formatted;

        try {
            formatted = NvdCpeQuery.formatted(cpe.trim());
        } catch (IllegalArgumentException e) {
            return false;
        }

        String version = extractVersion(formatted);

        return isConcrete(version);
    }

    private static String extractVersion(String cpe23) {
        // cpe:2.3:part:vendor:product:version:...
        String body = cpe23.substring("cpe:2.3:".length());

        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        boolean escaped = false;

        for (char c : body.toCharArray()) {
            if (escaped) {
                current.append('\\').append(c);
                escaped = false;
            } else if (c == '\\') {
                escaped = true;
            } else if (c == ':') {
                fields.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        fields.add(current.toString());

        if (fields.size() != 11) {
            throw new IllegalArgumentException(
                    "CPE 2.3 inválida: " + cpe23
            );
        }

        // part=0, vendor=1, product=2, version=3
        return fields.get(3);
    }

    private static boolean isConcrete(String value) {
        if (value == null
                || value.isBlank()
                || value.equals("*")
                || value.equals("-")) {
            return false;
        }

        boolean escaped = false;

        for (char c : value.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                continue;
            }

            if (c == '*' || c == '?') {
                return false;
            }
        }

        return true;
    }
}

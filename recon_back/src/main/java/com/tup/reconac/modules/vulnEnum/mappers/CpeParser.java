package com.tup.reconac.modules.vulnEnum.mappers;

import com.tup.reconac.modules.vulnEnum.models.Cpe;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class CpeParser {
    private CpeParser() {}

    public static void enrich(Cpe cpe) {
        Parts parts = parse(cpe.getUri());
        cpe.setVendor(parts.vendor());
        cpe.setProducto(parts.product());
        cpe.setVersion(parts.version());
        if (cpe.getUriLegible() == null) cpe.setUriLegible(cpe.getUri());
    }

    public static Parts parse(String uri) {
        if (uri == null) throw new IllegalArgumentException("CPE requerido");
        boolean legacy = uri.startsWith("cpe:/");
        if (!legacy && !uri.startsWith("cpe:2.3:")) {
            throw new IllegalArgumentException("Formato CPE no soportado: " + uri);
        }
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean escaped = false;
        for (char c : uri.substring(legacy ? 5 : 8).toCharArray()) {
            if (escaped) { field.append('\\').append(c); escaped = false; }
            else if (c == '\\') escaped = true;
            else if (c == ':') { fields.add(field.toString()); field.setLength(0); }
            else field.append(c);
        }
        if (escaped) throw new IllegalArgumentException("Escape CPE incompleto");
        fields.add(field.toString());
        if (fields.size() < 3 || (!legacy && fields.size() != 11)
                || !List.of("a", "o", "h").contains(fields.get(0))) {
            throw new IllegalArgumentException("CPE inválido: " + uri);
        }
        return new Parts(value(fields.get(1), legacy), value(fields.get(2), legacy),
                fields.size() > 3 ? value(fields.get(3), legacy) : null);
    }

    private static String value(String value, boolean legacy) {
        if (value.isEmpty() || value.equals("*") || value.equals("-")) return null;
        if (legacy) return URLDecoder.decode(value.replace("+", "%2B"), StandardCharsets.UTF_8);
        return value.replaceAll("\\\\(.)", "$1");
    }

    public record Parts(String vendor, String product, String version) {}
}

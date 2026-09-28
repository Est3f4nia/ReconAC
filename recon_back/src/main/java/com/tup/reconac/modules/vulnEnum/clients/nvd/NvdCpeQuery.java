package com.tup.reconac.modules.vulnEnum.clients.nvd;

import com.tup.reconac.modules.vulnEnum.mappers.CpeParser;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class NvdCpeQuery {

    private static final String CPE_23_PREFIX = "cpe:2.3:";
    private NvdCpeQuery() {}

    /**
     * Normaliza una CPE legacy (cpe:/...) al formato CPE 2.3.
     * Las CPE 2.3 se conservan tal cual después de validarlas.
     */
    public static String formatted(String cpe) {
        if (cpe == null || cpe.isBlank()) {
            throw new IllegalArgumentException("CPE requerido");
        }

        cpe = cpe.trim();

        // Valida estructura soportada.
        CpeParser.parse(cpe);

        if (cpe.startsWith(CPE_23_PREFIX)) {
            return cpe;
        }

        // Legacy: cpe:/part:vendor:product:version:update:edition:language
        String[] uri = cpe.substring(5).split(":", -1);

        if (uri.length > 7) {
            throw new IllegalArgumentException(
                    "CPE URI contiene demasiados campos: " + cpe
            );
        }

        String[] fields = new String[11];
        Arrays.fill(fields, "*");

        for (int i = 0; i < uri.length; i++) {
            fields[i] = bind(uri[i]);
        }

        /*
         * CPE 2.2 puede empaquetar:
         *
         * edition~sw_edition~target_sw~target_hw~other
         *
         * dentro del campo edition.
         */
        if (uri.length > 5 && uri[5].startsWith("~")) {
            String[] packed = uri[5].split("~", -1);

            if (packed.length != 6) {
                throw new IllegalArgumentException(
                        "Edición CPE empaquetada inválida: " + cpe
                );
            }

            fields[5] = bind(packed[1]); // edition
            fields[7] = bind(packed[2]); // sw_edition
            fields[8] = bind(packed[3]); // target_sw
            fields[9] = bind(packed[4]); // target_hw
            fields[10] = bind(packed[5]); // other
        }

        return CPE_23_PREFIX + String.join(":", fields);
    }

    /**
     * Decide qué parámetro utilizar contra la CVE API de NVD.
     *
     * cpeName:
     *   CPE suficientemente concreta.
     *
     * virtualMatchString:
     *   CPE genérica, normalmente porque la versión es ANY (*)
     *   o contiene wildcards.
     *
     * Importante:
     *   "-" NO es un wildcard. Significa N/A y por tanto puede formar
     *   parte de una CPE concreta.
     */
    public static String parameter(String original) {
        String cpe = formatted(original);
        List<String> fields = split23(cpe);

        String vendor = fields.get(1);
        String product = fields.get(2);
        String version = fields.get(3);

        /*
         * Para ReconAC una CPE sin vendor o producto concreto no aporta
         * suficiente precisión para relacionar vulnerabilidades con
         * el servicio detectado.
         *
         * Además evita consultas monstruosas como:
         *
         * cpe:2.3:a:apache:*:*:*:*:*:*:*:*:*
         * cpe:2.3:a:*:*:*:*:*:*:*:*:*:*
         */
        if (isGeneric(vendor) || isGeneric(product)) {
            throw new IllegalArgumentException(
                    "CPE demasiado genérica para consultar NVD: " + cpe
            );
        }

        /*
         * Una versión ANY o con wildcard debe compararse contra
         * CPE Match Criteria.
         *
         * Ej:
         * cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*
         *
         * -> virtualMatchString
         */
        if (isGeneric(version)) {
            return "virtualMatchString";
        }

        /*
         * Versiones concretas, incluida "-", utilizan cpeName.
         *
         * Ej:
         * cpe:2.3:a:apache:http_server:2.4.58:*:*:*:*:*:*:*
         *
         * -> cpeName
         */
        return "cpeName";
    }

    /**
     * Separa una CPE 2.3 respetando ':' escapados.
     */
    private static List<String> split23(String cpe) {
        if (!cpe.startsWith(CPE_23_PREFIX)) {
            throw new IllegalArgumentException(
                    "Se esperaba CPE 2.3: " + cpe
            );
        }

        String body = cpe.substring(CPE_23_PREFIX.length());

        List<String> fields = new ArrayList<>(11);
        StringBuilder field = new StringBuilder();

        boolean escaped = false;

        for (char ch : body.toCharArray()) {
            if (escaped) {
                field.append('\\').append(ch);
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                escaped = true;
                continue;
            }

            if (ch == ':') {
                fields.add(field.toString());
                field.setLength(0);
                continue;
            }

            field.append(ch);
        }

        if (escaped) {
            throw new IllegalArgumentException(
                    "Escape CPE incompleto: " + cpe
            );
        }

        fields.add(field.toString());

        if (fields.size() != 11) {
            throw new IllegalArgumentException(
                    "CPE 2.3 debe contener 11 componentes: " + cpe
            );
        }

        return fields;
    }

    /**
     * Determina si un componente representa una búsqueda genérica.
     *
     * "*"     -> genérico
     * vacío   -> genérico
     * "8.*"   -> genérico
     * "8.?"   -> genérico
     * "-"     -> NO genérico (N/A)
     * "2.4.58"-> NO genérico
     */
    private static boolean isGeneric(String value) {
        if (value == null || value.isEmpty() || value.equals("*")) {
            return true;
        }

        boolean escaped = false;

        for (char ch : value.toCharArray()) {
            if (escaped) {
                escaped = false;
                continue;
            }

            if (ch == '\\') {
                escaped = true;
                continue;
            }

            if (ch == '*' || ch == '?') {
                return true;
            }
        }

        return false;
    }

    private static String bind(String raw) {
        if (raw == null || raw.isEmpty() || raw.equals("*")) {
            return "*";
        }

        if (raw.equals("-")) {
            return "-";
        }

        String decoded = URLDecoder.decode(
                raw.replace("+", "%2B"),
                StandardCharsets.UTF_8
        );

        StringBuilder result = new StringBuilder();

        for (char c : decoded.toCharArray()) {
            if (c == 1) {
                result.append('?');
            } else if (c == 2) {
                result.append('*');
            } else {
                if (!Character.isLetterOrDigit(c)
                        && c != '_'
                        && c != '.'
                        && c != '-') {
                    result.append('\\');
                }

                result.append(c);
            }
        }

        return result.toString();
    }
}

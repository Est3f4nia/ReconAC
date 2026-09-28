package com.tup.reconac.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.*;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.*;
import io.swagger.v3.oas.models.servers.Server;
import jakarta.validation.Valid;
import org.springdoc.core.customizers.OperationCustomizer;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.*;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI reconApi() {

        var components = new Components()
                .addSecuritySchemes("bearerAuth", new SecurityScheme().type(SecurityScheme.Type.HTTP)
                        .scheme("bearer").bearerFormat("JWT")
                        .description("Access token obtenido en /api/auth/login. Si hay cookie access_token, el servidor la prioriza."))
                .addSecuritySchemes("cookieAuth", new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE).name("access_token")
                        .description("Cookie emitida por login/refresh. Las escrituras requieren X-XSRF-TOKEN = cookie XSRF-TOKEN."));

        var problem = new ObjectSchema()
                .addProperty("type", new StringSchema())
                .addProperty("title", new StringSchema())
                .addProperty("status", new IntegerSchema())
                .addProperty("detail", new StringSchema())
                .addProperty("instance", new StringSchema())
                .addProperty("errors", new ArraySchema().items(new StringSchema()))
                .addProperty("timestamp", new StringSchema().format("date-time"));

        problem.setDescription("ProblemDetail del manejador de errores. errors, instance y timestamp pueden no estar presentes en errores de filtros o integración.");
        components.addSchemas("ApiProblem", problem);

        Map.of("400", "Validación o solicitud rechazada", "401", "JWT o credenciales ausentes, inválidos o expirados",
                "403", "Permisos insuficientes o CSRF inválido", "404", "Recurso no encontrado o no accesible",
                "409", "Conflicto con los datos existentes", "429", "Límite de solicitudes o resultados excedido",
                "500", "Error inesperado del servidor", "502", "Fallo de consulta al proveedor NVD")
                .forEach((code, description) -> components.addResponses("Error" + code,
                        new ApiResponse().description(description).content(new Content().addMediaType(
                                "application/problem+json", new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem"))))));

        return new OpenAPI()
                .info(new Info()
                        .title("ReconAC API")
                        .version("0.0.1-SNAPSHOT")
                        .description("API de auditorías, reconocimiento de activos y análisis de vulnerabilidades. "
                        + "Autenticación mediante JWT Bearer o cookies. Los endpoints internos son exclusivos de la integración Python "
                        + "y no exigen JWT actualmente: deben permanecer dentro de la red de confianza. "
                        + "Los listados usan page (base 0), size y sort=propiedad,asc|desc. Las respuestas conservan sus tipos originales: "
                        + "algunas usan BaseResponse, otras DTO directo o Page. Los errores utilizan application/problem+json."))
                .tags(List.of(
                        new Tag().name("Auth / Usuario").description("Registro, sesión, renovación y clave NVD del usuario."),
                        new Tag().name("Auditorías").description("Gestión de auditorías y estadísticas de ejecución."),
                        new Tag().name("Escaneos").description("Inicio asíncrono, consulta, progreso, salida y organización de escaneos."),
                        new Tag().name("Activos").description("Inventario de hosts y asociación con escaneos."),
                        new Tag().name("Métricas / Dashboard").description("Resúmenes y análisis agregados del servicio de métricas."),
                        new Tag().name("Reportes").description("Descargas Markdown o ZIP/CSV generadas con Python."),
                        new Tag().name("Vulnerabilidades").description("Detalle del catálogo CVE persistido."),
                        new Tag().name("Integración interna").description("Callbacks y lookup exclusivos de Python. Sin JWT/CSRF; bloqueados en el proxy público.")))
                .components(components).servers(List.of(new Server().url("/").description("Mismo origen que la documentación")));
    }

    @Bean
    OperationCustomizer reconOperations() {

        return (operation, handler) -> {

            String controller = handler.getBeanType().getSimpleName();
            boolean internal = controller.equals("ScanInternalController") || controller.equals("InternalVulnerabilityController");
            boolean publicAuth = controller.equals("AuthController");

            operation.setSecurity(internal || publicAuth ? List.of() : List.of(
                    new SecurityRequirement().addList("bearerAuth"), new SecurityRequirement().addList("cookieAuth")));

            if (!internal && !publicAuth) {
                operation.getResponses().addApiResponse("401", new ApiResponse().$ref("#/components/responses/Error401"));
                operation.getResponses().addApiResponse("403", new ApiResponse().$ref("#/components/responses/Error403"));
            }

            if (Arrays.stream(handler.getMethodParameters()).anyMatch(p -> p.hasParameterAnnotation(Valid.class)) && !internal) {
                operation.getResponses().addApiResponse("400", new ApiResponse().$ref("#/components/responses/Error400"));
            }

            operation.getResponses().addApiResponse("500", new ApiResponse().$ref("#/components/responses/Error500"));

            if (operation.getParameters() != null) operation.getParameters().forEach(p -> {

                if (p.getDescription() != null) return;

                String description = switch (p.getName()) {
                    case "jobId" -> "Identificador del trabajo Python (moduloJobId), no el UUID persistido del escaneo";
                    case "escaneoId" -> "UUID del escaneo persistido en PostgreSQL";
                    case "auditoriaId" -> "UUID de la auditoría";
                    case "cveId" -> "Identificador público CVE, por ejemplo CVE-2024-0001; no es un UUID";
                    case "id" -> "UUID del recurso";
                    case "after" -> "Último seq recibido; 0 inicia la lectura. Usar nextCursor de la respuesta siguiente";
                    case "formato" -> "MD devuelve Markdown; CSV devuelve un ZIP de archivos CSV";
                    default -> null;
                };

                if (description != null) p.setDescription(description);

            });

            operation.getResponses().values().forEach(r -> jsonContent(r.getContent()));
            if (operation.getRequestBody() != null) jsonContent(operation.getRequestBody().getContent());

            return operation;
        };
    }

    private static void jsonContent(Content content) {
        if (content != null && content.containsKey("*/*")) content.addMediaType("application/json", content.remove("*/*"));
    }
}

package com.tup.reconac.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuración externalizada de los endpoints de los módulos de reconocimiento.
 * Cada módulo se registra por nombre (p.ej. "recon") con su URL y timeout.
 *
 * ADR-013: Opción A (mapa externalizado). Opción B (registro en DB/Redis) queda
 * como mejora futura y no alteraría los call sites (ver comentario en
 * EscaneoCreateService).
 */
@Component
@ConfigurationProperties(prefix = "app.modules")
public class ModulesConfig {

    private Map<String, ModuleEndpoint> endpoints = new HashMap<>();

    public Map<String, ModuleEndpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Map<String, ModuleEndpoint> endpoints) {
        this.endpoints = endpoints;
    }

    public ModuleEndpoint getEndpoint(String name) {
        ModuleEndpoint endpoint = endpoints.get(name);
        if (endpoint == null || endpoint.getUrl() == null || endpoint.getUrl().isBlank()) {
            throw new IllegalStateException("No hay endpoint configurado para el módulo: " + name);
        }
        return endpoint;
    }
}

package com.tup.reconac.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "app.modules")
@Getter
@Setter
public class ModulesConfig {

    private Map<String, ModuleEndpoint> endpoints = new HashMap<>();

    public ModuleEndpoint getEndpoint(String name) {
        ModuleEndpoint endpoint = endpoints.get(name);
        if (endpoint == null || endpoint.getUrl() == null || endpoint.getUrl().isBlank()) {
            throw new IllegalStateException("No hay endpoint configurado para el módulo: " + name);
        }
        return endpoint;
    }
}

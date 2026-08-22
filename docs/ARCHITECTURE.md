# ReconAC — Architecture

## Overview

ReconAC es una aplicación compuesta por tres componentes principales:

- `recon_back/` → Backend Java/Spring Boot.
- `recon_front/` → Frontend React.
- `recon_modules/` → Módulos de reconocimiento.

## Principios

- Arquitectura modular.
- Separación de responsabilidades.
- Bajo acoplamiento entre componentes.
- Extensibilidad.
- Priorizar soluciones simples y justificadas.
- No introducir complejidad innecesaria para el alcance del proyecto.

## Backend

El backend utiliza Java y Spring Boot.

Responsabilidades principales:

- API.
- Lógica de negocio.
- Persistencia.
- Autenticación/autorización.
- Coordinación con los módulos de reconocimiento.

## Base de datos

PostgreSQL (Flyway para migraciones): almacenamiento principal.
Redis: caché de tablas de referencia.

### Estrategia de caché

Lectura (Read-Through): backend consulta Redis primero.
En miss, consulta PostgreSQL y almacena en Redis con TTL de 24h.

Escritura (Write-Around): datos nuevos se persisten directamente
en PostgreSQL. Se invalida la key de Redis si existe.

Guard: Cpe.ultimo_check determina si se reconsultan datos externos
(NVD/KEV/EPSS). Si el check es reciente, se reutilizan los datos
almacenados.

### Tablas de referencia (PostgreSQL + Redis)

Cpe, Cve, Cwe y sus junction tables (Cpe_Cve, Cve_Cwe).
Datos compartidos entre bases de datos, alta reutilización.

### Tablas de auditoría (solo PostgreSQL)

Usuario, Auditoria, Activo, Puerto, Puerto_Cpe.
Datos específicos de cada sesión de escaneo.

> DER: `bd_reconac.drawio`

## Reconocimiento

Los módulos de reconocimiento están desarrollados en Python.

Responsabilidades actuales:

- Reconocimiento mediante Nmap.
- Identificación de hosts, puertos y servicios.
- Extracción de CPE.
- Consulta de información de vulnerabilidades.

## Frontend

`recon_front/` contiene la interfaz de usuario y consume la API del backend.

## Regla arquitectónica

Las decisiones que modifiquen esta arquitectura deben ser propuestas y aprobadas por el PO antes de implementarse.
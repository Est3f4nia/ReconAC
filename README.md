# ReconAC

Aplicación de auditoría y reconocimiento de activos con análisis de vulnerabilidades.

- `recon_back`: API Spring Boot, PostgreSQL, Redis, JWT y CSRF.
- `recon_modules`: motor Flask/Python, Nmap y generación de reportes.
- `recon_front`: interfaz React, TypeScript y Vite.

## API y Docker

Consultar [DEPLOYMENT.md](DEPLOYMENT.md) para configurar el entorno, iniciar los cinco servicios, acceder a Swagger y entender los requisitos de Nmap.

```sh
cp .env.example .env
# Completar DB_PASSWD, JWT_SECRET y NVD_ENCRYPTION_KEY en .env
docker compose config --quiet
docker compose up --build
```

Frontend: http://localhost:3000. Swagger: http://localhost:3000/swagger-ui/index.html.

Resultados de pruebas, inventario de cambios y limitaciones: [VERIFY.md](VERIFY.md).

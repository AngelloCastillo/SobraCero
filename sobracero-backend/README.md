# SobraCero – Backend

Spring Boot 4 · Java 21 · PostgreSQL · Flyway · JWT · Swagger. Sin Docker.

> Los nombres del dominio están en español (`Oferta`, `Reserva`, `Comercio`, `Donacion`, `/api/ofertas`, tablas y columnas
> en español). Lo técnico de Spring sigue en inglés (`Controller`, `Service`, `Repository`).

Plataforma con canal dual: los comercios venden excedentes con descuento y, si nadie los reserva,
se derivan automáticamente a una organización social (donación).

## Qué incluye

| Módulo | Historias de usuario |
|---|---|
| Autenticación JWT y roles (`CONSUMER`, `MERCHANT`, `NGO`, `ADMIN`) | US18 |
| Ofertas con ventana de recojo y búsqueda geolocalizada (Haversine) | US02, US03, US06, US07 |
| Reservas, pago (sandbox), código de reserva y canje | US08, US09 |
| Recordatorios y reservas no recogidas (tarea programada) | US10 |
| Derivación automática a donación, avisos, aceptar/rechazar, confirmar con código | US04, US11, US12, US14 |
| Registro de donaciones recibidas (Ley 30498) | US13 |
| Validación de cuentas y calificación SUNAT | US18, US19 |
| Impacto (comercio, organización, plataforma), reporte CSV y distintivo | US05, US16, US17, US20 |
| Predicción de excedentes por categoría (plan Premium) | US01, US15 |
| Notificaciones in-app | US10, US11 |

## 1. Requisitos

- JDK 21 o superior (IntelliJ puede descargarlo: *File → Project Structure → SDK → Download JDK*).
- PostgreSQL con pgAdmin, **o** nada: ver la opción “sin Postgres” abajo.

## 2. Correr en local (IntelliJ + tu PostgreSQL)

1. En pgAdmin crea la base: `CREATE DATABASE sobracero;`
   **Si ya tenías una base `sobracero` de una versión anterior, bórrala y créala vacía de nuevo**: el esquema cambió a
   nombres en español y Flyway lo arma desde cero.
2. En `src/main/resources/application-dev.yml` pon tu contraseña de Postgres.
3. *File → Open* → carpeta del proyecto. Activa **Annotation Processing** (Lombok).
4. Ejecuta `SobraceroBackendApplication`. Flyway crea las tablas y el perfil `dev` carga datos de demostración.
5. Swagger: http://localhost:8080/swagger-ui.html · Salud: http://localhost:8080/actuator/health

### Sin instalar Postgres
Ejecuta la clase de pruebas `LocalEmbeddedApplication` (clic derecho → Run). Levanta un PostgreSQL embebido
y la API con datos demo. Los datos se pierden al detenerla.

## 3. Cuentas de demostración (perfil dev)

| Rol | Correo | Contraseña |
|---|---|---|
| Administrador | `admin@sobracero.pe` | `Admin1234` |
| Consumidor | `consumidor@demo.pe` | `Demo1234` |
| Comercio (Premium, con historial) | `comercio1@demo.pe` | `Demo1234` |
| Organización (validada, SUNAT) | `ong1@demo.pe` | `Demo1234` |

### Usar Swagger con login
1. Abre `/swagger-ui.html` (local: http://localhost:8080/swagger-ui.html; desplegado: `https://TU-BACKEND/swagger-ui.html`).
2. Despliega **Autenticación → POST /api/autenticacion/login** → *Try it out* → escribe `correo` y `clave` → *Execute*.
3. Copia el valor de `token` de la respuesta (sin comillas).
4. Pulsa el botón verde **Authorize** (arriba a la derecha), pega el token y confirma. Ya puedes probar los endpoints protegidos.
5. El token dura 12 horas; después repite el login.

## 4. Pruebas

```bash
./mvnw test
```
Las pruebas de integración usan un PostgreSQL embebido (sin Docker) y recorren el flujo completo:
registro → validación → publicación → búsqueda → pago rechazado → reserva → canje → derivación a donación →
rechazo y redirección → pérdida no evitada → recordatorios → impacto → plan Premium.

## 5. Desplegar (Railway, sin Docker)

1. Sube el proyecto a GitHub (el `.gitignore` excluye `target/` y `.idea/`).
2. En https://railway.com: **New Project → Deploy from GitHub repo**, y luego **+ New → Database → PostgreSQL**.
3. En el servicio de la app → **Variables**:

   | Variable | Valor |
   |---|---|
   | `SPRING_PROFILES_ACTIVE` | `prod` |
   | `DB_URL` | `jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}` |
   | `DB_USER` | `${{Postgres.PGUSER}}` |
   | `DB_PASSWORD` | `${{Postgres.PGPASSWORD}}` |
   | `JWT_SECRET` | una cadena larga aleatoria (mínimo 32 caracteres) |
   | `ADMIN_PASSWORD` | contraseña del administrador |
   | `CORS_ALLOWED_ORIGINS` | URL del frontend (ej. `https://sobracero.netlify.app`) |
   | `SEED_DEMO` | `true` para cargar datos de demostración (útil para la presentación) |

4. **Settings → Networking → Generate Domain**. Prueba `https://TU-DOMINIO/actuator/health`.
5. Si no compila por la versión de Java, agrega `NIXPACKS_JDK_VERSION=21` (o `RAILPACK_JDK_VERSION=21`).

### Alternativa: Azure App Service
Azure Database for PostgreSQL (Flexible Server) + App Service (Java 21, Java SE) con las mismas variables
(`DB_URL` con `?sslmode=require`). Sube `target/*.jar` con `./mvnw -DskipTests package`.

## 6. Configuración útil (`application.yml`)

- `app.donacion.minutos-previos`: minutos antes del cierre para derivar a donación (15).
- `app.recordatorio.minutos-antes-cierre`: recordatorio al consumidor (30).
- `app.impacto.distintivo-min-kg`, `app.impacto.co2-kg-por-kg`: distintivo y factor de CO₂.
- `app.tareas.intervalo-ms`: frecuencia de la tarea programada (60 000 ms).

## 7. Pendientes conocidos

- Pasarela de pago real (hoy `SandboxPaymentGateway`; la interfaz `PaymentGateway` está lista para Culqi/Niubiz).
- Notificaciones push (hoy in-app) y reporte en PDF (hoy CSV).
- Modelo de predicción con ML (hoy promedio por día de la semana).
- Migrar la búsqueda a PostGIS si el volumen lo requiere.

## 8. Endpoints principales

| Área | Endpoints |
|---|---|
| Autenticación | `POST /api/autenticacion/registro`, `POST /api/autenticacion/login`, `GET /api/autenticacion/yo` |
| Ofertas | `GET /api/ofertas/cercanas` (público), `GET /api/ofertas/{id}` (público), `POST /api/ofertas`, `GET /api/ofertas/mias` |
| Reservas | `POST /api/reservas`, `GET /api/reservas/mias`, `POST /api/reservas/canjear` |
| Donaciones | `GET /api/donaciones/mias`, `POST /api/donaciones/{id}/aceptar`, `POST /api/donaciones/{id}/rechazar`, `POST /api/donaciones/completar`, `GET /api/donaciones/registro`, `GET /api/donaciones/comercio` |
| Comercios | `GET /api/comercios/yo`, `GET /api/comercios`, `PATCH /api/comercios/{id}/validar`, `/rechazar`, `/plan` |
| Organizaciones | `GET /api/organizaciones/yo`, `PATCH /api/organizaciones/yo/capacidad`, `PATCH /api/organizaciones/{id}/validar`, `/rechazar` |
| Impacto | `GET /api/impacto/comercio`, `GET /api/impacto/comercio/reporte.csv`, `GET /api/impacto/organizacion`, `GET /api/impacto/plataforma` (público) |
| Predicción y avisos | `GET /api/prediccion/comercio`, `GET /api/notificaciones`, `POST /api/notificaciones/leer-todas` |

Roles: `COMERCIO`, `CONSUMIDOR`, `ORGANIZACION`, `ADMINISTRADOR`. Planes: `GRATIS`, `PREMIUM`.
Métodos de pago de prueba: `YAPE`, `PLIN`, `TARJETA`, `TARJETA_RECHAZADA` (simula un pago rechazado).

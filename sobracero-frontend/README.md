# SobraCero – Frontend (Angular 22)

Consume la API del backend Spring Boot (`sobracero-backend`). Login con JWT, rutas protegidas por rol,
mapa con Leaflet y notificaciones.

## Abrirlo desde la terminal (VS Code)

1. Instala Node.js 20+ (https://nodejs.org).
2. Levanta el backend (http://localhost:8080). Ver el README del backend.
3. En la terminal de VS Code, dentro de esta carpeta:
   ```bash
   npm install
   npm start
   ```
4. Se abre solo en http://localhost:4200 (si no, ábrelo a mano).

La URL del backend se lee de `public/config.js`.

## Cuentas de demostración (con el backend en perfil dev)

Contraseña `Demo1234` (administrador: `admin@sobracero.pe` / `Admin1234`).
`consumidor@demo.pe` · `comercio1@demo.pe` (Premium) · `ong1@demo.pe`

## Pantallas

- **Ofertas** (público): mapa y lista por cercanía, filtros, reserva y pago (modo prueba).
- **Mis reservas** (consumidor): códigos de recojo y estados.
- **Mi comercio**: impacto y distintivo, publicar ofertas, canjear códigos, donaciones, predicción y reporte CSV.
- **Donaciones** (organización): aceptar/rechazar, confirmar recojo con código, registro Ley 30498, capacidad diaria.
- **Administración**: impacto de la plataforma, validar/rechazar cuentas, plan Premium, calificación SUNAT.

## Desplegar (Netlify)

1. Pon la URL pública del backend en `public/config.js` (sin `/` al final).
2. `npm run build` → salida en `dist/sobracero-frontend/browser`.
3. Arrastra esa carpeta a https://app.netlify.com/drop. Ya incluye `_redirects` para que funcionen las rutas.
   (Con GitHub: comando `npm run build`, carpeta de publicación `dist/sobracero-frontend/browser`.)
4. En el backend (Railway) define `CORS_ALLOWED_ORIGINS` con la URL de Netlify.

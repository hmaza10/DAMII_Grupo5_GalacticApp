# GalacticAPP - Portal administrativo

Frontend Angular del portal administrativo de GalacticAPP, proyecto del curso
Desarrollo de Aplicaciones Móviles II - Grupo 5.

## Requisitos

- Node.js 22 LTS o superior
- npm
- Angular 21
- Backend GalacticAPP en `http://localhost:8080`
- MySQL 8 o MariaDB de XAMPP iniciado

## Instalación

```bash
npm install
```

## Ejecutar en desarrollo

```bash
npm start
```

Abrir:

```text
http://localhost:4200
```

El backend debe estar corriendo en:

```text
http://localhost:8080/api
```

## Usuarios de prueba

Administrador del portal:

- Usuario: `admin`
- Clave: `admin123`

Cliente de prueba:

- Usuario: `cliente`
- Clave: `cliente123`

El cliente sirve solo para comprobar que no puede acceder al portal administrativo.

## Compilar

```bash
npm run build
```

La salida se genera en `dist/frontend-angular`.

## Rutas del portal

- `/login`
- `/dashboard`
- `/categorias`
- `/platos`
- `/menus`
- `/clientes`
- `/pedidos`
- `/zonas`

## Notas

- El portal usa autenticación Basic Auth contra la API.
- Solo el rol administrador puede acceder.
- El módulo de reportes y FCM quedan fuera del alcance inmediato.
- No commitear `node_modules`, `dist` ni `.angular`.
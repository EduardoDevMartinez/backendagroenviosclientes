# AgroEnvíos - Backend Clientes

Backend para la aplicación de clientes de AgroEnvíos.

## Características

- Autenticación JWT
- Catálogo de productos
- Gestión de carrito
- Órdenes y entregas
- Historial de compras
- Perfil de usuario

## Puertos

- Backend Clientes: puerto 8081
- Backend Proveedores: puerto 8080

## Base de datos

Usa MySQL con base de datos `agroenvios_clientes`

## Endpoints principales

### Autenticación
- POST /auth/login
- POST /auth/register
- POST /auth/logout

### Productos
- GET /products
- GET /products/search
- GET /products/{id}
- GET /categories

### Carrito
- GET /cart
- POST /cart/add
- PUT /cart/update/{itemId}
- DELETE /cart/remove/{itemId}

### Órdenes
- POST /orders/create
- GET /orders
- GET /orders/{id}
- PUT /orders/{id}/cancel

### Usuario
- GET /user/profile
- PUT /user/profile
- POST /user/change-password

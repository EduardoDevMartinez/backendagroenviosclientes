---
name: project-push-notifications
description: Feature pendiente de implementar — notificaciones push Expo para cambios de estado de pedido en el backend de clientes Agroenvios
metadata:
  type: project
---

## Feature: Notificaciones Push (Expo) — IMPLEMENTADO (2026-06-10)

**Why:** La app móvil de clientes usa `expo-notifications`. El backend debe guardar el Expo Push Token por usuario y enviar notificaciones cuando cambia el estado de un pedido.

**How to apply:** Al retomar esta tarea, implementar los siguientes cambios.

---

### Cambios requeridos

**1. `User.java` — agregar campo:**
```java
private String pushToken;
```

**2. Crear `ExpoPushNotificationService.java`** en `primary/service/`:
- Método `@Async sendPedidoNotification(User user, String estado, Long pedidoId)`
- POST a `https://exp.host/--/api/v2/push/send`
- Si respuesta contiene `"DeviceNotRegistered"` → limpiar pushToken del usuario en BD
- Try/catch silencioso para no bloquear el flujo principal
- Mensajes por estado:
  - APROBADO → title: "Pedido confirmado", body: "Tu pedido #<id> fue aprobado y está en preparación"
  - RECHAZADO → title: "Pedido rechazado", body: "Tu pedido #<id> no pudo ser procesado"
  - CANCELADO → title: "Pedido cancelado", body: "Tu pedido #<id> fue cancelado"
- Siempre incluir `"data": { "pedidoId": <id> }` en el payload

**3. Crear `PushTokenRequest.java`** en `primary/dto/user/`:
```java
@Data public class PushTokenRequest { @NotBlank private String token; }
```

**4. `UserService.java` — agregar métodos:**
- `savePushToken(String token)` — sobreescribe pushToken del usuario autenticado
- `deletePushToken()` — pone pushToken = null

**5. `UserController.java` — agregar endpoints:**
- `POST /user/push-token` → Body: `{ "token": "ExponentPushToken[...]" }` → 200 vacío
- `DELETE /user/push-token` → 200 vacío

**6. `PedidoService.java` — inyectar `ExpoPushNotificationService` y llamarlo:**
- En `crearPedidoAprobado()`, después de `pedidoRepository.save(pedido)`:
  `pushNotificationService.sendPedidoNotification(pp.getUser(), "APROBADO", pedido.getId())`
- En `procesarPago()`, bloque `rejected/cancelled`:
  `pushNotificationService.sendPedidoNotification(pp.getUser(), "RECHAZADO", null)`

---

### Notas técnicas
- `RestTemplate` bean ya existe en `ApplicationConfig.java`
- `@EnableAsync` ya está en `ClientesApplication.java`
- Sobreescribir siempre el token (nunca acumular varios por usuario)
- Envío asíncrono con `@Async` para no bloquear el webhook de MercadoPago

# La Sazón de Doña Meche: sistema de restaurante criollo

Es un sistema completo para un restaurante criollo de barrio. El comensal ve el plato del día y pide desde el QR de su mesa. La cocina ve los pedidos en vivo por estación, el mozo confirma y lleva los platos, y la caja cobra con Yape, Plin, tarjeta o efectivo y cierra el día. También acepta pedidos para llevar y delivery, con seguimiento en vivo.

> **Demo:** el restaurante, la carta y los pedidos son ficticios. Los datos se reinician cada madrugada.
> Para entrar al panel usa `cocina@donameche.demo`, `mozo@donameche.demo`, `caja@donameche.demo` o `admin@donameche.demo`. La contraseña de todas es `demo1234`.

Es el segundo de una serie de seis sistemas de restaurante: cevichería, comida criolla, pollería, chifa, cafetería y sanguchería. Todos comparten el mismo núcleo de pedidos, cocina y caja, y cada uno tiene su propia carta, temática y diseño. Este usa un diseño de **letrero de lata rotulado**: el plato del día se anuncia en letras de rotulista sobre esmalte rojo y cada plato es una tablilla esmaltada.

## Qué resuelve

Al mediodía, un restaurante criollo de barrio pierde pedidos entre la comanda de papel y la pizarra del plato del día, que además se acaba antes de las dos. Este sistema resuelve eso así:

- **Plato del día según el día de la semana.** Lunes seco con frejoles, martes ají de gallina, miércoles carapulcra… La carta muestra solo el de hoy, en el letrero rojo de la puerta.
- **Carta por QR en cada mesa.** El comensal arma su pedido, le agrega notas, lo envía y sigue su estado. También puede pedir la cuenta.
- **«Se terminó» en vivo.** Cuando la cocina marca un plato como agotado, una franja azul pintada lo cruza en el celular de todos los comensales y el servidor rechaza los pedidos que lo incluyan.
- **Pantalla de cocina por estación** (fogón, entradas y postres, y bebidas). Muestra el número de pedido en grande y marca con un anillo rojo lo que espera más de 15 minutos.
- **Vista del mozo.** Tiene el mapa de mesas, los pedidos por confirmar, los platos listos para llevar y los pedidos para llevar o delivery.
- **Caja.** Muestra la cuenta por mesa y permite pagar en partes o dividir entre varios. Calcula el vuelto, muestra un QR de Yape o Plin simulado e incluye lo cobrado por hora, los platos más pedidos y el cierre del día.
- **Carta editable**, con disponibilidad en un toque.

## Reglas que se validan en el servidor

- El precio sale de la carta guardada, nunca del navegador.
- No se aceptan platos agotados: la API responde 409 con los IDs exactos.
- La numeración del día no se repite aunque lleguen pedidos al mismo tiempo, gracias a un contador por día con bloqueo de fila.
- No se cobra más que el saldo ni una mesa con un pedido sin confirmar. Un pedido para llevar no se entrega sin estar pagado, y la caja no cierra con mesas abiertas.
- Los avisos en vivo (WebSocket/STOMP) salen después de guardar. El canal del personal exige un JWT.

## Tecnologías

| Parte | Stack |
| --- | --- |
| Frontend | React 19, TypeScript, Vite, TanStack Query, STOMP sobre WebSocket |
| Backend | Java 17, Spring Boot 4, Spring Security (JWT), Spring WebSocket, Spring Data JPA, Flyway |
| Base de datos | PostgreSQL 17 (H2 en modo PostgreSQL para las pruebas) |
| Despliegue | Vercel (frontend), Render con Docker (API) y Neon (PostgreSQL) |

## Correr en local

```bash
docker compose up -d                 # PostgreSQL en el puerto 5435
cd backend && ./mvnw spring-boot:run # API en http://localhost:8083
cd frontend && npm install && npm run dev  # http://localhost:5175
```

Para correr las pruebas: `cd backend && ./mvnw test`. Son 13 pruebas de integración.

## Publicar

1. **Neon:** crea la base de datos.
2. **Render:** crea un Blueprint con `render.yaml` y define estas variables:
   - `DATABASE_URL=jdbc:postgresql://<host>/<base>?sslmode=require`
   - `DATABASE_USER`
   - `DATABASE_PASSWORD`
   - `CORS_ORIGENES`
3. **Vercel:** importa `frontend/` con `VITE_API_URL=https://<tu-api>.onrender.com`.

## Fotos

Las fotos de los platos son de Wikimedia Commons, con licencias CC BY, CC BY-SA o de dominio público. Son de referencia y no son del local. Los créditos están al pie de la carta (`frontend/src/carta/creditos.ts`).

## Autor

David Vega, desarrollador full stack en Lima. [GitHub](https://github.com/DarkCodeKnight25) · [LinkedIn](https://www.linkedin.com/in/dvegaortiz)

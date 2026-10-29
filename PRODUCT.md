# Product

<!-- impeccable:product-schema 1 -->

## Platform

web

## Stack

- Es el mismo stack del proyecto 1 (sistema-citas-dental):
  - **Backend:** Spring Boot 4 (Java 17) y PostgreSQL, con Flyway.
  - **Tiempo real:** WebSocket (STOMP) para la cocina y las mesas.
  - **Frontend:** React + TypeScript (Vite).
- **Demo:** Vercel, Render (Docker) y Neon, en plan gratuito.
- **Repositorio:** `sistema-restaurant-comida-criolla`. Es el segundo de 6 sistemas de restaurante. Reutiliza el núcleo de pedidos, cocina y caja de la cevichería, con su propia carta, temática y diseño.

## Users

- **Comensal:** almuerza en el local, muchas veces solo y con poco tiempo (oficinistas, trabajadores de la zona) o en familia el fin de semana. Escanea el QR de su mesa, mira la carta y el plato del día, arma el pedido y lo envía sin esperar al mozo. Puede pedir más después y pedir la cuenta. No crea una cuenta.
- **Cliente de delivery o para llevar:** pide desde la misma carta, deja su nombre y celular, y la dirección si es delivery.
- **Mozo:** confirma los pedidos que llegan de las mesas, puede tomar pedidos desde su celular, lleva los platos que están listos y atiende las cuentas.
- **Cocina:** trabaja con una pantalla grande en dos estaciones: fogón (guisos, frituras, arroces y el plato del día) y entradas y postres (papa a la huancaína, causa, mazamorra, arroz con leche). Las bebidas las sirve el mozo.
- **Caja o administración:** cobra por mesa, cierra la caja del día, gestiona la carta y marca platos como agotados.
- **Público secundario:** dueños de restaurantes que evalúan contratar a David y reclutadores. Todos ven la demo con datos ficticios.

## Product Purpose

Reemplazar la comanda de papel y la pizarra del «plato del día» en un restaurante criollo de barrio con mucho movimiento al mediodía. El sistema tiene éxito si:
- el comensal pide sin esperar al mozo;
- la cocina ve cada plato al instante y separado por estación;
- ningún pedido se pierde;
- la caja cierra el día con las cuentas cuadradas.

Para David, es una pieza de portafolio que un dueño de restaurante pueda probar en su propio celular.

## Positioning

Está hecho para cómo funciona un restaurante criollo en Lima:
- cada día de la semana tiene su plato del día (lunes frejoles con seco, martes ají de gallina…) y se acaba antes de las dos;
- hay entradas y postres de olla (mazamorra, arroz con leche);
- la chicha y la jarra de refresco son parte del almuerzo;
- se paga con Yape o Plin además de efectivo;
- los precios van en soles.

No es un POS genérico traducido.

## Operating Context

- Cada mesa tiene un QR que abre la carta con la mesa ya elegida.
- **Estados del pedido:** enviado (por confirmar) → confirmado → en preparación → listo → servido.
- **Estados de la mesa:** libre → ocupada → pidió la cuenta → pagada.
- Un pedido tiene varios platos y cada plato va a su estación (fríos o calientes). La cocina avanza cada plato por separado.
- **Cobro:** la caja cobra al final, por mesa, en efectivo (con vuelto), Yape o Plin (QR simulado) o tarjeta. Se puede dividir la cuenta de forma simple.
- **Cierre de caja:** total por medio de pago, número de mesas y tickets, ticket promedio, platos más vendidos y diferencia de efectivo.
- **Delivery y para llevar:** son pedidos sin mesa, con nombre y celular (y dirección y referencia en delivery). Tienen estados propios: recibido → en cocina → listo → en camino o entregado.

## Capabilities and Constraints

- **Carta pública por mesa:** el plato del día según el día de la semana, entradas, platos de fondo, postres y bebidas, fotos, precio en soles, nota por plato («sin cebolla», «bien picante»), platos agotados en vivo, enviar el pedido, ver su estado y pedir la cuenta.
- **Pantalla de cocina en vivo** por estación, con el tiempo que lleva cada pedido y aviso sonoro opcional.
- **Panel del mozo:** mapa de mesas, pedidos por confirmar y platos listos para llevar.
- **Caja:** cuenta por mesa, cobro con medio de pago, cierre del día.
- **Gestión de carta:** crear y editar platos, precios, fotos, estación y disponibilidad (agotado).
- **Validaciones en el servidor:** precios desde la BD (nunca desde el cliente), no aceptar platos agotados, mesa válida, transiciones de estado y permisos por rol.
- Zona horaria America/Lima. Moneda PEN. Interfaz en español.
- **Sin decidir:** boleta o factura electrónica SUNAT (fuera de la primera versión) y la integración real con Yape (se simula).

## Brand Commitments

- **La Sazón de Doña Meche** es un restaurante criollo **ficticio** de barrio en Lima.
- El sistema lo firma David Vega (GitHub: DarkCodeKnight25). Los commits van solo a su nombre.
- Esta temática y su diseño deben ser **distintos** de la cevichería «El Muelle Norteño» (carteles de cartón a plumón), de los demás restaurantes de la serie y del proyecto de citas dental.

## Evidence on Hand

- No hay clientes, reseñas ni cifras reales. No se inventan testimonios ni «la mejor sazón de Lima».
- Las fotos de los platos tienen que ser reales y con licencia libre (con su crédito) o hechas a propósito. No pueden presentarse como del local.
- Las ventas, las mesas y los pedidos de la demo son ficticios y deben verse así.

## Product Principles

1. **El pedido nunca se pierde:** cada paso queda confirmado y visible para el comensal, el mozo y la cocina.
2. **La cocina manda en hora punta:** la pantalla de cocina se lee de lejos, de un vistazo y con las manos ocupadas.
3. **Lo agotado se ve al instante:** nadie pide un plato que ya no hay.
4. **Pedir es más fácil que llamar al mozo:** la carta se usa con una mano, en un celular de gama baja y con poca luz.
5. **Las cuentas cuadran:** el cierre de caja suma desde los pedidos y los pagos, nunca con números escritos a mano.

## Accessibility & Inclusion

- Contraste WCAG AA. La carta tiene que leerse con luz de fluorescente o en la vereda y la cocina, a 2 metros.
- Objetivos táctiles de 44 px o más. La carta funciona en pantallas de 360 px.
- Comensales de cualquier edad: textos claros, precios grandes y nada de jerga técnica.

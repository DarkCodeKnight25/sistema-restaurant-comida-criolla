---
name: La Sazón de Doña Meche
description: Carta QR, cocina en vivo, mozo y caja de un restaurante criollo de barrio, vestidos como un letrero de lata rotulado.
colors:
  esmalte: "#F6EBD3"
  lata: "#FFFAF0"
  tinta: "#1A1A1A"
  tinta-suave: "#4B4438"
  tinta-tenue: "#6B6252"
  linea: "#DCCDAA"
  rojo: "#C8102E"
  rojo-oscuro: "#9A0C23"
  amarillo: "#F2B705"
  amarillo-oscuro: "#9C7400"
  azul: "#14406B"
  ok: "#2E7D32"
  alerta: "#A35A00"
  peligro: "#B3261E"
  estacion-fogon: "#C8102E"
  estacion-entradas: "#14406B"
  estacion-bebidas: "#1A1A1A"
typography:
  rotulo-letrero:
    fontFamily: "'Bungee', 'Work Sans', sans-serif"
    fontSize: "clamp(34px, 6vw, 56px)"
    fontWeight: 400
    lineHeight: 1.05
  rotulo-precio:
    fontFamily: "'Bungee', 'Work Sans', sans-serif"
    fontSize: "20px"
    fontWeight: 400
  rotulo-titulo:
    fontFamily: "'Bungee', 'Work Sans', sans-serif"
    fontSize: "24px"
    fontWeight: 400
  texto:
    fontFamily: "'Work Sans', system-ui, sans-serif"
    fontSize: "15px"
    fontWeight: 400
    lineHeight: 1.45
  marca:
    fontFamily: "'Work Sans', system-ui, sans-serif"
    fontSize: "18px"
    fontWeight: 700
radius:
  lata: "3px"
  control: "4px"
---

# La Sazón de Doña Meche: letrero de lata rotulado

## Idea

Es la puerta de un restaurante criollo de barrio en Lima, donde un letrero de lata rojo, pintado a mano por un rotulista, anuncia el plato del día. Cada plato de la carta es una tablilla esmaltada colgada con remaches. El panel del personal es una hoja crema dentro de un marco de esmalte rojo.

Se diferencia de la cevichería (cartones con plumón sobre hielo): aquí no hay escritura a mano, solo letras de rotulista, esmalte plano y remaches.

## Color

- **Esmalte crema (#F6EBD3)** es el fondo de todo, tanto en la carta como en la hoja del panel.
- **Rojo de rótulo (#C8102E)** se usa en el letrero del día, la barra «Tu pedido», las acciones principales y el marco del panel.
- **Amarillo (#F2B705)** se usa solo como sombra desplazada de las letras del letrero, en el disco del precio del día, en el contador del pedido y para marcar la pestaña activa.
- **Azul de lata (#14406B)** se usa para lo secundario: la franja «SE TERMINÓ», el estado pendiente en cocina y la estación de entradas.
- **Tinta (#1A1A1A)** se usa para los bordes de las placas y para la estación de bebidas.

No hay degradados ni sombras de caja. La única sombra del sistema es la de rotulista: `text-shadow: 3px 3px 0 amarillo`.

## Tipografía

- **Bungee** es rara a propósito. Solo aparece en el letrero del día, los precios, los títulos de sección y los números grandes: pedido, mesa y cantidad.
- **Work Sans** cubre todo lo demás, incluida la marca del local (en peso 700).

Ambas fuentes se cargan localmente con @fontsource.

## Componentes

- **Letrero del día:** esmalte rojo con doble marco crema y remaches en las esquinas. El título va en Bungee crema con sombra amarilla, precedido de «Hoy:» en amarillo. El precio va en un disco amarillo inclinado. En celular la foto queda debajo; en escritorio, al costado.
- **Tablilla (plato):** placa crema con borde de tinta de 2 px, un filete interior de 1 px y un remache en cada esquina. Lleva miniatura cuadrada, nombre, descripción, precio en Bungee rojo y el botón rojo «+».
- **«SE TERMINÓ»:** franja azul pintada, ligeramente inclinada, con letras Bungee blancas. La placa se atenúa y, si se agota en vivo, la franja entra pintándose.
- **Salto del precio:** al agregar un plato, un clon del precio viaja en 260 ms hasta la barra roja del pedido. No ocurre si el usuario prefiere menos movimiento.
- **Título de sección:** va en Bungee rojo, con doble filete debajo y el nombre de la estación a la derecha.
- **Panel:**
  - El marco es la barra superior roja, con enlaces crema y la pestaña activa subrayada en amarillo.
  - La hoja es crema, con filete doble y esquinas de 3 px.
  - Las tarjetas son placas con filete doble de tinta.
- **Cocina:** cada estación lleva una cabecera con doble filete de su color. El estado se lee por la forma de la línea del botón: «Empezar» con línea discontinua azul, «Listo» en tinta sólida y un borde rojo en los pedidos con más de 15 minutos de espera.
- **Mesas:** una mesa libre tiene borde discontinuo; una ocupada, filete doble de tinta. Un pedido por confirmar se marca en rojo y los platos listos en azul.

## Movimiento

Las transiciones duran 160–260 ms con `cubic-bezier(0.22, 1, 0.36, 1)`. Todo se desactiva con `prefers-reduced-motion`.

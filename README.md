# Video Blocks (Minecraft Forge 1.12.2)

Copyright (c) 2026 NotXoot. Todos los derechos reservados.

Reproduce videos dentro del mundo usando **un bloque por píxel**, con resolución ajustable
mientras el video está corriendo.

## Requisitos
- Minecraft 1.12.2 + Forge 14.23.5.2860
- **ffmpeg** instalado (en el PATH o con la ruta en `config/videoblocks.cfg`)
- Permisos de operador (nivel 2). En un mundo de un jugador: crear el mundo con trucos activados.
- Es un mod **de servidor**: en multijugador solo hay que instalarlo en el servidor
  (los clientes vanilla pueden entrar). En un solo jugador, instalarlo normalmente.

## Compilar
1. Descarga el MDK oficial de Forge 1.12.2 (14.23.5.2860) y ábrelo con JDK 8.
2. Reemplaza su `src/` por el de este proyecto y copia este `build.gradle`
   (o conserva el del MDK y solo cambia `version`, `group` y `archivesBaseName`).
3. `gradlew build` → el `.jar` queda en `build/libs/`.

## Uso
Pon tus videos en la carpeta `videos/` (junto a `mods/`) y, dentro del juego:

| Comando | Qué hace |
|---|---|
| `/vblocks create <ancho> <alto> [wall\|floor]` | Crea la pantalla frente a ti. `wall` = pared vertical, `floor` = plana en el suelo |
| `/vblocks play <archivo\|url> [fps 1-20] [loop]` | Reproduce un video (por defecto 10 fps) |
| `/vblocks resolution <ancho> <alto>` | Cambia la resolución en caliente; el video sigue donde iba |
| `/vblocks pause` · `resume` · `stop` | Control de reproducción |
| `/vblocks fps <1-20>` | Cambia los fps |
| `/vblocks fit <on\|off>` | `on` respeta la proporción (barras negras), `off` estira |
| `/vblocks dither <on\|off>` | Dithering: más colores aparentes, más bloques cambiando por frame |
| `/vblocks gain <0.25-4\|auto>` | Brillo previo (compensa el sombreado de caras de los bloques) |
| `/vblocks remove` | Borra la pantalla y sus bloques |
| `/vblocks info` | Estado actual |

Alias: `/videoblocks`. Ejemplo: `/vblocks create 128 72 floor` y luego `/vblocks play mi_video.mp4 10 loop`.

## Configuración (`config/videoblocks.cfg`)
- `ffmpegPath`: ruta de ffmpeg.
- `maxSide`: ancho/alto máximo de una pantalla (256 por defecto).
- `blocksPerTick`: tope de bloques cambiados por tick (5000). Bájalo si hay lag.
- `palette`: `concrete`, `wool`, `terracotta` o `all`, combinables con comas.

## Notas
- Las pantallas tipo `floor` se ven con el color más fiel (la cara superior no se oscurece);
  en `wall` las caras laterales se ven más oscuras y el mod lo compensa con `gain auto`.
- Altura máxima del mundo: 255; lo que se salga se recorta.
- No incluye audio. Los bloques que había antes en la zona se reemplazan y, al borrar, quedan como aire.
- Una pantalla activa por servidor.

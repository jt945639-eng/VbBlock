# Video Blocks (Minecraft Forge 1.12.2)

Copyright (c) 2026 NotXoot. Todos los derechos reservados.

Reproduce videos dentro del mundo usando **un bloque por píxel**, con la resolución que quieras.
Eliges el video (archivo de tu PC o URL), el mod te da una **lámpara de Redstone** especial, y al
colocarla en el suelo **genera la pantalla** y empieza a reproducir. Cualquier señal de Redstone
activa y detiene el video, y un **menú** guarda todas las lámparas.

## Requisitos
- Minecraft 1.12.2 + Forge 14.23.5.2860 (el mod va en cliente **y** servidor)
- **ffmpeg** instalado para videos mp4/webm/etc. (en el PATH o con la ruta en `config/videoblocks.cfg`). **No hace falta para GIFs**: se leen con Java puro (ideal para el teléfono / PojavLauncher)
- Ser operador (o el dueño del mundo en un solo jugador / LAN). Configurable con `requireOp`.

## Cómo se usa
1. Pulsa **V** (tecla configurable en Controles → Video Blocks) para abrir el menú de lámparas.
2. **Nueva lámpara** → elige el video con **Examinar...** (archivo de tu PC) o pega una **URL** (http/https).
   El editor tiene dos pestañas:
   - **Video:** archivo/URL, nombre, **resolución** (ancho × alto en bloques, con atajos 64×36, 128×72, 192×108 y 256×144),
     **FPS** (1-20), bucle y ajuste de imagen (respetar proporción o estirar).
   - **Opciones y bloques:** modo de Redstone, qué hacer al detener, dithering y el botón **Bloques**: abre la lista de los
     **48 bloques** (16 de concrete, 16 de lana y 16 de terracota), cada uno con su interruptor. Los desactivados **no se generan**
     en el video (mínimo uno activo). Más bloques = más colores y una imagen más fiel.
3. **Crear lámpara**: el video queda marcado y la lámpara llega a tu inventario.
4. **Colócala en el suelo** mirando hacia donde quieres la pantalla: la pantalla se genera **acostada en el suelo**
   (horizontal, nunca de pie), a la altura de la lámpara y justo delante de ella, con la parte de arriba del video
   hacia donde miras, y el video empieza a reproducirse.
5. **Redstone** (palanca, botón, comparador, lo que sea):
   - *Cada pulso alterna* (por defecto): cada señal nueva detiene el video o lo reproduce de nuevo desde el inicio.
   - *Mientras haya señal*: reproduce con señal y se detiene sin ella.
   - **Nunca queda una pantalla negra:** cuando la lámpara está detenida (o se coloca sin señal en modo "mientras haya señal")
     la pantalla muestra el **primer fotograma del video** hecho con bloques. En el editor puedes elegir en su lugar
     *dejar la imagen congelada* en el fotograma donde se detuvo.
6. El **menú** guarda todas las lámparas del mundo: iniciar/detener (`>` / `||`), **Editar** (abre el mismo editor de arriba
   con los ajustes de esa lámpara), **Ir** (te teletransporta), **Dar** (copia de la lámpara con su video) y **X** (olvidar y
   borrar la pantalla). Los cambios se aplican **en caliente**, sin quitar la lámpara:
   - cambiar la **resolución** borra la pantalla vieja y dibuja la nueva en el mismo sitio, y el video sigue por donde iba;
   - cambiar los **FPS** reinicia la decodificación en el mismo segundo;
   - cambiar los **bloques** redibuja la imagen encima con la paleta nueva, sin borrarla y sin detener el video.

### GIFs y teléfono (Android / PojavLauncher)
En el teléfono no existe ffmpeg, así que usa **GIFs** (archivo o URL): el mod los lee solo con Java y funcionan igual
(bucle, FPS, resolución, primer fotograma al detener). Convierte tu video en https://ezgif.com (video a GIF) con unos
160×90 – 256×144 píxeles y ~10 fps. Empieza con resolución 64×36 y baja `blocksPerTick` si el juego va lento.

### Videos del PC, URL y servidores
- **Un jugador / LAN:** cualquier archivo de tu PC funciona.
- **URL:** funciona siempre (enlaces directos a `.mp4`, `.webm`, etc.; no páginas de YouTube).
- **Servidor dedicado:** el servidor es quien lee el archivo, así que usa una URL o archivos que estén en la carpeta
  `videos/` del servidor (el selector del PC solo sirve si el servidor corre en tu mismo PC).

## Comandos (opcionales)
| Comando | Qué hace |
|---|---|
| `/vblocks menu` | Abre el menú |
| `/vblocks lamp <archivo\|url>` | Te da una lámpara con ese video (ajustes por defecto) |
| `/vblocks create <ancho> <alto> [floor\|wall]` (por defecto `floor`, acostada en el suelo) + `play`, `resolution`, `pause`, `resume`, `stop`, `fps`, `fit`, `dither`, `gain`, `remove`, `info` | Una pantalla manual suelta, útil para pruebas |

## Configuración (`config/videoblocks.cfg`)
- `ffmpegPath`: ruta de ffmpeg.
- `maxSide`: ancho/alto máximo de una pantalla (256 por defecto).
- `blocksPerTick`: tope de bloques cambiados por tick entre todas las pantallas (5000). Bájalo si hay lag.
- `palette`: bloques de la pantalla manual de `/vblocks create` (`concrete`, `wool`, `terracotta` o `all`). Las lámparas eligen los suyos en el menú.
- `requireOp`: si es `true`, solo operadores (y el dueño del mundo en un jugador/LAN) usan lámparas y menú.

## Bloques (todo el video es de bloques de supervivencia 1.12.2)
Cada píxel del video es un **bloque real** del mundo (nada de mapas, entidades ni texturas). Solo se usan bloques completos
de vanilla 1.12.2 que se consiguen en modo supervivencia, y el código rechaza cualquier otro:

| Bloque | Cómo se consigue en supervivencia |
|---|---|
| **Concrete** (16 colores) | polvo de concrete (4 arena + 4 grava + 1 tinte) puesto en agua |
| **Lana** (16 colores) | ovejas, o 4 hilos; se tiñe con tinte |
| **Terracota** (16 colores, opcional en `palette`) | arcilla cocida en horno + 8 terracota y 1 tinte |

Las barras negras (modo "respetar proporción") y el primer fotograma también son bloques (concrete negro). La pantalla
la genera el mod sin gastar bloques de tu inventario.

## Notas
- La pantalla reemplaza los bloques de su capa; al olvidar/romper la lámpara queda aire. Mejor sobre terreno plano.
- Romper el bloque de la lámpara borra su pantalla y la quita del menú. Si el chunk se descarga, la pantalla se
  suspende y se reanuda al volver.
- Las pantallas en el suelo muestran los colores más fieles (la cara superior no se oscurece).
- No incluye audio.

## Compilar
**Con GitHub (recomendado):** al subir el proyecto, el workflow `.github/workflows/build.yml` corre las pruebas del núcleo y
compila el mod con el **MDK oficial de Forge 1.12.2-14.23.5.2860**: lo descarga, copia dentro el `src/` de este repo y usa
el `gradlew` del propio MDK (con JDK 8). El `.jar` queda en *Actions → Build → Artifacts*. Si creas una *tag* `v1.0.0`
(`git tag v1.0.0 && git push --tags`) también se publica como *Release*.

**En tu PC:** descarga el MDK de Forge 1.12.2-14.23.5.2860, reemplaza su carpeta `src/` por la de este repo, pon en su
`build.gradle` `group = "com.videoblocks"` y `archivesBaseName = "VideoBlocks-1.12.2"`, y corre `gradlew build` con JDK 8.
El `.jar` queda en `build/libs/`.

**Pruebas del núcleo (sin Minecraft):** `bash tools/smoke-test.sh` (necesita JDK y ffmpeg).

## Subir a GitHub
```bash
git init
git add .
git commit -m "Video Blocks 1.0.0"
git branch -M main
git remote add origin https://github.com/<tu-usuario>/<tu-repo>.git
git push -u origin main
```

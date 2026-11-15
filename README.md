# Video Blocks (Minecraft Forge 1.12.2)

Copyright (c) 2026 NotXoot. Todos los derechos reservados.

Reproduce videos dentro del mundo usando **un bloque por píxel**, con la resolución que quieras.
Eliges el video (archivo de tu PC o URL), el mod te da una **lámpara de Redstone** especial, y al
colocarla en el suelo **genera la pantalla** y empieza a reproducir. Cualquier señal de Redstone
activa y detiene el video, y un **menú** guarda todas las lámparas.

## Requisitos
- Minecraft 1.12.2 + Forge 14.23.5.2860 (el mod va en cliente **y** servidor)
- **ffmpeg** instalado (en el PATH o con la ruta en `config/videoblocks.cfg`)
- Ser operador (o el dueño del mundo en un solo jugador / LAN). Configurable con `requireOp`.

## Cómo se usa
1. Pulsa **V** (tecla configurable en Controles → Video Blocks) para abrir el menú de lámparas.
2. **Nueva lámpara** → elige el video con **Examinar...** (archivo de tu PC) o pega una **URL** (http/https),
   y ajusta nombre, resolución (ancho × alto en bloques), FPS, bucle, ajuste de imagen y modo de Redstone.
3. **Crear lámpara**: el video queda marcado y la lámpara llega a tu inventario.
4. **Colócala en el suelo** mirando hacia donde quieres la pantalla: se genera una pantalla plana,
   en la capa de la lámpara y justo delante de ella, y el video empieza a reproducirse.
5. **Redstone** (palanca, botón, comparador, lo que sea):
   - *Cada pulso alterna* (por defecto): cada señal nueva detiene el video o lo reproduce de nuevo desde el inicio.
   - *Mientras haya señal*: reproduce con señal y se detiene sin ella.
   - **Nunca queda una pantalla negra:** cuando la lámpara está detenida (o se coloca sin señal en modo "mientras haya señal")
     la pantalla muestra el **primer fotograma del video** hecho con bloques. En el editor puedes elegir en su lugar
     *dejar la imagen congelada* en el fotograma donde se detuvo.
6. El **menú** guarda todas las lámparas del mundo: iniciar/detener (`>` / `||`), **Editar** (resolución en caliente,
   video, modo...), **Ir** (te teletransporta), **Dar** (copia de la lámpara con su video) y **X** (olvidar y borrar la pantalla).

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
| `/vblocks create <ancho> <alto> [wall\|floor]` + `play`, `resolution`, `pause`, `resume`, `stop`, `fps`, `fit`, `dither`, `gain`, `remove`, `info` | Una pantalla manual suelta, útil para pruebas |

## Configuración (`config/videoblocks.cfg`)
- `ffmpegPath`: ruta de ffmpeg.
- `maxSide`: ancho/alto máximo de una pantalla (256 por defecto).
- `blocksPerTick`: tope de bloques cambiados por tick entre todas las pantallas (5000). Bájalo si hay lag.
- `palette`: `concrete`, `wool`, `terracotta` o `all`, combinables con comas.
- `requireOp`: si es `true`, solo operadores (y el dueño del mundo en un jugador/LAN) usan lámparas y menú.

## Bloques (todos de supervivencia)
La pantalla solo usa bloques que se consiguen en modo supervivencia: **concrete** (polvo de concrete + agua), **lana**
(ovejas y tinte) y, si lo activas en `palette`, **terracota** (arcilla cocida y tinte). No se usan bloques exclusivos de
creativo. Es una pantalla decorativa: el mod la genera sin gastar bloques de tu inventario.

## Notas
- La pantalla reemplaza los bloques de su capa; al olvidar/romper la lámpara queda aire. Mejor sobre terreno plano.
- Romper el bloque de la lámpara borra su pantalla y la quita del menú. Si el chunk se descarga, la pantalla se
  suspende y se reanuda al volver.
- Las pantallas en el suelo muestran los colores más fieles (la cara superior no se oscurece).
- No incluye audio.

## Compilar
**Con GitHub (recomendado):** al subir el proyecto, el workflow `.github/workflows/build.yml` corre las pruebas del núcleo
y compila el mod con Gradle 4.9 + JDK 8. El `.jar` queda en *Actions → Build → Artifacts*. Si creas una *tag* `v1.0.0`
(`git tag v1.0.0 && git push --tags`) también se publica como *Release*.

**En tu PC:** JDK 8 + Gradle 4.9 (o el `gradlew` del MDK oficial de Forge 1.12.2) → `gradle setupDecompWorkspace build`;
el `.jar` queda en `build/libs/`.

**Pruebas del núcleo (sin Minecraft):** `./tools/smoke-test.sh` (necesita JDK y ffmpeg).

## Subir a GitHub
```bash
git init
git add .
git commit -m "Video Blocks 1.0.0"
git branch -M main
git remote add origin https://github.com/<tu-usuario>/<tu-repo>.git
git push -u origin main
```

# Naciones en Guerra — mod para Minecraft 1.20.1 (Forge)

## Cómo compilarlo (Windows)

1. Instala **Java 17** (JDK). Por ejemplo "Eclipse Temurin 17" desde adoptium.net.
2. Descomprime esta carpeta donde quieras (por ejemplo en el Escritorio).
3. Abre la carpeta `guerramod`, haz clic en la barra de direcciones, escribe `cmd` y pulsa Enter.
4. En la ventana negra escribe:
   ```
   gradlew build
   ```
   La primera vez tarda bastante (descarga Minecraft y Forge, 10–20 minutos).
5. El mod queda en `build\libs\guerramod-1.0.0.jar`.
6. Copia ese `.jar` a la carpeta `mods` de tu Minecraft con **Forge 1.20.1** instalado
   (`%appdata%\.minecraft\mods`).

Para probarlo sin instalar nada: `gradlew runClient` abre un Minecraft de pruebas con el mod.

Si al compilar sale algún error, copia el mensaje y pásamelo para arreglarlo.

## Cómo se juega

### Países y fronteras
| Comando | Qué hace |
|---|---|
| `/pais crear <color> <nombre>` | Funda un país (eres fundador y presidente) |
| `/pais reclamar [radio]` | Reclama el chunk donde estás (radio 0–5 para un área) |
| `/pais aldea [radio]` | Anexiona la aldea cercana: sus aldeanos pasan a ser ciudadanos |
| `/pais unir <jugador>` / `expulsar` / `salir` | Gestiona los ciudadanos jugadores |
| `/pais color <color>` | Cambia el color del país |
| `/pais info [pais]` / `/pais lista` | Ficha del país / lista de países |
| `/pais abandonar` / `/pais disolver` | Suelta un chunk / borra el país |

Colores: rojo, azul, verde, amarillo, naranja, morado, rosa, cian, blanco, negro, gris, marron, lima, magenta, celeste, granate.

**Minimapa** (tecla **M**): arriba a la derecha, con el terreno, cada país pintado con su color, las fronteras marcadas,
los demás jugadores y los aviones. Debajo pone en qué país estás y si es enemigo.

Al cruzar una frontera te sale un aviso y el país recibe una alerta.
En tiempo de paz **nadie de fuera puede romper, construir ni abrir cofres** en un país; en guerra, sí.
Las explosiones y balas de un jugador también respetan esto.

### Gobierno
| Comando | Qué hace |
|---|---|
| `/pais presidente yo` | Te nombras presidente |
| `/pais presidente aldeano` | El aldeano más cercano pasa a ser presidente (con nombre visible) |
| `/pais ministro <cargo>` | El aldeano más cercano pasa a ser ministro (defensa, economia, exteriores, interior, sanidad, educacion, justicia, transporte) |
| `/pais cesar <cargo>` | Destituye a un ministro |
| `/pais elecciones` | Elecciones al azar entre jugadores del país y aldeanos ciudadanos |

Los ministros los nombra el presidente (o el fundador si el presidente es un aldeano). Si matan al presidente o a un
ministro aldeano, el cargo queda vacante y se avisa al país.

Bloques: **Mesa de gobierno** (muestra el gobierno), **Urna electoral** (convoca elecciones), **Puesto de control**
(registro de quién ha cruzado la frontera).

### Guerra
* `/pais guerra <pais>` declara la guerra (aviso en pantalla a todo el servidor).
* `/pais paz <pais>` ofrece la paz; si el otro país también la ofrece, se firma.
* `/pais conquistar`: en guerra, quédate en un chunk enemigo pegado a tu frontera sin defensores a 32 bloques y será tuyo.

### Armas (clic derecho para disparar)
* **Pistola, Fusil de asalto** (automático manteniendo clic), **Escopeta**, **Rifle de francotirador**
  (agáchate para apuntar con la mira) → gastan **Munición**.
* **Lanzacohetes** → gasta **Cohetes**. **Granadas** se lanzan.
* Las balas **rompen cristales**, hojas y a veces bloques blandos. Los bloques **antibalas** (hormigón blindado,
  cristal blindado, sacos terreros, muros...) no se rompen con balas, pero **sí con bombas**.

### Aviones y misiles
* **Avión de combate**: clic derecho en el suelo para ponerlo y otro clic para subir.
  W acelera, S frena, el ratón dirige, Shift te bajas. **R** ametralladora, **B** suelta una Bomba aérea.
  Si vas lento, el avión pierde altura. Agachado y golpeándolo vacío lo recoges.
* **Radio de bombardeo**: apunta a un sitio (hasta 300 bloques) y llega un bombardero que suelta una alfombra de bombas.
* **Designador láser** + **Lanzador de misiles**: marca un objetivo con el designador, haz clic derecho en el
  lanzador con el designador en la mano. Usa un **Misil balístico** o un **Misil nuclear** del inventario.
* **Radar**: clic derecho para ver aviones y misiles en 160 bloques. **Sirena antiaérea**: suena con redstone o clic.

### Bomba nuclear y radiación
* **Bomba nuclear**: actívala con mechero, carga de fuego o redstone. 10 segundos de cuenta atrás.
  Hace un cráter enorme, quema la zona, lanza a la gente por los aires y deja **radiación** 10 minutos (90 bloques).
* Protección: **Traje NBQ completo**, **Pastillas de yodo** (curan y protegen 5 min), estar cerca de un
  **Purificador de aire** (6 bloques), o bajo techo de **Hormigón reforzado / Bloque de plomo / Puerta de búnker**.
* El hormigón reforzado y la puerta de búnker aguantan incluso la bomba nuclear: haz tu **búnker** con ellos.
* **Contador Geiger**: en la mano hace clics en zona radiactiva y muestra el nivel.

### Otros bloques
Hito de frontera, valla y muro fronterizo, **barrera de control** (los ciudadanos la atraviesan, los extranjeros no),
alambre de espino, mina terrestre, asfalto, carretera con línea discontinua/continua, paso de cebra, losa de asfalto,
farola, y **22 banderas reales** (España, Francia, Alemania, Italia, Portugal, Reino Unido, EE. UU., México, Argentina,
Brasil, Japón, Ucrania, Rusia, China, Canadá, Polonia, Países Bajos, Bélgica, Suecia, Grecia, Colombia, Irlanda).

### Armaduras
Casco militar, Chaleco antibalas (la mitad de daño de bala), Pantalón y Botas militares (camuflaje),
y el equipo NBQ amarillo contra la radiación.

Todo tiene receta de crafteo (mira el libro de recetas) y está en dos pestañas del modo creativo.

## Estructura del proyecto
* `src/main/java/es/guerramod/` — código (pais, bloques, items, entidades, radiacion, cliente, red, eventos)
* `src/main/resources/` — texturas, modelos, recetas, idioma
* `herramientas/` — scripts de Python que generan los modelos, recetas, texturas y sonidos

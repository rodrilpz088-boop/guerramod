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

## Novedades de la versión 2

### Armas realistas
* **Clic derecho (mantener) = apuntar por la mira** (el arma se centra, se hace zoom y se anda más despacio).
* **Clic izquierdo = disparar.** Fusil, subfusil y ametralladora disparan en ráfaga manteniendo el clic.
* **R = recargar** (con cargador real: 15, 30, 100 balas…). La escopeta se recarga cartucho a cartucho.
* **V = cambiar entre automático y semiautomático.**
* Física del arma en la mano: tiene peso e inercia (se retrasa al girar), se balancea al andar, se baja al correr,
  da una patada hacia atrás y hacia arriba al disparar (retroceso de cámara con recuperación), se agacha al recargar
  y sube al sacarla. Las armas pesadas se mueven más lentas.
* El francotirador tiene **mira telescópica real** con retícula; la respiración mueve la mira (agáchate para estabilizar).
* Balas con física: caída por gravedad, pérdida de daño a distancia, **tiros a la cabeza x2**, piernas x0,7, atraviesan
  cristales y hojas, **rebotan** con ángulo rasante, rompen bloques blandos. Los bloques antibalas las paran.
* Casquillos que salen despedidos, fogonazo, humo y sonidos nuevos (cada arma suena distinta, eco a lo lejos).
* Granadas que rebotan y ruedan, y explotan a los 4 segundos.
* Munición: 9 mm (pistola, subfusil), 5,56 (fusil, ametralladora), 7,62 (francotirador), cartuchos del 12 (escopeta).
* El avión ahora tiene inercia, gira según la velocidad y se inclina al girar.

### Carreteras de España
Líneas continua/discontinua/doble, bordes, línea de detención, flechas, STOP y ceda pintados, líneas amarillas,
zigzag, aparcamiento, zona azul, carril bus y bici, cuadrícula de cruce, sumidero, tapa de alcantarilla, aceras de
pastillas, baldosas podotáctiles, bordillo de granito, adoquín, **34 señales oficiales** (R, P y S), **semáforos que
funcionan solos** (todos sincronizados: clic derecho cambia entre fase A y B para montar cruces; el de peatones va al
revés), guardarraíl, barrera New Jersey, conos, bolardos, hito kilométrico, captafaros, papelera (destruye lo que
tiras), contenedores de reciclaje, banco, buzón y farolas. Las señales van sobre el **poste de señal** (apílalos).

### Hogar (por apartados)
* **Salón:** sofás por módulos que se unen, sillones, mesa de centro, mueble de TV, televisiones (se encienden),
  estantería, alfombras, planta, cuadro, chimenea con fuego, aire acondicionado, radiador, mesa y sillas.
* **Cocina:** encimeras con cajones (guardan cosas), fregadero (llena botellas y cubos), vitrocerámica con horno,
  lavavajillas, armario alto, frigorífico de 2 bloques (54 huecos), microondas, campana, lavadora, taburete, cafetera.
* **Baño:** inodoro, bidé, lavabo, mueble de lavabo, plato de ducha con mampara, alcachofa de ducha (sale agua),
  bañera, espejo, toallero.
* **Dormitorio:** camas (se duerme de verdad), armario ropero, cómoda, mesita, escritorio, silla de oficina, ordenador.
* **Puertas y ventanas:** puertas lacada blanca, roble, nogal, cristal y acorazada (antibalas); ventanas de aluminio,
  PVC y madera; persianas que suben y bajan (las de la misma ventana se mueven juntas).
* **Jardín:** césped artificial, seto, valla blanca, verja de forja, malla de simple torsión, tumbona, sombrilla,
  barbacoa, silla y mesa de plástico, farolillo, gresite y borde de piscina.
* **Iluminación:** panel LED, foco LED y tira LED que cambian de color con el **mando LED** (clic derecho: siguiente
  color; Shift + clic: encender/apagar, a 12 bloques), plafón, lámpara colgante, aplique, lámparas de pie y mesa,
  e **interruptor** de pared (enciende/apaga todas las luces de la habitación).
* **Construcción:** gotelé, paredes pintadas, azulejo metro, baldosa hidráulica, gres, mármol, parquet, parquet en
  espiga, tarima, ladrillo caravista, teja árabe, hormigón pulido y granito de Ávila (con losas).

En los asientos (sillas, sofás, inodoro, bancos…) te sientas con clic derecho con la mano vacía.

## Estructura del proyecto
* `src/main/java/es/guerramod/` — código (pais, bloques, items, entidades, radiacion, cliente, red, eventos)
* `src/main/resources/` — texturas, modelos, recetas, idioma
* `herramientas/` — scripts de Python que generan los modelos, recetas, texturas y sonidos (`generar_todo.sh` los ejecuta todos)

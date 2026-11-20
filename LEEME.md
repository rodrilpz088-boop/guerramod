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

## Novedades de la versión 3: Incendios y bomberos

### Fuego realista
* El fuego de los incendios forestales es distinto al de Minecraft: **la hierba y el pasto arden en segundos** y el fuego
  avanza muy rápido; los árboles arden por las copas (las hojas) y los troncos se quedan **ardiendo** durante minutos.
* **Viento**: cambia de dirección y fuerza poco a poco. El fuego corre a favor del viento y lanza **pavesas** que crean
  focos nuevos a varios metros. En el minimapa ves la flecha del viento y el **riesgo de incendio**.
* **Pendiente**: el fuego sube las laderas mucho más rápido de lo que baja.
* **Sequía**: cuantos más días sin llover y más caluroso el bioma (sabana, desierto, badlands), peor.
* **Lluvia**: apaga las llamas y casi no deja propagar. Donde has echado agua, no prende durante un rato.
* Al pasar el fuego queda **tierra quemada** negra, **ceniza**, **troncos carbonizados**, **ramas quemadas** y
  **rescoldos** que humean durante minutos, queman al pisarlos y **pueden reavivar el incendio** (hay que mojarlos o
  removerlos con el rastrillo). Con el tiempo la hierba vuelve a crecer en la tierra quemada.
* **Velocidad realista** con los modelos de los servicios forestales (CSIRO/McArthur): la velocidad en la hierba depende
  de la **temperatura**, la **humedad del aire**, la **humedad de la hierba**, lo **seca (curada)** que esté, el **viento**
  y la **pendiente**. Ejemplos: llanura a mediodía con 20 km/h de viento ≈ 75 m/min; sabana a 38 °C con 30 km/h ≈ 175
  m/min; sin viento apenas avanza (repta medio metro por minuto); de noche, con frío o recién llovido casi se para.
  El incendio toma forma alargada a favor del viento y avanza muy despacio en contra. La **estación meteorológica** te dice
  temperatura, humedad, la velocidad de avance prevista y el índice de peligro (GFDI).
* **Los árboles hacen crecer el fuego**: las copas y los troncos ardiendo sacan **llamas altas** de casi dos bloques que
  propagan más y lanzan más pavesas.
* **Recuperación**: la tierra quemada pasa por 4 fases (negra → brotes → verdea → césped) y al final sale hierba y a
  veces un árbol joven. La ceniza se va con la lluvia, las ramas quemadas se caen y los troncos carbonizados se pudren
  con los años (a veces brota un árbol nuevo en su base). **Con lluvia todo se recupera mucho más rápido.**
* **Rayos secos**: con riesgo MUY ALTO o EXTREMO y sin llover, a veces cae un rayo y empieza un incendio forestal.
  Comando `/incendios` (info), y para operadores: `/incendios naturales true|false`, `/incendios provocar`,
  `/incendios rayo`, `/incendios viento <grados> <km/h>`, `/incendios apagar <radio>`.
* Tiempo a mano (operador): `/incendios temperatura <°C>` o `auto`, `/incendios humedad <%>` o `auto`,
  `/incendios sequia <días>` (lo seca que está la hierba), `/incendios oladecalor` (42 °C, 10 % de humedad,
  30 días sin llover, viento fuerte y cielo despejado) y `/incendios normal` para volver al tiempo automático.
* Columnas de humo que se ven desde lejos. El fuego normal (rayos, mecheros, lava) sobre hierba o árboles se convierte
  en fuego forestal.

### Material de bomberos
* **Manguera**: conéctala a una **boca de incendio** o a una **BIE** (clic con ella en la boca) y mantén clic derecho para
  lanzar un chorro de 16 m. Funciona hasta 45 m de la boca. Sin boca, usa el agua de la **mochila extintora** (en el pecho;
  se rellena metiéndote en agua con Shift + clic derecho con la manguera).
* **Extintor de polvo** (y extintor de pared, que se coge con la mano vacía): descarga corta, apaga a personas ardiendo.
* **Batefuegos**: sofoca las llamas a golpes.
* **Rastrillo forestal**: raspa una franja de 3 bloques hasta la tierra: **cortafuegos** que el fuego no puede cruzar.
* **Antorcha de goteo**: para hacer contrafuegos (¡ojo con el viento!).
* **Estación meteorológica**: viento, días sin llover, riesgo de incendio y fuegos activos cerca.
* **Radio de hidroavión**: llama a un hidroavión (tipo Canadair) que suelta agua sobre el incendio.
* **Detector de humo** (pita y da señal de redstone) y **rociador automático** (apaga solo el fuego de debajo).
* **Equipo de bombero**: cada pieza quita un 20 % del daño por fuego; con el equipo completo no te quemas.
* **Pasto seco**: hierba agostada que arde muchísimo.

## Novedades de la versión 4: Clima, cielo y ciudad

### Cielo realista
* El cielo del mundo normal se calcula con **física atmosférica de verdad** (dispersión de Rayleigh y de Mie y la capa de
  ozono). Por eso el cielo es azul intenso a mediodía, blanquecino en el horizonte, y al amanecer y al atardecer salen solos
  los **naranjas, rosas y violetas**, la sombra de la Tierra y la **hora azul**.
* El **sol** cambia de color (blanco a mediodía, naranja y rojo al ponerse) y tiene halo. La **luna** tiene sus fases y un
  halo suave. De noche hay **2600 estrellas de colores** que titilan, con la **Vía Láctea**.
* **Nubes nuevas** en dos capas (cúmulos y cirros altos) que se mueven con el viento y se iluminan con el sol: blancas a
  mediodía, doradas y rosas al atardecer, grises y oscuras con tormenta. A lo lejos se funden con el cielo.
* Con **humo de incendios, polvo del desierto o humedad** el aire se vuelve más turbio: atardeceres más rojos y cielo lechoso.
* **Niebla y neblina** de verdad cuando el aire está saturado (temperatura cerca del punto de rocío y poco viento): por la
  noche y al amanecer en valles, ríos, pantanos y selvas, después de llover, y en lo alto de las montañas cuando están
  dentro de las nubes. La bruma del horizonte toma el color del cielo.
* Si otro mod o un paquete de sombreadores da problemas, el mod vuelve solo al cielo normal.

### Clima por zonas
Cada zona tiene su clima: la tundra y las cumbres nevadas están bajo cero, la taiga es fresca, los desiertos son muy secos
con mucha diferencia entre el día y la noche, la selva y los pantanos son húmedos y estables, y el mar suaviza las
temperaturas. Con nubes hace menos calor de día; en altura hace más frío; bajo tierra la temperatura es estable.
El minimapa muestra la temperatura, la humedad y el cielo (despejado, nuboso, niebla...).

### Cambio climático
* Las **chimeneas de fábricas y centrales** emiten CO2 (también los incendios). Con más CO2 el mundo **se calienta**
  (3 °C cada vez que se duplica, y casi el doble en las zonas frías), con retraso, como pasa con los océanos.
* Con el calentamiento: **más calor y sequía, más tormentas secas y más incendios**, y la nieve y el hielo se derriten.
* El CO2 se va absorbiendo poco a poco solo (océanos y bosques). Las **plantas de captura de CO2** lo absorben más rápido.
* Se puede desactivar con `/clima cambio false`.

### Planos de construcción (pestaña "Ciudad, industria y clima")
* **Clic derecho al aire** para elegir el edificio (agachado, hacia atrás). **Clic en el suelo**: se ve el contorno;
  **otro clic en el mismo bloque**: el edificio se construye solo, planta a planta, con la fachada mirando hacia ti.
* Edificios: casa unifamiliar, bloque de pisos con balcones, rascacielos de 22 plantas con helipuerto, nave almacén,
  **fábrica** (2 chimeneas), **central térmica de carbón** (2 chimeneas de 47 m y torre de refrigeración), aerogenerador y
  **planta de captura de CO2**.
* Clic derecho en una chimenea para **apagarla o encenderla** (apagada no emite).

### Comandos de clima
* `/clima`: temperatura, humedad, punto de rocío, cielo, CO2, calentamiento y humo en el aire.
* Operador: `/clima cambio true|false`, `/clima co2 <ppm>`, `/clima calentar <°C>`, `/clima nubes <%>|auto`, `/clima reiniciar`.
* `/incendios temperatura` ahora fija la temperatura de las zonas templadas: las zonas de nieve siguen más frías y los
  desiertos más calurosos.

## Estructura del proyecto
* `src/main/java/es/guerramod/` — código (pais, bloques, items, entidades, radiacion, cliente, red, eventos)
* `src/main/resources/` — texturas, modelos, recetas, idioma
* `herramientas/` — scripts de Python que generan los modelos, recetas, texturas y sonidos (`generar_todo.sh` los ejecuta todos)

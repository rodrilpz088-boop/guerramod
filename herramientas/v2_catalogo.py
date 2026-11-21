#!/usr/bin/env python3
"""Catálogo de la versión 2: muebles, carreteras, señales, construcción, iluminación y armas en 3D.
Genera modelos, estados de bloque, recetas, botín, etiquetas, idioma y el registro en Java.
Ejecutar DESPUÉS de generar_datos.py, generar_texturas.py y v2_texturas.py:
    python3 herramientas/v2_catalogo.py
"""
import json
import os
from collections import Counter

MOD = "guerramod"
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(RAIZ, "src", "main", "resources")
A = os.path.join(RES, "assets", MOD)
D = os.path.join(RES, "data", MOD)
DMC = os.path.join(RES, "data", "minecraft")
JAVA = os.path.join(RAIZ, "src", "main", "java", "es", "guerramod", "registro", "CatalogoGenerado.java")

TRANSLUCIDAS = {"mat_cristal", "puerta_cristal_arriba", "puerta_cristal_abajo", "ventana_aluminio", "ventana_pvc",
                "ventana_madera", "farol_cristal"}


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=1)


def tex(n):
    if ":" in n:
        return n
    return f"{MOD}:block/{n}"


def existe_textura(ref):
    ns, p = ref.split(":")
    if ns != MOD:
        return True
    return os.path.exists(os.path.join(A, "textures", p + ".png"))


# ===================================================================== geometría

CARAS = ["north", "south", "east", "west", "up", "down"]


def B(x1, y1, z1, x2, y2, z2, t, caras=None, tinte=False, solo=None, rot=None):
    return {"f": [x1, y1, z1], "t": [x2, y2, z2], "tex": t, "caras": caras or {}, "tinte": tinte, "solo": solo, "rot": rot}


def patas(x1, z1, x2, z2, alto, t, g=1.5, y0=0):
    return [B(x1, y0, z1, x1 + g, alto, z1 + g, t), B(x2 - g, y0, z1, x2, alto, z1 + g, t),
            B(x1, y0, z2 - g, x1 + g, alto, z2, t), B(x2 - g, y0, z2 - g, x2, alto, z2, t)]


def uv_auto(cara, f, t):
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {"down": [x1, 16 - z2, x2, 16 - z1], "up": [x1, z1, x2, z2], "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1],
            "south": [x1, 16 - y2, x2, 16 - y1], "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[cara]


def uv_seguro(cara, f, t):
    """UV dentro de 0..16 aunque el elemento se salga del bloque (para modelos de item y armas)."""
    f2, t2 = list(f), list(t)
    for i in range(3):
        base = (f2[i] // 16) * 16
        f2[i] -= base
        t2[i] -= base
        if t2[i] > 16:
            if t2[i] - f2[i] >= 16:
                f2[i], t2[i] = 0, 16
            else:
                d = t2[i] - 16
                f2[i] -= d
                t2[i] = 16
    u = uv_auto(cara, f2, t2)
    return [round(min(16, max(0, v)), 3) for v in u]


def modelo(elementos, uv_explicito=False, render=None, extra=None, ao=True):
    texturas = {}
    elems = []
    for e in elementos:
        caras = {}
        for c in (e["solo"] or CARAS):
            tn = e["caras"].get(c, e["tex"])
            if tn is None:
                continue
            ref = tex(tn)
            if ref not in texturas:
                texturas[ref] = "t" + str(len(texturas))
            cara = {"texture": "#" + texturas[ref]}
            if uv_explicito:
                cara["uv"] = uv_seguro(c, e["f"], e["t"])
            if e["tinte"]:
                cara["tintindex"] = 0
            caras[c] = cara
        el = {"from": [round(v, 3) for v in e["f"]], "to": [round(v, 3) for v in e["t"]], "faces": caras}
        if e["rot"]:
            el["rotation"] = e["rot"]
        elems.append(el)
    tx = {v: k for k, v in texturas.items()}
    tx["particle"] = next(iter(texturas.keys())) if texturas else "minecraft:block/stone"
    m = {"parent": "minecraft:block/block", "textures": tx, "elements": elems}
    if not ao:
        m["ambientocclusion"] = False
    if render is None:
        render = "minecraft:translucent" if any(k.split("/")[-1] in TRANSLUCIDAS for k in texturas) else "minecraft:cutout"
    m["render_type"] = render
    if extra:
        m.update(extra)
    return m


def cajas_java(elementos, desp=0):
    out = []
    for e in elementos:
        f, t = e["f"], e["t"]
        out.append([f[0], f[1] - desp, f[2], t[0], t[1] - desp, t[2]])
    return out


def partir(elementos):
    """Divide un modelo de 0..32 de alto en mitad de abajo y de arriba."""
    abajo, arriba = [], []
    for e in elementos:
        y1, y2 = e["f"][1], e["t"][1]
        if y1 < 16:
            n = dict(e, f=list(e["f"]), t=list(e["t"]))
            n["t"][1] = min(16, y2)
            if n["t"][1] - n["f"][1] > 0.01:
                if y2 > 16:
                    n["solo"] = [c for c in (e["solo"] or CARAS) if c != "up"]
                abajo.append(n)
        if y2 > 16:
            n = dict(e, f=list(e["f"]), t=list(e["t"]))
            n["f"][1] = max(0, y1 - 16)
            n["t"][1] = y2 - 16
            if y1 < 16:
                n["solo"] = [c for c in (e["solo"] or CARAS) if c != "down"]
            if y1 >= 16:
                n["f"][1] = y1 - 16
            arriba.append(n)
    return abajo, arriba


def desplazar(elementos, dx=0, dy=0, dz=0):
    return [dict(e, f=[e["f"][0] + dx, e["f"][1] + dy, e["f"][2] + dz], t=[e["t"][0] + dx, e["t"][1] + dy, e["t"][2] + dz])
            for e in elementos]


def cambiar_tex(elementos, mapa):
    out = []
    for e in elementos:
        n = dict(e)
        n["tex"] = mapa.get(e["tex"], e["tex"])
        n["caras"] = {k: mapa.get(v, v) for k, v in e["caras"].items()}
        out.append(n)
    return out


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
DISPLAY_MUEBLE = {
    "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
    "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
    "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
    "firstperson_lefthand": {"rotation": [0, 225, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]},
}


def mb(nombre, datos):
    escribir(os.path.join(A, "models", "block", nombre + ".json"), datos)


def mi(nombre, datos):
    escribir(os.path.join(A, "models", "item", nombre + ".json"), datos)


def bs(nombre, datos):
    escribir(os.path.join(A, "blockstates", nombre + ".json"), datos)


def variantes_facing(modelo_por_estado, extra_props=None, base_rot=0):
    """modelo_por_estado: dict 'clave_extra' -> nombre de modelo; claves '' si no hay más propiedades."""
    v = {}
    for f, y in ROT.items():
        for clave, mod in modelo_por_estado.items():
            k = f"facing={f}" + ("," + clave if clave else "")
            d = {"model": f"{MOD}:block/{mod}"}
            if (y + base_rot) % 360:
                d["y"] = (y + base_rot) % 360
            v[k] = d
    return {"variants": v}


# ===================================================================== catálogo

CAT = []


def E(id_, nombre, cat, tipo, mat="madera", receta=None, cantidad=1, **kw):
    CAT.append(dict(id=id_, nombre=nombre, cat=cat, tipo=tipo, mat=mat, receta=receta, cantidad=cantidad, **kw))


mc = lambda n: "minecraft:" + n
G = lambda n: f"{MOD}:" + n
PLANKS = "#minecraft:planks"
WOOL = "#minecraft:wool"

# ------------------------------------------------------------------ geometrías de muebles


def g_sofa(t):
    return [B(0, 2, 2, 16, 7, 16, t), B(0.5, 7, 2, 15.5, 9, 13, t), B(0, 7, 13, 16, 14, 16, t), B(0.5, 9, 11.5, 15.5, 13.5, 13, t),
            B(1, 0, 3, 2.5, 2, 4.5, "mat_metal_negro"), B(13.5, 0, 3, 15, 2, 4.5, "mat_metal_negro"),
            B(1, 0, 14, 2.5, 2, 15.5, "mat_metal_negro"), B(13.5, 0, 14, 15, 2, 15.5, "mat_metal_negro")]


def g_brazo(t, izq):
    return [B(0, 2, 2, 2.5, 11, 16, t)] if izq else [B(13.5, 2, 2, 16, 11, 16, t)]


def g_sillon(t):
    return g_sofa(t) + g_brazo(t, True) + g_brazo(t, False)


def g_silla(asiento, respaldo, patas_t):
    return patas(2.5, 2.5, 13.5, 13.5, 8, patas_t, 1.2) + [B(2.5, 8, 2.5, 13.5, 9, 13.5, respaldo), B(3, 9, 3, 13, 9.6, 12.5, asiento),
                                                          B(2.5, 9, 12.3, 3.7, 15, 13.5, patas_t), B(12.3, 9, 12.3, 13.5, 15, 13.5, patas_t),
                                                          B(3.7, 11, 12.6, 12.3, 15, 13.3, respaldo)]


def g_mesa(top, pata, alto=14, grosor=1):
    return [B(0, alto, 0, 16, alto + grosor, 16, top)] + patas(1, 1, 15, 15, alto, pata, 1.5)


def g_encimera(frente, top="mat_granito_negro", cuerpo="mat_lacado_blanco", top_cara=None):
    e = [B(0, 1, 1, 16, 14, 16, cuerpo, {"north": frente}), B(0.5, 0, 1.5, 15.5, 1, 15.5, "mat_metal_negro"),
         B(0, 14, 0, 16, 16, 16, top, {"up": top_cara} if top_cara else None)]
    return e


def g_grifo():
    return [B(7.5, 16, 13, 8.5, 21, 14, "mat_cromo"), B(7.5, 20, 9.5, 8.5, 21, 13, "mat_cromo"), B(7.6, 19.2, 9.6, 8.4, 20, 10.4, "mat_cromo")]


def g_tele(t_pantalla):
    return [B(4, 0, 6.5, 12, 0.5, 11, "mat_negro"), B(7.3, 0.5, 8.6, 8.7, 3.2, 9.6, "mat_negro"),
            B(0, 3, 8.5, 16, 13, 9.4, "mat_negro", {"north": t_pantalla}), B(0.3, 3.3, 8.4, 15.7, 12.7, 8.5, t_pantalla, solo=["north"])]


def g_lampara_pie(pantalla):
    return [B(5.5, 0, 5.5, 10.5, 0.6, 10.5, "mat_metal_negro"), B(7.5, 0.6, 7.5, 8.5, 11, 8.5, "mat_metal_negro"),
            B(4.5, 11, 4.5, 11.5, 16, 11.5, pantalla)]


def g_lampara_mesa(pantalla):
    return [B(6, 0, 6, 10, 1, 10, "mat_cromo"), B(7.5, 1, 7.5, 8.5, 6, 8.5, "mat_cromo"), B(5, 6, 5, 11, 10, 11, pantalla)]


def g_inodoro():
    return [B(5.5, 0, 3, 10.5, 4, 10, "mat_ceramica"), B(4, 4, 1.5, 12, 6.5, 11.5, "mat_ceramica"),
            B(4, 6.5, 1.5, 12, 7, 11.5, "mat_plastico_blanco"), B(3.5, 4, 11.5, 12.5, 12, 15.5, "mat_ceramica"),
            B(3.3, 12, 11.3, 12.7, 12.6, 15.7, "mat_ceramica"), B(7, 12.6, 13, 9, 12.9, 14, "mat_cromo")]


def g_cama(cabeza, sabana, edredon, madera_t):
    if cabeza:
        return [B(0, 0, 0, 16, 15, 1.5, madera_t), B(0, 2, 1.5, 16, 5, 16, madera_t), B(0.5, 5, 1.5, 15.5, 9, 16, "mat_colchon"),
                B(1.5, 9, 2.2, 7.5, 11, 6.5, sabana), B(8.5, 9, 2.2, 14.5, 11, 6.5, sabana),
                B(0.3, 8.6, 7, 15.7, 9.7, 16, edredon)] + [B(0, 0, 1.5, 1.5, 2, 3, madera_t), B(14.5, 0, 1.5, 16, 2, 3, madera_t)]
    return [B(0, 2, 0, 16, 5, 15, madera_t), B(0, 0, 14.5, 16, 7, 16, madera_t), B(0.5, 5, 0, 15.5, 9, 14.5, "mat_colchon"),
            B(0.3, 3.5, 0, 15.7, 9.7, 14.5, edredon), B(0, 0, 13, 1.5, 2, 14.5, madera_t), B(14.5, 0, 13, 16, 2, 14.5, madera_t)]


def g_contenedor(color, frente):
    return [B(0.5, 1.5, 1, 15.5, 15, 15, color, {"north": frente}), B(0.2, 15, 0.7, 15.8, 16, 15.3, color),
            B(1.5, 13, 0.4, 14.5, 14, 1, "mat_plastico_negro"), B(1.5, 0, 2, 3.5, 1.5, 4, "mat_plastico_negro"),
            B(12.5, 0, 2, 14.5, 1.5, 4, "mat_plastico_negro"), B(1.5, 0, 12, 3.5, 1.5, 14, "mat_plastico_negro"),
            B(12.5, 0, 12, 14.5, 1.5, 14, "mat_plastico_negro")]


# ------------------------------------------------------------------ SALÓN
for col, nom, lana in (("gris", "gris", "gray_wool"), ("beige", "beige", "white_wool"), ("azul", "azul", "blue_wool")):
    E(f"sofa_{col}", f"Sofá {nom} (módulo)", "SALON", "sofa", "tela", geo=g_sofa(f"mat_tela_{col}"),
      brazo=f"mat_tela_{col}", asiento=0.56, receta=[mc(lana), mc(lana), mc(lana), PLANKS, PLANKS])
E("sillon_gris", "Sillón gris", "SALON", "mueble", "tela", geo=g_sillon("mat_tela_gris"), asiento=0.56,
  receta=[mc("gray_wool"), mc("gray_wool"), PLANKS])
E("sillon_cuero", "Sillón de cuero", "SALON", "mueble", "tela", geo=g_sillon("mat_cuero"), asiento=0.56,
  receta=[mc("leather"), mc("leather"), mc("leather"), PLANKS])
E("mesa_centro", "Mesa de centro", "SALON", "mueble", "madera",
  geo=[B(0, 6, 2, 16, 7, 14, "mat_roble"), B(1, 2, 3, 15, 2.6, 13, "mat_roble")] + patas(1, 3, 15, 13, 6, "mat_metal_negro", 1),
  receta=[mc("oak_slab"), mc("oak_slab"), mc("iron_nugget")])
E("mueble_tv", "Mueble de televisión", "SALON", "mueble", "madera", huecos=9,
  geo=[B(0, 1, 4, 16, 7, 16, "mat_lacado_blanco", {"north": "frente_mueble_tv"}), B(0, 7, 3.5, 16, 7.6, 16, "mat_roble")]
      + patas(1, 5, 15, 15, 1, "mat_metal_negro", 1),
  receta=[mc("chest"), mc("white_dye"), mc("oak_slab")])
E("television", "Televisión", "SALON", "encendible", "metal", efecto="TELE", luz=7, geo=g_tele("tele_off"),
  on={"tele_off": "tele_on"}, receta=[mc("black_concrete"), mc("glass_pane"), mc("redstone"), mc("iron_ingot")])
E("television_pared", "Televisión de pared", "SALON", "encendible", "metal", efecto="TELE", luz=7,
  geo=[B(0, 3, 14.6, 16, 13, 16, "mat_negro", {"north": "tele_off"}), B(0.3, 3.3, 14.5, 15.7, 12.7, 14.6, "tele_off", solo=["north"])],
  on={"tele_off": "tele_on"}, receta=[mc("black_concrete"), mc("glass_pane"), mc("redstone"), mc("iron_nugget")])
E("estanteria", "Estantería con libros", "SALON", "alto", "madera", huecos=27,
  geo=[B(0, 0, 3, 1, 31, 16, "mat_roble"), B(15, 0, 3, 16, 31, 16, "mat_roble"), B(1, 0, 15, 15, 31, 16, "mat_roble")]
      + [B(1, y, 3, 15, y + 1, 15, "mat_roble") for y in (0, 8, 16, 24, 30)]
      + [B(1.5, y + 1, 5, 14.5, y + 7, 14.5, "mat_libros", {"up": "mat_roble", "down": "mat_roble"}) for y in (0, 8, 16, 24)],
  receta=[mc("bookshelf"), mc("bookshelf"), PLANKS])
for col, nom, lana in (("gris", "gris", "gray_carpet"), ("beige", "beige", "white_carpet"), ("roja", "roja", "red_carpet")):
    E(f"alfombra_{col}", f"Alfombra {nom}", "SALON", "alfombra", "tela", textura=f"alfombra_{col}",
      receta=[mc(lana), mc(lana), mc("string")])
E("planta_interior", "Planta de interior", "SALON", "mueble", "hojas",
  geo=[B(5, 0, 5, 11, 6, 11, "mat_ceramica"), B(5.5, 5.5, 5.5, 10.5, 6, 10.5, "mat_tierra"), B(3, 6, 3, 13, 14, 13, "mat_planta"),
       B(5, 13, 5, 11, 17, 11, "mat_planta")], receta=[mc("flower_pot"), mc("oak_sapling"), mc("bone_meal")])
E("cuadro_abstracto", "Cuadro abstracto", "SALON", "mueble", "madera",
  geo=[B(1, 3, 15, 15, 13, 16, "mat_negro", {"north": "cuadro_abstracto"})], receta=[mc("painting"), mc("black_dye")])
E("chimenea", "Chimenea", "SALON", "encendible", "piedra", efecto="FUEGO", luz=13,
  geo=[B(0, 0, 8, 16, 2, 16, "granito_avila"), B(0, 2, 8, 2, 12, 16, "granito_avila"), B(14, 2, 8, 16, 12, 16, "granito_avila"),
       B(0, 12, 7, 16, 16, 16, "granito_avila"), B(2, 2, 15, 14, 12, 16, "mat_negro"), B(3, 2, 10, 13, 3.5, 13, "mat_nogal"),
       B(3, 2, 9.5, 13, 2.4, 14, "mat_ceniza")], on={"mat_ceniza": "mat_brasas"},
  receta=[mc("campfire"), mc("stone_bricks"), mc("stone_bricks"), mc("stone_bricks")])
E("aire_acondicionado", "Aire acondicionado", "SALON", "encendible", "plastico", efecto="AIRE", luz=0,
  geo=[B(0, 10, 12, 16, 15, 16, "mat_plastico_blanco", {"north": "aire_frente"})],
  receta=[mc("white_concrete"), mc("redstone"), mc("blue_ice")])
E("radiador", "Radiador", "SALON", "mueble", "metal", geo=[B(0, 2, 13.5, 16, 11, 16, "radiador", {"up": "mat_plastico_blanco"})],
  receta=[mc("iron_ingot"), mc("iron_ingot"), mc("white_dye"), mc("water_bucket")])
E("mesa_comedor", "Mesa de comedor", "SALON", "mueble", "madera", geo=g_mesa("mat_roble", "mat_roble"),
  receta=[PLANKS, PLANKS, PLANKS, mc("stick"), mc("stick")])
E("silla_comedor", "Silla de comedor", "SALON", "mueble", "madera", geo=g_silla("mat_tela_beige", "mat_roble", "mat_roble"),
  asiento=0.6, receta=[PLANKS, PLANKS, mc("stick"), mc("white_wool")])

# ------------------------------------------------------------------ COCINA
E("encimera", "Encimera con cajones", "COCINA", "mueble", "madera", huecos=27, geo=g_encimera("frente_cajones_blanco"),
  receta=[mc("chest"), mc("smooth_stone_slab"), mc("white_dye")])
E("encimera_roble", "Encimera de roble", "COCINA", "mueble", "madera", huecos=27,
  geo=g_encimera("frente_cajones_roble", "mat_marmol", "mat_roble"), receta=[mc("chest"), mc("quartz_slab"), mc("oak_planks")])
E("fregadero", "Fregadero", "COCINA", "mueble", "madera", accion="GRIFO",
  geo=g_encimera("frente_armario_cocina", top_cara="fregadero_top") + g_grifo(),
  receta=[mc("cauldron"), mc("smooth_stone_slab"), mc("white_dye")])
E("vitroceramica", "Vitrocerámica con horno", "COCINA", "mueble", "metal",
  geo=g_encimera("frente_horno", top="mat_granito_negro", cuerpo="mat_inox", top_cara="vitro_top"),
  receta=[mc("furnace"), mc("smooth_stone_slab"), mc("black_dye")])
E("lavavajillas", "Lavavajillas", "COCINA", "mueble", "metal", huecos=9,
  geo=g_encimera("frente_lavavajillas", cuerpo="mat_inox"), receta=[mc("chest"), mc("iron_ingot"), mc("water_bucket")])
E("armario_cocina", "Armario alto de cocina", "COCINA", "mueble", "madera", huecos=9,
  geo=[B(0, 4, 8, 16, 16, 16, "mat_lacado_blanco", {"north": "frente_armario_cocina"})],
  receta=[mc("barrel"), mc("white_dye"), mc("iron_nugget")])
E("frigorifico", "Frigorífico", "COCINA", "alto", "metal", huecos=54,
  geo=[B(0, 0, 1, 16, 16, 16, "mat_inox", {"north": "frigo_abajo"}), B(0, 16, 1, 16, 30, 16, "mat_inox", {"north": "frigo_arriba"})],
  receta=[mc("iron_block"), mc("chest"), mc("blue_ice")])
E("microondas", "Microondas", "COCINA", "mueble", "metal",
  geo=[B(2, 0, 4, 14, 6, 13, "mat_inox", {"north": "frente_microondas"})], receta=[mc("iron_ingot"), mc("glass_pane"), mc("redstone")])
E("campana", "Campana extractora", "COCINA", "mueble", "metal",
  geo=[B(2, 6, 7, 14, 8, 16, "mat_inox", {"down": "mat_rejilla"}), B(5.5, 8, 11.5, 10.5, 16, 16, "mat_inox")],
  receta=[mc("iron_ingot"), mc("iron_ingot"), mc("iron_bars")])
E("lavadora", "Lavadora", "COCINA", "mueble", "plastico", huecos=9,
  geo=[B(0, 0.5, 1, 16, 14, 16, "mat_plastico_blanco", {"north": "frente_lavadora"}), B(0, 14, 1, 16, 14.6, 16, "mat_plastico_blanco")],
  receta=[mc("iron_block"), mc("glass_pane"), mc("water_bucket")])
E("mesa_cocina", "Mesa de cocina", "COCINA", "mueble", "madera", geo=g_mesa("mat_lacado_blanco", "mat_cromo"),
  receta=[mc("smooth_quartz_slab"), mc("iron_ingot"), mc("iron_ingot")])
E("taburete", "Taburete alto", "COCINA", "mueble", "metal",
  geo=patas(3.5, 3.5, 12.5, 12.5, 11, "mat_cromo", 0.8) + [B(3.5, 5, 3.5, 12.5, 5.5, 4.3, "mat_cromo"),
                                                           B(3, 11, 3, 13, 12.5, 13, "mat_cuero")],
  asiento=0.8, receta=[mc("iron_ingot"), mc("leather"), mc("iron_nugget")])
E("cafetera", "Cafetera", "COCINA", "mueble", "metal",
  geo=[B(5, 0, 6, 11, 1, 12, "mat_negro"), B(5, 1, 10, 11, 7, 12, "mat_negro"), B(5, 6, 6, 11, 7, 10, "mat_negro"),
       B(6.5, 1, 6.5, 9.5, 4, 9.5, "mat_cristal"), B(7.3, 5.2, 7.3, 8.7, 6, 8.7, "mat_cromo")],
  receta=[mc("cauldron"), mc("cocoa_beans"), mc("redstone")])

# ------------------------------------------------------------------ BAÑO
E("inodoro", "Inodoro", "BANO", "mueble", "ceramica", geo=g_inodoro(), asiento=0.44,
  receta=[mc("quartz_block"), mc("cauldron"), mc("lever")])
E("bide", "Bidé", "BANO", "mueble", "ceramica",
  geo=[B(5.5, 0, 3, 10.5, 4, 10, "mat_ceramica"), B(4, 4, 1.5, 12, 6.5, 11.5, "mat_ceramica"), B(7.5, 6.5, 10.5, 8.5, 7.5, 11.5, "mat_cromo")],
  asiento=0.41, receta=[mc("quartz_block"), mc("cauldron"), mc("iron_nugget")])
E("lavabo", "Lavabo de pie", "BANO", "mueble", "ceramica", accion="GRIFO",
  geo=[B(6, 0, 9, 10, 11, 14, "mat_ceramica"), B(2, 11, 5, 14, 14, 16, "mat_ceramica", {"up": "lavabo_top"}),
       B(7.5, 14, 13.5, 8.5, 16.5, 14.5, "mat_cromo"), B(7.5, 15.5, 11.5, 8.5, 16.5, 13.5, "mat_cromo")],
  receta=[mc("quartz_block"), mc("cauldron"), mc("iron_ingot")])
E("mueble_lavabo", "Mueble de lavabo", "BANO", "mueble", "madera", accion="GRIFO",
  geo=[B(1, 0, 6, 15, 11, 16, "mat_roble", {"north": "frente_cajones_roble"}), B(0.5, 11, 5, 15.5, 13, 16, "mat_ceramica", {"up": "lavabo_top"}),
       B(7.5, 13, 14, 8.5, 15.5, 15, "mat_cromo"), B(7.5, 14.5, 12, 8.5, 15.5, 14, "mat_cromo")],
  receta=[mc("barrel"), mc("cauldron"), mc("quartz_slab")])
E("plato_ducha", "Plato de ducha con mampara", "BANO", "mueble", "ceramica",
  geo=[B(0, 0, 0, 16, 1, 16, "mat_ceramica", {"up": "plato_ducha"}), B(0, 1, 0, 0.4, 16, 16, "mat_cristal"),
       B(0.4, 1, 15.6, 16, 16, 16, "mat_cristal"), B(0, 1, 0, 8, 16, 0.4, "mat_cristal"), B(0, 15.6, 0, 8, 16, 0.6, "mat_aluminio")],
  receta=[mc("quartz_slab"), mc("glass_pane"), mc("glass_pane"), mc("glass_pane")])
E("alcachofa_ducha", "Alcachofa de ducha", "BANO", "encendible", "metal", efecto="AGUA", luz=0,
  geo=[B(7.5, 12, 11, 8.5, 13, 16, "mat_cromo"), B(5.5, 11, 6.5, 10.5, 12, 11.5, "mat_cromo", {"down": "mat_rejilla"})],
  receta=[mc("iron_ingot"), mc("water_bucket"), mc("iron_nugget")])
E("banera", "Bañera", "BANO", "mueble", "ceramica", geo=[B(0, 0, 0, 16, 8, 16, "mat_ceramica", {"up": "banera_top"}),
                                                     B(7.5, 8, 14, 8.5, 10, 15, "mat_cromo"), B(7.5, 9.3, 12.5, 8.5, 10, 14, "mat_cromo")],
  asiento=0.3, receta=[mc("quartz_block"), mc("quartz_block"), mc("cauldron")])
E("espejo_bano", "Espejo de baño", "BANO", "mueble", "cristal",
  geo=[B(1, 4, 15.4, 15, 15, 16, "mat_aluminio", {"north": "espejo"})], receta=[mc("glass_pane"), mc("iron_nugget"), mc("quartz")])
E("toallero", "Toallero con toalla", "BANO", "mueble", "metal",
  geo=[B(2, 10, 14.6, 14, 10.6, 15.3, "mat_cromo"), B(2, 9.5, 15.3, 3, 11, 16, "mat_cromo"), B(13, 9.5, 15.3, 14, 11, 16, "mat_cromo"),
       B(3.5, 4, 14.2, 12.5, 10.8, 15.6, "mat_tela_azul")], receta=[mc("iron_nugget"), mc("blue_wool"), mc("stick")])

# ------------------------------------------------------------------ DORMITORIO
for col, nom, sab, edr, mad, lana in (("blanca", "blanca", "mat_sabana_blanca", "mat_edredon_blanco", "mat_lacado_blanco", "white_bed"),
                                      ("gris", "gris", "mat_sabana_blanca", "mat_sabana_gris", "mat_nogal", "gray_bed"),
                                      ("azul", "azul", "mat_sabana_blanca", "mat_edredon_azul", "mat_roble", "blue_bed")):
    E(f"cama_{col}", f"Cama {nom}", "DORMITORIO", "cama", "madera", cabeza=g_cama(True, sab, edr, mad),
      pies=g_cama(False, sab, edr, mad), receta=[mc(lana), PLANKS, mc("white_wool")])
E("armario_ropero", "Armario ropero", "DORMITORIO", "alto", "madera", huecos=54,
  geo=[B(0, 0, 2, 16, 16, 16, "mat_roble", {"north": "armario_abajo"}), B(0, 16, 2, 16, 31, 16, "mat_roble", {"north": "armario_arriba"}),
       B(0, 31, 1.5, 16, 32, 16, "mat_roble")], receta=[mc("chest"), mc("chest"), PLANKS, PLANKS])
E("comoda", "Cómoda", "DORMITORIO", "mueble", "madera", huecos=27,
  geo=[B(0, 0, 3, 16, 13, 16, "mat_roble", {"north": "frente_comoda"}), B(0, 13, 2.5, 16, 14, 16, "mat_roble")],
  receta=[mc("chest"), PLANKS, mc("iron_nugget")])
E("mesita_noche", "Mesita de noche", "DORMITORIO", "mueble", "madera", huecos=9,
  geo=[B(2, 1, 3, 14, 9, 15, "mat_lacado_blanco", {"north": "frente_mesita"}), B(2, 9, 2.7, 14, 9.6, 15, "mat_roble")]
      + patas(2.5, 3.5, 13.5, 14.5, 1, "mat_roble", 1), receta=[mc("barrel"), mc("white_dye"), mc("oak_slab")])
E("escritorio", "Escritorio", "DORMITORIO", "mueble", "madera",
  geo=[B(0, 12, 2, 16, 13, 16, "mat_roble"), B(0.5, 0, 3, 1.5, 12, 15, "mat_lacado_blanco"),
       B(10.5, 0, 3, 15.5, 12, 15, "mat_lacado_blanco", {"north": "frente_cajones_blanco"})],
  receta=[PLANKS, PLANKS, mc("oak_slab"), mc("white_dye")])
E("silla_oficina", "Silla de oficina", "DORMITORIO", "mueble", "metal",
  geo=[B(4, 0, 7.5, 12, 1, 8.5, "mat_plastico_negro"), B(7.5, 0, 4, 8.5, 1, 12, "mat_plastico_negro"),
       B(7.5, 1, 7.5, 8.5, 7, 8.5, "mat_cromo"), B(3, 7, 3, 13, 9, 13, "mat_tela_negra"), B(3.5, 9, 12, 12.5, 16, 13.5, "mat_tela_negra"),
       B(2.5, 9, 6, 3.5, 11, 10, "mat_plastico_negro"), B(12.5, 9, 6, 13.5, 11, 10, "mat_plastico_negro")],
  asiento=0.6, receta=[mc("black_wool"), mc("iron_ingot"), mc("black_dye")])
E("ordenador", "Ordenador", "DORMITORIO", "encendible", "metal", efecto="TELE", luz=5,
  geo=[B(2, 3, 10, 14, 11, 11, "mat_negro", {"north": "tele_off"}), B(7, 0.5, 10.5, 9, 3, 11.5, "mat_negro"),
       B(5, 0, 9.5, 11, 0.5, 13, "mat_negro"), B(3, 0, 3, 13, 0.5, 7, "mat_negro", {"up": "teclado"}),
       B(13.5, 0, 4, 15.5, 0.6, 7, "mat_negro")], on={"tele_off": "monitor_on"},
  receta=[mc("black_concrete"), mc("glass_pane"), mc("redstone"), mc("comparator")])

# ------------------------------------------------------------------ ILUMINACIÓN
E("led_techo", "Panel LED de techo", "ILUMINACION", "led", "metal", luz=15,
  geo=[B(2, 15.2, 2, 14, 16, 14, "led_marco"), B(2.7, 15, 2.7, 13.3, 15.2, 13.3, "led_luz", tinte=True)],
  receta=[mc("glowstone"), mc("iron_ingot"), mc("white_stained_glass_pane")], cantidad=4)
E("foco_led", "Foco LED empotrado", "ILUMINACION", "led", "metal", luz=14,
  geo=[B(5, 15.4, 5, 11, 16, 11, "led_marco"), B(6, 15.2, 6, 10, 15.4, 10, "led_luz", tinte=True)],
  receta=[mc("glowstone_dust"), mc("iron_nugget"), mc("glass_pane")], cantidad=4)
E("tira_led", "Tira LED", "ILUMINACION", "tira_led", "metal", luz=12,
  receta=[mc("glowstone_dust"), mc("string"), mc("redstone")], cantidad=8)
E("plafon", "Plafón de techo", "ILUMINACION", "encendible", "cristal", efecto="LUZ", luz=15,
  geo=[B(3, 14.8, 3, 13, 16, 13, "pantalla_lampara_off"), B(4, 14.4, 4, 12, 14.8, 12, "pantalla_lampara_off")],
  on={"pantalla_lampara_off": "mat_luz_calida"}, receta=[mc("glowstone"), mc("glass_pane"), mc("iron_nugget")])
E("lampara_colgante", "Lámpara colgante", "ILUMINACION", "encendible", "metal", efecto="LUZ", luz=15,
  geo=[B(7.8, 8, 7.8, 8.2, 16, 8.2, "mat_metal_negro"), B(4, 4, 4, 12, 8, 12, "mat_metal_negro", {"down": "pantalla_lampara_off"}),
       B(6.5, 3.6, 6.5, 9.5, 4, 9.5, "pantalla_lampara_off")], on={"pantalla_lampara_off": "mat_luz_calida"},
  receta=[mc("lantern"), mc("chain"), mc("black_dye")])
E("aplique_pared", "Aplique de pared", "ILUMINACION", "encendible", "metal", efecto="LUZ", luz=13,
  geo=[B(6.5, 6, 15, 9.5, 11, 16, "mat_metal_negro"), B(5.5, 8, 11, 10.5, 12, 15, "pantalla_lampara_off")],
  on={"pantalla_lampara_off": "pantalla_lampara_on"}, receta=[mc("torch"), mc("iron_nugget"), mc("white_carpet")])
E("lampara_pie", "Lámpara de pie", "ILUMINACION", "encendible", "metal", efecto="LUZ", luz=14,
  geo=g_lampara_pie("pantalla_lampara_off"), on={"pantalla_lampara_off": "pantalla_lampara_on"},
  receta=[mc("torch"), mc("iron_ingot"), mc("white_wool"), mc("stick")])
E("lampara_mesa", "Lámpara de mesa", "ILUMINACION", "encendible", "metal", efecto="LUZ", luz=12,
  geo=g_lampara_mesa("pantalla_lampara_off"), on={"pantalla_lampara_off": "pantalla_lampara_on"},
  receta=[mc("torch"), mc("iron_nugget"), mc("white_wool")])
E("interruptor", "Interruptor de luz", "ILUMINACION", "interruptor", "plastico",
  receta=[mc("lever"), mc("white_dye")], cantidad=2)

# ------------------------------------------------------------------ PUERTAS Y VENTANAS
for p, nom, mat, rec in (("puerta_blanca", "Puerta lacada blanca", "madera", [mc("oak_door"), mc("white_dye")]),
                         ("puerta_roble", "Puerta de roble", "madera", [mc("oak_door"), mc("iron_nugget")]),
                         ("puerta_nogal", "Puerta de nogal", "madera", [mc("dark_oak_door"), mc("iron_nugget")]),
                         ("puerta_cristal", "Puerta de cristal y aluminio", "cristal", [mc("iron_door"), mc("glass_pane"), mc("glass_pane")]),
                         ("puerta_acorazada", "Puerta acorazada de entrada", "madera", [mc("iron_door"), mc("dark_oak_planks"), mc("iron_ingot")])):
    E(p, nom, "PUERTAS", "puerta", mat, receta=rec)
for v, nom, rec in (("ventana_aluminio", "Ventana de aluminio", [mc("glass_pane"), mc("glass_pane"), mc("iron_nugget")]),
                    ("ventana_pvc", "Ventana de PVC blanca", [mc("glass_pane"), mc("glass_pane"), mc("white_dye")]),
                    ("ventana_madera", "Ventana de madera", [mc("glass_pane"), mc("glass_pane"), mc("stick")])):
    E(v, nom, "PUERTAS", "panel", "cristal", textura=v, canto=v + "_canto", receta=rec, cantidad=4)
for col, nom, rec in (("gris", "gris", [mc("iron_trapdoor"), mc("gray_dye")]), ("blanca", "blanca", [mc("iron_trapdoor"), mc("white_dye")])):
    E(f"persiana_{col}", f"Persiana {nom}", "PUERTAS", "persiana", "metal", textura=f"persiana_{col}", receta=rec, cantidad=2)

# ------------------------------------------------------------------ JARDÍN
E("cesped_artificial", "Césped artificial", "JARDIN", "cubo", "cesped", textura="cesped_artificial",
  receta=[mc("green_wool"), mc("dirt"), mc("lime_dye")], cantidad=4)
E("cesped_artificial_fino", "Césped artificial (rollo)", "JARDIN", "alfombra", "cesped", textura="cesped_artificial",
  receta=[mc("green_carpet"), mc("lime_dye")], cantidad=4)
E("seto", "Seto", "JARDIN", "cubo", "hojas", textura="seto", receta=[mc("oak_leaves"), mc("oak_leaves"), mc("bone_meal")], cantidad=2)
E("valla_madera_blanca", "Valla de madera blanca", "JARDIN", "valla", "madera", textura="valla_blanca",
  receta=[mc("oak_fence"), mc("oak_fence"), mc("white_dye")], cantidad=2)
E("puerta_valla_madera", "Puerta de valla blanca", "JARDIN", "puerta_valla", "madera", textura="valla_blanca",
  receta=[mc("oak_fence_gate"), mc("white_dye")])
E("verja_forja", "Verja de forja", "JARDIN", "panel", "metal", textura="verja_forja", canto="verja_forja_canto",
  receta=[mc("iron_bars"), mc("iron_bars"), mc("black_dye")], cantidad=4)
E("puerta_verja_forja", "Puerta de verja de forja", "JARDIN", "puerta_valla", "metal", textura="forja",
  receta=[mc("iron_bars"), mc("oak_fence_gate"), mc("black_dye")])
E("malla_torsion", "Valla de malla de simple torsión", "JARDIN", "panel", "metal", textura="malla_torsion",
  canto="malla_torsion_canto", receta=[mc("iron_bars"), mc("string"), mc("green_dye")], cantidad=8)
E("tumbona", "Tumbona", "JARDIN", "mueble", "plastico",
  geo=patas(2, 1, 14, 15, 3, "mat_aluminio", 1) + [B(2, 3, 1, 14, 4, 11, "mat_tumbona"),
                                                  B(2, 4, 11, 14, 9, 15, "mat_tumbona", rot={"origin": [8, 4, 11], "axis": "x", "angle": -22.5})],
  asiento=0.3, receta=[mc("iron_nugget"), mc("blue_wool"), mc("white_wool")])
E("sombrilla", "Sombrilla de playa", "JARDIN", "alto", "tela",
  geo=[B(7.5, 0, 7.5, 8.5, 28, 8.5, "mat_aluminio"), B(5, 0, 5, 11, 1, 11, "mat_hormigon"), B(-6, 26, -6, 22, 27, 22, "mat_tumbona"),
       B(-2, 27, -2, 18, 28, 18, "mat_tumbona"), B(3, 28, 3, 13, 29, 13, "mat_tumbona")],
  receta=[mc("stick"), mc("blue_wool"), mc("white_wool"), mc("white_wool")])
E("barbacoa", "Barbacoa", "JARDIN", "encendible", "metal", efecto="FUEGO", luz=12,
  geo=patas(2, 3, 14, 13, 8, "mat_metal_negro", 1) + [B(2, 8, 3, 14, 11, 13, "mat_metal_negro"),
                                                     B(2.5, 11, 3.5, 13.5, 11.2, 12.5, "mat_ceniza"), B(2.5, 11.4, 3.5, 13.5, 11.6, 12.5, "mat_rejilla")],
  on={"mat_ceniza": "mat_brasas"}, receta=[mc("campfire"), mc("iron_bars"), mc("iron_ingot")])
E("silla_plastico", "Silla de plástico blanca", "JARDIN", "mueble", "plastico",
  geo=g_silla("mat_plastico_blanco", "mat_plastico_blanco", "mat_plastico_blanco"), asiento=0.6,
  receta=[mc("white_concrete"), mc("white_concrete"), mc("bone_meal")])
E("mesa_plastico", "Mesa de plástico blanca", "JARDIN", "mueble", "plastico",
  geo=g_mesa("mat_plastico_blanco", "mat_plastico_blanco", 12, 1), receta=[mc("white_concrete"), mc("white_concrete"), mc("white_concrete"), mc("bone_meal")])
E("farolillo_jardin", "Farolillo de jardín", "JARDIN", "encendible", "metal", efecto="LUZ", luz=11,
  geo=[B(6.5, 0, 6.5, 9.5, 6, 9.5, "mat_metal_negro"), B(5.5, 6, 5.5, 10.5, 10, 10.5, "pantalla_lampara_off"),
       B(5, 10, 5, 11, 11, 11, "mat_metal_negro")], on={"pantalla_lampara_off": "mat_luz_calida"},
  receta=[mc("lantern"), mc("black_dye"), mc("stone_slab")])
E("gresite", "Gresite de piscina", "JARDIN", "cubo", "ceramica", textura="gresite",
  receta=[mc("light_blue_glazed_terracotta"), mc("blue_dye")], cantidad=4)
E("borde_piscina", "Borde de piscina", "JARDIN", "losa", "piedra", textura="borde_piscina",
  receta=[mc("smooth_quartz_slab"), mc("white_dye"), mc("sand")], cantidad=4)

# ------------------------------------------------------------------ CONSTRUCCIÓN
CONSTR = [("gotele_blanco", "Pared de gotelé blanca", "yeso", [mc("white_concrete_powder"), mc("clay_ball")]),
          ("pared_beige", "Pared pintada beige", "yeso", [mc("white_concrete_powder"), mc("yellow_dye")]),
          ("pared_gris", "Pared pintada gris", "yeso", [mc("white_concrete_powder"), mc("light_gray_dye")]),
          ("pared_azul", "Pared pintada azul", "yeso", [mc("white_concrete_powder"), mc("light_blue_dye")]),
          ("pared_verde", "Pared pintada verde", "yeso", [mc("white_concrete_powder"), mc("lime_dye")]),
          ("pared_terracota", "Pared pintada terracota", "yeso", [mc("white_concrete_powder"), mc("orange_dye")]),
          ("azulejo_metro", "Azulejo blanco tipo metro", "ceramica", [mc("white_glazed_terracotta"), mc("quartz")]),
          ("azulejo_hidraulico", "Baldosa hidráulica", "ceramica", [mc("blue_glazed_terracotta"), mc("red_dye")]),
          ("gres_gris", "Gres porcelánico gris", "ceramica", [mc("gray_glazed_terracotta"), mc("quartz")]),
          ("marmol_blanco", "Mármol blanco", "piedra", [mc("quartz_block"), mc("gray_dye")]),
          ("parquet_roble", "Parquet de roble", "madera", [mc("oak_planks"), mc("oak_slab")]),
          ("parquet_espiga", "Parquet en espiga", "madera", [mc("oak_planks"), mc("spruce_slab")]),
          ("tarima_gris", "Tarima gris", "madera", [mc("oak_planks"), mc("light_gray_dye")]),
          ("ladrillo_caravista", "Ladrillo caravista", "ladrillo", [mc("bricks"), mc("sand")]),
          ("teja_arabe", "Teja árabe", "ladrillo", [mc("bricks"), mc("terracotta")]),
          ("hormigon_pulido", "Hormigón pulido", "hormigon", [mc("gray_concrete"), mc("quartz")]),
          ("granito_avila", "Granito gris de Ávila", "piedra", [mc("diorite"), mc("andesite")])]
for id_, nom, mat, rec in CONSTR:
    E(id_, nom, "CONSTRUCCION", "cubo", mat, textura=id_, receta=rec, cantidad=4)
for id_, nom, mat, t in (("gres_gris_losa", "Losa de gres gris", "ceramica", "gres_gris"),
                         ("marmol_blanco_losa", "Losa de mármol blanco", "piedra", "marmol_blanco"),
                         ("parquet_roble_losa", "Losa de parquet de roble", "madera", "parquet_roble"),
                         ("granito_avila_losa", "Losa de granito de Ávila", "piedra", "granito_avila")):
    E(id_, nom, "CONSTRUCCION", "losa", mat, textura=t, receta=[G(t), G(t), G(t)], cantidad=6)

# ------------------------------------------------------------------ CARRETERAS
RECETA_VIAL = lambda *extra: [G("asfalto")] + list(extra)
VIALES = [("asfalto_desgastado", "Asfalto desgastado", False, [G("asfalto"), mc("gravel")]),
          ("asfalto_doble_continua", "Carretera: doble línea continua", True, [G("asfalto"), mc("white_dye"), mc("white_dye"), mc("white_dye")]),
          ("asfalto_borde", "Carretera: línea de borde", True, [G("asfalto"), mc("bone_meal")]),
          ("asfalto_borde_discontinua", "Carretera: borde discontinuo", True, [G("asfalto"), mc("bone_meal"), mc("bone_meal")]),
          ("asfalto_detencion", "Carretera: línea de detención", True, [G("asfalto"), mc("white_dye"), mc("bone_meal")]),
          ("asfalto_flecha_recto", "Carretera: flecha recto", True, [G("asfalto"), mc("white_dye"), mc("arrow")]),
          ("asfalto_flecha_izquierda", "Carretera: flecha a la izquierda", True, [G("asfalto"), mc("white_dye"), mc("arrow"), mc("stick")]),
          ("asfalto_flecha_derecha", "Carretera: flecha a la derecha", True, [G("asfalto"), mc("white_dye"), mc("arrow"), mc("feather")]),
          ("asfalto_flecha_recto_derecha", "Carretera: flecha recto y derecha", True, [G("asfalto"), mc("white_dye"), mc("arrow"), mc("arrow")]),
          ("asfalto_stop", "Carretera: STOP pintado", True, [G("asfalto"), mc("white_dye"), mc("red_dye")]),
          ("asfalto_ceda", "Carretera: ceda el paso pintado", True, [G("asfalto"), mc("white_dye"), mc("yellow_dye")]),
          ("asfalto_amarilla", "Carretera: línea amarilla (no estacionar)", True, [G("asfalto"), mc("yellow_dye")]),
          ("asfalto_zigzag", "Carretera: zigzag amarillo", True, [G("asfalto"), mc("yellow_dye"), mc("yellow_dye")]),
          ("asfalto_aparcamiento", "Carretera: plaza de aparcamiento", True, [G("asfalto"), mc("white_dye"), mc("minecart")]),
          ("asfalto_zona_azul", "Carretera: zona azul (ORA)", True, [G("asfalto"), mc("blue_dye")]),
          ("asfalto_bus", "Carretera: carril BUS", True, [G("asfalto"), mc("white_dye"), mc("paper")]),
          ("asfalto_bici", "Carril bici con símbolo", True, [G("asfalto"), mc("red_dye"), mc("bone_meal")]),
          ("carril_bici", "Carril bici (asfalto rojo)", False, [G("asfalto"), mc("red_dye")]),
          ("asfalto_cuadricula", "Carretera: cuadrícula amarilla de cruce", False, [G("asfalto"), mc("yellow_dye"), mc("yellow_dye"), mc("yellow_dye")]),
          ("asfalto_rejilla", "Sumidero (rejilla)", True, [G("asfalto"), mc("iron_bars")]),
          ("asfalto_tapa", "Tapa de alcantarilla", False, [G("asfalto"), mc("iron_trapdoor")])]
for id_, nom, orient, rec in VIALES:
    E(id_, nom, "CARRETERAS", "vial_orientable" if orient else "vial", "asfalto", textura=id_, receta=rec,
      cantidad=1 if len(rec) > 1 else 1)
E("acera", "Acera (baldosa de pastillas)", "CARRETERAS", "cubo", "piedra", textura="acera",
  receta=[mc("smooth_stone"), mc("stone_button")], cantidad=4)
E("acera_podotactil_botones", "Baldosa podotáctil de botones", "CARRETERAS", "cubo", "piedra", textura="acera_podotactil_botones",
  receta=[mc("smooth_stone"), mc("yellow_dye"), mc("stone_button")], cantidad=4)
E("acera_podotactil_rayas", "Baldosa podotáctil direccional", "CARRETERAS", "vial_orientable", "piedra",
  textura="acera_podotactil_rayas", lado="acera", receta=[mc("smooth_stone"), mc("yellow_dye"), mc("stick")], cantidad=4)
E("bordillo", "Bordillo de granito", "CARRETERAS", "vial_orientable", "piedra", textura="bordillo_arriba", lado="bordillo_lado",
  receta=[mc("smooth_stone"), mc("diorite"), mc("stone_slab")], cantidad=4)
E("adoquin_granito", "Adoquín de granito", "CARRETERAS", "cubo", "piedra", textura="adoquin_granito",
  receta=[mc("cobblestone"), mc("diorite")], cantidad=4)
E("poste_senal", "Poste de señal", "CARRETERAS", "poste", "metal", receta=[mc("iron_ingot"), mc("stick"), mc("stick")], cantidad=4)

SENALES = [("stop", "STOP (R-2)"), ("ceda", "Ceda el paso (R-1)"), ("prohibido_entrar", "Entrada prohibida (R-101)"),
           ("circulacion_prohibida", "Circulación prohibida (R-100)")] + \
          [(f"velocidad_{v}", f"Velocidad máxima {v} (R-301)") for v in (20, 30, 40, 50, 60, 80, 90, 100, 120)] + \
          [("prohibido_girar_izquierda", "Giro a la izquierda prohibido (R-302)"), ("prohibido_girar_derecha", "Giro a la derecha prohibido (R-303)"),
           ("prohibido_adelantar", "Adelantamiento prohibido (R-305)"), ("prohibido_estacionar", "Estacionamiento prohibido (R-308)"),
           ("prohibido_parar", "Parada y estacionamiento prohibido (R-307)"), ("sentido_obligatorio_derecha", "Sentido obligatorio derecha (R-400)"),
           ("sentido_obligatorio_recto", "Sentido obligatorio recto (R-400)"), ("rotonda", "Intersección de sentido giratorio (R-402)"),
           ("paso_peatones", "Paso para peatones (S-13)"), ("aparcamiento", "Estacionamiento (S-17)"), ("sentido_unico", "Calzada de sentido único (S-11)"),
           ("hospital", "Hospital (S-??)"), ("parada_bus", "Parada de autobús (S-19)"), ("calle_residencial", "Calle residencial (S-28)"),
           ("autopista", "Autopista (S-1)"), ("peligro", "Otros peligros (P-50)"), ("peligro_rotonda", "Peligro: glorieta (P-4)"),
           ("peligro_peatones", "Peligro: peatones (P-20)"), ("peligro_ninos", "Peligro: niños (P-21)"),
           ("peligro_curva_derecha", "Peligro: curva a la derecha (P-13a)"), ("peligro_obras", "Peligro: obras (P-18)"),
           ("peligro_semaforo", "Peligro: semáforos (P-3)"), ("peligro_cruce", "Peligro: intersección con prioridad (P-1)")]
TINTES = ["white_dye", "red_dye", "blue_dye", "yellow_dye", "black_dye", "orange_dye", "lime_dye", "gray_dye", "light_blue_dye",
          "pink_dye", "purple_dye", "cyan_dye", "brown_dye", "green_dye", "magenta_dye", "light_gray_dye"]
EXTRAS = ["paper", "feather", "string", "stick", "flint", "clay_ball"]
for i, (s, nom) in enumerate(SENALES):
    nom = nom.replace(" (S-??)", "")
    rec = [G("poste_senal"), mc("iron_ingot"), mc(TINTES[i % 16])] + ([mc(EXTRAS[i // 16 - 1])] if i >= 16 else [])
    E("senal_" + s, "Señal: " + nom, "CARRETERAS", "senal", "metal", senal=s, receta=rec)
E("semaforo", "Semáforo", "CARRETERAS", "semaforo", "metal", peatonal=False,
  receta=[mc("redstone_lamp"), mc("red_dye"), mc("yellow_dye"), mc("lime_dye"), G("poste_senal")])
E("semaforo_peatones", "Semáforo de peatones", "CARRETERAS", "semaforo", "metal", peatonal=True,
  receta=[mc("redstone_lamp"), mc("red_dye"), mc("lime_dye"), G("poste_senal")])
E("guardarrail", "Guardarraíl (bionda)", "CARRETERAS", "mueble", "metal",
  geo=[B(7, 0, 13, 9, 11, 15, "poste_senal"), B(0, 6, 11, 16, 10, 12.5, "bionda")],
  receta=[mc("iron_ingot"), mc("iron_ingot"), mc("iron_ingot"), mc("stick")], cantidad=4)
E("barrera_new_jersey", "Barrera New Jersey de hormigón", "CARRETERAS", "mueble", "hormigon",
  geo=[B(0, 0, 3, 16, 4, 13, "mat_hormigon"), B(0, 4, 5, 16, 8, 11, "mat_hormigon"), B(0, 8, 6, 16, 14, 10, "mat_hormigon")],
  receta=[mc("gray_concrete"), mc("gray_concrete"), mc("iron_nugget")], cantidad=4)
E("cono", "Cono de tráfico", "CARRETERAS", "mueble", "plastico",
  geo=[B(3, 0, 3, 13, 1, 13, "cono"), B(5, 1, 5, 11, 4, 11, "cono"), B(5.75, 4, 5.75, 10.25, 7, 10.25, "reflectante"),
       B(6.5, 7, 6.5, 9.5, 10, 9.5, "cono"), B(7.25, 10, 7.25, 8.75, 12, 8.75, "cono")],
  receta=[mc("orange_dye"), mc("white_dye"), mc("slime_ball")], cantidad=4)
E("bolardo", "Bolardo (pivote)", "CARRETERAS", "mueble", "metal",
  geo=[B(6.5, 0, 6.5, 9.5, 11, 9.5, "mat_metal_negro"), B(6.4, 8, 6.4, 9.6, 9, 9.6, "reflectante"), B(6, 11, 6, 10, 12, 10, "mat_metal_negro")],
  receta=[mc("iron_ingot"), mc("black_dye")], cantidad=4)
E("hito_kilometrico", "Hito kilométrico", "CARRETERAS", "mueble", "hormigon",
  geo=[B(4, 0, 6, 12, 14, 10, "reflectante", {"north": "hito_km", "south": "hito_km", "up": "cono"})],
  receta=[mc("white_concrete"), mc("red_dye"), mc("sign")] if False else [mc("white_concrete"), mc("red_dye"), mc("oak_sign")])
E("captafaro", "Captafaro (ojo de gato)", "CARRETERAS", "mueble", "plastico",
  geo=[B(5, 0, 5, 11, 0.8, 11, "captafaro")], receta=[mc("glowstone_dust"), mc("orange_dye"), mc("stone_button")], cantidad=8)
E("papelera", "Papelera (destruye lo que tiras)", "CARRETERAS", "mueble", "metal", accion="PAPELERA",
  geo=[B(7.5, 0, 7.5, 8.5, 3, 8.5, "mat_metal_negro"), B(4.5, 3, 4.5, 11.5, 11, 11.5, "mat_verde_oscuro"),
       B(4.3, 10.5, 4.3, 11.7, 11, 11.7, "mat_metal_negro")], receta=[mc("cauldron"), mc("green_dye"), mc("lava_bucket")])
for col, nom, lana in (("amarillo", "amarillo (envases)", "yellow_dye"), ("azul", "azul (papel y cartón)", "blue_dye"),
                       ("verde", "verde (vidrio)", "green_dye"), ("gris", "gris (resto)", "gray_dye"), ("marron", "marrón (orgánico)", "brown_dye")):
    E(f"contenedor_{col}", f"Contenedor {nom}", "CARRETERAS", "mueble", "plastico", huecos=27,
      geo=g_contenedor(f"mat_{col}", f"contenedor_{col}"), receta=[mc("chest"), mc(lana), mc("iron_ingot")])
E("banco_publico", "Banco público", "CARRETERAS", "mueble", "madera",
  geo=[B(1, 0, 4, 2, 7, 12, "mat_metal_negro"), B(14, 0, 4, 15, 7, 12, "mat_metal_negro"), B(0, 7, 3, 16, 8, 11, "tablillas"),
       B(0, 8, 11, 16, 15, 12.5, "tablillas"), B(1, 7, 11.5, 2, 13, 12.5, "mat_metal_negro"), B(14, 7, 11.5, 15, 13, 12.5, "mat_metal_negro")],
  asiento=0.5, receta=[mc("oak_slab"), mc("oak_slab"), mc("iron_ingot")])
E("buzon", "Buzón de correos", "CARRETERAS", "mueble", "metal", huecos=9,
  geo=[B(6, 0, 7, 10, 5, 9, "mat_amarillo"), B(3, 5, 4, 13, 16, 12, "mat_amarillo", {"north": "buzon_frente"})],
  receta=[mc("chest"), mc("yellow_dye"), mc("paper")])
E("farola_moderna", "Cabeza de farola LED", "CARRETERAS", "luz_fija", "metal", luz=15,
  geo=[B(7, 0, 7, 9, 12, 9, "poste_senal"), B(2, 12, 2, 14, 14, 14, "mat_lacado_gris"), B(3, 11.5, 3, 13, 12, 13, "farol_luz")],
  receta=[mc("glowstone"), mc("iron_ingot"), G("poste_senal")])
E("farola_fernandina", "Farola fernandina", "CARRETERAS", "luz_fija", "metal", luz=15,
  geo=[B(6, 0, 6, 10, 4, 10, "forja"), B(5, 4, 5, 11, 5, 11, "forja"), B(5.3, 5, 5.3, 10.7, 13, 10.7, "farol_cristal"),
       B(6.5, 5, 6.5, 9.5, 12, 9.5, "farol_luz"), B(4.5, 13, 4.5, 11.5, 14, 11.5, "forja"), B(6, 14, 6, 10, 15.5, 10, "forja"),
       B(7.5, 15.5, 7.5, 8.5, 16, 8.5, "forja")], receta=[mc("lantern"), mc("iron_ingot"), mc("black_dye")])

# ===================================================================== VERSIÓN 1.0.0: vivienda, calle y Navidad
# ------------------------------------------------------------------ VIVIENDA
E("puerta_ascensor", "Puerta de ascensor", "VIVIENDA", "puerta", "metal", receta=[mc("iron_door"), mc("iron_ingot"), mc("orange_dye")])
E("placa_solar", "Placa solar", "VIVIENDA", "mueble", "metal",
  geo=[B(0, 0, 0, 16, 2, 16, "mat_aluminio", {"up": "placa_solar"}), B(1, 2, 1, 15, 2.2, 15, "placa_solar")],
  receta=[mc("daylight_detector"), mc("blue_stained_glass_pane"), mc("iron_ingot")])
E("termo_electrico", "Termo eléctrico", "VIVIENDA", "mueble", "metal",
  geo=[B(3, 1, 9, 13, 15, 16, "mat_plastico_blanco", {"north": "termo_frente"}), B(6, 0, 11, 7, 1, 13, "mat_cromo"),
       B(9, 0, 11, 10, 1, 13, "mat_cromo")], receta=[mc("cauldron"), mc("redstone"), mc("white_dye")])
E("caldera_gas", "Caldera de gas", "VIVIENDA", "mueble", "metal",
  geo=[B(2, 0, 9, 14, 16, 16, "mat_plastico_blanco", {"north": "caldera_frente"})], receta=[mc("furnace"), mc("white_dye"), mc("copper_ingot")])
E("portero_automatico", "Portero automático", "VIVIENDA", "mueble", "metal",
  geo=[B(5, 3, 15, 11, 13, 16, "mat_aluminio", {"north": "portero_frente"})], receta=[mc("note_block"), mc("stone_button"), mc("iron_nugget")])
E("buzones_comunidad", "Buzones de la comunidad", "VIVIENDA", "mueble", "madera", huecos=27,
  geo=[B(0, 0, 10, 16, 16, 16, "mat_roble", {"north": "buzones_frente"})], receta=[mc("barrel"), mc("paper"), mc("iron_nugget")])
E("ventilador_techo", "Ventilador de techo", "VIVIENDA", "luz_fija", "metal", luz=12,
  geo=[B(7.5, 12, 7.5, 8.5, 16, 8.5, "mat_cromo"), B(6, 10, 6, 10, 12, 10, "mat_lacado_blanco"), B(6.5, 9, 6.5, 9.5, 10, 9.5, "mat_luz_calida"),
       B(0, 11, 7, 6, 11.5, 9, "mat_roble"), B(10, 11, 7, 16, 11.5, 9, "mat_roble"), B(7, 11, 0, 9, 11.5, 6, "mat_roble"),
       B(7, 11, 10, 9, 11.5, 16, "mat_roble")], receta=[mc("lightning_rod"), mc("oak_slab"), mc("glowstone_dust")])
E("barandilla_cristal", "Barandilla de cristal", "VIVIENDA", "mueble", "cristal",
  geo=[B(0, 0, 14, 16, 1, 16, "mat_inox"), B(0, 1, 14.6, 16, 14, 15.4, "mat_cristal"), B(0, 14, 14, 16, 15, 16, "mat_inox")],
  receta=[mc("glass_pane"), mc("iron_bars"), mc("quartz")], cantidad=4)
E("barandilla_forja", "Barandilla de forja (balcón)", "VIVIENDA", "mueble", "metal",
  geo=[B(0, 0, 14.5, 16, 1, 15.5, "forja"), B(0, 13, 14.5, 16, 14, 15.5, "forja"), B(1, 1, 14.75, 2, 13, 15.25, "forja"),
       B(5, 1, 14.75, 6, 13, 15.25, "forja"), B(9, 1, 14.75, 10, 13, 15.25, "forja"), B(13, 1, 14.75, 14, 13, 15.25, "forja"),
       B(3, 5, 14.75, 13, 6, 15.25, "forja")], receta=[mc("iron_bars"), mc("black_dye"), mc("iron_nugget")], cantidad=4)
E("toldo", "Toldo de rayas", "VIVIENDA", "mueble", "tela",
  geo=[B(0, 14, 12, 16, 15, 16, "toldo_rayas"), B(0, 12, 8, 16, 13, 12, "toldo_rayas"), B(0, 10, 4, 16, 11, 8, "toldo_rayas"),
       B(0, 8, 0, 16, 9, 4, "toldo_rayas"), B(0, 6, 0, 16, 8, 0.5, "toldo_rayas")], receta=[mc("green_wool"), mc("white_wool"), mc("stick")], cantidad=2)
E("split_exterior", "Aire acondicionado (unidad exterior)", "VIVIENDA", "mueble", "metal",
  geo=[B(0, 2, 6, 16, 13, 16, "mat_plastico_blanco", {"north": "split_frente"}), B(2, 0, 8, 3, 2, 15, "mat_metal_negro"),
       B(13, 0, 8, 14, 2, 15, "mat_metal_negro")], receta=[mc("iron_bars"), mc("blue_ice"), mc("redstone"), mc("white_dye")])
E("antena_tv", "Antena de televisión", "VIVIENDA", "mueble", "metal",
  geo=[B(7.5, 0, 7.5, 8.5, 16, 8.5, "mat_aluminio"), B(1, 14, 7.5, 15, 14.6, 8.5, "mat_aluminio"), B(3, 11, 7.5, 13, 11.6, 8.5, "mat_aluminio"),
       B(5, 8, 7.5, 11, 8.6, 8.5, "mat_aluminio"), B(7.5, 14, 2, 8.5, 14.6, 14, "mat_aluminio")],
  receta=[mc("lightning_rod"), mc("iron_nugget"), mc("iron_nugget")])
E("antena_parabolica", "Antena parabólica", "VIVIENDA", "mueble", "metal",
  geo=[B(7, 0, 12, 9, 6, 16, "mat_gris"), B(2, 4, 9, 14, 14, 11, "mat_lacado_blanco"), B(3, 3, 9.5, 13, 15, 10.5, "mat_lacado_blanco"),
       B(7.5, 8, 2, 8.5, 9, 9, "mat_gris"), B(7, 7.5, 1, 9, 9.5, 2, "mat_metal_negro")],
  receta=[mc("iron_ingot"), mc("white_dye"), mc("redstone"), mc("bowl")])
E("contador_luz", "Contador de la luz", "VIVIENDA", "mueble", "metal",
  geo=[B(3, 2, 12, 13, 14, 16, "mat_gris", {"north": "contador_frente"})], receta=[mc("clock"), mc("iron_ingot"), mc("redstone")])
E("cuadro_electrico", "Cuadro eléctrico", "VIVIENDA", "mueble", "plastico",
  geo=[B(2, 3, 14, 14, 13, 16, "mat_plastico_blanco", {"north": "cuadro_electrico_frente"})],
  receta=[mc("lever"), mc("redstone"), mc("white_dye"), mc("iron_nugget")])
E("enchufe", "Enchufe", "VIVIENDA", "mueble", "plastico",
  geo=[B(5, 5, 15.5, 11, 11, 16, "mat_plastico_blanco", {"north": "enchufe"})], receta=[mc("redstone"), mc("white_dye"), mc("iron_nugget")], cantidad=4)
E("extractor_bano", "Extractor de baño", "VIVIENDA", "mueble", "plastico",
  geo=[B(3, 3, 15, 13, 13, 16, "mat_plastico_blanco", {"north": "extractor"})], receta=[mc("iron_bars"), mc("white_dye"), mc("feather")])
E("puerta_garaje", "Puerta de garaje", "VIVIENDA", "mueble", "metal",
  geo=[B(0, 0, 14, 16, 16, 16, "mat_gris", {"north": "puerta_garaje", "south": "puerta_garaje"})],
  receta=[mc("iron_trapdoor"), mc("iron_trapdoor"), mc("white_dye")], cantidad=2)
E("felpudo", "Felpudo", "VIVIENDA", "alfombra", "tela", textura="felpudo", receta=[mc("brown_carpet"), mc("wheat")], cantidad=2)
E("camara_seguridad", "Cámara de seguridad", "VIVIENDA", "mueble", "metal",
  geo=[B(6, 10, 14, 10, 13, 16, "mat_plastico_blanco"), B(5, 8, 6, 11, 12, 14, "mat_plastico_blanco", {"north": "camara_frente"})],
  receta=[mc("spyglass"), mc("redstone"), mc("white_dye")])
E("alarma_casa", "Alarma de la casa", "VIVIENDA", "mueble", "plastico",
  geo=[B(3, 3, 14, 13, 13, 16, "mat_plastico_blanco", {"north": "alarma_frente"})], receta=[mc("note_block"), mc("red_dye"), mc("redstone")])
E("aplique_portal", "Aplique de portal", "VIVIENDA", "luz_fija", "metal", luz=14,
  geo=[B(6, 4, 14, 10, 12, 16, "mat_metal_negro"), B(6.5, 5, 10, 9.5, 11, 14, "farol_cristal"), B(7, 6, 11, 9, 10, 13, "farol_luz")],
  receta=[mc("lantern"), mc("black_dye"), mc("glass_pane")])

# ------------------------------------------------------------------ CALLE (más mobiliario urbano)
E("marquesina_bus", "Marquesina de autobús", "CARRETERAS", "alto", "metal",
  geo=[B(0, 0, 13, 16, 26, 14, "mat_cristal", {"north": "marquesina_cartel", "south": "marquesina_cartel"}),
       B(0, 26, 0, 16, 27.5, 16, "mat_lacado_gris"), B(0, 0, 14, 1, 26, 15, "mat_inox"), B(15, 0, 14, 16, 26, 15, "mat_inox"),
       B(1, 7, 9, 15, 8, 13, "mat_inox"), B(2, 0, 11, 3, 7, 12, "mat_inox"), B(13, 0, 11, 14, 7, 12, "mat_inox")],
  receta=[mc("glass"), mc("iron_ingot"), mc("smooth_stone_slab"), mc("painting")])
E("parada_bus", "Poste de parada de autobús", "CARRETERAS", "mueble", "metal",
  geo=[B(7.5, 0, 7.5, 8.5, 10, 8.5, "mat_inox"), B(4, 10, 7, 12, 16, 9, "mat_rojo", {"north": "parada_bus_cartel", "south": "parada_bus_cartel"})],
  receta=[G("poste_senal"), mc("red_dye"), mc("paper")])
E("parquimetro", "Parquímetro", "CARRETERAS", "mueble", "metal",
  geo=[B(7, 0, 7, 9, 8, 9, "mat_gris"), B(5, 8, 6, 11, 16, 10, "mat_azul", {"north": "parquimetro_frente"})],
  receta=[mc("iron_ingot"), mc("gold_nugget"), mc("blue_dye"), mc("redstone")])
E("fuente_agua", "Fuente de agua potable", "CARRETERAS", "mueble", "metal", accion="GRIFO",
  geo=[B(5, 0, 5, 11, 2, 11, "mat_verde_oscuro"), B(6, 2, 6, 10, 12, 10, "mat_verde_oscuro"), B(4, 12, 4, 12, 13, 12, "mat_verde_oscuro"),
       B(5, 13, 5, 11, 14, 11, "mat_cromo"), B(7.5, 14, 7.5, 8.5, 15.5, 8.5, "mat_cromo")],
  receta=[mc("water_bucket"), mc("iron_ingot"), mc("green_dye")])
E("alcorque", "Alcorque (rejilla de árbol)", "CARRETERAS", "vial", "piedra", textura="alcorque", lado="acera",
  receta=[mc("iron_bars"), mc("dirt"), mc("smooth_stone")], cantidad=4)
E("boca_riego", "Boca de riego", "CARRETERAS", "vial", "piedra", textura="boca_riego", lado="acera",
  receta=[mc("iron_ingot"), mc("smooth_stone"), mc("water_bucket"), mc("stone_button")], cantidad=2)
E("valla_obra", "Valla de obra", "CARRETERAS", "mueble", "plastico",
  geo=[B(0, 6, 7.5, 16, 12, 8.5, "valla_obra"), B(1, 0, 6, 2, 12, 10, "mat_gris"), B(14, 0, 6, 15, 12, 10, "mat_gris"),
       B(0, 0, 5, 3, 1, 11, "mat_naranja"), B(13, 0, 5, 16, 1, 11, "mat_naranja")],
  receta=[mc("red_dye"), mc("white_dye"), mc("stick"), mc("iron_nugget")], cantidad=4)
E("aparcabicis", "Aparcabicis", "CARRETERAS", "mueble", "metal",
  geo=[B(1, 0, 7.5, 2, 9, 8.5, "mat_inox"), B(14, 0, 7.5, 15, 9, 8.5, "mat_inox"), B(1, 9, 7.5, 15, 10, 8.5, "mat_inox"),
       B(5, 0, 7.5, 6, 6, 8.5, "mat_inox"), B(10, 0, 7.5, 11, 6, 8.5, "mat_inox"), B(5, 6, 7.5, 11, 7, 8.5, "mat_inox")],
  receta=[mc("iron_bars"), mc("iron_ingot"), mc("chain")], cantidad=2)
E("panel_informativo", "Panel informativo (plano de la ciudad)", "CARRETERAS", "luz_fija", "metal", luz=8,
  geo=[B(1, 0, 7, 3, 16, 9, "mat_lacado_gris"), B(13, 0, 7, 15, 16, 9, "mat_lacado_gris"),
       B(3, 4, 7.5, 13, 15, 8.5, "mat_lacado_gris", {"north": "panel_info", "south": "panel_info"})],
  receta=[mc("map"), mc("iron_ingot"), mc("glowstone_dust")])
E("radar_trafico", "Radar de tráfico", "CARRETERAS", "mueble", "metal",
  geo=[B(7, 0, 7, 9, 9, 9, "mat_gris"), B(4, 9, 5, 12, 16, 11, "mat_gris", {"north": "radar_frente"})],
  receta=[mc("observer"), mc("iron_ingot"), mc("gray_dye")])
E("espejo_trafico", "Espejo de tráfico", "CARRETERAS", "mueble", "metal",
  geo=[B(7.5, 0, 9, 8.5, 9, 10, "mat_gris"), B(3, 7, 7, 13, 16, 8, "mat_naranja", {"north": "espejo_trafico"})],
  receta=[mc("glass_pane"), mc("orange_dye"), mc("iron_ingot"), mc("stick")])
E("reductor_velocidad", "Reductor de velocidad", "CARRETERAS", "mueble", "asfalto",
  geo=[B(0, 0, 0, 16, 1, 16, "reductor"), B(0, 1, 3, 16, 2, 13, "reductor"), B(0, 2, 6, 16, 2.5, 10, "reductor")],
  receta=[mc("yellow_dye"), mc("black_dye"), mc("slime_ball"), mc("smooth_stone_slab")], cantidad=4)

# ------------------------------------------------------------------ NAVIDAD (decoración de casa)
for col, nom, tinte in (("rojo", "rojo", "red_dye"), ("azul", "azul", "blue_dye"), ("dorado", "dorado", "yellow_dye")):
    E(f"regalo_{col}", f"Regalo {nom}", "NAVIDAD", "mueble", "tela",
      geo=[B(3, 0, 3, 13, 9, 13, f"regalo_{col}"), B(2.5, 9, 2.5, 13.5, 10, 13.5, f"regalo_{col}"),
           B(6, 10, 7, 10, 12, 9, f"regalo_{col}")], receta=[mc("paper"), mc(tinte), mc("string"), mc("chest")])
E("munieco_nieve", "Muñeco de nieve", "NAVIDAD", "alto", "tela",
  geo=[B(1, 0, 1, 15, 13, 15, "nieve_munieco"), B(3, 13, 3, 13, 23, 13, "nieve_munieco", {"north": "munieco_cara"}),
       B(2, 23, 2, 14, 24, 14, "mat_negro"), B(4, 24, 4, 12, 30, 12, "mat_negro"), B(2, 13, 2, 14, 14.5, 14, "mat_rojo")],
  receta=[mc("snow_block"), mc("snow_block"), mc("carrot"), mc("coal")])
E("corona_navidad", "Corona de Navidad (puerta)", "NAVIDAD", "mueble", "tela",
  geo=[B(1, 1, 15.5, 15, 15, 16, "corona_navidad")], receta=[mc("spruce_leaves"), mc("red_dye"), mc("string")])
E("arbol_navidad", "Árbol de Navidad de casa", "NAVIDAD", "alto", "hojas",
  geo=[B(6, 0, 6, 10, 3, 10, "mat_rojo"), B(1, 3, 1, 15, 9, 15, "arbol_navidad_ramas"), B(2.5, 9, 2.5, 13.5, 15, 13.5, "arbol_navidad_ramas"),
       B(4, 15, 4, 12, 21, 12, "arbol_navidad_ramas"), B(5.5, 21, 5.5, 10.5, 26, 10.5, "arbol_navidad_ramas"),
       B(7, 26, 7, 9, 29, 9, "mat_luz_calida")],
  receta=[mc("spruce_sapling"), mc("glowstone_dust"), mc("red_dye"), mc("yellow_dye")])

# ===================================================================== generación de recursos

java = []
lang = {}
loot = []
tags = {"axe": [], "pickaxe": [], "shovel": [], "fences": [], "fence_gates": [], "doors": [], "wooden_doors": [], "beds": [],
        "slabs": [], "leaves": [], "carpets": []}
HERRAMIENTA = {"madera": "axe", "tela": "axe", "hojas": "axe", "cesped": "shovel"}


def item_bloque(nombre, modelo_nombre=None):
    mi(nombre, {"parent": f"{MOD}:block/{modelo_nombre or nombre}"})


def item_mueble(nombre, elementos, desp_z=0, escala=None):
    m = modelo(elementos, uv_explicito=True)
    disp = json.loads(json.dumps(DISPLAY_MUEBLE))
    if escala:
        for k in disp:
            disp[k]["scale"] = [v * escala for v in disp[k]["scale"]]
    m["display"] = disp
    mi(nombre, m)


def lit(v):
    return "true" if v else "false"


def jcajas(c):
    return "new double[][]{" + ", ".join("{" + ", ".join(f"{v:g}" for v in b) + "}" for b in c) + "}"


def jprops(e):
    return f'Fabrica.props("{e["mat"]}")'


def jmueble(e):
    return f'Fabrica.mueble("{e["mat"]}")'


for e in CAT:
    n = e["id"]
    t = e["tipo"]
    cat = "Categoria." + e["cat"]
    lang[f"block.{MOD}.{n}"] = e["nombre"]
    tags[HERRAMIENTA.get(e["mat"], "pickaxe")].append(G(n))
    drop_cond = None

    if t == "cubo":
        tx = e["textura"]
        render = {"render_type": "minecraft:cutout_mipped"} if e["mat"] == "hojas" else {}
        mb(n, dict({"parent": "minecraft:block/cube_all", "textures": {"all": tex(tx)}}, **render))
        bs(n, {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
        item_bloque(n)
        props = jprops(e) if e["mat"] != "hojas" else 'Fabrica.props("hojas")'
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new Block({props}));')
        if e["mat"] == "hojas":
            tags["leaves"].append(G(n))

    elif t == "losa":
        tx = tex(e["textura"])
        mb(n, {"parent": "minecraft:block/slab", "textures": {"bottom": tx, "top": tx, "side": tx}})
        mb(n + "_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": tx, "top": tx, "side": tx}})
        mb(n + "_doble", {"parent": "minecraft:block/cube_all", "textures": {"all": tx}})
        bs(n, {"variants": {"type=bottom": {"model": f"{MOD}:block/{n}"}, "type=top": {"model": f"{MOD}:block/{n}_top"},
                            "type=double": {"model": f"{MOD}:block/{n}_doble"}}})
        item_bloque(n)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new SlabBlock({jprops(e)}));')
        tags["slabs"].append(G(n))
        drop_cond = "losa"

    elif t in ("vial", "vial_orientable"):
        lado = tex(e.get("lado", "asfalto"))
        if t == "vial":
            mb(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex(e["textura"]), "side": lado, "bottom": lado}})
            bs(n, {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
            java.append(f'ModBloques.reg("{n}", {cat}, () -> new Block({jprops(e)}));')
        else:
            mb(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": tex(e["textura"]), "side": lado, "bottom": lado}})
            bs(n, {"variants": {f"facing={f}": dict({"model": f"{MOD}:block/{n}"}, **({"y": y} if y else {})) for f, y in ROT.items()}})
            java.append(f'ModBloques.reg("{n}", {cat}, () -> new BloquesVarios.BloqueOrientable({jprops(e)}, false));')
        item_bloque(n)

    elif t == "alfombra":
        mb(n, {"parent": "minecraft:block/carpet", "textures": {"wool": tex(e["textura"])}})
        bs(n, {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
        item_bloque(n)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new CarpetBlock({jprops(e)}.noOcclusion()));')
        tags["carpets"].append(G(n))

    elif t in ("mueble", "luz_fija"):
        geo = e["geo"]
        mb(n, modelo(geo))
        bs(n, variantes_facing({"": n}))
        item_mueble(n, geo)
        asiento = e.get("asiento", -1)
        huecos = e.get("huecos", 0)
        accion = e.get("accion", "NINGUNA")
        props = jmueble(e) if t == "mueble" else f'Fabrica.luzFija({jmueble(e)}, {e["luz"]})'
        expr = f'new MuebleBlock({props}, {jcajas(cajas_java(geo))}, {asiento}, {huecos}, MuebleBlock.Accion.{accion})'
        linea = f'ModBloques.reg("{n}", {cat}, () -> {expr})'
        java.append(f'ModBloques.ALMACENES.add({linea});' if huecos else linea + ";")

    elif t == "encendible":
        geo = e["geo"]
        on = cambiar_tex(geo, e.get("on", {}))
        mb(n + "_off", modelo(geo))
        mb(n + "_on", modelo(on))
        bs(n, variantes_facing({"lit=false": n + "_off", "lit=true": n + "_on"}))
        item_mueble(n, geo)
        props = f'Fabrica.luz({jmueble(e)}, {e["luz"]})' if e["luz"] else jmueble(e)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new MuebleEncendibleBlock({props}, {jcajas(cajas_java(geo))}, '
                    f'MuebleEncendibleBlock.Efecto.{e["efecto"]}));')

    elif t == "alto":
        abajo, arriba = partir(e["geo"])
        mb(n + "_abajo", modelo(abajo))
        mb(n + "_arriba", modelo(arriba))
        bs(n, variantes_facing({"half=lower": n + "_abajo", "half=upper": n + "_arriba"}))
        item_mueble(n, e["geo"], escala=0.6)
        huecos = e.get("huecos", 0)
        expr = f'new MuebleAltoBlock({jmueble(e)}, {jcajas(cajas_java(abajo))}, {jcajas(cajas_java(arriba))}, {huecos})'
        linea = f'ModBloques.regAlto("{n}", {cat}, () -> {expr})'
        java.append(f'ModBloques.ALMACENES.add({linea});' if huecos else linea + ";")
        drop_cond = ("half", "lower")

    elif t == "sofa":
        geo = e["geo"]
        mb(n, modelo(geo))
        mb(n + "_brazo_izq", modelo(g_brazo(e["brazo"], True)))
        mb(n + "_brazo_der", modelo(g_brazo(e["brazo"], False)))
        mp = []
        for f, y in ROT.items():
            r = {"y": y} if y else {}
            mp.append({"when": {"facing": f}, "apply": dict({"model": f"{MOD}:block/{n}"}, **r)})
            mp.append({"when": {"facing": f, "izq": "false"}, "apply": dict({"model": f"{MOD}:block/{n}_brazo_izq"}, **r)})
            mp.append({"when": {"facing": f, "der": "false"}, "apply": dict({"model": f"{MOD}:block/{n}_brazo_der"}, **r)})
        bs(n, {"multipart": mp})
        item_mueble(n, geo + g_brazo(e["brazo"], True) + g_brazo(e["brazo"], False))
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new SofaBlock({jmueble(e)}, {jcajas(cajas_java(geo))}, {e["asiento"]}));')

    elif t == "led":
        geo = e["geo"]
        mb(n, modelo(geo))
        bs(n, {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
        item_mueble(n, desplazar(geo, 0, -12, 0))
        java.append(f'ModBloques.LEDS.add(ModBloques.reg("{n}", {cat}, () -> new LedBlock(Fabrica.luz({jmueble(e)}, {e["luz"]}), '
                    f'{jcajas(cajas_java(geo))})));')

    elif t == "tira_led":
        geo = [B(0, 0, 6.5, 16, 0.6, 9.5, "led_marco"), B(0.5, 0.6, 7, 15.5, 1, 9, "led_luz", tinte=True)]
        mb(n, modelo(geo))
        rot6 = {"up": {}, "down": {"x": 180}, "north": {"x": 90}, "south": {"x": 90, "y": 180}, "east": {"x": 90, "y": 90},
                "west": {"x": 90, "y": 270}}
        bs(n, {"variants": {f"facing={f}": dict({"model": f"{MOD}:block/{n}"}, **r) for f, r in rot6.items()}})
        item_mueble(n, desplazar(geo, 0, 6, 0))
        java.append(f'ModBloques.LEDS.add(ModBloques.reg("{n}", {cat}, () -> new TiraLedBlock(Fabrica.luz({jmueble(e)}, {e["luz"]}))));')

    elif t == "interruptor":
        base = [B(5.5, 4.5, 15, 10.5, 11.5, 16, "mat_plastico_blanco")]
        off = base + [B(6.5, 5.5, 14.4, 9.5, 10.5, 15, "mat_plastico_blanco", rot={"origin": [8, 8, 15], "axis": "x", "angle": -22.5})]
        on = base + [B(6.5, 5.5, 14.4, 9.5, 10.5, 15, "mat_plastico_blanco", rot={"origin": [8, 8, 15], "axis": "x", "angle": 22.5})]
        mb(n + "_off", modelo(off))
        mb(n + "_on", modelo(on))
        bs(n, variantes_facing({"lit=false": n + "_off", "lit=true": n + "_on"}))
        item_mueble(n, desplazar(off, 0, 0, -7), escala=1.6)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new InterruptorBlock({jmueble(e)}));')

    elif t == "persiana":
        tx = e["textura"]
        bajada = [B(0, 0, 7, 16, 16, 9, tx)]
        subida = [B(0, 12, 6, 16, 16, 10, tx, {"north": "mat_lacado_gris", "south": "mat_lacado_gris", "down": tx})]
        mb(n + "_bajada", modelo(bajada))
        mb(n + "_subida", modelo(subida))
        bs(n, variantes_facing({"open=false": n + "_bajada", "open=true": n + "_subida"}))
        item_mueble(n, bajada)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new PersianaBlock({jmueble(e)}));')

    elif t == "poste":
        geo = [B(7, 0, 7, 9, 16, 9, "poste_senal")]
        mb(n, modelo(geo))
        bs(n, {"variants": {"": {"model": f"{MOD}:block/{n}"}}})
        item_mueble(n, geo)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new BloquesVarios.BloqueForma({jmueble(e)}, 7, 0, 7, 9, 16, 9));')

    elif t == "senal":
        s = e["senal"]
        geo = [B(7, 0, 7, 9, 16, 9, "poste_senal"),
               B(1, 1, 6.4, 15, 15, 6.9, f"senal_{s}", {"south": f"senal_{s}_dorso"}, solo=["north", "south"]),
               B(7.2, 5, 6.9, 8.8, 6, 7, "poste_senal"), B(7.2, 11, 6.9, 8.8, 12, 7, "poste_senal")]
        mb(n, modelo(geo, render="minecraft:cutout"))
        bs(n, variantes_facing({"": n}))
        mi(n, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/senal_{s}"}})
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new SenalBlock({jmueble(e)}));')

    elif t == "semaforo":
        modelos = {}
        luces = (0, 2) if e["peatonal"] else (0, 1, 2)
        for l in (0, 1, 2):
            tl = f"semaforo_peatones_{0 if l < 2 else 2}" if e["peatonal"] else f"semaforo_{l}"
            if e["peatonal"]:
                geo = [B(7, 0, 7, 9, 3, 9, "poste_senal"), B(4.5, 3, 5, 11.5, 16, 9, "semaforo_carcasa", {"north": tl}),
                       B(4.2, 9.5, 4.2, 11.8, 10, 5, "semaforo_carcasa"), B(4.2, 15.5, 4.2, 11.8, 16, 5, "semaforo_carcasa")]
            else:
                geo = [B(7, 0, 7, 9, 1, 9, "poste_senal"), B(5, 1, 5, 11, 16, 9, "semaforo_carcasa", {"north": tl})] + \
                      [B(4.7, y, 3.6, 11.3, y + 0.5, 5, "semaforo_carcasa") for y in (5.6, 10.6, 15.5)] + \
                      [B(3, 1, 8.8, 13, 16, 9.2, "semaforo_carcasa")]
            mb(f"{n}_{l}", modelo(geo))
            modelos[f"luz={l}"] = f"{n}_{l}"
        bs(n, variantes_facing(modelos))
        item_mueble(n, geo)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new SemaforoBlock(Fabrica.luzFija({jmueble(e)}, 6), {str(e["peatonal"]).lower()}));')

    elif t == "cama":
        mb(n + "_cabeza", modelo(e["cabeza"]))
        mb(n + "_pies", modelo(e["pies"]))
        v = {}
        for f, y in ROT.items():
            for parte, mod in (("head", n + "_cabeza"), ("foot", n + "_pies")):
                d = {"model": f"{MOD}:block/{mod}"}
                if y:
                    d["y"] = y
                v[f"facing={f},part={parte}"] = d
        bs(n, {"variants": v})
        item_mueble(n, desplazar(e["cabeza"] + desplazar(e["pies"], 0, 0, 16), 0, 0, -8), escala=0.55)
        java.append(f'ModBloques.regCama("{n}", {cat}, () -> new CamaBlock({jmueble(e)}.strength(0.4f)));')
        tags["beds"].append(G(n))
        drop_cond = ("part", "head")

    elif t == "puerta":
        abajo, arriba = tex(n + "_abajo"), tex(n + "_arriba")
        render = "minecraft:translucent" if "cristal" in n else "minecraft:cutout"
        variantes = {}
        for f, y in {"east": 0, "south": 90, "west": 180, "north": 270}.items():
            for half in ("lower", "upper"):
                for hinge in ("left", "right"):
                    for abierta in ("false", "true"):
                        parte = "bottom" if half == "lower" else "top"
                        mod = f"{MOD}:block/{n}_{parte}_{hinge}" + ("_open" if abierta == "true" else "")
                        rot = y
                        if abierta == "true":
                            rot = (y + 90) % 360 if hinge == "left" else (y + 270) % 360
                        d = {"model": mod}
                        if rot:
                            d["y"] = rot
                        variantes[f"facing={f},half={half},hinge={hinge},open={abierta}"] = d
        bs(n, {"variants": variantes})
        for parte in ("bottom", "top"):
            for hinge in ("left", "right"):
                for suf in ("", "_open"):
                    mb(f"{n}_{parte}_{hinge}{suf}", {"parent": f"minecraft:block/door_{parte}_{hinge}{suf}", "render_type": render,
                                                    "textures": {"bottom": abajo, "top": arriba}})
        mi(n, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{n}"}})
        fuerte = ".strength(12.0f, 30.0f)" if "acorazada" in n else ""
        java.append(f'ModBloques.regAlto("{n}", {cat}, () -> new DoorBlock({jmueble(e)}{fuerte}, BlockSetType.OAK));')
        tags["doors"].append(G(n))
        if e["mat"] == "madera":
            tags["wooden_doors"].append(G(n))
        drop_cond = ("half", "lower")

    elif t == "valla":
        tx = tex(e["textura"])
        mb(n + "_poste", {"parent": "minecraft:block/fence_post", "textures": {"texture": tx}})
        mb(n + "_lado", {"parent": "minecraft:block/fence_side", "textures": {"texture": tx}})
        mb(n + "_inventario", {"parent": "minecraft:block/fence_inventory", "textures": {"texture": tx}})
        bs(n, {"multipart": [{"apply": {"model": f"{MOD}:block/{n}_poste"}}] +
                            [{"when": {d: "true"}, "apply": dict({"model": f"{MOD}:block/{n}_lado", "uvlock": True}, **({"y": y} if y else {}))}
                             for d, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))]})
        item_bloque(n, n + "_inventario")
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new FenceBlock({jprops(e)}));')
        tags["fences"].append(G(n))

    elif t == "puerta_valla":
        tx = tex(e["textura"])
        for suf, par in (("", "template_fence_gate"), ("_abierta", "template_fence_gate_open"), ("_muro", "template_fence_gate_wall"),
                         ("_muro_abierta", "template_fence_gate_wall_open")):
            mb(n + suf, {"parent": f"minecraft:block/{par}", "render_type": "minecraft:cutout", "textures": {"texture": tx}})
        v = {}
        for f, y in {"south": 0, "west": 90, "north": 180, "east": 270}.items():
            for muro in ("false", "true"):
                for ab in ("false", "true"):
                    suf = ("_muro" if muro == "true" else "") + ("_abierta" if ab == "true" else "")
                    d = {"model": f"{MOD}:block/{n}{suf}", "uvlock": True}
                    if y:
                        d["y"] = y
                    v[f"facing={f},in_wall={muro},open={ab}"] = d
        bs(n, {"variants": v})
        item_bloque(n)
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new FenceGateBlock({jmueble(e)}, WoodType.OAK));')
        tags["fence_gates"].append(G(n))

    elif t == "panel":
        tx, canto = tex(e["textura"]), tex(e["canto"])
        render = "minecraft:translucent" if n.startswith("ventana") else "minecraft:cutout"
        for suf in ("post", "side", "side_alt", "noside", "noside_alt"):
            mb(f"{n}_{suf}", {"parent": f"minecraft:block/template_glass_pane_{suf}", "render_type": render,
                              "textures": {"pane": tx, "edge": canto}})
        P = f"{MOD}:block/{n}_"
        bs(n, {"multipart": [
            {"apply": {"model": P + "post"}},
            {"when": {"north": "true"}, "apply": {"model": P + "side"}},
            {"when": {"east": "true"}, "apply": {"model": P + "side", "y": 90}},
            {"when": {"south": "true"}, "apply": {"model": P + "side_alt"}},
            {"when": {"west": "true"}, "apply": {"model": P + "side_alt", "y": 90}},
            {"when": {"north": "false"}, "apply": {"model": P + "noside"}},
            {"when": {"east": "false"}, "apply": {"model": P + "noside_alt"}},
            {"when": {"south": "false"}, "apply": {"model": P + "noside_alt", "y": 90}},
            {"when": {"west": "false"}, "apply": {"model": P + "noside", "y": 270}}]})
        icono = f"{MOD}:item/{n}" if os.path.exists(os.path.join(A, "textures", "item", n + ".png")) else tx
        mi(n, {"parent": "minecraft:item/generated", "textures": {"layer0": icono}})
        java.append(f'ModBloques.reg("{n}", {cat}, () -> new PanelBlock({jmueble(e)}));')
    else:
        raise ValueError(t)

    # botín
    entrada = {"type": "minecraft:item", "name": G(n)}
    pool = {"rolls": 1, "entries": [entrada], "conditions": [{"condition": "minecraft:survives_explosion"}]}
    if drop_cond == "losa":
        entrada["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                                 "conditions": [{"condition": "minecraft:block_state_property", "block": G(n), "properties": {"type": "double"}}]},
                                {"function": "minecraft:explosion_decay"}]
    elif drop_cond:
        entrada["conditions"] = [{"condition": "minecraft:block_state_property", "block": G(n), "properties": {drop_cond[0]: drop_cond[1]}}]
    escribir(os.path.join(D, "loot_tables", "blocks", n + ".json"), {"type": "minecraft:block", "pools": [pool]})

    # receta (sin forma)
    if e["receta"]:
        ings = [{"tag": i[1:]} if i.startswith("#") else {"item": i} for i in e["receta"]]
        escribir(os.path.join(D, "recipes", n + ".json"), {"type": "minecraft:crafting_shapeless", "category": "building",
                                                          "ingredients": ings, "result": {"item": G(n), "count": e["cantidad"]}})

# ------------------------------------------------------------------ comprobar recetas repetidas (todas las del mod)
firmas = {}
for f in os.listdir(os.path.join(D, "recipes")):
    r = json.load(open(os.path.join(D, "recipes", f), encoding="utf-8"))
    if r["type"] == "minecraft:crafting_shapeless":
        k = ("sl", tuple(sorted(json.dumps(i, sort_keys=True) for i in r["ingredients"])))
    else:
        k = ("sh", tuple(r["pattern"]), tuple(sorted((c, json.dumps(v, sort_keys=True)) for c, v in r["key"].items())))
    firmas.setdefault(k, []).append(f)
repes = [v for v in firmas.values() if len(v) > 1]
if repes:
    raise SystemExit("Recetas repetidas: " + "; ".join(" = ".join(v) for v in repes))

# ------------------------------------------------------------------ etiquetas


def anadir_tag(carpeta, nombre, valores):
    ruta = os.path.join(carpeta, "tags", "blocks", nombre + ".json")
    previo = []
    if os.path.exists(ruta):
        previo = json.load(open(ruta, encoding="utf-8"))["values"]
    todo = previo + [v for v in valores if v not in previo]
    escribir(ruta, {"replace": False, "values": todo})


anadir_tag(DMC, "mineable/axe", tags["axe"])
anadir_tag(DMC, "mineable/pickaxe", tags["pickaxe"])
anadir_tag(DMC, "mineable/shovel", tags["shovel"])
for k in ("fences", "fence_gates", "doors", "wooden_doors", "beds", "slabs", "leaves", "carpets"):
    if tags[k]:
        anadir_tag(DMC, k, tags[k])
anadir_tag(D, "a_prueba_de_balas", [G("puerta_acorazada"), G("barrera_new_jersey"), G("guardarrail"), G("granito_avila"),
                                    G("hormigon_pulido"), G("bordillo")])
anadir_tag(D, "anti_radiacion", [G("hormigon_pulido")])
escribir(os.path.join(DMC, "tags", "items", "doors.json"), {"replace": False, "values": tags["doors"]})

# ------------------------------------------------------------------ Java
cabecera = """package es.guerramod.registro;

import es.guerramod.bloques.BloquesVarios;
import es.guerramod.bloques.hogar.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/** GENERADO por herramientas/v2_catalogo.py: no editar a mano. */
public final class CatalogoGenerado {
    private CatalogoGenerado() {}

"""
# en varios métodos para no pasar el límite de tamaño de método de Java
trozos = [java[i:i + 40] for i in range(0, len(java), 40)]
cuerpo = "    public static void registrar() {\n" + "".join(f"        parte{i}();\n" for i in range(len(trozos))) + "    }\n"
for i, tr in enumerate(trozos):
    cuerpo += f"\n    private static void parte{i}() {{\n" + "".join("        " + l + "\n" for l in tr) + "    }\n"
open(JAVA, "w", encoding="utf-8").write(cabecera + cuerpo + "}\n")

# ===================================================================== ARMAS EN 3D
TI_ = lambda n: f"{MOD}:item/{n}"
PAV, POL, MAD, VER, ARE, MET, LAT, LEN, PUN = (TI_(x) for x in ("arma_pavonado", "arma_polimero", "arma_madera", "arma_verde",
                                                                 "arma_arena", "arma_metal_claro", "arma_laton", "arma_lente", "arma_punto_rojo"))

ARMAS = {
    "pistola": dict(agarre=(3.5, 12), terc=0.32, geo=[
        B(7, 8.2, 2, 9, 10.6, 14, PAV), B(8.95, 9.4, 6.8, 9.05, 10.3, 9.4, MET), B(7.6, 8.7, 1.6, 8.4, 9.7, 2.0, POL),
        B(7.1, 6.8, 3, 8.9, 8.2, 13.5, POL), B(7.2, 6.2, 3, 8.8, 6.8, 7, POL), B(7.4, 4.6, 6.8, 8.6, 5.0, 10.5, POL),
        B(7.4, 5.0, 6.8, 8.6, 6.8, 7.2, POL), B(7.8, 5.4, 9, 8.2, 6.8, 9.6, MET), B(7.05, 4, 10, 8.95, 6.8, 13.4, POL),
        B(7.05, 0.6, 10.8, 8.95, 4, 14.2, POL), B(7, 0.2, 10.8, 9, 0.6, 14.3, PAV), B(7.7, 10.6, 2.6, 8.3, 11.3, 3.4, PAV),
        B(7.1, 10.6, 12.6, 7.7, 11.3, 13.4, PAV), B(8.3, 10.6, 12.6, 8.9, 11.3, 13.4, PAV)]),
    "subfusil": dict(agarre=(4.5, 11.5), terc=0.42, geo=[
        B(7, 7.2, 1, 9, 10.2, 13, PAV), B(7.6, 8.1, -2, 8.4, 9.1, 1, PAV), B(6.8, 6.6, 1.2, 9.2, 8.8, 6, POL),
        B(7.3, 3, 6.5, 8.7, 7.2, 8.2, PAV), B(7.3, 0, 7.2, 8.7, 3, 9.0, PAV), B(7.1, 2.5, 10.4, 8.9, 7.2, 12.6, POL),
        B(7.4, 5.2, 8.8, 8.6, 5.6, 10.4, POL), B(7.2, 8.6, 13, 7.6, 9.4, 20, PAV), B(8.4, 8.6, 13, 8.8, 9.4, 20, PAV),
        B(7, 6, 19.5, 9, 10, 20.8, POL), B(7.4, 10.2, 1.4, 8.6, 11.8, 2.4, PAV), B(7.3, 10.2, 13, 8.7, 11.8, 14.2, PAV),
        B(7, 10.2, 1.5, 9, 10.8, 6, PAV), B(6.4, 9.6, 3, 7, 10.2, 3.6, MET)]),
    "fusil": dict(agarre=(4, 12), terc=0.42, geo=[
        B(7, 8.6, 0, 9, 10.6, 14, POL), B(7.1, 6.6, 2, 8.9, 8.6, 12, PAV), B(7.2, 10.6, -6, 8.8, 11.0, 14, POL),
        B(6.7, 7.4, -7, 9.3, 10.6, 0, ARE), B(7.65, 8.4, -11, 8.35, 9.1, -7, PAV), B(7.4, 8.1, -8, 8.6, 9.4, -7, PAV),
        B(7.5, 8.25, -12, 8.5, 9.25, -11, POL), B(7.7, 11, -5.5, 8.3, 12.5, -4.8, POL),
        B(7.3, 11, 7, 8.7, 11.4, 10.5, POL), B(7.1, 11.4, 7, 7.4, 13.4, 10.2, POL), B(8.6, 11.4, 7, 8.9, 13.4, 10.2, POL),
        B(7.1, 13.1, 7, 8.9, 13.4, 10.2, POL), B(7.4, 11.4, 7.2, 8.6, 13.1, 7.4, PUN, {"south": LEN}),
        B(7.3, 3, 4, 8.7, 6.6, 6.4, POL), B(7.3, 0.2, 3.4, 8.7, 3, 5.8, POL), B(7.1, 3.8, 10.6, 8.9, 6.6, 12.6, POL),
        B(7.1, 1, 11.2, 8.9, 3.8, 13.2, POL), B(7.4, 5.2, 8, 8.6, 5.6, 10.6, POL), B(7.85, 5.6, 9, 8.15, 6.6, 9.4, MET),
        B(7.4, 8.0, 14, 8.6, 9.6, 20, POL), B(7, 6.2, 18, 9, 10.4, 26, POL), B(7.6, 10.1, 13.5, 8.4, 10.6, 14.6, MET),
        B(8.95, 8.9, 4, 9.05, 10.1, 7, MET), B(7.3, 4.5, -5, 8.7, 7.4, -3.5, POL)]
        + [B(7.25, 11.0, z, 8.75, 11.25, z + 0.6) if False else B(7.25, 11.0, z, 8.75, 11.25, z + 0.6, POL) for z in [x * 1.2 - 6 for x in range(5)]]),
    "escopeta": dict(agarre=(5, 13.5), terc=0.42, geo=[
        B(7.5, 9, -10, 8.5, 10, 8, PAV), B(7.55, 7.6, -8, 8.45, 8.9, 6, PAV), B(7.0, 7.2, -4, 9.0, 9.3, 2, MAD),
        B(7, 7.2, 8, 9, 10.5, 15, PAV), B(7.85, 10, -9.5, 8.15, 10.5, -9, LAT), B(7.4, 5.6, 10, 8.6, 6, 13, PAV),
        B(7.85, 6, 11, 8.15, 7.2, 11.4, MET), B(7.1, 5.6, 14.5, 8.9, 9.2, 17, MAD), B(7, 5, 16.5, 9, 9.4, 20, MAD),
        B(7, 3.8, 20, 9, 8.8, 26, MAD), B(7, 3.8, 25.5, 9, 8.8, 26.2, POL), B(8.95, 8.2, 9, 9.05, 9.6, 12, MET)]),
    "francotirador": dict(agarre=(5.5, 15.5), terc=0.4, geo=[
        B(7.55, 8.45, -16, 8.45, 9.35, 2, PAV), B(7.35, 8.25, -16, 8.65, 9.55, -14.5, PAV), B(7, 6.6, -3, 9, 9.2, 6, ARE),
        B(7.1, 8.2, 2, 8.9, 10.4, 15, PAV), B(8.9, 9, 13, 10.6, 9.6, 13.6, MET), B(10.3, 8.4, 12.8, 11, 9.2, 13.8, PAV),
        B(7, 5.6, 6, 9, 9.2, 14, ARE), B(7.1, 4, 14, 8.9, 8.8, 17, ARE), B(7, 3.8, 17, 9, 10, 29, ARE),
        B(7.2, 10, 19, 8.8, 10.8, 25, ARE), B(7, 3.8, 28.5, 9, 10, 29.6, POL), B(7.3, 4.6, 8, 8.7, 6.6, 11, POL),
        B(7.4, 4.8, 12, 8.6, 5.2, 14.5, POL), B(7.3, 11.2, 2, 8.7, 12.8, 18, PAV), B(6.9, 10.8, 1, 9.1, 13.2, 4, PAV),
        B(7.05, 10.95, 15.5, 8.95, 13.05, 18.5, PAV, {"south": LEN}), B(7.6, 12.8, 8.5, 8.4, 13.6, 10, PAV),
        B(8.7, 11.6, 8.5, 9.5, 12.4, 10, PAV), B(7.5, 10.4, 5, 8.5, 11.2, 6, PAV), B(7.5, 10.4, 13, 8.5, 11.2, 14, PAV),
        B(7.0, 10.9, 0.9, 9.0, 13.1, 1.0, LEN), B(7.6, 7.6, -6, 7.9, 8.0, 1, MET), B(8.1, 7.6, -6, 8.4, 8.0, 1, MET)]),
    "ametralladora": dict(agarre=(5, 12), terc=0.42, geo=[
        B(6.8, 7.2, 0, 9.2, 11, 14, PAV), B(6.9, 11, 2, 9.1, 11.4, 9, PAV), B(7.5, 8.3, -14, 8.5, 9.3, 0, PAV),
        B(7.2, 8, -9, 8.8, 9.6, 0, MET), B(7.4, 8.2, -14, 8.6, 9.4, -12.5, POL), B(7.6, 11, -4, 8.4, 11.6, -1, POL),
        B(7.7, 9.6, -12, 8.3, 11.9, -11.4, PAV), B(7.4, 11.4, 10.6, 7.9, 12.0, 11.4, PAV), B(8.1, 11.4, 10.6, 8.6, 12.0, 11.4, PAV),
        B(3.5, 3, 3, 6.8, 8, 9, VER), B(6, 7, 4, 6.8, 8, 7, LAT), B(7.1, 3, 11, 8.9, 7.2, 13, POL), B(7, 6, 14, 9, 10.6, 26, POL),
        B(7.2, 3, -10, 7.6, 8, -9.4, MET), B(8.4, 3, -10, 8.8, 8, -9.4, MET), B(7, 7, -3, 9, 8.2, 0, POL),
        B(7.4, 5.4, 9, 8.6, 5.8, 11, POL)]),
    "lanzacohetes": dict(agarre=(4, 7), terc=0.42, geo=[
        B(6.5, 6.5, -6, 9.5, 10.5, 24, VER), B(6, 6, 24, 10, 11, 26, VER), B(6.2, 6.2, -10, 9.8, 10.8, -6, VER),
        B(7, 7, -12, 9, 10, -10, VER), B(7.6, 7.6, -13, 8.4, 9.4, -12, MET), B(7.3, 2, 6, 8.7, 6.5, 8, POL),
        B(7.3, 2, 12, 8.7, 6.5, 14, POL), B(7.4, 10.5, 9, 8.6, 12.8, 12, POL, {"south": LEN}), B(7.4, 4.5, 7.6, 8.6, 5, 10.5, POL)]),
}

for nombre, a in ARMAS.items():
    geo = a["geo"]
    zs = [v for e in geo for v in (e["f"][2], e["t"][2])]
    ys = [v for e in geo for v in (e["f"][1], e["t"][1])]
    zc, yc = (min(zs) + max(zs)) / 2, (min(ys) + max(ys)) / 2
    largo, alto = max(zs) - min(zs), max(ys) - min(ys)
    s_gui = round(min(15.0 / largo, 13.0 / alto), 3)
    gy, gz = a["agarre"]
    s3 = a["terc"]
    m = modelo(geo, uv_explicito=True, render="minecraft:cutout")
    m["parent"] = "minecraft:item/generated" if False else "minecraft:block/block"
    m["display"] = {
        "gui": {"rotation": [0, -90, 0], "translation": [round(s_gui * (zc - 8), 2), round(-s_gui * (yc - 8), 2), 0], "scale": [s_gui] * 3},
        "fixed": {"rotation": [0, -90, 0], "translation": [round(s_gui * (zc - 8), 2), round(-s_gui * (yc - 8), 2), 0], "scale": [s_gui] * 3},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.3, 0.3, 0.3]},
        "thirdperson_righthand": {"rotation": [0, 0, 0], "translation": [0, round(-s3 * (gy - 8) - 1, 2), round(-s3 * (gz - 8), 2)], "scale": [s3] * 3},
        "thirdperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, round(-s3 * (gy - 8) - 1, 2), round(-s3 * (gz - 8), 2)], "scale": [s3] * 3},
        "firstperson_righthand": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "firstperson_lefthand": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
        "head": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
    }
    m.pop("render_type", None)
    mi(nombre, m)

for n in ("municion_556", "municion_762", "cartuchos", "casquillo", "cartucho_vacio", "mando_led", "municion"):
    mi(n, {"parent": "minecraft:item/generated", "textures": {"layer0": f"{MOD}:item/{n}"}})

# recetas de armas y munición (con forma)


def receta(nombre, patron, claves, cantidad=1):
    k = {c: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for c, v in claves.items()}
    escribir(os.path.join(D, "recipes", nombre + ".json"), {"type": "minecraft:crafting_shaped", "category": "equipment",
                                                           "pattern": patron, "key": k, "result": {"item": G(nombre), "count": cantidad}})


P, C = G("placa_acero"), G("circuito")
receta("subfusil", ["PPP", " CW", "  P"], {"P": P, "C": C, "W": mc("iron_ingot")})
receta("ametralladora", ["PPP", "PCP", "WW "], {"P": P, "C": C, "W": mc("iron_block")})
receta("municion_556", ["NCN", "NGN", "NCN"], {"N": mc("iron_nugget"), "C": mc("copper_ingot"), "G": mc("gunpowder")}, 24)
receta("municion_762", ["NCN", "GCG"], {"N": mc("iron_nugget"), "C": mc("copper_ingot"), "G": mc("gunpowder")}, 10)
receta("cartuchos", ["PNP", "GGG"], {"P": mc("paper"), "N": mc("iron_nugget"), "G": mc("gunpowder")}, 8)
receta("mando_led", ["R", "C", "Q"], {"R": mc("redstone"), "C": C, "Q": mc("quartz")})

# ===================================================================== idioma
lang.update({
    f"item.{MOD}.subfusil": "Subfusil 9 mm", f"item.{MOD}.ametralladora": "Ametralladora ligera 5,56",
    f"item.{MOD}.pistola": "Pistola 9 mm", f"item.{MOD}.fusil": "Fusil de asalto 5,56", f"item.{MOD}.escopeta": "Escopeta de corredera",
    f"item.{MOD}.francotirador": "Fusil de precisión 7,62", f"item.{MOD}.lanzacohetes": "Lanzacohetes",
    f"item.{MOD}.municion": "Munición 9 mm", f"item.{MOD}.municion_556": "Munición 5,56 mm", f"item.{MOD}.municion_762": "Munición 7,62 mm",
    f"item.{MOD}.cartuchos": "Cartuchos del calibre 12", f"item.{MOD}.casquillo": "Casquillo", f"item.{MOD}.cartucho_vacio": "Cartucho vacío",
    f"item.{MOD}.mando_led": "Mando de luces LED", f"entity.{MOD}.asiento": "Asiento",
    f"itemGroup.{MOD}.armas": "Naciones en Guerra: Armas", f"itemGroup.{MOD}.militar": "Naciones en Guerra: Militar y búnker",
    f"itemGroup.{MOD}.naciones": "Naciones en Guerra: Fronteras y gobierno", f"itemGroup.{MOD}.carreteras": "Carreteras y señales",
    f"itemGroup.{MOD}.salon": "Hogar: Salón", f"itemGroup.{MOD}.cocina": "Hogar: Cocina", f"itemGroup.{MOD}.bano": "Hogar: Baño",
    f"itemGroup.{MOD}.dormitorio": "Hogar: Dormitorio", f"itemGroup.{MOD}.puertas": "Puertas, ventanas y persianas",
    f"itemGroup.{MOD}.jardin": "Jardín y exterior", f"itemGroup.{MOD}.iluminacion": "Iluminación y LED",
    f"itemGroup.{MOD}.construccion": "Materiales de construcción",
    f"key.{MOD}.disparar": "Recargar arma / ametralladora del avión", f"key.{MOD}.modo": "Cambiar modo de fuego",
    f"death.attack.{MOD}.bala": "%1$s murió de un disparo", f"death.attack.{MOD}.bala.player": "%1$s fue abatido por %2$s",
    f"death.attack.{MOD}.bala.item": "%1$s fue abatido por %2$s con %3$s",
    f"block.{MOD}.asfalto_linea": "Carretera: línea discontinua", f"block.{MOD}.asfalto_linea_continua": "Carretera: línea continua",
    f"block.{MOD}.paso_cebra": "Paso de cebra",
})
for s in ("disparo_pistola", "disparo_subfusil", "disparo_fusil", "disparo_escopeta", "disparo_francotirador", "disparo_ametralladora",
          "cargador_fuera", "cargador_dentro", "montar", "cerrojo", "corredera", "cartucho", "vacio", "modo", "rebote", "casquillo", "clic"):
    lang[f"subtitles.{MOD}.{s}"] = {"disparo_pistola": "Disparo de pistola", "disparo_subfusil": "Ráfaga de subfusil",
                                    "disparo_fusil": "Disparo de fusil", "disparo_escopeta": "Disparo de escopeta",
                                    "disparo_francotirador": "Disparo de francotirador", "disparo_ametralladora": "Ametralladora",
                                    "cargador_fuera": "Sacan un cargador", "cargador_dentro": "Meten un cargador", "montar": "Montan el arma",
                                    "cerrojo": "Cerrojo", "corredera": "Corredera", "cartucho": "Meten un cartucho", "vacio": "Clic de arma vacía",
                                    "modo": "Selector de fuego", "rebote": "Rebote de bala", "casquillo": "Casquillo cae", "clic": "Clic"}[s]

for archivo in ("es_es.json", "en_us.json"):
    ruta = os.path.join(A, "lang", archivo)
    actual = json.load(open(ruta, encoding="utf-8")) if os.path.exists(ruta) else {}
    actual.update(lang)
    escribir(ruta, actual)

sonidos_ruta = os.path.join(A, "sounds.json")
sonidos = json.load(open(sonidos_ruta, encoding="utf-8"))
for s in ("disparo_pistola", "disparo_subfusil", "disparo_fusil", "disparo_escopeta", "disparo_francotirador", "disparo_ametralladora",
          "cargador_fuera", "cargador_dentro", "montar", "cerrojo", "corredera", "cartucho", "vacio", "modo", "rebote", "casquillo", "clic"):
    sonidos[s] = {"subtitle": f"subtitles.{MOD}.{s}", "sounds": [{"name": f"{MOD}:{s}", "attenuation_distance": 16}]}
escribir(sonidos_ruta, sonidos)

# tipo de daño de bala
escribir(os.path.join(D, "damage_type", "bala.json"), {"message_id": f"{MOD}.bala", "exhaustion": 0.1, "scaling": "when_caused_by_living_non_player"})
escribir(os.path.join(DMC, "tags", "damage_type", "is_projectile.json"), {"replace": False, "values": [G("bala")]})

# ===================================================================== comprobación de texturas
faltan = set()
for raiz, _, archivos in os.walk(os.path.join(A, "models")):
    for f in archivos:
        m = json.load(open(os.path.join(raiz, f), encoding="utf-8"))
        for k, v in m.get("textures", {}).items():
            if not v.startswith("#") and not existe_textura(v if ":" in v else "minecraft:" + v):
                faltan.add(v)
if faltan:
    raise SystemExit("Faltan texturas: " + ", ".join(sorted(faltan)))
print(f"Catálogo: {len(CAT)} bloques nuevos, {len(java)} registros Java.")

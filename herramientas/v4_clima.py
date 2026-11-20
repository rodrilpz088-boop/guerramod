#!/usr/bin/env python3
"""Recursos de la versión 4 (clima y ciudad): planos de construcción, chimeneas industriales, torre de refrigeración,
captador de CO2.  python3 herramientas/v4_clima.py  (después de v3_incendios.py)
"""
import json
import os

import numpy as np
from PIL import Image, ImageDraw

MOD = "guerramod"
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(RAIZ, "src", "main", "resources")
A = os.path.join(RES, "assets", MOD)
D = os.path.join(RES, "data", MOD)
DMC = os.path.join(RES, "data", "minecraft")
TB = os.path.join(A, "textures", "block")
TI = os.path.join(A, "textures", "item")
G = lambda n: f"{MOD}:{n}"
mc = lambda n: "minecraft:" + n
R = np.random.default_rng(404)


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=1)


def guardar(img, carpeta, nombre):
    os.makedirs(carpeta, exist_ok=True)
    img.save(os.path.join(carpeta, nombre + ".png"))


def rgb(arr):
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def ladrillos(base, junta, hollin=0.0, sem=1):
    r = np.random.default_rng(sem)
    a = np.zeros((16, 16, 3), np.float32)
    for y in range(16):
        fila = y // 4
        for x in range(16):
            xx = (x + (2 if fila % 2 else 0)) % 16
            if y % 4 == 3 or xx % 8 == 7:
                a[y, x] = junta
            else:
                a[y, x] = np.array(base) * (0.88 + 0.2 * r.random())
    if hollin:
        k = np.linspace(1 - hollin, 1, 16)[:, None, None]  # más negro arriba
        a = a * k
    return a


def hormigon(color, sem, var=10):
    r = np.random.default_rng(sem)
    return np.array(color, np.float32)[None, None, :] + r.normal(0, var, (16, 16, 1))


def boca(borde, interior=(14, 12, 12)):
    a = np.zeros((16, 16, 3), np.float32) + borde
    yy, xx = np.mgrid[0:16, 0:16]
    d = np.hypot(xx - 7.5, yy - 7.5)
    a[d < 6.2] = interior
    a[(d >= 6.2) & (d < 7.2)] = np.array(borde) * 0.6
    return a


# --------------------------------------------------------------------- texturas de bloques
guardar(rgb(ladrillos((150, 62, 42), (95, 85, 80), 0.55, 3)), TB, "chimenea_industrial_lado")
guardar(rgb(boca((120, 52, 38))), TB, "chimenea_industrial_arriba")
franja = hormigon((228, 228, 226), 5, 5)
franja[:8] = hormigon((188, 36, 34), 6, 6)[:8]
franja *= np.linspace(0.65, 1, 16)[:, None, None]
guardar(rgb(franja), TB, "chimenea_central_lado")
guardar(rgb(boca((200, 200, 198))), TB, "chimenea_central_arriba")
guardar(rgb(hormigon((222, 224, 222), 7, 6)), TB, "chimenea_vapor_lado")
guardar(rgb(boca((214, 216, 214), (70, 74, 78))), TB, "chimenea_vapor_arriba")
# captador: marco metálico con dos ventiladores
cap = hormigon((150, 156, 160), 9, 6)
yy, xx = np.mgrid[0:16, 0:16]
for cx in (4.0, 11.5):
    d = np.hypot(xx - cx, yy - 7.5)
    ang = np.arctan2(yy - 7.5, xx - cx)
    aspa = (np.sin(ang * 3) > 0.2) & (d < 3.4)
    cap[d < 3.6] = (38, 42, 46)
    cap[aspa] = (120, 200, 190)
    cap[d < 0.9] = (210, 210, 210)
cap[0, :] = cap[15, :] = cap[:, 0] = cap[:, 15] = (90, 96, 100)
cap[13:15, 2:14] = (60, 170, 120)  # piloto verde
guardar(rgb(cap), TB, "captador_co2")

# --------------------------------------------------------------------- icono de los planos (papel azul con un edificio)
ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(ic)
d.rectangle((1, 2, 14, 14), fill=(38, 92, 170, 255), outline=(22, 58, 120, 255))
d.rectangle((12, 2, 14, 14), fill=(230, 226, 210, 255))  # rollo
d.line((13, 3, 13, 13), fill=(200, 194, 176, 255))
for x0, x1, h in ((3, 5, 6), (6, 8, 9), (9, 10, 4)):
    d.rectangle((x0, 13 - h, x1, 12), outline=(225, 238, 255, 255))
d.line((3, 12, 10, 12), fill=(225, 238, 255, 255))
d.point((7, 5), fill=(255, 255, 255, 255))
guardar(ic, TI, "planos_construccion")

# --------------------------------------------------------------------- modelos y estados
for n in ("chimenea_industrial", "chimenea_central", "chimenea_vapor"):
    escribir(os.path.join(A, "models", "block", n + ".json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
        "side": G("block/" + n + "_lado"), "top": G("block/" + n + "_arriba"), "bottom": G("block/" + n + "_lado")}})
    escribir(os.path.join(A, "blockstates", n + ".json"), {"variants": {
        "lit=true": {"model": G("block/" + n)}, "lit=false": {"model": G("block/" + n)}}})
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("block/" + n)})
escribir(os.path.join(A, "models", "block", "captador_co2.json"), {"parent": "minecraft:block/cube_all", "textures": {"all": G("block/captador_co2")}})
escribir(os.path.join(A, "blockstates", "captador_co2.json"), {"variants": {"": {"model": G("block/captador_co2")}}})
escribir(os.path.join(A, "models", "item", "captador_co2.json"), {"parent": G("block/captador_co2")})
escribir(os.path.join(A, "models", "item", "planos_construccion.json"), {"parent": "minecraft:item/generated",
                                                                         "textures": {"layer0": G("item/planos_construccion")}})

# --------------------------------------------------------------------- botín, recetas y etiquetas
for n in ("chimenea_industrial", "chimenea_central", "chimenea_vapor", "captador_co2"):
    escribir(os.path.join(D, "loot_tables", "blocks", n + ".json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": G(n)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def con_forma(n, patron, clave, cantidad=1):
    escribir(os.path.join(D, "recipes", n + ".json"), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": patron,
                                                      "key": {k: {"item": v} for k, v in clave.items()}, "result": {"item": G(n), "count": cantidad}})


con_forma("planos_construccion", ["PPP", "PLP", "PPP"], {"P": mc("paper"), "L": mc("blue_dye")})
con_forma("chimenea_industrial", ["B B", "BHB", "BBB"], {"B": mc("bricks"), "H": mc("furnace")})
con_forma("chimenea_central", ["W W", "RHR", "WWW"], {"W": mc("white_concrete"), "R": mc("red_concrete"), "H": mc("blast_furnace")})
con_forma("chimenea_vapor", ["W W", "WAW", "WWW"], {"W": mc("white_concrete"), "A": mc("water_bucket")})
con_forma("captador_co2", ["IGI", "RVR", "IGI"], {"I": mc("iron_ingot"), "G": mc("iron_bars"), "R": mc("redstone"), "V": mc("moss_block")})

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


def tag(carpeta, nombre, valores, tipo="blocks"):
    ruta = os.path.join(carpeta, "tags", tipo, nombre + ".json")
    previo = json.load(open(ruta, encoding="utf-8"))["values"] if os.path.exists(ruta) else []
    escribir(ruta, {"replace": False, "values": previo + [v for v in valores if v not in previo]})


tag(DMC, "mineable/pickaxe", [G("chimenea_industrial"), G("chimenea_central"), G("chimenea_vapor"), G("captador_co2")])

# --------------------------------------------------------------------- idioma
NOMBRES = {
    "es_es.json": {
        "item.planos_construccion": "Planos de construcción", "block.chimenea_industrial": "Chimenea industrial (emite CO2)",
        "block.chimenea_central": "Chimenea de central térmica (emite mucho CO2)", "block.chimenea_vapor": "Salida de vapor (torre de refrigeración)",
        "block.captador_co2": "Captador de CO2", "itemGroup.ciudad": "Ciudad, industria y clima",
    },
    "en_us.json": {
        "item.planos_construccion": "Construction Blueprints", "block.chimenea_industrial": "Industrial Chimney (emits CO2)",
        "block.chimenea_central": "Power Plant Chimney (emits lots of CO2)", "block.chimenea_vapor": "Steam Outlet (cooling tower)",
        "block.captador_co2": "CO2 Capture Unit", "itemGroup.ciudad": "City, Industry & Climate",
    },
}
for archivo, nombres in NOMBRES.items():
    ruta = os.path.join(A, "lang", archivo)
    actual = json.load(open(ruta, encoding="utf-8"))
    for k, v in nombres.items():
        tipo, n = k.split(".", 1)
        actual[f"{tipo}.{MOD}.{n}"] = v
    escribir(ruta, actual)
print("v4 clima: recursos listos")

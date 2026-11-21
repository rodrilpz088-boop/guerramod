#!/usr/bin/env python3
"""Recursos de la versión 1.0.0: tablet municipal, ascensor y escaleras mecánicas, metro de Madrid (azulejos, andenes,
carteles de estación y de línea, tornos, máquina de billetes, vagón) y luces de Navidad animadas.
python3 herramientas/v7_ciudad.py  (después de v6_ambiente.py)
"""
import json
import os
import sys

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

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
R = np.random.default_rng(1000)
FUENTE = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
FUENTE_COND = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
N = 32

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=1)


def guardar(img, carpeta, nombre):
    os.makedirs(carpeta, exist_ok=True)
    img.save(os.path.join(carpeta, nombre + ".png"))


def animada(frames, nombre, tiempo=4, interp=True):
    """Guarda una textura animada (tira vertical + .mcmeta)."""
    w, h = frames[0].size
    tira = Image.new("RGBA", (w, h * len(frames)))
    for i, f in enumerate(frames):
        tira.paste(f, (0, i * h))
    guardar(tira, TB, nombre)
    with open(os.path.join(TB, nombre + ".png.mcmeta"), "w") as f:
        json.dump({"animation": {"frametime": tiempo, "interpolate": interp}}, f)


def fuente(t, cond=False):
    try:
        return ImageFont.truetype(FUENTE_COND if cond else FUENTE, t)
    except OSError:
        return ImageFont.truetype(FUENTE, t)


def arr(img):
    return np.array(img).astype(float)


def img(a):
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")


def ruido(base, var, n=N, sem=None):
    r = np.random.default_rng(sem) if sem is not None else R
    a = np.zeros((n, n, 4))
    a[..., :3] = np.array(base, float) + r.normal(0, var, (n, n, 1))
    a[..., 3] = 255
    return img(a)


def cepillado(base, var=6, n=N, vertical=False, sem=None):
    r = np.random.default_rng(sem) if sem is not None else R
    vetas = r.normal(0, var, (1, n)) if vertical else r.normal(0, var, (n, 1))
    a = np.zeros((n, n, 4))
    a[..., :3] = np.array(base, float) + vetas[..., None] + r.normal(0, var * 0.3, (n, n, 1))
    a[..., 3] = 255
    return img(a)


def brillo(im, radio=1.5, fuerza=1.0):
    """Halo de luz alrededor de lo que brilla (bombillas)."""
    a = arr(im)
    halo = arr(im.filter(ImageFilter.GaussianBlur(float(radio))))
    a[..., :3] = np.maximum(a[..., :3], halo[..., :3] * fuerza)
    a[..., 3] = np.maximum(a[..., 3], halo[..., 3] * 0.9)
    return img(a)


lang_es, lang_en = {}, {}


def nombre(clave, es, en):
    lang_es[clave] = es
    lang_en[clave] = en


def bloque_simple(n, es, en, modelo=None):
    escribir(os.path.join(A, "models", "block", n + ".json"), modelo or {"parent": "minecraft:block/cube_all", "textures": {"all": G("block/" + n)}})
    escribir(os.path.join(A, "blockstates", n + ".json"), {"variants": {"": {"model": G("block/" + n)}}})
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("block/" + n)})
    nombre(f"block.{MOD}.{n}", es, en)


ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def estados_facing(n, modelo_de=None, extra=None):
    if modelo_de is None:
        modelo_de = lambda f: G("block/" + n)
    v = {}
    for f, y in ROT.items():
        if extra:
            for k, m in extra.items():
                d = {"model": m}
                if y:
                    d["y"] = y
                v[f"facing={f},{k}"] = d
        else:
            d = {"model": modelo_de(f)}
            if y:
                d["y"] = y
            v[f"facing={f}"] = d
    escribir(os.path.join(A, "blockstates", n + ".json"), {"variants": v})


def cara(t, uv=None, tinte=False):
    c = {"texture": t}
    if uv:
        c["uv"] = uv
    return c


def caja(f, t, caras, render=None):
    return {"from": f, "to": t, "faces": caras}


def placa(n, es, en, frente, lado, x0=0, y0=0, x1=16, y1=16, grosor=1, render="minecraft:cutout", luz=False, item_plano=True):
    """Placa pegada a la pared (lado sur del bloque, mirando al norte)."""
    z0 = 16 - grosor
    el = {"from": [x0, y0, z0], "to": [x1, y1, 16], "faces": {
        "north": {"texture": "#frente", "uv": [0, 0, 16, 16]},
        "south": {"texture": "#lado", "uv": [0, 0, 16, 16]},
        "east": {"texture": "#lado", "uv": [0, 0, 1, 16]}, "west": {"texture": "#lado", "uv": [0, 0, 1, 16]},
        "up": {"texture": "#lado", "uv": [0, 0, 16, 1]}, "down": {"texture": "#lado", "uv": [0, 0, 16, 1]}}}
    if luz:
        el["shade"] = False
    m = {"parent": "minecraft:block/block", "render_type": render, "textures": {"frente": frente, "lado": lado, "particle": frente},
         "elements": [el]}
    escribir(os.path.join(A, "models", "block", n + ".json"), m)
    estados_facing(n)
    if item_plano:
        escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": frente}})
    else:
        escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("block/" + n)})
    nombre(f"block.{MOD}.{n}", es, en)


def item_plano(n, es, en, tex=None):
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex or G("item/" + n)}})
    nombre(f"item.{MOD}.{n}", es, en)


BOTIN = []


def botin(n):
    BOTIN.append(n)
    escribir(os.path.join(D, "loot_tables", "blocks", n + ".json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": [{"type": "minecraft:item", "name": G(n)}], "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


def receta(n, patron, clave, cantidad=1):
    escribir(os.path.join(D, "recipes", n + ".json"), {"type": "minecraft:crafting_shaped", "category": "building", "pattern": patron,
                                                      "key": {k: ({"tag": v[1:]} if v.startswith("#") else {"item": v}) for k, v in clave.items()},
                                                      "result": {"item": G(n), "count": cantidad}})


def sin_forma(n, ings, cantidad=1):
    escribir(os.path.join(D, "recipes", n + ".json"), {"type": "minecraft:crafting_shapeless", "category": "building",
                                                      "ingredients": [({"tag": i[1:]} if i.startswith("#") else {"item": i}) for i in ings],
                                                      "result": {"item": G(n), "count": cantidad}})


# =========================================================================== TABLET MUNICIPAL
ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(ic)
d.rounded_rectangle((1, 2, 14, 13), 2, fill=(30, 32, 38, 255))
d.rectangle((2, 3, 13, 12), fill=(36, 74, 120, 255))
d.rectangle((3, 4, 12, 11), fill=(208, 230, 238, 255))
d.rectangle((3, 4, 12, 5), fill=(176, 40, 60, 255))
d.rectangle((4, 6, 6, 9), fill=(120, 140, 160, 255))
d.point((5, 7), fill=(240, 210, 180, 255))
d.line((8, 7, 11, 7), fill=(60, 70, 80, 255))
d.line((8, 9, 10, 9), fill=(60, 70, 80, 255))
d.point((14, 7), fill=(90, 90, 96, 255))
guardar(ic, TI, "tablet_municipal")
item_plano("tablet_municipal", "Tablet municipal", "Municipal Tablet")
receta("tablet_municipal", ["IGI", "IRI", "IPI"], {"I": mc("iron_ingot"), "G": mc("glass_pane"), "R": mc("redstone"), "P": mc("paper")})

# =========================================================================== ASCENSOR Y ESCALERAS MECÁNICAS
# suelo de la cabina: chapa lagrimada con marco cromado
im = cepillado((150, 154, 160), 5, sem=41)
d = ImageDraw.Draw(im)
for y in range(2, N, 6):
    for x in range(2 + (y // 6 % 2) * 3, N, 6):
        d.line((x, y + 1, x + 2, y), fill=(196, 200, 206, 255))
        d.line((x, y + 2, x + 2, y + 1), fill=(96, 100, 106, 255))
d.rectangle((0, 0, N - 1, N - 1), outline=(215, 220, 228, 255), width=2)
guardar(im, TB, "ascensor_arriba")
im = cepillado((168, 172, 178), 6, vertical=True, sem=42)
d = ImageDraw.Draw(im)
d.rectangle((0, 0, N - 1, 3), fill=(90, 94, 100, 255))
d.rectangle((0, 1, N - 1, 2), fill=(255, 236, 190, 255))
d.rectangle((0, N - 3, N - 1, N - 1), fill=(90, 94, 100, 255))
guardar(im, TB, "ascensor_lado")
bloque_simple("ascensor", "Ascensor (suelo de cabina)", "Elevator (cabin floor)",
              {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": G("block/ascensor_arriba"), "side": G("block/ascensor_lado"),
                                                                       "bottom": G("block/ascensor_lado")}})
botin("ascensor")
receta("ascensor", ["III", "RER", "III"], {"I": mc("iron_ingot"), "R": mc("redstone"), "E": mc("ender_pearl")}, 2)

# peldaños de la escalera mecánica (las ranuras se mueven)
frames = []
for k in range(8):
    im = Image.new("RGBA", (N, N), (120, 124, 130, 255))
    d = ImageDraw.Draw(im)
    for y in range(-4, N + 4, 4):
        yy = y + k * 4 // 8 * 1 + (k % 8) // 2
        d.line((0, yy, N - 1, yy), fill=(70, 72, 78, 255))
        d.line((0, yy + 1, N - 1, yy + 1), fill=(170, 174, 180, 255))
    d.rectangle((0, 0, 1, N - 1), fill=(240, 200, 30, 255))
    d.rectangle((N - 2, 0, N - 1, N - 1), fill=(240, 200, 30, 255))
    frames.append(im)
animada(frames, "escalera_mecanica_peldano", 2, False)
im = cepillado((60, 62, 68), 4, sem=43)
d = ImageDraw.Draw(im)
d.rectangle((0, 0, N - 1, 2), fill=(20, 20, 22, 255))
d.line((0, 1, N - 1, 1), fill=(240, 200, 30, 255))
guardar(im, TB, "escalera_mecanica_lado")
modelo_esc = {"parent": "minecraft:block/block", "textures": {"peldano": G("block/escalera_mecanica_peldano"),
                                                              "lado": G("block/escalera_mecanica_lado"),
                                                              "particle": G("block/escalera_mecanica_lado")},
              "elements": [
                  {"from": [0, 0, 0], "to": [16, 8, 16], "faces": {
                      "up": {"texture": "#peldano", "uv": [0, 8, 16, 16]}, "down": {"texture": "#lado"},
                      "south": {"texture": "#peldano", "uv": [0, 0, 16, 8]}, "north": {"texture": "#lado", "uv": [0, 8, 16, 16]},
                      "east": {"texture": "#lado", "uv": [0, 8, 16, 16]}, "west": {"texture": "#lado", "uv": [0, 8, 16, 16]}}},
                  {"from": [0, 8, 0], "to": [16, 16, 8], "faces": {
                      "up": {"texture": "#peldano", "uv": [0, 0, 16, 8]}, "south": {"texture": "#peldano", "uv": [0, 0, 16, 8]},
                      "north": {"texture": "#lado", "uv": [0, 0, 16, 8]}, "east": {"texture": "#lado", "uv": [8, 0, 16, 8]},
                      "west": {"texture": "#lado", "uv": [0, 0, 8, 8]}}},
                  {"from": [-0.5, 0, 0], "to": [0, 20, 16], "faces": {
                      "west": {"texture": "#lado"}, "east": {"texture": "#lado"}, "up": {"texture": "#lado"}, "north": {"texture": "#lado"},
                      "south": {"texture": "#lado"}}},
                  {"from": [16, 0, 0], "to": [16.5, 20, 16], "faces": {
                      "west": {"texture": "#lado"}, "east": {"texture": "#lado"}, "up": {"texture": "#lado"}, "north": {"texture": "#lado"},
                      "south": {"texture": "#lado"}}}]}
escribir(os.path.join(A, "models", "block", "escalera_mecanica.json"), modelo_esc)
for n, es, en in (("escalera_mecanica", "Escalera mecánica (subida)", "Escalator (up)"),
                  ("escalera_mecanica_bajada", "Escalera mecánica (bajada)", "Escalator (down)")):
    estados_facing(n, lambda f: G("block/escalera_mecanica"))
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("block/escalera_mecanica")})
    nombre(f"block.{MOD}.{n}", es, en)
    botin(n)
receta("escalera_mecanica", ["  I", " IR", "III"], {"I": mc("iron_ingot"), "R": mc("redstone")}, 4)
receta("escalera_mecanica_bajada", ["I  ", "RI ", "III"], {"I": mc("iron_ingot"), "R": mc("redstone")}, 4)

# panel de botones
im = cepillado((190, 194, 200), 5, sem=44)
d = ImageDraw.Draw(im)
d.rectangle((9, 2, 22, 7), fill=(10, 10, 12, 255))
d.text((15.5, 4.5), "3", font=fuente(6), fill=(255, 150, 40, 255), anchor="mm")
for i in range(5):
    for j in range(2):
        x, y = 10 + j * 7, 10 + i * 4
        d.ellipse((x, y, x + 3, y + 3), fill=(230, 232, 236, 255), outline=(120, 124, 130, 255))
d.ellipse((13, 29, 17, 31), fill=(220, 40, 30, 255))
guardar(im, TB, "panel_ascensor")
placa("panel_ascensor", "Panel de botones del ascensor", "Elevator Button Panel", G("block/panel_ascensor"), G("block/ascensor_lado"),
      5, 2, 11, 14, 1)
botin("panel_ascensor")
sin_forma("panel_ascensor", [mc("stone_button"), mc("stone_button"), mc("iron_ingot"), mc("glowstone_dust")])

# =========================================================================== LUCES DE NAVIDAD
COLORES = {"dorada": [(255, 214, 120), (255, 236, 170), (255, 200, 90)],
           "azul": [(110, 170, 255), (200, 225, 255), (70, 120, 255)],
           "multicolor": [(255, 70, 70), (90, 255, 110), (90, 150, 255), (255, 220, 80), (255, 255, 255), (255, 120, 220)]}


def malla(colores, sem, frames=8):
    r = np.random.default_rng(sem)
    bombillas = [(x, y) for y in range(2, N, 5) for x in range(2 + (y // 5 % 2) * 2, N, 4)]
    fase = r.uniform(0, 2 * np.pi, len(bombillas))
    col = [colores[r.integers(0, len(colores))] for _ in bombillas]
    out = []
    for k in range(frames):
        im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        for y in range(0, N, 5):
            d.line((0, y + 2, N - 1, y + 2), fill=(30, 50, 30, 255))
        for x in range(0, N, 8):
            d.line((x, 0, x, N - 1), fill=(30, 50, 30, 200))
        for (x, y), f, c in zip(bombillas, fase, col):
            v = 0.55 + 0.45 * np.sin(f + 2 * np.pi * k / frames)
            cc = tuple(int(min(255, ch * (0.5 + 0.6 * v))) for ch in c) + (255,)
            d.ellipse((x - 1, y - 1, x + 1, y + 1), fill=cc)
        out.append(brillo(im, 1.2, 1.1))
    return out


for n, cols, es, en in (("malla_luces", "dorada", "Malla de luces (cálida)", "Light Mesh (warm)"),
                        ("malla_luces_azul", "azul", "Malla de luces (azul)", "Light Mesh (blue)"),
                        ("malla_luces_multicolor", "multicolor", "Malla de luces (de colores)", "Light Mesh (multicolour)")):
    animada(malla(COLORES[cols], hash(n) % 1000), n, 3)
    bloque_simple(n, es, en, {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout", "textures": {"all": G("block/" + n)}})
    botin(n)
receta("malla_luces", ["SGS", "GLG", "SGS"], {"S": mc("string"), "G": mc("glowstone_dust"), "L": mc("yellow_dye")}, 8)
receta("malla_luces_azul", ["SGS", "GLG", "SGS"], {"S": mc("string"), "G": mc("glowstone_dust"), "L": mc("light_blue_dye")}, 8)
receta("malla_luces_multicolor", ["SGS", "GLG", "SGS"], {"S": mc("string"), "G": mc("glowstone_dust"), "L": mc("lime_dye")}, 8)

# estrella (dos planos en cruz)
frames = []
for k in range(6):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = []
    for i in range(10):
        r = 14 if i % 2 == 0 else 6
        t = -np.pi / 2 + i * np.pi / 5
        pts.append((16 + r * np.cos(t), 15 + r * np.sin(t)))
    d.polygon(pts, fill=(255, 210, 90, 255), outline=(255, 245, 200, 255))
    v = 0.5 + 0.5 * np.sin(2 * np.pi * k / 6)
    d.ellipse((13, 12, 19, 18), fill=(255, 255, int(200 + 55 * v), 255))
    for i in range(5):
        t = -np.pi / 2 + i * 2 * np.pi / 5
        x, y = 16 + 10 * np.cos(t), 15 + 10 * np.sin(t)
        d.point((x, y), fill=(255, 255, 255, 255))
    frames.append(brillo(im, 1.5 + v, 1.0))
animada(frames, "estrella_navidad", 4)
bloque_simple("estrella_navidad", "Estrella de Navidad", "Christmas Star",
              {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": G("block/estrella_navidad")}})
escribir(os.path.join(A, "models", "item", "estrella_navidad.json"), {"parent": "minecraft:item/generated",
                                                                     "textures": {"layer0": G("block/estrella_navidad")}})
botin("estrella_navidad")
receta("estrella_navidad", [" G ", "GSG", " G "], {"G": mc("gold_ingot"), "S": mc("glowstone")})

# lluvia de luces (hilos verticales con un brillo que baja)
frames = []
hilos = [(x, R.uniform(0, 1)) for x in range(2, N, 4)]
for k in range(8):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for x, f in hilos:
        largo = int(18 + 14 * ((x * 7) % 5) / 4)
        d.line((x, 0, x, largo), fill=(60, 60, 70, 140))
        for y in range(1, largo, 3):
            caida = (y / N - (k / 8 + f)) % 1.0
            v = max(0.0, 1 - caida * 4)
            c = int(150 + 105 * v)
            d.point((x, y), fill=(c, c, 255 if v > 0.1 else c, 255))
    frames.append(brillo(im, 1.0, 1.0))
animada(frames, "lluvia_luces", 2)
bloque_simple("lluvia_luces", "Lluvia de luces", "Light Rain",
              {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": G("block/lluvia_luces")}})
escribir(os.path.join(A, "models", "item", "lluvia_luces.json"), {"parent": "minecraft:item/generated",
                                                                 "textures": {"layer0": G("block/lluvia_luces")}})
botin("lluvia_luces")
receta("lluvia_luces", ["SSS", "G G", "G G"], {"S": mc("string"), "G": mc("glowstone_dust")}, 6)


# cortina de luces, guirnalda, adorno de farola
def cortina(frames=6):
    out = []
    for k in range(frames):
        im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
        d = ImageDraw.Draw(im)
        d.line((0, 0, N - 1, 0), fill=(40, 40, 40, 255))
        for x in range(1, N, 3):
            d.line((x, 0, x, N - 1), fill=(50, 50, 50, 110))
            for y in range(2, N, 4):
                v = 0.5 + 0.5 * np.sin(x * 1.7 + y * 0.9 + 2 * np.pi * k / frames)
                c = int(170 + 85 * v)
                d.point((x, y), fill=(255, c, int(c * 0.6), 255))
        out.append(brillo(im, 1.0, 1.0))
    return out


animada(cortina(), "cortina_luces", 4)
frames = []
for k in range(6):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    pts = [(x, 8 + 6 * np.sin(np.pi * x / (N - 1))) for x in range(N)]
    d.line(pts, fill=(30, 70, 30, 255), width=2)
    cols = COLORES["multicolor"]
    for i, x in enumerate(range(2, N, 4)):
        y = 8 + 6 * np.sin(np.pi * x / (N - 1)) + 2
        c = cols[(i + k) % len(cols)]
        d.ellipse((x - 1, y - 1, x + 1, y + 2), fill=c + (255,))
    frames.append(brillo(im, 1.2, 1.1))
animada(frames, "guirnalda_luces", 6, False)
frames = []
for k in range(6):
    im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    for i in range(6):
        t = i * np.pi / 3
        x1, y1 = 16 + 14 * np.cos(t), 16 + 14 * np.sin(t)
        d.line((16, 16, x1, y1), fill=(220, 235, 255, 255), width=1)
        for s in (0.45, 0.75):
            xm, ym = 16 + 14 * s * np.cos(t), 16 + 14 * s * np.sin(t)
            for dt in (-0.6, 0.6):
                d.line((xm, ym, xm + 4 * np.cos(t + dt), ym + 4 * np.sin(t + dt)), fill=(200, 225, 255, 255))
    for i in range(12):
        t = i * np.pi / 6
        v = 0.5 + 0.5 * np.sin(i + 2 * np.pi * k / 6)
        x1, y1 = 16 + 12 * np.cos(t), 16 + 12 * np.sin(t)
        c = int(180 + 75 * v)
        d.ellipse((x1 - 1, y1 - 1, x1 + 1, y1 + 1), fill=(c, c, 255, 255))
    frames.append(brillo(im, 1.4, 1.1))
animada(frames, "adorno_farola", 4)
LADO_LUZ = G("block/mat_metal_negro")
placa("cortina_luces", "Cortina de luces", "Curtain Lights", G("block/cortina_luces"), LADO_LUZ, grosor=0.5, luz=True)
placa("guirnalda_luces", "Guirnalda de luces", "String Lights", G("block/guirnalda_luces"), LADO_LUZ, 0, 0, 16, 16, 0.5, luz=True)
placa("adorno_farola", "Adorno de luces para farola", "Lamp Post Light Ornament", G("block/adorno_farola"), LADO_LUZ, grosor=0.5, luz=True)
for n in ("cortina_luces", "guirnalda_luces", "adorno_farola"):
    botin(n)
receta("cortina_luces", ["SSS", "SGS", "SGS"], {"S": mc("string"), "G": mc("glowstone_dust")}, 4)
receta("guirnalda_luces", ["S S", "RGB"], {"S": mc("string"), "G": mc("glowstone_dust"), "R": mc("red_dye"), "B": mc("blue_dye")}, 6)
receta("adorno_farola", ["G G", " W ", "G G"], {"G": mc("glowstone_dust"), "W": mc("white_dye")}, 2)


# arco de luces y cartel de Feliz Navidad (tres bloques de ancho)
def ancho(nombre_tex, dibujar, w=384, h=128):
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    dibujar(im, d, w, h)
    im = brillo(im, 2.5, 1.0)
    guardar(im.resize((256, 256), Image.LANCZOS), TB, nombre_tex)


def dib_arco(im, d, w, h):
    pts = [(x, h - 10 - (h - 30) * np.sin(np.pi * x / (w - 1))) for x in range(0, w, 2)]
    d.line(pts, fill=(255, 230, 160, 255), width=3)
    pts2 = [(x, h - 4 - (h - 50) * np.sin(np.pi * x / (w - 1))) for x in range(0, w, 2)]
    d.line(pts2, fill=(255, 210, 120, 255), width=2)
    for x in range(8, w, 16):
        y = h - 10 - (h - 30) * np.sin(np.pi * x / (w - 1))
        for yy in range(int(y) + 6, h - 2, 9):
            d.ellipse((x - 2, yy - 2, x + 2, yy + 2), fill=(255, 245, 210, 255))
    for cx in (w * 0.25, w * 0.5, w * 0.75):
        cy = h - 10 - (h - 30) * np.sin(np.pi * cx / (w - 1)) - 14
        pts = []
        for i in range(10):
            r = 12 if i % 2 == 0 else 5
            t = -np.pi / 2 + i * np.pi / 5
            pts.append((cx + r * np.cos(t), cy + r * np.sin(t)))
        d.polygon(pts, fill=(255, 220, 110, 255))


def dib_cartel(im, d, w, h):
    d.rounded_rectangle((4, 4, w - 5, h - 5), 16, outline=(255, 214, 120, 255), width=4)
    for x in range(14, w - 10, 14):
        d.ellipse((x - 3, 6, x + 3, 12), fill=(255, 80, 80, 255) if (x // 14) % 2 else (90, 255, 120, 255))
        d.ellipse((x - 3, h - 12, x + 3, h - 6), fill=(90, 150, 255, 255) if (x // 14) % 2 else (255, 230, 90, 255))
    tam = 64
    f = fuente(tam)
    while d.textlength("Feliz Navidad", font=f) > w - 50:
        tam -= 2
        f = fuente(tam)
    d.text((w / 2, h / 2 + 2), "Feliz Navidad", font=f, fill=(255, 240, 200, 255), anchor="mm")


ancho("arco_luces", dib_arco)
ancho("cartel_feliz_navidad", dib_cartel)


def placa_ancha(n, es, en, tex, lado, y0, y1, grosor=0.5, luz=True, x0=-16, x1=32, render="minecraft:cutout"):
    el = {"from": [x0, y0, 16 - grosor], "to": [x1, y1, 16], "faces": {
        "north": {"texture": "#frente", "uv": [0, 0, 16, 16]}, "south": {"texture": "#lado"},
        "east": {"texture": "#lado"}, "west": {"texture": "#lado"}, "up": {"texture": "#lado"}, "down": {"texture": "#lado"}}}
    if luz:
        el["shade"] = False
    escribir(os.path.join(A, "models", "block", n + ".json"), {"parent": "minecraft:block/block", "render_type": render,
                                                              "textures": {"frente": tex, "lado": lado, "particle": lado}, "elements": [el]})
    estados_facing(n)
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": "minecraft:item/generated", "textures": {"layer0": tex}})
    nombre(f"block.{MOD}.{n}", es, en)


placa_ancha("arco_luces", "Arco de luces (3 de ancho)", "Light Arch (3 wide)", G("block/arco_luces"), LADO_LUZ, 0, 16)
placa_ancha("cartel_feliz_navidad", "Cartel de luces «Feliz Navidad» (3 de ancho)", "\"Feliz Navidad\" Light Sign (3 wide)",
            G("block/cartel_feliz_navidad"), LADO_LUZ, 2, 14)
botin("arco_luces")
botin("cartel_feliz_navidad")
receta("arco_luces", ["GGG", "S S", "S S"], {"G": mc("glowstone_dust"), "S": mc("string")}, 2)
receta("cartel_feliz_navidad", ["GPG", "RSR", "GPG"], {"G": mc("glowstone_dust"), "P": mc("paper"), "R": mc("red_dye"), "S": mc("string")}, 2)


# bolas gigantes
def bola(color, nombre_tex):
    a = np.zeros((N, N, 4))
    yy, xx = np.mgrid[0:N, 0:N]
    for c in range(3):
        a[..., c] = color[c]
    luz = np.exp(-(((xx - 10) / 7.0) ** 2 + ((yy - 9) / 6.0) ** 2))
    sombra = (xx + yy) / (2 * N)
    a[..., :3] = a[..., :3] * (1.05 - 0.45 * sombra[..., None]) + 120 * luz[..., None]
    a[..., :3] += R.normal(0, 3, (N, N, 1))
    a[..., 3] = 255
    im = img(a)
    d = ImageDraw.Draw(im)
    for y in (10, 20):
        d.line((0, y, N - 1, y), fill=tuple(min(255, int(c * 1.25 + 30)) for c in color) + (255,))
    guardar(im, TB, nombre_tex)


bola((200, 20, 30), "bola_navidad_roja")
bola((214, 168, 50), "bola_navidad_dorada")
for col, es, en in (("roja", "Bola de Navidad gigante (roja)", "Giant Christmas Bauble (red)"),
                    ("dorada", "Bola de Navidad gigante (dorada)", "Giant Christmas Bauble (gold)")):
    n = "bola_navidad_" + col
    t = G("block/" + n)
    m = {"parent": "minecraft:block/block", "textures": {"b": t, "c": G("block/mat_aluminio"), "particle": t}, "elements": [
        {"from": [2, 1, 2], "to": [14, 13, 14], "faces": {f: {"texture": "#b"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [1, 3, 3], "to": [15, 11, 13], "faces": {f: {"texture": "#b"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [3, 3, 1], "to": [13, 11, 15], "faces": {f: {"texture": "#b"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [3, 0, 3], "to": [13, 14, 13], "faces": {f: {"texture": "#b"} for f in ("north", "south", "east", "west", "up", "down")}},
        {"from": [6, 14, 6], "to": [10, 16, 10], "faces": {f: {"texture": "#c"} for f in ("north", "south", "east", "west", "up", "down")}}]}
    escribir(os.path.join(A, "models", "block", n + ".json"), m)
    escribir(os.path.join(A, "blockstates", n + ".json"), {"variants": {"": {"model": G("block/" + n)}}})
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("block/" + n)})
    nombre(f"block.{MOD}.{n}", es, en)
    botin(n)
receta("bola_navidad_roja", [" I ", "RGR", " R "], {"I": mc("iron_nugget"), "R": mc("red_dye"), "G": mc("glass")}, 2)
receta("bola_navidad_dorada", [" I ", "YGY", " Y "], {"I": mc("iron_nugget"), "Y": mc("gold_nugget"), "G": mc("glass")}, 2)

# =========================================================================== METRO DE MADRID


def azulejo(color, borde, nombre_tex, sem):
    """Azulejo biselado blanco (o de color) del metro de Madrid, a soga."""
    r = np.random.default_rng(sem)
    a = np.zeros((N, N, 4))
    a[..., 3] = 255
    a[..., :3] = (70, 70, 72)
    filas = 4
    for j in range(filas):
        y0 = j * 8
        desp = 0 if j % 2 == 0 else 8
        for i in range(-1, 3):
            x0 = i * 16 + desp
            tono = np.array(color, float) + r.normal(0, 3)
            for y in range(y0 + 1, y0 + 8):
                for x in range(x0 + 1, x0 + 16):
                    if 0 <= x < N:
                        b = 1.0
                        if y == y0 + 1 or x == x0 + 1:
                            b = 1.12
                        elif y == y0 + 7 or x == x0 + 15:
                            b = 0.82
                        elif y == y0 + 2 or x == x0 + 2:
                            b = 1.05
                        a[y, x, :3] = tono * b
    a[..., :3] += r.normal(0, 1.5, (N, N, 1))
    guardar(img(a), TB, nombre_tex)


azulejo((236, 236, 230), None, "azulejo_metro_biselado", 1)
azulejo((40, 90, 170), None, "azulejo_metro_azul", 2)
azulejo((190, 30, 40), None, "azulejo_metro_rojo", 3)
azulejo((40, 140, 80), None, "azulejo_metro_verde", 4)
for n, es, en in (("azulejo_metro_biselado", "Azulejo biselado del metro (blanco)", "Metro Bevelled Tile (white)"),
                  ("azulejo_metro_azul", "Azulejo del metro (azul)", "Metro Tile (blue)"),
                  ("azulejo_metro_rojo", "Azulejo del metro (rojo)", "Metro Tile (red)"),
                  ("azulejo_metro_verde", "Azulejo del metro (verde)", "Metro Tile (green)")):
    bloque_simple(n, es, en)
    botin(n)
receta("azulejo_metro_biselado", ["QQ", "QQ"], {"Q": mc("quartz_block")}, 8)
sin_forma("azulejo_metro_azul", [G("azulejo_metro_biselado"), G("azulejo_metro_biselado"), mc("blue_dye")], 2)
sin_forma("azulejo_metro_rojo", [G("azulejo_metro_biselado"), G("azulejo_metro_biselado"), mc("red_dye")], 2)
sin_forma("azulejo_metro_verde", [G("azulejo_metro_biselado"), G("azulejo_metro_biselado"), mc("green_dye")], 2)

# suelo del andén: terrazo gris
a = arr(ruido((150, 150, 148), 7))
for _ in range(90):
    x, y = R.integers(0, N, 2)
    a[y, x, :3] = R.choice([60, 210, 120, 180]) + R.normal(0, 6, 3)
guardar(img(a), TB, "suelo_anden")
bloque_simple("suelo_anden", "Suelo de andén (terrazo)", "Platform Floor (terrazzo)")
botin("suelo_anden")
receta("suelo_anden", ["SD", "DS"], {"S": mc("smooth_stone"), "D": mc("diorite")}, 4)

# borde del andén: franja amarilla con botones podotáctiles
a = arr(ruido((150, 150, 148), 7))
a[0:9, :, :3] = (236, 196, 30)
im = img(a)
d = ImageDraw.Draw(im)
for y in range(1, 8, 3):
    for x in range(1 + (y // 3 % 2), N, 3):
        d.point((x, y), fill=(255, 226, 90, 255))
d.line((0, 9, N - 1, 9), fill=(250, 250, 250, 255))
guardar(im, TB, "borde_anden_arriba")
a = arr(ruido((140, 140, 138), 6))
a[0:3, :, :3] = (236, 196, 30)
guardar(img(a), TB, "borde_anden_lado")
escribir(os.path.join(A, "models", "block", "borde_anden.json"), {"parent": "minecraft:block/cube_bottom_top", "textures": {
    "top": G("block/borde_anden_arriba"), "side": G("block/borde_anden_lado"), "bottom": G("block/suelo_anden")}})
estados_facing("borde_anden")
escribir(os.path.join(A, "models", "item", "borde_anden.json"), {"parent": G("block/borde_anden")})
nombre(f"block.{MOD}.borde_anden", "Borde de andén (franja de seguridad)", "Platform Edge (safety strip)")
botin("borde_anden")
sin_forma("borde_anden", [G("suelo_anden"), G("suelo_anden"), mc("yellow_dye")], 2)

# señal de parada (va debajo de la vía en las estaciones)
im = ruido((70, 70, 72), 6)
d = ImageDraw.Draw(im)
for k in range(-N, N * 2, 8):
    d.polygon([(k, 0), (k + 4, 0), (k + 4 - N, N), (k - N, N)], fill=(236, 196, 30, 255))
d.rectangle((8, 8, 23, 23), fill=(250, 250, 250, 255), outline=(20, 20, 20, 255))
d.text((16, 16), "P", font=fuente(14), fill=(20, 60, 160, 255), anchor="mm")
guardar(im, TB, "senal_parada")
bloque_simple("senal_parada", "Señal de parada del metro (bajo la vía)", "Metro Stop Marker (under the rail)")
botin("senal_parada")
receta("senal_parada", ["YBY", "BRB", "YBY"], {"Y": mc("yellow_dye"), "B": mc("stone_bricks"), "R": mc("redstone")}, 4)

# torno
im = cepillado((186, 190, 196), 5, sem=51)
d = ImageDraw.Draw(im)
d.rectangle((4, 2, 27, 12), fill=(16, 18, 20, 255))
d.polygon([(10, 5), (16, 5), (16, 3), (21, 7), (16, 11), (16, 9), (10, 9)], fill=(40, 230, 90, 255))
d.rectangle((8, 16, 23, 22), fill=(40, 40, 46, 255))
d.text((15.5, 19), "BILLETE", font=fuente(4), fill=(220, 220, 220, 255), anchor="mm")
guardar(im, TB, "torno_frente")
cristal = Image.new("RGBA", (N, N), (190, 220, 235, 90))
d = ImageDraw.Draw(cristal)
d.rectangle((0, 0, N - 1, N - 1), outline=(220, 224, 230, 220), width=1)
d.line((3, 3, 10, 10), fill=(255, 255, 255, 140))
guardar(cristal, TB, "torno_cristal")
INOX = G("block/mat_inox")
col = {"from": [0, 0, 3], "to": [4, 15, 13], "faces": {
    "north": {"texture": "#f"}, "south": {"texture": "#f"}, "east": {"texture": "#i"}, "west": {"texture": "#i"},
    "up": {"texture": "#f", "uv": [0, 0, 16, 4]}, "down": {"texture": "#i"}}}
hoja = {"from": [4, 4, 7.5], "to": [15.5, 14, 8.5], "faces": {f: {"texture": "#c"} for f in ("north", "south", "east", "west", "up", "down")}}
for estado, els in (("cerrado", [col, hoja]), ("abierto", [col])):
    escribir(os.path.join(A, "models", "block", "torno_metro_" + estado + ".json"), {
        "parent": "minecraft:block/block", "render_type": "minecraft:translucent",
        "textures": {"f": G("block/torno_frente"), "i": INOX, "c": G("block/torno_cristal"), "particle": INOX}, "elements": els})
v = {}
for f, y in (("north", 0), ("south", 0), ("east", 90), ("west", 90)):
    for o, m in (("false", "cerrado"), ("true", "abierto")):
        dd = {"model": G("block/torno_metro_" + m)}
        if y:
            dd["y"] = y
        v[f"facing={f},open={o}"] = dd
escribir(os.path.join(A, "blockstates", "torno_metro.json"), {"variants": v})
escribir(os.path.join(A, "models", "item", "torno_metro.json"), {"parent": G("block/torno_metro_cerrado")})
nombre(f"block.{MOD}.torno_metro", "Torno de acceso al metro", "Metro Turnstile")
botin("torno_metro")
receta("torno_metro", ["IGI", "IRI", "I I"], {"I": mc("iron_ingot"), "G": mc("glass_pane"), "R": mc("redstone")}, 2)

# máquina de billetes
im = Image.new("RGBA", (64, 64), (200, 30, 40, 255))
d = ImageDraw.Draw(im)
d.rectangle((0, 0, 63, 11), fill=(240, 240, 240, 255))
d.text((32, 6), "BILLETES", font=fuente(9), fill=(200, 30, 40, 255), anchor="mm")
d.rectangle((8, 16, 55, 36), fill=(20, 40, 70, 255))
d.text((31.5, 22), "Sencillo", font=fuente(8), fill=(180, 230, 255, 255), anchor="mm")
d.text((31.5, 31), "1,50 €", font=fuente(9), fill=(255, 255, 255, 255), anchor="mm")
for i in range(3):
    for j in range(3):
        d.rectangle((10 + i * 8, 41 + j * 6, 15 + i * 8, 44 + j * 6), fill=(230, 230, 230, 255))
d.rectangle((40, 42, 52, 44), fill=(20, 20, 20, 255))
d.rectangle((38, 52, 54, 58), fill=(30, 30, 30, 255))
guardar(im, TB, "maquina_billetes_frente")
guardar(ruido((190, 30, 40), 4), TB, "maquina_billetes_lado")
escribir(os.path.join(A, "models", "block", "maquina_billetes.json"), {"parent": "minecraft:block/block", "textures": {
    "f": G("block/maquina_billetes_frente"), "l": G("block/maquina_billetes_lado"), "particle": G("block/maquina_billetes_lado")},
    "elements": [{"from": [2, 0, 2], "to": [14, 16, 14], "faces": {
        "north": {"texture": "#f"}, "south": {"texture": "#l"}, "east": {"texture": "#l"}, "west": {"texture": "#l"},
        "up": {"texture": "#l"}, "down": {"texture": "#l"}}}]})
estados_facing("maquina_billetes", lambda f: G("block/maquina_billetes"))
escribir(os.path.join(A, "models", "item", "maquina_billetes.json"), {"parent": G("block/maquina_billetes")})
nombre(f"block.{MOD}.maquina_billetes", "Máquina de billetes", "Ticket Machine")
botin("maquina_billetes")
receta("maquina_billetes", ["III", "RPR", "III"], {"I": mc("iron_ingot"), "R": mc("red_dye"), "P": mc("paper")})

# cartel de la boca de metro
im = Image.new("RGBA", (64, 64), (20, 50, 140, 255))
d = ImageDraw.Draw(im)
d.rectangle((0, 44, 63, 51), fill=(210, 30, 40, 255))
d.rectangle((2, 2, 61, 61), outline=(250, 250, 250, 255), width=2)
d.text((32, 24), "METRO", font=fuente(15), fill=(255, 255, 255, 255), anchor="mm")
guardar(im, TB, "boca_metro")
placa("boca_metro", "Cartel de entrada al metro", "Metro Entrance Sign", G("block/boca_metro"), G("block/mat_lacado_gris"), 0, 2, 16, 14, 4,
      luz=True, item_plano=False)
botin("boca_metro")
receta("boca_metro", ["BBB", "WGW", "BRB"], {"B": mc("blue_dye"), "W": mc("white_dye"), "G": mc("glowstone"), "R": mc("red_dye")})

# panel de próximos trenes (LED naranja)
im = Image.new("RGBA", (128, 128), (8, 8, 10, 255))
d = ImageDraw.Draw(im)
d.rectangle((0, 0, 127, 127), outline=(60, 60, 64, 255), width=5)
f = fuente(10, True)
for i, (dest, mins) in enumerate((("1 Valdecarros", "2 min"), ("1 P. Castilla", "5 min"))):
    y = 34 + i * 26
    d.text((12, y), dest, font=f, fill=(255, 150, 30, 255), anchor="lm")
    d.text((116, y), mins, font=f, fill=(255, 150, 30, 255), anchor="rm")
d.line((12, 47, 116, 47), fill=(80, 50, 10, 255))
d.text((64, 96), "Andén 1", font=fuente(12, True), fill=(255, 190, 80, 255), anchor="mm")
guardar(im, TB, "panel_trenes")
placa("panel_trenes", "Panel de próximos trenes", "Next Trains Display", G("block/panel_trenes"), G("block/mat_metal_negro"), 0, 2, 16, 14, 2,
      luz=True)
botin("panel_trenes")
receta("panel_trenes", ["III", "OGO", "III"], {"I": mc("iron_ingot"), "O": mc("orange_dye"), "G": mc("glowstone_dust")}, 2)

# reloj del andén
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.ellipse((2, 2, 61, 61), fill=(40, 40, 44, 255))
d.ellipse((6, 6, 57, 57), fill=(250, 250, 248, 255))
for i in range(12):
    t = i * np.pi / 6
    d.line((32 + 21 * np.cos(t), 32 + 21 * np.sin(t), 32 + 25 * np.cos(t), 32 + 25 * np.sin(t)), fill=(20, 20, 20, 255), width=2)
d.line((32, 32, 32, 14), fill=(20, 20, 20, 255), width=3)
d.line((32, 32, 46, 38), fill=(20, 20, 20, 255), width=3)
d.line((32, 32, 22, 50), fill=(210, 30, 30, 255), width=1)
guardar(im, TB, "reloj_anden")
placa("reloj_anden", "Reloj del andén", "Platform Clock", G("block/reloj_anden"), G("block/mat_metal_negro"), 2, 2, 14, 14, 2)
botin("reloj_anden")
receta("reloj_anden", [" I ", "ICI", " I "], {"I": mc("iron_nugget"), "C": mc("clock")})


# carteles de estación: azul con letras blancas (3 bloques de ancho)
def quitar_tildes(s):
    import unicodedata
    return "".join(c for c in unicodedata.normalize("NFD", s.lower()) if unicodedata.category(c) != "Mn")


import re

ESTACIONES = ["Sol", "Gran Vía", "Callao", "Ópera", "Atocha", "Chamartín", "Nuevos Ministerios", "Plaza de Castilla", "Moncloa",
              "Príncipe Pío", "Retiro", "Banco de España", "Tribunal", "Argüelles", "Cuatro Caminos", "Legazpi", "Ventas", "Lavapiés",
              "Bilbao", "Tirso de Molina"]
LINEAS_DE = {"Sol": [1, 2, 3], "Gran Vía": [1, 5], "Callao": [3, 5], "Ópera": [2, 5], "Atocha": [1], "Chamartín": [1, 10],
             "Nuevos Ministerios": [6, 8, 10], "Plaza de Castilla": [1, 9, 10], "Moncloa": [3, 6], "Príncipe Pío": [6, 10],
             "Retiro": [2], "Banco de España": [2], "Tribunal": [1, 10], "Argüelles": [3, 4, 6], "Cuatro Caminos": [1, 2, 6],
             "Legazpi": [3, 6], "Ventas": [2, 5], "Lavapiés": [3], "Bilbao": [1, 4], "Tirso de Molina": [1]}
COLOR_LINEA = [(0x38, 0xA3, 0xDC), (0xE0, 0x00, 0x1A), (0xFF, 0xD1, 0x00), (0xB0, 0x6B, 0x2E), (0x8E, 0xBE, 0x3F), (0x9A, 0x9A, 0x9A),
               (0xF3, 0x96, 0x2B), (0xEA, 0x8F, 0xB8), (0x9C, 0x3E, 0x95), (0x1B, 0x3C, 0x8F), (0x2E, 0x9E, 0x47), (0xA4, 0x9A, 0x00)]


def circulo_linea(d, cx, cy, r, n):
    c = COLOR_LINEA[n - 1]
    d.ellipse((cx - r, cy - r, cx + r, cy + r), fill=c + (255,), outline=(255, 255, 255, 255), width=max(1, r // 8))
    texto_col = (20, 20, 20, 255) if n == 3 else (255, 255, 255, 255)
    d.text((cx, cy + 1), str(n), font=fuente(int(r * 1.15)), fill=texto_col, anchor="mm")


# un ingrediente distinto para cada cartel (así ninguna receta se repite)
MARCAS = ["sunflower", "cobblestone", "stone", "andesite", "diorite", "granite", "dirt", "sand", "gravel", "clay_ball", "brick", "flint",
          "coal", "charcoal", "string", "feather", "leather", "bone", "wheat", "apple"]

for est in ESTACIONES:
    ide = re.sub(r"[^a-z0-9]+", "_", quitar_tildes(est)).strip("_")
    w, h = 768, 128
    im = Image.new("RGBA", (w, h), (24, 52, 128, 255))
    d = ImageDraw.Draw(im)
    d.rectangle((5, 5, w - 6, h - 6), outline=(240, 240, 240, 255), width=4)
    tam = 78
    f = fuente(tam)
    hueco = w - 70 - 40 - 92 * len(LINEAS_DE.get(est, [1]))
    while d.textlength(est, font=f) > hueco and tam > 30:
        tam -= 4
        f = fuente(tam)
    d.text((40, h / 2 + 3), est, font=f, fill=(255, 255, 255, 255), anchor="lm")
    lin = LINEAS_DE.get(est, [1])
    for i, n in enumerate(reversed(lin)):
        circulo_linea(d, w - 70 - i * 92, h / 2, 38, n)
    t = "placa_estacion_" + ide
    guardar(im.resize((256, 256), Image.LANCZOS), TB, t)
    placa_ancha(t, f"Cartel de estación: {est}", f"Station Sign: {est}", G("block/" + t), G("block/mat_lacado_gris"), 4, 12, 1,
                luz=False, render="minecraft:solid")
    botin(t)
    sin_forma(t, [mc("blue_dye"), mc("white_dye"), mc("iron_ingot"), mc(MARCAS[ESTACIONES.index(est)])], 1)

for n in range(1, 13):
    im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    circulo_linea(d, 32, 32, 29, n)
    t = f"cartel_linea_{n}"
    guardar(im, TB, t)
    placa(t, f"Cartel de la línea {n}", f"Line {n} Sign", G("block/" + t), G("block/mat_lacado_gris"), 2, 2, 14, 14, 1)
    botin(t)
    sin_forma(t, [mc("white_dye"), mc("iron_nugget"), mc("paper"), mc(MARCAS[n - 1])])

# billete y vagón (objetos)
ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(ic)
d.rectangle((1, 4, 14, 11), fill=(246, 246, 240, 255), outline=(180, 180, 176, 255))
d.rectangle((1, 4, 14, 5), fill=(200, 30, 60, 255))
d.rectangle((1, 9, 14, 10), fill=(60, 50, 40, 255))
d.point((3, 7), fill=(24, 52, 128, 255))
d.line((5, 7, 11, 7), fill=(120, 120, 120, 255))
guardar(ic, TI, "billete_metro")
item_plano("billete_metro", "Billete de metro", "Metro Ticket")
sin_forma("billete_metro", [mc("paper"), mc("iron_nugget")], 4)

ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(ic)
d.rounded_rectangle((2, 1, 13, 14), 2, fill=(242, 242, 242, 255), outline=(120, 120, 120, 255))
d.rectangle((4, 3, 11, 7), fill=(30, 40, 54, 255))
d.rectangle((3, 9, 12, 10), fill=(210, 30, 40, 255))
d.point((4, 12), fill=(255, 250, 200, 255))
d.point((11, 12), fill=(255, 250, 200, 255))
d.rectangle((6, 2, 9, 2), fill=(255, 150, 30, 255))
d.line((2, 15, 13, 15), fill=(60, 60, 60, 255))
guardar(ic, TI, "vagon_metro")
item_plano("vagon_metro", "Vagón de metro", "Metro Carriage")
receta("vagon_metro", ["IGI", "IMI", "RCR"], {"I": mc("iron_ingot"), "G": mc("glass_pane"), "M": mc("minecart"), "R": mc("red_dye"),
                                              "C": mc("redstone_block")})


# modelo del vagón (lo dibuja el tren): costados, frentes, techo y bajos
def lado_vagon():
    w, h = 480, 200
    im = Image.new("RGBA", (w, h), (238, 238, 236, 255))
    d = ImageDraw.Draw(im)
    for x0 in range(10, w - 10, 160):
        d.rounded_rectangle((x0 + 40, 48, x0 + 150, 112), 10, fill=(34, 44, 58, 255))
        d.line((x0 + 95, 48, x0 + 95, 112), fill=(200, 200, 200, 255), width=3)
    for xd in (10, 170, 330):
        d.rectangle((xd, 30, xd + 34, 180), fill=(214, 214, 212, 255), outline=(120, 120, 120, 255), width=2)
        d.line((xd + 17, 30, xd + 17, 180), fill=(120, 120, 120, 255), width=2)
        d.rounded_rectangle((xd + 4, 46, xd + 14, 100), 3, fill=(34, 44, 58, 255))
        d.rounded_rectangle((xd + 20, 46, xd + 30, 100), 3, fill=(34, 44, 58, 255))
        d.rectangle((xd + 4, 22, xd + 30, 27), fill=(255, 150, 30, 255))
    d.rectangle((0, 124, w, 136), fill=(210, 30, 40, 255))
    d.rectangle((0, 136, w, 140), fill=(24, 52, 128, 255))
    d.rectangle((0, 182, w, h), fill=(60, 62, 66, 255))
    d.rectangle((0, 0, w, 8), fill=(180, 182, 186, 255))
    a = arr(im)
    a[..., :3] *= (1.0 - 0.08 * np.linspace(0, 1, h))[:, None, None]
    return img(a).resize((256, 256), Image.LANCZOS)


def frente_vagon():
    w, h = 160, 200
    im = Image.new("RGBA", (w, h), (238, 238, 236, 255))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle((14, 34, w - 14, 112), 14, fill=(24, 32, 44, 255))
    d.polygon([(20, 40), (50, 40), (30, 100), (20, 100)], fill=(70, 86, 110, 255))
    d.rectangle((40, 12, w - 40, 28), fill=(10, 10, 10, 255))
    d.text((w / 2, 20), "SOL", font=fuente(12), fill=(255, 150, 30, 255), anchor="mm")
    d.rectangle((0, 124, w, 136), fill=(210, 30, 40, 255))
    d.rectangle((0, 136, w, 140), fill=(24, 52, 128, 255))
    for cx in (28, w - 28):
        d.ellipse((cx - 9, 148, cx + 9, 166), fill=(255, 252, 220, 255), outline=(150, 150, 150, 255), width=2)
    d.rectangle((60, 150, w - 60, 172), fill=(200, 200, 200, 255), outline=(120, 120, 120, 255))
    d.rectangle((0, 182, w, h), fill=(60, 62, 66, 255))
    return im.resize((128, 128), Image.LANCZOS)


guardar(lado_vagon(), TB, "metro_lado")
guardar(frente_vagon(), TB, "metro_frente")
im = ruido((150, 152, 156), 5)
d = ImageDraw.Draw(im)
for y in range(4, N, 8):
    d.rectangle((6, y, 25, y + 3), fill=(110, 112, 116, 255))
guardar(im, TB, "metro_techo")
guardar(ruido((44, 44, 46), 6), TB, "metro_bajos")
todas = ("north", "south", "east", "west", "up", "down")
escribir(os.path.join(A, "models", "block", "modelo_vagon.json"), {
    "parent": "minecraft:block/block", "ambientocclusion": False,
    "textures": {"lado": G("block/metro_lado"), "frente": G("block/metro_frente"), "techo": G("block/metro_techo"),
                 "bajos": G("block/metro_bajos"), "particle": G("block/metro_lado")},
    "elements": [
        {"from": [0, 3, -16], "to": [16, 23, 32], "faces": {
            "east": {"texture": "#lado"}, "west": {"texture": "#lado"}, "north": {"texture": "#frente"}, "south": {"texture": "#frente"},
            "up": {"texture": "#techo"}, "down": {"texture": "#bajos"}}},
        {"from": [2, 0, -13], "to": [14, 3, -3], "faces": {f: {"texture": "#bajos"} for f in todas}},
        {"from": [2, 0, 19], "to": [14, 3, 29], "faces": {f: {"texture": "#bajos"} for f in todas}},
        {"from": [3, 23, -5], "to": [13, 24.5, 9], "faces": {f: {"texture": "#techo"} for f in todas}},
        {"from": [3, 23, 15], "to": [13, 24.5, 27], "faces": {f: {"texture": "#techo"} for f in todas}}]})
escribir(os.path.join(A, "blockstates", "modelo_vagon.json"), {"variants": {"": {"model": G("block/modelo_vagon")}}})
nombre(f"block.{MOD}.modelo_vagon", "Vagón de metro", "Metro Carriage")
nombre(f"entity.{MOD}.metro", "Vagón de metro", "Metro Carriage")

# =========================================================================== nombres de pestañas
nombre(f"itemGroup.{MOD}.vivienda", "Vivienda: ascensores, instalaciones y fachada", "Housing: Elevators, Fittings & Facade")
nombre(f"itemGroup.{MOD}.metro", "Metro de Madrid", "Madrid Metro")
nombre(f"itemGroup.{MOD}.navidad", "Navidad: luces de Madrid y adornos", "Christmas: Madrid Lights & Decorations")

# =========================================================================== etiquetas, comprobación de recetas e idioma


def tag(carpeta, nombre_tag, valores, tipo="blocks"):
    ruta = os.path.join(carpeta, "tags", tipo, nombre_tag + ".json")
    previo = json.load(open(ruta, encoding="utf-8"))["values"] if os.path.exists(ruta) else []
    escribir(ruta, {"replace": False, "values": previo + [v for v in valores if v not in previo]})


tag(DMC, "mineable/pickaxe", [G(n) for n in BOTIN])

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

for archivo, nombres in (("es_es.json", lang_es), ("en_us.json", lang_en)):
    ruta = os.path.join(A, "lang", archivo)
    actual = json.load(open(ruta, encoding="utf-8"))
    actual.update(nombres)
    escribir(ruta, actual)

print(f"v7_ciudad: {len(BOTIN)} bloques, {len(lang_es)} nombres")

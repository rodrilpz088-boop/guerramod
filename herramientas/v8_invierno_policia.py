#!/usr/bin/env python3
"""Recursos de las cosas para la nieve y de la Policía Nacional.
python3 herramientas/v8_invierno_policia.py  (después de v7_ciudad.py)
"""
import json
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont

import v7_ciudad as V  # reutiliza utilidades (y vuelve a generar lo suyo, que es determinista)

MOD = V.MOD
A, D, DMC, TB, TI = V.A, V.D, V.DMC, V.TB, V.TI
G, mc = V.G, V.mc
escribir, guardar, nombre = V.escribir, V.guardar, V.nombre
TA = os.path.join(A, "textures", "models", "armor")
TE = os.path.join(A, "textures", "entity")
R = np.random.default_rng(8008)
N = 32
V.lang_es.clear()
V.lang_en.clear()
BOTIN = []


def fuente(t, cond=False):
    return V.fuente(t, cond)


def botin(n):
    BOTIN.append(n)
    V.botin(n)


def icono(n, dibujar, es, en, mano=False):
    ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dibujar(ImageDraw.Draw(ic), ic)
    guardar(ic, TI, n)
    escribir(os.path.join(A, "models", "item", n + ".json"),
             {"parent": "minecraft:item/handheld" if mano else "minecraft:item/generated", "textures": {"layer0": G("item/" + n)}})
    nombre(f"item.{MOD}.{n}", es, en)


# =========================================================================== NIEVE


def d_pala(d, im):
    d.line((3, 13, 10, 6), fill=(120, 80, 40, 255), width=1)
    d.line((2, 14, 4, 12), fill=(30, 30, 30, 255), width=2)
    d.polygon([(9, 4), (14, 2), (14, 9), (11, 9)], fill=(230, 60, 40, 255), outline=(150, 30, 20, 255))


icono("pala_nieve", d_pala, "Pala quitanieves", "Snow Shovel", True)


def d_sal(d, im):
    d.rounded_rectangle((3, 3, 12, 14), 2, fill=(240, 240, 236, 255), outline=(150, 150, 150, 255))
    d.rectangle((3, 6, 12, 10), fill=(30, 90, 180, 255))
    d.text((7.5, 8), "SAL", font=fuente(5), fill=(255, 255, 255, 255), anchor="mm")
    for x, y in ((5, 12), (8, 13), (10, 12)):
        d.point((x, y), fill=(200, 200, 200, 255))


icono("sal_carretera", d_sal, "Sal gruesa para carreteras", "Road Salt")


def d_raq(d, im):
    d.ellipse((2, 1, 9, 15), outline=(120, 80, 40, 255), width=1)
    for y in range(3, 14, 2):
        d.line((3, y, 8, y), fill=(200, 170, 120, 255))
    d.ellipse((8, 1, 15, 15), outline=(120, 80, 40, 255), width=1)
    for y in range(3, 14, 2):
        d.line((9, y, 14, y), fill=(200, 170, 120, 255))


icono("raquetas_nieve", d_raq, "Raquetas de nieve", "Snowshoes")


def d_esq(d, im):
    d.line((2, 14, 13, 1), fill=(210, 30, 40, 255), width=2)
    d.line((5, 15, 15, 3), fill=(30, 60, 200, 255), width=2)
    d.rectangle((6, 7, 9, 10), fill=(40, 40, 40, 255))


icono("esquis", d_esq, "Esquís (botas con esquís)", "Skis (ski boots)")


def d_tri(d, im):
    d.rectangle((2, 6, 13, 9), fill=(190, 40, 40, 255), outline=(120, 20, 20, 255))
    d.line((1, 12, 14, 12), fill=(80, 80, 86, 255), width=1)
    d.line((14, 12, 15, 9), fill=(80, 80, 86, 255))
    d.line((4, 9, 4, 12), fill=(80, 80, 86, 255))
    d.line((11, 9, 11, 12), fill=(80, 80, 86, 255))
    d.line((2, 6, 1, 3), fill=(120, 80, 40, 255))


icono("trineo", d_tri, "Trineo", "Sled")
nombre(f"entity.{MOD}.trineo", "Trineo", "Sled")

# textura del trineo (con la forma de la barca): madera pintada de rojo con remaches
im = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
a = np.zeros((64, 128, 4))
a[..., 0], a[..., 1], a[..., 2], a[..., 3] = 176, 34, 34, 255
a[..., :3] += R.normal(0, 6, (64, 128, 1))
for y in range(0, 64, 4):
    a[y, :, :3] *= 0.75
for x in range(0, 128, 16):
    a[:, x, :3] *= 0.8
a[::4, ::8, :3] = (200, 200, 200)
guardar(V.img(a), TE, "trineo")

# armaduras de nieve
def armadura_vacia():
    return Image.new("RGBA", (64, 32), (0, 0, 0, 0))


def botas(im, col, suela, filas=(26, 32), lazo=None):
    d = ImageDraw.Draw(im)
    d.rectangle((0, filas[0], 15, filas[1] - 1), fill=col)
    d.rectangle((8, 16, 11, 19), fill=suela)  # suela (cara de abajo)
    d.rectangle((4, 16, 7, 19), fill=col)
    if lazo:
        for y in range(filas[0] + 1, filas[1] - 1, 2):
            d.point((5, y), fill=lazo)
            d.point((6, y), fill=lazo)


im = armadura_vacia()
botas(im, (110, 74, 40, 255), (200, 170, 120, 255), (25, 32), (230, 220, 200, 255))
guardar(im, TA, "raquetas_layer_1")
guardar(armadura_vacia(), TA, "raquetas_layer_2")
im = armadura_vacia()
botas(im, (210, 30, 40, 255), (30, 30, 30, 255), (23, 32), (250, 250, 250, 255))
d = ImageDraw.Draw(im)
d.rectangle((0, 27, 15, 27), fill=(240, 240, 240, 255))
guardar(im, TA, "esquis_layer_1")
guardar(armadura_vacia(), TA, "esquis_layer_2")

# bloques: ladrillo de hielo, nieve pisada, cañón de nieve
a = V.arr(V.ruido((176, 214, 240), 6))
for y in range(0, N, 8):
    a[y, :, :3] = (220, 240, 252)
    off = 0 if (y // 8) % 2 == 0 else 8
    for x in range(off, N, 16):
        a[y:y + 8, x, :3] = (220, 240, 252)
a[..., :3] += np.linspace(8, -8, N)[:, None, None]
guardar(V.img(a), TB, "ladrillo_hielo")
V.bloque_simple("ladrillo_hielo", "Ladrillo de hielo (iglús)", "Ice Brick (igloos)")
botin("ladrillo_hielo")
V.receta("ladrillo_hielo", ["II", "II"], {"I": mc("packed_ice")}, 4)

a = V.arr(V.ruido((236, 240, 246), 5))
for _ in range(40):
    x, y = R.integers(0, N, 2)
    a[y, x, :3] -= 18
for y in range(4, N, 9):
    a[y:y + 2, 6:12, :3] -= 12
    a[y + 4:y + 6, 18:24, :3] -= 12
guardar(V.img(a), TB, "nieve_pisada")
V.bloque_simple("nieve_pisada", "Nieve pisada (no se derrite)", "Packed Snow (does not melt)")
botin("nieve_pisada")
V.receta("nieve_pisada", ["SS", "SS"], {"S": mc("snow_block")}, 3)

im = V.cepillado((220, 222, 226), 4, sem=81)
d = ImageDraw.Draw(im)
d.ellipse((3, 3, 28, 28), fill=(40, 42, 46, 255), outline=(240, 200, 30, 255), width=2)
for k in range(0, 360, 45):
    t = np.radians(k)
    d.line((15.5, 15.5, 15.5 + 11 * np.cos(t), 15.5 + 11 * np.sin(t)), fill=(150, 154, 160, 255), width=2)
d.ellipse((12, 12, 19, 19), fill=(220, 222, 226, 255))
guardar(im, TB, "canon_nieve_boca")
im = V.cepillado((230, 200, 30), 5, sem=82)
d = ImageDraw.Draw(im)
d.rectangle((0, 0, N - 1, 3), fill=(40, 40, 44, 255))
d.text((16, 18), "SNOW", font=fuente(7), fill=(40, 40, 44, 255), anchor="mm")
guardar(im, TB, "canon_nieve_lado")
caras = lambda t: {f: {"texture": t} for f in ("north", "south", "east", "west", "up", "down")}
escribir(os.path.join(A, "models", "block", "canon_nieve.json"), {
    "parent": "minecraft:block/block", "textures": {"b": G("block/canon_nieve_boca"), "l": G("block/canon_nieve_lado"),
                                                   "p": G("block/mat_lacado_gris"), "particle": G("block/canon_nieve_lado")},
    "elements": [
        {"from": [7, 0, 7], "to": [9, 8, 9], "faces": caras("#p")},
        {"from": [3, 0, 3], "to": [13, 1, 13], "faces": caras("#p")},
        {"from": [2, 6, 3], "to": [14, 15, 14], "faces": {"north": {"texture": "#b"}, "south": {"texture": "#b"}, "east": {"texture": "#l"},
                                                         "west": {"texture": "#l"}, "up": {"texture": "#l"}, "down": {"texture": "#l"}}}]})
V.estados_facing("canon_nieve", extra={"lit=false": G("block/canon_nieve"), "lit=true": G("block/canon_nieve")})
escribir(os.path.join(A, "models", "item", "canon_nieve.json"), {"parent": G("block/canon_nieve")})
nombre(f"block.{MOD}.canon_nieve", "Cañón de nieve (estación de esquí)", "Snow Cannon (ski resort)")
botin("canon_nieve")
V.receta("canon_nieve", ["III", "FWF", " I "], {"I": mc("iron_ingot"), "F": mc("iron_bars"), "W": mc("powder_snow_bucket")})

V.receta("pala_nieve", [" PP", " SP", "S  "], {"P": mc("red_dye"), "S": mc("stick")})
V.sin_forma("sal_carretera", [mc("sugar"), mc("gravel"), mc("bone_meal")], 8)
V.receta("raquetas_nieve", ["SLS", "S S"], {"S": mc("stick"), "L": mc("leather")})
V.receta("esquis", ["I I", "L L", "S S"], {"I": mc("iron_nugget"), "L": mc("leather"), "S": mc("stick")})
V.receta("trineo", ["S  ", "PPP", "I I"], {"S": mc("stick"), "P": mc("red_terracotta"), "I": mc("iron_ingot")})

# =========================================================================== POLICÍA NACIONAL
AZUL = (22, 32, 66, 255)
AZUL_CL = (60, 120, 200, 255)
NEGRO = (26, 26, 30, 255)
GRIS = (60, 62, 68, 255)
BLANCO = (240, 240, 240, 255)
AMARILLO = (240, 200, 40, 255)


def cabeza(d, col, filas=(8, 16), top=True):
    if top:
        d.rectangle((8, 0, 15, 7), fill=col)
    d.rectangle((0, filas[0], 31, filas[1] - 1), fill=col)


def cuerpo(d, col):
    d.rectangle((20, 16, 35, 19), fill=col)
    d.rectangle((16, 20, 39, 31), fill=col)


def brazos(d, col):
    d.rectangle((44, 16, 51, 19), fill=col)
    d.rectangle((40, 20, 55, 31), fill=col)


def piernas(d, col, desde=20):
    d.rectangle((4, 16, 11, 19), fill=col)
    d.rectangle((0, desde, 15, 31), fill=col)


def letras(d, x0, y0, color, n=7):
    """Rótulo «POLICÍA» en miniatura (rayitas)."""
    for i in range(n):
        d.point((x0 + i, y0), fill=color)
        if i % 2 == 0:
            d.point((x0 + i, y0 + 1), fill=color)


# uniforme de patrulla
im = armadura_vacia()
d = ImageDraw.Draw(im)
cabeza(d, AZUL, (8, 11))
d.rectangle((8, 11, 15, 11), fill=NEGRO)            # visera
d.rectangle((11, 8, 12, 9), fill=AMARILLO)          # emblema
d.rectangle((8, 10, 15, 10), fill=(200, 200, 200, 255))  # cinta
cuerpo(d, AZUL)
brazos(d, AZUL)
letras(d, 32, 23, BLANCO, 8)                        # «POLICÍA» en la espalda
d.rectangle((21, 22, 22, 23), fill=AMARILLO)        # placa en el pecho
d.rectangle((25, 22, 27, 22), fill=BLANCO)
for y, c in ((21, (200, 20, 30, 255)), (22, AMARILLO), (23, (200, 20, 30, 255))):  # bandera en la manga
    d.rectangle((40, y, 43, y), fill=c)
d.rectangle((40, 29, 55, 31), fill=(14, 22, 48, 255))
piernas(d, NEGRO, 26)                                # botas
d.rectangle((0, 26, 15, 26), fill=(50, 50, 54, 255))
guardar(im, TA, "policia_layer_1")
im = armadura_vacia()
d = ImageDraw.Draw(im)
piernas(d, AZUL)
d.rectangle((20, 16, 35, 19), fill=AZUL)
d.rectangle((16, 27, 39, 31), fill=AZUL)
d.rectangle((16, 27, 39, 27), fill=NEGRO)            # cinturón
d.rectangle((23, 27, 24, 27), fill=(200, 200, 200, 255))
d.rectangle((17, 28, 18, 30), fill=NEGRO)            # funda
d.rectangle((0, 24, 0, 31), fill=AZUL_CL)
d.rectangle((15, 24, 15, 31), fill=AZUL_CL)
guardar(im, TA, "policia_layer_2")

# GEO
im = armadura_vacia()
d = ImageDraw.Draw(im)
cabeza(d, NEGRO, (8, 13))
d.rectangle((10, 8, 13, 9), fill=GRIS)               # soporte de visión nocturna
d.rectangle((0, 12, 31, 12), fill=(40, 40, 44, 255))
cuerpo(d, (34, 38, 46, 255))
d.rectangle((20, 21, 27, 28), fill=NEGRO)            # placa balística
for x in (20, 23, 26):
    d.rectangle((x, 26, x + 1, 30), fill=GRIS)       # portacargadores
d.rectangle((32, 21, 39, 28), fill=NEGRO)
letras(d, 32, 23, BLANCO, 8)
brazos(d, (40, 46, 58, 255))
d.rectangle((40, 22, 43, 24), fill=GRIS)             # parche
piernas(d, NEGRO, 25)
guardar(im, TA, "geo_layer_1")
im = armadura_vacia()
d = ImageDraw.Draw(im)
piernas(d, (40, 46, 58, 255))
d.rectangle((4, 24, 7, 26), fill=NEGRO)              # rodilleras
d.rectangle((0, 22, 3, 26), fill=(30, 34, 42, 255))  # bolsillo de pierna
d.rectangle((20, 16, 35, 19), fill=(40, 46, 58, 255))
d.rectangle((16, 27, 39, 31), fill=(40, 46, 58, 255))
d.rectangle((16, 27, 39, 28), fill=NEGRO)
guardar(im, TA, "geo_layer_2")


# iconos de la ropa y del equipo
def ropa(n, es, en, dib):
    icono(n, dib, es, en)


ropa("gorra_policia", "Gorra de la Policía Nacional", "National Police Cap", lambda d, im: (
    d.rectangle((3, 6, 12, 10), fill=AZUL), d.rectangle((2, 10, 13, 11), fill=NEGRO), d.rectangle((7, 7, 8, 8), fill=AMARILLO),
    d.rectangle((3, 9, 12, 9), fill=(200, 200, 200, 255))))
ropa("chaqueta_policia", "Chaqueta de la Policía Nacional", "National Police Jacket", lambda d, im: (
    d.polygon([(4, 2), (11, 2), (14, 5), (12, 7), (12, 14), (3, 14), (3, 7), (1, 5)], fill=AZUL),
    d.rectangle((4, 9, 11, 9), fill=BLANCO), d.rectangle((5, 4, 6, 5), fill=AMARILLO), d.line((7, 2, 7, 14), fill=(10, 16, 36, 255))))
ropa("pantalon_policia", "Pantalón de la Policía Nacional", "National Police Trousers", lambda d, im: (
    d.polygon([(4, 2), (11, 2), (12, 14), (9, 14), (8, 6), (7, 6), (6, 14), (3, 14)], fill=AZUL), d.rectangle((4, 2, 11, 3), fill=NEGRO)))
ropa("botas_policia", "Botas de la Policía Nacional", "National Police Boots", lambda d, im: (
    d.polygon([(2, 5), (6, 5), (6, 11), (8, 13), (1, 13)], fill=NEGRO), d.polygon([(9, 5), (13, 5), (13, 11), (15, 13), (8, 13)], fill=NEGRO)))
ropa("casco_geo", "Casco balístico del GEO", "GEO Ballistic Helmet", lambda d, im: (
    d.chord((2, 3, 13, 15), 180, 360, fill=NEGRO), d.rectangle((2, 9, 13, 10), fill=NEGRO), d.rectangle((6, 3, 9, 5), fill=GRIS)))
ropa("chaleco_geo", "Chaleco táctico del GEO", "GEO Tactical Vest", lambda d, im: (
    d.polygon([(4, 2), (11, 2), (13, 5), (13, 14), (2, 14), (2, 5)], fill=(34, 38, 46, 255)), d.rectangle((4, 5, 11, 10), fill=NEGRO),
    d.rectangle((3, 11, 5, 13), fill=GRIS), d.rectangle((7, 11, 8, 13), fill=GRIS), d.rectangle((10, 11, 12, 13), fill=GRIS)))
ropa("pantalon_geo", "Pantalón táctico del GEO", "GEO Tactical Trousers", lambda d, im: (
    d.polygon([(4, 2), (11, 2), (12, 14), (9, 14), (8, 6), (7, 6), (6, 14), (3, 14)], fill=(40, 46, 58, 255)),
    d.rectangle((4, 9, 6, 10), fill=NEGRO), d.rectangle((9, 9, 11, 10), fill=NEGRO)))
ropa("botas_geo", "Botas tácticas del GEO", "GEO Tactical Boots", lambda d, im: (
    d.polygon([(2, 4), (6, 4), (6, 11), (8, 13), (1, 13)], fill=(20, 20, 22, 255)), d.polygon([(9, 4), (13, 4), (13, 11), (15, 13), (8, 13)], fill=(20, 20, 22, 255)),
    d.line((2, 6, 5, 6), fill=GRIS), d.line((9, 6, 12, 6), fill=GRIS)))


def d_placa(d, im):
    d.rounded_rectangle((1, 3, 14, 13), 2, fill=(60, 40, 24, 255))
    d.polygon([(8, 4), (11, 6), (11, 10), (8, 12), (5, 10), (5, 6)], fill=AMARILLO, outline=(170, 130, 20, 255))
    d.point((8, 8), fill=(30, 60, 160, 255))


icono("placa_policial", d_placa, "Placa de la Policía Nacional", "National Police Badge")


def d_esp(d, im):
    d.ellipse((1, 4, 8, 11), outline=(190, 194, 200, 255), width=2)
    d.ellipse((8, 4, 15, 11), outline=(190, 194, 200, 255), width=2)
    d.line((7, 7, 9, 7), fill=(150, 150, 156, 255))


icono("esposas", d_esp, "Esposas", "Handcuffs")


def d_porra(d, im):
    d.line((3, 13, 13, 3), fill=(20, 20, 22, 255), width=2)
    d.line((2, 14, 4, 12), fill=(60, 60, 64, 255), width=2)
    d.point((12, 4), fill=(110, 110, 116, 255))


icono("porra_policial", d_porra, "Defensa (porra policial)", "Police Baton", True)

# pistola y subfusil: el mismo modelo 3D que las armas del mod
for n, base, es, en in (("pistola_policia", "pistola", "Pistola reglamentaria 9 mm (Policía)", "Police 9 mm Service Pistol"),
                        ("subfusil_geo", "subfusil", "Subfusil del GEO 9 mm", "GEO 9 mm Submachine Gun")):
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("item/" + base)})
    nombre(f"item.{MOD}.{n}", es, en)

# escudo antidisturbios: policarbonato transparente con rótulo
im = Image.new("RGBA", (64, 64), (170, 200, 220, 110))
d = ImageDraw.Draw(im)
d.rectangle((0, 0, 63, 63), outline=(40, 40, 46, 255), width=3)
d.rectangle((3, 22, 60, 34), fill=(22, 32, 66, 220))
d.text((32, 28), "POLICÍA", font=fuente(10), fill=(255, 255, 255, 255), anchor="mm")
d.line((8, 6, 20, 18), fill=(255, 255, 255, 120), width=2)
guardar(im, TI, "escudo_antidisturbios")
el = {"from": [2, -4, 7], "to": [14, 20, 8], "faces": {"north": {"texture": "#t"}, "south": {"texture": "#t"},
                                                      "east": {"texture": "#m"}, "west": {"texture": "#m"}, "up": {"texture": "#m"},
                                                      "down": {"texture": "#m"}}}
asa = {"from": [7, 6, 8], "to": [9, 10, 10], "faces": {f: {"texture": "#m"} for f in ("north", "south", "east", "west", "up", "down")}}
normal = {"thirdperson_righthand": {"rotation": [0, 90, 0], "translation": [10, 6, -4], "scale": [1, 1, 1]},
          "thirdperson_lefthand": {"rotation": [0, 90, 0], "translation": [10, 6, 12], "scale": [1, 1, 1]},
          "firstperson_righthand": {"rotation": [0, 180, 5], "translation": [-10, 2, -10], "scale": [1.25, 1.25, 1.25]},
          "firstperson_lefthand": {"rotation": [0, 180, 5], "translation": [10, 0, -10], "scale": [1.25, 1.25, 1.25]},
          "gui": {"rotation": [15, -25, -5], "translation": [2, 3, 0], "scale": [0.65, 0.65, 0.65]},
          "fixed": {"rotation": [0, 180, 0], "translation": [-2, 4, -5], "scale": [0.5, 0.5, 0.5]},
          "ground": {"translation": [4, 4, 2], "scale": [0.25, 0.25, 0.25]}}
bloqueando = dict(normal)
bloqueando.update({"thirdperson_righthand": {"rotation": [45, 135, 0], "translation": [3.51, 11, -2], "scale": [1, 1, 1]},
                   "thirdperson_lefthand": {"rotation": [45, 135, 0], "translation": [13.51, 3, 5], "scale": [1, 1, 1]},
                   "firstperson_righthand": {"rotation": [0, 180, -5], "translation": [-15, 5, -11], "scale": [1.25, 1.25, 1.25]},
                   "firstperson_lefthand": {"rotation": [0, 180, -5], "translation": [5, 5, -11], "scale": [1.25, 1.25, 1.25]}})
tex = {"t": G("item/escudo_antidisturbios"), "m": G("block/mat_metal_negro"), "particle": G("item/escudo_antidisturbios")}
escribir(os.path.join(A, "models", "item", "escudo_antidisturbios_bloqueando.json"),
         {"render_type": "minecraft:translucent", "gui_light": "front", "textures": tex, "elements": [el, asa], "display": bloqueando})
escribir(os.path.join(A, "models", "item", "escudo_antidisturbios.json"),
         {"render_type": "minecraft:translucent", "gui_light": "front", "textures": tex, "elements": [el, asa], "display": normal,
          "overrides": [{"predicate": {"blocking": 1}, "model": G("item/escudo_antidisturbios_bloqueando")}]})
nombre(f"item.{MOD}.escudo_antidisturbios", "Escudo antidisturbios", "Riot Shield")

# recetas del equipo
V.receta("gorra_policia", ["BBB", "BGB"], {"B": mc("blue_wool"), "G": mc("gold_nugget")})
V.receta("chaqueta_policia", ["B B", "BGB", "BWB"], {"B": mc("blue_wool"), "G": mc("gold_nugget"), "W": mc("white_dye")})
V.receta("pantalon_policia", ["BLB", "B B", "B B"], {"B": mc("blue_wool"), "L": mc("leather")})
V.receta("botas_policia", ["L L", "B B"], {"L": mc("leather"), "B": mc("black_dye")})
V.receta("casco_geo", ["III", "IKI"], {"I": mc("iron_ingot"), "K": mc("black_dye")})
V.receta("chaleco_geo", ["I I", "IKI", "III"], {"I": mc("iron_ingot"), "K": mc("black_dye")})
V.receta("pantalon_geo", ["KIK", "K K", "K K"], {"K": mc("black_wool"), "I": mc("iron_ingot")})
V.receta("botas_geo", ["K K", "I I"], {"K": mc("black_wool"), "I": mc("iron_ingot")})
V.receta("placa_policial", [" G ", "GLG", " G "], {"G": mc("gold_nugget"), "L": mc("leather")})
V.receta("esposas", ["N N", "ICI"], {"N": mc("iron_nugget"), "I": mc("iron_ingot"), "C": mc("chain")})
V.receta("porra_policial", ["  K", " K ", "L  "], {"K": mc("black_concrete"), "L": mc("leather")})
V.receta("escudo_antidisturbios", ["GBG", "GGG", " I "], {"G": mc("glass_pane"), "B": mc("blue_dye"), "I": mc("iron_ingot")})
V.receta("pistola_policia", ["III", " KB"], {"I": mc("iron_ingot"), "K": mc("black_dye"), "B": mc("blue_dye")})
V.receta("subfusil_geo", ["III", "KIB", "  K"], {"I": mc("iron_ingot"), "K": mc("black_dye"), "B": mc("blue_dye")})

# ---------------------------------------------------------------------- bloques de policía
# cinta policial azul y blanca
im = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.rectangle((0, 12, 31, 23), fill=(250, 250, 250, 255))
for k in range(-12, 44, 8):
    d.polygon([(k, 12), (k + 4, 12), (k - 7, 23), (k - 11, 23)], fill=(30, 70, 170, 255))
d.text((16, 17.5), "POLICÍA", font=fuente(6), fill=(20, 20, 40, 255), anchor="mm")
guardar(im, TB, "cinta_policial")
V.placa("cinta_policial", "Cinta policial «No pasar»", "Police Tape", G("block/cinta_policial"), G("block/cinta_policial"), 0, 0, 16, 16, 0.3)
botin("cinta_policial")
V.receta("cinta_policial", ["WBW"], {"W": mc("white_wool"), "B": mc("blue_dye")}, 8)

# rotativo azul (destella)
frames = []
for k in range(8):
    im = Image.new("RGBA", (32, 32), (0, 0, 0, 255))
    a = np.zeros((32, 32, 4))
    xs = np.arange(32)
    centro = (k / 8) * 32
    dist = np.minimum(np.abs(xs - centro), 32 - np.abs(xs - centro))
    luz = np.exp(-(dist / 5.0) ** 2)
    a[..., 0] = 20 + 120 * luz[None, :]
    a[..., 1] = 60 + 140 * luz[None, :]
    a[..., 2] = 160 + 95 * luz[None, :]
    a[..., 3] = 255
    a[..., :3] *= (0.85 + 0.15 * np.sin(np.arange(32) / 3))[:, None, None]
    frames.append(V.img(a))
V.animada(frames, "luz_emergencia", 1, True)
escribir(os.path.join(A, "models", "block", "luz_emergencia.json"), {
    "parent": "minecraft:block/block", "render_type": "minecraft:cutout",
    "textures": {"l": G("block/luz_emergencia"), "b": G("block/mat_metal_negro"), "particle": G("block/luz_emergencia")},
    "elements": [{"from": [4, 0, 4], "to": [12, 2, 12], "faces": caras("#b")},
                 {"from": [5, 2, 5], "to": [11, 7, 11], "shade": False, "faces": caras("#l")}]})
escribir(os.path.join(A, "blockstates", "luz_emergencia.json"), {"variants": {"": {"model": G("block/luz_emergencia")}}})
escribir(os.path.join(A, "models", "item", "luz_emergencia.json"), {"parent": G("block/luz_emergencia")})
nombre(f"block.{MOD}.luz_emergencia", "Rotativo azul de emergencia", "Blue Emergency Light")
botin("luz_emergencia")
V.receta("luz_emergencia", [" G ", "GBG", "III"], {"G": mc("glass_pane"), "B": mc("blue_dye"), "I": mc("iron_ingot")}, 2)


# rótulo de comisaría (3 de ancho)
def dib_rotulo(im, d, w, h):
    d.rectangle((0, 0, w - 1, h - 1), fill=(22, 40, 110, 255))
    d.rectangle((6, 6, w - 7, h - 7), outline=(250, 250, 250, 255), width=3)
    d.text((w / 2, h / 2 + 2), "POLICÍA", font=fuente(70), fill=(255, 255, 255, 255), anchor="mm")
    d.text((w / 2, h - 18), "COMISARÍA", font=fuente(16), fill=(200, 220, 255, 255), anchor="mm")


V.ancho("rotulo_comisaria", dib_rotulo)
V.placa_ancha("rotulo_comisaria", "Rótulo «Policía» de comisaría (3 de ancho)", "Police Station Sign (3 wide)", G("block/rotulo_comisaria"),
              G("block/mat_lacado_gris"), 3, 13, 1, luz=True, render="minecraft:solid")
botin("rotulo_comisaria")
V.receta("rotulo_comisaria", ["BWB", "BGB"], {"B": mc("blue_concrete"), "W": mc("white_dye"), "G": mc("glowstone")})

# valla de contención
im = Image.new("RGBA", (64, 64), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
d.rectangle((0, 2, 63, 5), fill=(170, 174, 180, 255))
d.rectangle((0, 2, 3, 63), fill=(170, 174, 180, 255))
d.rectangle((60, 2, 63, 63), fill=(170, 174, 180, 255))
for x in range(8, 60, 6):
    d.rectangle((x, 6, x + 1, 56), fill=(160, 164, 170, 255))
d.rectangle((0, 52, 63, 55), fill=(170, 174, 180, 255))
d.rectangle((8, 20, 55, 36), fill=(30, 70, 170, 255))
d.text((32, 28), "POLICÍA", font=fuente(10), fill=(255, 255, 255, 255), anchor="mm")
guardar(im, TB, "valla_policial")
V.placa("valla_policial", "Valla de contención policial", "Police Crowd Barrier", G("block/valla_policial"), G("block/mat_inox"), 0, 0, 16, 16, 1)
botin("valla_policial")
V.receta("valla_policial", ["III", "IBI"], {"I": mc("iron_bars"), "B": mc("blue_dye")}, 3)


# coche patrulla y furgón del GEO (modelos grandes)
def lado_coche(furgon=False):
    w, h = 480, 160
    im = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if furgon:
        d.rectangle((0, 0, w, h), fill=(28, 34, 48, 255))
        d.rectangle((300, 10, 470, 60), fill=(20, 24, 30, 255))
        d.rectangle((0, 96, w, 104), fill=(60, 120, 200, 255))
        d.text((170, 76), "POLICÍA", font=fuente(34), fill=(200, 206, 214, 255), anchor="mm")
    else:
        d.rectangle((0, 0, w, 70), fill=(244, 244, 244, 255))
        d.rectangle((0, 70, w, h), fill=(20, 34, 86, 255))
        d.rectangle((0, 66, w, 72), fill=(70, 150, 230, 255))
        d.rectangle((150, 20, 152, 60), fill=(200, 200, 200, 255))  # manetas de las puertas
        d.rectangle((300, 20, 302, 60), fill=(200, 200, 200, 255))
        d.text((240, 104), "POLICÍA", font=fuente(38), fill=(255, 255, 255, 255), anchor="mm")
        d.text((240, 140), "091", font=fuente(16), fill=(200, 220, 255, 255), anchor="mm")
    for cx in (80, 400):
        d.ellipse((cx - 36, h - 50, cx + 36, h + 22), fill=(16, 16, 18, 255))
        d.ellipse((cx - 18, h - 32, cx + 18, h + 4), fill=(150, 154, 160, 255))
    d.line((300, 10, 300, h - 30), fill=(10, 10, 14, 255), width=2)
    d.line((150, 10, 150, h - 30), fill=(10, 10, 14, 255), width=2)
    return im.resize((256, 256), Image.LANCZOS)


def frente_coche(furgon=False, trasera=False):
    w, h = 200, 160
    im = Image.new("RGBA", (w, h), (244, 244, 244, 255) if not furgon else (28, 34, 48, 255))
    d = ImageDraw.Draw(im)
    if not furgon:
        d.rectangle((0, 80, w, h), fill=(20, 34, 86, 255))
    d.rectangle((16, 6, w - 16, 60), fill=(30, 38, 50, 255))
    c = (220, 30, 30, 255) if trasera else (255, 250, 220, 255)
    d.rectangle((8, 86, 46, 104), fill=c)
    d.rectangle((w - 46, 86, w - 8, 104), fill=c)
    d.rectangle((64, 110, w - 64, 128), fill=(250, 250, 250, 255), outline=(40, 40, 40, 255))
    d.text((w / 2, 119), "1234 KLM", font=fuente(11), fill=(20, 20, 20, 255), anchor="mm")
    d.text((w / 2, 74), "POLICÍA", font=fuente(18), fill=(20, 34, 86, 255) if not furgon else (200, 206, 214, 255), anchor="mm")
    return im.resize((128, 128), Image.LANCZOS)


for furgon, pref in ((False, "coche"), (True, "furgon")):
    guardar(lado_coche(furgon), TB, pref + "_policia_lado")
    guardar(frente_coche(furgon), TB, pref + "_policia_frente")
    guardar(frente_coche(furgon, True), TB, pref + "_policia_trasera")
    guardar(V.ruido((244, 244, 244) if not furgon else (28, 34, 48), 3), TB, pref + "_policia_techo")
# barra de luces: encendida (destella azul) y apagada
frames = []
for k in range(4):
    im = Image.new("RGBA", (32, 32), (30, 30, 36, 255))
    d = ImageDraw.Draw(im)
    izq = k % 2 == 0
    d.rectangle((1, 4, 14, 27), fill=(60, 140, 255, 255) if izq else (20, 40, 110, 255))
    d.rectangle((17, 4, 30, 27), fill=(20, 40, 110, 255) if izq else (60, 140, 255, 255))
    frames.append(im)
V.animada(frames, "barra_luces_on", 3, False)
im = Image.new("RGBA", (32, 32), (30, 30, 36, 255))
d = ImageDraw.Draw(im)
d.rectangle((1, 4, 14, 27), fill=(24, 44, 100, 255))
d.rectangle((17, 4, 30, 27), fill=(24, 44, 100, 255))
guardar(im, TB, "barra_luces_off")
caras4 = lambda: None


def modelo_coche(pref, encendido, furgon):
    alto = 24 if furgon else 13
    els = [
        {"from": [-4, 3, -14], "to": [20, alto, 30], "faces": {
            "east": {"texture": "#lado"}, "west": {"texture": "#lado"}, "north": {"texture": "#frente"}, "south": {"texture": "#trasera"},
            "up": {"texture": "#techo"}, "down": {"texture": "#bajos"}}},
    ]
    if not furgon:
        els.append({"from": [-3, 13, -2], "to": [19, 21, 22], "faces": {
            "east": {"texture": "#ventana"}, "west": {"texture": "#ventana"}, "north": {"texture": "#ventana"},
            "south": {"texture": "#ventana"}, "up": {"texture": "#techo"}}})
    y0 = 21 if not furgon else alto
    els.append({"from": [0, y0, 6], "to": [16, y0 + 2, 10], "shade": False, "faces": {f: {"texture": "#luz"} for f in ("north", "south", "east", "west", "up")}})
    for x0, z0 in ((-4.5, -10), (16.5, -10), (-4.5, 20), (16.5, 20)):
        els.append({"from": [x0, 0, z0], "to": [x0 + 4, 6, z0 + 6], "faces": {f: {"texture": "#rueda"} for f in ("north", "south", "east", "west", "up", "down")}})
    m = {"parent": "minecraft:block/block", "ambientocclusion": False,
         "textures": {"lado": G(f"block/{pref}_policia_lado"), "frente": G(f"block/{pref}_policia_frente"),
                      "trasera": G(f"block/{pref}_policia_trasera"), "techo": G(f"block/{pref}_policia_techo"),
                      "bajos": G("block/metro_bajos"), "ventana": G("block/metro_bajos"), "rueda": G("block/mat_metal_negro"),
                      "luz": G("block/barra_luces_on" if encendido else "block/barra_luces_off"), "particle": G(f"block/{pref}_policia_lado")},
         "elements": els}
    return m


for n, pref, furgon, es, en in (("coche_patrulla", "coche", False, "Coche patrulla de la Policía Nacional (3 de largo)", "National Police Patrol Car (3 long)"),
                                 ("furgon_geo", "furgon", True, "Furgón del GEO (3 de largo)", "GEO Van (3 long)")):
    escribir(os.path.join(A, "models", "block", n + ".json"), modelo_coche(pref, False, furgon))
    escribir(os.path.join(A, "models", "block", n + "_on.json"), modelo_coche(pref, True, furgon))
    V.estados_facing(n, extra={"lit=false": G("block/" + n), "lit=true": G("block/" + n + "_on")})
    escribir(os.path.join(A, "models", "item", n + ".json"), {"parent": G("block/" + n), "display": {
        "gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
        "ground": {"translation": [0, 3, 0], "scale": [0.15, 0.15, 0.15]},
        "fixed": {"scale": [0.3, 0.3, 0.3]},
        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.2, 0.2, 0.2]},
        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.2, 0.2, 0.2]},
        "firstperson_lefthand": {"rotation": [0, 225, 0], "scale": [0.2, 0.2, 0.2]}}})
    nombre(f"block.{MOD}.{n}", es, en)
    botin(n)
V.receta("coche_patrulla", [" L ", "WBW", "IRI"], {"L": G("luz_emergencia"), "W": mc("white_concrete"), "B": mc("blue_concrete"),
                                                  "I": mc("iron_block"), "R": mc("redstone")})
V.receta("furgon_geo", [" L ", "KKK", "IRI"], {"L": G("luz_emergencia"), "K": mc("black_concrete"), "I": mc("iron_block"), "R": mc("redstone")})

# pestañas
nombre(f"itemGroup.{MOD}.invierno", "Invierno: nieve y esquí", "Winter: Snow & Skiing")
nombre(f"itemGroup.{MOD}.policia", "Policía Nacional y GEO", "National Police & GEO")

# etiquetas, recetas repetidas e idioma
V.tag(DMC, "mineable/pickaxe", [G(n) for n in BOTIN])
firmas = {}
for f in os.listdir(os.path.join(D, "recipes")):
    r = json.load(open(os.path.join(D, "recipes", f), encoding="utf-8"))
    if r["type"] == "minecraft:crafting_shapeless":
        k = ("sl", tuple(sorted(json.dumps(i, sort_keys=True) for i in r["ingredients"])))
    elif r["type"] == "minecraft:crafting_shaped":
        k = ("sh", tuple(r["pattern"]), tuple(sorted((c, json.dumps(v, sort_keys=True)) for c, v in r["key"].items())))
    else:
        continue
    firmas.setdefault(k, []).append(f)
repes = [v for v in firmas.values() if len(v) > 1]
if repes:
    raise SystemExit("Recetas repetidas: " + "; ".join(" = ".join(v) for v in repes))
for archivo, nombres in (("es_es.json", V.lang_es), ("en_us.json", V.lang_en)):
    ruta = os.path.join(A, "lang", archivo)
    actual = json.load(open(ruta, encoding="utf-8"))
    actual.update(nombres)
    escribir(ruta, actual)
print(f"v8_invierno_policia: {len(BOTIN)} bloques, {len(V.lang_es)} nombres")

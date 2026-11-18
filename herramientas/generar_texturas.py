#!/usr/bin/env python3
"""Dibuja todas las texturas del mod (pixel art procedural) y genera los sonidos .ogg.
Ejecutar desde la carpeta del proyecto:  python3 herramientas/generar_texturas.py
Necesita Pillow, numpy y ffmpeg (para los sonidos).
"""
import math
import os
import random
import shutil
import subprocess
import tempfile
import wave

import numpy as np
from PIL import Image, ImageDraw

MOD = "guerramod"
A = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", MOD)
TB = os.path.join(A, "textures", "block")
TI = os.path.join(A, "textures", "item")
for d in [TB, TI, os.path.join(A, "textures", "entity"), os.path.join(A, "textures", "models", "armor"),
          os.path.join(A, "textures", "mob_effect"), os.path.join(A, "sounds")]:
    os.makedirs(d, exist_ok=True)


def nueva(w=16, h=16, color=(0, 0, 0, 0)):
    return Image.new("RGBA", (w, h), color)


def ruido(img, base, var=10, semilla=0, caja=None):
    r = random.Random(semilla)
    x0, y0, x1, y1 = caja or (0, 0, img.width - 1, img.height - 1)
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            v = r.randint(-var, var)
            img.putpixel((x, y), tuple(max(0, min(255, c + v)) for c in base[:3]) + (255,))
    return img


def rect(img, x0, y0, x1, y1, c):
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], fill=c)


def borde(img, c, x0=0, y0=0, x1=None, y1=None):
    x1 = img.width - 1 if x1 is None else x1
    y1 = img.height - 1 if y1 is None else y1
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], outline=c)


def px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c if len(c) == 4 else c + (255,))


def linea(img, a, b, c, w=1):
    ImageDraw.Draw(img).line([a, b], fill=c, width=w)


def elipse(img, caja, c, contorno=None):
    ImageDraw.Draw(img).ellipse(caja, fill=c, outline=contorno)


def oscuro(c, f=0.7):
    return tuple(int(v * f) for v in c[:3]) + (255,)


def claro(c, f=1.3):
    return tuple(min(255, int(v * f)) for v in c[:3]) + (255,)


def guardar(img, carpeta, nombre):
    img.save(os.path.join(carpeta, nombre + ".png"))


def remache(img, x, y):
    px(img, x, y, (210, 210, 215))
    px(img, x + 1, y + 1, (60, 60, 65))


def trebol(img, cx, cy, r, color, fondo):
    """Símbolo de radiactividad aproximado."""
    for y in range(img.height):
        for x in range(img.width):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            ang = (math.degrees(math.atan2(dy, dx)) + 90) % 120
            if d <= r * 0.22:
                px(img, x, y, color)
            elif r * 0.32 <= d <= r and ang < 60:
                px(img, x, y, color)


# ====================================================================== BLOQUES
ASF = (52, 52, 56)
img = ruido(nueva(), ASF, 9, 1)
guardar(img, TB, "asfalto")

img = ruido(nueva(), ASF, 9, 2)
rect(img, 7, 1, 8, 6, (235, 235, 230, 255))
rect(img, 7, 9, 8, 14, (235, 235, 230, 255))
guardar(img, TB, "asfalto_linea")

img = ruido(nueva(), ASF, 9, 3)
rect(img, 7, 0, 8, 15, (235, 235, 230, 255))
guardar(img, TB, "asfalto_linea_continua")

img = ruido(nueva(), ASF, 9, 4)
rect(img, 2, 0, 5, 15, (235, 235, 230, 255))
rect(img, 10, 0, 13, 15, (235, 235, 230, 255))
guardar(img, TB, "paso_cebra")

img = ruido(nueva(), (128, 130, 134), 7, 5)
borde(img, (90, 92, 96, 255))
for (x, y) in [(2, 2), (12, 2), (2, 12), (12, 12), (7, 2), (7, 12), (2, 7), (12, 7)]:
    remache(img, x, y)
guardar(img, TB, "hormigon_blindado")

img = nueva()
borde(img, (80, 92, 104, 255))
borde(img, (120, 135, 150, 255), 1, 1, 14, 14)
for i in range(3, 8):
    px(img, i, 10 - i, (225, 240, 250))
for i in range(9, 12):
    px(img, i, 20 - i, (225, 240, 250))
guardar(img, TB, "cristal_blindado")

img = nueva(color=(85, 70, 45, 255))
r = random.Random(6)
for fila in range(4):
    off = 0 if fila % 2 == 0 else -4
    for col in range(3):
        x0 = off + col * 8
        y0 = fila * 4
        elipse(img, [x0, y0, x0 + 7, y0 + 3], (190 + r.randint(-12, 12), 165, 112, 255), (120, 98, 62, 255))
guardar(img, TB, "sacos_terreros")

img = ruido(nueva(), (84, 87, 92), 6, 7)
for i in range(16):
    px(img, i, 7, (62, 64, 68))
    px(img, 7, i, (62, 64, 68))
for (x, y) in [(3, 3), (11, 12), (13, 4), (4, 11)]:
    px(img, x, y, (120, 75, 50))
borde(img, (60, 62, 66, 255))
guardar(img, TB, "hormigon_reforzado")

img = nueva()
for y in range(16):
    for x in range(16):
        v = int(110 + 25 * math.sin((x + y) / 5.0))
        img.putpixel((x, y), (v - 5, v + 2, v + 15, 255))
borde(img, (80, 86, 98, 255))
linea(img, (2, 3), (6, 3), (190, 198, 210, 255))
guardar(img, TB, "bloque_plomo")

img = ruido(nueva(), (150, 155, 160), 5, 8)
borde(img, (95, 98, 102, 255))
elipse(img, [2, 2, 13, 13], (45, 48, 52, 255), (95, 98, 102, 255))
for a in range(0, 360, 60):
    linea(img, (7.5, 7.5), (7.5 + 5 * math.cos(math.radians(a)), 7.5 + 5 * math.sin(math.radians(a))), (150, 155, 160, 255))
rect(img, 6, 6, 9, 9, (90, 200, 230, 255))
guardar(img, TB, "purificador_aire")

img = ruido(nueva(), (215, 215, 210), 5, 9)
rect(img, 0, 4, 15, 9, (70, 120, 170, 255))
rect(img, 1, 5, 14, 8, (130, 180, 220, 255))
linea(img, (5, 5), (5, 8), (70, 120, 170, 255))
linea(img, (10, 5), (10, 8), (70, 120, 170, 255))
rect(img, 0, 12, 15, 13, (190, 30, 30, 255))
borde(img, (140, 140, 135, 255))
guardar(img, TB, "puesto_control")

img = nueva(color=(25, 35, 28, 255))
elipse(img, [1, 1, 14, 14], (20, 60, 30, 255), (60, 200, 90, 255))
elipse(img, [4, 4, 11, 11], None, (40, 140, 60, 255))
linea(img, (7, 7), (13, 3), (120, 255, 140, 255))
guardar(img, TB, "radar_arriba")
img = ruido(nueva(), (120, 125, 130), 5, 10)
for y in [3, 6, 9]:
    linea(img, (3, y), (12, y), (60, 62, 66, 255))
rect(img, 12, 12, 13, 13, (80, 255, 110, 255))
borde(img, (80, 82, 86, 255))
guardar(img, TB, "radar_lado")

img = ruido(nueva(), (110, 112, 116), 5, 11)
for y in range(2, 14, 3):
    for x in range(2, 14, 3):
        rect(img, x, y, x + 1, y + 1, (30, 30, 32, 255))
guardar(img, TB, "sirena_arriba")
img = ruido(nueva(), (180, 35, 30), 8, 12)
for y in range(3, 13, 3):
    linea(img, (2, y), (13, y), (60, 15, 12, 255))
borde(img, (110, 20, 18, 255))
guardar(img, TB, "sirena_lado")

OLIVA = (88, 98, 58)
img = ruido(nueva(), OLIVA, 6, 13)
for i in range(16):
    c = (235, 190, 30, 255) if (i // 2) % 2 == 0 else (25, 25, 25, 255)
    px(img, i, 0, c); px(img, i, 15, c); px(img, 0, i, c); px(img, 15, i, c)
elipse(img, [3, 3, 12, 12], (70, 78, 45, 255), (40, 45, 25, 255))
linea(img, (4, 7), (11, 7), (40, 45, 25, 255))
guardar(img, TB, "lanzador_misiles_arriba")
img = ruido(nueva(), OLIVA, 6, 14)
for x in range(16):
    for y in range(10, 13):
        px(img, x, y, (235, 190, 30) if ((x + y) // 2) % 2 == 0 else (25, 25, 25))
linea(img, (0, 4), (15, 4), (60, 68, 38, 255))
borde(img, (55, 62, 35, 255))
guardar(img, TB, "lanzador_misiles_lado")

AMA = (238, 196, 30)
img = ruido(nueva(), AMA, 5, 15)
trebol(img, 8, 8, 7, (20, 20, 20), AMA)
borde(img, (20, 20, 20, 255))
guardar(img, TB, "bomba_nuclear_arriba")
img = ruido(nueva(), AMA, 5, 16)
for x in range(16):
    for y in list(range(0, 3)) + list(range(13, 16)):
        px(img, x, y, (20, 20, 20) if ((x + y) // 2) % 2 == 0 else AMA)
trebol(img, 8, 8, 4.6, (20, 20, 20), AMA)
guardar(img, TB, "bomba_nuclear_lado")

MAD = (92, 58, 32)
img = ruido(nueva(), MAD, 6, 17)
rect(img, 2, 2, 13, 13, (30, 95, 55, 255))
rect(img, 4, 4, 8, 10, (240, 238, 228, 255))
linea(img, (5, 5), (7, 5), (90, 90, 90, 255)); linea(img, (5, 7), (7, 7), (90, 90, 90, 255))
rect(img, 10, 4, 11, 6, (200, 30, 30, 255))
linea(img, (10, 4), (10, 9), (60, 40, 20, 255))
guardar(img, TB, "mesa_gobierno_arriba")
img = ruido(nueva(), MAD, 6, 18)
for y in [4, 9, 14]:
    linea(img, (0, y), (15, y), oscuro(MAD + (255,), 0.65))
rect(img, 6, 6, 9, 7, (210, 180, 60, 255))
guardar(img, TB, "mesa_gobierno_lado")
guardar(ruido(nueva(), oscuro(MAD, 0.8), 5, 19), TB, "mesa_gobierno_abajo")

img = ruido(nueva(), (120, 124, 128), 5, 20)
for y in [0, 5, 10, 15]:
    linea(img, (0, y), (15, y), (80, 82, 86, 255))
for x in [0, 8, 15]:
    linea(img, (x, 0), (x, 15), (80, 82, 86, 255))
guardar(img, TB, "valla_fronteriza")

img = ruido(nueva(), (175, 175, 170), 5, 21)
linea(img, (0, 7), (15, 7), (130, 130, 126, 255))
linea(img, (0, 15), (15, 15), (130, 130, 126, 255))
linea(img, (5, 0), (5, 7), (130, 130, 126, 255))
linea(img, (12, 8), (12, 15), (130, 130, 126, 255))
guardar(img, TB, "muro_fronterizo")

img = ruido(nueva(), (235, 235, 230), 4, 22)
rect(img, 0, 5, 15, 8, (200, 30, 30, 255))
rect(img, 0, 0, 15, 1, (90, 90, 90, 255))
guardar(img, TB, "hito_frontera")

guardar(ruido(nueva(), (55, 58, 62), 4, 23), TB, "farola_poste")
img = ruido(nueva(), (255, 240, 170), 6, 24)
borde(img, (60, 62, 66, 255))
guardar(img, TB, "farola_luz")

img = nueva()
for y in range(16):
    for x in range(16):
        img.putpixel((x, y), (215, 30, 30, 255) if ((x + y) // 4) % 2 == 0 else (240, 240, 240, 255))
guardar(img, TB, "barrera_control")

img = nueva()
for x in range(16):
    for base_y in (4, 11):
        y = base_y + int(round(1.5 * math.sin(x * 1.2)))
        px(img, x, y, (150, 150, 155))
        if x % 4 == 0:
            px(img, x, y - 1, (190, 190, 195)); px(img, x, y + 1, (190, 190, 195))
for y in range(16):
    px(img, 7, y, (110, 110, 115)) if y % 2 == 0 else None
guardar(img, TB, "alambre_espino")

img = nueva(color=(70, 78, 45, 255))
elipse(img, [1, 1, 14, 14], (95, 105, 60, 255), (45, 50, 28, 255))
elipse(img, [5, 5, 10, 10], (60, 66, 38, 255), (35, 38, 20, 255))
rect(img, 7, 7, 8, 8, (180, 40, 30, 255))
guardar(img, TB, "mina_terrestre")

img = ruido(nueva(), (205, 220, 232), 4, 25)
borde(img, (150, 160, 170, 255))
rect(img, 4, 9, 7, 13, (250, 250, 250, 255))
rect(img, 8, 10, 11, 14, (240, 240, 235, 255))
guardar(img, TB, "urna_lado")
img = ruido(nueva(), (60, 70, 120), 4, 26)
rect(img, 4, 7, 11, 8, (15, 15, 20, 255))
borde(img, (40, 46, 80, 255))
guardar(img, TB, "urna_arriba")

ACERO = (118, 124, 130)
for nombre, ventana in [("puerta_bunker_arriba", True), ("puerta_bunker_abajo", False)]:
    img = ruido(nueva(), ACERO, 5, 27 if ventana else 28)
    borde(img, (70, 74, 78, 255))
    borde(img, (95, 100, 105, 255), 1, 1, 14, 14)
    if ventana:
        rect(img, 5, 4, 10, 8, (40, 60, 70, 255))
        borde(img, (60, 64, 68, 255), 4, 3, 11, 9)
        rect(img, 2, 14, 13, 15, (235, 190, 30, 255))
    else:
        elipse(img, [4, 2, 11, 9], None, (200, 50, 40, 255))
        linea(img, (7, 2), (7, 9), (200, 50, 40, 255)); linea(img, (4, 5), (11, 5), (200, 50, 40, 255))
        rect(img, 2, 0, 13, 1, (235, 190, 30, 255))
    for (x, y) in [(2, 2), (12, 2), (2, 12), (12, 12)]:
        remache(img, x, y)
    guardar(img, TB, nombre)

# ====================================================================== BANDERAS


def estrella(x, y, cx, cy, r, puntas=5, rot=-90, interior=0.4):
    pts = []
    for i in range(puntas * 2):
        rr = r if i % 2 == 0 else r * interior
        a = math.radians(rot + i * 180 / puntas)
        pts.append((cx + rr * math.cos(a), cy + rr * math.sin(a)))
    dentro = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if (yi > y) != (yj > y) and x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi:
            dentro = not dentro
        j = i
    return dentro


def rayas_h(colores):
    return lambda x, y: colores[min(len(colores) - 1, int(y / 2 * len(colores)))]


def rayas_v(colores):
    return lambda x, y: colores[min(len(colores) - 1, int(x / 3 * len(colores)))]


ROJO, BLANCO, AZUL = (200, 16, 46), (250, 250, 250), (0, 40, 120)


def espana(x, y):
    c = (198, 11, 30) if (y < 0.5 or y >= 1.5) else (255, 196, 0)
    if 0.55 <= x <= 0.95 and 0.65 <= y <= 1.3:
        c = (170, 21, 27) if x < 0.75 else (240, 240, 235)
        if y > 1.05:
            c = (255, 196, 0) if x < 0.75 else (170, 21, 27)
        if abs(x - 0.55) < 0.04 or abs(x - 0.95) < 0.04 or abs(y - 1.3) < 0.04:
            c = (120, 70, 20)
    if 0.6 <= x <= 0.9 and 0.5 <= y < 0.65:
        c = (230, 170, 30)
    return c


def portugal(x, y):
    c = (0, 102, 0) if x < 1.2 else (255, 0, 0)
    d = math.hypot(x - 1.2, y - 1)
    if 0.32 < d < 0.45:
        c = (255, 220, 0)
    if abs(x - 1.2) < 0.2 and abs(y - 1) < 0.25:
        c = (250, 250, 250) if abs(x - 1.2) < 0.13 and abs(y - 1) < 0.18 else (200, 0, 0)
    return c


def reino_unido(x, y):
    c = (1, 33, 105)
    d1 = abs(2 * x - 3 * y) / math.sqrt(13)
    d2 = abs(2 * x + 3 * y - 6) / math.sqrt(13)
    if d1 < 0.2 or d2 < 0.2:
        c = BLANCO
    if (d1 < 0.07 and ((x < 1.5) == (y < 1))) or (d2 < 0.07 and ((x < 1.5) != (y < 1))):
        c = (200, 16, 46)
    if abs(x - 1.5) < 0.33 or abs(y - 1) < 0.33:
        c = BLANCO
    if abs(x - 1.5) < 0.2 or abs(y - 1) < 0.2:
        c = (200, 16, 46)
    return c


def eeuu(x, y):
    c = (178, 34, 52) if int(y / 2 * 13) % 2 == 0 else BLANCO
    if x < 1.2 and y < 2 * 7 / 13:
        c = (60, 59, 110)
        for i in range(6):
            for j in range(5):
                sx, sy = 0.1 + i * 0.2, 0.1 + j * 0.205
                if math.hypot(x - sx, y - sy) < 0.045:
                    c = BLANCO
    return c


def mexico(x, y):
    c = rayas_v([(0, 104, 71), BLANCO, (206, 17, 38)])(x, y)
    if math.hypot(x - 1.5, y - 1) < 0.28:
        c = (139, 90, 43) if math.hypot(x - 1.5, y - 1) < 0.2 else (0, 104, 71)
    return c


def argentina(x, y):
    c = (116, 172, 223) if (y < 2 / 3 or y >= 4 / 3) else BLANCO
    d = math.hypot(x - 1.5, y - 1)
    if d < 0.17:
        c = (246, 180, 14)
    elif d < 0.28 and int(math.degrees(math.atan2(y - 1, x - 1.5)) / 22.5) % 2 == 0:
        c = (246, 180, 14)
    return c


def brasil(x, y):
    c = (0, 156, 59)
    if abs(x - 1.5) / 1.3 + abs(y - 1) / 0.82 <= 1:
        c = (255, 223, 0)
    if math.hypot(x - 1.5, y - 1) < 0.5:
        c = (0, 39, 118)
        if abs(math.hypot(x - 1.2, y - 2.3) - 1.55) < 0.06:
            c = BLANCO
    return c


def japon(x, y):
    return (188, 0, 45) if math.hypot(x - 1.5, y - 1) < 0.6 else BLANCO


def china(x, y):
    am = (255, 222, 0)
    if estrella(x, y, 0.5, 0.5, 0.3):
        return am
    for (sx, sy, rot) in [(1.0, 0.2, -60), (1.2, 0.4, -80), (1.2, 0.7, -100), (1.0, 0.9, -120)]:
        if estrella(x, y, sx, sy, 0.1, rot=rot):
            return am
    return (238, 28, 37)


def canada(x, y):
    rj = (216, 6, 33)
    if x < 0.75 or x > 2.25:
        return rj
    if estrella(x, y, 1.5, 0.92, 0.55, puntas=7, interior=0.55) or (abs(x - 1.5) < 0.04 and 1.2 < y < 1.6):
        return rj
    return BLANCO


def suecia(x, y):
    if 0.9375 <= x <= 1.3125 or 0.8 <= y <= 1.2:
        return (254, 204, 0)
    return (0, 106, 167)


def grecia(x, y):
    az = (13, 94, 175)
    c = az if int(y / 2 * 9) % 2 == 0 else BLANCO
    if x < 1.111 and y < 1.111:
        c = BLANCO if (abs(x - 0.555) < 0.111 or abs(y - 0.555) < 0.111) else az
    return c


def colombia(x, y):
    return (252, 209, 22) if y < 1 else (0, 56, 147) if y < 1.5 else (206, 17, 38)


BANDERAS = {
    "espana": espana,
    "francia": rayas_v([(0, 35, 149), BLANCO, (237, 41, 57)]),
    "alemania": rayas_h([(20, 20, 20), (221, 0, 0), (255, 206, 0)]),
    "italia": rayas_v([(0, 146, 70), BLANCO, (206, 43, 55)]),
    "portugal": portugal,
    "reino_unido": reino_unido,
    "estados_unidos": eeuu,
    "mexico": mexico,
    "argentina": argentina,
    "brasil": brasil,
    "japon": japon,
    "ucrania": rayas_h([(0, 87, 183), (255, 215, 0)]),
    "rusia": rayas_h([BLANCO, (0, 57, 166), (213, 43, 30)]),
    "china": china,
    "canada": canada,
    "polonia": rayas_h([BLANCO, (220, 20, 60)]),
    "paises_bajos": rayas_h([(174, 28, 40), BLANCO, (33, 70, 139)]),
    "belgica": rayas_v([(20, 20, 20), (253, 218, 36), (239, 51, 64)]),
    "suecia": suecia,
    "grecia": grecia,
    "colombia": colombia,
    "irlanda": rayas_v([(22, 155, 98), BLANCO, (255, 136, 62)]),
}
N = 64
SS = 3
for nombre, f in BANDERAS.items():
    grande = Image.new("RGB", (N * SS, N * SS))
    pix = grande.load()
    for j in range(N * SS):
        for i in range(N * SS):
            pix[i, j] = f((i + 0.5) / (N * SS) * 3, (j + 0.5) / (N * SS) * 2)
    peq = grande.resize((N, N), Image.LANCZOS).convert("RGBA")
    guardar(peq, TB, "bandera_" + nombre)

# ====================================================================== ITEMS
K = (28, 28, 30, 255)
GR = (95, 98, 104, 255)
GRC = (150, 154, 160, 255)
MADERA = (122, 80, 45, 255)
MADERA_O = (82, 52, 28, 255)


def arma(nombre, culata, cuerpo, canon, cargador=None, mira=None, extra=None, color=GR):
    img = nueva()
    if culata:
        x0, x1 = culata
        rect(img, x0, 7, x1, 9, MADERA)
        rect(img, x0, 10, x0 + 1, 10, MADERA_O)
        linea(img, (x0, 7), (x1, 7), claro(MADERA, 1.2))
    x0, x1 = cuerpo
    rect(img, x0, 6, x1, 9, color)
    linea(img, (x0, 6), (x1, 6), claro(color, 1.4))
    linea(img, (x0, 9), (x1, 9), oscuro(color))
    c0, c1, cy = canon
    rect(img, c0, cy, c1, cy + 1, K)
    linea(img, (c0, cy), (c1, cy), (70, 70, 75, 255))
    if cargador:
        mx, mh = cargador
        rect(img, mx, 10, mx + 1, 10 + mh, K)
    if mira:
        mx0, mx1 = mira
        rect(img, mx0, 4, mx1, 5, K)
        px(img, mx1, 4, (120, 200, 255))
    # empuñadura
    gx = x0 + (2 if culata else 1)
    rect(img, gx, 10, gx + 1, 13, MADERA_O if culata else K)
    if extra:
        extra(img)
    guardar(img, TI, nombre)


arma("pistola", None, (3, 9), (9, 12, 6), None)
arma("fusil", (0, 3), (4, 10), (10, 15, 7), (7, 3), extra=lambda i: px(i, 14, 6, K))
arma("escopeta", (0, 4), (5, 9), (9, 15, 7), None, extra=lambda i: rect(i, 10, 9, 13, 9, MADERA))
arma("francotirador", (0, 4), (5, 9), (9, 15, 7), (7, 2), mira=(5, 9))


def lanzacohetes_extra(i):
    rect(i, 13, 5, 15, 10, (110, 125, 70, 255))
    rect(i, 0, 5, 2, 10, (60, 65, 40, 255))
    px(i, 15, 7, (200, 40, 30))


arma("lanzacohetes", None, (0, 15), (0, 15, 7), None, mira=(6, 8), extra=lanzacohetes_extra, color=(95, 108, 60, 255))

img = nueva()
rect(img, 2, 3, 13, 12, GRC)
borde(img, (90, 94, 100, 255), 2, 3, 13, 12)
linea(img, (3, 4), (10, 4), (210, 214, 220, 255))
for (x, y) in [(3, 5), (11, 5), (3, 10), (11, 10)]:
    px(img, x, y, (80, 82, 86))
guardar(img, TI, "placa_acero")

img = nueva()
rect(img, 2, 3, 13, 12, (40, 120, 60, 255))
borde(img, (25, 80, 40, 255), 2, 3, 13, 12)
for y in [5, 7, 10]:
    linea(img, (3, y), (12, y), (220, 180, 60, 255))
rect(img, 6, 5, 9, 9, K)
guardar(img, TI, "circuito")

img = nueva()
for i, x in enumerate([3, 7, 11]):
    rect(img, x, 6, x + 2, 13, (210, 160, 60, 255))
    linea(img, (x, 6), (x, 13), (240, 200, 100, 255))
    rect(img, x, 3, x + 2, 5, (190, 110, 60, 255))
    px(img, x + 1, 2, (190, 110, 60))
    linea(img, (x, 13), (x + 2, 13), (150, 100, 40, 255))
guardar(img, TI, "municion")

img = nueva()
elipse(img, [5, 5, 10, 10], (255, 220, 120, 255), (255, 170, 40, 255))
guardar(img, TI, "proyectil_bala")


def vertical(nombre, cuerpo, punta, aletas, simbolo=None):
    img = nueva()
    rect(img, 6, 3, 9, 12, cuerpo)
    linea(img, (6, 3), (6, 12), claro(cuerpo, 1.25))
    linea(img, (9, 3), (9, 12), oscuro(cuerpo, 0.75))
    rect(img, 7, 1, 8, 2, punta)
    px(img, 6, 2, punta); px(img, 9, 2, punta)
    rect(img, 4, 11, 5, 14, aletas); rect(img, 10, 11, 11, 14, aletas); rect(img, 7, 13, 8, 14, aletas)
    if simbolo:
        simbolo(img)
    guardar(img, TI, nombre)


vertical("cohete", (110, 125, 70, 255), (200, 40, 30, 255), (60, 65, 40, 255))
vertical("misil_balistico", (225, 225, 225, 255), (200, 40, 30, 255), K)
vertical("misil_nuclear", (240, 200, 40, 255), K, K,
         simbolo=lambda i: [px(i, 7, 6, K), px(i, 8, 6, K), px(i, 7, 8, K), px(i, 8, 8, K), px(i, 6, 7, K), px(i, 9, 7, K)])

img = nueva()
elipse(img, [5, 4, 10, 14], (55, 70, 45, 255), (30, 40, 25, 255))
rect(img, 6, 1, 9, 3, K)
linea(img, (5, 2), (5, 3), K); linea(img, (10, 2), (10, 3), K)
px(img, 6, 6, (90, 110, 75))
guardar(img, TI, "bomba_aerea")

img = nueva()
elipse(img, [4, 5, 11, 14], (70, 90, 50, 255), (35, 45, 25, 255))
linea(img, (5, 9), (10, 9), (45, 58, 32, 255)); linea(img, (7, 6), (7, 13), (45, 58, 32, 255))
rect(img, 6, 3, 9, 4, GR)
linea(img, (9, 3), (12, 7), GRC)
elipse(img, [10, 1, 13, 4], None, (210, 180, 60, 255))
guardar(img, TI, "granada")

img = nueva()
rect(img, 7, 1, 8, 14, (150, 155, 160, 255))
rect(img, 1, 6, 14, 8, (130, 135, 140, 255))
rect(img, 4, 12, 11, 13, (130, 135, 140, 255))
rect(img, 7, 3, 8, 4, (120, 190, 240, 255))
linea(img, (6, 0), (9, 0), K)
px(img, 1, 7, (200, 40, 30)); px(img, 14, 7, (40, 160, 60))
guardar(img, TI, "avion_caza")

img = nueva()
rect(img, 4, 5, 11, 14, (40, 42, 40, 255))
rect(img, 5, 6, 10, 8, (80, 200, 100, 255))
for y in [10, 12]:
    for x in [5, 7, 9]:
        px(img, x, y, (150, 150, 150))
linea(img, (10, 4), (12, 0), (60, 60, 60, 255))
px(img, 12, 0, (220, 40, 30))
guardar(img, TI, "radio_bombardeo")

img = nueva()
rect(img, 2, 5, 6, 11, (45, 50, 45, 255)); rect(img, 9, 5, 13, 11, (45, 50, 45, 255))
rect(img, 6, 7, 9, 9, (70, 75, 70, 255))
rect(img, 3, 5, 5, 6, (120, 190, 240, 255)); rect(img, 10, 5, 12, 6, (120, 190, 240, 255))
px(img, 7, 6, (255, 40, 40)); px(img, 8, 6, (255, 40, 40))
guardar(img, TI, "designador_laser")

img = nueva()
rect(img, 3, 4, 11, 13, (235, 190, 30, 255))
borde(img, (150, 115, 10, 255), 3, 4, 11, 13)
elipse(img, [4, 5, 10, 10], (245, 245, 235, 255), K)
linea(img, (7, 8), (9, 6), (200, 30, 30, 255))
rect(img, 12, 6, 13, 12, K)
px(img, 13, 5, K)
guardar(img, TI, "geiger")

img = nueva()
rect(img, 3, 3, 12, 13, (245, 245, 245, 255))
borde(img, (170, 170, 175, 255), 3, 3, 12, 13)
rect(img, 7, 5, 8, 10, (220, 40, 40, 255)); rect(img, 5, 7, 10, 8, (220, 40, 40, 255))
guardar(img, TI, "pastillas_yodo")

img = nueva()
rect(img, 4, 1, 11, 14, (118, 124, 130, 255))
borde(img, (70, 74, 78, 255), 4, 1, 11, 14)
rect(img, 6, 3, 9, 5, (40, 60, 70, 255))
elipse(img, [5, 7, 10, 12], None, (200, 50, 40, 255))
guardar(img, TI, "puerta_bunker")

# camuflaje para armaduras
CAMO = [(88, 98, 58), (60, 70, 38), (110, 95, 60), (40, 45, 30)]


def camuflaje(w, h, semilla, escala=3):
    r = random.Random(semilla)
    img = Image.new("RGBA", (w, h), CAMO[0] + (255,))
    d = ImageDraw.Draw(img)
    for _ in range(w * h // (escala * 6)):
        x, y = r.randrange(w), r.randrange(h)
        s = r.randint(1, escala)
        d.ellipse([x, y, x + s * 2, y + s], fill=r.choice(CAMO[1:]) + (255,))
    return img


def forma_item(nombre, mascara, colorear):
    img = nueva()
    base = colorear()
    for y, fila in enumerate(mascara):
        for x, ch in enumerate(fila):
            if ch == "#":
                img.putpixel((x, y), base.getpixel((x, y)))
            elif ch == "o":
                img.putpixel((x, y), oscuro(base.getpixel((x, y)), 0.55))
            elif ch == "v":
                img.putpixel((x, y), (40, 50, 60, 255))
            elif ch == "f":
                img.putpixel((x, y), (60, 60, 60, 255))
    guardar(img, TI, nombre)


CASCO = ["................", "................", "................", ".....oooooo.....", "....o######o....",
         "...o########o...", "...o########o...", "..o##########o..", "..o##########o..", "..o##o####o##o..",
         "..oo.o....o.oo..", "................", "................", "................", "................", "................"]
PECHO = ["................", "...oo......oo...", "..o##o....o##o..", "..o###oooo###o..", "..o##########o..",
         "..o##########o..", "..o#oo####oo#o..", "..o#oo####oo#o..", "..o##########o..", "..o##########o..",
         "..o#oo####oo#o..", "..o#oo####oo#o..", "..o##########o..", "..oooooooooooo..", "................", "................"]
PIERNAS = ["................", "...oooooooooo...", "...o########o...", "...o########o...", "...o###oo###o...",
           "...o###oo###o...", "...o###oo###o...", "...o###oo###o...", "...o###oo###o...", "...o###oo###o...",
           "...o###oo###o...", "...o###oo###o...", "...o###oo###o...", "...ooooooooooo..", "................", "................"]
BOTAS = ["................", "................", "................", "................", "................",
         "...oooo..oooo...", "...o##o..o##o...", "...o##o..o##o...", "...o##o..o##o...", "...o##o..o##o...",
         "..o###o..o###o..", ".o####o..o####o.", ".oooooo..oooooo.", "................", "................", "................"]
MASCARA = ["................", "................", "....oooooooo....", "...o########o...", "..o##########o..",
           "..o#vvv##vvv#o..", "..o#vvv##vvv#o..", "..o##########o..", "..o####ff####o..", "...o##ffff##o...",
           "....o#ffff#o....", ".....offffo.....", "......oooo......", "................", "................", "................"]

forma_item("casco_militar", CASCO, lambda: camuflaje(16, 16, 31, 2))
forma_item("chaleco_antibalas", PECHO, lambda: camuflaje(16, 16, 32, 2))
forma_item("pantalon_militar", PIERNAS, lambda: camuflaje(16, 16, 33, 2))
forma_item("botas_militares", BOTAS, lambda: Image.new("RGBA", (16, 16), (55, 42, 30, 255)))
NBQ = (232, 192, 32, 255)
forma_item("mascara_nbq", MASCARA, lambda: Image.new("RGBA", (16, 16), (110, 112, 100, 255)))
forma_item("traje_nbq", PECHO, lambda: Image.new("RGBA", (16, 16), NBQ))
forma_item("pantalon_nbq", PIERNAS, lambda: Image.new("RGBA", (16, 16), NBQ))
forma_item("botas_nbq", BOTAS, lambda: Image.new("RGBA", (16, 16), (35, 35, 35, 255)))

# ====================================================================== ARMADURAS (64x32)
ARM = os.path.join(A, "textures", "models", "armor")
camuflaje(64, 32, 41).save(os.path.join(ARM, "militar_layer_1.png"))
camuflaje(64, 32, 42).save(os.path.join(ARM, "militar_layer_2.png"))
img = Image.new("RGBA", (64, 32), NBQ)
d = ImageDraw.Draw(img)
for x in range(0, 64, 8):
    d.line([(x, 0), (x, 31)], fill=(170, 135, 15, 255))
# visor y filtro en la cara del casco (cara delantera de la cabeza: x 8..15, y 8..15)
d.rectangle([8, 8, 15, 15], fill=(110, 112, 100, 255))
d.rectangle([9, 10, 10, 11], fill=(40, 50, 60, 255))
d.rectangle([13, 10, 14, 11], fill=(40, 50, 60, 255))
d.rectangle([11, 13, 12, 15], fill=(50, 50, 50, 255))
img.save(os.path.join(ARM, "nbq_layer_1.png"))
img2 = Image.new("RGBA", (64, 32), NBQ)
d = ImageDraw.Draw(img2)
for y in range(0, 32, 6):
    d.line([(0, y), (63, y)], fill=(170, 135, 15, 255))
img2.save(os.path.join(ARM, "nbq_layer_2.png"))

# ====================================================================== AVIONES (128x64)


def textura_avion(base, nombre, semilla):
    img = Image.new("RGBA", (128, 64), (0, 0, 0, 0))
    ruido(img, base, 8, semilla, (0, 0, 127, 63))
    d = ImageDraw.Draw(img)
    # líneas de paneles en el fuselaje
    for x in range(0, 72, 9):
        d.line([(x, 0), (x, 35)], fill=oscuro(base + (255,), 0.8))
    d.rectangle([28, 28, 35, 35], fill=(60, 62, 66, 255))  # morro
    d.rectangle([74, 0, 101, 11], fill=(140, 200, 235, 255))  # cabina de cristal
    d.rectangle([74, 14, 110, 29], fill=(35, 35, 38, 255))  # hélice
    d.rectangle([110, 0, 120, 5], fill=(200, 40, 30, 255))  # buje
    d.rectangle([90, 20, 98, 25], fill=(25, 25, 25, 255))  # ruedas
    d.rectangle([40, 46, 53, 58], fill=claro(base + (255,), 1.1))
    d.line([(40, 52), (53, 52)], fill=(200, 40, 30, 255), width=2)  # franja de la deriva
    # escarapelas en las alas
    for cx in (12, 76):
        d.ellipse([cx - 3, 37, cx + 3, 43], fill=(240, 240, 240, 255), outline=(30, 30, 30, 255))
    img.save(os.path.join(A, "textures", "entity", nombre + ".png"))


textura_avion((205, 208, 200), "avion_caza", 51)
textura_avion((150, 152, 148), "bombardero", 52)

# ====================================================================== EFECTOS (18x18)
img = nueva(18, 18)
trebol(img, 9, 9, 8, (140, 230, 60), None)
img.save(os.path.join(A, "textures", "mob_effect", "radiacion.png"))
img = nueva(18, 18)
d = ImageDraw.Draw(img)
d.polygon([(9, 1), (16, 4), (15, 11), (9, 17), (3, 11), (2, 4)], fill=(60, 150, 220, 255), outline=(20, 60, 110, 255))
d.line([(9, 4), (9, 13)], fill=(255, 255, 255, 255), width=2)
d.line([(5, 8), (13, 8)], fill=(255, 255, 255, 255), width=2)
img.save(os.path.join(A, "textures", "mob_effect", "proteccion_radiacion.png"))

# ====================================================================== SONIDOS
SR = 44100


def guardar_sonido(nombre, s):
    s = np.clip(s, -1, 1)
    tmp = tempfile.mkdtemp()
    wav = os.path.join(tmp, nombre + ".wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((s * 32000).astype(np.int16).tobytes())
    salida = os.path.join(A, "sounds", nombre + ".ogg")
    if shutil.which("ffmpeg"):
        subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", salida], check=True)
    else:
        print("AVISO: falta ffmpeg, no se generó", salida)
    shutil.rmtree(tmp)


def suavizar(x, n):
    k = np.ones(n) / n
    return np.convolve(x, k, mode="same")


rng = np.random.default_rng(7)
t = np.arange(int(SR * 0.35)) / SR
ruido_b = rng.uniform(-1, 1, t.size)
disparo = (suavizar(ruido_b, 3) * np.exp(-t * 28) + 0.6 * np.sin(2 * np.pi * 90 * t) * np.exp(-t * 20))
disparo[:40] *= np.linspace(0, 1, 40)
guardar_sonido("disparo", disparo * 0.95)

t = np.arange(int(SR * 0.8)) / SR
ruido_b = rng.uniform(-1, 1, t.size)
fuerte = suavizar(ruido_b, 6) * np.exp(-t * 9) * 1.4 + 0.8 * np.sin(2 * np.pi * 55 * t) * np.exp(-t * 7)
guardar_sonido("disparo_fuerte", fuerte * 0.9)

t = np.arange(int(SR * 3.0)) / SR
f = 600 + 300 * np.sin(2 * np.pi * t / 3.0 - np.pi / 2)
fase = 2 * np.pi * np.cumsum(f) / SR
sirena = 0.6 * np.sin(fase) + 0.25 * np.sin(2 * fase)
env = np.minimum(1, np.minimum(t / 0.2, (3.0 - t) / 0.2))
guardar_sonido("sirena", sirena * env * 0.8)

t = np.arange(int(SR * 0.04)) / SR
guardar_sonido("geiger", rng.uniform(-1, 1, t.size) * np.exp(-t * 300))

t = np.arange(int(SR * 0.35)) / SR
alarma = np.sign(np.sin(2 * np.pi * 880 * t)) * 0.4
alarma *= np.minimum(1, np.minimum(t / 0.01, (0.35 - t) / 0.02))
guardar_sonido("alarma", alarma)

print("Texturas y sonidos generados.")

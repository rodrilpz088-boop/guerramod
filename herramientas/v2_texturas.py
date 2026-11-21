#!/usr/bin/env python3
"""Texturas de la versión 2: materiales de muebles, carreteras, señales de España, construcción,
puertas, ventanas, luces, armas y la interfaz de las armas. Todo procedural (pixel art realista).
Ejecutar desde la carpeta del proyecto:  python3 herramientas/v2_texturas.py
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter, ImageFont

MOD = "guerramod"
A = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", MOD)
TB = os.path.join(A, "textures", "block")
TI = os.path.join(A, "textures", "item")
TM = os.path.join(A, "textures", "misc")
for d in (TB, TI, TM):
    os.makedirs(d, exist_ok=True)

FUENTE = "/usr/share/fonts/truetype/dejavu/DejaVuSansCondensed-Bold.ttf"
FUENTE_N = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"


def fuente(tam, condensada=True):
    try:
        return ImageFont.truetype(FUENTE if condensada else FUENTE_N, tam)
    except OSError:
        return ImageFont.load_default()


# ===================================================================== utilidades


def rng(semilla):
    return np.random.default_rng(semilla)


def ruido_suave(w, h, escala, semilla):
    """Ruido de valor suavizado (0..1)."""
    r = rng(semilla)
    gw, gh = max(2, int(w / escala) + 2), max(2, int(h / escala) + 2)
    g = r.random((gh, gw))
    img = Image.fromarray((g * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(img).astype(np.float32) / 255.0


def a_img(arr):
    arr = np.clip(arr, 0, 255).astype(np.uint8)
    if arr.shape[2] == 3:
        alfa = np.full(arr.shape[:2] + (1,), 255, np.uint8)
        arr = np.concatenate([arr, alfa], axis=2)
    return Image.fromarray(arr, "RGBA")


def base(color, w=32, h=32, var=6, semilla=0, escala=4):
    n = ruido_suave(w, h, escala, semilla) - 0.5
    fino = rng(semilla + 1).normal(0, 0.5, (h, w))
    c = np.array(color[:3], np.float32)
    arr = c[None, None, :] + (n * var * 2 + fino * var * 0.6)[:, :, None]
    return a_img(arr)


def guardar(img, nombre, carpeta=TB):
    img.save(os.path.join(carpeta, nombre + ".png"))


def oscurecer(c, f):
    return tuple(int(max(0, min(255, v * f))) for v in c[:3])


def madera(color, w=32, h=32, semilla=0, vertical=False, vetas=7.0):
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    if vertical:
        x, y = y, x
    n = ruido_suave(w, h, 6, semilla)
    n2 = ruido_suave(w, h, 2, semilla + 3)
    v = np.sin((y / h * vetas + n * 2.2) * math.pi * 2) * 0.5 + 0.5
    f = 0.82 + 0.18 * v + (n2 - 0.5) * 0.08
    c = np.array(color, np.float32)
    return a_img(c[None, None, :] * f[:, :, None])


def tela(color, w=32, h=32, semilla=0):
    y, x = np.mgrid[0:h, 0:w]
    trama = ((x + y) % 2) * 0.06 - 0.03 + ((x % 4 == 0) * -0.03)
    n = ruido_suave(w, h, 3, semilla) - 0.5
    f = 1 + trama + n * 0.1
    c = np.array(color, np.float32)
    return a_img(c[None, None, :] * f[:, :, None])


def cepillado(color, w=32, h=32, semilla=0, brillo=True):
    r = rng(semilla)
    filas = r.normal(0, 1, (h, 1)) * 5
    ruido = r.normal(0, 1, (h, w)) * 2.5
    arr = np.array(color, np.float32)[None, None, :] + (filas + ruido)[:, :, None]
    if brillo:
        y = np.arange(h)[:, None]
        arr = arr + (np.exp(-((y - h * 0.3) ** 2) / (h * 2.0)) * 22)[:, :, None]
    return a_img(arr)


def rect(img, x0, y0, x1, y1, c):
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], fill=tuple(c) if len(c) == 4 else tuple(c) + (255,))


def linea(img, a, b, c, w=1):
    ImageDraw.Draw(img).line([a, b], fill=tuple(c) if len(c) == 4 else tuple(c) + (255,), width=w)


def marco(img, c, x0=0, y0=0, x1=None, y1=None, w=1):
    x1 = img.width - 1 if x1 is None else x1
    y1 = img.height - 1 if y1 is None else y1
    ImageDraw.Draw(img).rectangle([x0, y0, x1, y1], outline=tuple(c) + (255,), width=w)


def tirador(img, x, y0, y1, c=(200, 202, 206)):
    rect(img, x, y0, x + 1, y1, c)
    linea(img, (x, y0), (x, y1), (240, 242, 245))
    linea(img, (x + 1, y0), (x + 1, y1), (120, 122, 126))


def sombreado_borde(img, f=0.82):
    """Oscurece ligeramente el borde (da volumen)."""
    arr = np.asarray(img).astype(np.float32)
    h, w = arr.shape[:2]
    m = np.ones((h, w), np.float32)
    m[0, :] *= 1.08
    m[:, 0] *= 1.05
    m[-1, :] *= f
    m[:, -1] *= f
    arr[:, :, :3] *= m[:, :, None]
    return a_img(arr[:, :, :3]) if arr.shape[2] == 3 else Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


# ===================================================================== materiales de muebles

ROBLE = (184, 140, 92)
NOGAL = (98, 64, 40)
guardar(madera(ROBLE, semilla=1), "mat_roble")
guardar(madera(NOGAL, semilla=2), "mat_nogal")
guardar(base((236, 235, 230), var=2, semilla=3), "mat_lacado_blanco")
guardar(base((118, 121, 126), var=3, semilla=4), "mat_lacado_gris")
guardar(base((34, 34, 37), var=3, semilla=5), "mat_negro")
guardar(tela((112, 114, 119), semilla=6), "mat_tela_gris")
guardar(tela((200, 184, 158), semilla=7), "mat_tela_beige")
guardar(tela((52, 76, 112), semilla=8), "mat_tela_azul")
guardar(tela((58, 58, 62), semilla=9), "mat_tela_negra")
guardar(tela((84, 110, 82), semilla=10), "mat_tela_verde")
guardar(base((96, 58, 36), var=6, semilla=11, escala=3), "mat_cuero")
guardar(cepillado((206, 210, 216), semilla=12), "mat_cromo")
guardar(cepillado((174, 178, 182), semilla=13, brillo=False), "mat_inox")
guardar(base((196, 198, 201), var=3, semilla=14), "mat_aluminio")
guardar(base((44, 46, 50), var=4, semilla=15), "mat_metal_negro")
guardar(base((243, 243, 239), var=2, semilla=16), "mat_ceramica")
guardar(base((240, 240, 238), var=2, semilla=17), "mat_plastico_blanco")
guardar(base((36, 36, 38), var=2, semilla=18), "mat_plastico_negro")
guardar(base((190, 191, 187), var=7, semilla=19, escala=2), "mat_hormigon")


def marmol(semilla, color=(236, 234, 230), veta=(150, 150, 156)):
    w = h = 32
    n = ruido_suave(w, h, 8, semilla) + 0.5 * ruido_suave(w, h, 3, semilla + 1)
    y, x = np.mgrid[0:h, 0:w].astype(np.float32)
    v = np.abs(np.sin((x * 0.15 + y * 0.08 + n * 3.0) * math.pi))
    m = np.clip((0.08 - v) / 0.08, 0, 1) ** 1.5
    c = np.array(color, np.float32)[None, None, :] * (1 - m[:, :, None]) + np.array(veta, np.float32)[None, None, :] * m[:, :, None]
    c = c + (rng(semilla).normal(0, 1.5, (h, w)))[:, :, None]
    return a_img(c)


guardar(marmol(20), "mat_marmol")


def granito(color, motas, semilla, w=32, h=32):
    r = rng(semilla)
    arr = np.array(color, np.float32)[None, None, :] + r.normal(0, 6, (h, w))[:, :, None]
    for col, prob in motas:
        m = r.random((h, w)) < prob
        arr[m] = col
    return a_img(arr)


guardar(granito((46, 46, 48), [((90, 90, 92), 0.10), ((20, 20, 22), 0.08)], 21), "mat_granito_negro")

img = Image.new("RGBA", (32, 32), (205, 225, 235, 60))
d = ImageDraw.Draw(img)
for i in range(4, 14):
    d.point((i, 18 - i), fill=(255, 255, 255, 150))
for i in range(18, 24):
    d.point((i, 40 - i), fill=(255, 255, 255, 120))
d.rectangle([0, 0, 31, 31], outline=(170, 190, 200, 140))
guardar(img, "mat_cristal")

y = np.arange(32)[:, None, None].astype(np.float32)
pant = np.full((32, 32, 3), 14, np.float32) + (31 - y) * 0.6
guardar(a_img(pant), "mat_pantalla")

# colchón acolchado, sábanas, edredón
img = base((238, 236, 230), var=2, semilla=22)
for x in range(0, 32, 8):
    for yy in range(0, 32, 8):
        rect(img, x + 3, yy + 3, x + 4, yy + 4, (210, 208, 202))
guardar(img, "mat_colchon")
guardar(tela((244, 243, 239), semilla=23), "mat_sabana_blanca")
guardar(tela((150, 154, 160), semilla=24), "mat_sabana_gris")
guardar(tela((70, 98, 140), semilla=25), "mat_edredon_azul")
img = tela((236, 230, 220), semilla=26)
for yy in range(0, 32, 6):
    linea(img, (0, yy), (31, yy), (214, 206, 194))
guardar(img, "mat_edredon_blanco")

# libros
r = random.Random(27)
img = Image.new("RGBA", (32, 32), (40, 30, 22, 255))
x = 0
COLS_LIBRO = [(140, 30, 30), (30, 60, 120), (40, 100, 60), (200, 170, 90), (90, 40, 90), (220, 220, 210), (30, 30, 30),
              (160, 90, 40)]
while x < 32:
    w = r.randint(2, 4)
    hgt = r.randint(22, 31)
    c = r.choice(COLS_LIBRO)
    rect(img, x, 32 - hgt, x + w - 1, 31, c)
    linea(img, (x, 32 - hgt + 3), (x + w - 1, 32 - hgt + 3), oscurecer(c, 0.6))
    linea(img, (x, 32 - hgt), (x, 31), oscurecer(c, 1.25))
    x += w
guardar(img, "mat_libros")

# planta (hojas recortadas)
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
r = random.Random(28)
d = ImageDraw.Draw(img)
for _ in range(70):
    cx, cy = r.randint(0, 31), r.randint(0, 31)
    g = r.randint(80, 150)
    d.ellipse([cx - 2, cy - 1, cx + 2, cy + 1], fill=(30, g, 40, 255))
guardar(img, "mat_planta")
guardar(base((58, 42, 30), var=8, semilla=29, escala=2), "mat_tierra")
guardar(base((176, 92, 58), var=6, semilla=30), "mat_terracota")

img = base((30, 18, 12), var=10, semilla=31, escala=2)
r = random.Random(32)
for _ in range(60):
    img.putpixel((r.randint(0, 31), r.randint(0, 31)), (255, r.randint(90, 180), 20, 255))
guardar(img, "mat_brasas")
guardar(base((70, 68, 66), var=10, semilla=33, escala=2), "mat_ceniza")

img = Image.new("RGBA", (32, 32), (60, 62, 66, 255))
for xx in range(0, 32, 3):
    linea(img, (xx, 0), (xx, 31), (20, 20, 22))
for yy in range(0, 32, 3):
    linea(img, (0, yy), (31, yy), (20, 20, 22))
guardar(img, "mat_rejilla")

# luz LED (blanca: se tiñe con el color elegido)
img = base((250, 250, 250), var=1, semilla=34)
guardar(img, "led_luz")
img = base((255, 244, 214), var=2, semilla=35)
guardar(img, "mat_luz_calida")
guardar(tela((236, 222, 196), semilla=36), "pantalla_lampara_off")
img = tela((255, 236, 190), semilla=37)
guardar(img, "pantalla_lampara_on")

# colores de plástico y metal pintado
for nombre, c in {"amarillo": (240, 196, 22), "azul": (30, 92, 178), "verde": (34, 122, 64), "gris": (86, 90, 94),
                  "marron": (112, 72, 40), "naranja": (238, 108, 22), "verde_oscuro": (36, 70, 46), "rojo": (196, 30, 30)}.items():
    guardar(base(c, var=4, semilla=hash(nombre) % 1000), "mat_" + nombre)

# cesped artificial
r = rng(38)
arr = np.zeros((32, 32, 3), np.float32) + np.array([70, 160, 50], np.float32)
arr += r.normal(0, 14, (32, 32))[:, :, None] * np.array([0.5, 1.0, 0.4])
m = r.random((32, 32)) < 0.12
arr[m] = [120, 200, 90]
guardar(a_img(arr), "cesped_artificial")
# tela de tumbona a rayas
img = Image.new("RGBA", (32, 32), (240, 240, 236, 255))
for xx in range(0, 32, 8):
    rect(img, xx, 0, xx + 3, 31, (40, 90, 160))
guardar(img, "mat_tumbona")

# ===================================================================== frentes de muebles


def frente_cajones(fondo, n=3, tir=(200, 202, 206)):
    img = fondo.copy()
    h = 32 // n
    for i in range(n):
        y0 = i * h
        marco(img, oscurecer(fondo.getpixel((5, 5)), 0.78), 1, y0 + 1, 30, y0 + h - 1)
        rect(img, 12, y0 + h // 2 - 1, 19, y0 + h // 2, tir)
        linea(img, (12, y0 + h // 2 - 1), (19, y0 + h // 2 - 1), (245, 245, 248))
    return img


guardar(frente_cajones(Image.open(os.path.join(TB, "mat_lacado_blanco.png"))), "frente_cajones_blanco")
guardar(frente_cajones(Image.open(os.path.join(TB, "mat_roble.png"))), "frente_cajones_roble")
guardar(frente_cajones(Image.open(os.path.join(TB, "mat_roble.png")), 4), "frente_comoda")
guardar(frente_cajones(Image.open(os.path.join(TB, "mat_lacado_blanco.png")), 2), "frente_mesita")


def frente_puertas(fondo, tir_y=(10, 20), dos=True):
    img = fondo.copy()
    sombra = oscurecer(fondo.getpixel((5, 5)), 0.78)
    if dos:
        marco(img, sombra, 1, 1, 15, 30)
        marco(img, sombra, 16, 1, 30, 30)
        tirador(img, 13, tir_y[0], tir_y[1])
        tirador(img, 18, tir_y[0], tir_y[1])
    else:
        marco(img, sombra, 1, 1, 30, 30)
        tirador(img, 27, tir_y[0], tir_y[1])
    return img


blanco = Image.open(os.path.join(TB, "mat_lacado_blanco.png"))
guardar(frente_puertas(blanco, (20, 28)), "frente_armario_cocina")
guardar(frente_puertas(blanco, (4, 12)), "frente_mueble_tv")
roble = Image.open(os.path.join(TB, "mat_roble.png"))
guardar(frente_puertas(roble, (2, 31)), "armario_abajo")
img = frente_puertas(roble, (0, 6))
guardar(img, "armario_arriba")

inox = Image.open(os.path.join(TB, "mat_inox.png"))
img = inox.copy()
marco(img, (130, 134, 138), 0, 0, 31, 31)
tirador(img, 27, 0, 18)
guardar(img, "frigo_abajo")
img = inox.copy()
marco(img, (130, 134, 138), 0, 0, 31, 31)
linea(img, (0, 9), (31, 9), (120, 124, 128))
tirador(img, 27, 14, 31)
rect(img, 4, 14, 9, 18, (30, 34, 38))
guardar(img, "frigo_arriba")

img = inox.copy()
rect(img, 3, 9, 28, 28, (22, 22, 24))
rect(img, 5, 11, 26, 26, (40, 34, 30))
linea(img, (4, 6), (27, 6), (220, 222, 226), 2)
for xx in (6, 12, 19, 25):
    ImageDraw.Draw(img).ellipse([xx - 1, 1, xx + 1, 3], fill=(40, 40, 42, 255))
guardar(img, "frente_horno")

img = Image.open(os.path.join(TB, "mat_plastico_blanco.png")).copy()
d = ImageDraw.Draw(img)
d.ellipse([5, 8, 27, 30], fill=(180, 184, 190, 255))
d.ellipse([8, 11, 24, 27], fill=(40, 50, 60, 255))
d.ellipse([10, 13, 18, 20], fill=(90, 110, 130, 255))
rect(img, 2, 1, 29, 5, (220, 220, 216))
d.ellipse([22, 1, 26, 5], fill=(170, 170, 175, 255))
rect(img, 4, 2, 12, 3, (30, 60, 90))
guardar(img, "frente_lavadora")

img = inox.copy()
rect(img, 0, 0, 31, 5, (40, 42, 46))
rect(img, 4, 2, 10, 3, (60, 200, 120))
tirador(img, 3, 8, 9)
linea(img, (3, 8), (28, 8), (230, 232, 236), 2)
guardar(img, "frente_lavavajillas")

img = Image.open(os.path.join(TB, "mat_negro.png")).copy()
rect(img, 2, 4, 22, 27, (16, 16, 18))
rect(img, 4, 6, 20, 25, (34, 30, 28))
rect(img, 24, 4, 29, 27, (60, 62, 66))
rect(img, 25, 6, 28, 9, (30, 200, 120))
guardar(img, "frente_microondas")

img = Image.new("RGBA", (32, 32), (14, 14, 16, 255))
d = ImageDraw.Draw(img)
for (cx, cy, rr) in ((9, 9, 6), (23, 9, 5), (9, 23, 5), (23, 23, 6)):
    d.ellipse([cx - rr, cy - rr, cx + rr, cy + rr], outline=(70, 70, 74, 255))
    d.ellipse([cx - rr + 2, cy - rr + 2, cx + rr - 2, cy + rr - 2], outline=(44, 44, 48, 255))
linea(img, (2, 30), (12, 30), (120, 30, 30))
guardar(img, "vitro_top")

g = Image.open(os.path.join(TB, "mat_granito_negro.png")).copy()
rect(g, 6, 6, 25, 25, (150, 154, 158))
rect(g, 8, 8, 23, 23, (122, 126, 130))
ImageDraw.Draw(g).ellipse([14, 14, 17, 17], fill=(60, 60, 62, 255))
guardar(g, "fregadero_top")

img = Image.open(os.path.join(TB, "mat_ceramica.png")).copy()
d = ImageDraw.Draw(img)
d.ellipse([4, 4, 27, 24], fill=(222, 224, 226, 255))
d.ellipse([7, 6, 24, 21], fill=(206, 210, 214, 255))
d.ellipse([14, 13, 17, 15], fill=(120, 124, 128, 255))
guardar(img, "lavabo_top")
img = Image.open(os.path.join(TB, "mat_ceramica.png")).copy()
rect(img, 3, 3, 28, 28, (226, 228, 230))
for xx in range(6, 27, 3):
    linea(img, (xx, 4), (xx, 27), (214, 216, 218))
ImageDraw.Draw(img).ellipse([14, 14, 17, 17], fill=(150, 152, 156, 255))
guardar(img, "plato_ducha")
img = Image.open(os.path.join(TB, "mat_ceramica.png")).copy()
ImageDraw.Draw(img).rounded_rectangle([3, 3, 28, 28], 7, fill=(120, 190, 225, 255))
ImageDraw.Draw(img).rounded_rectangle([3, 3, 28, 28], 7, outline=(225, 228, 230, 255))
guardar(img, "banera_top")

y = np.arange(32)[:, None, None].astype(np.float32)
x = np.arange(32)[None, :, None].astype(np.float32)
esp = np.array([196, 206, 214], np.float32)[None, None, :] + (x - y) * 0.9
img = a_img(np.broadcast_to(esp, (32, 32, 3)).copy())
linea(img, (6, 25), (16, 15), (240, 245, 250))
linea(img, (10, 27), (20, 17), (232, 238, 244))
guardar(img, "espejo")

img = Image.new("RGBA", (32, 32), (236, 236, 232, 255))
r = random.Random(40)
d = ImageDraw.Draw(img)
for col in [(214, 80, 50), (40, 70, 130), (230, 190, 60), (30, 30, 30), (120, 160, 170)]:
    x0, y0 = r.randint(0, 22), r.randint(0, 22)
    if r.random() < 0.5:
        d.rectangle([x0, y0, x0 + r.randint(5, 12), y0 + r.randint(5, 12)], fill=col + (255,))
    else:
        d.ellipse([x0, y0, x0 + r.randint(6, 12), y0 + r.randint(6, 12)], fill=col + (255,))
guardar(img, "cuadro_abstracto")


def pantalla_tele():
    w = h = 32
    img = Image.new("RGBA", (w, h))
    for yy in range(h):
        t = yy / h
        c = (int(70 + 120 * t), int(140 + 60 * t), int(220 - 40 * t)) if yy < 18 else (60, 130, 70)
        ImageDraw.Draw(img).line([(0, yy), (31, yy)], fill=c + (255,))
    d = ImageDraw.Draw(img)
    d.polygon([(0, 18), (8, 10), (15, 17), (22, 8), (31, 18)], fill=(90, 100, 120, 255))
    d.polygon([(19, 11), (22, 8), (25, 11)], fill=(240, 240, 245, 255))
    d.ellipse([24, 2, 29, 7], fill=(255, 236, 160, 255))
    marco(img, (8, 8, 8), 0, 0, 31, 31)
    return img


guardar(pantalla_tele(), "tele_on")
img = Image.open(os.path.join(TB, "mat_pantalla.png")).copy()
marco(img, (8, 8, 8), 0, 0, 31, 31)
guardar(img, "tele_off")

img = Image.new("RGBA", (32, 32), (30, 90, 160, 255))
rect(img, 0, 27, 31, 31, (210, 210, 214))
for i, xx in enumerate((4, 12, 20)):
    rect(img, xx, 4 + i, xx + 5, 9 + i, (240, 240, 245))
rect(img, 6, 14, 26, 22, (250, 250, 250))
linea(img, (6, 14), (26, 14), (60, 60, 160))
marco(img, (8, 8, 8), 0, 0, 31, 31)
guardar(img, "monitor_on")
img = Image.new("RGBA", (32, 32), (50, 50, 54, 255))
for yy in range(4, 30, 5):
    for xx in range(2, 30, 3):
        rect(img, xx, yy, xx + 1, yy + 2, (200, 200, 204))
guardar(img, "teclado")

img = Image.open(os.path.join(TB, "mat_plastico_blanco.png")).copy()
for yy in (22, 24, 26):
    linea(img, (2, yy), (29, yy), (170, 172, 176))
rect(img, 24, 5, 27, 6, (60, 200, 120))
guardar(img, "aire_frente")
img = Image.new("RGBA", (32, 32), (238, 238, 236, 255))
for xx in range(0, 32, 4):
    rect(img, xx, 0, xx + 2, 31, (250, 250, 250))
    linea(img, (xx + 3, 0), (xx + 3, 31), (196, 196, 198))
guardar(img, "radiador")

# ===================================================================== carreteras (32x32)
ASF = (54, 55, 58)


def ruido_periodico(n, escala, semilla):
    """Ruido suave que se repite sin costuras (para que el asfalto no haga cuadrícula)."""
    r = rng(semilla)
    f = np.fft.fft2(r.normal(0, 1, (n, n)))
    ky, kx = np.meshgrid(np.fft.fftfreq(n), np.fft.fftfreq(n), indexing="ij")
    f *= np.exp(-(kx ** 2 + ky ** 2) * (escala ** 2) * 20)
    a = np.real(np.fft.ifft2(f))
    return (a - a.mean()) / (a.std() + 1e-9)


def asfalto(semilla=100, color=ASF, desgaste=False):
    """Aglomerado asfáltico: betún oscuro con grava de varios tamaños y tonos, manchas suaves y polvo claro."""
    r = rng(semilla)
    arr = np.array(color, np.float32)[None, None, :] + r.normal(0, 4.5, (32, 32))[:, :, None]
    arr += (ruido_periodico(32, 0.9, semilla + 7) * 5.5)[:, :, None]
    m = r.random((32, 32))
    # grava: granos claros (cuarcita), grises y alguno rojizo
    claros = m < 0.075
    arr[claros] = np.array([118, 118, 116], np.float32) + r.normal(0, 14, (claros.sum(), 1))
    grises = (m >= 0.075) & (m < 0.16)
    arr[grises] += 14 + r.normal(0, 5, (grises.sum(), 1))
    rojizos = (m >= 0.16) & (m < 0.175)
    arr[rojizos] = np.array([96, 82, 74], np.float32) + r.normal(0, 6, (rojizos.sum(), 1))
    # huecos del árido (sombras)
    arr[m > 0.955] -= 20
    # piedras de 2×2
    for _ in range(9):
        x0, y0 = r.integers(0, 31, 2)
        t = 92 + r.normal(0, 16)
        arr[y0:y0 + 2, x0:x0 + 2] = np.array([t, t, t - 2], np.float32)
        arr[min(31, y0 + 2), x0:x0 + 2] -= 12
    if desgaste:
        n = ruido_periodico(32, 1.6, semilla + 1)
        arr += (n * 9)[:, :, None]
        for _ in range(4):  # grietas finas
            x, y = r.integers(0, 32, 2)
            for _k in range(r.integers(5, 12)):
                arr[y % 32, x % 32] -= 30
                x += r.integers(-1, 2)
                y += 1
    return a_img(arr)


BL = (232, 232, 226)
AM = (238, 190, 30)


def pintura(img, poligono_o_rect, color=BL, semilla=0):
    """Pinta una marca con aspecto de pintura vial (algo gastada)."""
    capa = Image.new("L", img.size, 0)
    d = ImageDraw.Draw(capa)
    if isinstance(poligono_o_rect[0], (int, float)):
        d.rectangle(poligono_o_rect, fill=255)
    else:
        d.polygon(poligono_o_rect, fill=255)
    m = np.asarray(capa).astype(np.float32) / 255
    r = rng(semilla)
    gasto = r.random(m.shape) < 0.07
    m[gasto] *= 0.55
    arr = np.asarray(img).astype(np.float32)
    col = np.array(color, np.float32) + r.normal(0, 4, m.shape + (1,))[..., 0:1]
    arr[:, :, :3] = arr[:, :, :3] * (1 - m[:, :, None]) + col * m[:, :, None]
    return Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")


def via(nombre, marcas, semilla, color_base=ASF):
    img = asfalto(semilla, color_base)
    for i, (forma, col) in enumerate(marcas):
        img = pintura(img, forma, col, semilla + i)
    guardar(img, nombre)


guardar(asfalto(100), "asfalto")
guardar(asfalto(101, desgaste=True), "asfalto_desgastado")
via("asfalto_linea", [((15, 0, 16, 12), BL)], 102)
via("asfalto_linea_continua", [((15, 0, 16, 31), BL)], 103)
via("asfalto_doble_continua", [((12, 0, 13, 31), BL), ((18, 0, 19, 31), BL)], 104)
via("asfalto_borde", [((1, 0, 3, 31), BL)], 105)
via("asfalto_borde_discontinua", [((1, 0, 3, 15), BL)], 106)
via("asfalto_detencion", [((0, 0, 31, 7), BL)], 107)
via("paso_cebra", [((2, 0, 11, 31), BL), ((20, 0, 29, 31), BL)], 108)
via("asfalto_flecha_recto", [((14, 10, 17, 31), BL), ([(9, 11), (15.5, 1), (22, 11)], BL)], 109)
via("asfalto_flecha_derecha", [((13, 14, 16, 31), BL), ((13, 14, 22, 17), BL), ([(21, 9), (30, 15.5), (21, 22)], BL)], 110)
via("asfalto_flecha_izquierda", [((15, 14, 18, 31), BL), ((9, 14, 18, 17), BL), ([(10, 9), (1, 15.5), (10, 22)], BL)], 111)
via("asfalto_flecha_recto_derecha", [((10, 8, 13, 31), BL), ([(5, 9), (11.5, 0), (18, 9)], BL), ((10, 17, 20, 20), BL),
                                     ([(19, 12), (28, 18.5), (19, 25)], BL)], 112)
via("asfalto_ceda", [([(3, 4), (28, 4), (15.5, 29)], BL)], 113)
via("asfalto_amarilla", [((1, 0, 2, 31), AM)], 114)
via("asfalto_zigzag", [([(2, 0), (5, 0), (12, 16), (5, 31), (2, 31), (9, 16)], AM)], 115)
via("asfalto_aparcamiento", [((0, 0, 31, 1), BL), ((0, 0, 1, 31), BL)], 116)
via("asfalto_zona_azul", [((0, 0, 31, 1), (40, 90, 200)), ((0, 0, 1, 31), (40, 90, 200))], 117)
via("asfalto_cuadricula", [([(0, 0), (3, 0), (31, 28), (31, 31), (28, 31), (0, 3)], AM), ([(31, 0), (31, 3), (3, 31), (0, 31), (0, 28), (28, 0)], AM),
                           ((0, 0, 31, 1), AM), ((0, 30, 31, 31), AM), ((0, 0, 1, 31), AM), ((30, 0, 31, 31), AM)], 118)

img = asfalto(119)
d = ImageDraw.Draw(img)
f = fuente(11)
d.text((16, 16), "STOP", font=f, fill=BL + (255,), anchor="mm")
guardar(img.rotate(0), "asfalto_stop")
img = asfalto(120)
d = ImageDraw.Draw(img)
d.text((16, 16), "BUS", font=fuente(15), fill=BL + (255,), anchor="mm")
guardar(img, "asfalto_bus")

ROJO_BICI = (150, 52, 46)
img = asfalto(121, ROJO_BICI)
guardar(img, "carril_bici")
img = asfalto(122, ROJO_BICI)
d = ImageDraw.Draw(img)
d.ellipse([3, 15, 13, 25], outline=BL + (255,), width=2)
d.ellipse([18, 15, 28, 25], outline=BL + (255,), width=2)
d.line([(8, 20), (13, 12), (21, 12), (23, 20)], fill=BL + (255,), width=2)
d.line([(13, 12), (16, 20), (21, 12)], fill=BL + (255,), width=2)
d.line([(11, 9), (15, 9)], fill=BL + (255,), width=2)
guardar(img, "asfalto_bici")

img = asfalto(123)
rect(img, 2, 6, 29, 25, (40, 40, 42))
for xx in range(4, 29, 3):
    rect(img, xx, 8, xx + 1, 23, (100, 100, 104))
marco(img, (110, 110, 114), 2, 6, 29, 25)
guardar(img, "asfalto_rejilla")
img = asfalto(124)
d = ImageDraw.Draw(img)
d.ellipse([3, 3, 28, 28], fill=(84, 82, 80, 255), outline=(60, 58, 56, 255))
for rr in range(4, 12, 3):
    d.ellipse([16 - rr, 16 - rr, 15 + rr, 15 + rr], outline=(66, 64, 62, 255))
guardar(img, "asfalto_tapa")

# aceras
GR_ACERA = (176, 176, 170)
img = base(GR_ACERA, var=4, semilla=130)
for x0 in (0, 16):
    for y0 in (0, 16):
        marco(img, (140, 140, 136), x0, y0, x0 + 15, y0 + 15)
        for px_ in (3, 9):
            for py_ in (3, 9):
                rect(img, x0 + px_, y0 + py_, x0 + px_ + 3, y0 + py_ + 3, (160, 160, 155))
                linea(img, (x0 + px_, y0 + py_ + 3), (x0 + px_ + 3, y0 + py_ + 3), (136, 136, 132))
guardar(img, "acera")
img = base((208, 156, 60), var=4, semilla=131)
d = ImageDraw.Draw(img)
for yy in range(3, 32, 6):
    for xx in range(3, 32, 6):
        d.ellipse([xx - 2, yy - 2, xx + 2, yy + 2], fill=(226, 176, 74, 255), outline=(170, 124, 44, 255))
marco(img, (160, 116, 40), 0, 0, 31, 31)
guardar(img, "acera_podotactil_botones")
img = base((208, 156, 60), var=4, semilla=132)
for xx in range(2, 32, 6):
    rect(img, xx, 1, xx + 2, 30, (226, 176, 74))
    linea(img, (xx + 3, 1), (xx + 3, 30), (170, 124, 44))
marco(img, (160, 116, 40), 0, 0, 31, 31)
guardar(img, "acera_podotactil_rayas")
GRANITO = (150, 150, 148)
gran = granito(GRANITO, [((100, 100, 104), 0.15), ((210, 210, 206), 0.12), ((40, 40, 44), 0.05)], 133)
guardar(gran, "granito_avila")
img = Image.open(os.path.join(TB, "acera.png")).copy()
gr = granito(GRANITO, [((100, 100, 104), 0.15), ((210, 210, 206), 0.12)], 134)
img.paste(gr.crop((0, 0, 32, 10)), (0, 0))
linea(img, (0, 10), (31, 10), (110, 110, 108))
linea(img, (0, 0), (31, 0), (200, 200, 196))
guardar(img, "bordillo_arriba")
guardar(granito(GRANITO, [((100, 100, 104), 0.15), ((210, 210, 206), 0.12)], 135), "bordillo_lado")
img = Image.new("RGBA", (32, 32))
r = random.Random(136)
for fila in range(4):
    off = 0 if fila % 2 == 0 else 4
    for col in range(-1, 4):
        x0 = col * 8 + off
        c = r.randint(-15, 15)
        rect(img, x0, fila * 8, x0 + 7, fila * 8 + 7, (128 + c, 126 + c, 122 + c))
        marco(img, (80, 78, 74), x0, fila * 8, x0 + 7, fila * 8 + 7)
        img.putpixel((min(31, max(0, x0 + 2)), fila * 8 + 2), (170, 168, 164, 255))
guardar(img, "adoquin_granito")

# mobiliario urbano
guardar(base((160, 166, 170), var=5, semilla=140), "poste_senal")
img = base((176, 182, 186), var=4, semilla=141)
for yy in (5, 13, 21, 29):
    linea(img, (0, yy), (31, yy), (130, 136, 140))
    linea(img, (0, yy - 1), (31, yy - 1), (214, 218, 222))
guardar(img, "bionda")
img = base((236, 112, 24), var=4, semilla=142)
guardar(img, "cono")
img = base((238, 238, 236), var=2, semilla=143)
guardar(img, "reflectante")
img = Image.new("RGBA", (32, 32), (238, 238, 234, 255))
rect(img, 0, 0, 31, 8, (200, 30, 30))
d = ImageDraw.Draw(img)
d.text((16, 15), "N-VI", font=fuente(8), fill=(20, 20, 20, 255), anchor="mm")
d.text((16, 25), "112", font=fuente(10), fill=(20, 20, 20, 255), anchor="mm")
guardar(img, "hito_km")
img = Image.new("RGBA", (32, 32), (230, 230, 226, 255))
rect(img, 4, 8, 27, 23, (240, 170, 30))
rect(img, 8, 11, 23, 20, (255, 210, 90))
guardar(img, "captafaro")


def contenedor(nombre, color, texto, txt_col=(255, 255, 255)):
    img = base(color, var=4, semilla=hash(nombre) % 999)
    for xx in range(3, 32, 7):
        linea(img, (xx, 2), (xx, 29), oscurecer(color, 0.8))
    rect(img, 2, 12, 29, 20, oscurecer(color, 0.75))
    d = ImageDraw.Draw(img)
    d.text((16, 16), texto, font=fuente(6), fill=txt_col + (255,), anchor="mm")
    guardar(img, "contenedor_" + nombre)


contenedor("amarillo", (240, 196, 22), "ENVASES", (40, 40, 40))
contenedor("azul", (30, 92, 178), "PAPEL")
contenedor("verde", (34, 122, 64), "VIDRIO")
contenedor("gris", (78, 84, 80), "RESTO")
contenedor("marron", (112, 72, 40), "ORGÁNICO")

img = base((250, 206, 0), var=4, semilla=150)
rect(img, 6, 6, 25, 9, (30, 30, 30))
d = ImageDraw.Draw(img)
d.ellipse([10, 14, 21, 25], outline=(40, 60, 140, 255), width=2)
guardar(img, "buzon_frente")
img = madera(ROBLE, semilla=151)
for yy in range(0, 32, 8):
    linea(img, (0, yy), (31, yy), oscurecer(ROBLE, 0.55))
guardar(img, "tablillas")
guardar(base((255, 248, 220), var=2, semilla=152), "farol_luz")
img = Image.new("RGBA", (32, 32), (255, 236, 170, 200))
marco(img, (30, 30, 30), 0, 0, 31, 31, 2)
linea(img, (15, 0), (15, 31), (30, 30, 30), 2)
guardar(img, "farol_cristal")

# ===================================================================== semáforos
for luz in range(3):
    img = Image.new("RGBA", (32, 32), (24, 24, 26, 255))
    d = ImageDraw.Draw(img)
    cols = [(240, 30, 20), (255, 170, 0), (40, 230, 90)]
    for i, c in enumerate(cols):
        cy = 5 + i * 11
        encendida = (2 - i) == luz if False else (i == 0 and luz == 0) or (i == 1 and luz == 1) or (i == 2 and luz == 2)
        cc = c if encendida else oscurecer(c, 0.22)
        d.ellipse([10, cy - 4, 21, cy + 6], fill=cc + (255,), outline=(10, 10, 10, 255))
        if encendida:
            d.ellipse([12, cy - 2, 15, cy + 1], fill=tuple(min(255, v + 80) for v in c) + (255,))
    guardar(img, f"semaforo_{luz}")
for luz in (0, 2):
    img = Image.new("RGBA", (32, 32), (24, 24, 26, 255))
    d = ImageDraw.Draw(img)
    rojo = (240, 40, 30) if luz == 0 else (60, 14, 12)
    verde = (40, 230, 110) if luz == 2 else (12, 50, 24)
    rect(img, 4, 1, 27, 15, (10, 10, 10))
    rect(img, 4, 17, 27, 30, (10, 10, 10))
    # peatón parado (arriba) y andando (abajo)
    d.ellipse([14, 2, 17, 5], fill=rojo + (255,))
    d.rectangle([13, 6, 18, 11], fill=rojo + (255,))
    d.rectangle([13, 11, 14, 14], fill=rojo + (255,))
    d.rectangle([17, 11, 18, 14], fill=rojo + (255,))
    d.ellipse([15, 18, 18, 21], fill=verde + (255,))
    d.polygon([(14, 22), (18, 22), (19, 26), (16, 26)], fill=verde + (255,))
    d.line([(15, 26), (12, 30)], fill=verde + (255,), width=2)
    d.line([(17, 26), (20, 30)], fill=verde + (255,), width=2)
    d.line([(15, 23), (12, 26)], fill=verde + (255,), width=1)
    d.line([(18, 23), (21, 25)], fill=verde + (255,), width=1)
    guardar(img, f"semaforo_peatones_{luz}")
guardar(base((28, 28, 30), var=3, semilla=160), "semaforo_carcasa")

# ===================================================================== señales de tráfico de España (64x64)
S = 64
SS = 4
ROJO_S = (200, 16, 28)
AZUL_S = (0, 82, 160)
BLANCO_S = (250, 250, 250)
NEGRO_S = (18, 18, 18)
AMAR_S = (250, 200, 0)


class Lienzo:
    """Dibuja a 4x y reduce (antialias)."""

    def __init__(self):
        self.img = Image.new("RGBA", (S * SS, S * SS), (0, 0, 0, 0))
        self.d = ImageDraw.Draw(self.img)

    def p(self, v):
        return v * SS

    def circulo(self, cx, cy, r, color):
        self.d.ellipse([self.p(cx - r), self.p(cy - r), self.p(cx + r), self.p(cy + r)], fill=color + (255,))

    def poli(self, puntos, color):
        self.d.polygon([(self.p(x), self.p(y)) for x, y in puntos], fill=color + (255,))

    def rect(self, x0, y0, x1, y1, color, radio=0):
        if radio:
            self.d.rounded_rectangle([self.p(x0), self.p(y0), self.p(x1), self.p(y1)], self.p(radio), fill=color + (255,))
        else:
            self.d.rectangle([self.p(x0), self.p(y0), self.p(x1), self.p(y1)], fill=color + (255,))

    def linea(self, pts, color, ancho):
        self.d.line([(self.p(x), self.p(y)) for x, y in pts], fill=color + (255,), width=int(self.p(ancho)), joint="curve")

    def texto(self, x, y, t, tam, color, cond=True):
        self.d.text((self.p(x), self.p(y)), t, font=fuente(int(self.p(tam)), cond), fill=color + (255,), anchor="mm")

    def flecha(self, x0, y0, x1, y1, color, ancho=5, punta=9):
        ang = math.atan2(y1 - y0, x1 - x0)
        bx, by = x1 - math.cos(ang) * punta * 0.8, y1 - math.sin(ang) * punta * 0.8
        self.linea([(x0, y0), (bx, by)], color, ancho)
        px_, py_ = -math.sin(ang), math.cos(ang)
        self.poli([(x1, y1), (bx + px_ * punta * 0.7, by + py_ * punta * 0.7), (bx - px_ * punta * 0.7, by - py_ * punta * 0.7)], color)

    def final(self):
        return self.img.resize((S, S), Image.LANCZOS)


def circular_prohibicion(c):
    c.circulo(32, 32, 30, ROJO_S)
    c.circulo(32, 32, 23, BLANCO_S)


def triangular(c, invertido=False):
    if invertido:
        c.poli([(2, 6), (62, 6), (32, 60)], ROJO_S)
        c.poli([(11, 11), (53, 11), (32, 48)], BLANCO_S)
    else:
        c.poli([(32, 3), (62, 57), (2, 57)], ROJO_S)
        c.poli([(32, 15), (52.5, 51), (11.5, 51)], BLANCO_S)


def coche(c, cx, cy, esc, color):
    c.rect(cx - 7 * esc, cy - 1 * esc, cx + 7 * esc, cy + 4 * esc, color, 1.5 * esc)
    c.poli([(cx - 4.5 * esc, cy - 1 * esc), (cx - 2.5 * esc, cy - 5 * esc), (cx + 2.5 * esc, cy - 5 * esc), (cx + 4.5 * esc, cy - 1 * esc)], color)
    c.circulo(cx - 4 * esc, cy + 4.5 * esc, 1.8 * esc, color)
    c.circulo(cx + 4 * esc, cy + 4.5 * esc, 1.8 * esc, color)


def peaton(c, cx, cy, esc, color, andando=True):
    c.circulo(cx, cy - 9 * esc, 2.3 * esc, color)
    c.linea([(cx, cy - 6 * esc), (cx - 0.5 * esc, cy + 1 * esc)], color, 2.6 * esc)
    c.linea([(cx - 0.5 * esc, cy + 1 * esc), (cx - 4 * esc, cy + 8 * esc)], color, 2.2 * esc)
    c.linea([(cx - 0.5 * esc, cy + 1 * esc), (cx + 3.5 * esc, cy + 8 * esc)], color, 2.2 * esc)
    c.linea([(cx, cy - 5 * esc), (cx - 4 * esc, cy - 0.5 * esc)], color, 1.8 * esc)
    c.linea([(cx, cy - 5 * esc), (cx + 4 * esc, cy - 1 * esc)], color, 1.8 * esc)


SENALES = {}


def senal(nombre):
    def deco(f):
        SENALES[nombre] = f
        return f

    return deco


@senal("stop")
def _(c):
    pts = [(32 + 30 * math.cos(math.radians(22.5 + i * 45)), 32 + 30 * math.sin(math.radians(22.5 + i * 45))) for i in range(8)]
    c.poli(pts, BLANCO_S)
    pts = [(32 + 27.5 * math.cos(math.radians(22.5 + i * 45)), 32 + 27.5 * math.sin(math.radians(22.5 + i * 45))) for i in range(8)]
    c.poli(pts, ROJO_S)
    c.texto(32, 33, "STOP", 17, BLANCO_S)


@senal("ceda")
def _(c):
    triangular(c, invertido=True)


@senal("prohibido_entrar")
def _(c):
    c.circulo(32, 32, 30, ROJO_S)
    c.rect(12, 27, 52, 37, BLANCO_S)


@senal("circulacion_prohibida")
def _(c):
    circular_prohibicion(c)


for v in (20, 30, 40, 50, 60, 80, 90, 100, 120):
    def _vel(c, v=v):
        circular_prohibicion(c)
        c.texto(32, 33, str(v), 20 if v < 100 else 16, NEGRO_S, cond=True)
    SENALES[f"velocidad_{v}"] = _vel


@senal("prohibido_girar_izquierda")
def _(c):
    circular_prohibicion(c)
    c.linea([(36, 48), (36, 30), (22, 30)], NEGRO_S, 5)
    c.poli([(15, 30), (24, 23), (24, 37)], NEGRO_S)
    c.linea([(15, 15), (49, 49)], ROJO_S, 5)


@senal("prohibido_girar_derecha")
def _(c):
    circular_prohibicion(c)
    c.linea([(28, 48), (28, 30), (42, 30)], NEGRO_S, 5)
    c.poli([(49, 30), (40, 23), (40, 37)], NEGRO_S)
    c.linea([(49, 15), (15, 49)], ROJO_S, 5)


@senal("prohibido_adelantar")
def _(c):
    circular_prohibicion(c)
    coche(c, 23, 31, 1.15, ROJO_S)
    coche(c, 41, 31, 1.15, NEGRO_S)


@senal("prohibido_estacionar")
def _(c):
    c.circulo(32, 32, 30, ROJO_S)
    c.circulo(32, 32, 24, AZUL_S)
    c.linea([(15, 15), (49, 49)], ROJO_S, 6)


@senal("prohibido_parar")
def _(c):
    c.circulo(32, 32, 30, ROJO_S)
    c.circulo(32, 32, 24, AZUL_S)
    c.linea([(15, 15), (49, 49)], ROJO_S, 6)
    c.linea([(49, 15), (15, 49)], ROJO_S, 6)


@senal("sentido_obligatorio_derecha")
def _(c):
    c.circulo(32, 32, 30, AZUL_S)
    c.flecha(16, 44, 48, 20, BLANCO_S, 6, 12)


@senal("sentido_obligatorio_recto")
def _(c):
    c.circulo(32, 32, 30, AZUL_S)
    c.flecha(32, 52, 32, 12, BLANCO_S, 6, 12)


@senal("rotonda")
def _(c):
    c.circulo(32, 32, 30, AZUL_S)
    for i in range(3):
        a0 = math.radians(i * 120 - 90)
        a1 = a0 + math.radians(80)
        pts = [(32 + 15 * math.cos(a0 + (a1 - a0) * k / 8), 32 + 15 * math.sin(a0 + (a1 - a0) * k / 8)) for k in range(9)]
        c.linea(pts, BLANCO_S, 5)
        ex, ey = pts[-1]
        tx, ty = -math.sin(a1), math.cos(a1)
        nx, ny = math.cos(a1), math.sin(a1)
        c.poli([(ex + tx * 7, ey + ty * 7), (ex + nx * 6, ey + ny * 6), (ex - nx * 6, ey - ny * 6)], BLANCO_S)


@senal("paso_peatones")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.poli([(32, 9), (56, 53), (8, 53)], BLANCO_S)
    peaton(c, 31, 38, 1.15, NEGRO_S)
    for i in range(4):
        c.rect(14 + i * 9, 49, 19 + i * 9, 52, NEGRO_S)


@senal("aparcamiento")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.texto(32, 33, "P", 46, BLANCO_S, cond=False)


@senal("sentido_unico")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.rect(22, 8, 42, 56, BLANCO_S)
    c.flecha(32, 52, 32, 13, AZUL_S, 6, 12)


@senal("hospital")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.rect(12, 12, 52, 52, BLANCO_S, 2)
    c.texto(32, 33, "H", 34, ROJO_S, cond=False)


@senal("parada_bus")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.rect(12, 14, 52, 46, BLANCO_S, 4)
    c.rect(15, 18, 49, 30, AZUL_S)
    c.circulo(20, 47, 4, BLANCO_S)
    c.circulo(44, 47, 4, BLANCO_S)
    c.circulo(20, 47, 2, AZUL_S)
    c.circulo(44, 47, 2, AZUL_S)


@senal("calle_residencial")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.poli([(10, 30), (24, 18), (38, 30)], BLANCO_S)
    c.rect(14, 30, 34, 44, BLANCO_S)
    peaton(c, 46, 40, 0.9, BLANCO_S)
    c.rect(8, 50, 56, 53, BLANCO_S)


@senal("peligro")
def _(c):
    triangular(c)
    c.rect(29.5, 24, 34.5, 40, NEGRO_S, 1)
    c.circulo(32, 45.5, 2.8, NEGRO_S)


@senal("peligro_rotonda")
def _(c):
    triangular(c)
    for i in range(3):
        a0 = math.radians(i * 120 - 90)
        a1 = a0 + math.radians(75)
        pts = [(32 + 9 * math.cos(a0 + (a1 - a0) * k / 8), 38 + 9 * math.sin(a0 + (a1 - a0) * k / 8)) for k in range(9)]
        c.linea(pts, NEGRO_S, 3.2)
        ex, ey = pts[-1]
        tx, ty = -math.sin(a1), math.cos(a1)
        nx, ny = math.cos(a1), math.sin(a1)
        c.poli([(ex + tx * 4.5, ey + ty * 4.5), (ex + nx * 4, ey + ny * 4), (ex - nx * 4, ey - ny * 4)], NEGRO_S)


@senal("peligro_peatones")
def _(c):
    triangular(c)
    peaton(c, 32, 40, 1.0, NEGRO_S)


@senal("peligro_ninos")
def _(c):
    triangular(c)
    peaton(c, 26, 41, 0.85, NEGRO_S)
    peaton(c, 38, 43, 0.7, NEGRO_S)


@senal("peligro_curva_derecha")
def _(c):
    triangular(c)
    pts = [(28, 48), (28, 40)] + [(28 + 8 * (1 - math.cos(t)), 40 - 8 * math.sin(t)) for t in [k * math.pi / 2 / 8 for k in range(9)]]
    c.linea(pts, NEGRO_S, 4.5)
    c.poli([(42, 32), (36, 27), (36, 37)], NEGRO_S)


@senal("peligro_obras")
def _(c):
    triangular(c)
    c.circulo(28, 25, 2.6, NEGRO_S)
    c.linea([(29, 28), (33, 38)], NEGRO_S, 3.2)
    c.linea([(33, 38), (29, 46)], NEGRO_S, 2.6)
    c.linea([(33, 38), (37, 46)], NEGRO_S, 2.6)
    c.linea([(30, 31), (38, 34)], NEGRO_S, 2.2)
    c.linea([(37, 33), (41, 44)], NEGRO_S, 1.8)
    c.poli([(38, 44), (45, 44), (44, 47), (38, 47)], NEGRO_S)


@senal("peligro_semaforo")
def _(c):
    triangular(c)
    c.rect(27, 21, 37, 49, NEGRO_S, 2)
    c.circulo(32, 26, 3, ROJO_S)
    c.circulo(32, 35, 3, AMAR_S)
    c.circulo(32, 44, 3, (40, 170, 70))


@senal("peligro_cruce")
def _(c):
    triangular(c)
    c.rect(29.5, 20, 34.5, 50, NEGRO_S)
    c.rect(19, 32, 45, 37, NEGRO_S)


@senal("autopista")
def _(c):
    c.rect(3, 3, 61, 61, AZUL_S, 4)
    c.poli([(14, 54), (26, 12), (29, 12), (21, 54)], BLANCO_S)
    c.poli([(50, 54), (38, 12), (35, 12), (43, 54)], BLANCO_S)
    c.rect(30.5, 16, 33.5, 22, BLANCO_S)
    c.rect(30.5, 28, 33.5, 36, BLANCO_S)
    c.rect(30.5, 42, 33.5, 52, BLANCO_S)
    c.rect(8, 6, 56, 9, BLANCO_S)


def dorso(frente):
    alfa = np.asarray(frente)[:, :, 3]
    r = rng(170)
    g = np.full(alfa.shape + (3,), 150, np.float32) + r.normal(0, 4, alfa.shape)[:, :, None]
    arr = np.concatenate([np.clip(g, 0, 255).astype(np.uint8), alfa[:, :, None]], axis=2)
    return Image.fromarray(arr, "RGBA")


for nombre, f in SENALES.items():
    c = Lienzo()
    f(c)
    img = c.final()
    guardar(img, "senal_" + nombre)
    guardar(dorso(img), "senal_" + nombre + "_dorso")
    # icono de inventario (la señal sola)
    img.resize((32, 32), Image.LANCZOS).save(os.path.join(TI, "senal_" + nombre + ".png"))

# ===================================================================== construcción

img = Image.new("RGBA", (32, 32))
r = rng(200)
n = ruido_suave(32, 32, 1.5, 201)
arr = np.array([238, 236, 230], np.float32)[None, None, :] + ((n - 0.5) * 26)[:, :, None] + r.normal(0, 2, (32, 32))[:, :, None]
guardar(a_img(arr), "gotele_blanco")
for nombre, col in {"beige": (226, 210, 184), "gris": (176, 178, 178), "azul": (164, 190, 210), "verde": (170, 186, 160),
                    "terracota": (196, 120, 90)}.items():
    n = ruido_suave(32, 32, 3, hash(nombre) % 777)
    arr = np.array(col, np.float32)[None, None, :] + ((n - 0.5) * 8)[:, :, None]
    guardar(a_img(arr), "pared_" + nombre)

img = Image.new("RGBA", (32, 32), (180, 182, 184, 255))
for fila in range(4):
    off = 0 if fila % 2 == 0 else 8
    for col in range(-1, 3):
        x0 = col * 16 + off
        rect(img, x0 + 1, fila * 8 + 1, x0 + 15, fila * 8 + 7, (246, 246, 244))
        linea(img, (x0 + 2, fila * 8 + 2), (x0 + 13, fila * 8 + 2), (255, 255, 255))
guardar(img, "azulejo_metro")

img = Image.new("RGBA", (32, 32), (232, 226, 210, 255))
d = ImageDraw.Draw(img)
for qx in (0, 16):
    for qy in (0, 16):
        d.rectangle([qx, qy, qx + 15, qy + 15], outline=(160, 150, 130, 255))
        d.polygon([(qx + 8, qy + 1), (qx + 15, qy + 8), (qx + 8, qy + 15), (qx + 1, qy + 8)], fill=(40, 70, 110, 255))
        d.polygon([(qx + 8, qy + 4), (qx + 12, qy + 8), (qx + 8, qy + 12), (qx + 4, qy + 8)], fill=(190, 80, 50, 255))
        d.ellipse([qx + 6, qy + 6, qx + 10, qy + 10], fill=(232, 226, 210, 255))
        for cx, cy in ((qx, qy), (qx + 15, qy), (qx, qy + 15), (qx + 15, qy + 15)):
            d.ellipse([cx - 3, cy - 3, cx + 3, cy + 3], fill=(40, 70, 110, 255))
guardar(img, "azulejo_hidraulico")

img = base((132, 134, 136), var=3, semilla=210)
marco(img, (110, 112, 114), 0, 0, 31, 31)
linea(img, (0, 15), (31, 15), (110, 112, 114))
guardar(img, "gres_gris")
img = marmol(211)
marco(img, (200, 200, 200), 0, 0, 31, 31)
guardar(img, "marmol_blanco")

img = Image.new("RGBA", (32, 32))
r = random.Random(212)
for fila in range(4):
    off = r.randint(0, 24)
    tabla = madera(tuple(v + r.randint(-12, 12) for v in ROBLE), 32, 8, semilla=213 + fila, vetas=2.5).crop((0, 0, 32, 8))
    tabla = Image.fromarray(np.roll(np.asarray(tabla), off, axis=1))
    img.paste(tabla, (0, fila * 8))
    linea(img, (0, fila * 8), (31, fila * 8), oscurecer(ROBLE, 0.6))
    linea(img, (off % 32, fila * 8), (off % 32, fila * 8 + 7), oscurecer(ROBLE, 0.6))
guardar(img, "parquet_roble")
img = Image.new("RGBA", (32, 32), oscurecer(ROBLE, 0.6) + (255,))
r = random.Random(214)
for i in range(-4, 8):
    for j in range(-2, 6):
        c = tuple(v + r.randint(-14, 14) for v in ROBLE)
        x0, y0 = i * 8 + j * 4, j * 8 - i * 4
        ImageDraw.Draw(img).polygon([(x0, y0), (x0 + 8, y0 + 4), (x0 + 8, y0 + 7), (x0, y0 + 3)], fill=c + (255,))
        ImageDraw.Draw(img).polygon([(x0 + 8, y0 + 4), (x0 + 12, y0 - 4), (x0 + 15, y0 - 4), (x0 + 11, y0 + 4)], fill=oscurecer(c, 0.92) + (255,))
guardar(img, "parquet_espiga")
img = Image.new("RGBA", (32, 32))
for fila in range(4):
    tabla = madera((150, 146, 140), 32, 8, semilla=215 + fila, vetas=2).crop((0, 0, 32, 8))
    img.paste(tabla, (0, fila * 8))
    linea(img, (0, fila * 8), (31, fila * 8), (100, 98, 94))
guardar(img, "tarima_gris")

img = Image.new("RGBA", (32, 32), (196, 190, 180, 255))
r = random.Random(216)
for fila in range(8):
    off = 0 if fila % 2 == 0 else 6
    for col in range(-1, 3):
        x0 = col * 12 + off
        c = (170 + r.randint(-18, 18), 82 + r.randint(-12, 12), 52 + r.randint(-10, 10))
        rect(img, x0, fila * 4, x0 + 10, fila * 4 + 2, c)
guardar(img, "ladrillo_caravista")
img = Image.new("RGBA", (32, 32), (120, 60, 40, 255))
r = random.Random(217)
for fila in range(4):
    for col in range(4):
        x0, y0 = col * 8, fila * 8
        c = (186 + r.randint(-20, 20), 96 + r.randint(-15, 15), 60 + r.randint(-10, 10))
        for k in range(8):
            sh = int(30 * math.sin(math.pi * k / 7))
            linea(img, (x0 + k, y0), (x0 + k, y0 + 7), tuple(max(0, min(255, v + sh - 15)) for v in c))
        linea(img, (x0, y0 + 7), (x0 + 7, y0 + 7), (90, 40, 26))
guardar(img, "teja_arabe")
n = ruido_suave(32, 32, 10, 218)
arr = np.array([168, 168, 164], np.float32)[None, None, :] + ((n - 0.5) * 24)[:, :, None]
guardar(a_img(arr), "hormigon_pulido")
img = base((30, 120, 190), var=3, semilla=219)
r = random.Random(220)
for yy in range(0, 32, 4):
    for xx in range(0, 32, 4):
        c = (30 + r.randint(-12, 12), 120 + r.randint(-20, 20), 196 + r.randint(-20, 20))
        rect(img, xx, yy, xx + 2, yy + 2, c)
guardar(img, "gresite")
guardar(base((236, 232, 222), var=4, semilla=221), "borde_piscina")
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
r = random.Random(222)
d = ImageDraw.Draw(img)
for _ in range(220):
    cx, cy = r.randint(0, 31), r.randint(0, 31)
    g = r.randint(60, 110)
    d.ellipse([cx - 1, cy - 1, cx + 1, cy + 1], fill=(26, g, 30, 255))
guardar(img, "seto")

# ===================================================================== puertas, ventanas, vallas


def puerta(nombre, fondo, detalle):
    """Genera la parte de abajo y de arriba de la puerta (32x32) y el icono."""
    arriba = fondo.copy()
    abajo = fondo.copy()
    detalle(arriba, abajo)
    guardar(arriba, nombre + "_arriba")
    guardar(abajo, nombre + "_abajo")
    icono = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    a = arriba.resize((8, 8), Image.LANCZOS)
    b = abajo.resize((8, 8), Image.LANCZOS)
    icono.paste(a, (4, 0))
    icono.paste(b, (4, 8))
    icono.save(os.path.join(TI, nombre + ".png"))


def manilla(img, y):
    rect(img, 26, y - 3, 27, y + 3, (150, 152, 156))
    rect(img, 22, y - 1, 27, y, (220, 222, 226))
    linea(img, (22, y - 1), (27, y - 1), (250, 250, 252))


def det_blanca(arr, ab):
    for img in (arr, ab):
        marco(img, (210, 208, 202), 0, 0, 31, 31)
    for yy in (8, 20):
        linea(arr, (4, yy), (27, yy), (206, 204, 198))
    for yy in (8, 20):
        linea(ab, (4, yy), (27, yy), (206, 204, 198))
    manilla(ab, 1)
    rect(ab, 26, 4, 27, 5, (90, 92, 96))


puerta("puerta_blanca", Image.open(os.path.join(TB, "mat_lacado_blanco.png")), det_blanca)


def det_madera(arr, ab):
    for img in (arr, ab):
        marco(img, (0, 0, 0), 0, 0, 31, 31)
    marco(arr, oscurecer(ROBLE, 0.7), 4, 4, 27, 31)
    marco(ab, oscurecer(ROBLE, 0.7), 4, 0, 27, 27)
    manilla(ab, 2)


puerta("puerta_roble", madera(ROBLE, semilla=230, vertical=True), det_madera)
puerta("puerta_nogal", madera(NOGAL, semilla=231, vertical=True), det_madera)


def det_cristal(arr, ab):
    for img in (arr, ab):
        d = ImageDraw.Draw(img)
        d.rectangle([4, 0, 27, 31], fill=(190, 215, 228, 90))
    rect(arr, 0, 0, 31, 3, (90, 94, 98))
    rect(ab, 0, 26, 31, 31, (90, 94, 98))
    linea(arr, (8, 22), (16, 10), (255, 255, 255, 160))
    rect(ab, 24, 0, 25, 6, (200, 202, 206))


img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
rect(img, 0, 0, 3, 31, (90, 94, 98))
rect(img, 28, 0, 31, 31, (90, 94, 98))
puerta("puerta_cristal", img, det_cristal)


def det_acorazada(arr, ab):
    for img in (arr, ab):
        marco(img, (30, 22, 16), 0, 0, 31, 31)
        for yy in range(0, 32, 8):
            linea(img, (3, yy), (28, yy), (60, 42, 28))
    ImageDraw.Draw(arr).ellipse([14, 12, 17, 15], fill=(200, 202, 206, 255))
    ImageDraw.Draw(ab).ellipse([23, 0, 28, 5], fill=(214, 200, 140, 255))
    rect(ab, 24, 9, 27, 13, (200, 190, 140))


puerta("puerta_acorazada", madera((76, 52, 34), semilla=232, vertical=False, vetas=4), det_acorazada)


def ventana(nombre, marco_col, vidrio=(196, 220, 232, 80)):
    img = Image.new("RGBA", (32, 32), vidrio)
    ImageDraw.Draw(img).rectangle([0, 0, 31, 31], outline=marco_col + (255,), width=2)
    rect(img, 15, 0, 16, 31, marco_col)
    linea(img, (5, 12), (11, 6), (255, 255, 255, 150))
    linea(img, (20, 14), (26, 8), (255, 255, 255, 120))
    rect(img, 12, 14, 13, 18, oscurecer(marco_col, 0.8))
    guardar(img, nombre)
    guardar(Image.new("RGBA", (32, 32), marco_col + (255,)), nombre + "_canto")
    img.resize((16, 16), Image.LANCZOS).save(os.path.join(TI, nombre + ".png"))


ventana("ventana_aluminio", (150, 154, 158))
ventana("ventana_pvc", (240, 240, 238))
ventana("ventana_madera", (120, 80, 48))

for nombre, col in {"gris": (150, 152, 154), "blanca": (232, 232, 228)}.items():
    img = Image.new("RGBA", (32, 32), col + (255,))
    for yy in range(0, 32, 3):
        linea(img, (0, yy), (31, yy), oscurecer(col, 0.75))
        linea(img, (0, yy + 1), (31, yy + 1), oscurecer(col, 1.08))
    guardar(img, "persiana_" + nombre)

img = madera((236, 234, 228), semilla=240, vertical=True, vetas=2)
guardar(img, "valla_blanca")
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for xx in range(3, 32, 6):
    d.line([(xx, 4), (xx, 31)], fill=(26, 26, 28, 255), width=2)
    d.polygon([(xx - 2, 5), (xx + 0.5, 0), (xx + 3, 5)], fill=(26, 26, 28, 255))
d.line([(0, 9), (31, 9)], fill=(26, 26, 28, 255), width=2)
d.line([(0, 27), (31, 27)], fill=(26, 26, 28, 255), width=2)
for xx in range(0, 32, 6):
    d.arc([xx, 10, xx + 6, 16], 0, 180, fill=(26, 26, 28, 255))
guardar(img, "verja_forja")
guardar(Image.new("RGBA", (32, 32), (26, 26, 28, 255)), "verja_forja_canto")
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for k in range(-32, 64, 6):
    d.line([(k, 0), (k + 32, 32)], fill=(46, 110, 60, 255))
    d.line([(k + 32, 0), (k, 32)], fill=(46, 110, 60, 255))
d.line([(0, 1), (31, 1)], fill=(40, 96, 52, 255), width=2)
d.line([(0, 30), (31, 30)], fill=(40, 96, 52, 255), width=2)
guardar(img, "malla_torsion")
guardar(Image.new("RGBA", (32, 32), (40, 96, 52, 255)), "malla_torsion_canto")
guardar(base((26, 26, 28), var=3, semilla=241), "forja")

# alfombras
for nombre, (c1, c2) in {"gris": ((120, 122, 126), (90, 92, 96)), "beige": ((210, 196, 170), (176, 160, 130)),
                         "roja": ((150, 40, 40), (210, 180, 120))}.items():
    img = tela(c1, semilla=hash(nombre) % 500)
    marco(img, c2, 1, 1, 30, 30, 2)
    marco(img, c2, 6, 6, 25, 25, 1)
    ImageDraw.Draw(img).polygon([(16, 9), (23, 16), (16, 23), (9, 16)], outline=c2 + (255,))
    guardar(img, "alfombra_" + nombre)

# panel LED: marco + difusor
img = base((228, 228, 226), var=2, semilla=250)
guardar(img, "led_marco")

# ===================================================================== armas (texturas de material 16x16)


def mat16(nombre, color, var=5, semilla=0, rayas=False):
    img = base(color, 16, 16, var, semilla, escala=2)
    if rayas:
        arr = np.asarray(img).astype(np.float32)
        arr[:, :, :3] += rng(semilla).normal(0, 3, (16, 1))[:, :, None]
        img = Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8), "RGBA")
    guardar(img, nombre, TI)


mat16("arma_pavonado", (46, 48, 54), 4, 300, True)
mat16("arma_polimero", (30, 30, 32), 3, 301)
mat16("arma_madera", (110, 66, 36), 6, 302, True)
mat16("arma_verde", (78, 86, 58), 4, 303)
mat16("arma_arena", (170, 148, 108), 4, 304)
mat16("arma_metal_claro", (120, 124, 130), 4, 305, True)
mat16("arma_laton", (196, 156, 70), 6, 306)
mat16("arma_rojo", (176, 40, 30), 4, 307)
img = Image.new("RGBA", (16, 16), (40, 80, 110, 255))
ImageDraw.Draw(img).ellipse([3, 3, 8, 8], fill=(150, 200, 240, 255))
guardar(img, "arma_lente", TI)
img = Image.new("RGBA", (16, 16), (60, 90, 100, 120))
ImageDraw.Draw(img).ellipse([6, 6, 9, 9], fill=(255, 30, 30, 255))
guardar(img, "arma_punto_rojo", TI)

# casquillos y cajas de munición (iconos 16x16)


def icono(nombre, dibujar):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dibujar(ImageDraw.Draw(img), img)
    img.save(os.path.join(TI, nombre + ".png"))


def _casq(d, img):
    d.rectangle([6, 4, 9, 13], fill=(205, 160, 70, 255))
    d.line([(6, 4), (6, 13)], fill=(240, 200, 110, 255))
    d.rectangle([5, 13, 10, 14], fill=(170, 130, 50, 255))


icono("casquillo", _casq)


def _cart_vacio(d, img):
    d.rectangle([5, 3, 10, 11], fill=(190, 30, 30, 255))
    d.rectangle([5, 11, 10, 14], fill=(205, 160, 70, 255))


icono("cartucho_vacio", _cart_vacio)


def caja_municion(nombre, color, texto, balas):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle([1, 6, 14, 15], fill=color + (255,))
    d.rectangle([1, 6, 14, 7], fill=oscurecer(color, 1.25) + (255,))
    d.rectangle([1, 15, 14, 15], fill=oscurecer(color, 0.6) + (255,))
    for i, x0 in enumerate(range(2, 14, 3)):
        d.rectangle([x0, 1, x0 + 1, 6], fill=(205, 160, 70, 255))
        d.point((x0, 0), fill=balas + (255,))
        d.point((x0 + 1, 0), fill=balas + (255,))
    d.text((8, 11), texto, font=fuente(6), fill=(240, 240, 230, 255), anchor="mm")
    img.save(os.path.join(TI, nombre + ".png"))


caja_municion("municion", (70, 80, 50), "9", (150, 110, 60))
caja_municion("municion_556", (60, 70, 46), "5.56", (190, 120, 60))
caja_municion("municion_762", (80, 64, 40), "7.62", (190, 120, 60))
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for i, x0 in enumerate((2, 6, 10)):
    d.rectangle([x0, 2, x0 + 3, 11], fill=(200, 30, 30, 255))
    d.line([(x0, 2), (x0, 11)], fill=(240, 70, 70, 255))
    d.rectangle([x0, 11, x0 + 3, 14], fill=(205, 160, 70, 255))
img.save(os.path.join(TI, "cartuchos.png"))

# mando LED
img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
d.rounded_rectangle([4, 1, 11, 15], 2, fill=(236, 236, 234, 255), outline=(150, 150, 152, 255))
cols = [(255, 40, 40), (40, 220, 60), (40, 90, 255), (255, 220, 40), (255, 255, 255), (200, 60, 255)]
for i, c in enumerate(cols):
    d.point((6 + (i % 2) * 3, 5 + (i // 2) * 3), fill=c + (255,))
    d.point((7 + (i % 2) * 3, 5 + (i // 2) * 3), fill=c + (255,))
d.rectangle([7, 2, 8, 3], fill=(200, 40, 40, 255))
img.save(os.path.join(TI, "mando_led.png"))

# ===================================================================== interfaz de armas
# fogonazo
N = 64
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
arr = np.zeros((N, N, 4), np.float32)
y, x = np.mgrid[0:N, 0:N].astype(np.float32)
dx, dy = (x - N / 2 + 0.5) / (N / 2), (y - N / 2 + 0.5) / (N / 2)
rr = np.sqrt(dx ** 2 + dy ** 2)
ang = np.arctan2(dy, dx)
estrella = np.clip(1 - rr / (0.35 + 0.65 * np.abs(np.cos(ang * 3)) ** 6), 0, 1)
nucleo = np.clip(1 - rr / 0.35, 0, 1)
intens = np.clip(estrella * 0.9 + nucleo, 0, 1)
arr[:, :, 0] = 255
arr[:, :, 1] = 200 + 55 * nucleo
arr[:, :, 2] = 90 + 160 * nucleo
arr[:, :, 3] = 255 * intens
Image.fromarray(arr.astype(np.uint8), "RGBA").save(os.path.join(TM, "fogonazo.png"))

# mira telescópica (256x256): negro con círculo transparente, retícula con mil-dots
N = 256
SSM = 2
img = Image.new("RGBA", (N * SSM, N * SSM), (0, 0, 0, 255))
d = ImageDraw.Draw(img)
c0 = N * SSM // 2
rad = int(N * SSM * 0.47)
d.ellipse([c0 - rad, c0 - rad, c0 + rad, c0 + rad], fill=(0, 0, 0, 0))
# viñeta suave dentro
arr = np.asarray(img).astype(np.float32)
yy, xx = np.mgrid[0:N * SSM, 0:N * SSM].astype(np.float32)
dist = np.sqrt((xx - c0) ** 2 + (yy - c0) ** 2) / rad
vin = np.clip((dist - 0.82) / 0.18, 0, 1) ** 2
dentro = dist < 1
arr[dentro, 3] = np.maximum(arr[dentro, 3], vin[dentro] * 230)
img = Image.fromarray(arr.astype(np.uint8), "RGBA")
d = ImageDraw.Draw(img)
grueso, fino = 7, 2
d.rectangle([c0 - rad, c0 - grueso // 2, c0 - rad * 0.32, c0 + grueso // 2], fill=(0, 0, 0, 255))
d.rectangle([c0 + rad * 0.32, c0 - grueso // 2, c0 + rad, c0 + grueso // 2], fill=(0, 0, 0, 255))
d.rectangle([c0 - grueso // 2, c0 + rad * 0.32, c0 + grueso // 2, c0 + rad], fill=(0, 0, 0, 255))
d.rectangle([c0 - grueso // 2, c0 - rad, c0 + grueso // 2, c0 - rad * 0.32], fill=(0, 0, 0, 255))
d.line([(c0 - rad * 0.32, c0), (c0 + rad * 0.32, c0)], fill=(0, 0, 0, 255), width=fino)
d.line([(c0, c0 - rad * 0.32), (c0, c0 + rad * 0.32)], fill=(0, 0, 0, 255), width=fino)
for k in range(1, 5):
    off = int(rad * 0.32 * k / 5)
    for sx, sy in ((off, 0), (-off, 0), (0, off), (0, -off)):
        d.ellipse([c0 + sx - 4, c0 + sy - 4, c0 + sx + 4, c0 + sy + 4], fill=(0, 0, 0, 255))
d.ellipse([c0 - 2, c0 - 2, c0 + 2, c0 + 2], fill=(200, 20, 20, 255))
img.resize((N, N), Image.LANCZOS).save(os.path.join(TM, "mira_telescopica.png"))

print("Texturas v2 generadas.")

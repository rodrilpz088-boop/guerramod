#!/usr/bin/env python3
"""Texturas de los objetos nuevos del catálogo de la versión 1.0.0 (vivienda, calle y Navidad).
Se ejecuta ANTES de v2_catalogo.py (el catálogo comprueba que las texturas existen).
python3 herramientas/v7_texturas.py
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFont

MOD = "guerramod"
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
A = os.path.join(RAIZ, "src", "main", "resources", "assets", MOD)
TB = os.path.join(A, "textures", "block")
TI = os.path.join(A, "textures", "item")
R = np.random.default_rng(7007)
FUENTE = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
N = 32


def fuente(t):
    try:
        return ImageFont.truetype(FUENTE, t)
    except OSError:
        return ImageFont.load_default()


def guardar(img, carpeta, nombre):
    os.makedirs(carpeta, exist_ok=True)
    img.save(os.path.join(carpeta, nombre + ".png"))


def ruido(base, var, n=N, sem=None):
    r = np.random.default_rng(sem) if sem is not None else R
    a = np.array(base, float)[None, None, :] + r.normal(0, var, (n, n, 1))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def cepillado(base, var=6, n=N, vertical=False, sem=None):
    """Metal cepillado: vetas finas en una dirección."""
    r = np.random.default_rng(sem) if sem is not None else R
    vetas = r.normal(0, var, (1, n)) if vertical else r.normal(0, var, (n, 1))
    a = np.array(base, float)[None, None, :] + vetas[..., None] + r.normal(0, var * 0.3, (n, n, 1))
    return Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGB").convert("RGBA")


def texto(d, xy, s, tam, color, anchor="mm"):
    d.text(xy, s, font=fuente(tam), fill=color, anchor=anchor)


# =========================================================================== VIVIENDA

# puerta de ascensor: acero cepillado, dos hojas con una junta en el medio
for parte in ("abajo", "arriba"):
    im = cepillado((176, 180, 186), 7, vertical=True, sem=11 if parte == "abajo" else 12)
    d = ImageDraw.Draw(im)
    d.rectangle((0, 0, 31, 31), outline=(110, 114, 120, 255))
    d.line((15, 0, 15, 31), fill=(80, 84, 90, 255))
    d.line((16, 0, 16, 31), fill=(214, 218, 224, 255))
    if parte == "arriba":
        d.rectangle((0, 0, 31, 3), fill=(70, 74, 80, 255))
        d.rectangle((11, 1, 20, 2), fill=(255, 140, 40, 255))  # indicador de planta
    guardar(im, TB, "puerta_ascensor_" + parte)
ic = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
d = ImageDraw.Draw(ic)
d.rectangle((3, 0, 12, 15), fill=(176, 180, 186, 255), outline=(100, 104, 110, 255))
d.line((7, 1, 7, 14), fill=(80, 84, 90, 255))
d.line((8, 1, 8, 14), fill=(220, 224, 228, 255))
d.rectangle((5, 1, 10, 1), fill=(255, 140, 40, 255))
guardar(ic, TI, "puerta_ascensor")

# placa solar: células azul oscuro con rejilla plateada
im = Image.new("RGBA", (N, N), (20, 34, 70, 255))
a = np.array(im).astype(float)
a[..., :3] += R.normal(0, 5, (N, N, 1))
im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
d = ImageDraw.Draw(im)
for i in range(0, N, 8):
    d.line((i, 0, i, N - 1), fill=(170, 180, 196, 255))
    d.line((0, i, N - 1, i), fill=(170, 180, 196, 255))
for i in range(2, N, 4):
    d.line((0, i, N - 1, i), fill=(40, 58, 100, 255))
d.rectangle((0, 0, N - 1, N - 1), outline=(200, 206, 214, 255))
d.line((1, 1, 10, 1), fill=(120, 150, 210, 255))
guardar(im, TB, "placa_solar")

# termo eléctrico: blanco con piloto y termostato
im = ruido((236, 236, 232), 2)
d = ImageDraw.Draw(im)
d.ellipse((10, 18, 21, 29), fill=(210, 210, 206, 255), outline=(150, 150, 150, 255))
d.line((15, 23, 19, 20), fill=(60, 60, 60, 255))
d.rectangle((14, 6, 17, 8), fill=(255, 60, 40, 255))
texto(d, (16, 13), "80L", 7, (90, 90, 90, 255))
guardar(im, TB, "termo_frente")

# caldera de gas: carcasa blanca con pantalla y mandos
im = ruido((240, 240, 238), 2)
d = ImageDraw.Draw(im)
d.rectangle((4, 18, 27, 27), fill=(205, 205, 200, 255))
d.rectangle((7, 20, 18, 24), fill=(30, 60, 90, 255))
texto(d, (12, 22), "55°", 6, (120, 220, 255, 255))
d.ellipse((20, 20, 24, 24), fill=(80, 80, 80, 255))
d.rectangle((6, 4, 25, 6), fill=(190, 190, 186, 255))
guardar(im, TB, "caldera_frente")

# portero automático: placa de aluminio con rejilla de altavoz y botones
im = cepillado((190, 192, 196), 5, sem=21)
d = ImageDraw.Draw(im)
for y in range(4, 11, 2):
    d.line((10, y, 21, y), fill=(60, 62, 66, 255))
d.rectangle((11, 12, 20, 15), fill=(20, 20, 24, 255))
for i in range(4):
    y = 17 + i * 3
    d.rectangle((8, y, 13, y + 1), fill=(240, 240, 240, 255))
    d.rectangle((16, y, 23, y + 1), fill=(150, 150, 150, 255))
d.rectangle((0, 0, N - 1, N - 1), outline=(120, 122, 126, 255))
guardar(im, TB, "portero_frente")

# buzones de la comunidad: 3×3 puertas con ranura y nombre
im = Image.new("RGBA", (N, N), (150, 112, 70, 255))
d = ImageDraw.Draw(im)
for i in range(3):
    for j in range(3):
        x, y = 1 + i * 10, 1 + j * 10
        d.rectangle((x, y, x + 9, y + 9), fill=(186, 148, 100, 255), outline=(110, 80, 50, 255))
        d.rectangle((x + 2, y + 2, x + 7, y + 2), fill=(30, 22, 16, 255))
        d.rectangle((x + 3, y + 5, x + 6, y + 6), fill=(236, 236, 230, 255))
        d.point((x + 7, y + 7), fill=(210, 200, 120, 255))
guardar(im, TB, "buzones_frente")

# toldo a rayas verdes y blancas
im = Image.new("RGBA", (N, N), (240, 240, 232, 255))
d = ImageDraw.Draw(im)
for x in range(0, N, 8):
    d.rectangle((x, 0, x + 3, N - 1), fill=(30, 120, 70, 255))
a = np.array(im).astype(float)
a[..., :3] *= (0.9 + 0.1 * np.linspace(0, 1, N))[:, None, None]
guardar(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), TB, "toldo_rayas")

# unidad exterior del aire acondicionado: rejilla y ventilador
im = ruido((228, 228, 224), 2)
d = ImageDraw.Draw(im)
d.ellipse((3, 5, 22, 24), fill=(60, 62, 64, 255))
for k in range(0, 360, 60):
    t = np.radians(k)
    d.line((12.5, 14.5, 12.5 + 8 * np.cos(t), 14.5 + 8 * np.sin(t)), fill=(150, 150, 150, 255), width=2)
for y in range(5, 25, 2):
    d.line((3, y, 22, y), fill=(200, 200, 196, 120))
d.rectangle((25, 6, 29, 24), fill=(205, 205, 200, 255))
guardar(im, TB, "split_frente")

# contador de la luz: caja gris con ventanilla y rueda
im = ruido((200, 202, 196), 3)
d = ImageDraw.Draw(im)
d.rectangle((6, 6, 25, 20), fill=(24, 26, 28, 255))
d.rectangle((8, 9, 23, 13), fill=(40, 60, 50, 255))
texto(d, (15, 11), "04312", 5, (140, 255, 170, 255))
d.ellipse((13, 22, 18, 27), fill=(60, 60, 60, 255))
guardar(im, TB, "contador_frente")

# cuadro eléctrico: diferenciales
im = ruido((232, 232, 228), 2)
d = ImageDraw.Draw(im)
d.rectangle((3, 8, 28, 22), fill=(245, 245, 245, 255), outline=(150, 150, 150, 255))
for i in range(6):
    x = 5 + i * 4
    d.rectangle((x, 11, x + 2, 19), fill=(250, 250, 250, 255), outline=(170, 170, 170, 255))
    d.rectangle((x, 13, x + 2, 15), fill=(40, 40, 40, 255) if i else (230, 40, 40, 255))
d.rectangle((3, 3, 28, 6), fill=(210, 210, 206, 255))
guardar(im, TB, "cuadro_electrico_frente")

# enchufe
im = Image.new("RGBA", (N, N), (246, 246, 244, 255))
d = ImageDraw.Draw(im)
d.ellipse((7, 7, 24, 24), fill=(232, 232, 230, 255), outline=(200, 200, 198, 255))
d.ellipse((11, 14, 13, 16), fill=(30, 30, 30, 255))
d.ellipse((18, 14, 20, 16), fill=(30, 30, 30, 255))
guardar(im, TB, "enchufe")

# rejilla de extractor
im = Image.new("RGBA", (N, N), (238, 238, 236, 255))
d = ImageDraw.Draw(im)
for y in range(4, 28, 3):
    d.rectangle((4, y, 27, y + 1), fill=(160, 160, 158, 255))
guardar(im, TB, "extractor")

# puerta de garaje seccional
im = cepillado((210, 210, 206), 4, sem=31)
d = ImageDraw.Draw(im)
for y in range(0, N, 8):
    d.line((0, y, N - 1, y), fill=(150, 150, 146, 255))
    d.line((0, y + 1, N - 1, y + 1), fill=(236, 236, 232, 255))
    for x in range(2, N, 6):
        d.line((x, y + 3, x + 3, y + 3), fill=(180, 180, 176, 255))
guardar(im, TB, "puerta_garaje")

# felpudo de coco
im = ruido((150, 104, 56), 14)
d = ImageDraw.Draw(im)
d.rectangle((0, 0, N - 1, N - 1), outline=(60, 40, 22, 255), width=2)
texto(d, (16, 16), "HOLA", 9, (40, 26, 14, 255))
guardar(im, TB, "felpudo")

# cámara de seguridad y alarma
im = ruido((236, 236, 236), 2)
d = ImageDraw.Draw(im)
d.ellipse((9, 9, 22, 22), fill=(20, 20, 24, 255))
d.ellipse((13, 13, 18, 18), fill=(40, 60, 110, 255))
d.point((15, 14), fill=(200, 220, 255, 255))
d.ellipse((24, 6, 26, 8), fill=(255, 40, 40, 255))
guardar(im, TB, "camara_frente")
im = ruido((240, 240, 240), 2)
d = ImageDraw.Draw(im)
d.rectangle((2, 4, 29, 27), fill=(250, 250, 250, 255), outline=(190, 190, 190, 255))
d.rectangle((4, 6, 27, 13), fill=(200, 30, 30, 255))
texto(d, (16, 10), "ALARMA", 6, (255, 255, 255, 255))
texto(d, (16, 21), "24h", 8, (90, 90, 90, 255))
guardar(im, TB, "alarma_frente")

# =========================================================================== CALLE

# marquesina de autobús: cartel de publicidad iluminado
im = Image.new("RGBA", (N, N), (240, 240, 245, 255))
d = ImageDraw.Draw(im)
for y in range(N):
    t = y / (N - 1)
    d.line((0, y, N - 1, y), fill=(int(40 + 180 * t), int(120 + 80 * t), int(220 - 60 * t), 255))
d.ellipse((6, 4, 26, 24), fill=(255, 230, 120, 255))
texto(d, (16, 26), "VERANO", 6, (255, 255, 255, 255))
d.rectangle((0, 0, N - 1, N - 1), outline=(60, 60, 64, 255), width=1)
guardar(im, TB, "marquesina_cartel")

# poste de parada de bus
im = Image.new("RGBA", (64, 64), (230, 30, 40, 255))
d = ImageDraw.Draw(im)
d.rectangle((3, 3, 60, 60), fill=(250, 250, 250, 255))
d.rectangle((6, 6, 57, 28), fill=(230, 30, 40, 255))
texto(d, (32, 17), "BUS", 18, (255, 255, 255, 255))
for i, (n, c) in enumerate((("27", (230, 30, 40)), ("N2", (20, 40, 120)), ("150", (230, 30, 40)))):
    d.rounded_rectangle((5 + i * 19, 36, 21 + i * 19, 52), 3, fill=c + (255,))
    texto(d, (13 + i * 19, 44), n, 9, (255, 255, 255, 255))
guardar(im, TB, "parada_bus_cartel")

# parquímetro
im = ruido((60, 120, 200), 3)
d = ImageDraw.Draw(im)
d.rectangle((6, 4, 25, 12), fill=(20, 30, 30, 255))
texto(d, (15, 8), "0,85€", 5, (120, 255, 160, 255))
d.rectangle((8, 15, 23, 22), fill=(230, 230, 230, 255))
for i in range(3):
    for j in range(2):
        d.rectangle((9 + i * 5, 16 + j * 3, 11 + i * 5, 17 + j * 3), fill=(80, 80, 80, 255))
d.rectangle((11, 25, 20, 26), fill=(10, 10, 10, 255))
texto(d, (16, 29), "SER", 5, (255, 255, 255, 255))
guardar(im, TB, "parquimetro_frente")

# alcorque: rejilla de fundición alrededor del árbol
im = ruido((70, 52, 36), 10)
d = ImageDraw.Draw(im)
d.rectangle((0, 0, N - 1, N - 1), outline=(40, 40, 42, 255), width=3)
for r in range(6, 16, 3):
    d.ellipse((16 - r, 16 - r, 15 + r, 15 + r), outline=(52, 52, 54, 255))
for k in range(0, 360, 30):
    t = np.radians(k)
    d.line((16 + 5 * np.cos(t), 16 + 5 * np.sin(t), 16 + 15 * np.cos(t), 16 + 15 * np.sin(t)), fill=(48, 48, 50, 255))
guardar(im, TB, "alcorque")

# boca de riego (tapa)
im = ruido((96, 96, 96), 6)
d = ImageDraw.Draw(im)
d.ellipse((4, 4, 27, 27), fill=(60, 60, 62, 255), outline=(40, 40, 42, 255))
texto(d, (16, 16), "RIEGO", 5, (150, 150, 150, 255))
guardar(im, TB, "boca_riego")

# valla de obra: franjas rojas y blancas
im = Image.new("RGBA", (N, N), (250, 250, 250, 255))
d = ImageDraw.Draw(im)
for k in range(-N, N * 2, 10):
    d.polygon([(k, 0), (k + 5, 0), (k + 5 - N, N), (k - N, N)], fill=(220, 30, 30, 255))
guardar(im, TB, "valla_obra")

# panel informativo de la ciudad (plano)
im = Image.new("RGBA", (N, N), (236, 232, 220, 255))
d = ImageDraw.Draw(im)
d.rectangle((2, 2, 29, 29), fill=(214, 228, 206, 255))
for i in range(4, 30, 6):
    d.line((i, 2, i + 4, 29), fill=(250, 250, 250, 255), width=2)
    d.line((2, i, 29, i - 3), fill=(250, 250, 250, 255), width=1)
d.ellipse((13, 13, 18, 18), fill=(220, 30, 30, 255))
texto(d, (16, 25), "USTED ESTÁ AQUÍ", 3, (40, 40, 40, 255))
guardar(im, TB, "panel_info")

# radar de tráfico y espejo convexo
im = ruido((90, 92, 96), 3)
d = ImageDraw.Draw(im)
d.rectangle((6, 8, 25, 23), fill=(30, 30, 34, 255))
d.ellipse((10, 11, 21, 21), fill=(60, 70, 90, 255))
d.ellipse((14, 14, 17, 17), fill=(180, 200, 240, 255))
guardar(im, TB, "radar_frente")
im = Image.new("RGBA", (N, N), (230, 120, 20, 255))
d = ImageDraw.Draw(im)
d.ellipse((3, 3, 28, 28), fill=(180, 200, 214, 255))
d.ellipse((7, 6, 16, 12), fill=(236, 244, 250, 255))
guardar(im, TB, "espejo_trafico")

# reductor de velocidad (paso de cebra elevado: amarillo y negro)
im = Image.new("RGBA", (N, N), (30, 30, 30, 255))
d = ImageDraw.Draw(im)
for x in range(0, N, 8):
    d.rectangle((x, 0, x + 3, N - 1), fill=(240, 200, 20, 255))
a = np.array(im).astype(float)
a[..., :3] += R.normal(0, 8, (N, N, 1))
guardar(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), TB, "reductor")

# =========================================================================== NAVIDAD (decoración de casa)

for nom, papel, cinta in (("rojo", (200, 30, 40), (240, 210, 80)), ("azul", (30, 70, 170), (230, 230, 240)),
                          ("dorado", (220, 180, 60), (180, 30, 40))):
    im = Image.new("RGBA", (N, N), papel + (255,))
    a = np.array(im).astype(float)
    a[..., :3] += R.normal(0, 4, (N, N, 1))
    im = Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA")
    d = ImageDraw.Draw(im)
    for k in range(0, N, 8):
        d.point([(k + 2, (k * 3) % N), (k + 5, (k * 5 + 7) % N)], fill=tuple(min(255, c + 40) for c in papel) + (255,))
    d.rectangle((13, 0, 18, N - 1), fill=cinta + (255,))
    d.rectangle((0, 13, N - 1, 18), fill=cinta + (255,))
    guardar(im, TB, "regalo_" + nom)

# muñeco de nieve: nieve con cara (carbón y zanahoria)
nieve = ruido((244, 248, 252), 4)
guardar(nieve, TB, "nieve_munieco")
im = nieve.copy()
d = ImageDraw.Draw(im)
d.rectangle((9, 10, 12, 13), fill=(20, 20, 20, 255))
d.rectangle((19, 10, 22, 13), fill=(20, 20, 20, 255))
d.polygon([(15, 15), (17, 15), (16, 20)], fill=(240, 120, 20, 255))
for i, x in enumerate(range(9, 24, 3)):
    d.rectangle((x, 23 + (1 if i in (0, 4) else 2), x + 1, 24 + (1 if i in (0, 4) else 2)), fill=(20, 20, 20, 255))
guardar(im, TB, "munieco_cara")

# corona de Navidad (puerta)
im = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(im)
for r in range(9, 14):
    d.ellipse((16 - r, 16 - r, 15 + r, 15 + r), outline=(30 + r * 3, 100 + r * 4, 40, 255), width=1)
for k in range(0, 360, 40):
    t = np.radians(k)
    x, y = 16 + 11 * np.cos(t), 16 + 11 * np.sin(t)
    d.ellipse((x - 1.5, y - 1.5, x + 1.5, y + 1.5), fill=(220, 20, 30, 255))
d.polygon([(12, 26), (20, 26), (16, 31)], fill=(200, 20, 30, 255))
d.rectangle((13, 24, 19, 27), fill=(220, 30, 40, 255))
guardar(im, TB, "corona_navidad")

# árbol de Navidad de casa: ramas de abeto con bolas y espumillón
im = ruido((26, 84, 44), 14)
a = np.array(im).astype(float)
for _ in range(14):
    x, y = R.integers(1, N - 2, 2)
    c = [(220, 30, 40), (240, 200, 60), (40, 90, 220), (240, 240, 240)][R.integers(0, 4)]
    a[y:y + 2, x:x + 2, :3] = c
for y in range(4, N, 9):
    for x in range(N):
        yy = int(y + 2 * np.sin(x / 4.0))
        if 0 <= yy < N:
            a[yy, x, :3] = (236, 200, 90)
guardar(Image.fromarray(np.clip(a, 0, 255).astype(np.uint8), "RGBA"), TB, "arbol_navidad_ramas")

print("v7_texturas: texturas del catálogo 1.0.0 listas")

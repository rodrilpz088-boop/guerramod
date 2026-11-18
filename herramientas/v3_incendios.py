#!/usr/bin/env python3
"""Recursos del apartado de incendios: llamas animadas, bloques quemados, material de bomberos, sonidos,
modelos, recetas e idioma.  python3 herramientas/v3_incendios.py  (después de v2_catalogo.py)
"""
import json
import math
import os
import shutil
import subprocess
import tempfile
import wave

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
R = np.random.default_rng(77)


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=1)


def ruido(w, h, escala, semilla):
    r = np.random.default_rng(semilla)
    gw, gh = max(2, int(w / escala) + 2), max(2, int(h / escala) + 2)
    img = Image.fromarray((r.random((gh, gw)) * 255).astype(np.uint8)).resize((w, h), Image.BICUBIC)
    return np.asarray(img).astype(np.float32) / 255


def rgba(arr, alfa=None):
    arr = np.clip(arr, 0, 255).astype(np.uint8)
    if alfa is None:
        alfa = np.full(arr.shape[:2], 255, np.uint8)
    return Image.fromarray(np.dstack([arr, np.clip(alfa, 0, 255).astype(np.uint8)]).copy(), "RGBA").copy()


def animada(nombre, frames, frametime=2, interpolar=False):
    w = frames[0].width
    tira = Image.new("RGBA", (w, w * len(frames)))
    for i, f in enumerate(frames):
        tira.paste(f, (0, i * w))
    tira.save(os.path.join(TB, nombre + ".png"))
    anim = {"frametime": frametime}
    if interpolar:
        anim["interpolate"] = True
    escribir(os.path.join(TB, nombre + ".png.mcmeta"), {"animation": anim})


# ===================================================================== llamas (animadas, 32x32, 24 fotogramas)


def paleta_fuego(t):
    """t 0..1 (0 frío/arriba, 1 caliente/base) -> color."""
    pts = [(0.0, (90, 20, 5)), (0.35, (200, 50, 8)), (0.6, (250, 120, 20)), (0.8, (255, 200, 70)), (1.0, (255, 250, 210))]
    for (a, ca), (b, cb) in zip(pts, pts[1:]):
        if t <= b:
            k = (t - a) / (b - a)
            return tuple(ca[i] + (cb[i] - ca[i]) * k for i in range(3))
    return pts[-1][1]


PAL = np.array([paleta_fuego(i / 255) for i in range(256)], np.float32)


def llamas(semilla, n=24, w=32):
    base = ruido(w, w * 4, 5, semilla)
    fino = ruido(w, w * 4, 2, semilla + 1)
    frames = []
    y, x = np.mgrid[0:w, 0:w].astype(np.float32)
    altura = 1 - y / (w - 1)  # 1 arriba, 0 abajo
    for f in range(n):
        off = int(f * (w * 3) / n)
        nb = np.roll(base, -off * 1, axis=0)[:w]
        nf = np.roll(fino, -off * 2, axis=0)[:w]
        # lenguas de fuego: columnas que suben y se estrechan
        lenguas = 0.55 + 0.45 * np.sin(x / w * math.pi * 3 + f * 0.6 + nb * 4)
        calor = (1 - altura) * 1.05 + nb * 0.6 + nf * 0.25 - 0.3 + lenguas * 0.3 - altura * 0.2
        calor = np.clip(calor, 0, 1)
        idx = (calor * 255).astype(int)
        col = PAL[idx]
        alfa = np.where(calor > 0.28, 255, 0)
        frames.append(rgba(col, alfa))
    return frames


animada("llama_0", llamas(1))
animada("llama_1", llamas(2))


def brasas(semilla, base_col, n=12, w=32, densidad=0.55, grietas=True):
    frames = []
    nb = ruido(w, w, 3, semilla)
    gr = ruido(w, w, 1.5, semilla + 5)
    carb = np.array(base_col, np.float32)[None, None, :] + (ruido(w, w, 2, semilla + 9)[:, :, None] - 0.5) * 30
    for f in range(n):
        pulso = 0.75 + 0.25 * math.sin(f / n * math.pi * 2)
        brillo = np.clip((nb - (1 - densidad)) * 3, 0, 1) * pulso
        if grietas:
            brillo = np.maximum(brillo, np.clip((np.abs(gr - 0.5) < 0.04) * 1.0, 0, 1) * pulso * 0.9)
        idx = (np.clip(0.45 + brillo * 0.55, 0, 1) * 255).astype(int)
        glow = PAL[idx]
        col = carb * (1 - brillo[:, :, None]) + glow * brillo[:, :, None]
        frames.append(rgba(col))
    return frames


animada("tronco_ardiendo", brasas(10, (34, 26, 22), densidad=0.35), 3, True)
animada("tronco_ardiendo_top", brasas(11, (40, 30, 24), densidad=0.5), 3, True)
animada("rescoldo", brasas(12, (40, 38, 36), densidad=0.45, grietas=False), 4, True)

# ===================================================================== restos del incendio (32x32)


def textura(color, var, semilla, escala=3):
    n = ruido(32, 32, escala, semilla)
    f = R.normal(0, 1, (32, 32))
    arr = np.array(color, np.float32)[None, None, :] + ((n - 0.5) * var * 2 + f * var * 0.4)[:, :, None]
    return rgba(arr)


img = textura((34, 30, 28), 10, 20, 2)
px = img.load()
for _ in range(90):
    x, y = R.integers(0, 32, 2)
    g = int(R.integers(80, 130))
    px[int(x), int(y)] = (g, g, g, 255)
img.save(os.path.join(TB, "tierra_quemada_top.png"))
lado = textura((84, 58, 40), 10, 21, 2)
lado.paste(Image.open(os.path.join(TB, "tierra_quemada_top.png")).crop((0, 0, 32, 6)), (0, 0))
lado.save(os.path.join(TB, "tierra_quemada.png"))
img = textura((118, 116, 112), 12, 22, 2)
img.save(os.path.join(TB, "ceniza.png"))

# fases de recuperación: de negro a verde (brotes de hierba que van cubriendo)
negro = np.asarray(Image.open(os.path.join(TB, "tierra_quemada_top.png")).convert("RGB")).astype(np.float32)
tierra = np.asarray(textura((96, 70, 48), 10, 30, 2).convert("RGB")).astype(np.float32)
verde = np.asarray(textura((96, 150, 60), 14, 31, 2).convert("RGB")).astype(np.float32)
manchas = ruido(32, 32, 3, 32)
for fase in range(4):
    if fase == 0:
        top = negro
    else:
        mezcla_tierra = min(1.0, fase * 0.45)
        base_ = negro * (1 - mezcla_tierra) + tierra * mezcla_tierra
        umbral = {1: 0.82, 2: 0.6, 3: 0.32}[fase]
        m = (manchas + R.random((32, 32)) * 0.15 > umbral)[:, :, None]
        top = np.where(m, verde, base_)
    rgba(top).save(os.path.join(TB, f"tierra_quemada_top_{fase}.png"))
    lado_ = textura((84, 58, 40), 10, 21, 2)
    lado_.paste(rgba(top).crop((0, 0, 32, 5)), (0, 0))
    lado_.save(os.path.join(TB, f"tierra_quemada_lado_{fase}.png"))

y, x = np.mgrid[0:32, 0:32].astype(np.float32)
corteza = 26 + 16 * (np.sin(x * 0.9 + ruido(32, 32, 4, 23) * 6) > 0.6) + ruido(32, 32, 2, 24) * 14
grieta = (np.abs(np.sin(y * 0.55 + x * 0.1 + ruido(32, 32, 5, 25) * 3)) < 0.12)
c = np.dstack([corteza, corteza * 0.92, corteza * 0.85])
c[grieta] *= 0.5
rgba(c).save(os.path.join(TB, "tronco_quemado.png"))
rr = np.sqrt((x - 15.5) ** 2 + (y - 15.5) ** 2)
anillos = 30 + 12 * np.sin(rr * 1.4) + ruido(32, 32, 2, 26) * 12
rgba(np.dstack([anillos, anillos * 0.9, anillos * 0.82])).save(os.path.join(TB, "tronco_quemado_top.png"))
img = textura((30, 26, 22), 8, 27, 2)
d = ImageDraw.Draw(img)
for yy in (7, 15, 23, 31):
    d.line([(0, yy), (31, yy)], fill=(12, 10, 9, 255))
for yy in range(2, 32, 5):
    d.line([(int(R.integers(0, 20)), yy), (int(R.integers(20, 32)), yy + 1)], fill=(54, 46, 40, 255))
img.save(os.path.join(TB, "madera_quemada.png"))
img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for _ in range(26):
    x0, y0 = int(R.integers(0, 32)), int(R.integers(0, 32))
    ang = R.random() * math.pi
    l = int(R.integers(3, 9))
    d.line([(x0, y0), (x0 + math.cos(ang) * l, y0 + math.sin(ang) * l)], fill=(28, 24, 22, 255), width=1)
img.save(os.path.join(TB, "ramas_quemadas.png"))

img = textura((132, 98, 66), 9, 28, 2)
d = ImageDraw.Draw(img)
for yy in range(1, 32, 4):
    d.line([(0, yy), (31, yy)], fill=(108, 78, 52, 255))
for _ in range(25):
    x0, y0 = int(R.integers(0, 32)), int(R.integers(0, 32))
    d.point((x0, y0), fill=(160, 150, 140, 255))
img.save(os.path.join(TB, "tierra_rastrillada_top.png"))
lado = textura((122, 88, 60), 9, 29, 2)
lado.save(os.path.join(TB, "tierra_rastrillada.png"))

img = Image.new("RGBA", (32, 32), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
for i in range(28):
    x0 = int(R.integers(1, 31))
    h = int(R.integers(14, 31))
    inc = R.normal(0, 3)
    col = (int(R.integers(180, 225)), int(R.integers(150, 185)), int(R.integers(70, 100)), 255)
    d.line([(x0, 31), (x0 + inc, 31 - h)], fill=col, width=1)
img.save(os.path.join(TB, "pasto_seco.png"))
img.save(os.path.join(TI, "pasto_seco.png"))

# materiales de bomberos


def mat(nombre, color, var=4, sem=0):
    textura(color, var, 40 + sem, 3).save(os.path.join(TB, nombre + ".png"))


mat("rojo_bombero", (196, 26, 22), 4, 1)
mat("amarillo_bombero", (238, 190, 26), 4, 2)
img = Image.open(os.path.join(TB, "rojo_bombero.png")).copy()
d = ImageDraw.Draw(img)
d.rectangle([3, 3, 28, 28], fill=(170, 200, 210, 110), outline=(220, 220, 224, 255))
d.ellipse([7, 7, 24, 24], outline=(150, 18, 16, 255), width=3)
d.ellipse([12, 12, 19, 19], fill=(60, 60, 64, 255))
d.text((16, 29), "BIE", fill=(255, 255, 255, 255), anchor="mb")
img.save(os.path.join(TB, "bie_frente.png"))
img = Image.new("RGBA", (32, 32), (242, 242, 240, 255))
d = ImageDraw.Draw(img)
for r_ in range(4, 15, 3):
    d.ellipse([16 - r_, 16 - r_, 15 + r_, 15 + r_], outline=(214, 214, 212, 255))
for k in range(8):
    a = k * math.pi / 4
    d.ellipse([16 + math.cos(a) * 9 - 1, 16 + math.sin(a) * 9 - 1, 16 + math.cos(a) * 9 + 1, 16 + math.sin(a) * 9 + 1], fill=(170, 170, 170, 255))
img.save(os.path.join(TB, "detector_humo.png"))
img = Image.new("RGBA", (32, 32), (180, 180, 186, 255))
d = ImageDraw.Draw(img)
for k in range(12):
    a = k * math.pi / 6
    d.line([(16, 16), (16 + math.cos(a) * 15, 16 + math.sin(a) * 15)], fill=(120, 120, 128, 255), width=2)
d.ellipse([12, 12, 19, 19], fill=(200, 40, 40, 255))
img.save(os.path.join(TB, "rociador.png"))

# ===================================================================== iconos de objetos (16x16)


def icono(nombre, dib):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    dib(ImageDraw.Draw(img))
    img.save(os.path.join(TI, nombre + ".png"))


def _manguera(d):
    for r_, c in ((6, (170, 30, 26)), (4, (200, 40, 34)), (2, (150, 24, 20))):
        d.ellipse([7 - r_, 8 - r_, 7 + r_, 8 + r_], outline=c + (255,), width=2)
    d.line([(12, 9), (14, 4)], fill=(190, 36, 30, 255), width=2)
    d.rectangle([13, 1, 15, 4], fill=(205, 160, 70, 255))


def _extintor(d):
    d.rounded_rectangle([5, 4, 10, 15], 2, fill=(206, 30, 26, 255))
    d.line([(6, 5), (6, 14)], fill=(240, 90, 80, 255))
    d.rectangle([6, 1, 9, 3], fill=(40, 40, 42, 255))
    d.line([(9, 2), (13, 2), (13, 8)], fill=(30, 30, 30, 255), width=1)
    d.rectangle([6, 8, 9, 11], fill=(240, 240, 240, 255))


def _batefuegos(d):
    d.line([(3, 14), (10, 5)], fill=(140, 96, 52, 255), width=2)
    d.polygon([(9, 1), (15, 1), (15, 7), (11, 7)], fill=(30, 30, 30, 255))


def _rastrillo(d):
    d.line([(3, 14), (11, 5)], fill=(140, 96, 52, 255), width=2)
    d.line([(8, 2), (15, 9)], fill=(150, 150, 156, 255), width=2)
    for k in range(4):
        d.point((9 + k * 2, 3 + k * 2 + 2), fill=(110, 110, 116, 255))


def _antorcha(d):
    d.rounded_rectangle([3, 6, 9, 15], 2, fill=(180, 36, 30, 255))
    d.line([(9, 7), (14, 3)], fill=(150, 150, 156, 255), width=1)
    d.ellipse([12, 0, 15, 3], fill=(255, 150, 30, 255))


def _anemometro(d):
    d.line([(8, 6), (8, 15)], fill=(150, 150, 156, 255), width=1)
    d.line([(2, 5), (14, 5)], fill=(150, 150, 156, 255), width=1)
    for x0 in (1, 12):
        d.ellipse([x0, 3, x0 + 3, 6], fill=(230, 230, 232, 255))
    d.ellipse([6, 0, 9, 3], fill=(230, 230, 232, 255))
    d.rectangle([5, 12, 11, 15], fill=(240, 196, 22, 255))


def _radio(d):
    d.rectangle([4, 5, 11, 15], fill=(240, 196, 22, 255))
    d.rectangle([5, 6, 10, 8], fill=(40, 120, 200, 255))
    d.line([(10, 5), (12, 0)], fill=(60, 60, 60, 255))
    d.point((12, 0), fill=(220, 40, 30, 255))


def _mochila(d):
    d.rounded_rectangle([3, 2, 12, 15], 2, fill=(206, 50, 30, 255))
    d.rectangle([5, 4, 10, 6], fill=(240, 240, 240, 255))
    d.line([(12, 6), (15, 12)], fill=(30, 30, 30, 255))


icono("manguera", _manguera)
icono("extintor", _extintor)
icono("batefuegos", _batefuegos)
icono("rastrillo", _rastrillo)
icono("antorcha_goteo", _antorcha)
icono("anemometro", _anemometro)
icono("radio_hidroavion", _radio)
icono("mochila_extintora", _mochila)

AZUL_B = (30, 40, 60)
REFL = (230, 230, 120)


def _casco(d):
    d.pieslice([2, 3, 14, 15], 180, 360, fill=(240, 196, 22, 255))
    d.rectangle([1, 9, 15, 10], fill=(220, 176, 12, 255))
    d.rectangle([5, 9, 11, 13], fill=(120, 150, 170, 180))


def _chaqueta(d):
    d.polygon([(3, 2), (6, 1), (10, 1), (13, 2), (14, 14), (2, 14)], fill=AZUL_B + (255,))
    d.rectangle([2, 9, 14, 10], fill=REFL + (255,))
    d.rectangle([3, 4, 13, 4], fill=REFL + (255,))
    d.line([(8, 1), (8, 14)], fill=(20, 26, 40, 255))


def _pantalon(d):
    d.polygon([(4, 1), (12, 1), (13, 15), (9, 15), (8, 6), (7, 15), (3, 15)], fill=AZUL_B + (255,))
    d.rectangle([3, 11, 13, 12], fill=REFL + (255,))


def _botas(d):
    d.rectangle([3, 5, 6, 13], fill=(26, 26, 28, 255))
    d.rectangle([9, 5, 12, 13], fill=(26, 26, 28, 255))
    d.rectangle([2, 13, 7, 14], fill=(200, 40, 30, 255))
    d.rectangle([8, 13, 13, 14], fill=(200, 40, 30, 255))


icono("casco_bombero", _casco)
icono("chaqueta_bombero", _chaqueta)
icono("pantalon_bombero", _pantalon)
icono("botas_bombero", _botas)

ARM = os.path.join(A, "textures", "models", "armor")
img = Image.new("RGBA", (64, 32), AZUL_B + (255,))
d = ImageDraw.Draw(img)
d.rectangle([0, 0, 31, 15], fill=(240, 196, 22, 255))  # casco
d.rectangle([8, 12, 15, 15], fill=(120, 150, 170, 255))
for yy in (24, 29):
    d.rectangle([16, yy, 63, yy], fill=REFL + (255,))
img.save(os.path.join(ARM, "bombero_layer_1.png"))
img = Image.new("RGBA", (64, 32), AZUL_B + (255,))
d = ImageDraw.Draw(img)
d.rectangle([0, 26, 63, 27], fill=REFL + (255,))
img.save(os.path.join(ARM, "bombero_layer_2.png"))

# hidroavión (128x64)
img = Image.new("RGBA", (128, 64), (242, 194, 27, 255))
arr = np.asarray(img).astype(np.float32)
arr[:, :, :3] += R.normal(0, 5, (64, 128))[:, :, None]
img = rgba(arr[:, :, :3])
d = ImageDraw.Draw(img)
d.rectangle([0, 16, 71, 22], fill=(200, 30, 26, 255))
d.rectangle([74, 0, 101, 11], fill=(140, 200, 235, 255))
d.rectangle([74, 14, 110, 29], fill=(35, 35, 38, 255))
d.rectangle([40, 46, 53, 58], fill=(200, 30, 26, 255))
img.save(os.path.join(A, "textures", "entity", "hidroavion.png"))

# ===================================================================== modelos y estados


def mb(n, d):
    escribir(os.path.join(A, "models", "block", n + ".json"), d)


def mi(n, d):
    escribir(os.path.join(A, "models", "item", n + ".json"), d)


def bs(n, d):
    escribir(os.path.join(A, "blockstates", n + ".json"), d)


T = lambda n: f"{MOD}:block/{n}"

for i in (0, 1):
    mb(f"llama_suelo{i}", {"parent": "minecraft:block/template_fire_floor", "render_type": "minecraft:cutout", "textures": {"fire": T(f"llama_{i}")}})
    mb(f"llama_lado{i}", {"parent": "minecraft:block/template_fire_side", "render_type": "minecraft:cutout", "textures": {"fire": T(f"llama_{i}")}})


def plano(f, t, cara_a, cara_b):
    return {"from": f, "to": t, "shade": False, "faces": {cara_a: {"uv": [0, 0, 16, 16], "texture": "#fire"},
                                                         cara_b: {"uv": [16, 0, 0, 16], "texture": "#fire"}}}


for i in (0, 1):
    # llamas altas: planos cruzados de casi dos bloques de alto y un anillo exterior
    els = [plano([0, 0, 4], [16, 30, 4], "north", "south"), plano([0, 0, 12], [16, 30, 12], "north", "south"),
           plano([4, 0, 0], [4, 30, 16], "east", "west"), plano([12, 0, 0], [12, 30, 16], "east", "west"),
           plano([-2, 0, 0.2], [18, 22, -0.2 + 0.4], "north", "south")]
    els[4] = plano([0, 0, 0.3], [16, 24, 0.3], "north", "south")
    els.append(plano([0, 0, 15.7], [16, 24, 15.7], "north", "south"))
    els.append(plano([0.3, 0, 0], [0.3, 24, 16], "east", "west"))
    els.append(plano([15.7, 0, 0], [15.7, 24, 16], "east", "west"))
    mb(f"llama_grande{i}", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "ambientocclusion": False,
                           "textures": {"particle": T(f"llama_{i}"), "fire": T(f"llama_{i}")}, "elements": els})
mp = [{"when": {"grande": "false"}, "apply": [{"model": G("block/llama_suelo0")}, {"model": G("block/llama_suelo1")}]}]
for y in (0, 90, 180, 270):
    mp.append({"when": {"grande": "false"}, "apply": [dict({"model": G("block/llama_lado0")}, **({"y": y} if y else {})),
                                                      dict({"model": G("block/llama_lado1")}, **({"y": y} if y else {}))]})
mp.append({"when": {"grande": "true"}, "apply": [{"model": G("block/llama_grande0")}, {"model": G("block/llama_grande1")},
                                                 {"model": G("block/llama_grande0"), "y": 90}, {"model": G("block/llama_grande1"), "y": 90}]})
bs("llama", {"multipart": mp})


def columna(n, lado, arriba):
    mb(n, {"parent": "minecraft:block/cube_column", "textures": {"side": T(lado), "end": T(arriba)}})
    bs(n, {"variants": {"axis=y": {"model": G("block/" + n)}, "axis=z": {"model": G("block/" + n), "x": 90},
                        "axis=x": {"model": G("block/" + n), "x": 90, "y": 90}}})


columna("tronco_ardiendo", "tronco_ardiendo", "tronco_ardiendo_top")
columna("tronco_quemado", "tronco_quemado", "tronco_quemado_top")
mi("tronco_quemado", {"parent": G("block/tronco_quemado")})


def capa(n, tex, alto):
    t = T(tex)
    mb(n, {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": {"particle": t, "t": t},
           "elements": [{"from": [0, 0, 0], "to": [16, alto, 16], "faces": {
               "up": {"texture": "#t"}, "down": {"texture": "#t", "cullface": "down"},
               "north": {"texture": "#t", "uv": [0, 16 - alto, 16, 16]}, "south": {"texture": "#t", "uv": [0, 16 - alto, 16, 16]},
               "east": {"texture": "#t", "uv": [0, 16 - alto, 16, 16]}, "west": {"texture": "#t", "uv": [0, 16 - alto, 16, 16]}}}]})
    bs(n, {"variants": {"": {"model": G("block/" + n)}}})


capa("rescoldo", "rescoldo", 2)
capa("ceniza", "ceniza", 1.5)
mi("ceniza", {"parent": G("block/ceniza")})

v = {}
for fase in range(4):
    nom = "tierra_quemada" if fase == 0 else f"tierra_quemada_{fase}"
    mb(nom, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": T(f"tierra_quemada_top_{fase}"),
                                                                      "side": T(f"tierra_quemada_lado_{fase}"), "bottom": "minecraft:block/dirt"}})
    v[f"fase={fase}"] = [{"model": G("block/" + nom)}, {"model": G("block/" + nom), "y": 90},
                         {"model": G("block/" + nom), "y": 180}, {"model": G("block/" + nom), "y": 270}]
bs("tierra_quemada", {"variants": v})
mi("tierra_quemada", {"parent": G("block/tierra_quemada")})
mb("tierra_rastrillada", {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": T("tierra_rastrillada_top"),
                                                                                     "side": T("tierra_rastrillada"), "bottom": "minecraft:block/dirt"}})
bs("tierra_rastrillada", {"variants": {"": {"model": G("block/tierra_rastrillada")}}})
mi("tierra_rastrillada", {"parent": G("block/tierra_rastrillada")})
mb("madera_quemada", {"parent": "minecraft:block/cube_all", "textures": {"all": T("madera_quemada")}})
bs("madera_quemada", {"variants": {"": {"model": G("block/madera_quemada")}}})
mi("madera_quemada", {"parent": G("block/madera_quemada")})
mb("ramas_quemadas", {"parent": "minecraft:block/leaves", "render_type": "minecraft:cutout_mipped", "textures": {"all": T("ramas_quemadas")}})
bs("ramas_quemadas", {"variants": {"": {"model": G("block/ramas_quemadas")}}})
mi("ramas_quemadas", {"parent": G("block/ramas_quemadas")})
mb("pasto_seco", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": T("pasto_seco")}})
bs("pasto_seco", {"variants": {"": {"model": G("block/pasto_seco")}}})
mi("pasto_seco", {"parent": "minecraft:item/generated", "textures": {"layer0": G("item/pasto_seco")}})

ROT = {"north": 0, "east": 90, "south": 180, "west": 270}
DISPLAY = {"gui": {"rotation": [30, 225, 0], "translation": [0, 0, 0], "scale": [0.625, 0.625, 0.625]},
           "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.25, 0.25, 0.25]},
           "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.5, 0.5, 0.5]},
           "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0], "scale": [0.375, 0.375, 0.375]},
           "firstperson_righthand": {"rotation": [0, 45, 0], "translation": [0, 0, 0], "scale": [0.4, 0.4, 0.4]}}


def caja(f, t, tex, caras=None):
    caras = caras or {}
    return {"from": f, "to": t, "faces": {c: {"texture": "#" + caras.get(c, tex)} for c in ("north", "south", "east", "west", "up", "down")}}


def modelo_3d(n, elementos, texturas, orientado=True, item=True, desp_y=0):
    tx = {k: (T(v) if ":" not in v else v) for k, v in texturas.items()}
    tx["particle"] = next(iter(tx.values()))
    mb(n, {"parent": "minecraft:block/block", "render_type": "minecraft:cutout", "textures": tx, "elements": elementos})
    if orientado:
        bs(n, {"variants": {f"facing={f}": dict({"model": G("block/" + n)}, **({"y": y} if y else {})) for f, y in ROT.items()}})
    if item:
        mi(n, {"parent": G("block/" + n), "display": DISPLAY} if not desp_y else
           {"parent": "minecraft:block/block", "textures": tx, "display": DISPLAY,
            "elements": [dict(e, **{"from": [e["from"][0], e["from"][1] + desp_y, e["from"][2]],
                                    "to": [e["to"][0], e["to"][1] + desp_y, e["to"][2]]}) for e in elementos]})


TEXB = {"r": "rojo_bombero", "c": "mat_cromo", "n": "mat_metal_negro", "a": "amarillo_bombero"}
modelo_3d("boca_incendio", [caja([5, 0, 5], [11, 13, 11], "r"), caja([4.5, 0, 4.5], [11.5, 1.5, 11.5], "r"),
                            caja([6, 13, 6], [10, 14.5, 10], "a"), caja([3.5, 7, 6.5], [5, 9.5, 9.5], "c"), caja([11, 7, 6.5], [12.5, 9.5, 9.5], "c"),
                            caja([6.5, 6.5, 3.5], [9.5, 9.5, 5], "c"), caja([7.3, 14.5, 7.3], [8.7, 15.3, 8.7], "c")], TEXB)
modelo_3d("bie", [caja([1, 1, 11], [15, 15, 16], "r", {"north": "f"})], dict(TEXB, f="bie_frente"))
modelo_3d("extintor_pared", [caja([6, 2, 11], [10, 12, 15], "r"), caja([7, 12, 12], [9, 13.5, 14], "n"),
                             caja([5.5, 13, 11.5], [10.5, 13.5, 14.5], "n"), caja([9.5, 5, 11.5], [10.5, 12.5, 12.5], "n"),
                             caja([7, 9, 15], [9, 13, 16], "n")], TEXB)
modelo_3d("detector_humo", [caja([4.5, 14.5, 4.5], [11.5, 16, 11.5], "d", {"down": "d"})], {"d": "detector_humo"}, orientado=False, desp_y=-12)
bs("detector_humo", {"variants": {"": {"model": G("block/detector_humo")}}})
modelo_3d("rociador", [caja([7, 14, 7], [9, 16, 9], "c"), caja([7.5, 13, 7.5], [8.5, 14, 8.5], "c"), caja([5.5, 12.6, 5.5], [10.5, 13, 10.5], "x")],
          {"c": "mat_cromo", "x": "rociador"}, orientado=False, desp_y=-10)
bs("rociador", {"variants": {"": {"model": G("block/rociador")}}})

for n in ("manguera", "extintor", "batefuegos", "rastrillo", "antorcha_goteo", "anemometro", "radio_hidroavion", "mochila_extintora",
          "casco_bombero", "chaqueta_bombero", "pantalon_bombero", "botas_bombero"):
    padre = "minecraft:item/handheld" if n in ("batefuegos", "rastrillo") else "minecraft:item/generated"
    mi(n, {"parent": padre, "textures": {"layer0": G("item/" + n)}})

# ===================================================================== botín


def botin(n, entradas):
    escribir(os.path.join(D, "loot_tables", "blocks", n + ".json"), {"type": "minecraft:block", "pools": [
        {"rolls": 1, "entries": entradas, "conditions": [{"condition": "minecraft:survives_explosion"}]}]})


propio = lambda n: [{"type": "minecraft:item", "name": G(n)}]
carbon = lambda a, b: [{"type": "minecraft:item", "name": mc("charcoal"), "functions": [
    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": a, "max": b}}]}]
for n in ("tierra_quemada", "tierra_rastrillada", "boca_incendio", "bie", "extintor_pared", "detector_humo", "rociador"):
    botin(n, propio(n))
botin("tronco_quemado", carbon(1, 3))
botin("madera_quemada", carbon(0, 1))
botin("tronco_ardiendo", carbon(1, 2))
botin("ramas_quemadas", [{"type": "minecraft:item", "name": mc("stick"), "functions": [
    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": 0, "max": 1}}]}])
escribir(os.path.join(D, "loot_tables", "blocks", "pasto_seco.json"), {"type": "minecraft:block", "pools": [{"rolls": 1, "entries": [
    {"type": "minecraft:alternatives", "children": [
        {"type": "minecraft:item", "name": G("pasto_seco"), "conditions": [{"condition": "minecraft:match_tool", "predicate": {"items": [mc("shears")]}}]},
        {"type": "minecraft:item", "name": mc("wheat_seeds"), "conditions": [{"condition": "minecraft:random_chance", "chance": 0.125}]}]}]}]})
botin("ceniza", [])
escribir(os.path.join(D, "loot_tables", "blocks", "ceniza.json"), {"type": "minecraft:block", "pools": []})

# ===================================================================== recetas


def sin_forma(n, ings, cantidad=1):
    escribir(os.path.join(D, "recipes", n + ".json"), {"type": "minecraft:crafting_shapeless", "category": "equipment",
                                                      "ingredients": [{"item": i} for i in ings], "result": {"item": G(n), "count": cantidad}})


sin_forma("boca_incendio", [mc("iron_block"), mc("red_dye"), mc("water_bucket")])
sin_forma("bie", [mc("iron_ingot"), mc("iron_ingot"), mc("red_dye"), mc("glass_pane"), mc("string"), mc("water_bucket")])
sin_forma("extintor", [mc("iron_ingot"), mc("iron_ingot"), mc("red_dye"), mc("snowball")])
sin_forma("extintor_pared", [G("extintor"), mc("iron_nugget")])
sin_forma("detector_humo", [mc("white_concrete"), mc("redstone"), mc("note_block")])
sin_forma("rociador", [mc("iron_ingot"), mc("water_bucket"), mc("redstone"), mc("iron_nugget")])
sin_forma("manguera", [mc("string"), mc("string"), mc("string"), mc("red_dye"), mc("leather"), mc("copper_ingot")])
sin_forma("batefuegos", [mc("stick"), mc("stick"), mc("leather"), mc("iron_nugget")])
sin_forma("rastrillo", [mc("stick"), mc("stick"), mc("iron_ingot"), mc("iron_nugget"), mc("flint")])
sin_forma("antorcha_goteo", [mc("iron_ingot"), mc("torch"), mc("coal"), mc("bucket")])
sin_forma("anemometro", [mc("compass"), mc("iron_ingot"), mc("feather")])
sin_forma("radio_hidroavion", [G("circuito"), mc("water_bucket"), mc("lightning_rod")])
sin_forma("mochila_extintora", [mc("leather"), mc("leather"), mc("bucket"), mc("string"), mc("red_dye")])
sin_forma("casco_bombero", [mc("iron_helmet"), mc("yellow_dye")])
sin_forma("chaqueta_bombero", [mc("leather_chestplate"), mc("yellow_dye"), mc("blue_dye"), mc("magma_cream")])
sin_forma("pantalon_bombero", [mc("leather_leggings"), mc("yellow_dye"), mc("blue_dye"), mc("magma_cream")])
sin_forma("botas_bombero", [mc("leather_boots"), mc("black_dye"), mc("iron_ingot")])
sin_forma("pasto_seco", [mc("grass"), mc("wheat")], 2)
sin_forma("tierra_rastrillada", [mc("dirt"), mc("flint")], 2)

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

# ===================================================================== etiquetas


def tag(carpeta, nombre, valores, tipo="blocks"):
    ruta = os.path.join(carpeta, "tags", tipo, nombre + ".json")
    previo = json.load(open(ruta, encoding="utf-8"))["values"] if os.path.exists(ruta) else []
    escribir(ruta, {"replace": False, "values": previo + [v for v in valores if v not in previo]})


tag(DMC, "mineable/shovel", [G("tierra_quemada"), G("ceniza"), G("tierra_rastrillada"), G("rescoldo")])
tag(DMC, "mineable/axe", [G("tronco_quemado"), G("madera_quemada"), G("tronco_ardiendo")])
tag(DMC, "mineable/hoe", [G("ramas_quemadas")])
tag(DMC, "mineable/pickaxe", [G("boca_incendio"), G("bie"), G("extintor_pared"), G("detector_humo"), G("rociador")])
tag(DMC, "dirt", [G("tierra_quemada"), G("tierra_rastrillada")])
tag(DMC, "replaceable_by_trees", [G("pasto_seco")])
tag(DMC, "enderman_holdable", [G("pasto_seco")])

# ===================================================================== idioma
NOMBRES = {
    "block.llama": "Llamas", "block.tronco_ardiendo": "Tronco ardiendo", "block.rescoldo": "Rescoldo",
    "block.tierra_quemada": "Tierra quemada", "block.ceniza": "Ceniza", "block.tronco_quemado": "Tronco carbonizado",
    "block.madera_quemada": "Madera carbonizada", "block.ramas_quemadas": "Ramas quemadas", "block.tierra_rastrillada": "Tierra rastrillada (cortafuegos)",
    "block.pasto_seco": "Pasto seco", "block.boca_incendio": "Boca de incendio (hidrante)", "block.bie": "BIE (boca de incendio equipada)",
    "block.extintor_pared": "Extintor de pared", "block.detector_humo": "Detector de humo", "block.rociador": "Rociador automático",
    "item.manguera": "Manguera de bomberos", "item.extintor": "Extintor de polvo", "item.batefuegos": "Batefuegos",
    "item.rastrillo": "Rastrillo forestal (McLeod)", "item.antorcha_goteo": "Antorcha de goteo", "item.anemometro": "Estación meteorológica portátil",
    "item.radio_hidroavion": "Radio: pedir hidroavión", "item.mochila_extintora": "Mochila extintora forestal",
    "item.casco_bombero": "Casco de bombero", "item.chaqueta_bombero": "Chaqueta ignífuga", "item.pantalon_bombero": "Pantalón ignífugo",
    "item.botas_bombero": "Botas de bombero", "entity.hidroavion": "Hidroavión de extinción", "itemGroup.incendios": "Incendios y bomberos",
    "subtitles.manguera": "Chorro de manguera", "subtitles.extintor": "Descarga de extintor", "subtitles.alarma_incendio": "Alarma de incendio",
}
for archivo in ("es_es.json", "en_us.json"):
    ruta = os.path.join(A, "lang", archivo)
    actual = json.load(open(ruta, encoding="utf-8"))
    for k, v in NOMBRES.items():
        tipo, n = k.split(".", 1)
        actual[f"{tipo}.{MOD}.{n}"] = v
    escribir(ruta, actual)

# ===================================================================== sonidos
SR = 44100
SAL = os.path.join(A, "sounds")


def guardar(nombre, s):
    s = s / max(1e-6, np.max(np.abs(s))) * 0.9
    tmp = tempfile.mkdtemp()
    wav = os.path.join(tmp, "a.wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((s * 32000).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5", os.path.join(SAL, nombre + ".ogg")], check=True)
    shutil.rmtree(tmp)


def filtrar(x, bajo, alto):
    X = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / SR)
    X *= ((f > bajo) & (f < alto)).astype(float) * 0.8 + 0.2 * np.exp(-((f - (bajo + alto) / 2) / (alto - bajo + 1)) ** 2)
    return np.fft.irfft(X, len(x))


t = np.arange(int(SR * 0.7)) / SR
env = np.minimum(1, np.minimum(t / 0.03, (0.7 - t) / 0.08))
guardar("manguera", filtrar(R.normal(0, 1, t.size), 900, 7000) * env * (1 + 0.15 * np.sin(2 * np.pi * 9 * t)))
t = np.arange(int(SR * 0.55)) / SR
env = np.minimum(1, np.minimum(t / 0.01, (0.55 - t) / 0.1))
guardar("extintor", filtrar(R.normal(0, 1, t.size), 2500, 12000) * env)
pit = np.arange(int(SR * 0.16)) / SR
bip = np.sign(np.sin(2 * np.pi * 3150 * pit)) * 0.5 * np.minimum(1, np.minimum(pit / 0.005, (0.16 - pit) / 0.005))
sil = np.zeros(int(SR * 0.08))
guardar("alarma_incendio", np.concatenate([bip, sil, bip, sil, bip, np.zeros(int(SR * 0.2))]))

ruta = os.path.join(A, "sounds.json")
s = json.load(open(ruta, encoding="utf-8"))
for n in ("manguera", "extintor", "alarma_incendio"):
    s[n] = {"subtitle": f"subtitles.{MOD}.{n}", "sounds": [{"name": f"{MOD}:{n}", "attenuation_distance": 24}]}
escribir(ruta, s)
print("Incendios: recursos generados.")

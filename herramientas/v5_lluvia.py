#!/usr/bin/env python3
"""Recursos de la versión 5 (lluvia y agua): texturas del agua realista (sustituyen a las de Minecraft) y charcos.
python3 herramientas/v5_lluvia.py  (después de v4_clima.py)
"""
import json
import math
import os

import numpy as np
from PIL import Image

MOD = "guerramod"
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
RES = os.path.join(RAIZ, "src", "main", "resources")
A = os.path.join(RES, "assets", MOD)
AMC = os.path.join(RES, "assets", "minecraft")
TB = os.path.join(A, "textures", "block")
TMC = os.path.join(AMC, "textures", "block")
G = lambda n: f"{MOD}:{n}"
R = np.random.default_rng(2026)


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=1)


def imagen(rgb, a):
    return Image.fromarray(np.dstack([np.clip(rgb, 0, 255), np.clip(a, 0, 255)]).astype(np.uint8), "RGBA")


def tira(frames, ruta, frametime, interpolar=True):
    w, h = frames[0].size
    t = Image.new("RGBA", (w, h * len(frames)))
    for i, f in enumerate(frames):
        t.paste(f, (0, i * h))
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    t.save(ruta)
    anim = {"frametime": frametime}
    if interpolar:
        anim["interpolate"] = True
    escribir(ruta + ".mcmeta", {"animation": anim})


# ===================================================================== agua (sustituye a la de Minecraft)
# Ondas que se repiten sin costuras en el bloque y en el tiempo. La textura es gris: el color lo pone cada zona.
N = 32
F = 48
yy, xx = np.mgrid[0:N, 0:N].astype(np.float32)
ONDAS = [(1, 2, 1, 0.55), (-2, 1, 2, 0.5), (3, -1, 2, 0.42), (2, 3, 3, 0.35), (-3, -4, 3, 0.3), (5, 1, 4, 0.22), (1, -6, 5, 0.2),
         (-4, 3, 4, 0.18), (6, -5, 6, 0.1), (-7, -2, 6, 0.09)]
fases = R.random(len(ONDAS)) * math.tau
cuadros = []
L = np.array([-0.45, -0.6, 0.66])
L /= np.linalg.norm(L)
H = L + np.array([0, 0, 1.0])
H /= np.linalg.norm(H)
for f in range(F):
    gx = np.zeros((N, N), np.float32)
    gy = np.zeros((N, N), np.float32)
    for (kx, ky, w, a), ph in zip(ONDAS, fases):
        arg = math.tau * (kx * xx / N + ky * yy / N - w * f / F) + ph
        c = np.cos(arg) * a
        gx += c * kx
        gy += c * ky
    gx *= 0.075
    gy *= 0.075
    n = np.dstack([-gx, -gy, np.ones_like(gx)])
    n /= np.linalg.norm(n, axis=2, keepdims=True)
    difusa = (n @ L)
    brillo = np.clip(n @ H, 0, 1) ** 60
    v = 0.80 + 0.16 * (difusa - difusa.mean()) + 0.55 * brillo
    v = np.clip(v, 0, 1.0)
    rgb = np.dstack([v * 232, v * 240, v * 244])
    alfa = 168 + 60 * brillo
    cuadros.append(imagen(rgb, alfa))
tira(cuadros, os.path.join(TMC, "water_still.png"), 2)

# agua que corre: estrías que bajan
base = R.random((N // 2, N // 4))
base = np.asarray(Image.fromarray((base * 255).astype(np.uint8)).resize((N, N * 2), Image.BICUBIC)).astype(np.float32) / 255
cuadros = []
for f in range(32):
    s = np.roll(np.vstack([base, base]), f * 2, axis=0)[:N, :]
    fino = np.roll(np.vstack([base, base]), f * 4 + 7, axis=0)[N // 2:N // 2 + N, ::-1]
    v = 0.78 + 0.13 * (s - 0.5) + 0.08 * (fino - 0.5)
    espuma = np.clip((s - 0.78) * 3, 0, 1) * 0.25
    v = np.clip(v + espuma, 0, 1)
    cuadros.append(imagen(np.dstack([v * 232, v * 240, v * 244]), 178 + 40 * espuma))
tira(cuadros, os.path.join(TMC, "water_flow.png"), 1)
ov = np.full((16, 16), 0.82)
Image.fromarray(np.dstack([ov * 232, ov * 240, ov * 244, np.full((16, 16), 176)]).astype(np.uint8), "RGBA").save(
    os.path.join(TMC, "water_overlay.png"))

# ===================================================================== charcos
T = 32
yy, xx = np.mgrid[0:T, 0:T].astype(np.float32)


def ruido(sem, escala):
    r = np.random.default_rng(sem)
    g = r.random((T // escala + 2, T // escala + 2))
    return np.asarray(Image.fromarray((g * 255).astype(np.uint8)).resize((T + escala * 2, T + escala * 2), Image.BICUBIC)).astype(
        np.float32)[escala:escala + T, escala:escala + T] / 255


forma = 0.55 * ruido(11, 8) + 0.3 * ruido(12, 4) + 0.15 * ruido(13, 2)
radial = 1 - np.hypot(xx - 15.5, yy - 15.5) / 22.0
campo = 0.55 * forma + 0.6 * radial
UMBRAL = {1: 0.60, 2: 0.50, 3: 0.38}
# reflejo del cielo: más claro hacia un lado, como un espejo oscuro
reflejo = 0.85 + 0.25 * (1 - yy / T) + 0.06 * (ruido(14, 8) - 0.5)
for nivel, u in UMBRAL.items():
    borde = np.clip((campo - u) / 0.07, 0, 1)
    nucleo = np.clip((campo - u - 0.05) / 0.08, 0, 1)
    base_rgb = np.dstack([94 * reflejo, 103 * reflejo, 112 * reflejo])
    barro = np.array([78, 66, 52], np.float32)
    rgb = base_rgb * nucleo[..., None] + barro * (1 - nucleo[..., None])
    alfa = borde * (120 + 75 * nucleo)
    calma = imagen(rgb, alfa)
    calma.save(os.path.join(TB, f"charco_{nivel}_calma.png"))
    # con lluvia: anillos que se abren donde caen las gotas
    gotas = []
    rr = np.random.default_rng(100 + nivel)
    while len(gotas) < 9 + nivel * 5:
        x, y = rr.random() * T, rr.random() * T
        if nucleo[int(y), int(x)] > 0.5:
            gotas.append((x, y, rr.integers(0, 16)))
    cuadros = []
    for f in range(16):
        luz = np.zeros((T, T), np.float32)
        for (x, y, f0) in gotas:
            edad = (f - f0) % 16
            if edad > 9:
                continue
            radio = 0.6 + edad * 0.55
            d = np.hypot(xx - x, yy - y)
            anillo = np.exp(-((d - radio) ** 2) / 0.35) * (1 - edad / 10.0)
            luz += anillo
        luz = np.clip(luz, 0, 1) * nucleo
        c = rgb + luz[..., None] * 70
        cuadros.append(imagen(c, alfa + luz * 40))
    tira(cuadros, os.path.join(TB, f"charco_{nivel}_lluvia.png"), 2, interpolar=False)

variantes = {}
for nivel in (1, 2, 3):
    for ll, nombre in (("true", "lluvia"), ("false", "calma")):
        m = f"charco_{nivel}_{nombre}"
        escribir(os.path.join(A, "models", "block", m + ".json"), {
            "ambientocclusion": False, "render_type": "minecraft:translucent",
            "textures": {"particle": G("block/" + m), "t": G("block/" + m)},
            "elements": [{"from": [0, 0.15, 0], "to": [16, 0.15, 16], "shade": False,
                          "faces": {"up": {"uv": [0, 0, 16, 16], "texture": "#t"}, "down": {"uv": [0, 0, 16, 16], "texture": "#t"}}}]})
        variantes[f"lluvia={ll},nivel={nivel}"] = [{"model": G("block/" + m), "y": r} for r in (0, 90, 180, 270)]
escribir(os.path.join(A, "blockstates", "charco.json"), {"variants": variantes})

for archivo, nombre in (("es_es.json", "Charco"), ("en_us.json", "Puddle")):
    ruta = os.path.join(A, "lang", archivo)
    actual = json.load(open(ruta, encoding="utf-8"))
    actual[f"block.{MOD}.charco"] = nombre
    actual[f"entity.{MOD}.tornado"] = "Tornado"
    escribir(ruta, actual)
print("v5 lluvia: recursos listos")

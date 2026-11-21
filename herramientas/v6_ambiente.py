#!/usr/bin/env python3
"""Sonidos de ambiente sintetizados (pájaros, grillos, chicharras, búho, ranas, olas, viento, arroyo) y partícula de hoja.
python3 herramientas/v6_ambiente.py  (después de v5_lluvia.py)
"""
import json
import os
import shutil
import subprocess
import tempfile
import wave

import numpy as np
from PIL import Image, ImageDraw

MOD = "guerramod"
RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
A = os.path.join(RAIZ, "src", "main", "resources", "assets", MOD)
SAL = os.path.join(A, "sounds", "ambiente")
os.makedirs(SAL, exist_ok=True)
SR = 44100
R = np.random.default_rng(606)


def guardar(nombre, s, vol=0.9):
    s = s - np.mean(s)
    s = s / max(1e-6, np.max(np.abs(s))) * vol
    tmp = tempfile.mkdtemp()
    wav = os.path.join(tmp, "a.wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((s * 32000).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "4", os.path.join(SAL, nombre + ".ogg")], check=True)
    shutil.rmtree(tmp)


def t(seg):
    return np.arange(int(SR * seg)) / SR


def silencio(seg):
    return np.zeros(int(SR * seg))


def env(n, a=0.01, r=0.05):
    e = np.ones(n)
    na, nr = max(1, int(a * SR)), max(1, int(r * SR))
    e[:na] = np.linspace(0, 1, na)
    e[-nr:] *= np.linspace(1, 0, nr)
    return e


def barrido(f0, f1, dur, forma="lin", arm=(1.0,), vib=0.0, vibf=0.0):
    tt = t(dur)
    if forma == "exp":
        f = f0 * (f1 / f0) ** (tt / dur)
    elif forma == "arco":
        f = f0 + (f1 - f0) * np.sin(np.pi * tt / dur)
    else:
        f = f0 + (f1 - f0) * tt / dur
    f = f * (1 + vib * np.sin(2 * np.pi * vibf * tt))
    fase = 2 * np.pi * np.cumsum(f) / SR
    s = sum(a * np.sin(fase * (k + 1)) for k, a in enumerate(arm))
    return s * env(len(tt), min(0.01, dur / 4), min(0.03, dur / 3))


def ruido_banda(dur, lo, hi):
    n = int(SR * dur)
    x = R.normal(0, 1, n)
    X = np.fft.rfft(x)
    fr = np.fft.rfftfreq(n, 1 / SR)
    X[(fr < lo) | (fr > hi)] = 0
    return np.fft.irfft(X, n)


def poner(base, trozo, inicio):
    i = int(inicio * SR)
    j = min(len(base), i + len(trozo))
    base[i:j] += trozo[:j - i]
    return base


def reverb(x, cola=0.6, mezcla=0.25):
    n = int(cola * SR)
    ir = R.normal(0, 1, n) * np.exp(-np.linspace(0, 6, n))
    y = np.convolve(x, ir)[:len(x)] / np.sqrt(n)
    return x + y * mezcla * 6


# --------------------------------------------------------------------- pájaros
def jilguero(i):
    s = silencio(3.2)
    tpos = 0.1
    while tpos < 2.9:
        f0 = R.uniform(3200, 5200)
        d = R.uniform(0.03, 0.09)
        tr = barrido(f0, f0 * R.uniform(0.7, 1.4), d, R.choice(["lin", "arco"]), arm=(1, 0.25))
        s = poner(s, tr * R.uniform(0.5, 1.0), tpos)
        tpos += d + R.uniform(0.01, 0.06)
        if R.random() < 0.12:
            tpos += R.uniform(0.1, 0.3)
    return reverb(s, 0.4, 0.15)


def mirlo(i):
    s = silencio(3.5)
    tpos = 0.15
    for _ in range(R.integers(4, 7)):
        f0 = R.uniform(1500, 2600)
        d = R.uniform(0.12, 0.32)
        s = poner(s, barrido(f0, f0 * R.uniform(0.8, 1.3), d, "arco", arm=(1, 0.15, 0.05), vib=0.02, vibf=30) * R.uniform(0.6, 1), tpos)
        tpos += d + R.uniform(0.04, 0.12)
    # final de gorjeo
    for _ in range(6):
        s = poner(s, barrido(3500, 4800, 0.03, "arco") * 0.4, tpos)
        tpos += 0.04
    return reverb(s, 0.7, 0.2)


def gorrion(i):
    s = silencio(2.6)
    tpos = 0.1
    for _ in range(R.integers(5, 9)):
        f0 = R.uniform(3300, 4200)
        s = poner(s, barrido(f0, f0 * 0.75, 0.07, "lin", arm=(1, 0.35, 0.1)) * R.uniform(0.6, 1), tpos)
        tpos += R.uniform(0.18, 0.32)
    return reverb(s, 0.3, 0.12)


def ruisenor(i):
    s = silencio(4.0)
    tpos = 0.1
    for _ in range(4):
        f0 = R.uniform(1800, 3000)
        # nota que crece y trino rápido
        n = barrido(f0, f0, 0.35, "lin", arm=(1, 0.1)) * np.linspace(0.2, 1, int(0.35 * SR))
        s = poner(s, n, tpos)
        tpos += 0.42
        for _ in range(R.integers(6, 12)):
            g = R.uniform(4000, 6500)
            s = poner(s, barrido(g, g * 0.6, 0.025) * 0.7, tpos)
            tpos += 0.035
        tpos += 0.15
    return reverb(s, 0.8, 0.25)


def paloma(i):
    s = silencio(3.6)
    patron = [(0.0, 0.25, 520), (0.35, 0.55, 600), (1.0, 0.25, 500), (1.35, 0.25, 500), (1.7, 0.4, 560)]
    for (ti, d, f) in patron:
        s = poner(s, barrido(f, f * 0.92, d, "arco", arm=(1, 0.3, 0.12)) * 0.9, 0.2 + ti)
    return reverb(paso_bajo(s, 1800), 0.5, 0.2)


def cuco(i):
    s = silencio(3.0)
    for k in range(3):
        s = poner(s, barrido(680, 670, 0.2, "lin", arm=(1, 0.08)), 0.2 + k * 0.85)
        s = poner(s, barrido(560, 540, 0.3, "lin", arm=(1, 0.08)), 0.45 + k * 0.85)
    return reverb(s, 1.0, 0.3)


def carpintero(i):
    s = silencio(2.5)
    tpos = 0.2
    golpe = ruido_banda(0.012, 600, 4000) * np.exp(-np.linspace(0, 8, int(0.012 * SR)))
    for k in range(int(R.integers(14, 22))):
        s = poner(s, golpe * (0.6 + 0.4 * np.sin(k / 3)), tpos)
        tpos += 0.045
    return reverb(s, 1.0, 0.35)


def gaviota(i):
    s = silencio(3.0)
    tpos = 0.1
    for _ in range(R.integers(2, 4)):
        d = R.uniform(0.25, 0.45)
        c = barrido(1300, 1900, d, "arco", arm=(1, 0.6, 0.4, 0.25)) + ruido_banda(d, 1000, 3000) * 0.15 * env(int(d * SR))
        s = poner(s, c, tpos)
        tpos += d + R.uniform(0.15, 0.35)
    for _ in range(4):
        s = poner(s, barrido(1500, 1100, 0.12, "lin", arm=(1, 0.5, 0.3)), tpos)
        tpos += 0.15
    return reverb(s, 0.8, 0.25)


def paso_bajo(x, corte):
    X = np.fft.rfft(x)
    fr = np.fft.rfftfreq(len(x), 1 / SR)
    X[fr > corte] *= np.exp(-(fr[fr > corte] - corte) / (corte * 0.3))
    return np.fft.irfft(X, len(x))


# --------------------------------------------------------------------- noche y verano
def grillos(i):
    dur = 6.0
    tt = t(dur)
    s = np.zeros(len(tt))
    for g in range(3):
        f = R.uniform(4300, 4900)
        periodo = R.uniform(0.45, 0.7)
        desfase = R.uniform(0, periodo)
        port = np.sin(2 * np.pi * f * tt)
        pulso = np.zeros(len(tt))
        tp = desfase
        while tp < dur:
            for k in range(R.integers(3, 5)):
                a = int((tp + k * 0.03) * SR)
                b = min(len(tt), a + int(0.018 * SR))
                pulso[a:b] = np.sin(np.linspace(0, np.pi, b - a))
            tp += periodo
        s += port * pulso * R.uniform(0.4, 1.0)
    return s


def chicharra(i):
    dur = 6.0
    tt = t(dur)
    zumbido = ruido_banda(dur, 3500, 8000) * (0.6 + 0.4 * np.sin(2 * np.pi * 180 * tt))
    onda = 0.5 + 0.5 * np.sin(2 * np.pi * tt / 3.0 - 1.2)
    return zumbido * (0.3 + 0.7 * onda)


def buho(i):
    s = silencio(4.0)
    s = poner(s, barrido(480, 440, 0.45, "arco", arm=(1, 0.2)), 0.2)
    s = poner(s, barrido(460, 420, 0.18, "arco", arm=(1, 0.2)) * 0.7, 1.1)
    tr = barrido(470, 430, 1.1, "arco", arm=(1, 0.2), vib=0.04, vibf=9)
    s = poner(s, tr, 1.5)
    return reverb(paso_bajo(s, 1500), 1.2, 0.35)


def rana(i):
    s = silencio(3.0)
    tpos = 0.2
    for _ in range(R.integers(3, 6)):
        d = R.uniform(0.12, 0.25)
        tt = t(d)
        f = R.uniform(320, 520)
        c = np.sin(2 * np.pi * f * tt) * (0.5 + 0.5 * np.sign(np.sin(2 * np.pi * 45 * tt))) * env(len(tt), 0.01, 0.04)
        s = poner(s, paso_bajo(c, 2000), tpos)
        tpos += d + R.uniform(0.2, 0.5)
    return reverb(s, 0.5, 0.2)


# --------------------------------------------------------------------- agua y viento
def olas(i):
    dur = 7.0
    tt = t(dur)
    n = ruido_banda(dur, 80, 6000)
    subida = np.clip(np.sin(np.pi * tt / dur), 0, 1) ** 2
    rompe = np.exp(-np.maximum(0, tt - 3.2) * 1.2) * (tt > 3.2)
    s = paso_bajo(n, 900) * subida * 0.8 + ruido_banda(dur, 1500, 9000) * rompe * 0.7
    return s


def viento_hojas(i):
    dur = 6.0
    tt = t(dur)
    mod = 0.5 + 0.5 * np.sin(2 * np.pi * tt / R.uniform(2.5, 4.0) + R.uniform(0, 6))
    hojas = ruido_banda(dur, 1500, 7000) * (R.random(len(tt)) < 0.25)
    return ruido_banda(dur, 300, 2500) * mod * 0.8 + paso_bajo(hojas, 6000) * mod * 0.6


def arroyo(i):
    dur = 6.0
    s = ruido_banda(dur, 200, 4000) * 0.3
    tpos = 0.0
    while tpos < dur - 0.1:
        d = R.uniform(0.02, 0.07)
        f0 = R.uniform(400, 1400)
        s = poner(s, barrido(f0, f0 * R.uniform(1.3, 2.0), d, "exp") * R.uniform(0.3, 0.8), tpos)
        tpos += R.uniform(0.01, 0.06)
    return s


def chapoteo(i):
    d = 0.6
    tt = t(d)
    s = ruido_banda(d, 300, 6000) * np.exp(-tt * 7)
    return poner(s, barrido(900, 300, 0.15, "exp") * 0.3, 0.0)


SONIDOS = {
    "jilguero": (jilguero, 4, 28), "mirlo": (mirlo, 3, 40), "gorrion": (gorrion, 3, 24), "ruisenor": (ruisenor, 3, 44),
    "paloma": (paloma, 2, 36), "cuco": (cuco, 2, 64), "carpintero": (carpintero, 2, 48), "gaviota": (gaviota, 3, 48),
    "grillos": (grillos, 3, 20), "chicharra": (chicharra, 2, 28), "buho": (buho, 2, 56), "rana": (rana, 3, 28),
    "olas": (olas, 3, 40), "viento_hojas": (viento_hojas, 3, 24), "arroyo": (arroyo, 2, 20), "chapoteo": (chapoteo, 2, 16),
}
NOMBRES = {"jilguero": "Canta un jilguero", "mirlo": "Canta un mirlo", "gorrion": "Pían gorriones", "ruisenor": "Canta un ruiseñor",
           "paloma": "Arrulla una paloma", "cuco": "Canta un cuco", "carpintero": "Tamborilea un pájaro carpintero",
           "gaviota": "Grazna una gaviota", "grillos": "Cantan los grillos", "chicharra": "Cantan las chicharras", "buho": "Ulula un búho",
           "rana": "Croan las ranas", "olas": "Rompen las olas", "viento_hojas": "El viento mueve las hojas", "arroyo": "Corre un arroyo",
           "chapoteo": "Chapoteo"}
ruta = os.path.join(A, "sounds.json")
sj = json.load(open(ruta, encoding="utf-8"))
for n, (fn, k, atenua) in SONIDOS.items():
    for i in range(k):
        guardar(f"{n}_{i}", fn(i), 0.85)
    sj["ambiente." + n] = {"subtitle": f"subtitles.{MOD}.ambiente.{n}",
                           "sounds": [{"name": f"{MOD}:ambiente/{n}_{i}", "attenuation_distance": atenua} for i in range(k)]}
json.dump(sj, open(ruta, "w", encoding="utf-8"), ensure_ascii=False, indent=1)
for archivo in ("es_es.json", "en_us.json"):
    rl = os.path.join(A, "lang", archivo)
    d = json.load(open(rl, encoding="utf-8"))
    for n, txt in NOMBRES.items():
        d[f"subtitles.{MOD}.ambiente.{n}"] = txt
    json.dump(d, open(rl, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

# --------------------------------------------------------------------- partícula de hoja (gris: se tiñe al aparecer)
TP = os.path.join(A, "textures", "particle")
os.makedirs(TP, exist_ok=True)
for k in range(4):
    im = Image.new("RGBA", (8, 8), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    if k == 0:
        d.ellipse((1, 2, 6, 6), fill=(230, 230, 230, 255)); d.line((1, 6, 6, 2), fill=(170, 170, 170, 255))
    elif k == 1:
        d.polygon([(4, 0), (7, 4), (4, 7), (1, 4)], fill=(225, 225, 225, 255)); d.line((4, 0, 4, 7), fill=(165, 165, 165, 255))
    elif k == 2:
        d.ellipse((2, 1, 5, 7), fill=(235, 235, 235, 255)); d.line((3, 1, 3, 7), fill=(170, 170, 170, 255))
    else:
        d.polygon([(1, 1), (6, 2), (7, 6), (2, 6)], fill=(220, 220, 220, 255))
    im.save(os.path.join(TP, f"hoja_{k}.png"))
os.makedirs(os.path.join(A, "particles"), exist_ok=True)
json.dump({"textures": [f"{MOD}:hoja_{k}" for k in range(4)]}, open(os.path.join(A, "particles", "hoja.json"), "w"), indent=1)
print("v6 ambiente: sonidos y partículas listos")

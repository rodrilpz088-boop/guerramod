#!/usr/bin/env python3
"""Sonidos de armas sintetizados: disparo (golpe + estampido + eco), mecanismos metálicos y casquillos.
Necesita numpy y ffmpeg.  python3 herramientas/v2_sonidos.py
"""
import os
import shutil
import subprocess
import tempfile
import wave

import numpy as np

MOD = "guerramod"
SAL = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src", "main", "resources", "assets", MOD, "sounds")
os.makedirs(SAL, exist_ok=True)
SR = 44100
R = np.random.default_rng(11)


def guardar(nombre, s):
    s = s / max(1e-6, np.max(np.abs(s))) * 0.95
    tmp = tempfile.mkdtemp()
    wav = os.path.join(tmp, "a.wav")
    with wave.open(wav, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes((s * 32000).astype(np.int16).tobytes())
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav, "-c:a", "libvorbis", "-q:a", "5",
                    os.path.join(SAL, nombre + ".ogg")], check=True)
    shutil.rmtree(tmp)


def t(seg):
    return np.arange(int(SR * seg)) / SR


def paso_bajo(x, corte):
    # filtro de un polo simple
    a = np.exp(-2 * np.pi * corte / SR)
    y = np.zeros_like(x)
    acc = 0.0
    for i in range(len(x)):
        acc = (1 - a) * x[i] + a * acc
        y[i] = acc
    return y


def paso_bajo_rapido(x, corte):
    from numpy.fft import rfft, irfft, rfftfreq
    X = rfft(x)
    f = rfftfreq(len(x), 1 / SR)
    X *= 1 / np.sqrt(1 + (f / corte) ** 4)
    return irfft(X, len(x))


def eco(x, retardo, ganancia, colas=4):
    y = np.copy(x)
    d = int(retardo * SR)
    for k in range(1, colas + 1):
        if d * k >= len(x):
            break
        y[d * k:] += x[:-d * k] * (ganancia ** k)
    return y


def reverb(x, segs, brillo):
    n = int(SR * segs)
    ir = R.normal(0, 1, n) * np.exp(-np.arange(n) / (SR * segs / 5))
    ir = paso_bajo_rapido(ir, brillo)
    y = np.convolve(x, ir)[: len(x) + n]
    return y / (np.max(np.abs(y)) + 1e-9)


def disparo(dur, grave, brillo, cola, golpe=1.0, crack=0.6):
    tt = t(dur)
    n = len(tt)
    ruido = R.normal(0, 1, n)
    transitorio = ruido * np.exp(-tt * 260) * golpe
    cuerpo = paso_bajo_rapido(ruido, brillo) * np.exp(-tt * 18)
    boom = np.sin(2 * np.pi * grave * tt * (1 - 0.4 * tt / dur)) * np.exp(-tt * 9) * 0.9
    chasquido = R.normal(0, 1, n) * np.exp(-tt * 900) * crack
    s = transitorio + cuerpo * 0.8 + boom + chasquido
    s = np.tanh(s * 2.2)
    seco = s
    hum = reverb(s[: int(SR * 0.25)], cola, 1800)
    out = np.zeros(max(len(seco), len(hum)))
    out[: len(seco)] += seco
    out[: len(hum)] += hum * 0.45
    out[:30] *= np.linspace(0, 1, 30)
    return out


def clic_metal(dur, frecs, decaer, ruido=0.4):
    tt = t(dur)
    s = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * decaer * (1 + i * 0.3)) for i, f in enumerate(frecs))
    s = s + R.normal(0, 1, len(tt)) * np.exp(-tt * decaer * 2) * ruido
    return s


def junta(*partes):
    total = max(int(off * SR) + len(p) for off, p in partes)
    out = np.zeros(total)
    for off, p in partes:
        i = int(off * SR)
        out[i:i + len(p)] += p
    return out


guardar("disparo_pistola", disparo(0.9, 110, 3800, 0.6, 1.0, 0.9))
guardar("disparo_subfusil", disparo(0.6, 120, 3200, 0.45, 0.9, 0.7))
guardar("disparo_fusil", disparo(1.2, 90, 5200, 0.9, 1.2, 1.2))
guardar("disparo_escopeta", disparo(1.4, 65, 2600, 1.1, 1.4, 0.5))
guardar("disparo_francotirador", disparo(2.2, 55, 4800, 1.8, 1.5, 1.4))
guardar("disparo_ametralladora", disparo(0.9, 80, 4600, 0.7, 1.2, 1.1))

guardar("cargador_fuera", junta((0, clic_metal(0.12, [2300, 3900], 60)), (0.05, clic_metal(0.2, [900, 1500], 30, 0.6) * 0.6)))
guardar("cargador_dentro", junta((0, clic_metal(0.1, [1700, 3100, 5200], 70, 0.7)), (0.06, clic_metal(0.12, [2600, 4400], 80) * 0.8)))
guardar("montar", junta((0, clic_metal(0.12, [1900, 3600], 55)), (0.14, clic_metal(0.15, [1500, 2800, 4700], 50, 0.6))))
guardar("cerrojo", junta((0, clic_metal(0.1, [1300, 2500], 60)), (0.12, R.normal(0, 1, int(SR * 0.12)) * 0.25 * np.exp(-t(0.12) * 25)),
                          (0.26, clic_metal(0.12, [1600, 3000, 4800], 55)), (0.4, clic_metal(0.1, [1200, 2300], 65))))
guardar("corredera", junta((0, R.normal(0, 1, int(SR * 0.09)) * np.exp(-t(0.09) * 30)), (0.08, clic_metal(0.12, [900, 1800, 3300], 45, 0.8)),
                            (0.24, R.normal(0, 1, int(SR * 0.08)) * np.exp(-t(0.08) * 35)), (0.31, clic_metal(0.12, [1100, 2100, 3600], 45, 0.8))))
guardar("cartucho", junta((0, clic_metal(0.08, [2100, 3400], 80, 0.5)), (0.05, clic_metal(0.07, [2800, 4100], 90) * 0.6)))
guardar("vacio", clic_metal(0.09, [3200, 5100], 90, 0.3))
guardar("modo", clic_metal(0.07, [2600, 4300], 100, 0.3))
guardar("clic", clic_metal(0.05, [3000, 4500], 140, 0.2))
tt = t(0.6)
silbido = np.sin(2 * np.pi * (3200 - 2200 * tt) * tt) * np.exp(-tt * 6)
guardar("rebote", junta((0, clic_metal(0.05, [2400, 4100], 120, 0.9)), (0.01, silbido * 0.6)))
guardar("casquillo", junta(*[(o, clic_metal(0.15, [5200 + k * 300, 7600], 40, 0.1) * a)
                             for k, (o, a) in enumerate([(0, 1.0), (0.11, 0.6), (0.19, 0.35), (0.24, 0.2)])]))
print("Sonidos v2 generados.")

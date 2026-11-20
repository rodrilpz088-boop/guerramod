#!/usr/bin/env python3
"""Comprueba que todos los programas del shader pack compilan (glslangValidator), expandiendo los #include como Iris."""
import os
import re
import subprocess
import sys
import tempfile

RAIZ = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "shaderpack", "shaders")
MACROS = """#define MC_VERSION 12001
#define MC_GL_VERSION 330
#define MC_GLSL_VERSION 330
#define MC_RENDER_STAGE_NONE 0
#define MC_RENDER_STAGE_SKY 1
#define MC_RENDER_STAGE_SUNSET 2
#define MC_RENDER_STAGE_CUSTOM_SKY 3
#define MC_RENDER_STAGE_SUN 4
#define MC_RENDER_STAGE_MOON 5
#define MC_RENDER_STAGE_STARS 6
#define MC_RENDER_STAGE_VOID 7
"""


def expandir(ruta, vistos):
    out = []
    for linea in open(ruta, encoding="utf-8").read().split("\n"):
        m = re.match(r'\s*#include\s+"([^"]+)"', linea)
        if m:
            inc = m.group(1)
            p = os.path.join(RAIZ, inc.lstrip("/")) if inc.startswith("/") else os.path.join(os.path.dirname(ruta), inc)
            out.append(expandir(os.path.normpath(p), vistos))
        else:
            out.append(linea)
    return "\n".join(out)


errores = 0
total = 0
tmp = tempfile.mkdtemp()
for carpeta in ("", "world-1", "world1"):
    d = os.path.join(RAIZ, carpeta)
    for f in sorted(os.listdir(d)):
        if not f.endswith((".vsh", ".fsh")):
            continue
        src = expandir(os.path.join(d, f), set())
        lineas = src.split("\n")
        src = lineas[0] + "\n" + MACROS + "\n".join(lineas[1:])
        ext = ".vert" if f.endswith(".vsh") else ".frag"
        nombre = os.path.join(tmp, (carpeta or "base") + "_" + f + ext)
        open(nombre, "w").write(src)
        total += 1
        r = subprocess.run(["glslangValidator", nombre], capture_output=True, text=True)
        if r.returncode != 0:
            errores += 1
            print(f"=== {carpeta or 'shaders'}/{f}")
            print("\n".join(l for l in r.stdout.split("\n") if "ERROR" in l or "error" in l)[:4000])
print(f"{total} programas, {errores} con errores")
sys.exit(1 if errores else 0)

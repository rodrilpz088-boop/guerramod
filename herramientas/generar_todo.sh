#!/bin/sh
# Regenera todos los recursos del mod (texturas, modelos, recetas, idioma, sonidos y el registro Java).
set -e
cd "$(dirname "$0")/.."
python3 herramientas/generar_datos.py
python3 herramientas/generar_texturas.py
python3 herramientas/v2_texturas.py
python3 herramientas/v2_catalogo.py
python3 herramientas/v2_sonidos.py

#!/usr/bin/env python3
"""Genera blockstates, modelos, loot tables, etiquetas, recetas e idiomas del mod.
Ejecutar desde la carpeta del proyecto:  python3 herramientas/generar_datos.py
"""
import json
import os

MOD = "guerramod"
RAIZ = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources")
A = os.path.join(RAIZ, "assets", MOD)
D = os.path.join(RAIZ, "data", MOD)
DMC = os.path.join(RAIZ, "data", "minecraft")


def escribir(ruta, datos):
    os.makedirs(os.path.dirname(ruta), exist_ok=True)
    with open(ruta, "w", encoding="utf-8") as f:
        json.dump(datos, f, ensure_ascii=False, indent=2)


def bs(nombre, datos):
    escribir(os.path.join(A, "blockstates", nombre + ".json"), datos)


def mb(nombre, datos):
    escribir(os.path.join(A, "models", "block", nombre + ".json"), datos)


def mi(nombre, datos):
    escribir(os.path.join(A, "models", "item", nombre + ".json"), datos)


def t(n):
    return f"{MOD}:block/{n}"


def item_generado(nombre, textura=None, mano=False):
    mi(nombre, {"parent": "minecraft:item/handheld" if mano else "minecraft:item/generated",
                "textures": {"layer0": textura or f"{MOD}:item/{nombre}"}})


def item_de_bloque(nombre, modelo=None):
    mi(nombre, {"parent": f"{MOD}:block/{modelo or nombre}"})


def simple_bs(nombre):
    bs(nombre, {"variants": {"": {"model": f"{MOD}:block/{nombre}"}}})


def orientado_bs(nombre, modelo=None, base=0):
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    bs(nombre, {"variants": {f"facing={k}": {"model": f"{MOD}:block/{modelo or nombre}", "y": (v + base) % 360}
                             for k, v in rot.items()}})


def loot(nombre, extra=None):
    entrada = {"type": "minecraft:item", "name": f"{MOD}:{nombre}"}
    pool = {"rolls": 1, "entries": [entrada], "conditions": [{"condition": "minecraft:survives_explosion"}]}
    if extra:
        extra(pool, entrada)
    escribir(os.path.join(D, "loot_tables", "blocks", nombre + ".json"),
             {"type": "minecraft:block", "pools": [pool]})


# ------------------------------------------------------------------ BLOQUES
CUBOS = ["asfalto", "hormigon_blindado", "sacos_terreros", "hormigon_reforzado", "bloque_plomo", "purificador_aire",
         "puesto_control"]
for n in CUBOS:
    simple_bs(n)
    mb(n, {"parent": "minecraft:block/cube_all", "textures": {"all": t(n)}})
    item_de_bloque(n)

# cristal blindado (transparente)
simple_bs("cristal_blindado")
mb("cristal_blindado", {"parent": "minecraft:block/cube_all", "render_type": "minecraft:cutout",
                        "textures": {"all": t("cristal_blindado")}})
item_de_bloque("cristal_blindado")

# cubos con arriba / lados / abajo distintos
ARRIBA_LADOS = {
    "radar": ("radar_arriba", "radar_lado", "hormigon_blindado"),
    "sirena": ("sirena_arriba", "sirena_lado", "sirena_arriba"),
    "lanzador_misiles": ("lanzador_misiles_arriba", "lanzador_misiles_lado", "hormigon_reforzado"),
    "bomba_nuclear": ("bomba_nuclear_arriba", "bomba_nuclear_lado", "bomba_nuclear_arriba"),
    "mesa_gobierno": ("mesa_gobierno_arriba", "mesa_gobierno_lado", "mesa_gobierno_abajo"),
}
for n, (arr, lad, aba) in ARRIBA_LADOS.items():
    simple_bs(n)
    mb(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": t(arr), "side": t(lad), "bottom": t(aba)}})
    item_de_bloque(n)

# carreteras con línea (giran según hacia dónde mires al ponerlas)
for n in ["asfalto_linea", "asfalto_linea_continua", "paso_cebra"]:
    orientado_bs(n)
    mb(n, {"parent": "minecraft:block/cube_bottom_top", "textures": {"top": t(n), "side": t("asfalto"), "bottom": t("asfalto")}})
    item_de_bloque(n)

# losa de asfalto
bs("asfalto_losa", {"variants": {
    "type=bottom": {"model": f"{MOD}:block/asfalto_losa"},
    "type=top": {"model": f"{MOD}:block/asfalto_losa_top"},
    "type=double": {"model": f"{MOD}:block/asfalto"}}})
mb("asfalto_losa", {"parent": "minecraft:block/slab", "textures": {"bottom": t("asfalto"), "top": t("asfalto"), "side": t("asfalto")}})
mb("asfalto_losa_top", {"parent": "minecraft:block/slab_top", "textures": {"bottom": t("asfalto"), "top": t("asfalto"), "side": t("asfalto")}})
item_de_bloque("asfalto_losa")

# valla fronteriza (como una valla normal)
bs("valla_fronteriza", {"multipart": [
    {"apply": {"model": f"{MOD}:block/valla_fronteriza_poste"}},
    {"when": {"north": "true"}, "apply": {"model": f"{MOD}:block/valla_fronteriza_lado", "uvlock": True}},
    {"when": {"east": "true"}, "apply": {"model": f"{MOD}:block/valla_fronteriza_lado", "y": 90, "uvlock": True}},
    {"when": {"south": "true"}, "apply": {"model": f"{MOD}:block/valla_fronteriza_lado", "y": 180, "uvlock": True}},
    {"when": {"west": "true"}, "apply": {"model": f"{MOD}:block/valla_fronteriza_lado", "y": 270, "uvlock": True}}]})
mb("valla_fronteriza_poste", {"parent": "minecraft:block/fence_post", "textures": {"texture": t("valla_fronteriza")}})
mb("valla_fronteriza_lado", {"parent": "minecraft:block/fence_side", "textures": {"texture": t("valla_fronteriza")}})
mb("valla_fronteriza_inventario", {"parent": "minecraft:block/fence_inventory", "textures": {"texture": t("valla_fronteriza")}})
item_de_bloque("valla_fronteriza", "valla_fronteriza_inventario")

# muro fronterizo (como un muro de piedra)
mp = [{"when": {"up": "true"}, "apply": {"model": f"{MOD}:block/muro_fronterizo_poste"}}]
for lado, y in [("north", 0), ("east", 90), ("south", 180), ("west", 270)]:
    mp.append({"when": {lado: "low"}, "apply": {"model": f"{MOD}:block/muro_fronterizo_lado", "y": y, "uvlock": True}})
    mp.append({"when": {lado: "tall"}, "apply": {"model": f"{MOD}:block/muro_fronterizo_lado_alto", "y": y, "uvlock": True}})
bs("muro_fronterizo", {"multipart": mp})
mb("muro_fronterizo_poste", {"parent": "minecraft:block/template_wall_post", "textures": {"wall": t("muro_fronterizo")}})
mb("muro_fronterizo_lado", {"parent": "minecraft:block/template_wall_side", "textures": {"wall": t("muro_fronterizo")}})
mb("muro_fronterizo_lado_alto", {"parent": "minecraft:block/template_wall_side_tall", "textures": {"wall": t("muro_fronterizo")}})
mb("muro_fronterizo_inventario", {"parent": "minecraft:block/wall_inventory", "textures": {"wall": t("muro_fronterizo")}})
item_de_bloque("muro_fronterizo", "muro_fronterizo_inventario")


def cara(tex, uv=None, cull=None):
    c = {"texture": tex}
    if uv:
        c["uv"] = uv
    if cull:
        c["cullface"] = cull
    return c


def caja(desde, hasta, tex, uv=None):
    return {"from": desde, "to": hasta, "faces": {d: cara(tex, uv) for d in ["north", "south", "east", "west", "up", "down"]}}


# hito de frontera
simple_bs("hito_frontera")
mb("hito_frontera", {"parent": "minecraft:block/block", "textures": {"particle": t("hito_frontera"), "h": t("hito_frontera")},
                     "elements": [caja([5, 0, 5], [11, 14, 11], "#h")]})
item_de_bloque("hito_frontera")

# farola
simple_bs("farola")
mb("farola", {"parent": "minecraft:block/block",
              "textures": {"particle": t("farola_poste"), "p": t("farola_poste"), "l": t("farola_luz")},
              "elements": [caja([7, 0, 7], [9, 13, 9], "#p"), caja([5, 13, 5], [11, 16, 11], "#l")]})
item_de_bloque("farola")

# barrera de control (dos postes y dos barras a rayas)
orientado_bs("barrera_control")
mb("barrera_control", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                       "textures": {"particle": t("barrera_control"), "b": t("barrera_control"), "p": t("valla_fronteriza")},
                       "elements": [caja([0, 0, 6.5], [2, 14, 9.5], "#p"), caja([14, 0, 6.5], [16, 14, 9.5], "#p"),
                                    caja([0, 9, 7], [16, 12, 9], "#b", [0, 0, 16, 3]),
                                    caja([0, 3, 7], [16, 6, 9], "#b", [0, 8, 16, 11])]})
item_de_bloque("barrera_control")

# alambre de espino (como telaraña)
simple_bs("alambre_espino")
mb("alambre_espino", {"parent": "minecraft:block/cross", "render_type": "minecraft:cutout", "textures": {"cross": t("alambre_espino")}})
item_generado("alambre_espino", t("alambre_espino"))

# mina terrestre
simple_bs("mina_terrestre")
mb("mina_terrestre", {"parent": "minecraft:block/block", "textures": {"particle": t("mina_terrestre"), "m": t("mina_terrestre")},
                      "elements": [caja([2, 0, 2], [14, 2, 14], "#m")]})
item_de_bloque("mina_terrestre")

# urna electoral
simple_bs("urna_electoral")
mb("urna_electoral", {"parent": "minecraft:block/block", "render_type": "minecraft:cutout",
                      "textures": {"particle": t("urna_lado"), "s": t("urna_lado"), "a": t("urna_arriba")},
                      "elements": [{"from": [3, 0, 3], "to": [13, 12, 13], "faces": {
                          "north": cara("#s", [3, 4, 13, 16]), "south": cara("#s", [3, 4, 13, 16]),
                          "east": cara("#s", [3, 4, 13, 16]), "west": cara("#s", [3, 4, 13, 16]),
                          "up": cara("#a", [3, 3, 13, 13]), "down": cara("#a", [3, 3, 13, 13])}}]})
item_de_bloque("urna_electoral")

# puerta de búnker
variantes = {}
base = {"east": 0, "south": 90, "west": 180, "north": 270}
for f, y in base.items():
    for half in ["lower", "upper"]:
        for hinge in ["left", "right"]:
            for abierta in ["false", "true"]:
                parte = "bottom" if half == "lower" else "top"
                modelo = f"{MOD}:block/puerta_bunker_{parte}_{hinge}" + ("_open" if abierta == "true" else "")
                rot = y
                if abierta == "true":
                    rot = (y + 90) % 360 if hinge == "left" else (y + 270) % 360
                v = {"model": modelo}
                if rot:
                    v["y"] = rot
                variantes[f"facing={f},half={half},hinge={hinge},open={abierta}"] = v
bs("puerta_bunker", {"variants": variantes})
for parte in ["bottom", "top"]:
    for hinge in ["left", "right"]:
        for sufijo in ["", "_open"]:
            mb(f"puerta_bunker_{parte}_{hinge}{sufijo}", {
                "parent": f"minecraft:block/door_{parte}_{hinge}{sufijo}", "render_type": "minecraft:cutout",
                "textures": {"bottom": t("puerta_bunker_abajo"), "top": t("puerta_bunker_arriba")}})
item_generado("puerta_bunker")

# banderas
BANDERAS = ["espana", "francia", "alemania", "italia", "portugal", "reino_unido", "estados_unidos", "mexico",
            "argentina", "brasil", "japon", "ucrania", "rusia", "china", "canada", "polonia", "paises_bajos",
            "belgica", "suecia", "grecia", "colombia", "irlanda"]
mb("plantilla_bandera", {
    "parent": "minecraft:block/block",
    "textures": {"particle": "#bandera", "poste": "minecraft:block/iron_block"},
    "elements": [
        caja([7.25, 0, 7.25], [8.75, 30, 8.75], "#poste", [7, 0, 9, 16]),
        {"from": [8.75, 18, 7.75], "to": [26.75, 30, 8.25], "faces": {
            "north": cara("#bandera", [16, 0, 0, 16]), "south": cara("#bandera", [0, 0, 16, 16]),
            "east": cara("#bandera", [0, 0, 1, 16]), "west": cara("#bandera", [0, 0, 1, 16]),
            "up": cara("#bandera", [0, 0, 16, 1]), "down": cara("#bandera", [0, 15, 16, 16])}}]})
for b in BANDERAS:
    n = "bandera_" + b
    orientado_bs(n, base=180)
    mb(n, {"parent": f"{MOD}:block/plantilla_bandera", "textures": {"bandera": t(n)}})
    item_generado(n, t(n))

# ------------------------------------------------------------------ ITEMS
ITEMS_MANO = ["pistola", "fusil", "escopeta", "francotirador", "lanzacohetes", "designador_laser"]
ITEMS = ["placa_acero", "circuito", "municion", "cohete", "bomba_aerea", "misil_balistico", "misil_nuclear",
         "proyectil_bala", "granada", "avion_caza", "radio_bombardeo", "geiger", "pastillas_yodo",
         "casco_militar", "chaleco_antibalas", "pantalon_militar", "botas_militares",
         "mascara_nbq", "traje_nbq", "pantalon_nbq", "botas_nbq"]
for n in ITEMS_MANO:
    item_generado(n, mano=True)
for n in ITEMS:
    item_generado(n)

# ------------------------------------------------------------------ LOOT TABLES
BLOQUES = CUBOS + ["cristal_blindado"] + list(ARRIBA_LADOS) + ["asfalto_linea", "asfalto_linea_continua", "paso_cebra",
                                                               "valla_fronteriza", "muro_fronterizo", "hito_frontera",
                                                               "farola", "barrera_control", "alambre_espino",
                                                               "mina_terrestre", "urna_electoral"] + ["bandera_" + b for b in BANDERAS]
for n in BLOQUES:
    loot(n)


def losa(pool, entrada):
    entrada["functions"] = [{"function": "minecraft:set_count", "count": 2, "add": False,
                             "conditions": [{"condition": "minecraft:block_state_property",
                                             "block": f"{MOD}:asfalto_losa", "properties": {"type": "double"}}]},
                            {"function": "minecraft:explosion_decay"}]


def puerta(pool, entrada):
    entrada["conditions"] = [{"condition": "minecraft:block_state_property", "block": f"{MOD}:puerta_bunker",
                              "properties": {"half": "lower"}}]


loot("asfalto_losa", losa)
loot("puerta_bunker", puerta)

# ------------------------------------------------------------------ ETIQUETAS


def tag(carpeta, nombre, valores):
    escribir(os.path.join(carpeta, "tags", "blocks", nombre + ".json"), {"replace": False, "values": valores})


m = lambda n: f"{MOD}:{n}"
tag(D, "a_prueba_de_balas", [m(n) for n in ["hormigon_blindado", "cristal_blindado", "sacos_terreros", "hormigon_reforzado",
                                            "bloque_plomo", "puerta_bunker", "muro_fronterizo", "barrera_control",
                                            "valla_fronteriza", "puesto_control", "lanzador_misiles", "bomba_nuclear",
                                            "purificador_aire", "radar", "sirena"]]
    + ["minecraft:obsidian", "minecraft:iron_block", "minecraft:crying_obsidian"])
tag(D, "anti_radiacion", [m("hormigon_reforzado"), m("bloque_plomo"), m("puerta_bunker")])
tag(DMC, "fences", [m("valla_fronteriza")])
tag(DMC, "walls", [m("muro_fronterizo")])
tag(DMC, os.path.join("mineable", "pickaxe"),
    [m(n) for n in ["asfalto", "asfalto_losa", "asfalto_linea", "asfalto_linea_continua", "paso_cebra", "hormigon_blindado",
                    "cristal_blindado", "hormigon_reforzado", "bloque_plomo", "puerta_bunker", "purificador_aire",
                    "puesto_control", "radar", "sirena", "lanzador_misiles", "bomba_nuclear", "valla_fronteriza",
                    "muro_fronterizo", "hito_frontera", "farola", "barrera_control", "alambre_espino", "mina_terrestre"]])
tag(DMC, os.path.join("mineable", "axe"), [m("mesa_gobierno"), m("urna_electoral")])
tag(DMC, os.path.join("mineable", "shovel"), [m("sacos_terreros")])

# ------------------------------------------------------------------ RECETAS


def receta(nombre, patron, claves, cantidad=1, resultado=None):
    k = {}
    for c, v in claves.items():
        k[c] = {"tag": v[1:]} if v.startswith("#") else {"item": v}
    escribir(os.path.join(D, "recipes", nombre + ".json"), {
        "type": "minecraft:crafting_shaped", "category": "misc", "pattern": patron, "key": k,
        "result": {"item": resultado or f"{MOD}:{nombre}", "count": cantidad}})


def sin_forma(nombre, ingredientes, cantidad=1):
    escribir(os.path.join(D, "recipes", nombre + ".json"), {
        "type": "minecraft:crafting_shapeless", "category": "misc",
        "ingredients": [{"tag": i[1:]} if i.startswith("#") else {"item": i} for i in ingredientes],
        "result": {"item": f"{MOD}:{nombre}", "count": cantidad}})


mc = lambda n: "minecraft:" + n
P = m("placa_acero")
C = m("circuito")
receta("placa_acero", ["III"], {"I": mc("iron_ingot")}, 2)
receta("circuito", ["RGR", "CCC"], {"R": mc("redstone"), "G": mc("gold_nugget"), "C": mc("copper_ingot")}, 2)
receta("municion", ["NCN", "NGN"], {"N": mc("iron_nugget"), "C": mc("copper_ingot"), "G": mc("gunpowder")}, 16)
receta("cohete", ["P", "T", "G"], {"P": P, "T": mc("tnt"), "G": mc("gunpowder")}, 2)
receta("bomba_aerea", [" P ", "PTP", " P "], {"P": P, "T": mc("tnt")}, 2)
receta("misil_balistico", [" P ", "PTP", "FCF"], {"P": P, "T": mc("tnt"), "F": mc("firework_rocket"), "C": C})
receta("misil_nuclear", [" P ", "PNP", "FCF"], {"P": P, "N": m("bomba_nuclear"), "F": mc("firework_rocket"), "C": C})
receta("pistola", ["PPP", "  W"], {"P": P, "W": "#minecraft:planks"})
receta("fusil", ["PPP", "WCW"], {"P": P, "W": "#minecraft:planks", "C": C})
receta("escopeta", ["PPP", "IWW"], {"P": P, "W": "#minecraft:planks", "I": mc("iron_ingot")})
receta("francotirador", ["  G", "PPP", "WC "], {"G": mc("glass_pane"), "P": P, "W": "#minecraft:planks", "C": C})
receta("lanzacohetes", ["PPP", "CRP", "PPP"], {"P": P, "C": C, "R": m("cohete")})
receta("granada", [" I ", "IGI", " I "], {"I": mc("iron_nugget"), "G": mc("gunpowder")}, 4)
receta("avion_caza", [" P ", "PCP", "PFP"], {"P": P, "C": C, "F": mc("furnace")})
receta("radio_bombardeo", ["L", "C", "P"], {"L": mc("lightning_rod"), "C": C, "P": P})
receta("designador_laser", ["G G", "PCP"], {"G": mc("glass"), "P": P, "C": C})
receta("geiger", ["PRP", "PCP"], {"P": mc("yellow_dye"), "R": mc("redstone"), "C": C})
sin_forma("pastillas_yodo", [mc("dried_kelp"), mc("sugar"), mc("glass_bottle")], 4)
L = mc("leather")
receta("casco_militar", ["PPP", "PLP"], {"P": P, "L": L})
receta("chaleco_antibalas", ["PLP", "PPP", "PPP"], {"P": P, "L": L})
receta("pantalon_militar", ["PPP", "L L", "P P"], {"P": P, "L": L})
receta("botas_militares", ["L L", "P P"], {"P": P, "L": L})
Y = mc("yellow_wool")
receta("mascara_nbq", ["YYY", "YGY"], {"Y": Y, "G": mc("glass_pane")})
receta("traje_nbq", ["Y Y", "YLY", "YYY"], {"Y": Y, "L": L})
receta("pantalon_nbq", ["YYY", "Y Y", "Y Y"], {"Y": Y})
receta("botas_nbq", ["Y Y", "L L"], {"Y": Y, "L": L})

receta("hito_frontera", ["R", "S", "S"], {"R": mc("red_dye"), "S": mc("smooth_stone")}, 2)
receta("valla_fronteriza", ["PBP", "PBP"], {"P": P, "B": mc("iron_bars")}, 6)
receta("muro_fronterizo", ["CCC", "CCC"], {"C": mc("gray_concrete")}, 6)
receta("barrera_control", ["RWR", "P P"], {"R": mc("red_concrete"), "W": mc("white_concrete"), "P": P}, 2)
receta("puesto_control", ["PGP", "PCP", "SSS"], {"P": P, "G": mc("glass"), "C": C, "S": mc("stone_bricks")})
receta("alambre_espino", ["I I", " I ", "I I"], {"I": mc("iron_nugget")}, 4)
receta("asfalto", ["GGG", "GCG", "GGG"], {"G": mc("gravel"), "C": "#minecraft:coals"}, 8)
receta("asfalto_losa", ["AAA"], {"A": m("asfalto")}, 6)
receta("asfalto_linea", ["AWA"], {"A": m("asfalto"), "W": mc("white_dye")}, 3)
receta("asfalto_linea_continua", ["A", "W", "A"], {"A": m("asfalto"), "W": mc("white_dye")}, 2)
receta("paso_cebra", ["WAW"], {"A": m("asfalto"), "W": mc("white_dye")}, 1)
receta("farola", ["G", "I", "I"], {"G": mc("glowstone"), "I": mc("iron_ingot")}, 2)
receta("hormigon_blindado", ["CPC", "PCP", "CPC"], {"C": mc("gray_concrete"), "P": P}, 8)
receta("cristal_blindado", ["GPG", "PGP", "GPG"], {"G": mc("glass"), "P": P}, 4)
receta("sacos_terreros", [" W ", "WSW", " W "], {"W": mc("brown_wool"), "S": mc("sand")}, 4)
receta("hormigon_reforzado", ["PIP", "IOI", "PIP"], {"P": P, "I": mc("iron_ingot"), "O": mc("obsidian")}, 4)
receta("bloque_plomo", ["PPP", "PDP", "PPP"], {"P": P, "D": mc("deepslate")}, 2)
receta("puerta_bunker", ["PP", "PO", "PP"], {"P": P, "O": mc("obsidian")})
receta("purificador_aire", ["PBP", "BCB", "PRP"], {"P": P, "B": mc("iron_bars"), "C": C, "R": mc("redstone")})
receta("mina_terrestre", ["S", "T"], {"S": mc("stone_pressure_plate"), "T": mc("tnt")}, 2)
receta("sirena", ["PNP", "PCP", "PRP"], {"P": P, "N": mc("note_block"), "C": C, "R": mc("redstone")})
receta("radar", ["PLP", "PCP", "PPP"], {"P": P, "L": mc("lightning_rod"), "C": C})
receta("lanzador_misiles", ["PCP", "P P", "PFP"], {"P": P, "C": C, "F": mc("furnace")})
receta("bomba_nuclear", ["TGT", "GCG", "TGT"], {"T": mc("tnt"), "G": mc("glowstone"), "C": C})
receta("mesa_gobierno", ["BPB", "WWW", "W W"], {"B": mc("book"), "P": mc("paper"), "W": "#minecraft:planks"})
receta("urna_electoral", ["GPG", "G G", "GGG"], {"G": mc("glass"), "P": mc("paper")})

# banderas: palo a la izquierda y 4 lanas con los colores del país
COLORES_BANDERA = {
    "espana": ("red", "yellow", "yellow", "red"), "francia": ("blue", "white", "white", "red"),
    "alemania": ("black", "black", "red", "yellow"), "italia": ("green", "white", "white", "red"),
    "portugal": ("green", "red", "red", "red"), "reino_unido": ("blue", "red", "white", "blue"),
    "estados_unidos": ("blue", "red", "white", "red"), "mexico": ("green", "white", "red", "brown"),
    "argentina": ("light_blue", "white", "white", "yellow"), "brasil": ("green", "yellow", "blue", "green"),
    "japon": ("white", "red", "white", "white"), "ucrania": ("blue", "blue", "yellow", "yellow"),
    "rusia": ("white", "blue", "blue", "red"), "china": ("red", "yellow", "red", "red"),
    "canada": ("red", "white", "white", "red"), "polonia": ("white", "white", "red", "red"),
    "paises_bajos": ("red", "white", "white", "blue"), "belgica": ("black", "yellow", "yellow", "red"),
    "suecia": ("blue", "yellow", "yellow", "blue"), "grecia": ("blue", "white", "white", "blue"),
    "colombia": ("yellow", "yellow", "blue", "red"), "irlanda": ("green", "white", "white", "orange"),
}
assert len(set(COLORES_BANDERA.values())) == len(COLORES_BANDERA), "dos banderas con la misma receta"
for b, (a1, a2, b1, b2) in COLORES_BANDERA.items():
    receta("bandera_" + b, ["SAB", "SCD", "S  "],
           {"S": mc("stick"), "A": mc(a1 + "_wool"), "B": mc(a2 + "_wool"), "C": mc(b1 + "_wool"), "D": mc(b2 + "_wool")})

# ------------------------------------------------------------------ IDIOMA
NOMBRES = {
    # bloques
    "hito_frontera": "Hito de frontera", "valla_fronteriza": "Valla fronteriza", "muro_fronterizo": "Muro fronterizo",
    "barrera_control": "Barrera de control fronterizo", "puesto_control": "Puesto de control",
    "alambre_espino": "Alambre de espino", "asfalto": "Asfalto", "asfalto_losa": "Losa de asfalto",
    "asfalto_linea": "Carretera (línea discontinua)", "asfalto_linea_continua": "Carretera (línea continua)",
    "paso_cebra": "Paso de cebra", "farola": "Farola", "hormigon_blindado": "Hormigón blindado (antibalas)",
    "cristal_blindado": "Cristal blindado (antibalas)", "sacos_terreros": "Sacos terreros",
    "hormigon_reforzado": "Hormigón reforzado de búnker", "bloque_plomo": "Bloque de plomo",
    "puerta_bunker": "Puerta de búnker", "purificador_aire": "Purificador de aire",
    "mina_terrestre": "Mina terrestre", "sirena": "Sirena antiaérea", "radar": "Radar",
    "lanzador_misiles": "Lanzador de misiles", "bomba_nuclear": "Bomba nuclear",
    "mesa_gobierno": "Mesa de gobierno", "urna_electoral": "Urna electoral",
}
BANDERA_NOMBRES = {
    "espana": "España", "francia": "Francia", "alemania": "Alemania", "italia": "Italia", "portugal": "Portugal",
    "reino_unido": "Reino Unido", "estados_unidos": "Estados Unidos", "mexico": "México", "argentina": "Argentina",
    "brasil": "Brasil", "japon": "Japón", "ucrania": "Ucrania", "rusia": "Rusia", "china": "China", "canada": "Canadá",
    "polonia": "Polonia", "paises_bajos": "Países Bajos", "belgica": "Bélgica", "suecia": "Suecia", "grecia": "Grecia",
    "colombia": "Colombia", "irlanda": "Irlanda"}
NOMBRES_ITEMS = {
    "placa_acero": "Placa de acero", "circuito": "Circuito electrónico", "municion": "Munición",
    "cohete": "Cohete", "bomba_aerea": "Bomba aérea", "misil_balistico": "Misil balístico",
    "misil_nuclear": "Misil nuclear", "proyectil_bala": "Bala", "pistola": "Pistola", "fusil": "Fusil de asalto",
    "escopeta": "Escopeta", "francotirador": "Rifle de francotirador", "lanzacohetes": "Lanzacohetes",
    "granada": "Granada", "avion_caza": "Avión de combate", "radio_bombardeo": "Radio de bombardeo",
    "designador_laser": "Designador láser", "geiger": "Contador Geiger", "pastillas_yodo": "Pastillas de yodo",
    "casco_militar": "Casco militar", "chaleco_antibalas": "Chaleco antibalas", "pantalon_militar": "Pantalón militar",
    "botas_militares": "Botas militares", "mascara_nbq": "Máscara NBQ", "traje_nbq": "Traje NBQ",
    "pantalon_nbq": "Pantalón NBQ", "botas_nbq": "Botas NBQ",
}
idioma = {}
for k, v in NOMBRES.items():
    idioma[f"block.{MOD}.{k}"] = v
for k, v in BANDERA_NOMBRES.items():
    idioma[f"block.{MOD}.bandera_{k}"] = "Bandera de " + v
for k, v in NOMBRES_ITEMS.items():
    idioma[f"item.{MOD}.{k}"] = v
idioma.update({
    f"entity.{MOD}.bala": "Bala", f"entity.{MOD}.cohete": "Cohete", f"entity.{MOD}.granada": "Granada",
    f"entity.{MOD}.bomba_aerea": "Bomba aérea", f"entity.{MOD}.misil": "Misil", f"entity.{MOD}.avion": "Avión de combate",
    f"entity.{MOD}.bombardero": "Bombardero",
    f"effect.{MOD}.radiacion": "Radiación", f"effect.{MOD}.proteccion_radiacion": "Protección antirradiación",
    f"itemGroup.{MOD}.guerra": "Naciones en Guerra: Armas y búnker",
    f"itemGroup.{MOD}.naciones": "Naciones en Guerra: Fronteras y gobierno",
    "key.categories.guerramod": "Naciones en Guerra",
    f"key.{MOD}.minimapa": "Mostrar/ocultar minimapa", f"key.{MOD}.disparar": "Avión: ametralladora",
    f"key.{MOD}.bomba": "Avión: soltar bomba",
    f"subtitles.{MOD}.disparo": "Disparo", f"subtitles.{MOD}.disparo_fuerte": "Disparo potente",
    f"subtitles.{MOD}.sirena": "Sirena antiaérea", f"subtitles.{MOD}.geiger": "Contador Geiger",
    f"subtitles.{MOD}.alarma": "Alarma",
})
escribir(os.path.join(A, "lang", "es_es.json"), idioma)
escribir(os.path.join(A, "lang", "en_us.json"), idioma)  # mismos textos en español aunque el juego esté en inglés

# ------------------------------------------------------------------ SONIDOS
escribir(os.path.join(A, "sounds.json"), {
    n: {"subtitle": f"subtitles.{MOD}.{n}", "sounds": [{"name": f"{MOD}:{n}"}]}
    for n in ["disparo", "disparo_fuerte", "sirena", "geiger", "alarma"]})

print("Datos generados en", os.path.abspath(RAIZ))

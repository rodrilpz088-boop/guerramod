package es.guerramod.pais;

import java.util.LinkedHashMap;
import java.util.Map;

/** Colores disponibles para los países. */
public final class Colores {
    public static final Map<String, Integer> LISTA = new LinkedHashMap<>();

    static {
        LISTA.put("rojo", 0xE53935);
        LISTA.put("azul", 0x1E63D6);
        LISTA.put("verde", 0x2E9D3A);
        LISTA.put("amarillo", 0xF2C230);
        LISTA.put("naranja", 0xF57C00);
        LISTA.put("morado", 0x8E24AA);
        LISTA.put("rosa", 0xF06292);
        LISTA.put("cian", 0x00ACC1);
        LISTA.put("blanco", 0xEEEEEE);
        LISTA.put("negro", 0x2B2B2B);
        LISTA.put("gris", 0x8A8A8A);
        LISTA.put("marron", 0x795548);
        LISTA.put("lima", 0x9CCC65);
        LISTA.put("magenta", 0xD81B60);
        LISTA.put("celeste", 0x64B5F6);
        LISTA.put("granate", 0x8B1A1A);
    }

    private Colores() {}
}

package es.guerramod.registro;

import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** Apartados del modo creativo. Cada uno es una pestaña. */
public enum Categoria {
    ARMAS("armas"),
    MILITAR("militar"),
    NACIONES("naciones"),
    CARRETERAS("carreteras"),
    SALON("salon"),
    COCINA("cocina"),
    BANO("bano"),
    DORMITORIO("dormitorio"),
    PUERTAS("puertas"),
    JARDIN("jardin"),
    ILUMINACION("iluminacion"),
    CONSTRUCCION("construccion"),
    INCENDIOS("incendios"),
    CIUDAD("ciudad"),
    VIVIENDA("vivienda"),
    METRO("metro"),
    NAVIDAD("navidad");

    public final String id;
    public final List<Supplier<? extends ItemLike>> lista = new ArrayList<>();

    Categoria(String id) {
        this.id = id;
    }
}

package es.guerramod.clima;

import net.minecraft.core.Holder;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;

/**
 * Clima de cada zona (bioma) en un día de verano: temperatura media, oscilación entre el día y la noche, humedad del
 * aire y nubosidad. Así la nieve está fría, los desiertos son secos con noches frescas, la selva es húmeda y estable,
 * y el mar suaviza las temperaturas.
 */
public final class Zonas {
    private Zonas() {}

    /** Datos de una zona. tMedia y amplitud en °C, humedad en %, nubes multiplica la nubosidad general. */
    public record Zona(String nombre, double tMedia, double amplitud, double humedad, double nubes, boolean fria) {}

    private static String clave(Holder<Biome> b) {
        return b.unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    public static Zona de(Holder<Biome> b) {
        String k = clave(b);
        switch (k) {
            case "plains", "sunflower_plains": return new Zona("Llanura templada", 15, 6, 46, 1.0, false);
            case "meadow": return new Zona("Pradera de montaña", 11, 6, 55, 1.1, false);
            case "forest", "flower_forest": return new Zona("Bosque templado", 14, 5.5, 55, 1.0, false);
            case "birch_forest", "old_growth_birch_forest": return new Zona("Bosque de abedules", 13, 5.5, 56, 1.0, false);
            case "dark_forest": return new Zona("Bosque oscuro", 14, 4.5, 66, 1.1, false);
            case "cherry_grove": return new Zona("Cerezal", 14, 5.5, 58, 1.0, false);
            case "taiga", "old_growth_pine_taiga", "old_growth_spruce_taiga": return new Zona("Taiga", 7, 6, 62, 1.15, true);
            case "snowy_taiga": return new Zona("Taiga nevada", -6, 5, 72, 1.2, true);
            case "snowy_plains": return new Zona("Tundra nevada", -5, 5, 72, 1.1, true);
            case "ice_spikes": return new Zona("Glaciar", -12, 4, 70, 0.9, true);
            case "frozen_peaks", "jagged_peaks": return new Zona("Cumbres heladas", -13, 4, 60, 1.2, true);
            case "snowy_slopes": return new Zona("Laderas nevadas", -8, 4.5, 65, 1.2, true);
            case "grove": return new Zona("Arboleda nevada", -4, 5, 66, 1.15, true);
            case "stony_peaks": return new Zona("Picos rocosos", 6, 6, 45, 1.2, true);
            case "windswept_hills", "windswept_gravelly_hills": return new Zona("Colinas ventosas", 8, 6, 52, 1.2, true);
            case "windswept_forest": return new Zona("Bosque ventoso", 9, 5.5, 58, 1.2, true);
            case "windswept_savanna": return new Zona("Sabana ventosa", 25, 7, 30, 0.6, false);
            case "savanna": return new Zona("Sabana", 27, 7, 33, 0.6, false);
            case "savanna_plateau": return new Zona("Meseta de sabana", 25, 7.5, 30, 0.6, false);
            case "desert": return new Zona("Desierto", 31, 9.5, 17, 0.25, false);
            case "badlands", "eroded_badlands": return new Zona("Desierto árido (badlands)", 30, 9, 19, 0.35, false);
            case "wooded_badlands": return new Zona("Badlands arboladas", 28, 8.5, 22, 0.4, false);
            case "jungle", "bamboo_jungle": return new Zona("Selva tropical", 27, 4, 84, 1.35, false);
            case "sparse_jungle": return new Zona("Selva clara", 26, 4.5, 78, 1.25, false);
            case "swamp": return new Zona("Pantano", 21, 4, 86, 1.25, false);
            case "mangrove_swamp": return new Zona("Manglar", 27, 3.5, 88, 1.3, false);
            case "beach": return new Zona("Playa", 20, 4, 72, 0.9, false);
            case "snowy_beach": return new Zona("Playa helada", -3, 3.5, 75, 1.1, true);
            case "stony_shore": return new Zona("Costa rocosa", 14, 4, 70, 1.1, false);
            case "river": return new Zona("Río", 14, 4.5, 74, 1.0, false);
            case "frozen_river": return new Zona("Río helado", -6, 4, 74, 1.1, true);
            case "mushroom_fields": return new Zona("Isla de champiñones", 16, 3, 82, 1.2, false);
            case "lush_caves": return new Zona("Cueva frondosa", 17, 0.5, 88, 0, false);
            case "dripstone_caves": return new Zona("Cueva", 13, 0.3, 80, 0, false);
            case "deep_dark": return new Zona("Oscuridad profunda", 10, 0.2, 75, 0, false);
            case "warm_ocean": return new Zona("Mar tropical", 27, 2, 80, 1.0, false);
            case "lukewarm_ocean": return new Zona("Mar templado", 22, 2, 78, 1.05, false);
            case "deep_lukewarm_ocean": return new Zona("Mar templado profundo", 21, 2, 78, 1.05, false);
            case "ocean": return new Zona("Océano", 16, 2, 80, 1.1, false);
            case "deep_ocean": return new Zona("Océano profundo", 14, 2, 80, 1.15, false);
            case "cold_ocean", "deep_cold_ocean": return new Zona("Mar frío", 8, 2, 82, 1.2, true);
            case "frozen_ocean", "deep_frozen_ocean": return new Zona("Mar helado", -4, 2, 80, 1.15, true);
            default: break;
        }
        // biomas de otros mods: según su temperatura y etiquetas
        float bt = b.value().getBaseTemperature();
        double t = 20 * Math.min(bt, 1.6) - 2;
        double h = 50, amp = 6, nub = 1.0;
        if (b.is(BiomeTags.IS_JUNGLE) || b.is(Tags.Biomes.IS_SWAMP)) {
            h = 84; amp = 4; nub = 1.3;
        } else if (b.is(Tags.Biomes.IS_WET)) {
            h = 72; amp = 4.5; nub = 1.15;
        } else if (b.is(Tags.Biomes.IS_DRY) || !b.value().hasPrecipitation()) {
            h = 20; amp = 9; nub = 0.3;
        }
        if (b.is(BiomeTags.IS_OCEAN)) {
            amp = 2; h = 80;
        }
        return new Zona("Zona de clima " + (t < 0 ? "frío" : t < 12 ? "fresco" : t < 22 ? "templado" : "cálido"), t, amp, h, nub, t < 5);
    }
}

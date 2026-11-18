package es.guerramod.bloques.hogar;

import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;

/** Propiedades de bloque según el material. */
public final class Fabrica {
    private Fabrica() {}

    public static BlockBehaviour.Properties props(String material) {
        BlockBehaviour.Properties p = BlockBehaviour.Properties.of();
        return switch (material) {
            case "madera" -> p.mapColor(MapColor.WOOD).strength(2.0f, 3.0f).sound(SoundType.WOOD).ignitedByLava();
            case "tela" -> p.mapColor(MapColor.WOOL).strength(0.8f).sound(SoundType.WOOL).ignitedByLava();
            case "metal" -> p.mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL);
            case "ceramica" -> p.mapColor(MapColor.QUARTZ).strength(1.5f, 4.0f).sound(SoundType.STONE);
            case "cristal" -> p.mapColor(MapColor.NONE).strength(0.6f).sound(SoundType.GLASS);
            case "plastico" -> p.mapColor(MapColor.SNOW).strength(1.0f, 2.0f).sound(SoundType.BAMBOO_WOOD);
            case "cesped" -> p.mapColor(MapColor.GRASS).strength(0.8f).sound(SoundType.GRASS);
            case "hojas" -> p.mapColor(MapColor.PLANT).strength(0.3f).sound(SoundType.GRASS).noOcclusion()
                    .isSuffocating((s, l, q) -> false).isViewBlocking((s, l, q) -> false);
            case "asfalto" -> p.mapColor(MapColor.COLOR_BLACK).strength(1.8f, 6.0f).sound(SoundType.STONE);
            case "hormigon" -> p.mapColor(MapColor.STONE).strength(3.0f, 9.0f).sound(SoundType.STONE);
            case "ladrillo" -> p.mapColor(MapColor.COLOR_RED).strength(2.0f, 6.0f).sound(SoundType.STONE);
            case "yeso" -> p.mapColor(MapColor.SNOW).strength(1.2f, 3.0f).sound(SoundType.CALCITE);
            default -> p.mapColor(MapColor.STONE).strength(2.0f, 6.0f).sound(SoundType.STONE);
        };
    }

    /** Para muebles y objetos que no son cubos enteros. */
    public static BlockBehaviour.Properties mueble(String material) {
        return props(material).noOcclusion().isRedstoneConductor((s, l, q) -> false)
                .isSuffocating((s, l, q) -> false).isViewBlocking((s, l, q) -> false);
    }

    /** Da luz cuando el bloque está encendido. */
    public static BlockBehaviour.Properties luz(BlockBehaviour.Properties p, int nivel) {
        return p.lightLevel(s -> s.hasProperty(BlockStateProperties.LIT) && s.getValue(BlockStateProperties.LIT) ? nivel : 0);
    }

    /** Da siempre la misma luz. */
    public static BlockBehaviour.Properties luzFija(BlockBehaviour.Properties p, int nivel) {
        return p.lightLevel(s -> nivel);
    }
}

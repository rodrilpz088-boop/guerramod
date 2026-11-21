package es.guerramod.vivienda;

import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/** Vivienda moderna: ascensores, escaleras mecánicas y su panel de botones. */
public final class BloquesVivienda {
    private BloquesVivienda() {}

    private static final Categoria C = Categoria.VIVIENDA;

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL);
    }

    public static final RegistryObject<Block> ASCENSOR = ModBloques.reg("ascensor", C,
            () -> new AscensorBlock(metal().lightLevel(s -> 6)));
    public static final RegistryObject<Block> ESCALERA_MECANICA = ModBloques.reg("escalera_mecanica", C,
            () -> new EscaleraMecanicaBlock(metal().noOcclusion(), false));
    public static final RegistryObject<Block> ESCALERA_MECANICA_BAJADA = ModBloques.reg("escalera_mecanica_bajada", C,
            () -> new EscaleraMecanicaBlock(metal().noOcclusion(), true));
    public static final RegistryObject<Block> PANEL_ASCENSOR = ModBloques.reg("panel_ascensor", C,
            () -> new PlacaBlock(metal().noOcclusion().lightLevel(s -> 4), 1, 2, 14, false));

    public static void iniciar() {
    }
}

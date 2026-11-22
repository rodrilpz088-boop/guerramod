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

    public static final RegistryObject<Block> TERMOSTATO = ModBloques.reg("termostato", C,
            () -> new TermostatoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(0.5f).sound(SoundType.STONE).noOcclusion()
                    .lightLevel(s -> 3)));
    public static final RegistryObject<Block> TERMOMETRO_EXTERIOR = ModBloques.reg("termometro_exterior", C,
            () -> new Termometros.Exterior(metal().noOcclusion()));
    public static final RegistryObject<net.minecraft.world.item.Item> TERMOMETRO = registrarItem();

    private static RegistryObject<net.minecraft.world.item.Item> registrarItem() {
        RegistryObject<net.minecraft.world.item.Item> r = es.guerramod.registro.ModItems.ITEMS.register("termometro_digital",
                () -> new Termometros.Digital(new net.minecraft.world.item.Item.Properties().stacksTo(1)));
        C.lista.add(r);
        return r;
    }

    public static void iniciar() {
    }
}

package es.guerramod.ciudad;

import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import es.guerramod.registro.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

/** Ciudad e industria: planos de construcción, chimeneas que emiten CO2 y captura de CO2. */
public final class BloquesCiudad {
    private BloquesCiudad() {}

    private static final Categoria C = Categoria.CIUDAD;

    public static final RegistryObject<Item> PLANOS = registrarItem();

    private static RegistryObject<Item> registrarItem() {
        RegistryObject<Item> r = ModItems.ITEMS.register("planos_construccion", () -> new PlanosItem(new Item.Properties().durability(12)));
        C.lista.add(r);
        return r;
    }

    private static BlockBehaviour.Properties chimenea(MapColor c) {
        return BlockBehaviour.Properties.of().mapColor(c).strength(3.0f, 8.0f).sound(SoundType.STONE).requiresCorrectToolForDrops()
                .randomTicks().lightLevel(s -> s.getValue(BlockStateProperties.LIT) ? 3 : 0);
    }

    public static final RegistryObject<Block> CHIMENEA_INDUSTRIAL = ModBloques.reg("chimenea_industrial", C,
            () -> new ChimeneaBlock(chimenea(MapColor.COLOR_RED), 1.0, false));
    public static final RegistryObject<Block> CHIMENEA_CENTRAL = ModBloques.reg("chimenea_central", C,
            () -> new ChimeneaBlock(chimenea(MapColor.SNOW), 4.0, false));
    public static final RegistryObject<Block> CHIMENEA_VAPOR = ModBloques.reg("chimenea_vapor", C,
            () -> new ChimeneaBlock(chimenea(MapColor.SNOW), 0.0, true));
    public static final RegistryObject<Block> CAPTADOR_CO2 = ModBloques.reg("captador_co2", C,
            () -> new CaptadorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).requiresCorrectToolForDrops().randomTicks()));

    /** Fuerza la carga de esta clase (registra todo). */
    public static void iniciar() {
    }
}

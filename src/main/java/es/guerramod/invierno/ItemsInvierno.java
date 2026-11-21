package es.guerramod.invierno;

import es.guerramod.items.MaterialArmadura;
import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import es.guerramod.registro.ModItems;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** Cosas para la nieve: pala, sal, raquetas, esquís, trineo, cañón de nieve, ladrillos de hielo y nieve pisada. */
public final class ItemsInvierno {
    private ItemsInvierno() {}

    private static final Categoria C = Categoria.INVIERNO;

    private static <T extends Item> RegistryObject<T> item(String n, Supplier<T> s) {
        RegistryObject<T> r = ModItems.ITEMS.register(n, s);
        C.lista.add(r);
        return r;
    }

    public static final RegistryObject<Item> PALA_NIEVE = item("pala_nieve", () -> new PalaNieveItem(new Item.Properties().durability(400)));
    public static final RegistryObject<Item> SAL = item("sal_carretera", () -> new SalItem(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> RAQUETAS = item("raquetas_nieve",
            () -> new ArmorItem(MaterialArmadura.RAQUETAS, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> ESQUIS = item("esquis",
            () -> new ArmorItem(MaterialArmadura.ESQUIS, ArmorItem.Type.BOOTS, new Item.Properties()));
    public static final RegistryObject<Item> TRINEO = item("trineo", () -> new TrineoItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Block> CANON_NIEVE = ModBloques.reg("canon_nieve", C,
            () -> new CanonNieveBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> LADRILLO_HIELO = ModBloques.reg("ladrillo_hielo", C,
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.ICE).strength(1.2f).sound(SoundType.GLASS).friction(0.98f)));
    public static final RegistryObject<Block> NIEVE_PISADA = ModBloques.reg("nieve_pisada", C,
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SNOW).strength(0.6f).sound(SoundType.SNOW).friction(0.8f)));

    public static void iniciar() {
    }
}

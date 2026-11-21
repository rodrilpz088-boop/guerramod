package es.guerramod.policia;

import es.guerramod.armas.Armas;
import es.guerramod.items.ArmaItem;
import es.guerramod.items.MaterialArmadura;
import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import es.guerramod.registro.ModItems;
import es.guerramod.vivienda.PlacaBlock;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/**
 * Policía Nacional: uniforme de patrulla, equipo del GEO (grupo especial de operaciones), placa, esposas, defensa
 * (porra), escudo antidisturbios, pistola reglamentaria y subfusil del GEO, cinta policial, luz de emergencia, rótulo
 * de comisaría, valla y coche patrulla.
 */
public final class ItemsPolicia {
    private ItemsPolicia() {}

    private static final Categoria C = Categoria.POLICIA;

    private static <T extends Item> RegistryObject<T> item(String n, Supplier<T> s) {
        RegistryObject<T> r = ModItems.ITEMS.register(n, s);
        C.lista.add(r);
        return r;
    }

    private static RegistryObject<Item> armadura(String n, MaterialArmadura m, ArmorItem.Type t) {
        return item(n, () -> new ArmorItem(m, t, new Item.Properties()));
    }

    // uniforme de patrulla
    public static final RegistryObject<Item> GORRA = armadura("gorra_policia", MaterialArmadura.POLICIA, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHAQUETA = armadura("chaqueta_policia", MaterialArmadura.POLICIA, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PANTALON = armadura("pantalon_policia", MaterialArmadura.POLICIA, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOTAS = armadura("botas_policia", MaterialArmadura.POLICIA, ArmorItem.Type.BOOTS);
    // GEO
    public static final RegistryObject<Item> CASCO_GEO = armadura("casco_geo", MaterialArmadura.GEO, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHALECO_GEO = armadura("chaleco_geo", MaterialArmadura.GEO, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PANTALON_GEO = armadura("pantalon_geo", MaterialArmadura.GEO, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOTAS_GEO = armadura("botas_geo", MaterialArmadura.GEO, ArmorItem.Type.BOOTS);

    public static final RegistryObject<Item> PLACA = item("placa_policial", () -> new PlacaPolicialItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> ESPOSAS = item("esposas", () -> new EsposasItem(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> PORRA = item("porra_policial", () -> new PorraItem(Tiers.IRON, 2, -2.0f, new Item.Properties()));
    public static final RegistryObject<Item> ESCUDO = item("escudo_antidisturbios",
            () -> new net.minecraft.world.item.ShieldItem(new Item.Properties().durability(500)));

    public static final RegistryObject<Item> PISTOLA = item("pistola_policia", () -> new ArmaItem(Armas.PISTOLA));
    public static final RegistryObject<Item> SUBFUSIL = item("subfusil_geo", () -> new ArmaItem(Armas.SUBFUSIL));

    private static BlockBehaviour.Properties metal() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(2.0f, 6.0f).sound(SoundType.METAL).noOcclusion();
    }

    public static final RegistryObject<Block> CINTA = ModBloques.reg("cinta_policial", C,
            () -> new PlacaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(0.2f).sound(SoundType.WOOL).noOcclusion()
                    .noCollission(), 1, 6, 12, false));
    public static final RegistryObject<Block> LUZ = ModBloques.reg("luz_emergencia", C,
            () -> new LuzEmergenciaBlock(metal().lightLevel(s -> 12)));
    public static final RegistryObject<Block> ROTULO = ModBloques.reg("rotulo_comisaria", C,
            () -> new PlacaBlock(metal().lightLevel(s -> 10), 1, 3, 13, false));
    public static final RegistryObject<Block> VALLA = ModBloques.reg("valla_policial", C,
            () -> new VallaPolicialBlock(metal()));
    public static final RegistryObject<Block> COCHE = ModBloques.reg("coche_patrulla", C,
            () -> new CochePatrullaBlock(metal().lightLevel(s -> 6)));
    public static final RegistryObject<Block> FURGON_GEO = ModBloques.reg("furgon_geo", C,
            () -> new CochePatrullaBlock(metal().lightLevel(s -> 4)));

    public static void iniciar() {
    }
}

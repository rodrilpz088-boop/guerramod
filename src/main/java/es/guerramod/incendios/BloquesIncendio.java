package es.guerramod.incendios;

import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import es.guerramod.registro.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

/** Todo lo del apartado de incendios: fuego realista, bloques quemados y material de bomberos. */
public final class BloquesIncendio {
    private BloquesIncendio() {}

    private static final Categoria C = Categoria.INCENDIOS;

    private static BlockBehaviour.Properties p() {
        return BlockBehaviour.Properties.of();
    }

    private static <T extends Block> RegistryObject<T> sinItem(String n, Supplier<T> s) {
        return ModBloques.BLOQUES.register(n, s);
    }

    private static <T extends Item> RegistryObject<T> item(String n, Supplier<T> s) {
        RegistryObject<T> r = ModItems.ITEMS.register(n, s);
        C.lista.add(r);
        return r;
    }

    // ------------------------------------------------------------------ bomberos (objetos)
    public static final RegistryObject<Item> MANGUERA = item("manguera", () -> new MangueraItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> EXTINTOR = item("extintor", () -> new ExtintorItem(new Item.Properties().durability(300)));
    public static final RegistryObject<Item> BATEFUEGOS = item("batefuegos",
            () -> new HerramientasForestales.Batefuegos(new Item.Properties().durability(250)));
    public static final RegistryObject<Item> RASTRILLO = item("rastrillo",
            () -> new HerramientasForestales.Rastrillo(new Item.Properties().durability(400)));
    public static final RegistryObject<Item> ANTORCHA_GOTEO = item("antorcha_goteo",
            () -> new HerramientasForestales.AntorchaGoteo(new Item.Properties().durability(64)));
    public static final RegistryObject<Item> ANEMOMETRO = item("anemometro", () -> new AnemometroItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RADIO_HIDROAVION = item("radio_hidroavion",
            () -> new RadioHidroavionItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> MOCHILA_EXTINTORA = item("mochila_extintora",
            () -> new MochilaExtintoraItem(new Item.Properties()));
    public static final RegistryObject<Item> CASCO_BOMBERO = item("casco_bombero",
            () -> new ArmorItem(es.guerramod.items.MaterialArmadura.BOMBERO, ArmorItem.Type.HELMET, new Item.Properties()));
    public static final RegistryObject<Item> CHAQUETA_BOMBERO = item("chaqueta_bombero",
            () -> new ArmorItem(es.guerramod.items.MaterialArmadura.BOMBERO, ArmorItem.Type.CHESTPLATE, new Item.Properties()));
    public static final RegistryObject<Item> PANTALON_BOMBERO = item("pantalon_bombero",
            () -> new ArmorItem(es.guerramod.items.MaterialArmadura.BOMBERO, ArmorItem.Type.LEGGINGS, new Item.Properties()));
    public static final RegistryObject<Item> BOTAS_BOMBERO = item("botas_bombero",
            () -> new ArmorItem(es.guerramod.items.MaterialArmadura.BOMBERO, ArmorItem.Type.BOOTS, new Item.Properties()));

    // ------------------------------------------------------------------ fuego
    public static final RegistryObject<Block> LLAMA = sinItem("llama", () -> new LlamaBlock(p().mapColor(MapColor.FIRE).replaceable()
            .noCollission().instabreak().lightLevel(s -> 15).sound(SoundType.WOOL).pushReaction(PushReaction.DESTROY).noLootTable()));
    public static final RegistryObject<Block> TRONCO_ARDIENDO = sinItem("tronco_ardiendo", () -> new TroncoArdiendoBlock(
            p().mapColor(MapColor.FIRE).strength(2.0f).sound(SoundType.WOOD).lightLevel(s -> 12)));
    public static final RegistryObject<Block> RESCOLDO = sinItem("rescoldo", () -> new RescoldoBlock(p().mapColor(MapColor.COLOR_BLACK)
            .strength(0.3f).sound(SoundType.SAND).lightLevel(s -> 7).noOcclusion().noLootTable().randomTicks()));

    // ------------------------------------------------------------------ restos del incendio
    public static final RegistryObject<Block> TIERRA_QUEMADA = ModBloques.reg("tierra_quemada", C, () -> new TierraQuemadaBlock(
            p().mapColor(MapColor.COLOR_BLACK).strength(0.6f).sound(SoundType.ROOTED_DIRT).randomTicks()));
    public static final RegistryObject<Block> CENIZA = ModBloques.reg("ceniza", C, () -> new CenizaBlock(p().mapColor(MapColor.COLOR_GRAY)
            .strength(0.1f).sound(SoundType.SAND).replaceable().noOcclusion().randomTicks().pushReaction(PushReaction.DESTROY)));
    public static final RegistryObject<Block> TRONCO_QUEMADO = ModBloques.reg("tronco_quemado", C, () -> new RotatedPillarBlock(
            p().mapColor(MapColor.COLOR_BLACK).strength(1.5f).sound(SoundType.STEM)));
    public static final RegistryObject<Block> MADERA_QUEMADA = ModBloques.reg("madera_quemada", C, () -> new Block(
            p().mapColor(MapColor.COLOR_BLACK).strength(1.2f).sound(SoundType.STEM)));
    public static final RegistryObject<Block> RAMAS_QUEMADAS = ModBloques.reg("ramas_quemadas", C, () -> new RamasQuemadasBlock(
            p().mapColor(MapColor.COLOR_BLACK).strength(0.2f).sound(SoundType.AZALEA_LEAVES).noOcclusion().randomTicks()
                    .isSuffocating((s, l, q) -> false).isViewBlocking((s, l, q) -> false)));
    public static final RegistryObject<Block> TIERRA_RASTRILLADA = ModBloques.reg("tierra_rastrillada", C, () -> new Block(
            p().mapColor(MapColor.DIRT).strength(0.5f).sound(SoundType.GRAVEL)));
    public static final RegistryObject<Block> PASTO_SECO = ModBloques.reg("pasto_seco", C, () -> new PastoSecoBlock(
            p().mapColor(MapColor.COLOR_YELLOW).replaceable().noCollission().instabreak().sound(SoundType.GRASS)
                    .offsetType(BlockBehaviour.OffsetType.XZ).ignitedByLava().pushReaction(PushReaction.DESTROY)) {
        @Override
        public int getFlammability(BlockState st, BlockGetter g, BlockPos pos, Direction d) {
            return 100;
        }

        @Override
        public int getFireSpreadSpeed(BlockState st, BlockGetter g, BlockPos pos, Direction d) {
            return 60;
        }
    });

    // ------------------------------------------------------------------ bomberos (bloques)
    public static final RegistryObject<Block> BOCA_INCENDIO = ModBloques.reg("boca_incendio", C, () -> new BocaIncendioBlock(
            p().mapColor(MapColor.COLOR_RED).strength(5.0f, 10.0f).sound(SoundType.METAL).noOcclusion(),
            new double[][]{{5, 0, 5, 11, 13, 11}, {3.5, 7, 6.5, 12.5, 9.5, 9.5}, {6, 13, 6, 10, 14.5, 10}}, false));
    public static final RegistryObject<Block> BIE = ModBloques.reg("bie", C, () -> new BocaIncendioBlock(
            p().mapColor(MapColor.COLOR_RED).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion(),
            new double[][]{{1, 1, 11, 15, 15, 16}}, true));
    public static final RegistryObject<Block> EXTINTOR_PARED = ModBloques.reg("extintor_pared", C, () -> new ExtintorParedBlock(
            p().mapColor(MapColor.COLOR_RED).strength(1.0f).sound(SoundType.METAL).noOcclusion(),
            new double[][]{{6, 2, 11, 10, 12, 15}, {5.5, 12, 11.5, 10.5, 14, 14.5}}));
    public static final RegistryObject<Block> DETECTOR_HUMO = ModBloques.reg("detector_humo", C, () -> new DetectorHumoBlock(
            p().mapColor(MapColor.SNOW).strength(0.8f).sound(SoundType.STONE).noOcclusion(), false));
    public static final RegistryObject<Block> ROCIADOR = ModBloques.reg("rociador", C, () -> new DetectorHumoBlock(
            p().mapColor(MapColor.METAL).strength(1.5f).sound(SoundType.METAL).noOcclusion(), true));

    /** Fuerza la carga de esta clase (registra todo). */
    public static void iniciar() {
    }

    /** Piezas del equipo de bombero que lleva puestas (la mochila cuenta como chaqueta). */
    public static int piezasBombero(net.minecraft.world.entity.LivingEntity e) {
        int n = 0;
        for (net.minecraft.world.item.ItemStack s : e.getArmorSlots()) {
            if (s.getItem() instanceof ArmorItem a && a.getMaterial() == es.guerramod.items.MaterialArmadura.BOMBERO) n++;
        }
        return n;
    }
}

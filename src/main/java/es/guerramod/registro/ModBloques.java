package es.guerramod.registro;

import es.guerramod.GuerraMod;
import es.guerramod.bloques.*;
import net.minecraft.world.item.BedItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DoubleHighBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public final class ModBloques {
    public static final DeferredRegister<Block> BLOQUES = DeferredRegister.create(ForgeRegistries.BLOCKS, GuerraMod.MODID);

    /** Bloques con inventario (para el tipo de block entity) y luces LED (para el color). */
    public static final List<RegistryObject<? extends Block>> ALMACENES = new ArrayList<>();
    public static final List<RegistryObject<? extends Block>> LEDS = new ArrayList<>();

    private static final Categoria FRONTERAS = Categoria.NACIONES;
    private static final Categoria CARRETERAS = Categoria.CARRETERAS;
    private static final Categoria GUERRA = Categoria.MILITAR;
    private static final Categoria GOBIERNO = Categoria.NACIONES;

    private static BlockBehaviour.Properties hormigon() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.STONE).strength(6.0f, 9.0f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties asfalto() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(1.8f, 6.0f)
                .sound(SoundType.STONE).speedFactor(1.25f);
    }

    // ---------------- Fronteras y carreteras ----------------
    public static final RegistryObject<Block> HITO_FRONTERA = registrar("hito_frontera", FRONTERAS,
            () -> new BloquesVarios.BloqueForma(hormigon().noOcclusion().lightLevel(s -> 7), 5, 0, 5, 11, 14, 11));
    public static final RegistryObject<Block> VALLA_FRONTERIZA = registrar("valla_fronteriza", FRONTERAS,
            () -> new FenceBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 8.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> MURO_FRONTERIZO = registrar("muro_fronterizo", FRONTERAS,
            () -> new WallBlock(hormigon()));
    public static final RegistryObject<Block> BARRERA_CONTROL = registrar("barrera_control", FRONTERAS,
            () -> new BarreraControlBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(8.0f, 12.0f)
                    .sound(SoundType.METAL).noOcclusion().isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final RegistryObject<Block> PUESTO_CONTROL = registrar("puesto_control", FRONTERAS,
            () -> new BloquesVarios.PuestoControl(hormigon()));
    public static final RegistryObject<Block> ALAMBRE_ESPINO = registrar("alambre_espino", FRONTERAS,
            () -> new BloquesVarios.AlambreEspino(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 4.0f)
                    .sound(SoundType.CHAIN).noCollission().noOcclusion()));
    public static final RegistryObject<Block> ASFALTO = registrar("asfalto", CARRETERAS, () -> new Block(asfalto()));
    public static final RegistryObject<Block> ASFALTO_LOSA = registrar("asfalto_losa", CARRETERAS, () -> new SlabBlock(asfalto()));
    public static final RegistryObject<Block> ASFALTO_LINEA = registrar("asfalto_linea", CARRETERAS,
            () -> new BloquesVarios.BloqueOrientable(asfalto(), false));
    public static final RegistryObject<Block> ASFALTO_LINEA_CONTINUA = registrar("asfalto_linea_continua", CARRETERAS,
            () -> new BloquesVarios.BloqueOrientable(asfalto(), false));
    public static final RegistryObject<Block> PASO_CEBRA = registrar("paso_cebra", CARRETERAS,
            () -> new BloquesVarios.BloqueOrientable(asfalto(), false));
    public static final RegistryObject<Block> FAROLA = registrar("farola", CARRETERAS,
            () -> new BloquesVarios.BloqueForma(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f)
                    .sound(SoundType.METAL).noOcclusion().lightLevel(s -> 15), 6, 0, 6, 10, 16, 10));

    // ---------------- Guerra y búnker ----------------
    public static final RegistryObject<Block> HORMIGON_BLINDADO = registrar("hormigon_blindado", GUERRA, () -> new Block(hormigon()));
    public static final RegistryObject<Block> CRISTAL_BLINDADO = registrar("cristal_blindado", GUERRA,
            () -> new GlassBlock(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).strength(5.0f, 9.0f)
                    .sound(SoundType.GLASS).noOcclusion().isValidSpawn((s, l, p, e) -> false)
                    .isRedstoneConductor((s, l, p) -> false).isSuffocating((s, l, p) -> false).isViewBlocking((s, l, p) -> false)));
    public static final RegistryObject<Block> SACOS_TERREROS = registrar("sacos_terreros", GUERRA,
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.SAND).strength(2.5f, 12.0f).sound(SoundType.WOOL)));
    public static final RegistryObject<Block> HORMIGON_REFORZADO = registrar("hormigon_reforzado", GUERRA,
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GRAY).strength(30.0f, 1200.0f)
                    .sound(SoundType.STONE).pushReaction(PushReaction.BLOCK)));
    public static final RegistryObject<Block> BLOQUE_PLOMO = registrar("bloque_plomo", GUERRA,
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_LIGHT_GRAY).strength(8.0f, 60.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> PUERTA_BUNKER = registrarPuerta("puerta_bunker",
            () -> new DoorBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(25.0f, 1200.0f)
                    .sound(SoundType.METAL).noOcclusion().pushReaction(PushReaction.BLOCK), BlockSetType.IRON));
    public static final RegistryObject<Block> PURIFICADOR_AIRE = registrar("purificador_aire", GUERRA,
            () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(5.0f, 30.0f)
                    .sound(SoundType.METAL).lightLevel(s -> 8)));
    public static final RegistryObject<Block> MINA_TERRESTRE = registrar("mina_terrestre", GUERRA,
            () -> new BloquesVarios.Mina(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(0.5f)
                    .sound(SoundType.METAL).noCollission().noOcclusion()));
    public static final RegistryObject<Block> SIRENA = registrar("sirena", GUERRA,
            () -> new BloquesVarios.Sirena(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 6.0f).sound(SoundType.METAL)));
    public static final RegistryObject<Block> RADAR = registrar("radar", GUERRA,
            () -> new BloquesVarios.Radar(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(4.0f, 6.0f)
                    .sound(SoundType.METAL).lightLevel(s -> 5)));
    public static final RegistryObject<Block> LANZADOR_MISILES = registrar("lanzador_misiles", GUERRA,
            () -> new BloquesVarios.LanzadorMisiles(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN).strength(10.0f, 30.0f)
                    .sound(SoundType.METAL)));
    public static final RegistryObject<Block> BOMBA_NUCLEAR = registrar("bomba_nuclear", GUERRA,
            () -> new BombaNuclearBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_YELLOW).strength(3.0f, 3.0f)
                    .sound(SoundType.METAL)));

    // ---------------- Gobierno ----------------
    public static final RegistryObject<Block> MESA_GOBIERNO = registrar("mesa_gobierno", GOBIERNO,
            () -> new BloquesVarios.MesaGobierno(BlockBehaviour.Properties.of().mapColor(MapColor.WOOD).strength(2.5f).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> URNA_ELECTORAL = registrar("urna_electoral", GOBIERNO,
            () -> new BloquesVarios.Urna(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.5f).sound(SoundType.WOOD).noOcclusion()));

    /** Banderas reales: id -> nombre en español. */
    public static final Map<String, String> NOMBRES_BANDERAS = new LinkedHashMap<>();
    public static final List<RegistryObject<? extends Block>> BANDERAS = new ArrayList<>();

    static {
        String[][] banderas = {
                {"espana", "España"}, {"francia", "Francia"}, {"alemania", "Alemania"}, {"italia", "Italia"},
                {"portugal", "Portugal"}, {"reino_unido", "Reino Unido"}, {"estados_unidos", "Estados Unidos"},
                {"mexico", "México"}, {"argentina", "Argentina"}, {"brasil", "Brasil"}, {"japon", "Japón"},
                {"ucrania", "Ucrania"}, {"rusia", "Rusia"}, {"china", "China"}, {"canada", "Canadá"},
                {"polonia", "Polonia"}, {"paises_bajos", "Países Bajos"}, {"belgica", "Bélgica"},
                {"suecia", "Suecia"}, {"grecia", "Grecia"}, {"colombia", "Colombia"}, {"irlanda", "Irlanda"}
        };
        for (String[] b : banderas) {
            NOMBRES_BANDERAS.put(b[0], b[1]);
            BANDERAS.add(registrar("bandera_" + b[0], GOBIERNO,
                    () -> new BanderaBlock(BlockBehaviour.Properties.of().mapColor(MapColor.WOOL).strength(1.0f)
                            .sound(SoundType.WOOL).noOcclusion())));
        }
        CatalogoGenerado.registrar();
        es.guerramod.incendios.BloquesIncendio.iniciar();
        es.guerramod.ciudad.BloquesCiudad.iniciar();
    }

    private ModBloques() {}

    private static <T extends Block> RegistryObject<T> registrar(String nombre, Categoria cat, Supplier<T> s) {
        return reg(nombre, cat, s);
    }

    private static <T extends Block> RegistryObject<T> registrarPuerta(String nombre, Supplier<T> s) {
        return regAlto(nombre, GUERRA, s);
    }

    /** Bloque normal con su item. */
    public static <T extends Block> RegistryObject<T> reg(String nombre, Categoria cat, Supplier<T> s) {
        RegistryObject<T> bloque = BLOQUES.register(nombre, s);
        ModItems.ITEMS.register(nombre, () -> new BlockItem(bloque.get(), new Item.Properties()));
        cat.lista.add(bloque);
        return bloque;
    }

    /** Bloque de dos alturas (puertas, frigoríficos, armarios). */
    public static <T extends Block> RegistryObject<T> regAlto(String nombre, Categoria cat, Supplier<T> s) {
        RegistryObject<T> bloque = BLOQUES.register(nombre, s);
        ModItems.ITEMS.register(nombre, () -> new DoubleHighBlockItem(bloque.get(), new Item.Properties()));
        cat.lista.add(bloque);
        return bloque;
    }

    /** Camas. */
    public static <T extends Block> RegistryObject<T> regCama(String nombre, Categoria cat, Supplier<T> s) {
        RegistryObject<T> bloque = BLOQUES.register(nombre, s);
        ModItems.ITEMS.register(nombre, () -> new BedItem(bloque.get(), new Item.Properties().stacksTo(1)));
        cat.lista.add(bloque);
        return bloque;
    }
}

package es.guerramod.metro;

import es.guerramod.bloques.BloquesVarios;
import es.guerramod.registro.Categoria;
import es.guerramod.registro.ModBloques;
import es.guerramod.registro.ModItems;
import es.guerramod.vivienda.PlacaBlock;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.LinkedHashMap;
import java.util.Map;

/** Metro ambientado en Madrid: azulejos, andenes, carteles de estación y de línea, tornos, máquinas de billetes y trenes. */
public final class BloquesMetro {
    private BloquesMetro() {}

    private static final Categoria C = Categoria.METRO;

    /** id -> nombre de la estación (para los carteles y los avisos del tren). */
    public static final Map<String, String> ESTACIONES = new LinkedHashMap<>();
    /** Colores de las 12 líneas. */
    public static final int[] COLOR_LINEA = {0x38A3DC, 0xE0001A, 0xFFD100, 0xB06B2E, 0x8EBE3F, 0x9A9A9A, 0xF3962B, 0xEA8FB8,
            0x9C3E95, 0x1B3C8F, 0x2E9E47, 0xA49A00};

    static {
        String[] n = {"Sol", "Gran Vía", "Callao", "Ópera", "Atocha", "Chamartín", "Nuevos Ministerios", "Plaza de Castilla", "Moncloa",
                "Príncipe Pío", "Retiro", "Banco de España", "Tribunal", "Argüelles", "Cuatro Caminos", "Legazpi", "Ventas", "Lavapiés",
                "Bilbao", "Tirso de Molina"};
        for (String s : n) ESTACIONES.put(id(s), s);
    }

    public static String id(String nombre) {
        String s = java.text.Normalizer.normalize(nombre.toLowerCase(), java.text.Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return s.replaceAll("[^a-z0-9]+", "_").replaceAll("^_|_$", "");
    }

    private static BlockBehaviour.Properties azulejo() {
        return BlockBehaviour.Properties.of().mapColor(MapColor.QUARTZ).strength(1.5f, 6.0f).sound(SoundType.STONE);
    }

    private static BlockBehaviour.Properties placa(int luz) {
        return BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLUE).strength(1.0f, 3.0f).sound(SoundType.METAL).noOcclusion()
                .lightLevel(s -> luz);
    }

    public static final RegistryObject<Block> AZULEJO_METRO = ModBloques.reg("azulejo_metro", C, () -> new Block(azulejo()));
    public static final RegistryObject<Block> AZULEJO_METRO_AZUL = ModBloques.reg("azulejo_metro_azul", C, () -> new Block(azulejo()));
    public static final RegistryObject<Block> AZULEJO_METRO_ROJO = ModBloques.reg("azulejo_metro_rojo", C, () -> new Block(azulejo()));
    public static final RegistryObject<Block> AZULEJO_METRO_VERDE = ModBloques.reg("azulejo_metro_verde", C, () -> new Block(azulejo()));
    public static final RegistryObject<Block> SUELO_ANDEN = ModBloques.reg("suelo_anden", C, () -> new Block(azulejo()));
    public static final RegistryObject<Block> BORDE_ANDEN = ModBloques.reg("borde_anden", C,
            () -> new BloquesVarios.BloqueOrientable(azulejo(), false));
    public static final RegistryObject<Block> SENAL_PARADA = ModBloques.reg("senal_parada", C, () -> new Block(azulejo()));
    public static final RegistryObject<Block> TORNO_METRO = ModBloques.reg("torno_metro", C,
            () -> new TornoBlock(BlockBehaviour.Properties.of().mapColor(MapColor.METAL).strength(3.0f, 6.0f).sound(SoundType.METAL).noOcclusion()));
    public static final RegistryObject<Block> MAQUINA_BILLETES = ModBloques.reg("maquina_billetes", C,
            () -> new MaquinaBilletesBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_RED).strength(3.0f, 6.0f).sound(SoundType.METAL)
                    .noOcclusion().lightLevel(s -> 5)));
    public static final RegistryObject<Block> BOCA_METRO = ModBloques.reg("boca_metro", C,
            () -> new PlacaBlock(placa(10), 4, 0, 16, true));
    public static final RegistryObject<Block> PANEL_TRENES = ModBloques.reg("panel_trenes", C, () -> new PlacaBlock(placa(6), 2, 2, 14, false));
    public static final RegistryObject<Block> RELOJ_ANDEN = ModBloques.reg("reloj_anden", C, () -> new PlacaBlock(placa(3), 2, 2, 14, false));

    public static final RegistryObject<Item> BILLETE = registrarItem("billete_metro", () -> new Item(new Item.Properties().stacksTo(64)));
    public static final RegistryObject<Item> VAGON = registrarItem("vagon_metro", () -> new VagonItem(new Item.Properties().stacksTo(8)));

    private static RegistryObject<Item> registrarItem(String n, java.util.function.Supplier<Item> s) {
        RegistryObject<Item> r = ModItems.ITEMS.register(n, s);
        C.lista.add(r);
        return r;
    }

    static {
        for (String id : ESTACIONES.keySet()) {
            ModBloques.reg("placa_estacion_" + id, C, () -> new PlacaBlock(placa(6), 1, 4, 12, false));
        }
        for (int i = 1; i <= 12; i++) {
            ModBloques.reg("cartel_linea_" + i, C, () -> new PlacaBlock(placa(4), 1, 2, 14, false));
        }
    }

    /** Modelo del vagón (sin objeto: solo lo dibuja el tren). */
    public static final RegistryObject<Block> MODELO_VAGON = ModBloques.BLOQUES.register("modelo_vagon",
            () -> new Block(BlockBehaviour.Properties.of().noOcclusion().noLootTable()));

    /** Nombre de estación de un cartel ("" si no lo es). */
    public static String estacionDe(Block b) {
        var k = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(b);
        if (k == null || !k.getPath().startsWith("placa_estacion_")) return "";
        return ESTACIONES.getOrDefault(k.getPath().substring("placa_estacion_".length()), "");
    }

    public static void iniciar() {
    }
}

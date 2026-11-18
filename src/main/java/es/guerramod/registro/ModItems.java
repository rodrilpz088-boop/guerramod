package es.guerramod.registro;

import es.guerramod.GuerraMod;
import es.guerramod.armas.Armas;
import es.guerramod.items.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GuerraMod.MODID);

    private static final Categoria A = Categoria.ARMAS;
    private static final Categoria M = Categoria.MILITAR;

    // ---------------- Armas de fuego ----------------
    public static final RegistryObject<Item> FUSIL = registrar("fusil", A, () -> new ArmaItem(Armas.FUSIL));
    public static final RegistryObject<Item> PISTOLA = registrar("pistola", A, () -> new ArmaItem(Armas.PISTOLA));
    public static final RegistryObject<Item> SUBFUSIL = registrar("subfusil", A, () -> new ArmaItem(Armas.SUBFUSIL));
    public static final RegistryObject<Item> ESCOPETA = registrar("escopeta", A, () -> new ArmaItem(Armas.ESCOPETA));
    public static final RegistryObject<Item> FRANCOTIRADOR = registrar("francotirador", A, () -> new ArmaItem(Armas.FRANCOTIRADOR));
    public static final RegistryObject<Item> AMETRALLADORA = registrar("ametralladora", A, () -> new ArmaItem(Armas.AMETRALLADORA));
    public static final RegistryObject<Item> LANZACOHETES = registrar("lanzacohetes", A, () -> new ArmaItem(Armas.LANZACOHETES));
    public static final RegistryObject<Item> GRANADA = registrar("granada", A, () -> new GranadaItem(new Item.Properties().stacksTo(16)));

    // ---------------- Munición ----------------
    public static final RegistryObject<Item> MUNICION = simple("municion", A);
    public static final RegistryObject<Item> MUNICION_556 = simple("municion_556", A);
    public static final RegistryObject<Item> MUNICION_762 = simple("municion_762", A);
    public static final RegistryObject<Item> CARTUCHOS = simple("cartuchos", A);
    public static final RegistryObject<Item> COHETE = simple("cohete", A);
    /** Solo para dibujar balas y casquillos (no salen en el creativo). */
    public static final RegistryObject<Item> PROYECTIL_BALA = ITEMS.register("proyectil_bala", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CASQUILLO = ITEMS.register("casquillo", () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> CARTUCHO_VACIO = ITEMS.register("cartucho_vacio", () -> new Item(new Item.Properties()));

    // ---------------- Armaduras ----------------
    public static final RegistryObject<Item> CASCO_MILITAR = armadura("casco_militar", MaterialArmadura.MILITAR, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHALECO_ANTIBALAS = armadura("chaleco_antibalas", MaterialArmadura.MILITAR, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PANTALON_MILITAR = armadura("pantalon_militar", MaterialArmadura.MILITAR, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOTAS_MILITARES = armadura("botas_militares", MaterialArmadura.MILITAR, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> MASCARA_NBQ = armadura("mascara_nbq", MaterialArmadura.NBQ, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> TRAJE_NBQ = armadura("traje_nbq", MaterialArmadura.NBQ, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PANTALON_NBQ = armadura("pantalon_nbq", MaterialArmadura.NBQ, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOTAS_NBQ = armadura("botas_nbq", MaterialArmadura.NBQ, ArmorItem.Type.BOOTS);

    // ---------------- Material militar ----------------
    public static final RegistryObject<Item> PLACA_ACERO = simple("placa_acero", M);
    public static final RegistryObject<Item> CIRCUITO = simple("circuito", M);
    public static final RegistryObject<Item> BOMBA_AEREA = simple("bomba_aerea", M);
    public static final RegistryObject<Item> MISIL_BALISTICO = registrar("misil_balistico", M, () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> MISIL_NUCLEAR = registrar("misil_nuclear", M,
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    public static final RegistryObject<Item> AVION_CAZA = registrar("avion_caza", M, () -> new AvionItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RADIO_BOMBARDEO = registrar("radio_bombardeo", M,
            () -> new RadioBombardeoItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DESIGNADOR_LASER = registrar("designador_laser", M,
            () -> new DesignadorItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> GEIGER = registrar("geiger", M, () -> new GeigerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PASTILLAS_YODO = registrar("pastillas_yodo", M, () -> new YodoItem(new Item.Properties().stacksTo(16)));

    // ---------------- Hogar ----------------
    public static final RegistryObject<Item> MANDO_LED = registrar("mando_led", Categoria.ILUMINACION,
            () -> new MandoLedItem(new Item.Properties().stacksTo(1)));

    private ModItems() {}

    private static <T extends Item> RegistryObject<T> registrar(String nombre, Categoria cat, Supplier<T> s) {
        RegistryObject<T> r = ITEMS.register(nombre, s);
        cat.lista.add(r);
        return r;
    }

    private static RegistryObject<Item> simple(String nombre, Categoria cat) {
        return registrar(nombre, cat, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> armadura(String nombre, MaterialArmadura m, ArmorItem.Type tipo) {
        return registrar(nombre, Categoria.MILITAR, () -> new ArmorItem(m, tipo, new Item.Properties()));
    }
}

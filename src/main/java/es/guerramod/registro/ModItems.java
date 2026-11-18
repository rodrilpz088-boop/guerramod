package es.guerramod.registro;

import es.guerramod.GuerraMod;
import es.guerramod.items.*;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, GuerraMod.MODID);

    /** Items de la pestaña Guerra (en orden). */
    public static final List<RegistryObject<? extends Item>> LISTA_GUERRA = new ArrayList<>();

    // Materiales
    public static final RegistryObject<Item> PLACA_ACERO = simple("placa_acero");
    public static final RegistryObject<Item> CIRCUITO = simple("circuito");

    // Munición
    public static final RegistryObject<Item> MUNICION = simple("municion");
    public static final RegistryObject<Item> COHETE = simple("cohete");
    public static final RegistryObject<Item> BOMBA_AEREA = simple("bomba_aerea");
    public static final RegistryObject<Item> MISIL_BALISTICO = registrar("misil_balistico", () -> new Item(new Item.Properties().stacksTo(4)));
    public static final RegistryObject<Item> MISIL_NUCLEAR = registrar("misil_nuclear",
            () -> new Item(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));
    /** Solo para dibujar las balas en vuelo (no sale en el creativo). */
    public static final RegistryObject<Item> PROYECTIL_BALA = ITEMS.register("proyectil_bala", () -> new Item(new Item.Properties()));

    // Armas
    public static final RegistryObject<Item> PISTOLA = registrar("pistola",
            () -> new ArmaItem(6f, 4.0f, 1.5f, 8, 1, 1.0f, false, MUNICION, ModSonidos.DISPARO, 1.3f));
    public static final RegistryObject<Item> FUSIL = registrar("fusil",
            () -> new ArmaItem(7f, 5.0f, 2.0f, 3, 1, 1.5f, false, MUNICION, ModSonidos.DISPARO, 1.0f));
    public static final RegistryObject<Item> ESCOPETA = registrar("escopeta",
            () -> new ArmaItem(4f, 3.5f, 9.0f, 18, 6, 0.8f, false, MUNICION, ModSonidos.DISPARO_FUERTE, 1.2f));
    public static final RegistryObject<Item> FRANCOTIRADOR = registrar("francotirador",
            () -> new ArmaItem(24f, 8.0f, 0.0f, 35, 1, 3.0f, false, MUNICION, ModSonidos.DISPARO_FUERTE, 0.8f));
    public static final RegistryObject<Item> LANZACOHETES = registrar("lanzacohetes",
            () -> new ArmaItem(0f, 2.2f, 0.5f, 40, 1, 0f, true, COHETE, ModSonidos.DISPARO_FUERTE, 0.5f));
    public static final RegistryObject<Item> GRANADA = registrar("granada", () -> new GranadaItem(new Item.Properties().stacksTo(16)));

    // Aviones y misiles
    public static final RegistryObject<Item> AVION_CAZA = registrar("avion_caza", () -> new AvionItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> RADIO_BOMBARDEO = registrar("radio_bombardeo",
            () -> new RadioBombardeoItem(new Item.Properties().stacksTo(4).rarity(Rarity.RARE)));
    public static final RegistryObject<Item> DESIGNADOR_LASER = registrar("designador_laser",
            () -> new DesignadorItem(new Item.Properties().stacksTo(1)));

    // Radiación
    public static final RegistryObject<Item> GEIGER = registrar("geiger", () -> new GeigerItem(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> PASTILLAS_YODO = registrar("pastillas_yodo", () -> new YodoItem(new Item.Properties().stacksTo(16)));

    // Armaduras
    public static final RegistryObject<Item> CASCO_MILITAR = armadura("casco_militar", MaterialArmadura.MILITAR, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> CHALECO_ANTIBALAS = armadura("chaleco_antibalas", MaterialArmadura.MILITAR, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PANTALON_MILITAR = armadura("pantalon_militar", MaterialArmadura.MILITAR, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOTAS_MILITARES = armadura("botas_militares", MaterialArmadura.MILITAR, ArmorItem.Type.BOOTS);
    public static final RegistryObject<Item> MASCARA_NBQ = armadura("mascara_nbq", MaterialArmadura.NBQ, ArmorItem.Type.HELMET);
    public static final RegistryObject<Item> TRAJE_NBQ = armadura("traje_nbq", MaterialArmadura.NBQ, ArmorItem.Type.CHESTPLATE);
    public static final RegistryObject<Item> PANTALON_NBQ = armadura("pantalon_nbq", MaterialArmadura.NBQ, ArmorItem.Type.LEGGINGS);
    public static final RegistryObject<Item> BOTAS_NBQ = armadura("botas_nbq", MaterialArmadura.NBQ, ArmorItem.Type.BOOTS);

    private ModItems() {}

    private static <T extends Item> RegistryObject<T> registrar(String nombre, Supplier<T> s) {
        RegistryObject<T> r = ITEMS.register(nombre, s);
        LISTA_GUERRA.add(r);
        return r;
    }

    private static RegistryObject<Item> simple(String nombre) {
        return registrar(nombre, () -> new Item(new Item.Properties()));
    }

    private static RegistryObject<Item> armadura(String nombre, MaterialArmadura m, ArmorItem.Type tipo) {
        return registrar(nombre, () -> new ArmorItem(m, tipo, new Item.Properties()));
    }
}

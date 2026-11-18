package es.guerramod.items;

import es.guerramod.GuerraMod;
import es.guerramod.registro.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.function.Supplier;

/** Materiales de armadura: militar (camuflaje, resistente) y NBQ (protege de la radiación). */
public enum MaterialArmadura implements ArmorMaterial {
    MILITAR("militar", 30, new int[]{3, 6, 8, 3}, 10, 1.5f, 0.1f, () -> Ingredient.of(ModItems.PLACA_ACERO.get())),
    NBQ("nbq", 18, new int[]{1, 3, 4, 1}, 8, 0.0f, 0.0f, () -> Ingredient.of(net.minecraft.world.item.Items.LEATHER));

    // orden de defensa: botas, pantalón, pecho, casco
    private static final int[] DURABILIDAD_BASE = {13, 15, 16, 11};

    private final String nombre;
    private final int multiplicador;
    private final int[] defensa;
    private final int encantabilidad;
    private final float dureza, antiEmpuje;
    private final Supplier<Ingredient> reparacion;

    MaterialArmadura(String nombre, int multiplicador, int[] defensa, int encantabilidad, float dureza, float antiEmpuje,
                     Supplier<Ingredient> reparacion) {
        this.nombre = nombre;
        this.multiplicador = multiplicador;
        this.defensa = defensa;
        this.encantabilidad = encantabilidad;
        this.dureza = dureza;
        this.antiEmpuje = antiEmpuje;
        this.reparacion = reparacion;
    }

    private static int indice(ArmorItem.Type t) {
        return switch (t) {
            case BOOTS -> 0;
            case LEGGINGS -> 1;
            case CHESTPLATE -> 2;
            case HELMET -> 3;
        };
    }

    @Override
    public int getDurabilityForType(ArmorItem.Type t) {
        return DURABILIDAD_BASE[indice(t)] * multiplicador;
    }

    @Override
    public int getDefenseForType(ArmorItem.Type t) {
        return defensa[indice(t)];
    }

    @Override
    public int getEnchantmentValue() {
        return encantabilidad;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_IRON;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return reparacion.get();
    }

    @Override
    public String getName() {
        return GuerraMod.MODID + ":" + nombre;
    }

    @Override
    public float getToughness() {
        return dureza;
    }

    @Override
    public float getKnockbackResistance() {
        return antiEmpuje;
    }
}

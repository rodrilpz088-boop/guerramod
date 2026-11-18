package es.guerramod.registro;

import es.guerramod.GuerraMod;
import es.guerramod.radiacion.EfectoRadiacion;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEfectos {
    public static final DeferredRegister<MobEffect> EFECTOS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, GuerraMod.MODID);

    public static final RegistryObject<MobEffect> RADIACION = EFECTOS.register("radiacion", EfectoRadiacion::new);
    public static final RegistryObject<MobEffect> PROTECCION_RADIACION = EFECTOS.register("proteccion_radiacion",
            () -> new EfectoSimple(MobEffectCategory.BENEFICIAL, 0x3FA7D6));

    private ModEfectos() {}

    /** Efecto sin comportamiento propio (solo sirve para comprobar si el jugador lo tiene). */
    public static class EfectoSimple extends MobEffect {
        public EfectoSimple(MobEffectCategory cat, int color) {
            super(cat, color);
        }
    }
}

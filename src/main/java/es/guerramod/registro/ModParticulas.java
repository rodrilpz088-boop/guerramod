package es.guerramod.registro;

import es.guerramod.GuerraMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModParticulas {
    private ModParticulas() {}

    public static final DeferredRegister<ParticleType<?>> PARTICULAS = DeferredRegister.create(ForgeRegistries.PARTICLE_TYPES, GuerraMod.MODID);
    /** Hoja que cae (el color va en la velocidad inicial). */
    public static final RegistryObject<SimpleParticleType> HOJA = PARTICULAS.register("hoja", () -> new SimpleParticleType(false));
}

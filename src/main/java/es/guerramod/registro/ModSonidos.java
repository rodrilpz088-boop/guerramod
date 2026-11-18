package es.guerramod.registro;

import es.guerramod.GuerraMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSonidos {
    public static final DeferredRegister<SoundEvent> SONIDOS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, GuerraMod.MODID);

    public static final RegistryObject<SoundEvent> DISPARO = registrar("disparo");
    public static final RegistryObject<SoundEvent> DISPARO_FUERTE = registrar("disparo_fuerte");
    public static final RegistryObject<SoundEvent> SIRENA = registrar("sirena");
    public static final RegistryObject<SoundEvent> GEIGER = registrar("geiger");
    public static final RegistryObject<SoundEvent> ALARMA = registrar("alarma");

    private ModSonidos() {}

    private static RegistryObject<SoundEvent> registrar(String nombre) {
        return SONIDOS.register(nombre, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GuerraMod.MODID, nombre)));
    }
}

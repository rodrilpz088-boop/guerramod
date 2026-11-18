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
    public static final RegistryObject<SoundEvent> DISPARO_PISTOLA = registrar("disparo_pistola");
    public static final RegistryObject<SoundEvent> DISPARO_SUBFUSIL = registrar("disparo_subfusil");
    public static final RegistryObject<SoundEvent> DISPARO_FUSIL = registrar("disparo_fusil");
    public static final RegistryObject<SoundEvent> DISPARO_ESCOPETA = registrar("disparo_escopeta");
    public static final RegistryObject<SoundEvent> DISPARO_FRANCOTIRADOR = registrar("disparo_francotirador");
    public static final RegistryObject<SoundEvent> DISPARO_AMETRALLADORA = registrar("disparo_ametralladora");
    public static final RegistryObject<SoundEvent> CARGADOR_FUERA = registrar("cargador_fuera");
    public static final RegistryObject<SoundEvent> CARGADOR_DENTRO = registrar("cargador_dentro");
    public static final RegistryObject<SoundEvent> MONTAR = registrar("montar");
    public static final RegistryObject<SoundEvent> CERROJO = registrar("cerrojo");
    public static final RegistryObject<SoundEvent> CORREDERA = registrar("corredera");
    public static final RegistryObject<SoundEvent> CARTUCHO = registrar("cartucho");
    public static final RegistryObject<SoundEvent> VACIO = registrar("vacio");
    public static final RegistryObject<SoundEvent> MODO = registrar("modo");
    public static final RegistryObject<SoundEvent> REBOTE = registrar("rebote");
    public static final RegistryObject<SoundEvent> CASQUILLO = registrar("casquillo");
    public static final RegistryObject<SoundEvent> CLIC = registrar("clic");
    public static final RegistryObject<SoundEvent> MANGUERA = registrar("manguera");
    public static final RegistryObject<SoundEvent> EXTINTOR = registrar("extintor");
    public static final RegistryObject<SoundEvent> ALARMA_INCENDIO = registrar("alarma_incendio");

    private ModSonidos() {}

    private static RegistryObject<SoundEvent> registrar(String nombre) {
        return SONIDOS.register(nombre, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GuerraMod.MODID, nombre)));
    }
}

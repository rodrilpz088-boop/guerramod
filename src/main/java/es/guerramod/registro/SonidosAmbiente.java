package es.guerramod.registro;

import es.guerramod.GuerraMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/** Sonidos de ambiente (sintetizados): pájaros, insectos, búho, ranas, olas, viento, arroyo. */
public final class SonidosAmbiente {
    private SonidosAmbiente() {}

    private static RegistryObject<SoundEvent> r(String n) {
        return ModSonidos.SONIDOS.register("ambiente." + n, () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(GuerraMod.MODID, "ambiente." + n)));
    }

    public static final RegistryObject<SoundEvent> JILGUERO = r("jilguero"), MIRLO = r("mirlo"), GORRION = r("gorrion"), RUISENOR = r("ruisenor"),
            PALOMA = r("paloma"), CUCO = r("cuco"), CARPINTERO = r("carpintero"), GAVIOTA = r("gaviota"), GRILLOS = r("grillos"),
            CHICHARRA = r("chicharra"), BUHO = r("buho"), RANA = r("rana"), OLAS = r("olas"), VIENTO_HOJAS = r("viento_hojas"),
            ARROYO = r("arroyo"), CHAPOTEO = r("chapoteo");

    public static void iniciar() {
    }
}

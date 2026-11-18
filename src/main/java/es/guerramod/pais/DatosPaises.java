package es.guerramod.pais;

import es.guerramod.red.Red;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Guarda los países dentro del mundo (carpeta data del Overworld). */
public class DatosPaises extends SavedData {
    private static final String NOMBRE = "guerramod_paises";
    public RegistroPaises registro;

    public DatosPaises() {
        this.registro = new RegistroPaises();
    }

    public static DatosPaises cargar(CompoundTag tag) {
        DatosPaises d = new DatosPaises();
        d.registro = RegistroPaises.cargar(tag);
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag t = registro.guardar();
        for (String k : t.getAllKeys()) tag.put(k, t.get(k));
        return tag;
    }

    public static DatosPaises get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DatosPaises::cargar, DatosPaises::new, NOMBRE);
    }

    /** Marca para guardar y envía los cambios a todos los jugadores. */
    public static void cambiado(MinecraftServer server) {
        get(server).setDirty();
        Red.sincronizarTodos(server);
    }
}

package es.guerramod;

import es.guerramod.red.Red;
import es.guerramod.registro.*;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/** Naciones en Guerra: países, fronteras, gobiernos y guerra para Minecraft 1.20.1 (Forge). */
@Mod(GuerraMod.MODID)
public class GuerraMod {
    public static final String MODID = "guerramod";

    public GuerraMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModBloques.BLOQUES.register(bus);
        ModItems.ITEMS.register(bus);
        ModBloqueEntidades.TIPOS.register(bus);
        ModEntidades.ENTIDADES.register(bus);
        ModEfectos.EFECTOS.register(bus);
        ModSonidos.SONIDOS.register(bus);
        ModPestanas.PESTANAS.register(bus);
        Red.registrar();
    }
}

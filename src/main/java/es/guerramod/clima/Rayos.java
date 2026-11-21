package es.guerramod.clima;

import es.guerramod.GuerraMod;
import net.minecraft.world.entity.LightningBolt;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Los rayos de las tormentas ya no prenden fuego (los incendios empiezan por descuidos humanos, no por rayos).
 * Los rayos del tridente con canalización siguen funcionando.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Rayos {
    private Rayos() {}

    @SubscribeEvent
    public static void rayo(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof LightningBolt r)) return;
        if (r.getCause() == null) r.setVisualOnly(true);
    }
}

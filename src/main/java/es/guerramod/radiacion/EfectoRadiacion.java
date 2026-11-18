package es.guerramod.radiacion;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** Radiación: hace daño poco a poco y da hambre. */
public class EfectoRadiacion extends MobEffect {
    public EfectoRadiacion() {
        super(MobEffectCategory.HARMFUL, 0x7FD93B);
    }

    @Override
    public void applyEffectTick(LivingEntity e, int nivel) {
        e.hurt(e.damageSources().magic(), 1.0f + nivel * 0.5f);
        if (e instanceof Player p) p.causeFoodExhaustion(0.5f * (nivel + 1));
    }

    @Override
    public boolean isDurationEffectTick(int duracion, int nivel) {
        int intervalo = Math.max(10, 50 >> nivel);
        return duracion % intervalo == 0;
    }
}

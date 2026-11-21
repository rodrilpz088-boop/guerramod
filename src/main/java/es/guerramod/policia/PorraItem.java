package es.guerramod.policia;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;

/** Defensa (porra policial): golpe que aturde (ralentiza) en vez de matar rápido. */
public class PorraItem extends SwordItem {
    public PorraItem(Tier t, int dano, float vel, Properties p) {
        super(t, dano, vel, p);
    }

    @Override
    public boolean hurtEnemy(ItemStack st, LivingEntity objetivo, LivingEntity atacante) {
        objetivo.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
        return super.hurtEnemy(st, objetivo, atacante);
    }
}

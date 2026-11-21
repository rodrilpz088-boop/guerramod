package es.guerramod.policia;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Esposas: clic derecho a alguien y queda detenido 1 minuto (no puede moverse ni pegar). */
public class EsposasItem extends Item {
    public EsposasItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack st, Player p, LivingEntity e, InteractionHand h) {
        if (p.level().isClientSide) return InteractionResult.SUCCESS;
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 1200, 6));
        e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 1200, 4));
        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 1200, 4));
        if (e instanceof net.minecraft.world.entity.Mob m) m.setTarget(null);
        p.level().playSound(null, e.blockPosition(), SoundEvents.CHAIN_PLACE, SoundSource.PLAYERS, 1f, 1.4f);
        p.displayClientMessage(Component.literal(e.getName().getString() + " queda detenido").withStyle(ChatFormatting.BLUE), true);
        if (e instanceof Player o) o.displayClientMessage(Component.literal("Estás detenido por la Policía Nacional").withStyle(ChatFormatting.RED), true);
        st.hurtAndBreak(1, p, x -> x.broadcastBreakEvent(h));
        return InteractionResult.CONSUME;
    }
}

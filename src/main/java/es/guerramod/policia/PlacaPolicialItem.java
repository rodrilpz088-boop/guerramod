package es.guerramod.policia;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Placa de la Policía Nacional: «¡Policía! ¡Alto!»: los que estén cerca se quedan quietos unos segundos. */
public class PlacaPolicialItem extends Item {
    public PlacaPolicialItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level l, Player p, InteractionHand h) {
        if (!l.isClientSide) {
            Component grito = Component.literal("«¡Policía Nacional! ¡Alto!» — " + p.getName().getString()).withStyle(ChatFormatting.BLUE, ChatFormatting.BOLD);
            for (ServerPlayer o : ((net.minecraft.server.level.ServerLevel) l).players()) if (o.distanceToSqr(p) < 32 * 32) o.sendSystemMessage(grito);
            for (Mob m : l.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(14), m -> m.isAlive())) {
                m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, m instanceof Enemy ? 2 : 4));
                if (m instanceof Enemy) m.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0));
            }
            l.playSound(null, p.blockPosition(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1f, 0.6f);
            p.getCooldowns().addCooldown(this, 200);
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(h), l.isClientSide);
    }
}

package es.guerramod.items;

import es.guerramod.registro.ModEfectos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Pastillas de yodo: curan la radiación y protegen durante 5 minutos. */
public class YodoItem extends Item {
    public YodoItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        if (!level.isClientSide) {
            p.removeEffect(ModEfectos.RADIACION.get());
            p.addEffect(new MobEffectInstance(ModEfectos.PROTECCION_RADIACION.get(), 6000, 0));
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.GENERIC_DRINK, SoundSource.PLAYERS, 0.8f, 1.2f);
        }
        if (!p.getAbilities().instabuild) st.shrink(1);
        p.getCooldowns().addCooldown(this, 40);
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Cura la radiación y protege 5 minutos").withStyle(ChatFormatting.GRAY));
    }
}

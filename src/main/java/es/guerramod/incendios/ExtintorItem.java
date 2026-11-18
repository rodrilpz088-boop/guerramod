package es.guerramod.incendios;

import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/** Extintor de polvo: unos 15 segundos de descarga, alcance corto. Ideal para fuegos pequeños y personas ardiendo. */
public class ExtintorItem extends Item {
    public ExtintorItem(Properties p) {
        super(p);
    }

    private static boolean vacio(ItemStack st) {
        return st.getDamageValue() >= st.getMaxDamage() - 1;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        if (vacio(st)) {
            if (!level.isClientSide) p.displayClientMessage(Component.literal("Extintor vacío").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(st);
        }
        p.startUsingItem(mano);
        return InteractionResultHolder.consume(st);
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack viejo, ItemStack nuevo, boolean cambioRanura) {
        return cambioRanura || viejo.getItem() != nuevo.getItem();
    }

    @Override
    public int getUseDuration(ItemStack st) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack st) {
        return UseAnim.BOW;
    }

    @Override
    public void onUseTick(Level level, LivingEntity e, ItemStack st, int quedan) {
        if (!(e instanceof Player p)) return;
        if (vacio(st)) {
            if (!level.isClientSide) p.displayClientMessage(Component.literal("Extintor vacío").withStyle(ChatFormatting.RED), true);
            p.releaseUsingItem();
            return;
        }
        if (level.isClientSide) {
            Chorro.particulas(level, p, false, 0.55);
            return;
        }
        if (!p.getAbilities().instabuild) st.setDamageValue(st.getDamageValue() + 1);
        Chorro.rociar((ServerLevel) level, p, 7, 1.3, 0.0, 600, 0.0);
        if (quedan % 10 == 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSonidos.EXTINTOR.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        int carga = 100 - st.getDamageValue() * 100 / st.getMaxDamage();
        tip.add(Component.literal("Carga: " + carga + " %").withStyle(carga > 20 ? ChatFormatting.GREEN : ChatFormatting.RED));
        tip.add(Component.literal("Mantén clic derecho para descargar").withStyle(ChatFormatting.DARK_GRAY));
    }
}

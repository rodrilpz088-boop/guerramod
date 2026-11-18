package es.guerramod.incendios;

import es.guerramod.items.DesignadorItem;
import es.guerramod.pais.Gobierno;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Radio de emergencias: pide un hidroavión que descarga agua donde apuntas. */
public class RadioHidroavionItem extends Item {
    public RadioHidroavionItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        if (level.isClientSide) return InteractionResultHolder.success(st);
        BlockPos objetivo = DesignadorItem.apuntar(p, 300);
        if (objetivo == null) {
            p.displayClientMessage(Component.literal("Apunta a un punto del suelo (máx. 300 bloques)").withStyle(ChatFormatting.RED), true);
            return InteractionResultHolder.fail(st);
        }
        Vec3 dir = new Vec3(p.getLookAngle().x, 0, p.getLookAngle().z);
        if (dir.lengthSqr() < 1.0E-4) dir = new Vec3(0, 0, 1);
        dir = dir.normalize();
        double altura = Math.min(level.getMaxBuildHeight() - 10, Math.max(objetivo.getY(), p.getY()) + 22);
        Vec3 inicio = Vec3.atCenterOf(objetivo).subtract(dir.scale(110)).with(net.minecraft.core.Direction.Axis.Y, altura);
        HidroavionEntity b = new HidroavionEntity(level, inicio, objetivo, altura, p);
        level.addFreshEntity(b);
        level.playSound(null, p.getX(), p.getY(), p.getZ(), net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 1.0f, 0.8f);
        p.sendSystemMessage(Component.literal("✈ Hidroavión en camino para descargar agua en " + objetivo.toShortString()).withStyle(ChatFormatting.GOLD));
        if (!p.getAbilities().instabuild) st.shrink(1);
        p.getCooldowns().addCooldown(this, 100);
        return InteractionResultHolder.consume(st);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Clic derecho mirando al objetivo").withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Llama a un hidroavión que suelta 6.000 l de agua").withStyle(ChatFormatting.DARK_GRAY));
    }
}

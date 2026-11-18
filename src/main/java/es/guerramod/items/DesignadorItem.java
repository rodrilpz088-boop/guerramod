package es.guerramod.items;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.List;

/** Designador láser: marca un objetivo para el lanzador de misiles. */
public class DesignadorItem extends Item {
    public DesignadorItem(Properties p) {
        super(p);
    }

    @Nullable
    public static BlockPos getObjetivo(ItemStack st) {
        if (st.getTag() == null || !st.getTag().contains("objetivo")) return null;
        return BlockPos.of(st.getTag().getLong("objetivo"));
    }

    /** Bloque al que mira el jugador hasta cierta distancia. */
    @Nullable
    public static BlockPos apuntar(Player p, double distancia) {
        HitResult hit = p.pick(distancia, 1.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return null;
        return ((BlockHitResult) hit).getBlockPos();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        if (!level.isClientSide) {
            BlockPos pos = apuntar(p, 400);
            if (pos == null) {
                p.displayClientMessage(Component.literal("Sin objetivo (demasiado lejos)").withStyle(ChatFormatting.RED), true);
            } else {
                st.getOrCreateTag().putLong("objetivo", pos.asLong());
                p.displayClientMessage(Component.literal("Objetivo fijado: " + pos.toShortString()).withStyle(ChatFormatting.GREEN), true);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.7f, 2.0f);
            }
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public boolean isFoil(ItemStack st) {
        return getObjetivo(st) != null;
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        BlockPos o = getObjetivo(st);
        tip.add(Component.literal(o == null ? "Sin objetivo" : "Objetivo: " + o.toShortString()).withStyle(ChatFormatting.GRAY));
        tip.add(Component.literal("Clic derecho: marcar objetivo").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Úsalo sobre el Lanzador de misiles").withStyle(ChatFormatting.DARK_GRAY));
    }
}

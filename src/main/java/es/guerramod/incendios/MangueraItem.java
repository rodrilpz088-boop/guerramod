package es.guerramod.incendios;

import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Manguera de bomberos. Se conecta a una boca de incendio o BIE (agua sin límite hasta 45 m) o funciona con el
 * agua de la mochila extintora. Mantén clic derecho para lanzar el chorro (unos 16 m).
 */
public class MangueraItem extends Item {
    public static final int LARGO = 45;

    public MangueraItem(Properties p) {
        super(p);
    }

    public static void conectar(ItemStack st, Level level, BlockPos pos, Player p) {
        st.getOrCreateTag().putLong("boca", pos.asLong());
        st.getOrCreateTag().putString("dim", level.dimension().location().toString());
        p.displayClientMessage(Component.literal("Manguera conectada (" + LARGO + " m de tramo)").withStyle(ChatFormatting.AQUA), true);
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.CHAIN_PLACE, SoundSource.BLOCKS, 1.0f, 0.8f);
    }

    @Nullable
    public static BlockPos boca(ItemStack st, Level level) {
        if (st.getTag() == null || !st.getTag().contains("boca")) return null;
        if (!level.dimension().location().toString().equals(st.getTag().getString("dim"))) return null;
        return BlockPos.of(st.getTag().getLong("boca"));
    }

    /** Hay agua: boca conectada y al alcance, o mochila con agua (gasta 1). */
    private static boolean agua(ServerPlayer p, ItemStack st) {
        BlockPos b = boca(st, p.level());
        if (b != null && p.level().isLoaded(b) && p.level().getBlockState(b).getBlock() instanceof BocaIncendioBlock
                && b.distSqr(p.blockPosition()) <= LARGO * LARGO) return true;
        ItemStack mochila = p.getItemBySlot(EquipmentSlot.CHEST);
        return mochila.getItem() instanceof MochilaExtintoraItem && MochilaExtintoraItem.gastar(mochila, 1);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player p = ctx.getPlayer();
        if (p == null) return InteractionResult.PASS;
        if (level.getBlockState(ctx.getClickedPos()).getBlock() instanceof BocaIncendioBlock) {
            if (!level.isClientSide) conectar(ctx.getItemInHand(), level, ctx.getClickedPos(), p);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        // rellenar la mochila en agua
        ItemStack mochila = p.getItemBySlot(EquipmentSlot.CHEST);
        if (mochila.getItem() instanceof MochilaExtintoraItem && p.isShiftKeyDown()
                && level.getFluidState(p.blockPosition()).is(FluidTags.WATER)) {
            if (!level.isClientSide) {
                MochilaExtintoraItem.llenar(mochila);
                p.displayClientMessage(Component.literal("Mochila extintora llena").withStyle(ChatFormatting.AQUA), true);
            }
            return InteractionResultHolder.success(st);
        }
        p.startUsingItem(mano);
        return InteractionResultHolder.consume(st);
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
        if (level.isClientSide) {
            Chorro.particulas(level, p, true, 1.25);
            return;
        }
        ServerPlayer sp = (ServerPlayer) p;
        if (!agua(sp, st)) {
            sp.displayClientMessage(Component.literal("Sin agua: conecta la manguera a una boca de incendio (a menos de "
                    + LARGO + " m) o lleva la mochila extintora con agua").withStyle(ChatFormatting.RED), true);
            sp.releaseUsingItem();
            return;
        }
        if (quedan % 2 == 0) Chorro.rociar((ServerLevel) level, p, 16, 0.7, 0.012, 1600, 0.12);
        if (quedan % 12 == 0) level.playSound(null, p.getX(), p.getY(), p.getZ(), ModSonidos.MANGUERA.get(), SoundSource.PLAYERS, 0.9f, 1.0f);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (st.getTag() != null && st.getTag().contains("boca")) {
            BlockPos b = BlockPos.of(st.getTag().getLong("boca"));
            tip.add(Component.literal("Conectada a la boca de " + b.toShortString()).withStyle(ChatFormatting.AQUA));
        } else {
            tip.add(Component.literal("Sin conectar").withStyle(ChatFormatting.GRAY));
        }
        tip.add(Component.literal("Clic en una boca de incendio o BIE: conectar").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Mantén clic derecho: lanzar agua").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Shift + clic dentro del agua: llenar la mochila").withStyle(ChatFormatting.DARK_GRAY));
    }
}

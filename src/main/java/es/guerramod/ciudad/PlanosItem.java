package es.guerramod.ciudad;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Rotation;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Planos de construcción.
 * <ul>
 *   <li>Clic derecho al aire: cambiar de edificio (agachado, hacia atrás).</li>
 *   <li>Clic derecho en el suelo: vista previa del contorno; otro clic en el mismo sitio: construir.</li>
 * </ul>
 * El edificio se orienta con la fachada mirando hacia ti.
 */
public class PlanosItem extends Item {
    public PlanosItem(Properties p) {
        super(p);
    }

    public static Edificio tipo(ItemStack st) {
        int i = st.getOrCreateTag().getInt("tipo");
        Edificio[] v = Edificio.values();
        return v[Math.floorMod(i, v.length)];
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        if (!level.isClientSide) {
            CompoundTag t = st.getOrCreateTag();
            int n = Edificio.values().length;
            t.putInt("tipo", Math.floorMod(t.getInt("tipo") + (p.isShiftKeyDown() ? -1 : 1), n));
            t.remove("pend");
            Edificio e = tipo(st);
            Plano pl = e.plano();
            p.displayClientMessage(Component.literal("Plano: " + e.nombre + " (" + pl.ancho() + "×" + pl.fondo() + ", " + pl.alto()
                    + " de alto) · " + e.descripcion).withStyle(e.contamina ? ChatFormatting.GOLD : ChatFormatting.AQUA), true);
            level.playSound(null, p.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 0.8f, 1.0f);
        }
        return InteractionResultHolder.sidedSuccess(st, level.isClientSide);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player p = ctx.getPlayer();
        if (level.isClientSide || p == null) return InteractionResult.sidedSuccess(level.isClientSide);
        ItemStack st = ctx.getItemInHand();
        BlockPos origen = ctx.getClickedPos();
        Direction mirando = ctx.getHorizontalDirection();
        Edificio e = tipo(st);
        Plano pl = e.plano();
        Rotation rot = Plano.rotacion(mirando);
        if (origen.getY() + pl.maxY >= level.getMaxBuildHeight()) {
            p.displayClientMessage(Component.literal("No cabe: el " + e.nombre + " supera la altura máxima del mundo").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        CompoundTag t = st.getOrCreateTag();
        long ahora = level.getGameTime();
        boolean confirmado = t.contains("pend") && t.getLong("pend") == origen.asLong() && t.getInt("pendDir") == mirando.get2DDataValue()
                && ahora - t.getLong("pendT") < 400;
        if (!confirmado) {
            t.putLong("pend", origen.asLong());
            t.putInt("pendDir", mirando.get2DDataValue());
            t.putLong("pendT", ahora);
            contorno((ServerLevel) level, (ServerPlayer) p, origen, rot, pl);
            p.displayClientMessage(Component.literal("Vista previa: " + e.nombre + ". Vuelve a hacer clic en el mismo bloque para construir")
                    .withStyle(ChatFormatting.YELLOW), true);
            return InteractionResult.SUCCESS;
        }
        if (Obras.ocupado()) {
            p.displayClientMessage(Component.literal("Hay demasiadas obras en marcha, espera a que terminen").withStyle(ChatFormatting.RED), true);
            return InteractionResult.FAIL;
        }
        t.remove("pend");
        Obras.encolar((ServerLevel) level, origen, mirando, e, (ServerPlayer) p);
        if (!p.getAbilities().instabuild) st.hurtAndBreak(1, p, x -> x.broadcastBreakEvent(ctx.getHand()));
        level.playSound(null, origen, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.2f);
        p.displayClientMessage(Component.literal("Construyendo: " + e.nombre + "...").withStyle(ChatFormatting.GREEN), true);
        return InteractionResult.SUCCESS;
    }

    /** Mientras lo llevas en la mano se ve el contorno de la vista previa. */
    @Override
    public void inventoryTick(ItemStack st, Level level, Entity ent, int slot, boolean enMano) {
        if (level.isClientSide || !enMano || !(ent instanceof ServerPlayer p) || level.getGameTime() % 10 != 0) return;
        CompoundTag t = st.getTag();
        if (t == null || !t.contains("pend") || level.getGameTime() - t.getLong("pendT") >= 400) return;
        Edificio e = tipo(st);
        contorno((ServerLevel) level, p, BlockPos.of(t.getLong("pend")), Plano.rotacion(Direction.from2DDataValue(t.getInt("pendDir"))), e.plano());
    }

    private static void contorno(ServerLevel level, ServerPlayer p, BlockPos origen, Rotation rot, Plano pl) {
        int[] xs = {pl.minX, pl.maxX + 1}, ys = {Math.max(pl.minY, 0), pl.maxY + 1}, zs = {pl.minZ, pl.maxZ + 1};
        // 12 aristas de la caja
        for (int a = 0; a < 2; a++) for (int b = 0; b < 2; b++) {
            linea(level, p, origen, rot, xs[0], ys[a], zs[b], xs[1], ys[a], zs[b]);
            linea(level, p, origen, rot, xs[a], ys[0], zs[b], xs[a], ys[1], zs[b]);
            linea(level, p, origen, rot, xs[a], ys[b], zs[0], xs[a], ys[b], zs[1]);
        }
    }

    private static void linea(ServerLevel level, ServerPlayer p, BlockPos origen, Rotation rot, int x1, int y1, int z1, int x2, int y2, int z2) {
        int n = Math.max(Math.abs(x2 - x1), Math.max(Math.abs(y2 - y1), Math.abs(z2 - z1)));
        int paso = n > 60 ? 2 : 1;
        for (int i = 0; i <= n; i += paso) {
            double f = n == 0 ? 0 : (double) i / n;
            double x = x1 + (x2 - x1) * f, y = y1 + (y2 - y1) * f, z = z1 + (z2 - z1) * f;
            // local -> mundo (como Plano.aMundo pero con decimales; z local crece hacia el fondo)
            double wx, wz;
            double lx = x - 0.5, lz = -(z - 0.5);
            switch (rot) {
                case CLOCKWISE_90 -> { wx = -lz; wz = lx; }
                case CLOCKWISE_180 -> { wx = -lx; wz = -lz; }
                case COUNTERCLOCKWISE_90 -> { wx = lz; wz = -lx; }
                default -> { wx = lx; wz = lz; }
            }
            level.sendParticles(p, ParticleTypes.END_ROD, true, origen.getX() + 0.5 + wx, origen.getY() + y, origen.getZ() + 0.5 + wz,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        Edificio e = tipo(st);
        tip.add(Component.literal("Edificio: " + e.nombre).withStyle(ChatFormatting.AQUA));
        tip.add(Component.literal(e.descripcion).withStyle(e.contamina ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        tip.add(Component.literal("Clic derecho al aire: cambiar de edificio").withStyle(ChatFormatting.DARK_GRAY));
        tip.add(Component.literal("Clic en el suelo: vista previa · otro clic: construir").withStyle(ChatFormatting.DARK_GRAY));
    }
}

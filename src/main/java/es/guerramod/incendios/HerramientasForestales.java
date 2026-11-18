package es.guerramod.incendios;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/** Herramientas de las brigadas forestales: batefuegos, rastrillo (cortafuegos) y antorcha de goteo (contrafuego). */
public final class HerramientasForestales {
    private HerramientasForestales() {}

    private static void gastar(UseOnContext ctx, int n) {
        Player p = ctx.getPlayer();
        if (p != null && !p.getAbilities().instabuild) ctx.getItemInHand().hurtAndBreak(n, p, x -> x.broadcastBreakEvent(ctx.getHand()));
    }

    /** Batefuegos: golpea las llamas y las apaga (zona de 3x3). */
    public static class Batefuegos extends Item {
        public Batefuegos(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            BlockPos c = ctx.getClickedPos();
            int n = Fuego.apagarZona(sl, Vec3.atCenterOf(c.relative(ctx.getClickedFace())), 1.8, 100)
                    + Fuego.apagarZona(sl, Vec3.atCenterOf(c), 1.2, 100);
            level.playSound(null, c, SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.8f, 0.7f);
            if (n > 0) gastar(ctx, 1);
            if (ctx.getPlayer() != null) ctx.getPlayer().getCooldowns().addCooldown(this, 7);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal("Clic derecho sobre las llamas para sofocarlas").withStyle(ChatFormatting.GRAY));
        }
    }

    public static boolean rastrillable(BlockState s) {
        return s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.PODZOL) || s.is(Blocks.MYCELIUM) || s.is(Blocks.MOSS_BLOCK) || s.is(Blocks.DIRT)
                || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.DIRT_PATH) || s.is(BloquesIncendio.TIERRA_QUEMADA.get());
    }

    /** Rastrillo forestal (McLeod): raspa la tierra hasta el suelo mineral en una franja de 3. Hace cortafuegos. */
    public static class Rastrillo extends Item {
        public Rastrillo(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            BlockState s = level.getBlockState(pos);
            if (!rastrillable(s)) pos = pos.below();
            if (!rastrillable(level.getBlockState(pos))) return InteractionResult.PASS;
            if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            Direction lado = ctx.getHorizontalDirection().getClockWise();
            int n = 0;
            for (int k = -1; k <= 1; k++) {
                BlockPos g = pos.relative(lado, k);
                if (!rastrillable(sl.getBlockState(g))) {
                    if (rastrillable(sl.getBlockState(g.above()))) g = g.above();
                    else if (rastrillable(sl.getBlockState(g.below()))) g = g.below();
                    else continue;
                }
                BlockPos a = g.above();
                BlockState sa = sl.getBlockState(a);
                if (Fuego.esFuego(sa)) Fuego.apagar(sl, a);
                sa = sl.getBlockState(a);
                if (Fuego.esPasto(sa) || sa.getBlock() instanceof CenizaBlock || sa.is(Blocks.SNOW)) sl.removeBlock(a, false);
                if (!sl.getBlockState(a).isAir()) continue;
                sl.setBlock(g, BloquesIncendio.TIERRA_RASTRILLADA.get().defaultBlockState(), Block.UPDATE_ALL);
                n++;
            }
            if (n > 0) {
                level.playSound(null, pos, SoundEvents.HOE_TILL, SoundSource.BLOCKS, 1.0f, 0.8f);
                level.playSound(null, pos, SoundEvents.GRAVEL_HIT, SoundSource.BLOCKS, 1.0f, 0.9f);
                gastar(ctx, 1);
            }
            if (ctx.getPlayer() != null) ctx.getPlayer().getCooldowns().addCooldown(this, 4);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal("Clic derecho en el suelo: raspa una franja de 3 bloques").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("El fuego no puede cruzar la tierra rastrillada (cortafuegos)").withStyle(ChatFormatting.DARK_GRAY));
            tip.add(Component.literal("También remueve y apaga rescoldos").withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /** Antorcha de goteo: prende fuego controlado para hacer contrafuegos y quemas prescritas. */
    public static class AntorchaGoteo extends Item {
        public AntorchaGoteo(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos().relative(ctx.getClickedFace());
            if (Fuego.esPasto(level.getBlockState(ctx.getClickedPos()))) pos = ctx.getClickedPos();
            if (!(level instanceof ServerLevel sl)) return InteractionResult.SUCCESS;
            if (Fuego.llueve(sl, pos)) {
                if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.literal("Con lluvia no prende").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            if (!Fuego.cabeLlama(sl.getBlockState(pos)) || Fuego.combustible(sl, pos) < 0.05) {
                if (ctx.getPlayer() != null) ctx.getPlayer().displayClientMessage(Component.literal("Aquí no hay nada que arda").withStyle(ChatFormatting.GRAY), true);
                return InteractionResult.FAIL;
            }
            sl.setBlock(pos, BloquesIncendio.LLAMA.get().defaultBlockState(), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.7f, 1.2f);
            gastar(ctx, 1);
            return InteractionResult.CONSUME;
        }

        @Override
        public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
            tip.add(Component.literal("Prende la vegetación para hacer contrafuegos").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("¡Cuidado con el viento!").withStyle(ChatFormatting.RED));
        }
    }
}

package es.guerramod.bloques;

import es.guerramod.registro.ModBloqueEntidades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;

/** Bomba nuclear: se activa con mechero, carga de fuego o redstone. Cuenta atrás de 10 segundos. */
public class BombaNuclearBlock extends Block implements EntityBlock {
    public BombaNuclearBlock(Properties p) {
        super(p);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BombaNuclearBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBloqueEntidades.BOMBA_NUCLEAR.get()) return null;
        return (l, p, s, be) -> ((BombaNuclearBlockEntity) be).tickServidor();
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
        ItemStack st = pl.getItemInHand(h);
        if (!st.is(Items.FLINT_AND_STEEL) && !st.is(Items.FIRE_CHARGE)) {
            if (!l.isClientSide) pl.sendSystemMessage(Component.literal("☢ Actívala con un mechero, una carga de fuego o redstone.")
                    .withStyle(ChatFormatting.YELLOW));
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
        if (!l.isClientSide && l.getBlockEntity(p) instanceof BombaNuclearBlockEntity be && be.activar(pl)) {
            if (!pl.getAbilities().instabuild) {
                if (st.is(Items.FLINT_AND_STEEL)) st.hurtAndBreak(1, pl, x -> x.broadcastBreakEvent(h));
                else st.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    public void neighborChanged(BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moving) {
        if (!l.isClientSide && l.hasNeighborSignal(p) && l.getBlockEntity(p) instanceof BombaNuclearBlockEntity be) {
            be.activar(null);
        }
    }
}

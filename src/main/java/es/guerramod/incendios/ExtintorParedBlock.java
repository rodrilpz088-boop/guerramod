package es.guerramod.incendios;

import es.guerramod.bloques.hogar.MuebleBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Extintor colgado en la pared. Clic derecho con la mano vacía para cogerlo (cargado). */
public class ExtintorParedBlock extends MuebleBlock {
    public ExtintorParedBlock(Properties p, double[][] forma) {
        super(p, forma, -1, 0, Accion.NINGUNA);
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!p.getItemInHand(mano).isEmpty()) return InteractionResult.PASS;
        if (!level.isClientSide) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            p.setItemInHand(mano, new ItemStack(BloquesIncendio.EXTINTOR.get()));
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 1.0f, 1.0f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}

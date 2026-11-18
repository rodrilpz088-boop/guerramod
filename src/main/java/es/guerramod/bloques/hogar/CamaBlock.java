package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;

/** Cama de verdad (se duerme y guarda el punto de aparición) con modelo propio. */
public class CamaBlock extends BedBlock {
    public CamaBlock(Properties p) {
        super(DyeColor.WHITE, p);
    }

    @Override
    public RenderShape getRenderShape(BlockState st) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return null;
    }
}

package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Capa de ceniza: no arde, la lluvia y el tiempo la van llevando. */
public class CenizaBlock extends Block {
    private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 1.5, 16);

    public CenizaBlock(Properties p) {
        super(p);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState st, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState st, Direction d, BlockState n, LevelAccessor level, BlockPos pos, BlockPos np) {
        return st.canSurvive(level, pos) ? st : Blocks.AIR.defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        // la lluvia se lleva la ceniza enseguida; sin lluvia, el viento poco a poco
        if ((level.isRainingAt(pos) && r.nextInt(2) == 0) || r.nextInt(30) == 0) level.removeBlock(pos, false);
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(30) == 0) level.addParticle(ParticleTypes.WHITE_ASH, pos.getX() + r.nextDouble(), pos.getY() + 0.2, pos.getZ() + r.nextDouble(), 0, 0.02, 0);
    }
}

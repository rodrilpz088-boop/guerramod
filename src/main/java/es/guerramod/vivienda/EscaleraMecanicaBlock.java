package es.guerramod.vivienda;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Escalera mecánica: un peldaño que te lleva hacia arriba (en la dirección en la que la pusiste). */
public class EscaleraMecanicaBlock extends HorizontalDirectionalBlock {
    private static final VoxelShape ABAJO = Block.box(0, 0, 0, 16, 8, 16);
    private static final VoxelShape[] FORMAS = new VoxelShape[4];

    static {
        FORMAS[Direction.NORTH.get2DDataValue()] = Shapes.or(ABAJO, Block.box(0, 8, 0, 16, 16, 8));
        FORMAS[Direction.SOUTH.get2DDataValue()] = Shapes.or(ABAJO, Block.box(0, 8, 8, 16, 16, 16));
        FORMAS[Direction.WEST.get2DDataValue()] = Shapes.or(ABAJO, Block.box(0, 8, 0, 8, 16, 16));
        FORMAS[Direction.EAST.get2DDataValue()] = Shapes.or(ABAJO, Block.box(8, 8, 0, 16, 16, 16));
    }

    private final boolean bajada;

    public EscaleraMecanicaBlock(Properties p, boolean bajada) {
        super(p);
        this.bajada = bajada;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return FORMAS[s.getValue(FACING).get2DDataValue()];
    }

    @Override
    public void stepOn(Level l, BlockPos p, BlockState s, Entity e) {
        if (e.isShiftKeyDown()) return;
        Direction d = s.getValue(FACING);
        if (bajada) d = d.getOpposite();
        Vec3 v = e.getDeltaMovement();
        double vel = 0.11;
        double ax = d.getStepX() * vel, az = d.getStepZ() * vel;
        // mantiene una velocidad constante en la dirección de la escalera y centra un poco
        double nx = d.getAxis() == Direction.Axis.X ? ax : v.x * 0.6 + (p.getX() + 0.5 - e.getX()) * 0.05;
        double nz = d.getAxis() == Direction.Axis.Z ? az : v.z * 0.6 + (p.getZ() + 0.5 - e.getZ()) * 0.05;
        e.setDeltaMovement(nx, v.y, nz);
        if (!bajada && e.onGround()) e.setDeltaMovement(nx, Math.max(v.y, 0.0), nz);
        super.stepOn(l, p, s, e);
    }
}

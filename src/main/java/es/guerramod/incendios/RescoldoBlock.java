package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Rescoldo: brasas que quedan tras el paso del fuego. Humean durante minutos, queman al pisarlas y pueden
 * reavivar el incendio (por eso hay que remover los rescoldos con el rastrillo o mojarlos).
 */
public class RescoldoBlock extends Block {
    public static final IntegerProperty EDAD = IntegerProperty.create("edad", 0, 7);
    private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 2, 16);

    public RescoldoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(EDAD, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(EDAD);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState st, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState st, Direction d, BlockState n, net.minecraft.world.level.LevelAccessor level, BlockPos pos, BlockPos np) {
        return st.canSurvive(level, pos) ? st : net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState st, Level level, BlockPos pos, BlockState viejo, boolean moviendo) {
        if (!level.isClientSide && !viejo.is(this)) level.scheduleTick(pos, this, 60 + level.random.nextInt(60));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        if (Fuego.llueve(level, pos) || Fuego.mojado(level, pos)) {
            Fuego.apagar(level, pos);
            return;
        }
        // reavivar: poco probable, pero con viento y sequía es como empiezan los rebrotes
        Fuego.propagar(level, pos, r, 0.3, 1);
        if (r.nextFloat() < 0.4f) Fuego.columnaHumo(level, pos, r, false);
        int edad = st.getValue(EDAD) + 1;
        if (edad >= 7) {
            level.setBlock(pos, BloquesIncendio.CENIZA.get().defaultBlockState(), Block.UPDATE_ALL);
        } else {
            level.setBlock(pos, st.setValue(EDAD, edad), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 100 + r.nextInt(120));
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState st, Entity e) {
        if (!e.isSteppingCarefully() && !e.fireImmune()) e.hurt(level.damageSources().hotFloor(), 1.0f);
        super.stepOn(level, pos, st, e);
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 0.2, pos.getZ() + r.nextDouble(), 0, 0.03, 0);
        if (r.nextInt(8) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 0, 0.05, 0);
        if (r.nextInt(10) == 0) level.addParticle(ParticleTypes.LAVA, pos.getX() + r.nextDouble(), pos.getY() + 0.15, pos.getZ() + r.nextDouble(), 0, 0, 0);
    }
}

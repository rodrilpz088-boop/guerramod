package es.guerramod.incendios;

import es.guerramod.registro.ModSonidos;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Detector de humo (techo) o rociador automático. El detector pita y da señal de redstone si hay fuego cerca;
 * el rociador además echa agua y apaga el fuego que tiene debajo.
 */
public class DetectorHumoBlock extends Block {
    private final VoxelShape forma;
    private final boolean rociador;

    public DetectorHumoBlock(Properties p, boolean rociador) {
        super(p);
        this.rociador = rociador;
        this.forma = rociador ? Block.box(6, 13, 6, 10, 16, 10) : Block.box(4.5, 14.5, 4.5, 11.5, 16, 11.5);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.POWERED);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return forma;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState st, Level level, BlockPos pos, BlockState viejo, boolean moviendo) {
        if (!level.isClientSide && !viejo.is(this)) level.scheduleTick(pos, this, 20);
    }

    private BlockPos buscarFuego(ServerLevel level, BlockPos pos) {
        int r = rociador ? 6 : 8;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-r, -r - 2, -r), pos.offset(r, 1, r))) {
            if (Fuego.esFuego(level.getBlockState(p))) return p.immutable();
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        BlockPos fuego = buscarFuego(level, pos);
        boolean alarma = fuego != null;
        if (alarma != st.getValue(BlockStateProperties.POWERED)) {
            level.setBlock(pos, st.setValue(BlockStateProperties.POWERED, alarma), Block.UPDATE_ALL);
            level.updateNeighborsAt(pos, this);
        }
        if (alarma) {
            if (rociador) {
                Fuego.apagarZona(level, net.minecraft.world.phys.Vec3.atCenterOf(pos.below(3)), 5.5, 1200);
                level.playSound(null, pos, net.minecraft.sounds.SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, 0.6f, 1.4f);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 0.8,
                        pos.getZ() + 0.5, 40, 2.0, 0.1, 2.0, 0);
            } else {
                level.playSound(null, pos, ModSonidos.ALARMA_INCENDIO.get(), SoundSource.BLOCKS, 1.5f, 1.0f);
            }
        }
        level.scheduleTick(pos, this, 20);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isSignalSource(BlockState st) {
        return !rociador;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getSignal(BlockState st, BlockGetter g, BlockPos pos, Direction d) {
        return !rociador && st.getValue(BlockStateProperties.POWERED) ? 15 : 0;
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (!rociador && r.nextInt(4) == 0) {
            level.addParticle(st.getValue(BlockStateProperties.POWERED) ? net.minecraft.core.particles.DustParticleOptions.REDSTONE
                            : new net.minecraft.core.particles.DustParticleOptions(new org.joml.Vector3f(0.2f, 1.0f, 0.3f), 0.4f),
                    pos.getX() + 0.5, pos.getY() + 0.88, pos.getZ() + 0.5, 0, 0, 0);
        }
        if (rociador && st.getValue(BlockStateProperties.POWERED)) {
            for (int i = 0; i < 8; i++) {
                double a = r.nextDouble() * Math.PI * 2;
                level.addParticle(net.minecraft.core.particles.ParticleTypes.FALLING_WATER, pos.getX() + 0.5 + Math.cos(a) * r.nextDouble() * 2.5,
                        pos.getY() + 0.75, pos.getZ() + 0.5 + Math.sin(a) * r.nextDouble() * 2.5, 0, 0, 0);
            }
        }
    }
}

package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Tronco ardiendo: arde mucho más tiempo que la hierba, prende las hojas y troncos de alrededor y queda carbonizado. */
public class TroncoArdiendoBlock extends RotatedPillarBlock {
    public static final IntegerProperty EDAD = IntegerProperty.create("edad", 0, 7);

    public TroncoArdiendoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(AXIS, Direction.Axis.Y).setValue(EDAD, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(EDAD);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onPlace(BlockState st, Level level, BlockPos pos, BlockState viejo, boolean moviendo) {
        if (!level.isClientSide && !viejo.is(this)) level.scheduleTick(pos, this, 20 + level.random.nextInt(20));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        if ((Fuego.llueve(level, pos.above()) && r.nextFloat() < 0.25f) || Fuego.mojado(level, pos)) {
            Fuego.apagar(level, pos);
            return;
        }
        final double dt = 2.0;
        Fuego.extender(level, pos, r, dt, true, 1.6);
        // el tronco ardiendo saca llamas altas por los lados y por arriba (el árbol entero arde)
        for (int i = 0; i < 2; i++) {
            Direction d = r.nextFloat() < 0.4f ? Direction.UP : Direction.Plane.HORIZONTAL.getRandomDirection(r);
            BlockPos np = pos.relative(d);
            if (level.getBlockState(np).isAir()) {
                level.setBlock(np, BloquesIncendio.LLAMA.get().defaultBlockState().setValue(LlamaBlock.COPA, true)
                        .setValue(LlamaBlock.GRANDE, true), Block.UPDATE_ALL);
            }
        }
        if (r.nextFloat() < 0.08f * Clima.get(level.getServer()).fuerza()) Fuego.pavesa(level, pos, r);
        if (r.nextFloat() < 0.35f) Fuego.columnaHumo(level, pos.above(), r, true);
        // un tronco arde unos dos minutos y medio
        int edad = st.getValue(EDAD) + (r.nextDouble() < 8 * dt / 150.0 ? 1 : 0);
        if (edad >= 7) {
            Clima.get(level.getServer()).emitir(0.05, 0.002);
            level.setBlock(pos, BloquesIncendio.TRONCO_QUEMADO.get().defaultBlockState().setValue(AXIS, st.getValue(AXIS)), Block.UPDATE_ALL);
            // a veces se queda humeando un rescoldo al pie del árbol
            if (r.nextFloat() < 0.15f && level.getBlockState(pos.above()).isAir()) {
                level.setBlock(pos.above(), BloquesIncendio.RESCOLDO.get().defaultBlockState(), Block.UPDATE_ALL);
            }
        } else {
            level.setBlock(pos, st.setValue(EDAD, edad), Block.UPDATE_CLIENTS);
            level.scheduleTick(pos, this, 36 + r.nextInt(9));
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState st, Entity e) {
        if (!e.fireImmune()) e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 60));
        super.stepOn(level, pos, st, e);
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        for (Direction d : Direction.values()) {
            if (r.nextInt(3) != 0) continue;
            BlockPos np = pos.relative(d);
            if (!level.getBlockState(np).isAir()) continue;
            double x = pos.getX() + 0.5 + d.getStepX() * 0.55 + (r.nextDouble() - 0.5) * (d.getStepX() == 0 ? 0.9 : 0);
            double y = pos.getY() + 0.5 + d.getStepY() * 0.55 + (r.nextDouble() - 0.5) * (d.getStepY() == 0 ? 0.9 : 0);
            double z = pos.getZ() + 0.5 + d.getStepZ() * 0.55 + (r.nextDouble() - 0.5) * (d.getStepZ() == 0 ? 0.9 : 0);
            level.addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.02, 0);
            if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.3, z, 0, 0.05, 0);
        }
        if (r.nextInt(14) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.2f, 0.6f + r.nextFloat() * 0.3f, false);
        }
    }
}

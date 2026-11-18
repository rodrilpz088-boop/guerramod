package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Mueble que se enciende y apaga con clic derecho: lámparas, tele, ducha, barbacoa, chimenea. */
public class MuebleEncendibleBlock extends MuebleBlock implements Luminaria {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public enum Efecto { LUZ, TELE, AGUA, FUEGO, AIRE }

    public final Efecto efecto;

    public MuebleEncendibleBlock(Properties p, double[][] forma, Efecto efecto) {
        super(p, forma, -1, 0, Accion.NINGUNA);
        this.efecto = efecto;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        super.createBlockStateDefinition(b);
        b.add(LIT);
    }

    @Override
    public boolean controlable(BlockState st) {
        return efecto == Efecto.LUZ;
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) {
            boolean on = !st.getValue(LIT);
            level.setBlock(pos, st.setValue(LIT, on), Block.UPDATE_ALL);
            var sonido = switch (efecto) {
                case AGUA -> on ? SoundEvents.BUCKET_EMPTY : SoundEvents.BUCKET_FILL;
                case FUEGO -> on ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH;
                default -> SoundEvents.STONE_BUTTON_CLICK_ON;
            };
            level.playSound(null, pos, sonido, SoundSource.BLOCKS, 0.6f, on ? 1.2f : 0.9f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (!st.getValue(LIT)) return;
        double x = pos.getX() + 0.5, y = pos.getY(), z = pos.getZ() + 0.5;
        switch (efecto) {
            case AGUA -> {
                // alcachofa de ducha: el agua cae desde la cabeza de la ducha
                Vec3 d = Vec3.atLowerCornerOf(st.getValue(FACING).getNormal());
                for (int i = 0; i < 6; i++) {
                    level.addParticle(ParticleTypes.FALLING_WATER, x + d.x * 0.06 + (r.nextDouble() - 0.5) * 0.28, y + 0.66,
                            z + d.z * 0.06 + (r.nextDouble() - 0.5) * 0.28, 0, 0, 0);
                }
                if (r.nextInt(3) == 0) level.playLocalSound(x, y, z, SoundEvents.WEATHER_RAIN, SoundSource.BLOCKS, 0.15f, 1.6f, false);
            }
            case FUEGO -> {
                level.addParticle(ParticleTypes.SMOKE, x + (r.nextDouble() - 0.5) * 0.5, y + 0.9, z + (r.nextDouble() - 0.5) * 0.5, 0, 0.03, 0);
                if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.FLAME, x + (r.nextDouble() - 0.5) * 0.4, y + 0.7,
                        z + (r.nextDouble() - 0.5) * 0.4, 0, 0.01, 0);
                if (r.nextInt(8) == 0) level.playLocalSound(x, y, z, SoundEvents.CAMPFIRE_CRACKLE, SoundSource.BLOCKS, 0.5f, 1.0f, false);
            }
            case AIRE -> {
                if (r.nextInt(3) == 0) {
                    Vec3 d = Vec3.atLowerCornerOf(st.getValue(FACING).getNormal());
                    level.addParticle(ParticleTypes.WHITE_ASH, x + d.x * 0.5, y + 0.3, z + d.z * 0.5, d.x * 0.05, -0.02, d.z * 0.05);
                }
            }
            default -> { }
        }
    }
}

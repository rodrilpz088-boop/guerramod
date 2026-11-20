package es.guerramod.ciudad;

import es.guerramod.incendios.Clima;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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
import net.minecraft.world.phys.BlockHitResult;

/**
 * Boca de chimenea industrial. Mientras está encendida echa humo (o vapor) y emite CO2 a la atmósfera.
 * Clic derecho: encender / apagar la instalación.
 */
public class ChimeneaBlock extends Block {
    /** ppm de CO2 por cada "latido" (tick aleatorio, más o menos cada minuto). */
    private final double ppm;
    private final boolean vapor;

    public ChimeneaBlock(Properties p, double ppm, boolean vapor) {
        super(p);
        this.ppm = ppm;
        this.vapor = vapor;
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.LIT);
    }

    @Override
    public boolean isRandomlyTicking(BlockState st) {
        return st.getValue(BlockStateProperties.LIT) && ppm > 0;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        if (level.dimension() == Level.OVERWORLD) Clima.get(level.getServer()).emitir(ppm, ppm * 0.004);
    }

    @Override
    @SuppressWarnings("deprecation")
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockState n = st.cycle(BlockStateProperties.LIT);
            level.setBlock(pos, n, Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.6f, 0.8f);
            p.displayClientMessage(Component.literal(n.getValue(BlockStateProperties.LIT)
                    ? (vapor ? "Torre de refrigeración en marcha" : "Chimenea encendida: emite " + String.format("%.1f", ppm) + " ppm de CO2 por minuto")
                    : "Instalación parada: no emite nada"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (!st.getValue(BlockStateProperties.LIT)) return;
        // el humo se inclina con el viento
        double ang = Math.toRadians(Clima.clienteAngulo);
        double v = Math.min(1.0, Clima.clienteVelocidad / 40.0) * 0.035;
        double vx = Math.cos(ang) * v, vz = Math.sin(ang) * v;
        double x = pos.getX() + 0.5, y = pos.getY() + 1.0, z = pos.getZ() + 0.5;
        if (vapor) {
            for (int i = 0; i < 3; i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x + r.nextGaussian() * 0.8, y,
                        z + r.nextGaussian() * 0.8, vx, 0.09 + r.nextDouble() * 0.04, vz);
            }
            level.addParticle(ParticleTypes.CLOUD, x + r.nextGaussian(), y + 0.5, z + r.nextGaussian(), vx * 2, 0.15, vz * 2);
        } else {
            for (int i = 0; i < 2; i++) {
                level.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, x + r.nextGaussian() * 0.25, y,
                        z + r.nextGaussian() * 0.25, vx, 0.07 + r.nextDouble() * 0.03, vz);
            }
            level.addParticle(ParticleTypes.LARGE_SMOKE, x, y + 0.2, z, vx, 0.12, vz);
            if (r.nextInt(3) == 0) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.1, 0);
        }
    }
}

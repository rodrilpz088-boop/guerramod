package es.guerramod.invierno;

import es.guerramod.clima.Nieve;
import es.guerramod.incendios.Meteo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Cañón de nieve (como en las estaciones de esquí): clic derecho para encenderlo. Si hace frío (menos de 2 °C) lanza
 * nieve hacia delante y la va acumulando en un abanico de unos 12 bloques.
 */
public class CanonNieveBlock extends HorizontalDirectionalBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 15, 14);

    public CanonNieveBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING, LIT);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext c) {
        return defaultBlockState().setValue(FACING, c.getHorizontalDirection());
    }

    @Override
    public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
        return FORMA;
    }

    @Override
    public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
        if (!l.isClientSide) {
            boolean on = !s.getValue(LIT);
            l.setBlock(p, s.setValue(LIT, on), Block.UPDATE_ALL);
            if (on) l.scheduleTick(p, this, 10);
            double t = Meteo.en((ServerLevel) l, p).temperatura();
            pl.displayClientMessage(Component.literal(on ? (t < 2 ? String.format("Cañón encendido (%.0f °C): fabricando nieve", t)
                    : String.format("Cañón encendido, pero hace %.0f °C: necesita menos de 2 °C", t)) : "Cañón apagado")
                    .withStyle(on ? ChatFormatting.AQUA : ChatFormatting.GRAY), true);
            l.playSound(null, p, on ? SoundEvents.PISTON_EXTEND : SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 0.6f, 1.2f);
        }
        return InteractionResult.sidedSuccess(l.isClientSide);
    }

    @Override
    public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        if (!s.getValue(LIT)) return;
        l.scheduleTick(p, this, 10);
        if (Meteo.en(l, p).temperatura() >= 2) return;
        Direction d = s.getValue(FACING);
        double ox = p.getX() + 0.5 + d.getStepX() * 0.7, oy = p.getY() + 0.9, oz = p.getZ() + 0.5 + d.getStepZ() * 0.7;
        l.sendParticles(ParticleTypes.SNOWFLAKE, ox, oy, oz, 25, 0.2, 0.2, 0.2, 0.0);
        for (int i = 0; i < 12; i++) {
            double v = 0.4 + r.nextDouble() * 0.6;
            l.sendParticles(ParticleTypes.SNOWFLAKE, ox, oy, oz, 0, d.getStepX() * v + (r.nextDouble() - 0.5) * 0.4, 0.25 + r.nextDouble() * 0.2,
                    d.getStepZ() * v + (r.nextDouble() - 0.5) * 0.4, 1.0);
        }
        l.sendParticles(ParticleTypes.CLOUD, ox + d.getStepX() * 3, oy + 0.6, oz + d.getStepZ() * 3, 3, 1, 0.4, 1, 0.01);
        if (r.nextInt(4) == 0) l.playSound(null, p, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.5f, 0.6f);
        if (r.nextInt(6) == 0) l.playSound(null, p, SoundEvents.ELYTRA_FLYING, SoundSource.BLOCKS, 0.15f, 1.8f);
        // la nieve cae en un abanico delante del cañón
        for (int i = 0; i < 5; i++) {
            double dist = 2 + r.nextDouble() * 11, lado = (r.nextDouble() - 0.5) * dist * 0.7;
            int x = (int) Math.floor(p.getX() + 0.5 + d.getStepX() * dist + d.getClockWise().getStepX() * lado);
            int z = (int) Math.floor(p.getZ() + 0.5 + d.getStepZ() * dist + d.getClockWise().getStepZ() * lado);
            BlockPos a = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
            if (!l.isLoaded(a)) continue;
            Nieve.anadirCapa(l, a, 24);
        }
    }
}

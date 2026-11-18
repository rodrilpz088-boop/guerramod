package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Tierra quemada. No arde (hace de cortafuegos natural) y se va recuperando sola en 4 fases:
 * negra -> brotes -> verdea -> césped otra vez (con hierba y, a veces, un árbol joven).
 * La lluvia acelera mucho la recuperación.
 */
public class TierraQuemadaBlock extends Block {
    public static final IntegerProperty FASE = IntegerProperty.create("fase", 0, 3);

    public TierraQuemadaBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FASE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FASE);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        BlockPos arriba = pos.above();
        BlockState sa = level.getBlockState(arriba);
        if (!sa.isAir() && !(sa.getBlock() instanceof CenizaBlock) && !Fuego.esPasto(sa)) return;
        if (level.getMaxLocalRawBrightness(arriba) < 9) return;
        if (Fuego.esFuego(sa)) return;
        boolean lluvia = level.isRainingAt(arriba);
        // seco: unos 25 minutos por fase; con lluvia, unos 5
        if (r.nextInt(lluvia ? 5 : 22) != 0) return;
        int fase = st.getValue(FASE);
        if (fase < 3) {
            level.setBlock(pos, st.setValue(FASE, fase + 1), Block.UPDATE_ALL);
            return;
        }
        level.setBlock(pos, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
        if (sa.getBlock() instanceof CenizaBlock) level.removeBlock(arriba, false);
        float x = r.nextFloat();
        if (x < 0.03f) {
            // regeneración del bosque: brota un árbol joven
            Block brote = switch (r.nextInt(4)) {
                case 0 -> Blocks.BIRCH_SAPLING;
                case 1 -> Blocks.SPRUCE_SAPLING;
                default -> Blocks.OAK_SAPLING;
            };
            level.setBlock(arriba, brote.defaultBlockState(), Block.UPDATE_ALL);
        } else if (x < 0.45f) {
            level.setBlock(arriba, Blocks.GRASS.defaultBlockState(), Block.UPDATE_ALL);
        }
    }
}

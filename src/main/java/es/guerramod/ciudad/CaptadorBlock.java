package es.guerramod.ciudad;

import es.guerramod.incendios.Clima;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Módulo de captura directa de CO2: unos ventiladores pasan el aire por un filtro que retiene el CO2. */
public class CaptadorBlock extends Block {
    public CaptadorBlock(Properties p) {
        super(p);
    }

    @Override
    public boolean isRandomlyTicking(BlockState st) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        if (level.dimension() == Level.OVERWORLD) Clima.get(level.getServer()).absorber(0.08);
    }
}

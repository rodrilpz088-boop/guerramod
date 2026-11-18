package es.guerramod.bloques;

import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Barrera de control fronterizo: los ciudadanos del país dueno del chunk la atraviesan,
 * los extranjeros (y los mobs) chocan contra ella.
 */
public class BarreraControlBlock extends HorizontalDirectionalBlock {
    public BarreraControlBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection());
    }

    @Override
    public VoxelShape getCollisionShape(BlockState s, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        if (ctx instanceof EntityCollisionContext ec && ec.getEntity() instanceof Player p) {
            Pais dueno = RegistroPaises.de(p.level()).dueno(p.level(), pos);
            if (dueno == null || dueno.esMiembro(p.getUUID())) return Shapes.empty();
        }
        return Shapes.block();
    }

    @Override
    public boolean isPathfindable(BlockState s, BlockGetter g, BlockPos p, PathComputationType t) {
        return false;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState s, BlockGetter g, BlockPos p) {
        return true;
    }
}

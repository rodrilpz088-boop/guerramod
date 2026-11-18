package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Panel o foco LED de techo. Cambia de color con el mando LED; clic derecho enciende/apaga. */
public class LedBlock extends Block implements Luminaria {
    private final VoxelShape forma;

    public LedBlock(Properties p, double[][] cajas) {
        super(p);
        this.forma = Formas.girar(cajas, net.minecraft.core.Direction.NORTH);
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LIT, true).setValue(COLOR, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(BlockStateProperties.LIT, COLOR);
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return forma;
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        if (!level.isClientSide) {
            level.setBlock(pos, st.cycle(BlockStateProperties.LIT), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.STONE_BUTTON_CLICK_ON, SoundSource.BLOCKS, 0.4f, 1.4f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return true;
    }
}

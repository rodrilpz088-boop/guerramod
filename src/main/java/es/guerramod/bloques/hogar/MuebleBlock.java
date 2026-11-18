package es.guerramod.bloques.hogar;

import es.guerramod.entidades.AsientoEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import javax.annotation.Nullable;

/**
 * Mueble genérico que mira hacia el jugador al colocarlo. Según cómo se cree puede:
 * servir de asiento, guardar objetos (como un cofre), dar agua (grifo) o destruir objetos (papelera).
 */
public class MuebleBlock extends HorizontalDirectionalBlock implements EntityBlock {
    public enum Accion { NINGUNA, GRIFO, PAPELERA }

    protected final VoxelShape[] formas;
    /** Altura del asiento en bloques (o negativo si no es asiento). */
    public final double asiento;
    /** Huecos de inventario (0 = no guarda nada). */
    public final int huecos;
    public final Accion accion;

    public MuebleBlock(Properties p, double[][] forma, double asiento, int huecos, Accion accion) {
        super(p);
        this.formas = Formas.crear(forma);
        this.asiento = asiento;
        this.huecos = huecos;
        this.accion = accion;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite());
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return formas[st.getValue(FACING).get2DDataValue()];
    }

    /** Posición del bloque que tiene el inventario (en muebles altos, la mitad de abajo). */
    protected BlockPos posBase(BlockState st, BlockPos pos) {
        return pos;
    }

    @Override
    public InteractionResult use(BlockState st, Level level, BlockPos pos, Player p, InteractionHand mano, BlockHitResult hit) {
        ItemStack enMano = p.getItemInHand(mano);
        if (accion == Accion.GRIFO) return grifo(level, pos, p, mano, enMano);
        if (accion == Accion.PAPELERA) {
            if (enMano.isEmpty()) return InteractionResult.PASS;
            if (!level.isClientSide) {
                p.displayClientMessage(Component.literal("Tirado a la basura: ").append(enMano.getHoverName())
                        .withStyle(ChatFormatting.GRAY), true);
                p.setItemInHand(mano, ItemStack.EMPTY);
                level.playSound(null, pos, SoundEvents.BUNDLE_DROP_CONTENTS, SoundSource.BLOCKS, 0.8f, 0.8f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (huecos > 0) {
            if (!level.isClientSide && level.getBlockEntity(posBase(st, pos)) instanceof MenuProvider menu) {
                p.openMenu(menu);
                level.playSound(null, pos, SoundEvents.BARREL_OPEN, SoundSource.BLOCKS, 0.5f, 1.2f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (asiento >= 0 && !p.isSecondaryUseActive() && enMano.isEmpty()) {
            if (!level.isClientSide) AsientoEntity.sentar(level, pos, asiento, p);
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult grifo(Level level, BlockPos pos, Player p, InteractionHand mano, ItemStack enMano) {
        if (enMano.is(Items.GLASS_BOTTLE)) {
            if (!level.isClientSide) {
                ItemStack agua = PotionUtils.setPotion(new ItemStack(Items.POTION), Potions.WATER);
                p.setItemInHand(mano, ItemUtils.createFilledResult(enMano, p, agua));
                level.playSound(null, pos, SoundEvents.BOTTLE_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (enMano.is(Items.BUCKET)) {
            if (!level.isClientSide) {
                p.setItemInHand(mano, ItemUtils.createFilledResult(enMano, p, new ItemStack(Items.WATER_BUCKET)));
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level instanceof ServerLevel sl) {
            sl.sendParticles(ParticleTypes.FALLING_WATER, pos.getX() + 0.5, pos.getY() + 1.05, pos.getZ() + 0.5, 12, 0.08, 0.05, 0.08, 0.0);
            sl.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 10, 0.15, 0.02, 0.15, 0.05);
            level.playSound(null, pos, SoundEvents.WATER_AMBIENT, SoundSource.BLOCKS, 0.6f, 1.6f);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState st) {
        return huecos > 0 ? new AlmacenBlockEntity(pos, st) : null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState st, Level level, BlockPos pos, BlockState nuevo, boolean moviendo) {
        if (!st.is(nuevo.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof Container c) {
                Containers.dropContents(level, pos, c);
                level.updateNeighbourForOutputSignal(pos, this);
            }
        }
        super.onRemove(st, level, pos, nuevo, moviendo);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return true;
    }
}

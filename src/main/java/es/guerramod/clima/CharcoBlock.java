package es.guerramod.clima;

import es.guerramod.incendios.Meteo;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Charco que deja la lluvia en el césped, la tierra o el asfalto. Mientras llueve crece y se junta con los de al lado
 * en las zonas bajas; cuando para se va secando, más deprisa con calor, sol y aire seco. Al pisarlo salpica.
 */
public class CharcoBlock extends Block {
    public static final IntegerProperty NIVEL = IntegerProperty.create("nivel", 1, 3);
    public static final BooleanProperty LLUVIA = BooleanProperty.create("lluvia");
    /** Había una mata de hierba que el agua ha tapado (vuelve a salir al secarse). */
    public static final BooleanProperty HIERBA = BooleanProperty.create("hierba");
    private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 0.5, 16);

    public CharcoBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(NIVEL, 1).setValue(LLUVIA, true).setValue(HIERBA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(NIVEL, LLUVIA, HIERBA);
    }

    /** Suelos donde se encharca el agua. */
    public static boolean suelo(BlockState s) {
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.DIRT) || s.is(Blocks.COARSE_DIRT) || s.is(Blocks.ROOTED_DIRT) || s.is(Blocks.DIRT_PATH)
                || s.is(Blocks.PODZOL) || s.is(Blocks.MUD) || s.is(Blocks.PACKED_MUD) || s.is(Blocks.STONE) || s.is(Blocks.COBBLESTONE)
                || s.is(Blocks.SMOOTH_STONE) || s.is(Blocks.STONE_BRICKS) || s.is(Blocks.GRAY_CONCRETE) || s.is(Blocks.LIGHT_GRAY_CONCRETE)) return true;
        var k = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock());
        if (k == null || !k.getNamespace().equals("guerramod")) return false;
        String n = k.getPath();
        return n.startsWith("asfalto") || n.startsWith("paso_cebra") || n.equals("tierra_quemada") || n.equals("tierra_rastrillada")
                || n.equals("cesped_artificial") || n.startsWith("gres") || n.equals("hormigon_pulido") || n.startsWith("granito");
    }

    @Override
    public VoxelShape getShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return FORMA;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState st, BlockGetter g, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean canSurvive(BlockState st, LevelReader level, BlockPos pos) {
        BlockState b = level.getBlockState(pos.below());
        return b.isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState updateShape(BlockState st, Direction d, BlockState vecino, LevelAccessor level, BlockPos pos, BlockPos pv) {
        return d == Direction.DOWN && !canSurvive(st, level, pos) ? Blocks.AIR.defaultBlockState() : st;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState st, BlockGetter g, BlockPos pos, PathComputationType t) {
        return true;
    }

    @Override
    public boolean propagatesSkylightDown(BlockState st, BlockGetter g, BlockPos pos) {
        return true;
    }

    @Override
    public boolean isRandomlyTicking(BlockState st) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void randomTick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        int n = st.getValue(NIVEL);
        if (level.isRainingAt(pos)) {
            BlockState nuevo = st.setValue(LLUVIA, true);
            if (n < 3 && r.nextFloat() < 0.6f) nuevo = nuevo.setValue(NIVEL, n + 1);
            else if (n == 3 && r.nextFloat() < 0.35f) extender(level, pos, r);
            if (nuevo != st) level.setBlock(pos, nuevo, Block.UPDATE_CLIENTS);
            return;
        }
        // evaporación: calor, sol y aire seco la aceleran
        Meteo m = Meteo.en(level, pos);
        double sol = level.isDay() && level.canSeeSky(pos) ? 0.12 : 0;
        double p = 0.10 + (m.temperatura() - 10) * 0.018 - (m.humedad() - 60) * 0.004 + sol + m.viento() * 0.003;
        p = Math.max(0.03, Math.min(0.8, p));
        if (r.nextDouble() < p) {
            if (n <= 1) secar(level, pos, st);
            else level.setBlock(pos, st.setValue(NIVEL, n - 1).setValue(LLUVIA, false), Block.UPDATE_CLIENTS);
        } else if (st.getValue(LLUVIA)) {
            level.setBlock(pos, st.setValue(LLUVIA, false), Block.UPDATE_CLIENTS);
        }
    }

    private static void secar(Level level, BlockPos pos, BlockState st) {
        level.setBlock(pos, st.getValue(HIERBA) ? Blocks.GRASS.defaultBlockState() : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
    }

    /** El agua rebosa hacia un lado si está a la misma altura o más bajo. */
    private void extender(ServerLevel level, BlockPos pos, RandomSource r) {
        Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(r);
        for (int dy = 0; dy >= -1; dy--) {
            BlockPos q = pos.relative(d).above(dy);
            BlockState s = level.getBlockState(q);
            boolean hierba = s.is(Blocks.GRASS) || s.is(Blocks.FERN);
            if ((s.isAir() || hierba) && suelo(level.getBlockState(q.below())) && level.isRainingAt(q)) {
                level.setBlock(q, defaultBlockState().setValue(HIERBA, hierba), Block.UPDATE_ALL);
                return;
            }
            if (!s.isAir()) return;
        }
    }

    @Override
    public void entityInside(BlockState st, Level level, BlockPos pos, Entity e) {
        if (e.isOnFire()) e.clearFire();
        double v = e.getDeltaMovement().horizontalDistanceSqr();
        if (v < 0.002) return;
        if (level.isClientSide) {
            if (level.random.nextInt(3) == 0) {
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.SPLASH, e.getX() + (level.random.nextDouble() - 0.5) * 0.5, pos.getY() + 0.1,
                            e.getZ() + (level.random.nextDouble() - 0.5) * 0.5, 0, 0.1, 0);
                }
            }
        } else if (level.random.nextInt(8) == 0) {
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.12f, 1.5f + level.random.nextFloat() * 0.4f);
        }
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (!st.getValue(LLUVIA) || !level.isRainingAt(pos)) return;
        if (r.nextInt(2) == 0) {
            level.addParticle(ParticleTypes.RAIN, pos.getX() + r.nextDouble(), pos.getY() + 0.06, pos.getZ() + r.nextDouble(), 0, 0, 0);
        }
    }

    /** Coloca un charco nuevo si el sitio vale. */
    public static boolean intentar(ServerLevel level, BlockPos aire, BlockState charco) {
        BlockState s = level.getBlockState(aire);
        boolean hierba = s.is(Blocks.GRASS) || s.is(Blocks.FERN);
        if (!(s.isAir() || hierba)) return false;
        if (!suelo(level.getBlockState(aire.below()))) return false;
        level.setBlock(aire, charco.setValue(HIERBA, hierba), Block.UPDATE_ALL);
        return true;
    }
}

package es.guerramod.incendios;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Llamas de incendio forestal. Viven unos segundos según el combustible (la hierba arde muy rápido),
 * se propagan con el viento y cuesta arriba, y al apagarse dejan la tierra quemada, ceniza y a veces rescoldos.
 * COPA = llamas en las hojas de un árbol (fuego de copas).
 */
public class LlamaBlock extends Block {
    public static final IntegerProperty EDAD = IntegerProperty.create("edad", 0, 7);
    public static final BooleanProperty COPA = BooleanProperty.create("copa");
    private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 1, 16);

    public LlamaBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(EDAD, 0).setValue(COPA, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(EDAD, COPA);
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
    public void onPlace(BlockState st, Level level, BlockPos pos, BlockState viejo, boolean moviendo) {
        if (!level.isClientSide && !viejo.is(this)) level.scheduleTick(pos, this, 4 + level.random.nextInt(8));
    }

    @Override
    @SuppressWarnings("deprecation")
    public void tick(BlockState st, ServerLevel level, BlockPos pos, RandomSource r) {
        boolean copa = st.getValue(COPA);
        // lluvia o suelo mojado: se apaga
        if ((Fuego.llueve(level, pos) && r.nextFloat() < 0.55f) || Fuego.mojado(level, pos)) {
            Fuego.apagar(level, pos);
            return;
        }
        double comb = copa ? 0.9 : Fuego.combustible(level, pos);
        if (!copa && comb < 0.05) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        Clima c = Clima.get(level.getServer());
        int intentos = 2 + (comb > 0.6 ? 2 : 0) + (int) Math.round(c.fuerza() * 2);
        Fuego.propagar(level, pos, r, Math.min(1.3, comb + 0.25), intentos);
        if (r.nextFloat() < 0.05f * c.fuerza() * (copa ? 3 : 1)) Fuego.pavesa(level, pos, r);
        if (r.nextFloat() < (copa ? 0.35f : 0.12f)) Fuego.columnaHumo(level, pos, r, copa || comb > 0.8);

        int edad = st.getValue(EDAD) + 1 + (comb < 0.4 ? 1 : 0);
        if (edad >= 7) {
            consumir(level, pos, copa, r);
        } else {
            level.setBlock(pos, st.setValue(EDAD, edad), Block.UPDATE_CLIENTS);
            // la hierba arde en segundos; las copas y lo que tiene mucho combustible, más
            int espera = copa ? 14 + r.nextInt(14) : 6 + r.nextInt(8) + (int) (comb * 10);
            level.scheduleTick(pos, this, espera);
        }
    }

    private void consumir(ServerLevel level, BlockPos pos, boolean copa, RandomSource r) {
        if (copa) {
            level.setBlock(pos, r.nextFloat() < 0.3f ? BloquesIncendio.RAMAS_QUEMADAS.get().defaultBlockState()
                    : Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            return;
        }
        Fuego.quemarSuelo(level, pos);
        Fuego.carbonizarVecinos(level, pos, r);
        BlockState abajo = level.getBlockState(pos.below());
        boolean apoyo = abajo.isFaceSturdy(level, pos.below(), net.minecraft.core.Direction.UP);
        float x = r.nextFloat();
        BlockState resto;
        if (apoyo && x < 0.09f) resto = BloquesIncendio.RESCOLDO.get().defaultBlockState();
        else if (apoyo && x < 0.4f) resto = BloquesIncendio.CENIZA.get().defaultBlockState();
        else resto = Blocks.AIR.defaultBlockState();
        level.setBlock(pos, resto, Block.UPDATE_ALL);
    }

    @Override
    public void entityInside(BlockState st, Level level, BlockPos pos, Entity e) {
        if (!e.fireImmune()) {
            e.setRemainingFireTicks(Math.max(e.getRemainingFireTicks(), 100));
            e.hurt(level.damageSources().inFire(), 1.5f);
        }
    }

    @Override
    public void animateTick(BlockState st, Level level, BlockPos pos, RandomSource r) {
        if (r.nextInt(18) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS,
                    1.0f + r.nextFloat(), r.nextFloat() * 0.7f + 0.3f, false);
        }
        for (int i = 0; i < 2; i++) {
            level.addParticle(ParticleTypes.LARGE_SMOKE, pos.getX() + r.nextDouble(), pos.getY() + 0.6 + r.nextDouble() * 0.6,
                    pos.getZ() + r.nextDouble(), 0, 0.04, 0);
        }
        if (r.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + r.nextDouble(), pos.getY() + r.nextDouble() * 0.8,
                    pos.getZ() + r.nextDouble(), 0, 0.03, 0);
        }
        if (r.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.LAVA, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 0, 0, 0);
        }
    }
}

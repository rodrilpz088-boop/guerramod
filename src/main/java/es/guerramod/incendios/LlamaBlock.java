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
    /** Llamas altas (copas de árboles, junto a troncos, mucho combustible). */
    public static final BooleanProperty GRANDE = BooleanProperty.create("grande");
    private static final VoxelShape FORMA = Block.box(0, 0, 0, 16, 1, 16);

    public LlamaBlock(Properties p) {
        super(p);
        registerDefaultState(stateDefinition.any().setValue(EDAD, 0).setValue(COPA, false).setValue(GRANDE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(EDAD, COPA, GRANDE);
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
        if (!level.isClientSide && !viejo.is(this)) level.scheduleTick(pos, this, 6 + level.random.nextInt(6));
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
        final double dt = 0.5; // la llama se revisa cada medio segundo
        boolean grande = st.getValue(GRANDE);
        double rosCabeza = Fuego.extender(level, pos, r, dt, copa, grande ? 1.15 : 1.0);
        Meteo m = Meteo.en(level, pos);

        // pavesas: con viento fuerte y llamas altas (y mucho más desde las copas)
        if ((grande || copa) && m.viento() > 15) {
            double pav = dt * 0.012 * Math.pow((m.viento() - 15) / 20.0, 2) * (copa ? 4 : 1);
            if (r.nextDouble() < pav) Fuego.pavesa(level, pos, r);
        }
        if (r.nextFloat() < (copa ? 0.3f : grande ? 0.12f : 0.05f)) Fuego.columnaHumo(level, pos, r, copa || grande);

        // tiempo de residencia: la hierba arde unos segundos; si el fuego avanza muy despacio (sin viento, húmedo)
        // la llama se queda más tiempo "reptando"; las copas arden medio minuto
        double tRes = copa ? 30 : 4 + comb * 4;
        if (!copa) tRes = Math.max(tRes, Math.min(240, 1.5 / Math.max(0.006, rosCabeza)));
        int edad = st.getValue(EDAD);
        if (r.nextDouble() < 8 * dt / tRes) edad++;
        if (edad >= 7) {
            consumir(level, pos, copa, r);
            return;
        }
        boolean nuevaGrande = copa || m.alturaLlama(rosCabeza * Math.min(1.25, comb / 1.4)) > 1.6;
        BlockState nuevo = st.setValue(EDAD, edad).setValue(GRANDE, nuevaGrande);
        if (nuevo != st) level.setBlock(pos, nuevo, Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, this, 9 + r.nextInt(3));
    }

    private void consumir(ServerLevel level, BlockPos pos, boolean copa, RandomSource r) {
        // el fuego suelta CO2 y humo a la atmósfera (cambio climático y atardeceres rojos)
        Clima.get(level.getServer()).emitir(copa ? 0.02 : 0.004, copa ? 0.0008 : 0.0002);
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
        boolean grande = st.getValue(GRANDE);
        if (grande) {
            level.addParticle(ParticleTypes.FLAME, pos.getX() + r.nextDouble(), pos.getY() + 1 + r.nextDouble() * 0.8,
                    pos.getZ() + r.nextDouble(), 0, 0.06, 0);
            if (r.nextInt(2) == 0) level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.getX() + 0.5, pos.getY() + 1.8,
                    pos.getZ() + 0.5, 0, 0.07, 0);
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

package es.guerramod.desastres;

import es.guerramod.incendios.Clima;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/**
 * Tornado con la escala Fujita mejorada (EF0..EF5). El viento sigue un vórtice de Rankine: crece hasta el borde del
 * núcleo y luego baja con la distancia; además hay corriente de entrada hacia el centro y corriente ascendente.
 * Arrastra y levanta a los seres vivos, arranca lo que no resiste el viento (plantas, hojas, cristales, vallas, tejados
 * de madera, árboles...) y lanza escombros. Avanza con el viento general dando bandazos y se deshace al cabo de unos minutos.
 */
public class TornadoEntity extends Entity {
    private static final EntityDataAccessor<Integer> FUERZA = SynchedEntityData.defineId(TornadoEntity.class, EntityDataSerializers.INT);
    /** Viento máximo (m/s) para EF0..EF5. */
    private static final double[] VMAX = {32, 42, 54, 66, 80, 96};
    private int vida = 3000, edad = 0;
    private float rumbo = 0;

    public TornadoEntity(EntityType<?> t, Level l) {
        super(t, l);
        noPhysics = true;
    }

    @Override
    protected void defineSynchedData() {
        entityData.define(FUERZA, 1);
    }

    public int fuerza() {
        return Math.max(0, Math.min(5, entityData.get(FUERZA)));
    }

    public void preparar(int ef, float rumbo, RandomSource r) {
        entityData.set(FUERZA, Math.max(0, Math.min(5, ef)));
        this.rumbo = rumbo;
        this.vida = 1600 + r.nextInt(1400) + ef * 400;
    }

    /** Radio del núcleo (bloques = metros). */
    public double radio() {
        return 4 + fuerza() * 3.0;
    }

    /** Viento (m/s) a una distancia del centro. */
    public double viento(double r) {
        double R = radio(), v = VMAX[fuerza()];
        return r < R ? v * r / R : v * R / r;
    }

    @Override
    public void tick() {
        super.tick();
        edad++;
        if (level().isClientSide) {
            particulas();
            return;
        }
        ServerLevel sl = (ServerLevel) level();
        mover(sl);
        arrastrar(sl);
        destrozar(sl);
        if (edad % 30 == 0) {
            sl.playSound(null, getX(), getY() + 4, getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.WEATHER, 5f + fuerza(), 0.45f);
        }
        if (edad > vida) discard();
    }

    private void mover(ServerLevel sl) {
        Clima c = Clima.get(sl.getServer());
        float objetivo = (float) c.angulo;
        float dif = ((objetivo - rumbo + 540) % 360) - 180;
        rumbo += dif * 0.01f + (random.nextFloat() - 0.5f) * 3f;
        double v = 0.32 + 0.06 * fuerza(); // 6..12 m/s
        double nx = getX() + Math.cos(Math.toRadians(rumbo)) * v, nz = getZ() + Math.sin(Math.toRadians(rumbo)) * v;
        int suelo = sl.getHeight(Heightmap.Types.WORLD_SURFACE, (int) Math.floor(nx), (int) Math.floor(nz));
        double ny = getY() + (suelo - getY()) * 0.2;
        setPos(nx, ny, nz);
    }

    private void arrastrar(ServerLevel sl) {
        double R = radio(), Rext = R * 3.2;
        AABB caja = getBoundingBox().inflate(Rext, 45, Rext);
        for (Entity e : sl.getEntities(this, caja)) {
            if (e instanceof TornadoEntity || e.isSpectator()) continue;
            if (e instanceof Player p && (p.isCreative() || p.isSpectator())) continue;
            double dx = e.getX() - getX(), dz = e.getZ() - getZ();
            double r = Math.max(0.5, Math.sqrt(dx * dx + dz * dz));
            if (r > Rext) continue;
            double vt = viento(r);
            double alto = Math.max(0, e.getY() - getY());
            // giro ciclónico (antihorario), entrada hacia el centro y ascenso cerca del núcleo
            double tx = -dz / r, tz = dx / r;
            double ex = tx * vt - dx / r * vt * 0.35, ez = tz * vt - dz / r * vt * 0.35;
            double w = VMAX[fuerza()] * 0.32 * Math.max(0, 1 - r / Rext) * Math.max(0, 1 - alto / 45.0);
            double k = e instanceof LivingEntity ? 0.6 : 0.9;
            Vec3 m = e.getDeltaMovement();
            double mx = m.x + (ex / 20 * k - m.x) * 0.16, mz = m.z + (ez / 20 * k - m.z) * 0.16;
            double my = m.y + (w / 20 * k * 0.6 - m.y) * 0.12;
            e.setDeltaMovement(mx, my, mz);
            e.hurtMarked = true;
            if (vt > 38 && e instanceof LivingEntity && random.nextFloat() < 0.04f * (vt / 40)) {
                e.hurt(sl.damageSources().flyIntoWall(), (float) (1 + (vt - 38) / 12));
            }
        }
    }

    /** Viento (m/s) que aguanta cada bloque antes de romperse o salir volando. */
    private static double resistencia(BlockState s) {
        if (s.is(BlockTags.LEAVES)) return 20;
        if (s.canBeReplaced() || s.is(BlockTags.FLOWERS) || s.is(BlockTags.SAPLINGS) || s.is(Blocks.SNOW)) return 16;
        if (s.is(Blocks.GLASS) || s.is(Blocks.GLASS_PANE) || s.is(BlockTags.IMPERMEABLE)) return 34;
        if (s.is(BlockTags.WOOL) || s.is(BlockTags.WOOL_CARPETS) || s.is(BlockTags.BEDS) || s.is(Blocks.TORCH) || s.is(Blocks.LANTERN)) return 30;
        if (s.is(BlockTags.FENCES) || s.is(BlockTags.DOORS) || s.is(BlockTags.TRAPDOORS) || s.is(BlockTags.WOODEN_SLABS)
                || s.is(BlockTags.WOODEN_STAIRS) || s.is(BlockTags.PLANKS) || s.is(BlockTags.FENCE_GATES)) return 50;
        if (s.is(BlockTags.LOGS)) return 63;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)) return 70;
        float dureza = s.getBlock().defaultDestroyTime();
        if (dureza < 0 || dureza > 40) return 9999;
        return 55 + dureza * 14;
    }

    private void destrozar(ServerLevel sl) {
        double R = radio();
        int n = 10 + fuerza() * 14, lanzados = 0;
        for (int i = 0; i < n; i++) {
            double a = random.nextDouble() * Math.PI * 2, r = R * 1.7 * Math.sqrt(random.nextDouble());
            int x = (int) Math.floor(getX() + Math.cos(a) * r), z = (int) Math.floor(getZ() + Math.sin(a) * r);
            int top = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
            BlockPos p = new BlockPos(x, top - (random.nextFloat() < 0.3f ? random.nextInt(3) : 0), z);
            BlockState s = sl.getBlockState(p);
            if (s.isAir() || !s.getFluidState().isEmpty()) continue;
            double v = viento(Math.max(0.5, r)) * (0.8 + 0.4 * random.nextDouble());
            if (v < resistencia(s)) continue;
            if (s.is(Blocks.GRASS_BLOCK)) {
                sl.setBlockAndUpdate(p, Blocks.DIRT.defaultBlockState()); // el viento arranca la hierba
                continue;
            }
            if (lanzados < 3 && !s.hasBlockEntity() && !s.is(BlockTags.LEAVES) && !s.canBeReplaced()) {
                FallingBlockEntity fb = FallingBlockEntity.fall(sl, p, s);
                double tx = -Math.sin(a), tz = Math.cos(a);
                fb.setDeltaMovement(tx * v / 40, 0.5 + random.nextDouble() * 0.9, tz * v / 40);
                fb.dropItem = false;
                lanzados++;
            } else {
                sl.destroyBlock(p, false);
            }
        }
    }

    private void particulas() {
        Level l = level();
        RandomSource r = random;
        double R = radio();
        final double H = 75;
        int n = 14 + fuerza() * 5;
        for (int i = 0; i < n; i++) {
            double h = r.nextDouble() * H;
            double f = h / H;
            double rf = R * (0.3 + 1.1 * Math.pow(f, 1.6)) * (0.85 + 0.3 * r.nextDouble());
            double ang = edad * (0.32 - f * 0.12) + r.nextDouble() * Math.PI * 2;
            double px = getX() + Math.cos(ang) * rf, pz = getZ() + Math.sin(ang) * rf;
            double vx = -Math.sin(ang) * 0.35, vz = Math.cos(ang) * 0.35;
            if (f < 0.12) {
                BlockState suelo = l.getBlockState(BlockPos.containing(px, getY() - 1, pz));
                if (!suelo.isAir()) l.addAlwaysVisibleParticle(new BlockParticleOption(ParticleTypes.BLOCK, suelo), true, px, getY() + h, pz, vx * 3, 0.4, vz * 3);
                l.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, true, px, getY() + h, pz, vx, 0.05, vz);
            } else {
                l.addAlwaysVisibleParticle(r.nextBoolean() ? ParticleTypes.CAMPFIRE_COSY_SMOKE : ParticleTypes.LARGE_SMOKE, true,
                        px, getY() + h, pz, vx, 0.08, vz);
            }
        }
        // nube de pared en lo alto
        if (r.nextInt(2) == 0) {
            double a = r.nextDouble() * Math.PI * 2, rr = R * (2 + r.nextDouble() * 2.5);
            l.addAlwaysVisibleParticle(ParticleTypes.CAMPFIRE_SIGNAL_SMOKE, true, getX() + Math.cos(a) * rr, getY() + H, getZ() + Math.sin(a) * rr,
                    -Math.sin(a) * 0.2, 0.01, Math.cos(a) * 0.2);
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        entityData.set(FUERZA, t.getInt("fuerza"));
        vida = t.getInt("vida");
        edad = t.getInt("edad");
        rumbo = t.getFloat("rumbo");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putInt("fuerza", fuerza());
        t.putInt("vida", vida);
        t.putInt("edad", edad);
        t.putFloat("rumbo", rumbo);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d) {
        return true;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}

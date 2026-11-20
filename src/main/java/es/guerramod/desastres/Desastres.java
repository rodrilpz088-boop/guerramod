package es.guerramod.desastres;

import es.guerramod.clima.Zonas;
import es.guerramod.incendios.Clima;
import es.guerramod.incendios.Meteo;
import es.guerramod.red.Red;
import es.guerramod.registro.ModEntidades;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Desastres naturales: tornados (en tormentas, EF0-EF5), terremotos (magnitud con la ley de Gutenberg-Richter, la
 * intensidad baja con la distancia al epicentro), granizo (en tormentas; daña a quien esté a la intemperie, cultivos y
 * cristales), riadas (después de mucha lluvia los ríos y lagos se desbordan y luego bajan dejando barro) y olas de calor.
 * Pueden pasar solos (/desastre naturales) o provocarse con /desastre.
 */
public final class Desastres {
    private Desastres() {}

    // ------------------------------------------------------------------ estado
    private record Terremoto(BlockPos epicentro, double magnitud, int duracion, int[] t) {}

    private static final List<Terremoto> TERREMOTOS = new ArrayList<>();
    public static int granizoTicks = 0;
    public static float granizoCm = 0;

    /** Riada: posiciones del agua puesta por el desbordamiento (para quitarla al bajar). */
    public static final class Riada extends SavedData {
        public final LongArrayList agua = new LongArrayList();
        public int nivel = 0;
        public int lluvia = 0;

        public static Riada get(MinecraftServer s) {
            return s.overworld().getDataStorage().computeIfAbsent(Riada::cargar, Riada::new, "guerramod_riada");
        }

        static Riada cargar(CompoundTag t) {
            Riada r = new Riada();
            for (long l : t.getLongArray("agua")) r.agua.add(l);
            r.nivel = t.getInt("nivel");
            r.lluvia = t.getInt("lluvia");
            return r;
        }

        @Override
        public CompoundTag save(CompoundTag t) {
            t.put("agua", new LongArrayTag(agua.toLongArray()));
            t.putInt("nivel", nivel);
            t.putInt("lluvia", lluvia);
            return t;
        }
    }

    // ------------------------------------------------------------------ avisos
    public static String rumbo(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = norte, 90 = este
        String[] n = {"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};
        return n[(int) Math.floorMod(Math.round(a / 45.0), 8)];
    }

    public static void alerta(ServerLevel l, BlockPos donde, double radio, String titulo, String detalle) {
        for (ServerPlayer p : l.players()) {
            if (radio > 0 && p.blockPosition().distSqr(donde) > radio * radio) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(10, 70, 20));
            p.connection.send(new ClientboundSetTitleTextPacket(Component.literal(titulo).withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(detalle).withStyle(ChatFormatting.GOLD)));
            p.sendSystemMessage(Component.literal("⚠ " + titulo + ": " + detalle).withStyle(ChatFormatting.RED));
            p.playNotifySound(SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.MASTER, 1f, 0.6f);
        }
    }

    // ------------------------------------------------------------------ provocar
    public static TornadoEntity tornado(ServerLevel l, BlockPos cerca, int ef) {
        RandomSource r = l.random;
        Clima c = Clima.get(l.getServer());
        // aparece a barlovento y viene hacia ti
        double ang = Math.toRadians(c.angulo + 180 + (r.nextDouble() - 0.5) * 60);
        double d = 70 + r.nextDouble() * 50;
        int x = (int) (cerca.getX() + Math.cos(ang) * d), z = (int) (cerca.getZ() + Math.sin(ang) * d);
        int y = l.getHeight(Heightmap.Types.WORLD_SURFACE, x, z);
        TornadoEntity t = ModEntidades.TORNADO.get().create(l);
        if (t == null) return null;
        t.moveTo(x + 0.5, y, z + 0.5, 0, 0);
        t.preparar(ef, (float) c.angulo, r);
        l.addFreshEntity(t);
        alerta(l, cerca, 400, "Aviso de tornado EF" + ef, "a " + (int) d + " m al " + rumbo(x - cerca.getX(), z - cerca.getZ())
                + ", viene hacia aquí. ¡Busca refugio bajo tierra!");
        return t;
    }

    public static void terremoto(ServerLevel l, BlockPos epicentro, double m) {
        int dur = (int) (20 * (6 + (m - 3) * 5));
        TERREMOTOS.add(new Terremoto(epicentro, m, dur, new int[]{0}));
        BlockPos ref = l.players().isEmpty() ? epicentro : l.players().get(0).blockPosition();
        alerta(l, epicentro, 0, String.format("Terremoto de magnitud %.1f", m), "epicentro a " + (int) Math.sqrt(ref.distSqr(epicentro)) + " m al "
                + rumbo(epicentro.getX() - ref.getX(), epicentro.getZ() - ref.getZ()));
        if (m >= 6.8) grieta(l, epicentro, m);
    }

    public static void granizo(ServerLevel l, float cm, int ticks) {
        granizoCm = cm;
        granizoTicks = ticks;
        if (!l.isRaining()) l.setWeatherParameters(0, ticks + 1200, true, true);
        alerta(l, BlockPos.ZERO, 0, "Granizada", String.format("piedras de %.0f cm: ponte a cubierto", cm));
    }

    public static int riada(ServerLevel l) {
        return Inundacion.provocar(l);
    }

    public static void olaCalor(ServerLevel l, double dias) {
        Clima c = Clima.get(l.getServer());
        c.extraCalor = 7 + l.random.nextDouble() * 4;
        c.extraCalorTicks = (int) (dias * 24000);
        c.setDirty();
        alerta(l, BlockPos.ZERO, 0, "Ola de calor", String.format("+%.0f °C durante %.0f días: riesgo extremo de incendio", c.extraCalor, dias));
    }

    public static void parar(ServerLevel l) {
        TERREMOTOS.clear();
        granizoTicks = 0;
        Inundacion.parar(l);
        Clima c = Clima.get(l.getServer());
        c.extraCalorTicks = 0;
        c.extraCalor = 0;
        for (TornadoEntity t : l.getEntities(ModEntidades.TORNADO.get(), e -> true)) t.discard();
        for (ServerPlayer p : l.players()) Red.enviarTemblor(p, 0);
    }

    // ------------------------------------------------------------------ cada tick
    public static void tick(MinecraftServer server) {
        ServerLevel l = server.overworld();
        RandomSource r = l.random;
        tickTerremotos(l, r);
        tickGranizo(l, r);
        Inundacion.tick(l);
        Clima c = Clima.get(server);
        if (c.extraCalorTicks > 0 && --c.extraCalorTicks == 0) {
            c.extraCalor = 0;
            c.setDirty();
        }
        if (server.getTickCount() % 600 == 0 && c.desastres && !l.players().isEmpty()) naturales(l, r, c);
    }

    private static void naturales(ServerLevel l, RandomSource r, Clima c) {
        ServerPlayer p = l.players().get(r.nextInt(l.players().size()));
        BlockPos pos = p.blockPosition();
        Meteo m = Meteo.en(l, pos);
        Zonas.Zona z = Zonas.de(l.getBiome(pos));
        boolean tornados = l.getEntitiesOfClass(TornadoEntity.class, p.getBoundingBox().inflate(600)).isEmpty();
        if (l.isThundering()) {
            if (tornados && !z.fria() && m.temperatura() > 14 && !l.getBiome(pos).is(BiomeTags.IS_OCEAN) && r.nextFloat() < 0.035f) {
                float x = r.nextFloat();
                int ef = x < 0.4f ? 0 : x < 0.7f ? 1 : x < 0.87f ? 2 : x < 0.95f ? 3 : x < 0.99f ? 4 : 5;
                tornado(l, pos, ef);
            }
            if (granizoTicks <= 0 && m.temperatura() > 3 && m.temperatura() < 32 && r.nextFloat() < 0.05f) {
                granizo(l, (float) Math.min(8, 1 + -Math.log(1 - r.nextDouble()) * 1.3), 1200 + r.nextInt(2400));
            }
        }
        if (r.nextFloat() < 1f / 250f) {
            // Gutenberg-Richter: los grandes son mucho más raros
            double mag = Math.min(8.6, 3.0 - Math.log10(1 - r.nextDouble()) * 1.0);
            double a = r.nextDouble() * Math.PI * 2, d = 50 + r.nextDouble() * 400;
            terremoto(l, pos.offset((int) (Math.cos(a) * d), 0, (int) (Math.sin(a) * d)), mag);
        }
        if (!l.isRaining() && c.extraCalorTicks <= 0 && c.diasSinLluvia(l) > 2 && m.temperatura() > 24 && r.nextFloat() < 1f / 400f) {
            olaCalor(l, 2 + r.nextInt(3));
        }
    }

    // ------------------------------------------------------------------ terremotos
    private static void tickTerremotos(ServerLevel l, RandomSource r) {
        Iterator<Terremoto> it = TERREMOTOS.iterator();
        while (it.hasNext()) {
            Terremoto q = it.next();
            int t = ++q.t[0];
            double prog = (double) t / q.duracion;
            double envolvente = Math.min(1, t / 30.0) * Math.pow(Math.max(0, 1 - prog), 0.6);
            if (t % 4 == 0) {
                for (ServerPlayer p : l.players()) {
                    double d = Math.sqrt(p.blockPosition().distSqr(q.epicentro));
                    double intensidad = 1.5 * q.magnitud - 2.6 * Math.log10(1 + d / 30.0) - 1.5; // escala tipo Mercalli
                    float temblor = (float) (Math.max(0, Math.min(1, (intensidad - 2) / 6.0)) * envolvente);
                    Red.enviarTemblor(p, temblor);
                    if (temblor > 0.05f && t % 20 == 0) {
                        l.playSound(null, p.blockPosition(), SoundEvents.GENERIC_EXPLODE, SoundSource.AMBIENT, 0.35f * temblor, 0.3f);
                    }
                }
            }
            danos(l, r, q, envolvente);
            if (t >= q.duracion) {
                for (ServerPlayer p : l.players()) Red.enviarTemblor(p, 0);
                it.remove();
            }
        }
    }

    private static void danos(ServerLevel l, RandomSource r, Terremoto q, double env) {
        double m = q.magnitud;
        if (m < 4.5) return;
        double radio = 8 * Math.pow(10, 0.3 * (m - 4));
        int k = (int) Math.min(140, 6 * (m - 3.5) * (m - 3.5));
        int caidas = 0;
        for (int i = 0; i < k; i++) {
            double a = r.nextDouble() * Math.PI * 2, d = radio * Math.sqrt(r.nextDouble());
            int x = q.epicentro.getX() + (int) (Math.cos(a) * d), z = q.epicentro.getZ() + (int) (Math.sin(a) * d);
            if (!l.hasChunkAt(new BlockPos(x, 64, z))) continue;
            int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            BlockPos p = new BlockPos(x, top - 1 - r.nextInt(18), z);
            BlockState s = l.getBlockState(p);
            if (s.isAir()) continue;
            double fuerza = Math.max(0, Math.min(1, (m - 4.5) / 3.0)) * (1 - d / radio) * env;
            if (r.nextDouble() > fuerza) continue;
            if (s.is(BlockTags.IMPERMEABLE) || s.is(Blocks.GLASS_PANE) || s.getBlock() instanceof net.minecraft.world.level.block.StainedGlassPaneBlock) {
                l.destroyBlock(p, false);
            } else if (s.is(Blocks.STONE_BRICKS)) {
                l.setBlockAndUpdate(p, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
            } else if (s.is(Blocks.TORCH) || s.is(Blocks.LANTERN) || s.is(Blocks.WALL_TORCH) || s.is(BlockTags.FLOWER_POTS)) {
                l.destroyBlock(p, true);
            } else if (caidas < 6 && !s.hasBlockEntity() && !s.is(BlockTags.LOGS) && !s.is(BlockTags.LEAVES) && s.getFluidState().isEmpty()
                    && s.getBlock().defaultDestroyTime() >= 0 && s.getBlock().defaultDestroyTime() < 30) {
                // derrumbes: lo que queda sin apoyo se cae; en laderas la tierra se desliza
                boolean sinApoyo = l.getBlockState(p.below()).isAir();
                boolean ladera = false;
                if (s.is(BlockTags.DIRT) || s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)) {
                    for (Direction dd : Direction.Plane.HORIZONTAL) {
                        if (l.getBlockState(p.relative(dd)).isAir() && l.getBlockState(p.relative(dd).below()).isAir()) ladera = true;
                    }
                }
                if ((sinApoyo && m >= 6) || ladera) {
                    FallingBlockEntity.fall(l, p, s);
                    caidas++;
                }
            }
        }
    }

    private static void grieta(ServerLevel l, BlockPos epi, double m) {
        RandomSource r = l.random;
        double ang = r.nextDouble() * Math.PI;
        int largo = (int) ((m - 6.5) * 45);
        double x = epi.getX() - Math.cos(ang) * largo / 2.0, z = epi.getZ() - Math.sin(ang) * largo / 2.0;
        for (int i = 0; i < largo; i++) {
            ang += (r.nextDouble() - 0.5) * 0.25;
            x += Math.cos(ang);
            z += Math.sin(ang);
            double centro = 1 - Math.abs(i - largo / 2.0) / (largo / 2.0);
            int prof = (int) (2 + centro * (m - 6.3) * 5 * (0.6 + 0.4 * r.nextDouble()));
            int ancho = centro > 0.4 ? 2 : 1;
            for (int w = 0; w < ancho; w++) {
                int bx = (int) Math.floor(x + w * Math.sin(ang)), bz = (int) Math.floor(z - w * Math.cos(ang));
                int top = l.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
                for (int y = top - 1; y >= top - prof; y--) {
                    BlockPos p = new BlockPos(bx, y, bz);
                    BlockState s = l.getBlockState(p);
                    if (s.is(Blocks.BEDROCK) || !s.getFluidState().isEmpty() || s.hasBlockEntity()) break;
                    l.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
        }
    }

    // ------------------------------------------------------------------ granizo
    private static void tickGranizo(ServerLevel l, RandomSource r) {
        if (granizoTicks <= 0) return;
        granizoTicks--;
        if (!l.isRaining()) {
            granizoTicks = 0;
            return;
        }
        if (granizoTicks % 10 != 0) return;
        float cm = granizoCm;
        for (ServerPlayer jugador : l.players()) {
            for (LivingEntity e : l.getEntitiesOfClass(LivingEntity.class, jugador.getBoundingBox().inflate(48, 24, 48))) {
                BlockPos cab = BlockPos.containing(e.getX(), e.getEyeY(), e.getZ());
                if (!l.isRainingAt(cab.above()) || r.nextFloat() > 0.4f) continue;
                if (e instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())) continue;
                float dano = (float) (0.15 * Math.pow(cm, 1.6));
                if (!e.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) dano *= 0.3f;
                if (dano >= 0.2f) e.hurt(l.damageSources().fallingBlock(e), dano);
            }
            for (int i = 0; i < 8; i++) {
                int x = jugador.getBlockX() + r.nextInt(65) - 32, z = jugador.getBlockZ() + r.nextInt(65) - 32;
                BlockPos aire = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
                if (!l.isRainingAt(aire)) continue;
                BlockState s = l.getBlockState(aire);
                BlockState abajo = l.getBlockState(aire.below());
                if (s.getBlock() instanceof CropBlock cb) {
                    if (cm >= 3) l.destroyBlock(aire, true);
                    else l.setBlockAndUpdate(aire, cb.getStateForAge(0));
                } else if (cm >= 4 && (abajo.is(Blocks.GLASS) || abajo.is(Blocks.GLASS_PANE) || abajo.is(BlockTags.IMPERMEABLE))) {
                    l.destroyBlock(aire.below(), false);
                } else if (s.isAir() && abajo.isFaceSturdy(l, aire.below(), Direction.UP) && r.nextFloat() < 0.35f) {
                    // el granizo se acumula (y luego se derrite)
                    l.setBlockAndUpdate(aire, Blocks.SNOW.defaultBlockState().setValue(SnowLayerBlock.LAYERS, 1));
                }
            }
        }
    }

}

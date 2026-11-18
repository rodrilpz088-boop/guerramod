package es.guerramod.bloques;

import es.guerramod.entidades.AeronaveBase;
import es.guerramod.entidades.MisilEntity;
import es.guerramod.items.DesignadorItem;
import es.guerramod.pais.ComandoPais;
import es.guerramod.pais.Gobierno;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.registro.ModItems;
import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Bloques pequeños del mod. */
public final class BloquesVarios {
    private BloquesVarios() {}

    /** Bloque que mira hacia una dirección (líneas de carretera, etc.). */
    public static class BloqueOrientable extends HorizontalDirectionalBlock {
        private final boolean mirarJugador;

        public BloqueOrientable(Properties p, boolean mirarJugador) {
            super(p);
            this.mirarJugador = mirarJugador;
            registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        }

        @Override
        protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
            b.add(FACING);
        }

        @Override
        public BlockState getStateForPlacement(BlockPlaceContext ctx) {
            Direction d = ctx.getHorizontalDirection();
            return defaultBlockState().setValue(FACING, mirarJugador ? d.getOpposite() : d);
        }
    }

    /** Bloque con una forma más pequeña que un cubo (hitos, farolas). */
    public static class BloqueForma extends Block {
        private final VoxelShape forma;

        public BloqueForma(Properties p, double x1, double y1, double z1, double x2, double y2, double z2) {
            super(p);
            this.forma = Block.box(x1, y1, z1, x2, y2, z2);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return forma;
        }
    }

    /** Alambre de espino: frena y hace daño. */
    public static class AlambreEspino extends Block {
        public AlambreEspino(Properties p) {
            super(p);
        }

        @Override
        public void entityInside(BlockState s, Level l, BlockPos p, Entity e) {
            if (e instanceof LivingEntity le) {
                e.makeStuckInBlock(s, new Vec3(0.45, 0.6, 0.45));
                if (!l.isClientSide && le.tickCount % 10 == 0) le.hurt(l.damageSources().cactus(), 1.5f);
            }
        }
    }

    /** Mina terrestre: explota al pisarla. */
    public static class Mina extends Block {
        private static final VoxelShape FORMA = Block.box(2, 0, 2, 14, 2, 14);

        public Mina(Properties p) {
            super(p);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return FORMA;
        }

        @Override
        public void entityInside(BlockState s, Level l, BlockPos p, Entity e) {
            if (!l.isClientSide && e instanceof LivingEntity && !e.isShiftKeyDown()) {
                l.removeBlock(p, false);
                l.explode(null, p.getX() + 0.5, p.getY() + 0.2, p.getZ() + 0.5, 3.0f, Level.ExplosionInteraction.TNT);
            }
        }
    }

    /** Sirena antiaérea: suena mientras recibe redstone o al hacer clic. */
    public static class Sirena extends Block {
        public Sirena(Properties p) {
            super(p);
        }

        @Override
        public void neighborChanged(BlockState s, Level l, BlockPos p, Block b, BlockPos from, boolean moving) {
            if (!l.isClientSide && l.hasNeighborSignal(p) && !l.getBlockTicks().hasScheduledTick(p, this)) {
                l.scheduleTick(p, this, 1);
            }
        }

        @Override
        public void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
            if (l.hasNeighborSignal(p)) {
                sonar(l, p);
                l.scheduleTick(p, this, 60);
            }
        }

        private static void sonar(Level l, BlockPos p) {
            l.playSound(null, p, ModSonidos.SIRENA.get(), SoundSource.BLOCKS, 8.0f, 1.0f);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (!l.isClientSide) sonar(l, p);
            return InteractionResult.sidedSuccess(l.isClientSide);
        }
    }

    /** Radar: muestra aviones y misiles cercanos. */
    public static class Radar extends Block {
        public Radar(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (l.isClientSide) return InteractionResult.SUCCESS;
            List<Entity> lista = l.getEntitiesOfClass(Entity.class, new net.minecraft.world.phys.AABB(p).inflate(160),
                    e -> e instanceof AeronaveBase || e instanceof MisilEntity);
            l.playSound(null, p, SoundEvents.NOTE_BLOCK_BIT.value(), SoundSource.BLOCKS, 1.0f, 1.6f);
            if (lista.isEmpty()) {
                pl.sendSystemMessage(Component.literal("Radar: cielo despejado (160 bloques)").withStyle(ChatFormatting.GREEN));
                return InteractionResult.CONSUME;
            }
            pl.sendSystemMessage(Component.literal("Radar: " + lista.size() + " contactos").withStyle(ChatFormatting.YELLOW));
            for (Entity e : lista) {
                double dx = e.getX() - p.getX(), dz = e.getZ() - p.getZ();
                int dist = (int) Math.sqrt(dx * dx + dz * dz);
                String tipo = e instanceof MisilEntity ? "§cMISIL" : "§6Aeronave";
                pl.sendSystemMessage(Component.literal(" " + tipo + "§7 a " + dist + " m, rumbo " + rumbo(dx, dz)
                        + ", altura " + (int) e.getY()));
            }
            return InteractionResult.CONSUME;
        }

        private static String rumbo(double dx, double dz) {
            double ang = Math.toDegrees(Math.atan2(dx, -dz));
            if (ang < 0) ang += 360;
            String[] d = {"N", "NE", "E", "SE", "S", "SO", "O", "NO"};
            return d[(int) Math.round(ang / 45.0) % 8];
        }
    }

    /** Mesa de gobierno: muestra el gobierno del país en el que está. */
    public static class MesaGobierno extends Block {
        public MesaGobierno(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (l.isClientSide) return InteractionResult.SUCCESS;
            Pais pais = RegistroPaises.de(l).dueno(l, p);
            if (pais == null) {
                pl.sendSystemMessage(Component.literal("Esta mesa está en territorio libre. Funda un país con /pais crear.")
                        .withStyle(ChatFormatting.GRAY));
            } else {
                pl.sendSystemMessage(ComandoPais.ficha(pais));
            }
            return InteractionResult.CONSUME;
        }
    }

    /** Urna electoral: el gobierno puede convocar elecciones. */
    public static class Urna extends Block {
        private static final VoxelShape FORMA = Block.box(3, 0, 3, 13, 12, 13);

        public Urna(Properties p) {
            super(p);
        }

        @Override
        public VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) {
            return FORMA;
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (l.isClientSide) return InteractionResult.SUCCESS;
            RegistroPaises r = RegistroPaises.de(l);
            Pais pais = r.dueno(l, p);
            if (pais == null) {
                pl.sendSystemMessage(Component.literal("La urna debe estar dentro de un país.").withStyle(ChatFormatting.RED));
            } else if (!pais.puedeGobernar(pl.getUUID())) {
                pl.sendSystemMessage(Component.literal("Solo el gobierno de ").append(pais.nombreColoreado())
                        .append(" puede convocar elecciones."));
            } else {
                l.playSound(null, p, SoundEvents.VILLAGER_CELEBRATE, SoundSource.BLOCKS, 1.0f, 1.0f);
                Gobierno.elecciones((ServerLevel) l, pais, Vec3.atCenterOf(p));
            }
            return InteractionResult.CONSUME;
        }
    }

    /** Puesto de control: registro de quién ha cruzado la frontera. */
    public static class PuestoControl extends Block {
        public PuestoControl(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            if (l.isClientSide) return InteractionResult.SUCCESS;
            Pais pais = RegistroPaises.de(l).dueno(l, p);
            if (pais == null) {
                pl.sendSystemMessage(Component.literal("Coloca el puesto dentro de tu país.").withStyle(ChatFormatting.GRAY));
            } else if (!pais.esMiembro(pl.getUUID())) {
                pl.sendSystemMessage(Component.literal("Control fronterizo de ").append(pais.nombreColoreado())
                        .append(Component.literal(". Muestre su pasaporte.").withStyle(ChatFormatting.GRAY)));
            } else if (pais.registroFronteras.isEmpty()) {
                pl.sendSystemMessage(Component.literal("Nadie ha cruzado la frontera todavía.").withStyle(ChatFormatting.GRAY));
            } else {
                MutableComponent t = Component.literal("Últimos cruces de frontera:").withStyle(ChatFormatting.GOLD);
                for (String e : pais.registroFronteras) t.append(Component.literal("\n §7" + e));
                pl.sendSystemMessage(t);
            }
            return InteractionResult.CONSUME;
        }
    }

    /** Lanzador de misiles balísticos: se usa con el designador láser. */
    public static class LanzadorMisiles extends Block {
        public LanzadorMisiles(Properties p) {
            super(p);
        }

        @Override
        public InteractionResult use(BlockState s, Level l, BlockPos p, Player pl, InteractionHand h, BlockHitResult hit) {
            ItemStack mano = pl.getItemInHand(h);
            if (!(mano.getItem() instanceof DesignadorItem)) {
                if (!l.isClientSide) pl.sendSystemMessage(Component.literal(
                        "Marca un objetivo con el Designador láser y haz clic derecho aquí con él. Necesitas un Misil balístico o un Misil nuclear en el inventario.")
                        .withStyle(ChatFormatting.GRAY));
                return InteractionResult.sidedSuccess(l.isClientSide);
            }
            if (l.isClientSide) return InteractionResult.SUCCESS;
            BlockPos objetivo = DesignadorItem.getObjetivo(mano);
            if (objetivo == null) {
                pl.sendSystemMessage(Component.literal("El designador no tiene objetivo. Apunta lejos y haz clic derecho.").withStyle(ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
            ItemStack municion = buscarMisil(pl);
            if (municion == null && !pl.getAbilities().instabuild) {
                pl.sendSystemMessage(Component.literal("No tienes misiles.").withStyle(ChatFormatting.RED));
                return InteractionResult.CONSUME;
            }
            boolean nuclear = municion != null && municion.is(ModItems.MISIL_NUCLEAR.get());
            ItemStack visual = new ItemStack(nuclear ? ModItems.MISIL_NUCLEAR.get() : ModItems.MISIL_BALISTICO.get());
            if (municion != null && !pl.getAbilities().instabuild) municion.shrink(1);
            MisilEntity m = new MisilEntity(l, pl, objetivo, nuclear);
            m.setItem(visual);
            m.setPos(p.getX() + 0.5, p.getY() + 1.5, p.getZ() + 0.5);
            l.addFreshEntity(m);
            l.playSound(null, p, SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.BLOCKS, 4.0f, 0.5f);
            l.playSound(null, p, ModSonidos.ALARMA.get(), SoundSource.BLOCKS, 4.0f, 1.0f);
            Pais atacante = RegistroPaises.de(l).paisDeJugador(pl.getUUID());
            Pais victima = RegistroPaises.de(l).dueno(l, objetivo);
            if (victima != null && victima != atacante && l instanceof ServerLevel sl) {
                Gobierno.avisarPais(sl.getServer(), victima, Component.literal("⚠ ¡ALERTA! Misil " + (nuclear ? "NUCLEAR " : "")
                        + "lanzado hacia " + objetivo.getX() + ", " + objetivo.getZ()).withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD));
            }
            pl.sendSystemMessage(Component.literal("Misil lanzado hacia " + objetivo.toShortString()).withStyle(ChatFormatting.GOLD));
            if (pl instanceof ServerPlayer sp) sp.getCooldowns().addCooldown(mano.getItem(), 40);
            return InteractionResult.CONSUME;
        }

        private static ItemStack buscarMisil(Player pl) {
            ItemStack off = pl.getOffhandItem();
            if (off.is(ModItems.MISIL_NUCLEAR.get()) || off.is(ModItems.MISIL_BALISTICO.get())) return off;
            for (ItemStack st : pl.getInventory().items) if (st.is(ModItems.MISIL_BALISTICO.get())) return st;
            for (ItemStack st : pl.getInventory().items) if (st.is(ModItems.MISIL_NUCLEAR.get())) return st;
            return null;
        }
    }
}

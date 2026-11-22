package es.guerramod.clima;

import es.guerramod.GuerraMod;
import es.guerramod.incendios.Clima;
import es.guerramod.incendios.Meteo;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Frío peligroso para la vida. Cuenta la sensación térmica (temperatura + viento). Por debajo de -10 °C a la intemperie
 * el cuerpo se va congelando (escarcha en la pantalla, lentitud) y por debajo de -27 °C la congelación hace daño. Te
 * salvas bajo techo, junto a un fuego (hoguera, horno encendido, lava) o bien abrigado (cuero: cada pieza abriga). Los
 * aldeanos corren a su casa y los animales también sufren.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Congelacion {
    private Congelacion() {}

    /** Abrigo de 0 (nada) a 1 (totalmente abrigado). */
    public static double abrigo(LivingEntity e) {
        double a = 0;
        for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack st = e.getItemBySlot(s);
            if (st.isEmpty()) continue;
            if (st.getItem() instanceof ArmorItem ai) {
                boolean cuero = ai.getMaterial() == ArmorMaterials.LEATHER;
                String n = ai.getMaterial().getName();
                boolean caliente = n.contains("bombero") || n.contains("geo") || n.contains("policia") || n.contains("raquetas") || n.contains("esquis");
                a += cuero ? 0.24 : caliente ? 0.15 : 0.06;
            }
        }
        if (e instanceof Animal) a += 0.45; // pelo y grasa
        return Math.min(0.96, a);
    }

    /** ¿Hay una fuente de calor a menos de 4 bloques? */
    public static boolean calorCerca(Level l, BlockPos p) {
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-4, -2, -4), p.offset(4, 2, 4))) {
            BlockState s = l.getBlockState(q);
            if (s.is(BlockTags.FIRE) || s.is(Blocks.LAVA) || s.is(Blocks.MAGMA_BLOCK)) return true;
            if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) return true;
            if (s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(AbstractFurnaceBlock.LIT)) return true;
            var k = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(s.getBlock());
            if (k != null && k.getNamespace().equals(GuerraMod.MODID) && (k.getPath().equals("llama") || k.getPath().contains("chimenea")
                    || k.getPath().contains("radiador") || k.getPath().contains("caldera") || k.getPath().equals("estufa"))) return true;
        }
        return false;
    }

    /** Bajo techo: no ve el cielo y hay techo sólido cerca encima. */
    public static boolean refugio(Level l, LivingEntity e) {
        BlockPos ojo = BlockPos.containing(e.getEyePosition());
        return !l.canSeeSky(ojo);
    }

    @SubscribeEvent
    public static void tick(LivingEvent.LivingTickEvent ev) {
        LivingEntity e = ev.getEntity();
        Level lv = e.level();
        if (lv.isClientSide() || lv.dimension() != Level.OVERWORLD || e.tickCount % 10 != 0) return;
        if (!(e instanceof Player || e instanceof Villager || e instanceof Animal)) return;
        if (e instanceof Player p && (p.isCreative() || p.isSpectator())) return;
        if (e.getType().is(net.minecraft.tags.EntityTypeTags.FREEZE_IMMUNE_ENTITY_TYPES)) return;
        ServerLevel l = (ServerLevel) lv;
        BlockPos pos = e.blockPosition();
        Meteo m = Meteo.en(l, pos);
        Clima c = Clima.get(l.getServer());
        boolean fuera = !refugio(l, e);
        double viento = fuera ? c.velocidad : 0;
        double sens = Meteo.sensacion(m.temperatura(), viento);
        if (!fuera) sens += 12; // dentro de casa se está bastante mejor
        boolean calor = calorCerca(l, pos);
        if (calor) sens += 25;
        int req = e.getTicksRequiredToFreeze();
        int f = e.getTicksFrozen();
        if (sens < -10) {
            double peligro = (-10 - sens) / 5.0 * (1 - abrigo(e));
            // Minecraft quita 2 por tick (20 cada medio segundo): sumamos eso más lo que congela el frío
            int nuevo = (int) Math.min(req + 60, f + 20 + Math.round(peligro * 3));
            e.setTicksFrozen(nuevo);
            if (nuevo >= req) {
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, sens < -30 ? 2 : 1, true, false));
                if (sens < -27 && e.tickCount % 40 == 0) {
                    float dano = (float) Math.max(1, Math.min(6, (-27 - sens) / 6 + 1) * (1 - abrigo(e)));
                    e.hurt(l.damageSources().freeze(), dano);
                }
                if (e instanceof Player p && e.tickCount % 200 == 0) {
                    p.displayClientMessage(Component.literal(String.format("❄ Te estás congelando (sensación %.0f °C): busca refugio o un fuego", sens))
                            .withStyle(ChatFormatting.AQUA), true);
                }
            }
        }
        // los aldeanos, con frío peligroso, se van a casa
        if (e instanceof Villager v && fuera && sens < -8 && e.tickCount % 100 == 0) {
            v.getBrain().getMemory(MemoryModuleType.HOME).map(GlobalPos::pos).ifPresent(casa ->
                    v.getBrain().setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(casa, 0.7f, 1)));
        }
    }
}

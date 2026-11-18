package es.guerramod.incendios;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

import javax.annotation.Nullable;
import java.util.List;

/** Estación meteorológica portátil: viento, días sin llover, riesgo de incendio y focos activos cerca. */
public class AnemometroItem extends Item {
    public AnemometroItem(Properties p) {
        super(p);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        if (!level.isClientSide && level.getServer() != null) {
            Clima c = Clima.get(level.getServer());
            BlockPos pos = p.blockPosition();
            int riesgo = c.riesgo(level, pos);
            p.sendSystemMessage(Component.literal("━━ Estación meteorológica ━━").withStyle(ChatFormatting.AQUA));
            Meteo m = Meteo.en((net.minecraft.server.level.ServerLevel) level, pos);
            p.sendSystemMessage(Component.literal(String.format("Temperatura %.0f °C · Humedad %.0f %% · Viento del %s a %.0f km/h",
                    m.temperatura(), m.humedad(), Clima.procedencia(c.angulo), m.viento())));
            p.sendSystemMessage(Component.literal(String.format("Días sin llover: %.1f · Hierba seca: %.0f %% · Humedad de la hierba: %.0f %%",
                    c.diasSinLluvia(level), m.curado(), m.humedadCombustible())));
            double ros = m.rosCabeza();
            p.sendSystemMessage(Component.literal(String.format("Avance previsto en pasto: %.1f m/min a favor del viento, %.1f m/min en contra",
                    ros * 60, m.rosDireccion(-1) * 60)).withStyle(ChatFormatting.GOLD));
            p.sendSystemMessage(Component.literal(String.format("Índice de peligro (GFDI): %.1f", m.indice())).withStyle(ChatFormatting.GRAY));
            p.sendSystemMessage(Component.literal("Riesgo de incendio: ").append(Component.literal(Clima.nombreRiesgo(riesgo))
                    .withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Clima.colorRiesgo(riesgo))).withBold(true))));
            // buscar fuego en 48 bloques
            int n = 0;
            double mejor = Double.MAX_VALUE;
            BlockPos cerca = null;
            for (int x = -48; x <= 48; x += 2) {
                for (int z = -48; z <= 48; z += 2) {
                    int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, pos.getX() + x, pos.getZ() + z) - 1;
                    BlockPos q = new BlockPos(pos.getX() + x, y, pos.getZ() + z);
                    if (Fuego.esFuego(level.getBlockState(q))) {
                        n++;
                        double d = q.distSqr(pos);
                        if (d < mejor) {
                            mejor = d;
                            cerca = q;
                        }
                    }
                }
            }
            if (cerca == null) {
                p.sendSystemMessage(Component.literal("Sin fuegos activos a la vista (48 m)").withStyle(ChatFormatting.GREEN));
            } else {
                p.sendSystemMessage(Component.literal("¡Fuego activo! " + n + "+ focos. El más cercano a " + Math.round(Math.sqrt(mejor))
                        + " m en " + cerca.getX() + ", " + cerca.getZ()).withStyle(ChatFormatting.RED));
            }
            level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.5f, 1.8f);
            p.getCooldowns().addCooldown(this, 20);
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(mano), level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.literal("Clic derecho: viento, sequía y riesgo de incendio").withStyle(ChatFormatting.GRAY));
    }
}

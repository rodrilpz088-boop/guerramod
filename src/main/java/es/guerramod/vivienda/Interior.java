package es.guerramod.vivienda;

import es.guerramod.GuerraMod;
import es.guerramod.incendios.Clima;
import es.guerramod.incendios.Meteo;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Temperatura dentro de una casa. El edificio amortigua el frío y el calor de fuera; los radiadores (más con caldera),
 * la chimenea encendida, las hogueras y los hornos calientan; el aire acondicionado encendido enfría. Si hay un
 * termostato cerca, la calefacción y el aire se paran al llegar a la temperatura que marque.
 */
public final class Interior {
    private Interior() {}

    public record Lectura(double exterior, double interior, boolean dentro, int radiadores, int chimeneas, int aires, boolean caldera,
                          int consigna, String estado, double humedad, double viento, double sensacion) {}

    private static String id(BlockState s) {
        ResourceLocation k = ForgeRegistries.BLOCKS.getKey(s.getBlock());
        return k != null && k.getNamespace().equals(GuerraMod.MODID) ? k.getPath() : "";
    }

    private static boolean encendido(BlockState s) {
        return !s.hasProperty(BlockStateProperties.LIT) || s.getValue(BlockStateProperties.LIT);
    }

    public static Lectura leer(ServerLevel l, BlockPos p) {
        Meteo m = Meteo.en(l, p);
        double ext = m.temperatura();
        Clima c = Clima.get(l.getServer());
        boolean dentro = !l.canSeeSky(p) && !l.canSeeSky(p.above());
        double sensExt = Meteo.sensacion(ext, c.velocidad);
        if (!dentro) return new Lectura(ext, ext, false, 0, 0, 0, false, -1, "Al aire libre", m.humedad(), c.velocidad, sensExt);
        int rad = 0, chim = 0, aires = 0, otros = 0, consigna = -1;
        boolean caldera = false;
        for (BlockPos q : BlockPos.betweenClosed(p.offset(-7, -3, -7), p.offset(7, 3, 7))) {
            BlockState s = l.getBlockState(q);
            if (s.isAir()) continue;
            String n = id(s);
            if (!n.isEmpty()) {
                switch (n) {
                    case "radiador" -> rad += 2;
                    case "toallero" -> rad += 1;
                    case "chimenea" -> {
                        if (encendido(s)) chim++;
                    }
                    case "caldera_gas" -> caldera = true;
                    case "aire_acondicionado" -> {
                        if (encendido(s)) aires++;
                    }
                    case "termostato" -> {
                        if (consigna < 0) consigna = s.getValue(TermostatoBlock.CONSIGNA);
                    }
                    default -> {
                    }
                }
            } else if (s.getBlock() instanceof CampfireBlock && s.getValue(CampfireBlock.LIT)) otros += 2;
            else if (s.getBlock() instanceof AbstractFurnaceBlock && s.getValue(AbstractFurnaceBlock.LIT)) otros++;
        }
        // la casa amortigua lo de fuera (paredes, gente dentro, inercia)
        double base = ext + (16 - ext) * 0.4;
        double calor = rad * 1.8 * (caldera ? 1.3 : 1.0) + chim * 6 + otros * 2.5;
        double frio = aires * 6;
        double t;
        String estado;
        if (consigna >= 0) {
            if (base < consigna) {
                t = Math.min(consigna, base + calor);
                estado = calor <= 0 ? "Hace frío y no hay calefacción" : t >= consigna - 0.2 ? "Calefacción: temperatura alcanzada" : "Calefacción encendida a tope";
            } else {
                t = Math.max(consigna, base - frio);
                estado = frio <= 0 ? (base > consigna + 1 ? "Hace calor y no hay aire acondicionado" : "Temperatura correcta")
                        : t <= consigna + 0.2 ? "Aire acondicionado: temperatura alcanzada" : "Aire acondicionado a tope";
            }
        } else {
            t = base + calor - frio;
            estado = calor > 0 && frio > 0 ? "Calefacción y aire a la vez" : calor > 0 ? "Calefacción encendida (sin termostato)"
                    : frio > 0 ? "Aire acondicionado encendido (sin termostato)" : "Sin calefacción ni aire";
        }
        return new Lectura(ext, t, true, rad / 2, chim, aires, caldera, consigna, estado, m.humedad(), c.velocidad, sensExt);
    }
}

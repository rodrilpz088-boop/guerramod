package es.guerramod.bloques.hogar;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Convierte cajas (en 1/16, mirando al norte) en formas giradas para las 4 direcciones. */
public final class Formas {
    private Formas() {}

    /** Devuelve un array indexado por {@link Direction#get2DDataValue()}. */
    public static VoxelShape[] crear(double[][] cajas) {
        VoxelShape[] r = new VoxelShape[4];
        for (Direction d : Direction.Plane.HORIZONTAL) r[d.get2DDataValue()] = girar(cajas, d);
        return r;
    }

    public static VoxelShape girar(double[][] cajas, Direction d) {
        if (cajas == null || cajas.length == 0) return Shapes.block();
        VoxelShape s = Shapes.empty();
        for (double[] c : cajas) {
            double x1 = c[0], y1 = c[1], z1 = c[2], x2 = c[3], y2 = c[4], z2 = c[5];
            double[] b = switch (d) {
                case EAST -> new double[]{16 - z2, y1, x1, 16 - z1, y2, x2};
                case SOUTH -> new double[]{16 - x2, y1, 16 - z2, 16 - x1, y2, 16 - z1};
                case WEST -> new double[]{z1, y1, 16 - x2, z2, y2, 16 - x1};
                default -> new double[]{x1, y1, z1, x2, y2, z2};
            };
            for (int i = 0; i < 6; i++) b[i] = Math.max(0, Math.min(16, b[i]));
            if (b[3] - b[0] < 0.01 || b[4] - b[1] < 0.01 || b[5] - b[2] < 0.01) continue;
            s = Shapes.or(s, Block.box(b[0], b[1], b[2], b[3], b[4], b[5]));
        }
        return s.isEmpty() ? Shapes.block() : s.optimize();
    }
}

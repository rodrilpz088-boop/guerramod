package es.guerramod.cliente;

import com.mojang.blaze3d.platform.NativeImage;
import es.guerramod.entidades.AeronaveBase;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Minimapa arriba a la derecha: terreno, fronteras con el color de cada país, jugadores y aviones. */
public class Minimapa implements IGuiOverlay {
    public static final Minimapa INSTANCIA = new Minimapa();
    public static boolean visible = true;

    private static final int TAM = 96;
    private static final int MITAD = TAM / 2;

    private DynamicTexture textura;
    private ResourceLocation idTextura;
    private int ultimoX = Integer.MIN_VALUE, ultimoZ = Integer.MIN_VALUE, contador;

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int ancho, int alto) {
        Minecraft mc = Minecraft.getInstance();
        if (!visible || mc.player == null || mc.level == null || mc.options.hideGui || mc.options.renderDebug) return;
        if (textura == null) {
            textura = new DynamicTexture(TAM, TAM, true);
            idTextura = mc.getTextureManager().register("guerramod_minimapa", textura);
        }
        int px = mc.player.getBlockX(), pz = mc.player.getBlockZ();
        if (++contador >= 20 || px != ultimoX || pz != ultimoZ) {
            actualizar(mc.level, px, pz);
            contador = 0;
            ultimoX = px;
            ultimoZ = pz;
        }

        int x0 = ancho - TAM - 8, y0 = 8;
        g.fill(x0 - 2, y0 - 2, x0 + TAM + 2, y0 + TAM + 2, 0xC0000000);
        g.blit(idTextura, x0, y0, 0, 0, TAM, TAM, TAM, TAM);

        RegistroPaises reg = RegistroPaises.CLIENTE;
        // otros jugadores (con el color de su país) y aviones
        for (Player o : mc.level.players()) {
            if (o == mc.player) continue;
            Pais p = reg.paisDeJugador(o.getUUID());
            punto(g, x0, y0, o.getBlockX() - px, o.getBlockZ() - pz, 0xFF000000 | (p == null ? 0xFFFFFF : p.color), 1);
        }
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof AeronaveBase a) {
                punto(g, x0, y0, e.getBlockX() - px, e.getBlockZ() - pz, 0xFF000000 | a.getColor(), 2);
            }
        }

        // flecha del jugador
        int cx = x0 + MITAD, cy = y0 + MITAD;
        double rad = Math.toRadians(mc.player.getYRot());
        for (int i = 1; i <= 6; i++) {
            int ax = cx + (int) Math.round(-Math.sin(rad) * i);
            int ay = cy + (int) Math.round(Math.cos(rad) * i);
            g.fill(ax, ay, ax + 1, ay + 1, 0xFFFF3030);
        }
        g.fill(cx - 1, cy - 1, cx + 2, cy + 2, 0xFFFFFFFF);

        Font f = mc.font;
        g.drawString(f, "N", x0 + MITAD - 2, y0 + 2, 0xFFFFFFFF, true);

        Pais aqui = reg.dueno(mc.level, mc.player.blockPosition());
        Pais mio = reg.paisDeJugador(mc.player.getUUID());
        String nombre = aqui == null ? "Territorio libre" : aqui.nombre;
        int colorNombre = aqui == null ? 0xFFAAAAAA : 0xFF000000 | aqui.color;
        int ty = y0 + TAM + 4;
        g.drawString(f, f.plainSubstrByWidth(nombre, TAM + 4), x0, ty, colorNombre, true);
        ty += 10;
        if (aqui != null && mio != null && mio != aqui && mio.enGuerraCon(aqui.id)) {
            g.drawString(f, "⚔ ENEMIGO", x0, ty, 0xFFFF4040, true);
            ty += 10;
        } else if (mio != null && !mio.enemigos.isEmpty()) {
            g.drawString(f, "⚔ En guerra", x0, ty, 0xFFFF8040, true);
            ty += 10;
        }
        g.drawString(f, px + ", " + pz, x0, ty, 0xFFDDDDDD, true);
        ty += 10;
        // tiempo: temperatura, humedad y cielo; y el calentamiento global si lo hay
        String tiempo = String.format("%.0f °C · %.0f %% · %s", es.guerramod.clima.TiempoCliente.temperatura,
                es.guerramod.clima.TiempoCliente.humedad, es.guerramod.clima.ComandoClima.cielo(es.guerramod.clima.TiempoCliente.nubes,
                        es.guerramod.clima.TiempoCliente.niebla, mc.level.isRaining() && mc.level.getBiome(mc.player.blockPosition()).value().hasPrecipitation()));
        float tc = es.guerramod.clima.TiempoCliente.temperatura;
        int colorT = tc < 0 ? 0xFF9FD4FF : tc < 15 ? 0xFFD8F0FF : tc < 28 ? 0xFFFFF2C0 : tc < 36 ? 0xFFFFB060 : 0xFFFF6040;
        g.drawString(f, tiempo, x0, ty, colorT, true);
        ty += 10;
        if (es.guerramod.clima.TiempoCliente.cambio && Math.abs(es.guerramod.clima.TiempoCliente.anomalia) >= 0.3f) {
            g.drawString(f, String.format("Calentamiento %+.1f °C", es.guerramod.clima.TiempoCliente.anomalia), x0, ty,
                    es.guerramod.clima.TiempoCliente.anomalia > 2 ? 0xFFFF4030 : 0xFFFF9040, true);
            ty += 10;
        }
        // viento: flecha hacia donde sopla y riesgo de incendio
        String viento = "Viento " + Math.round(es.guerramod.incendios.Clima.clienteVelocidad) + " km/h";
        g.drawString(f, viento, x0, ty, 0xFFBFD8FF, true);
        int ax = x0 + f.width(viento) + 8, ay = ty + 4;
        double ang = Math.toRadians(es.guerramod.incendios.Clima.clienteAngulo);
        for (int i = -4; i <= 4; i++) {
            int qx = ax + (int) Math.round(Math.cos(ang) * i), qy = ay + (int) Math.round(Math.sin(ang) * i);
            g.fill(qx, qy, qx + 1, qy + 1, 0xFFBFD8FF);
        }
        int hx = ax + (int) Math.round(Math.cos(ang) * 4), hy = ay + (int) Math.round(Math.sin(ang) * 4);
        g.fill(hx - 1, hy - 1, hx + 2, hy + 2, 0xFFFFFFFF);
        ty += 10;
        int riesgo = es.guerramod.incendios.Clima.clienteRiesgo;
        g.drawString(f, "Riesgo incendio: " + es.guerramod.incendios.Clima.nombreRiesgo(riesgo), x0, ty,
                0xFF000000 | es.guerramod.incendios.Clima.colorRiesgo(riesgo), true);
    }

    private static void punto(GuiGraphics g, int x0, int y0, int dx, int dz, int color, int r) {
        if (Math.abs(dx) >= MITAD - 1 || Math.abs(dz) >= MITAD - 1) return;
        int x = x0 + MITAD + dx, y = y0 + MITAD + dz;
        g.fill(x - r, y - r, x + r + 1, y + r + 1, 0xFF000000);
        g.fill(x - r + 1, y - r + 1, x + r, y + r, color);
        if (r == 1) g.fill(x, y, x + 1, y + 1, color);
    }

    private void actualizar(ClientLevel level, int px, int pz) {
        NativeImage img = textura.getPixels();
        if (img == null) return;
        RegistroPaises reg = RegistroPaises.CLIENTE;
        boolean overworld = level.dimension() == Level.OVERWORLD;
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int[] alturaNorte = new int[TAM];
        for (int z = 0; z < TAM; z++) {
            for (int x = 0; x < TAM; x++) {
                int wx = px - MITAD + x, wz = pz - MITAD + z;
                int color;
                if (!level.hasChunk(wx >> 4, wz >> 4)) {
                    color = 0x151515;
                    alturaNorte[x] = Integer.MIN_VALUE;
                } else {
                    int h = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                    m.set(wx, h, wz);
                    BlockState st = level.getBlockState(m);
                    MapColor mcol = st.getMapColor(level, m);
                    int intentos = 0;
                    while (mcol == MapColor.NONE && h > level.getMinBuildHeight() && intentos++ < 10) {
                        h--;
                        m.set(wx, h, wz);
                        st = level.getBlockState(m);
                        mcol = st.getMapColor(level, m);
                    }
                    color = mcol.col == 0 ? 0x202020 : mcol.col;
                    int hn = alturaNorte[x];
                    if (z > 0 && hn != Integer.MIN_VALUE) {
                        if (h > hn) color = brillo(color, 1.15f);
                        else if (h < hn) color = brillo(color, 0.8f);
                    }
                    alturaNorte[x] = h;
                }
                if (overworld) {
                    int cx = wx >> 4, cz = wz >> 4;
                    Pais p = reg.duenoChunk(ChunkPos.asLong(cx, cz));
                    if (p != null) {
                        int lx = wx & 15, lz = wz & 15;
                        boolean borde = (lx == 0 && reg.duenoChunk(ChunkPos.asLong(cx - 1, cz)) != p)
                                || (lx == 15 && reg.duenoChunk(ChunkPos.asLong(cx + 1, cz)) != p)
                                || (lz == 0 && reg.duenoChunk(ChunkPos.asLong(cx, cz - 1)) != p)
                                || (lz == 15 && reg.duenoChunk(ChunkPos.asLong(cx, cz + 1)) != p);
                        color = borde ? p.color : mezclar(color, p.color, 0.38f);
                    }
                }
                img.setPixelRGBA(x, z, abgr(color));
            }
        }
        textura.upload();
    }

    private static int abgr(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }

    private static int brillo(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 0xFF) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 0xFF) * f));
        int b = Math.min(255, (int) ((rgb & 0xFF) * f));
        return (r << 16) | (g << 8) | b;
    }

    private static int mezclar(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }
}

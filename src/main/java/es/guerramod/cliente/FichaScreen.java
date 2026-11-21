package es.guerramod.cliente;

import es.guerramod.pueblo.Rol;
import es.guerramod.pueblo.Tablet;
import es.guerramod.red.Red;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Pantalla de la tablet municipal: el DNI del aldeano, su oficio y el panel del pueblo (guerra y paz). */
public class FichaScreen extends Screen {
    private final int entidad;
    private final CompoundTag datos;
    private static final int W = 360, H = 252;
    private int x0, y0;

    public FichaScreen(int entidad, CompoundTag datos) {
        super(Component.literal("Tablet municipal"));
        this.entidad = entidad;
        this.datos = datos;
    }

    public static void abrir(int entidad, CompoundTag datos) {
        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new FichaScreen(entidad, datos));
    }

    private boolean tieneDni() {
        return datos.contains("dni");
    }

    private boolean tienePueblo() {
        return datos.contains("pueblo");
    }

    private void enviar(int accion, int valor) {
        Red.CANAL.sendToServer(new Red.AccionPueblo(entidad, accion, valor));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        y0 = (height - H) / 2;
        if (tieneDni() && !datos.getCompound("dni").getBoolean("menor")) {
            int actual = datos.getCompound("dni").getInt("rol");
            Rol[] rs = Rol.values();
            for (int i = 0; i < rs.length; i++) {
                Rol r = rs[i];
                int bx = x0 + 214 + (i % 2) * 70, by = y0 + 22 + (i / 2) * 20;
                Button b = Button.builder(Component.literal((r.ordinal() == actual ? "▶ " : "") + corto(r)), btn -> enviar(Tablet.ROL, r.ordinal()))
                        .bounds(bx, by, 68, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                                Component.literal(r.nombre + ": " + r.descripcion))).build();
                b.active = r.ordinal() != actual;
                addRenderableWidget(b);
            }
        }
        if (tienePueblo()) {
            CompoundTag p = datos.getCompound("pueblo");
            int by = y0 + H - 26;
            boolean esElegido = p.getBoolean("esElegido");
            addRenderableWidget(Button.builder(Component.literal(esElegido ? "✔ Elegido" : "Elegir pueblo"), b -> enviar(Tablet.ELEGIR, 0))
                    .bounds(x0 + 8, by, 84, 18).tooltip(net.minecraft.client.gui.components.Tooltip.create(
                            Component.literal("Elige este pueblo. Después ve a otro y declárale la guerra."))).build()).active = !esElegido;
            Button guerra = Button.builder(Component.literal("⚔ Declarar guerra"), b -> enviar(Tablet.GUERRA, 0)).bounds(x0 + 96, by, 100, 18)
                    .tooltip(net.minecraft.client.gui.components.Tooltip.create(Component.literal(p.contains("elegido") && !esElegido
                            ? p.getString("elegido") + " atacará a " + p.getString("nombre")
                            : "Primero elige otro pueblo con «Elegir pueblo»"))).build();
            guerra.active = p.contains("elegido") && !esElegido && !p.contains("enemigo");
            addRenderableWidget(guerra);
            Button paz = Button.builder(Component.literal("🕊 Firmar la paz"), b -> enviar(Tablet.PAZ, 0)).bounds(x0 + 200, by, 84, 18).build();
            paz.active = p.contains("enemigo");
            addRenderableWidget(paz);
            Button ind = Button.builder(Component.literal("Independencia"), b -> enviar(Tablet.INDEPENDENCIA, 0)).bounds(x0 + 288, by, 64, 18).build();
            ind.active = p.contains("pertenece");
            addRenderableWidget(ind);
        }
    }

    private static String corto(Rol r) {
        return switch (r) {
            case NINGUNO -> "Sin oficio";
            case REPRESENTANTE -> "Represent.";
            case CONSTRUCTOR -> "Constructor";
            case AGRICULTOR -> "Agricultor";
            case CAMINERO -> "Caminero";
            case FAROLERO -> "Electricista";
            case BOMBERO -> "Bombero";
            case DEFENSA -> "Defensa";
            case VENDEDOR -> "Comerciante";
            case POLICIA -> "Policía";
        };
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        // marco de la tablet
        g.fill(x0 - 6, y0 - 6, x0 + W + 6, y0 + H + 6, 0xFF1A1C20);
        g.fill(x0 - 4, y0 - 4, x0 + W + 4, y0 + H + 4, 0xFF2B2E35);
        g.fillGradient(x0, y0, x0 + W, y0 + H, 0xFFEFF3F7, 0xFFDDE4EC);
        g.fill(x0, y0, x0 + W, y0 + 14, 0xFF24496E);
        g.drawString(font, "Ayuntamiento · Tablet municipal", x0 + 6, y0 + 3, 0xFFFFFFFF, false);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            long t = mc.level.getDayTime() % 24000L;
            int h = (int) ((t / 1000 + 6) % 24), m = (int) (t % 1000 * 60 / 1000);
            String hora = String.format("%02d:%02d", h, m);
            g.drawString(font, hora, x0 + W - 6 - font.width(hora), y0 + 3, 0xFFFFFFFF, false);
        }
        if (tieneDni()) dibujarDni(g, mx, my);
        else if (tienePueblo()) g.drawString(font, "Panel del pueblo", x0 + 10, y0 + 22, 0xFF24496E, false);
        if (tieneDni()) {
            g.drawString(font, "Oficio", x0 + 214, y0 + 12 + 1, 0xFF24496E, false);
        }
        if (tienePueblo()) dibujarPueblo(g);
        super.render(g, mx, my, pt);
    }

    private void dibujarDni(GuiGraphics g, int mx, int my) {
        CompoundTag d = datos.getCompound("dni");
        int cx = x0 + 8, cy = y0 + 20, cw = 200, ch = 118;
        // tarjeta con fondo de seguridad
        g.fill(cx - 1, cy - 1, cx + cw + 1, cy + ch + 1, 0xFF8A9BAA);
        g.fillGradient(cx, cy, cx + cw, cy + ch, 0xFFCFE6EE, 0xFFF1DCE3);
        for (int i = 0; i < cw; i += 6) g.fill(cx + i, cy, cx + i + 1, cy + ch, 0x10FFFFFF);
        for (int j = 0; j < ch; j += 9) g.fill(cx, cy + j, cx + cw, cy + j + 1, 0x0C2A5070);
        g.fill(cx, cy, cx + cw, cy + 12, 0xFFB0283C);
        g.fill(cx, cy + 12, cx + cw, cy + 14, 0xFFE9B627);
        escala(g, "DOCUMENTO NACIONAL DE IDENTIDAD", cx + 4, cy + 3, 0xFFFFFFFF, 0.75f);
        // foto
        int fx = cx + 6, fy = cy + 18, fw = 48, fh = 62;
        g.fill(fx - 1, fy - 1, fx + fw + 1, fy + fh + 1, 0xFF6B7B88);
        g.fillGradient(fx, fy, fx + fw, fy + fh, 0xFFE8EEF2, 0xFFC5D2DC);
        Entity e = Minecraft.getInstance().level != null && entidad >= 0 ? Minecraft.getInstance().level.getEntity(entidad) : null;
        if (e instanceof LivingEntity le) {
            g.enableScissor(fx, fy, fx + fw, fy + fh);
            InventoryScreen.renderEntityInInventoryFollowsMouse(g, fx + fw / 2, fy + fh + 18, 30, (float) (fx + fw / 2) - mx,
                    (float) (fy + 20) - my, le);
            g.disableScissor();
        }
        int tx = cx + 60, ty = cy + 18;
        campo(g, "APELLIDOS", d.getString("ap1") + " " + d.getString("ap2"), tx, ty);
        campo(g, "NOMBRE", d.getString("nombre"), tx, ty + 17);
        campo(g, "SEXO", d.getString("sexo"), tx, ty + 34);
        campo(g, "EDAD", d.getBoolean("menor") ? "Menor" : d.getInt("edad") + " años", tx + 40, ty + 34);
        campo(g, "NACIMIENTO", d.getString("fechaTxt"), tx, ty + 51);
        String pueblo = tienePueblo() ? datos.getCompound("pueblo").getString("nombre") : "-";
        campo(g, "PUEBLO", pueblo, tx, ty + 68);
        // número en la franja de abajo
        g.fill(cx, cy + ch - 16, cx + cw, cy + ch, 0x30234060);
        escala(g, "DNI", cx + 6, cy + ch - 12, 0xFF234060, 0.75f);
        g.drawString(font, d.getString("numeroTxt"), cx + 26, cy + ch - 13, 0xFF101820, false);
        Rol r = Rol.de(d.getInt("rol"));
        String ofi = d.getBoolean("menor") ? "Estudiante" : r.nombre;
        g.drawString(font, ofi, cx + cw - 6 - font.width(ofi), cy + ch - 13, 0xFF000000 | r.color, true);
        // estado de salud y oficio de Minecraft
        String prof = d.getString("profesion");
        String info = "Vida " + Math.round(d.getFloat("vida")) + "/20" + (prof.equals("none") || prof.isEmpty() ? "" : " · " + traducirProfesion(prof)
                + " nv." + d.getInt("nivel"));
        escala(g, info, cx + 6, cy + ch + 4, 0xFF44505C, 0.75f);
        if (!d.getBoolean("menor")) {
            escala(g, r.descripcion, cx + 6, cy + ch + 12, 0xFF44505C, 0.75f);
        }
    }

    private static String traducirProfesion(String p) {
        return switch (p) {
            case "farmer" -> "granjero";
            case "armorer" -> "armero";
            case "butcher" -> "carnicero";
            case "cartographer" -> "cartógrafo";
            case "cleric" -> "clérigo";
            case "fisherman" -> "pescador";
            case "fletcher" -> "flechero";
            case "leatherworker" -> "peletero";
            case "librarian" -> "bibliotecario";
            case "mason" -> "albañil";
            case "shepherd" -> "pastor";
            case "toolsmith" -> "herrero";
            case "weaponsmith" -> "armero de armas";
            case "nitwit" -> "simple";
            default -> p;
        };
    }

    private void campo(GuiGraphics g, String etiqueta, String valor, int x, int y) {
        escala(g, etiqueta, x, y, 0xFF5A6B7A, 0.6f);
        String v = valor;
        while (font.width(v) > 136 && v.length() > 3) v = v.substring(0, v.length() - 2);
        g.drawString(font, v.toUpperCase(), x, y + 6, 0xFF101820, false);
    }

    private void escala(GuiGraphics g, String s, int x, int y, int color, float k) {
        g.pose().pushPose();
        g.pose().translate(x, y, 0);
        g.pose().scale(k, k, 1);
        g.drawString(font, s, 0, 0, color, false);
        g.pose().popPose();
    }

    private void dibujarPueblo(GuiGraphics g) {
        CompoundTag p = datos.getCompound("pueblo");
        int px = tieneDni() ? x0 + 8 : x0 + 10, py = tieneDni() ? y0 + 162 : y0 + 36;
        int ancho = W - 16;
        g.fill(px - 2, py - 3, px + ancho, py - 2, 0xFF9AAAB8);
        String tipo = p.getBoolean("ciudad") ? "Ciudad" : "Pueblo";
        g.drawString(font, tipo + " de " + p.getString("nombre"), px, py, 0xFF24496E, false);
        int linea = py + 11;
        String l1 = p.getInt("poblacion") + " vecinos · " + p.getLong("camas") + " camas · " + p.getInt("casas") + " construcciones · "
                + p.getInt("huertos") + " huertos · " + p.getInt("farolas") + " farolas";
        escala(g, l1, px, linea, 0xFF303A44, 0.75f);
        linea += 9;
        int[] roles = p.getIntArray("roles");
        StringBuilder sb = new StringBuilder();
        for (Rol r : Rol.values()) {
            if (r.ordinal() >= roles.length || roles[r.ordinal()] == 0) continue;
            if (sb.length() > 0) sb.append(" · ");
            sb.append(roles[r.ordinal()]).append(" ").append(corto(r).toLowerCase());
        }
        escala(g, sb.length() == 0 ? "Nadie tiene oficio todavía: nombra un representante o asígnalos tú" : sb.toString(), px, linea, 0xFF303A44, 0.75f);
        linea += 9;
        ListTag obras = p.getList("obras", Tag.TAG_STRING);
        if (!obras.isEmpty()) {
            StringBuilder o = new StringBuilder("Obras: ");
            for (int i = 0; i < obras.size() && i < 4; i++) o.append(i > 0 ? " · " : "").append(obras.getString(i));
            escala(g, o.toString(), px, linea, 0xFF6A4A10, 0.75f);
            linea += 9;
        }
        if (p.contains("enemigo")) {
            escala(g, "⚔ EN GUERRA CON " + p.getString("enemigo").toUpperCase(), px, linea, 0xFFB0202C, 0.75f);
            linea += 9;
        } else if (p.contains("pertenece")) {
            escala(g, "Pertenece a " + p.getString("pertenece") + " (lo conquistó en la guerra)", px, linea, 0xFF7A5A10, 0.75f);
            linea += 9;
        }
        if (p.contains("elegido") && !p.getBoolean("esElegido")) {
            escala(g, "Pueblo elegido: " + p.getString("elegido") + " (a " + p.getInt("distElegido") + " bloques)", px, linea, 0xFF24496E, 0.75f);
        }
        if (!tieneDni()) {
            int ly = py + 70;
            escala(g, "Clic derecho a un aldeano con la tablet para ver su DNI y darle un oficio.", px, ly, 0xFF55606A, 0.75f);
            escala(g, "Con un representante, él reparte los oficios. Cuanta más gente, más casas construyen.", px, ly + 9, 0xFF55606A, 0.75f);
            escala(g, "Guerra: «Elegir pueblo» aquí, ve a otro pueblo y pulsa «Declarar guerra».", px, ly + 18, 0xFF55606A, 0.75f);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

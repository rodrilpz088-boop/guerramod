package es.guerramod.cliente;

import com.mojang.blaze3d.systems.RenderSystem;
import es.guerramod.GuerraMod;
import es.guerramod.armas.DefArma;
import es.guerramod.items.ArmaItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;

/** Cruceta dinámica, munición, recarga y mira telescópica. */
public class HudArmas implements IGuiOverlay {
    public static final HudArmas INSTANCIA = new HudArmas();
    private static final ResourceLocation MIRA = new ResourceLocation(GuerraMod.MODID, "textures/misc/mira_telescopica.png");

    @Override
    public void render(ForgeGui gui, GuiGraphics g, float pt, int ancho, int alto) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) return;
        ArmaItem arma = ArmasCliente.armaEnMano(p);
        if (arma == null) return;
        DefArma d = arma.def;
        ItemStack st = p.getMainHandItem();
        float aim = ArmasCliente.apuntado;

        if (d.telescopica() && aim > 0.92f) {
            mira(g, ancho, alto);
        } else if (aim < 0.6f && mc.options.getCameraType().isFirstPerson()) {
            cruceta(g, ancho / 2, alto / 2, p, d, 1.0f - aim / 0.6f);
        }
        municion(g, mc.font, ancho, alto, p, arma, st);
    }

    private static void mira(GuiGraphics g, int w, int h) {
        int lado = Math.min(w, h);
        int x0 = (w - lado) / 2, y0 = (h - lado) / 2;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.blit(MIRA, x0, y0, 0, 0, lado, lado, lado, lado);
        int negro = 0xFF000000;
        g.fill(0, 0, x0, h, negro);
        g.fill(x0 + lado, 0, w, h, negro);
        g.fill(x0, 0, x0 + lado, y0, negro);
        g.fill(x0, y0 + lado, x0 + lado, h, negro);
    }

    private static void cruceta(GuiGraphics g, int cx, int cy, LocalPlayer p, DefArma d, float alfa) {
        float disp = d.dispCadera();
        boolean moviendo = Math.abs(p.input.forwardImpulse) > 0.01f || Math.abs(p.input.leftImpulse) > 0.01f;
        if (moviendo) disp = disp * 1.6f + 0.3f;
        if (!p.onGround()) disp = disp * 2.5f + 1.0f;
        if (p.isCrouching()) disp *= 0.6f;
        int hueco = (int) (2 + disp * 2.2f + ArmasCliente.bloom * 2.0f);
        int largo = 4;
        int a = (int) (alfa * 220) << 24;
        int c = a | 0xFFFFFF, sombra = ((int) (alfa * 120) << 24);
        // sombra
        g.fill(cx - hueco - largo, cy, cx - hueco + 1, cy + 2, sombra);
        g.fill(cx + hueco, cy, cx + hueco + largo + 1, cy + 2, sombra);
        g.fill(cx, cy - hueco - largo, cx + 2, cy - hueco + 1, sombra);
        g.fill(cx, cy + hueco, cx + 2, cy + hueco + largo + 1, sombra);
        // líneas
        g.fill(cx - hueco - largo, cy, cx - hueco, cy + 1, c);
        g.fill(cx + hueco + 1, cy, cx + hueco + largo + 1, cy + 1, c);
        g.fill(cx, cy - hueco - largo, cx + 1, cy - hueco, c);
        g.fill(cx, cy + hueco + 1, cx + 1, cy + hueco + largo + 1, c);
        g.fill(cx, cy, cx + 1, cy + 1, c);
    }

    private static void municion(GuiGraphics g, Font f, int w, int h, LocalPlayer p, ArmaItem arma, ItemStack st) {
        DefArma d = arma.def;
        int balas = ArmaItem.getBalas(st);
        Item m = d.municion().get();
        int reserva = 0;
        for (ItemStack s : p.getInventory().items) if (s.is(m)) reserva += s.getCount();
        if (p.getOffhandItem().is(m)) reserva += p.getOffhandItem().getCount();

        int x = w - 10, y = h - 46;
        String nombre = st.getHoverName().getString();
        String cargador = String.valueOf(balas);
        String total = " / " + d.cargador();
        String extra = "Reserva: " + (p.getAbilities().instabuild ? "∞" : String.valueOf(reserva));
        String modo = d.automatica() ? (arma.esSemi(st) ? "SEMI" : "AUTO") : (d.proyectiles() > 1 ? "CORREDERA" : "SEMI");

        g.fill(x - 112, y - 4, x + 4, y + 38, 0x70000000);
        g.drawString(f, nombre, x - 108, y, 0xFFE0E0E0, true);
        g.pose().pushPose();
        g.pose().translate(x - 108, y + 11, 0);
        g.pose().scale(2.0f, 2.0f, 1.0f);
        int color = balas == 0 ? 0xFFFF4040 : balas <= d.cargador() / 4 ? 0xFFFFB040 : 0xFFFFFFFF;
        g.drawString(f, cargador, 0, 0, color, true);
        g.pose().popPose();
        g.drawString(f, total, x - 108 + f.width(cargador) * 2 + 2, y + 18, 0xFFAAAAAA, true);
        g.drawString(f, modo, x - f.width(modo), y + 18, 0xFFE6C35A, true);
        g.drawString(f, extra, x - 108, y + 29, 0xFF9A9A9A, true);

        if (ArmaItem.recargando(st)) {
            long ini = ArmaItem.recargaInicio(st), fin = ArmaItem.recargaFin(st);
            float prog = Math.min(1.0f, (p.level().getGameTime() - ini) / (float) Math.max(1, fin - ini));
            int cx = w / 2, cy = h / 2 + 18;
            g.drawCenteredString(f, "RECARGANDO", cx, cy, 0xFFFFD060);
            g.fill(cx - 30, cy + 11, cx + 30, cy + 14, 0x90000000);
            g.fill(cx - 30, cy + 11, cx - 30 + (int) (60 * prog), cy + 14, 0xFFFFD060);
        } else if (balas == 0) {
            g.drawCenteredString(f, reserva > 0 || p.getAbilities().instabuild ? "Pulsa R para recargar" : "Sin munición",
                    w / 2, h / 2 + 18, 0xFFFF6060);
        }
    }
}

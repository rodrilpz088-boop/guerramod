package es.guerramod.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import es.guerramod.GuerraMod;
import es.guerramod.armas.DefArma;
import es.guerramod.items.ArmaItem;
import es.guerramod.red.Red;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.MovementInputUpdateEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Random;

/**
 * Todo lo que el jugador ve y siente al usar un arma:
 * apuntar con la mira, retroceso de cámara, peso e inercia del arma en la mano (muelles), balanceo al andar,
 * posición de carrera, animación de recarga, fogonazo y respiración con la mira telescópica.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class ArmasCliente {
    private static final ResourceLocation FOGONAZO = new ResourceLocation(GuerraMod.MODID, "textures/misc/fogonazo.png");
    private static final Random RND = new Random();

    // ---- estado general
    /** 0 = desde la cadera, 1 = apuntando por la mira. */
    public static float apuntado;
    /** Dispersión extra por disparar seguido (para la cruceta). */
    public static float bloom;
    static int ticksDesdeDisparo = 100;
    private static boolean gatilloAntes;
    private static long ultimoFrame;
    private static long fogonazoHasta;

    // ---- cámara
    private static float pendPitch, pendYaw, recuperar;
    private static float respX, respY;

    // ---- física del arma en la mano
    private static final Muelle SW_X = new Muelle(), SW_Y = new Muelle(), ROT_YAW = new Muelle(), ROT_PITCH = new Muelle(),
            ROLL = new Muelle(), RETRO_Z = new Muelle(), RETRO_PITCH = new Muelle(), SALTO = new Muelle();
    private static float ultimoYaw, ultimoPitch;
    private static boolean primerFrame = true;
    private static float carrera, equipar;
    private static ItemStack ultimoStack = ItemStack.EMPTY;
    private static int ultimaRanura = -1;

    private ArmasCliente() {}

    public static ArmaItem armaEnMano(Player p) {
        return p != null && p.getMainHandItem().getItem() instanceof ArmaItem a ? a : null;
    }

    public static boolean apuntando(Player p) {
        return p.isUsingItem() && p.getUseItem().getItem() instanceof ArmaItem;
    }

    private static float suave(float t) {
        return t * t * (3 - 2 * t);
    }

    // ================================================================== gatillo

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (ticksDesdeDisparo < 1000) ticksDesdeDisparo++;
        bloom *= 0.82f;
        ArmaItem arma = armaEnMano(p);
        if (arma == null || mc.screen != null || p.isSpectator()) {
            gatilloAntes = false;
            return;
        }
        int clics = 0;
        while (mc.options.keyAttack.consumeClick()) clics++;
        boolean gatillo = mc.options.keyAttack.isDown();
        ItemStack st = p.getMainHandItem();
        boolean auto = !arma.esSemi(st);
        boolean quiere = auto ? (gatillo || clics > 0) : (clics > 0 || (gatillo && !gatilloAntes));
        gatilloAntes = gatillo;
        if (apuntando(p) && p.isSprinting()) p.setSprinting(false);

        if (quiere && ticksDesdeDisparo >= arma.def.cadencia()) {
            boolean moviendose = Math.abs(p.input.forwardImpulse) > 0.01f || Math.abs(p.input.leftImpulse) > 0.01f;
            Red.CANAL.sendToServer(new Red.AccionArma(Red.AccionArma.DISPARAR, moviendose));
            ticksDesdeDisparo = 0;
            boolean puede = ArmaItem.getBalas(st) > 0 && (!ArmaItem.recargando(st) || arma.def.porUnidad());
            if (puede) retroceso(p, arma.def);
            else ticksDesdeDisparo = -6;
        }
    }

    /** Patada del arma: cámara hacia arriba y arma hacia atrás. */
    private static void retroceso(LocalPlayer p, DefArma d) {
        float f = apuntando(p) ? 0.65f : 1.0f;
        if (p.isCrouching()) f *= 0.7f;
        pendPitch += d.retrocesoV() * f;
        pendYaw += (RND.nextFloat() * 2 - 1) * d.retrocesoH() * f;
        RETRO_Z.impulso(d.retrocesoV() * 0.9f * (1.0f - 0.4f * apuntado));
        RETRO_PITCH.impulso(d.retrocesoV() * 45.0f * f);
        ROT_YAW.impulso((RND.nextFloat() * 2 - 1) * d.retrocesoH() * 30.0f);
        ROLL.impulso((RND.nextFloat() * 2 - 1) * d.retrocesoV() * 8.0f);
        bloom += 0.8f + d.retrocesoV() * 0.35f;
        fogonazoHasta = System.nanoTime() + 45_000_000L;
    }

    // ================================================================== clics del ratón

    @SubscribeEvent
    public static void clic(InputEvent.InteractionKeyMappingTriggered e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (armaEnMano(p) == null) return;
        if (e.isAttack()) {
            // el clic izquierdo dispara (lo gestiona el tick), nunca golpea ni rompe bloques
            e.setCanceled(true);
            e.setSwingHand(false);
        } else if (e.isUseItem() && e.getHand() == InteractionHand.MAIN_HAND) {
            // el clic derecho siempre apunta, aunque mires una puerta o un cofre
            e.setCanceled(true);
            e.setSwingHand(false);
            if (!p.isUsingItem() && mc.gameMode != null) mc.gameMode.useItem(p, InteractionHand.MAIN_HAND);
        }
    }

    /** Al apuntar se anda más despacio (la mitad), pero no tan lento como con un arco. */
    @SubscribeEvent
    public static void movimiento(MovementInputUpdateEvent e) {
        if (!(e.getEntity() instanceof LocalPlayer p) || !apuntando(p) || p.isPassenger()) return;
        ArmaItem arma = armaEnMano(p);
        float objetivo = arma == null ? 0.5f : 0.6f - 0.25f * arma.def.peso();
        float f = objetivo / 0.2f;
        e.getInput().forwardImpulse *= f;
        e.getInput().leftImpulse *= f;
    }

    // ================================================================== cámara (cada fotograma)

    @SubscribeEvent
    public static void fotograma(TickEvent.RenderTickEvent e) {
        if (e.phase != TickEvent.Phase.START) return;
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        long ahora = System.nanoTime();
        float dt = ultimoFrame == 0 ? 0.016f : Mth.clamp((ahora - ultimoFrame) / 1.0E9f, 0.0f, 0.1f);
        ultimoFrame = ahora;
        if (p == null || mc.isPaused()) {
            primerFrame = true;
            return;
        }
        ArmaItem arma = armaEnMano(p);
        actualizarFisica(p, arma, e.renderTickTime, dt);

        // retroceso de cámara: subida rápida y recuperación parcial
        if (Math.abs(pendPitch) > 1.0E-4f || Math.abs(pendYaw) > 1.0E-4f) {
            float k = Math.min(1.0f, dt * 30.0f);
            float ap = pendPitch * k, ay = pendYaw * k;
            pendPitch -= ap;
            pendYaw -= ay;
            girar(p, ay, -ap);
            recuperar += ap * 0.55f;
        }
        if (recuperar > 1.0E-4f && ticksDesdeDisparo > 1) {
            float r = recuperar * Math.min(1.0f, dt * 5.0f);
            recuperar -= r;
            girar(p, 0, r);
        }

        // respiración con la mira telescópica (agachado se estabiliza)
        float ox = 0, oy = 0;
        if (arma != null && arma.def.telescopica() && apuntado > 0.9f) {
            double t = ahora / 1.0E9;
            float a = p.isCrouching() ? 0.12f : 0.4f;
            ox = (float) (Math.sin(t * 0.83) * a);
            oy = (float) (Math.sin(t * 1.66) * a * 0.5);
        }
        if (ox != respX || oy != respY) {
            girar(p, ox - respX, oy - respY);
            respX = ox;
            respY = oy;
        }
    }

    private static void girar(LocalPlayer p, float grYaw, float grPitch) {
        p.turn(grYaw / 0.15, grPitch / 0.15);
    }

    private static void actualizarFisica(LocalPlayer p, ArmaItem arma, float pt, float dt) {
        float yaw = p.getViewYRot(pt), pitch = p.getViewXRot(pt);
        if (primerFrame) {
            ultimoYaw = yaw;
            ultimoPitch = pitch;
            primerFrame = false;
        }
        float dYaw = Mth.wrapDegrees(yaw - ultimoYaw) / Math.max(dt, 0.001f);
        float dPitch = (pitch - ultimoPitch) / Math.max(dt, 0.001f);
        ultimoYaw = yaw;
        ultimoPitch = pitch;

        // cambio de arma: animación de sacarla
        ItemStack st = p.getMainHandItem();
        int ranura = p.getInventory().selected;
        if (st.getItem() != ultimoStack.getItem() || ranura != ultimaRanura) equipar = 1.0f;
        ultimoStack = st;
        ultimaRanura = ranura;
        equipar = Math.max(0.0f, equipar - dt * 3.2f);

        float peso = arma == null ? 0.5f : arma.def.peso();
        float obj = arma != null && apuntando(p) && equipar < 0.3f ? 1.0f : 0.0f;
        float rapidez = 1.0f / (0.16f + 0.24f * peso);
        apuntado = obj > apuntado ? Math.min(obj, apuntado + dt * rapidez) : Math.max(obj, apuntado - dt * rapidez * 1.4f);
        float aim = suave(apuntado);

        float masa = 0.6f + peso;
        float k = 150.0f / masa;
        float c = 2.0f * (float) Math.sqrt(k) * 0.55f;
        float libre = 1.0f - 0.65f * aim;
        SW_X.paso(Mth.clamp(-dYaw * 0.00028f * masa, -0.07f, 0.07f) * libre, dt, k, c);
        SW_Y.paso(Mth.clamp(dPitch * 0.00028f * masa, -0.06f, 0.06f) * libre, dt, k, c);
        ROT_YAW.paso(Mth.clamp(dYaw * 0.011f * masa, -7.0f, 7.0f) * libre, dt, k, c);
        ROT_PITCH.paso(Mth.clamp(dPitch * 0.011f * masa, -7.0f, 7.0f) * libre, dt, k, c);
        ROLL.paso(p.input.leftImpulse * 5.0f * libre + ROT_YAW.pos * 0.35f, dt, k * 0.8f, c);
        RETRO_Z.paso(0, dt, 320.0f / masa, 20.0f);
        RETRO_PITCH.paso(0, dt, 260.0f / masa, 17.0f);
        double vy = p.getDeltaMovement().y;
        SALTO.paso(p.onGround() ? 0.0f : (float) Mth.clamp(-vy * 0.12, -0.07, 0.09), dt, 110.0f, 9.0f);

        float objCarrera = p.isSprinting() && aim < 0.1f ? 1.0f : 0.0f;
        carrera += (objCarrera - carrera) * Math.min(1.0f, dt * 7.0f);
    }

    // ================================================================== zoom

    @SubscribeEvent
    public static void fov(ViewportEvent.ComputeFov e) {
        if (!e.usedConfiguredFov()) return;
        LocalPlayer p = Minecraft.getInstance().player;
        ArmaItem arma = armaEnMano(p);
        if (arma == null || apuntado <= 0) return;
        float aim = suave(apuntado);
        float zoom = arma.def.telescopica() ? (aim > 0.9f ? arma.def.zoom() : Mth.lerp(aim, 1.0f, 0.8f)) : Mth.lerp(aim, 1.0f, arma.def.zoom());
        e.setFOV(e.getFOV() * zoom);
    }

    /** Sin cruceta normal: la nuestra cambia con la dispersión. */
    @SubscribeEvent
    public static void quitarCruceta(RenderGuiOverlayEvent.Pre e) {
        if (e.getOverlay().id().equals(VanillaGuiOverlay.CROSSHAIR.id()) && armaEnMano(Minecraft.getInstance().player) != null) {
            e.setCanceled(true);
        }
    }

    // ================================================================== el arma en primera persona

    @SubscribeEvent
    public static void mano(RenderHandEvent e) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) return;
        if (e.getHand() == InteractionHand.OFF_HAND) {
            if (armaEnMano(p) != null) e.setCanceled(true); // las armas se cogen con las dos manos
            return;
        }
        ItemStack st = e.getItemStack();
        if (!(st.getItem() instanceof ArmaItem arma)) return;
        e.setCanceled(true);
        DefArma d = arma.def;
        float aim = suave(apuntado);
        if (d.telescopica() && apuntado > 0.92f) return; // se ve la mira telescópica a pantalla completa

        float pt = e.getPartialTick();
        float s = d.escala();
        float cadX = 0.30f, cadY = -0.22f - (d.miraY() - 8.0f) / 16.0f * s, cadZ = -0.45f - (d.miraZ() - 8.0f) / 16.0f * s;
        float apY = -(d.miraY() - 8.0f) / 16.0f * s, apZ = -0.12f - (d.miraZ() - 8.0f) / 16.0f * s;

        // balanceo al andar (como la cámara, pero más marcado en el arma)
        float f = p.walkDist - p.walkDistO;
        float paso = -(p.walkDist + f * pt);
        float amp = Mth.lerp(pt, p.oBob, p.bob) * (1.0f - 0.8f * aim) * (1.0f + 0.6f * carrera);
        float bobX = Mth.sin(paso * (float) Math.PI) * amp * 0.5f;
        float bobY = -Math.abs(Mth.cos(paso * (float) Math.PI) * amp);
        float bobRoll = Mth.sin(paso * (float) Math.PI) * amp * 4.0f;

        // respiración en reposo
        double t = System.nanoTime() / 1.0E9;
        float resp = (float) Math.sin(t * 1.6) * 0.004f * (1.0f - 0.7f * aim);

        // recarga
        float rc = 0;
        if (ArmaItem.recargando(st)) {
            long ini = ArmaItem.recargaInicio(st), fin = ArmaItem.recargaFin(st);
            float prog = Mth.clamp((p.level().getGameTime() + pt - ini) / Math.max(1.0f, fin - ini), 0.0f, 1.0f);
            rc = (float) Math.pow(Math.sin(Math.PI * prog), 0.6);
        }
        float eq = suave(equipar);

        float x = Mth.lerp(aim, cadX, 0.0f) + SW_X.pos + bobX - 0.06f * carrera - 0.04f * rc;
        float y = Mth.lerp(aim, cadY, apY) + SW_Y.pos + bobY + SALTO.pos + resp - 0.06f * carrera - 0.10f * rc - 0.45f * eq;
        float z = Mth.lerp(aim, cadZ, apZ) + RETRO_Z.pos * 0.11f * (1.0f - 0.45f * aim) + 0.04f * carrera;

        PoseStack ps = e.getPoseStack();
        ps.pushPose();
        ps.translate(x, y, z);
        ps.mulPose(Axis.YP.rotationDegrees(ROT_YAW.pos + 3.0f * (1.0f - aim) + 38.0f * carrera + 8.0f * rc));
        ps.mulPose(Axis.XP.rotationDegrees(ROT_PITCH.pos + RETRO_PITCH.pos * 0.12f - 22.0f * carrera - 22.0f * rc - 40.0f * eq));
        ps.mulPose(Axis.ZP.rotationDegrees(ROLL.pos + bobRoll + 12.0f * carrera + 32.0f * rc));
        ps.scale(s, s, s);
        mc.getEntityRenderDispatcher().getItemInHandRenderer().renderItem(p, st, ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
                false, ps, e.getMultiBufferSource(), e.getPackedLight());
        if (System.nanoTime() < fogonazoHasta) {
            ps.translate(0.0f, d.bocaY() / 16.0f - 0.5f, d.bocaZ() / 16.0f - 0.5f - 0.05f);
            fogonazo(ps, e.getMultiBufferSource(), d.explosiva() ? 0.5f : 0.28f);
        }
        ps.popPose();
    }

    private static void fogonazo(PoseStack ps, MultiBufferSource buf, float r) {
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(FOGONAZO));
        ps.mulPose(Axis.ZP.rotationDegrees(RND.nextFloat() * 360.0f));
        for (int i = 0; i < 2; i++) {
            Matrix4f m = ps.last().pose();
            Matrix3f n = ps.last().normal();
            v(vc, m, n, -r, -r, 0, 1);
            v(vc, m, n, r, -r, 1, 1);
            v(vc, m, n, r, r, 1, 0);
            v(vc, m, n, -r, r, 0, 0);
            ps.mulPose(Axis.ZP.rotationDegrees(45.0f));
        }
    }

    private static void v(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float u, float w) {
        vc.vertex(m, x, y, 0).color(255, 255, 255, 255).uv(u, w).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(0xF000F0).normal(n, 0, 0, 1).endVertex();
    }
}

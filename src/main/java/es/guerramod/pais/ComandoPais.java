package es.guerramod.pais;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Comando /pais con todo lo de crear países, fronteras, gobierno y guerra. */
public final class ComandoPais {
    private ComandoPais() {}

    private static final SuggestionProvider<CommandSourceStack> SUG_PAISES = (ctx, b) ->
            SharedSuggestionProvider.suggest(RegistroPaises.servidor().paises.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> SUG_COLORES = (ctx, b) ->
            SharedSuggestionProvider.suggest(Colores.LISTA.keySet(), b);
    private static final SuggestionProvider<CommandSourceStack> SUG_CARGOS = (ctx, b) ->
            SharedSuggestionProvider.suggest(Gobierno.CARGOS, b);

    public static void registrar(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("pais")
                .executes(ComandoPais::ayuda)
                .then(Commands.literal("ayuda").executes(ComandoPais::ayuda))
                .then(Commands.literal("crear")
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(SUG_COLORES)
                                .then(Commands.argument("nombre", StringArgumentType.greedyString())
                                        .executes(ComandoPais::crear))))
                .then(Commands.literal("disolver").executes(ComandoPais::disolver))
                .then(Commands.literal("color")
                        .then(Commands.argument("color", StringArgumentType.word()).suggests(SUG_COLORES)
                                .executes(ComandoPais::cambiarColor)))
                .then(Commands.literal("reclamar")
                        .executes(c -> reclamar(c, 0))
                        .then(Commands.argument("radio", IntegerArgumentType.integer(0, 5))
                                .executes(c -> reclamar(c, IntegerArgumentType.getInteger(c, "radio")))))
                .then(Commands.literal("abandonar").executes(ComandoPais::abandonar))
                .then(Commands.literal("conquistar").executes(ComandoPais::conquistar))
                .then(Commands.literal("unir")
                        .then(Commands.argument("jugador", EntityArgument.player()).executes(ComandoPais::unir)))
                .then(Commands.literal("expulsar")
                        .then(Commands.argument("jugador", EntityArgument.player()).executes(ComandoPais::expulsar)))
                .then(Commands.literal("salir").executes(ComandoPais::salir))
                .then(Commands.literal("info")
                        .executes(c -> info(c, null))
                        .then(Commands.argument("pais", StringArgumentType.word()).suggests(SUG_PAISES)
                                .executes(c -> info(c, StringArgumentType.getString(c, "pais")))))
                .then(Commands.literal("lista").executes(ComandoPais::lista))
                .then(Commands.literal("guerra")
                        .then(Commands.argument("pais", StringArgumentType.word()).suggests(SUG_PAISES)
                                .executes(ComandoPais::guerra)))
                .then(Commands.literal("paz")
                        .then(Commands.argument("pais", StringArgumentType.word()).suggests(SUG_PAISES)
                                .executes(ComandoPais::paz)))
                .then(Commands.literal("presidente")
                        .then(Commands.literal("yo").executes(ComandoPais::presidenteYo))
                        .then(Commands.literal("aldeano").executes(ComandoPais::presidenteAldeano)))
                .then(Commands.literal("ministro")
                        .then(Commands.argument("cargo", StringArgumentType.word()).suggests(SUG_CARGOS)
                                .executes(ComandoPais::ministro)))
                .then(Commands.literal("cesar")
                        .then(Commands.argument("cargo", StringArgumentType.word()).suggests(SUG_CARGOS)
                                .executes(ComandoPais::cesar)))
                .then(Commands.literal("aldea")
                        .executes(c -> aldea(c, 2))
                        .then(Commands.argument("radio", IntegerArgumentType.integer(0, 5))
                                .executes(c -> aldea(c, IntegerArgumentType.getInteger(c, "radio")))))
                .then(Commands.literal("elecciones").executes(ComandoPais::elecciones))
        );
    }

    // ---------- utilidades ----------

    private static RegistroPaises reg() {
        return RegistroPaises.servidor();
    }

    private static int error(CommandContext<CommandSourceStack> c, String msg) {
        c.getSource().sendFailure(Component.literal(msg));
        return 0;
    }

    private static void ok(CommandContext<CommandSourceStack> c, Component msg) {
        c.getSource().sendSuccess(() -> msg, false);
    }

    private static Pais miPais(ServerPlayer p) {
        return reg().paisDeJugador(p.getUUID());
    }

    private static long chunkDe(ServerPlayer p) {
        return ChunkPos.asLong(p.getBlockX() >> 4, p.getBlockZ() >> 4);
    }

    private static boolean enOverworld(ServerPlayer p) {
        return p.level().dimension() == Level.OVERWORLD;
    }

    // ---------- comandos ----------

    private static int ayuda(CommandContext<CommandSourceStack> c) {
        String[] lineas = {
                "§6§l=== Naciones en Guerra ===",
                "§e/pais crear <color> <nombre>§7 - funda un país",
                "§e/pais reclamar [radio]§7 - reclama el chunk donde estás (o un área)",
                "§e/pais aldea [radio]§7 - anexiona la aldea cercana y sus aldeanos",
                "§e/pais unir <jugador>§7 / §e/pais salir§7 - miembros",
                "§e/pais presidente yo|aldeano§7 - nombra presidente",
                "§e/pais ministro <cargo>§7 - el aldeano más cercano pasa a ministro",
                "§e/pais elecciones§7 - elecciones entre jugadores y aldeanos",
                "§e/pais guerra <pais>§7 / §e/pais paz <pais>",
                "§e/pais conquistar§7 - en guerra, conquista el chunk enemigo donde estás",
                "§e/pais info [pais]§7 / §e/pais lista",
                "§7Tecla §fM§7: minimapa · §fR§7: disparar avión · §fB§7: soltar bomba"
        };
        for (String l : lineas) c.getSource().sendSystemMessage(Component.literal(l));
        return 1;
    }

    private static int crear(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        String color = StringArgumentType.getString(c, "color").toLowerCase();
        String nombre = StringArgumentType.getString(c, "nombre").trim();
        if (!Colores.LISTA.containsKey(color)) return error(c, "Color no válido. Usa: " + String.join(", ", Colores.LISTA.keySet()));
        if (nombre.length() < 2 || nombre.length() > 32) return error(c, "El nombre debe tener entre 2 y 32 letras.");
        if (miPais(p) != null) return error(c, "Ya perteneces a un país. Usa /pais salir primero.");
        String id = RegistroPaises.crearId(nombre);
        if (reg().get(id) != null) return error(c, "Ya existe un país con ese nombre.");
        Pais pais = new Pais(id, nombre, Colores.LISTA.get(color), p.getUUID());
        pais.miembros.add(p.getUUID());
        pais.nombresMiembros.put(p.getUUID(), p.getGameProfile().getName());
        Gobierno.nombrarPresidenteJugador(pais, p);
        reg().paises.put(id, pais);
        if (enOverworld(p) && reg().territorio.get(chunkDe(p)) == null) reg().territorio.put(chunkDe(p), id);
        DatosPaises.cambiado(c.getSource().getServer());
        c.getSource().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("⚑ Se ha fundado ").withStyle(ChatFormatting.GOLD).append(pais.nombreColoreado())
                        .append(Component.literal(" por " + p.getGameProfile().getName()).withStyle(ChatFormatting.GOLD)), false);
        ok(c, Component.literal("Usa /pais reclamar para ampliar las fronteras. ID del país: " + id).withStyle(ChatFormatting.GRAY));
        return 1;
    }

    private static int disolver(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!p.getUUID().equals(pais.fundador)) return error(c, "Solo el fundador puede disolver el país.");
        reg().borrarPais(pais.id);
        DatosPaises.cambiado(c.getSource().getServer());
        c.getSource().getServer().getPlayerList().broadcastSystemMessage(
                Component.literal("El país " + pais.nombre + " se ha disuelto.").withStyle(ChatFormatting.GRAY), false);
        return 1;
    }

    private static int cambiarColor(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        String color = StringArgumentType.getString(c, "color").toLowerCase();
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede cambiar el color.");
        if (!Colores.LISTA.containsKey(color)) return error(c, "Color no válido.");
        pais.color = Colores.LISTA.get(color);
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal("Nuevo color: ").append(pais.nombreColoreado()));
        return 1;
    }

    private static int reclamar(CommandContext<CommandSourceStack> c, int radio) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el presidente o el fundador pueden ampliar fronteras.");
        if (!enOverworld(p)) return error(c, "Solo se pueden reclamar tierras en el mundo normal.");
        int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
        int nuevos = 0, ocupados = 0;
        for (int x = -radio; x <= radio; x++) {
            for (int z = -radio; z <= radio; z++) {
                long k = ChunkPos.asLong(cx + x, cz + z);
                String dueno = reg().territorio.get(k);
                if (dueno == null) {
                    reg().territorio.put(k, pais.id);
                    nuevos++;
                } else if (!dueno.equals(pais.id)) ocupados++;
            }
        }
        DatosPaises.cambiado(c.getSource().getServer());
        String extra = ocupados > 0 ? " (" + ocupados + " ya eran de otro país)" : "";
        ok(c, Component.literal("Reclamados " + nuevos + " chunks para ").append(pais.nombreColoreado()).append(extra));
        return nuevos;
    }

    private static int abandonar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede ceder territorio.");
        long k = chunkDe(p);
        if (!pais.id.equals(reg().territorio.get(k))) return error(c, "Este chunk no es de tu país.");
        reg().territorio.remove(k);
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal("Chunk abandonado."));
        return 1;
    }

    private static int conquistar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!enOverworld(p)) return error(c, "Solo en el mundo normal.");
        long k = chunkDe(p);
        Pais enemigo = reg().duenoChunk(k);
        if (enemigo == null || enemigo == pais) return error(c, "Este chunk no es de un país enemigo.");
        if (!pais.enGuerraCon(enemigo.id)) return error(c, "No estás en guerra con " + enemigo.nombre + ".");
        int cx = p.getBlockX() >> 4, cz = p.getBlockZ() >> 4;
        boolean vecino = pais.id.equals(reg().territorio.get(ChunkPos.asLong(cx + 1, cz)))
                || pais.id.equals(reg().territorio.get(ChunkPos.asLong(cx - 1, cz)))
                || pais.id.equals(reg().territorio.get(ChunkPos.asLong(cx, cz + 1)))
                || pais.id.equals(reg().territorio.get(ChunkPos.asLong(cx, cz - 1)));
        if (!vecino) return error(c, "Solo puedes conquistar chunks pegados a tu frontera.");
        MinecraftServer server = c.getSource().getServer();
        for (UUID u : enemigo.miembros) {
            ServerPlayer e = server.getPlayerList().getPlayer(u);
            if (e != null && e.level() == p.level() && e.distanceTo(p) < 32 && e.isAlive())
                return error(c, "Hay defensores enemigos a menos de 32 bloques. ¡Elimínalos primero!");
        }
        reg().territorio.put(k, pais.id);
        DatosPaises.cambiado(server);
        Gobierno.avisarPais(server, enemigo, Component.literal("⚠ ").append(pais.nombreColoreado())
                .append(Component.literal(" ha conquistado parte de vuestro territorio en " + p.getBlockX() + ", " + p.getBlockZ())
                        .withStyle(ChatFormatting.RED)));
        ok(c, Component.literal("¡Chunk conquistado para ").append(pais.nombreColoreado()).append("!"));
        return 1;
    }

    private static int unir(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerPlayer otro = EntityArgument.getPlayer(c, "jugador");
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede anadir ciudadanos.");
        if (miPais(otro) != null) return error(c, "Ese jugador ya pertenece a un país.");
        pais.miembros.add(otro.getUUID());
        pais.nombresMiembros.put(otro.getUUID(), otro.getGameProfile().getName());
        DatosPaises.cambiado(c.getSource().getServer());
        otro.sendSystemMessage(Component.literal("Ahora eres ciudadano de ").append(pais.nombreColoreado()));
        ok(c, Component.literal(otro.getGameProfile().getName() + " se ha unido a ").append(pais.nombreColoreado()));
        return 1;
    }

    private static int expulsar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        ServerPlayer otro = EntityArgument.getPlayer(c, "jugador");
        Pais pais = miPais(p);
        if (pais == null || !pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede expulsar.");
        if (otro.getUUID().equals(pais.fundador)) return error(c, "No puedes expulsar al fundador.");
        if (!pais.miembros.remove(otro.getUUID())) return error(c, "Ese jugador no es de tu país.");
        if (otro.getUUID().equals(pais.presidente)) {
            pais.tipoPresidente = Pais.PRES_NINGUNO;
            pais.presidente = null;
            pais.nombrePresidente = "";
        }
        DatosPaises.cambiado(c.getSource().getServer());
        otro.sendSystemMessage(Component.literal("Has sido expulsado de " + pais.nombre).withStyle(ChatFormatting.RED));
        ok(c, Component.literal("Expulsado."));
        return 1;
    }

    private static int salir(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (p.getUUID().equals(pais.fundador)) return error(c, "Eres el fundador: usa /pais disolver.");
        pais.miembros.remove(p.getUUID());
        if (p.getUUID().equals(pais.presidente)) {
            pais.tipoPresidente = Pais.PRES_NINGUNO;
            pais.presidente = null;
            pais.nombrePresidente = "";
        }
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal("Has abandonado " + pais.nombre));
        return 1;
    }

    public static MutableComponent ficha(Pais pais) {
        RegistroPaises r = reg();
        MutableComponent t = Component.literal("━━━━ ").withStyle(ChatFormatting.DARK_GRAY).append(pais.nombreColoreado())
                .append(Component.literal(" ━━━━").withStyle(ChatFormatting.DARK_GRAY));
        String pres = switch (pais.tipoPresidente) {
            case Pais.PRES_JUGADOR -> pais.nombrePresidente + " (jugador)";
            case Pais.PRES_ALDEANO -> pais.nombrePresidente + " (aldeano)";
            default -> "vacante";
        };
        t.append(Component.literal("\n§7Presidente: §f" + pres));
        t.append(Component.literal("\n§7Fundador: §f" + pais.nombresMiembros.getOrDefault(pais.fundador, "?")));
        if (pais.ministros.isEmpty()) {
            t.append(Component.literal("\n§7Ministros: §fninguno"));
        } else {
            for (Map.Entry<String, Pais.Ministro> e : pais.ministros.entrySet())
                t.append(Component.literal("\n§7  " + Gobierno.cargoBonito(e.getKey()) + ": §f" + e.getValue().nombre()));
        }
        t.append(Component.literal("\n§7Jugadores: §f" + String.join(", ", pais.nombresMiembros.values())));
        t.append(Component.literal("\n§7Aldeanos ciudadanos: §f" + pais.ciudadanos.size()));
        t.append(Component.literal("\n§7Territorio: §f" + r.numChunks(pais.id) + " chunks"));
        if (pais.enemigos.isEmpty()) {
            t.append(Component.literal("\n§aEn paz con todos"));
        } else {
            t.append(Component.literal("\n§cEn guerra con: "));
            boolean primero = true;
            for (String e : pais.enemigos) {
                Pais o = r.get(e);
                if (o == null) continue;
                if (!primero) t.append(Component.literal(", "));
                t.append(o.nombreColoreado());
                primero = false;
            }
        }
        return t;
    }

    private static int info(CommandContext<CommandSourceStack> c, String id) throws CommandSyntaxException {
        Pais pais;
        if (id == null) {
            ServerPlayer p = c.getSource().getPlayerOrException();
            pais = miPais(p);
            if (pais == null && enOverworld(p)) pais = reg().duenoChunk(chunkDe(p));
            if (pais == null) return error(c, "No perteneces a ningún país. Usa /pais info <pais>.");
        } else {
            pais = reg().get(id);
            if (pais == null) return error(c, "No existe ese país.");
        }
        Pais finalPais = pais;
        c.getSource().sendSuccess(() -> ficha(finalPais), false);
        return 1;
    }

    private static int lista(CommandContext<CommandSourceStack> c) {
        if (reg().paises.isEmpty()) return error(c, "Todavía no hay países. Crea uno con /pais crear.");
        MutableComponent t = Component.literal("Países del mundo:").withStyle(ChatFormatting.GOLD);
        for (Pais p : reg().paises.values()) {
            t.append(Component.literal("\n • ")).append(p.nombreColoreado())
                    .append(Component.literal(" §7(" + p.id + ", " + reg().numChunks(p.id) + " chunks, "
                            + (p.enemigos.isEmpty() ? "§apaz" : "§cen guerra") + "§7)"));
        }
        c.getSource().sendSuccess(() -> t, false);
        return 1;
    }

    private static int guerra(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        Pais otro = reg().get(StringArgumentType.getString(c, "pais"));
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el presidente o el fundador pueden declarar la guerra.");
        if (otro == null) return error(c, "No existe ese país.");
        if (otro == pais) return error(c, "No puedes declararte la guerra a ti mismo.");
        if (pais.enGuerraCon(otro.id)) return error(c, "Ya estáis en guerra.");
        Gobierno.declararGuerra(c.getSource().getServer(), pais, otro);
        return 1;
    }

    private static int paz(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        Pais otro = reg().get(StringArgumentType.getString(c, "pais"));
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede negociar la paz.");
        if (otro == null || otro == pais) return error(c, "País no válido.");
        if (!pais.enGuerraCon(otro.id)) return error(c, "No estáis en guerra.");
        MinecraftServer server = c.getSource().getServer();
        if (otro.ofertasPaz.contains(pais.id)) {
            Gobierno.firmarPaz(server, pais, otro);
        } else {
            pais.ofertasPaz.add(otro.id);
            DatosPaises.cambiado(server);
            Gobierno.avisarPais(server, otro, pais.nombreColoreado()
                    .append(Component.literal(" os ofrece la paz. Aceptad con /pais paz " + pais.id).withStyle(ChatFormatting.GREEN)));
            ok(c, Component.literal("Oferta de paz enviada a ").append(otro.nombreColoreado()));
        }
        return 1;
    }

    private static int presidenteYo(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el fundador puede nombrarse presidente.");
        Gobierno.nombrarPresidenteJugador(pais, p);
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal("Ahora eres el presidente de ").append(pais.nombreColoreado()));
        return 1;
    }

    private static int presidenteAldeano(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede nombrar presidente.");
        Villager v = Gobierno.aldeanoCercano(p);
        if (v == null) return error(c, "Ponte al lado de un aldeano (8 bloques).");
        Gobierno.nombrarPresidenteAldeano(pais, v);
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal("El aldeano " + pais.nombrePresidente + " es el nuevo presidente de ").append(pais.nombreColoreado()));
        return 1;
    }

    private static int ministro(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        String cargo = StringArgumentType.getString(c, "cargo").toLowerCase();
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el presidente nombra ministros.");
        if (!Gobierno.CARGOS.contains(cargo)) return error(c, "Cargos: " + String.join(", ", Gobierno.CARGOS));
        Villager v = Gobierno.aldeanoCercano(p);
        if (v == null) return error(c, "Ponte al lado de un aldeano (8 bloques).");
        Gobierno.nombrarMinistro(pais, cargo, v);
        DatosPaises.cambiado(c.getSource().getServer());
        Pais.Ministro m = pais.ministros.get(cargo);
        ok(c, Component.literal(m.nombre() + " es el nuevo " + Gobierno.cargoBonito(cargo)).withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static int cesar(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        String cargo = StringArgumentType.getString(c, "cargo").toLowerCase();
        if (pais == null || !pais.puedeGobernar(p.getUUID())) return error(c, "Solo el presidente puede cesar ministros.");
        Pais.Ministro m = pais.ministros.remove(cargo);
        if (m == null) return error(c, "Ese cargo está vacío.");
        if (p.level() instanceof net.minecraft.server.level.ServerLevel sl
                && sl.getEntity(m.aldeano()) instanceof Villager v) {
            v.setCustomName(Gobierno.color("Ciudadano " + m.nombre(), pais.color));
        }
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal(m.nombre() + " ha sido cesado."));
        return 1;
    }

    private static int aldea(CommandContext<CommandSourceStack> c, int radio) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede anexionar aldeas.");
        if (!enOverworld(p)) return error(c, "Solo en el mundo normal.");
        int r = reclamar(c, radio);
        List<Villager> aldeanos = p.level().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(48));
        int n = 0;
        for (Villager v : aldeanos) {
            Pais dueno = reg().dueno(p.level(), v.blockPosition());
            if (dueno != pais) continue;
            if (pais.ciudadanos.add(v.getUUID())) {
                v.setPersistenceRequired();
                if (!v.hasCustomName()) {
                    v.setCustomName(Gobierno.color("Ciudadano " + Gobierno.nombreAleatorio(), pais.color));
                }
                n++;
            }
        }
        DatosPaises.cambiado(c.getSource().getServer());
        ok(c, Component.literal("Aldea anexionada: " + n + " aldeanos nuevos son ciudadanos de ").append(pais.nombreColoreado())
                .append(Component.literal(". Ahora nombra presidente y ministros.").withStyle(ChatFormatting.GRAY)));
        return Math.max(r, 1);
    }

    private static int elecciones(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Pais pais = miPais(p);
        if (pais == null) return error(c, "No perteneces a ningún país.");
        if (!pais.puedeGobernar(p.getUUID())) return error(c, "Solo el gobierno puede convocar elecciones.");
        Gobierno.elecciones(p.serverLevel(), pais, p.position());
        return 1;
    }
}

package de.jan.tennis.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import de.jan.tennis.Msg;
import de.jan.tennis.TennisServer;
import de.jan.tennis.court.Court;
import de.jan.tennis.court.CourtBuilder;
import de.jan.tennis.logic.Surface;
import de.jan.tennis.match.TennisMatch;
import de.jan.tennis.outfit.Outfit;
import de.jan.tennis.registry.ModItems;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Der Befehl {@code /tennis} mit allen Unterbefehlen. */
public final class TennisCommands {
	private TennisCommands() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("tennis")
			.executes(TennisCommands::help)
			.then(Commands.literal("hilfe").executes(TennisCommands::help))
			.then(Commands.literal("ausruestung").executes(TennisCommands::equipment))
			.then(Commands.literal("platz")
				.then(Commands.literal("bauen")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(ctx -> build(ctx, Surface.HARTPLATZ))
					.then(Commands.argument("belag", StringArgumentType.word())
						.suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Surface.values()).map(s -> s.id), b))
						.executes(ctx -> build(ctx, Surface.byId(StringArgumentType.getString(ctx, "belag"))))))
				.then(Commands.literal("entfernen")
					.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
					.executes(TennisCommands::removeCourt))
				.then(Commands.literal("liste").executes(TennisCommands::listCourts)))
			.then(Commands.literal("match")
				.then(Commands.argument("gegner", EntityArgument.player())
					.executes(ctx -> startMatch(ctx, 1))
					.then(Commands.argument("gewinnsaetze", IntegerArgumentType.integer(1, 3))
						.executes(ctx -> startMatch(ctx, IntegerArgumentType.getInteger(ctx, "gewinnsaetze"))))))
			.then(Commands.literal("aufgeben").executes(TennisCommands::forfeit))
			.then(Commands.literal("stand").executes(TennisCommands::showScore))
			.then(Commands.literal("outfit")
				.executes(TennisCommands::listOutfits)
				.then(Commands.literal("liste").executes(TennisCommands::listOutfits))
				.then(Commands.argument("name", StringArgumentType.word())
					.suggests((ctx, b) -> SharedSuggestionProvider.suggest(Stream.concat(Arrays.stream(Outfit.values()).map(o -> o.id), Stream.of(Outfit.NONE)), b))
					.executes(TennisCommands::setOutfit))));
	}

	private static TennisServer ts() {
		TennisServer ts = TennisServer.get();
		if (ts == null) {
			throw new IllegalStateException("Tennis ist noch nicht bereit");
		}
		return ts;
	}

	private static int help(CommandContext<CommandSourceStack> ctx) {
		String[] lines = {
			"§6§l🎾 Tennis – so geht's",
			"§e/tennis platz bauen [rasen|sand|hartplatz]§7 – baut einen Platz in Originalgröße (Blickrichtung = Längsachse)",
			"§e/tennis match <Spieler> [1-3]§7 – startet ein Einzel (Zahl = Gewinnsätze)",
			"§e/tennis ausruestung§7 – Schläger und Bälle",
			"§e/tennis outfit <Name>§7 – Tennis-Look wählen, §e/tennis outfit liste§7 zeigt alle",
			"§e/tennis stand§7, §e/tennis aufgeben",
			"§6Spielen:",
			"§f Rechtsklick halten§7 = ausholen, §floslassen§7 = schlagen. Länger halten = härter, aber ungenauer.",
			"§f Ohne Ball in der Nähe§7 wirft Rechtsklick den Ball zum Aufschlag hoch – im höchsten Punkt treffen!",
			"§f Fadenkreuz§7 auf den Punkt richten, wo der Ball aufkommen soll. §fNach oben schauen§7 = Lob.",
			"§f Schleichen§7 = Slice (Unterschnitt), sonst Topspin. §fLinksklick§7 auf den Ball = schneller Block-Schlag."
		};
		for (String line : lines) {
			ctx.getSource().sendSuccess(() -> Component.literal(line), false);
		}
		return 1;
	}

	private static int equipment(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		giveRacket(player);
		player.addItem(new ItemStack(ModItems.TENNIS_BALL, 3));
		Msg.chat(player, "§6[Tennis] §fViel Spaß! §7Tipp: /tennis hilfe");
		return 1;
	}

	private static void giveRacket(ServerPlayer player) {
		boolean has = player.getInventory().contains(new ItemStack(ModItems.TENNIS_RACKET));
		if (!has) {
			player.addItem(new ItemStack(ModItems.TENNIS_RACKET));
		}
	}

	private static int build(CommandContext<CommandSourceStack> ctx, Surface surface) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TennisServer ts = ts();
		Vec3 look = player.getLookAngle();
		boolean alongX = Math.abs(look.x) > Math.abs(look.z);
		BlockPos center = player.blockPosition();
		Court court = CourtBuilder.build(player.level(), center, alongX, surface, ts.courts.nextName());
		ts.courts.add(court);
		Vec3 spot = court.world(-14, 2, 0);
		player.teleportTo(player.level(), spot.x, court.groundY(), spot.z, java.util.Set.of(), court.yawTowardNet(-1), 0, false);
		ctx.getSource().sendSuccess(() -> Component.literal("§6[Tennis] §f" + court.name() + " (" + surface.displayName + ") gebaut. §7Starte ein Spiel mit /tennis match <Spieler>"), true);
		return 1;
	}

	private static int removeCourt(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TennisServer ts = ts();
		Optional<Court> court = ts.courts.find(player.level(), player.position(), 40);
		if (court.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Kein Tennisplatz in der Nähe."));
			return 0;
		}
		if (ts.matches.courtBusy(court.get())) {
			ctx.getSource().sendFailure(Component.literal("Auf diesem Platz läuft gerade ein Match."));
			return 0;
		}
		ts.courts.remove(court.get());
		ctx.getSource().sendSuccess(() -> Component.literal("§6[Tennis] §f" + court.get().name() + " ist kein Tennisplatz mehr (die Blöcke bleiben stehen)."), true);
		return 1;
	}

	private static int listCourts(CommandContext<CommandSourceStack> ctx) {
		TennisServer ts = ts();
		if (ts.courts.all().isEmpty()) {
			ctx.getSource().sendSuccess(() -> Component.literal("§6[Tennis] §fNoch kein Platz gebaut. §7/tennis platz bauen"), false);
		}
		for (Court c : ts.courts.all()) {
			ctx.getSource().sendSuccess(() -> Component.literal("§e" + c.name() + "§7: " + c.surface().displayName + " bei "
				+ (int) c.cx() + " " + c.groundY() + " " + (int) c.cz() + (ts.matches.courtBusy(c) ? " §c(belegt)" : "")), false);
		}
		return 1;
	}

	private static int startMatch(CommandContext<CommandSourceStack> ctx, int setsToWin) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		ServerPlayer opponent = EntityArgument.getPlayer(ctx, "gegner");
		TennisServer ts = ts();
		if (opponent == player) {
			ctx.getSource().sendFailure(Component.literal("Du brauchst einen Gegner."));
			return 0;
		}
		if (ts.matches.byPlayer(player) != null || ts.matches.byPlayer(opponent) != null) {
			ctx.getSource().sendFailure(Component.literal("Einer von euch spielt schon ein Match. (/tennis aufgeben)"));
			return 0;
		}
		Optional<Court> found = ts.courts.find(player.level(), player.position(), 40);
		if (found.isEmpty()) {
			ctx.getSource().sendFailure(Component.literal("Kein Tennisplatz in der Nähe. Erst /tennis platz bauen."));
			return 0;
		}
		Court court = found.get();
		if (ts.matches.courtBusy(court)) {
			ctx.getSource().sendFailure(Component.literal(court.name() + " ist gerade belegt."));
			return 0;
		}
		if (opponent.level() != player.level() || opponent.position().distanceTo(player.position()) > 80) {
			ctx.getSource().sendFailure(Component.literal(opponent.getGameProfile().name() + " ist zu weit weg."));
			return 0;
		}
		giveRacket(player);
		giveRacket(opponent);
		TennisMatch match = new TennisMatch(ts, court, player, opponent, setsToWin, player.getRandom().nextInt(2));
		ts.matches.add(match);
		return 1;
	}

	private static int forfeit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TennisMatch match = ts().matches.byPlayer(player);
		if (match == null) {
			ctx.getSource().sendFailure(Component.literal("Du spielst gerade kein Match."));
			return 0;
		}
		match.forfeit(player);
		return 1;
	}

	private static int showScore(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		TennisMatch match = ts().matches.byPlayer(player);
		String text = match == null ? "Du spielst gerade kein Match." : match.describe();
		ctx.getSource().sendSuccess(() -> Component.literal("§6[Tennis] §f" + text), false);
		return 1;
	}

	private static int listOutfits(CommandContext<CommandSourceStack> ctx) {
		ctx.getSource().sendSuccess(() -> Component.literal("§6§lTennis-Outfits §7(/tennis outfit <Name>, \"aus\" = eigener Skin)"), false);
		for (Outfit o : Outfit.values()) {
			ctx.getSource().sendSuccess(() -> Component.literal("§e" + o.id + "§f – " + o.displayName + "§7: " + o.description), false);
		}
		return 1;
	}

	private static int setOutfit(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		String name = StringArgumentType.getString(ctx, "name");
		if (name.equalsIgnoreCase(Outfit.NONE)) {
			ts().outfits.set(player, Outfit.NONE);
			Msg.chat(player, "§6[Tennis] §fDu trägst wieder deinen eigenen Skin.");
			return 1;
		}
		Outfit outfit = Outfit.byId(name);
		if (outfit == null) {
			ctx.getSource().sendFailure(Component.literal("Unbekanntes Outfit. /tennis outfit liste"));
			return 0;
		}
		ts().outfits.set(player, outfit.id);
		Msg.chat(player, "§6[Tennis] §fDu bist jetzt §e" + outfit.displayName + "§f!");
		return 1;
	}
}

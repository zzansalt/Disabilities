package io.github.disabilities.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.github.disabilities.ConditionStore;
import io.github.disabilities.ConditionType;
import io.github.disabilities.config.ModConfig;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public final class DisabilityCommands {
	private DisabilityCommands() {
	}

	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> registerCommands(dispatcher));
	}

	private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
		LiteralCommandNode<CommandSourceStack> root = dispatcher.register(
			Commands.literal("disability")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.literal("give")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("name", StringArgumentType.word())
							.suggests(DisabilityCommands::suggestNames)
							.executes(DisabilityCommands::executeGive))))
				.then(Commands.literal("remove")
					.then(Commands.argument("players", EntityArgument.players())
						.then(Commands.argument("name", StringArgumentType.word())
							.suggests(DisabilityCommands::suggestNames)
							.executes(DisabilityCommands::executeRemove))))
				.then(Commands.literal("next")
					.executes(ctx -> executeNext(ctx, ctx.getSource().getServer().getPlayerList().getPlayers()))
					.then(Commands.argument("players", EntityArgument.players())
						.executes(ctx -> executeNext(ctx, EntityArgument.getPlayers(ctx, "players")))))
				.then(Commands.literal("reset")
					.executes(DisabilityCommands::executeReset))
				.then(Commands.literal("timer")
					.then(Commands.literal("off")
						.executes(ctx -> {
							ConditionStore.setTimerEnabled(false);
							ConditionStore.syncAll(ctx.getSource().getServer());
							sendSuccess(ctx, "Automatic disability timer disabled.");
							return 1;
						}))
					.then(Commands.argument("minutes", DoubleArgumentType.doubleArg(0.05, 720.0))
						.executes(ctx -> {
							double minutes = DoubleArgumentType.getDouble(ctx, "minutes");
							ConditionStore.setIntervalMinutes(minutes);
							ConditionStore.syncAll(ctx.getSource().getServer());
							String formatted = minutes == Math.floor(minutes) ? String.valueOf((int) minutes) : String.valueOf(minutes);
							sendSuccess(ctx, "Automatic disability timer set to " + formatted + (minutes == 1.0 ? " minute." : " minutes."));
							return 1;
						})))
				.then(Commands.literal("list")
					.executes(ctx -> executeList(ctx, List.of(ctx.getSource().getPlayerOrException())))
					.then(Commands.argument("players", EntityArgument.players())
						.executes(ctx -> executeList(ctx, EntityArgument.getPlayers(ctx, "players")))))
				.then(Commands.literal("lang")
					.then(Commands.literal("en").executes(ctx -> executeLang(ctx, ModConfig.Language.EN)))
					.then(Commands.literal("ko").executes(ctx -> executeLang(ctx, ModConfig.Language.KO)))
					.then(Commands.literal("auto").executes(ctx -> executeLang(ctx, ModConfig.Language.AUTO))))
				.then(Commands.literal("reload")
					.executes(ctx -> {
						ModConfig.load();
						ConditionStore.syncConfig(ctx.getSource().getServer());
						sendSuccess(ctx, ModConfig.isKorean()
							? "설정을 다시 불러와 동기화했습니다. (언어: " + ModConfig.getLanguage().key + ", 고소공포증 Y: " + ModConfig.getAcrophobiaY() + ")"
							: "Reloaded & synced config. (lang: " + ModConfig.getLanguage().key + ", acrophobia Y: " + ModConfig.getAcrophobiaY() + ")");
						return 1;
					}))
				.then(Commands.literal("config")
					.then(Commands.literal("acrophobia")
						.executes(ctx -> {
							int y = ModConfig.getAcrophobiaY();
							sendSuccess(ctx, ModConfig.isKorean()
								? "현재 고소공포증 발동 높이: Y=" + y
								: "Current Acrophobia Y threshold: " + y);
							return 1;
						})
						.then(Commands.argument("y", IntegerArgumentType.integer(-64, 320))
							.executes(ctx -> {
								int y = IntegerArgumentType.getInteger(ctx, "y");
								ModConfig.setAcrophobiaY(y);
								ConditionStore.syncConfig(ctx.getSource().getServer());
								sendSuccess(ctx, ModConfig.isKorean()
									? "고소공포증 발동 높이를 Y=" + y + "(으)로 변경하고 모든 플레이어에게 동기화했습니다."
									: "Set Acrophobia Y threshold to " + y + " and synced to all players.");
								return 1;
							})))
					.then(Commands.literal("tourette")
						.then(Commands.literal("list")
							.executes(ctx -> {
								List<String> list = ModConfig.getTouretteMessages();
								if (list.isEmpty()) {
									sendSuccess(ctx, ModConfig.isKorean()
										? "설정된 투렛 메시지가 없습니다. (기본값: 마인크래프트 랜덤 단어)"
										: "No custom Tourette's messages configured. (Using random Minecraft words)");
								} else {
									StringBuilder sb = new StringBuilder();
									for (int i = 0; i < list.size(); i++) {
										if (i > 0) sb.append(", ");
										sb.append('[').append(i + 1).append("] ").append(list.get(i));
									}
									sendSuccess(ctx, (ModConfig.isKorean() ? "투렛 메시지 목록 (" : "Tourette's messages (") + list.size() + "): " + sb);
								}
								return 1;
							}))
						.then(Commands.literal("add")
							.then(Commands.argument("message", StringArgumentType.greedyString())
								.executes(ctx -> {
									String msg = StringArgumentType.getString(ctx, "message");
									if (ModConfig.addTouretteMessage(msg)) {
										ConditionStore.syncConfig(ctx.getSource().getServer());
										sendSuccess(ctx, ModConfig.isKorean()
											? "투렛 메시지를 추가했습니다: \"" + msg.trim() + "\""
											: "Added Tourette's message: \"" + msg.trim() + "\"");
										return 1;
									}
									sendFailure(ctx, ModConfig.isKorean() ? "빈 메시지는 추가할 수 없습니다." : "Cannot add an empty message.");
									return 0;
								})))
						.then(Commands.literal("remove")
							.then(Commands.argument("target", StringArgumentType.greedyString())
								.executes(ctx -> {
									String target = StringArgumentType.getString(ctx, "target");
									String removed = ModConfig.removeTouretteMessage(target);
									if (removed != null) {
										ConditionStore.syncConfig(ctx.getSource().getServer());
										sendSuccess(ctx, ModConfig.isKorean()
											? "투렛 메시지를 삭제했습니다: \"" + removed + "\""
											: "Removed Tourette's message: \"" + removed + "\"");
										return 1;
									}
									sendFailure(ctx, ModConfig.isKorean()
										? "해당 번호 또는 메시지를 찾을 수 없습니다: " + target
										: "No matching index or message found: " + target);
									return 0;
								})))
						.then(Commands.literal("clear")
							.executes(ctx -> {
								ModConfig.clearTouretteMessages();
								ConditionStore.syncConfig(ctx.getSource().getServer());
								sendSuccess(ctx, ModConfig.isKorean()
									? "커스텀 투렛 메시지를 모두 초기화했습니다. (기본 랜덤 단어 사용)"
									: "Cleared all custom Tourette's messages. (Reverted to random Minecraft words)");
								return 1;
							}))))
		);

		dispatcher.register(
			Commands.literal("dis")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.redirect(root)
		);

		dispatcher.register(Commands.literal("질병도감").executes(DisabilityCommands::executeOpenGuide));
		dispatcher.register(Commands.literal("도감").executes(DisabilityCommands::executeOpenGuide));
		dispatcher.register(Commands.literal("disbook").executes(DisabilityCommands::executeOpenGuide));
	}

	private static int executeOpenGuide(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		ServerPlayer player = ctx.getSource().getPlayerOrException();
		boolean isOp = Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(ctx.getSource());
		List<UUID> ids = new ArrayList<>();
		List<String> names = new ArrayList<>();
		if (isOp) {
			ids.add(player.getUUID());
			names.add(player.getName().getString());
			for (ServerPlayer online : ctx.getSource().getServer().getPlayerList().getPlayers()) {
				if (!online.getUUID().equals(player.getUUID())) {
					ids.add(online.getUUID());
					names.add(online.getName().getString());
				}
			}
		}
		ConditionStore.syncAll(ctx.getSource().getServer());
		ServerPlayNetworking.send(player, new ModPackets.OpenGuiPacket(
			ModPackets.OpenGuiPacket.GUI_GUIDE, 0, isOp, ids, names));
		return 1;
	}

	private static CompletableFuture<Suggestions> suggestNames(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
		builder.suggest("all");
		builder.suggest("random");
		for (ConditionType type : ConditionType.values()) {
			builder.suggest(type.name().toLowerCase());
		}
		return builder.buildFuture();
	}

	private static int executeGive(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
		String arg = StringArgumentType.getString(ctx, "name");

		if (arg.equalsIgnoreCase("all")) {
			for (ServerPlayer player : players) {
				ConditionStore.grantAll(player.getUUID());
			}
			ConditionStore.syncAll(ctx.getSource().getServer());
			sendSuccess(ctx, "Granted all disabilities to " + formatNames(players) + ".");
			return players.size();
		}

		if (arg.equalsIgnoreCase("random")) {
			return executeNext(ctx, players);
		}

		ConditionType condition = ConditionType.parse(arg);
		if (condition == null) {
			sendFailure(ctx, "Unknown disability: " + arg);
			return 0;
		}

		int affected = 0;
		for (ServerPlayer player : players) {
			if (ConditionStore.grant(player.getUUID(), condition)) {
				ConditionStore.notifyPlayer(player, condition, "received");
				affected++;
			}
		}
		ConditionStore.syncAll(ctx.getSource().getServer());
		sendSuccess(ctx, ModConfig.isKorean()
			? affected + "명에게 " + condition.getDisplayName() + " 장애를 부여했습니다."
			: "Granted " + condition.getDisplayName() + " to " + affected + " player(s).");
		return affected;
	}

	private static int executeRemove(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
		String arg = StringArgumentType.getString(ctx, "name");

		if (arg.equalsIgnoreCase("all")) {
			for (ServerPlayer player : players) {
				ConditionStore.revokeAll(player.getUUID());
			}
			ConditionStore.syncAll(ctx.getSource().getServer());
			sendSuccess(ctx, "Cleared all disabilities from " + formatNames(players) + ".");
			return players.size();
		}

		if (arg.equalsIgnoreCase("random")) {
			int removed = 0;
			for (ServerPlayer player : players) {
				if (ConditionStore.revokeRandom(player.getUUID()) != null) {
					removed++;
				}
			}
			ConditionStore.syncAll(ctx.getSource().getServer());
			sendSuccess(ctx, "Removed a random disability from " + removed + " player(s).");
			return removed;
		}

		ConditionType condition = ConditionType.parse(arg);
		if (condition == null) {
			sendFailure(ctx, "Unknown disability: " + arg);
			return 0;
		}

		int removed = 0;
		for (ServerPlayer player : players) {
			if (ConditionStore.revoke(player.getUUID(), condition)) {
				removed++;
			}
		}
		ConditionStore.syncAll(ctx.getSource().getServer());
		sendSuccess(ctx, ModConfig.isKorean()
			? removed + "명에게서 " + condition.getDisplayName() + " 장애를 제거했습니다."
			: "Removed " + condition.getDisplayName() + " from " + removed + " player(s).");
		return removed;
	}

	private static int executeNext(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
		int grantedCount = 0;
		for (ServerPlayer player : players) {
			ConditionType granted = ConditionStore.grantRandom(player.getUUID());
			if (granted != null) {
				ConditionStore.notifyPlayer(player, granted, "developed");
				grantedCount++;
			}
		}
		ConditionStore.syncAll(ctx.getSource().getServer());
		if (grantedCount == 0) {
			sendSuccess(ctx, "Target player(s) already have every disability.");
		} else {
			sendSuccess(ctx, "Granted a random disability to " + grantedCount + " player(s).");
		}
		return grantedCount;
	}

	private static int executeReset(CommandContext<CommandSourceStack> ctx) {
		ConditionStore.resetAll();
		ConditionStore.syncAll(ctx.getSource().getServer());
		sendSuccess(ctx, "Reset all disabilities and restored the 10-minute timer.");
		return 1;
	}

	private static int executeList(CommandContext<CommandSourceStack> ctx, Collection<ServerPlayer> players) {
		for (ServerPlayer player : players) {
			Set<ConditionType> active = ConditionStore.getActive(player.getUUID());
			String none = ModConfig.isKorean() ? "없음" : "None";
			String summary = active.isEmpty()
				? none
				: active.stream().map(ConditionType::getDisplayName).collect(Collectors.joining(", "));
			sendSuccess(ctx, player.getName().getString() + " (" + active.size() + "): " + summary);
		}
		return players.size();
	}

	private static int executeLang(CommandContext<CommandSourceStack> ctx, ModConfig.Language lang) {
		ModConfig.setLanguage(lang);
		String msg = ModConfig.isKorean()
			? "표시 언어를 '" + lang.key + "'(으)로 변경하고 config/disabilities.properties에 저장했습니다."
			: "Display language set to '" + lang.key + "' and saved to config/disabilities.properties.";
		sendSuccess(ctx, msg);
		return 1;
	}

	private static String formatNames(Collection<ServerPlayer> players) {
		return players.stream().map(p -> p.getName().getString()).collect(Collectors.joining(", "));
	}

	private static void sendSuccess(CommandContext<CommandSourceStack> ctx, String text) {
		ctx.getSource().sendSuccess(() -> Component.literal("[Disabilities] ").withStyle(ChatFormatting.GOLD)
			.append(Component.literal(text).withStyle(ChatFormatting.WHITE)), false);
	}

	private static void sendFailure(CommandContext<CommandSourceStack> ctx, String text) {
		ctx.getSource().sendFailure(Component.literal("[Disabilities] " + text));
	}
}

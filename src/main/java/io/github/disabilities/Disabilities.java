package io.github.disabilities;

import io.github.disabilities.command.DisabilityCommands;
import io.github.disabilities.config.ModConfig;
import io.github.disabilities.item.ModItems;
import io.github.disabilities.logic.ConditionTickHandler;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.EntitySleepEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Disabilities implements ModInitializer {
	public static final String MOD_ID = "disabilities";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModConfig.load();
		ModItems.register();

		PayloadTypeRegistry.clientboundPlay().register(ModPackets.SyncStatePacket.ID, ModPackets.SyncStatePacket.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.EpisodePacket.ID, ModPackets.EpisodePacket.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.CompulsionPacket.ID, ModPackets.CompulsionPacket.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.ConcealedSlotsPacket.ID, ModPackets.ConcealedSlotsPacket.CODEC);
		PayloadTypeRegistry.clientboundPlay().register(ModPackets.OpenGuiPacket.ID, ModPackets.OpenGuiPacket.CODEC);

		PayloadTypeRegistry.serverboundPlay().register(ModPackets.CureConditionPacket.ID, ModPackets.CureConditionPacket.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ModPackets.CureConditionPacket.ID, (payload, context) ->
			context.server().execute(() -> handleCureRequest(context.player(), payload))
		);

		PayloadTypeRegistry.serverboundPlay().register(ModPackets.AdminToggleConditionPacket.ID, ModPackets.AdminToggleConditionPacket.CODEC);
		ServerPlayNetworking.registerGlobalReceiver(ModPackets.AdminToggleConditionPacket.ID, (payload, context) ->
			context.server().execute(() -> handleAdminToggleRequest(context.player(), payload))
		);

		DisabilityCommands.register();

		ServerLifecycleEvents.SERVER_STARTED.register(ConditionStore::load);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> ConditionStore.save());

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			ConditionStore.tick(server);
			ConditionTickHandler.tickServer(server);
		});

		ServerLivingEntityEvents.AFTER_DAMAGE.register(ConditionTickHandler::onAfterDamage);

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> ConditionStore.syncAll(server));
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ConditionTickHandler.removePlayer(handler.player.getUUID()));

		PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
			if (player instanceof ServerPlayer serverPlayer) {
				ConditionTickHandler.onBlockBroken(serverPlayer, state.getBlock());
			}
		});

		EntitySleepEvents.ALLOW_SLEEPING.register((entity, pos) -> {
			if (entity instanceof Player player && !player.level().isClientSide()
				&& ConditionStore.has(player.getUUID(), ConditionType.INSOMNIA)) {
				return Player.BedSleepingProblem.OTHER_PROBLEM;
			}
			return null;
		});

		UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
			if (!world.isClientSide() && ConditionStore.has(player.getUUID(), ConditionType.INSOMNIA)) {
				if (world.getBlockState(hitResult.getBlockPos()).is(BlockTags.BEDS)
					&& world.dimension() != Level.NETHER && world.dimension() != Level.END) {
					if (player instanceof ServerPlayer sp) {
						String msg = ModConfig.isKorean() ? "불면증 때문에 잠을 잘 수 없습니다." : "You cannot fall asleep (Insomnia).";
						sp.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.GRAY), true);
					}
					return InteractionResult.FAIL;
				}
			}
			return InteractionResult.PASS;
		});
	}

	private static void handleCureRequest(ServerPlayer player, ModPackets.CureConditionPacket payload) {
		ConditionType[] all = ConditionType.values();
		int idx = payload.conditionOrdinal();
		if (idx < 0 || idx >= all.length) {
			return;
		}
		ConditionType target = all[idx];
		if (!ConditionStore.has(player.getUUID(), target)) {
			return;
		}

		InteractionHand preferredHand = payload.handOrdinal() == InteractionHand.OFF_HAND.ordinal()
			? InteractionHand.OFF_HAND
			: InteractionHand.MAIN_HAND;
		ItemStack stack = player.getItemInHand(preferredHand);
		if (!stack.is(ModItems.CURE)) {
			InteractionHand other = preferredHand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
			stack = player.getItemInHand(other);
			if (!stack.is(ModItems.CURE)) {
				return;
			}
		}

		if (ConditionStore.revoke(player.getUUID(), target)) {
			if (!player.isCreative()) {
				stack.shrink(1);
			}
			player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8f, 1.25f);
			Component msg = ModConfig.isKorean()
				? Component.literal("[Disabilities] ").withStyle(ChatFormatting.GREEN)
					.append(Component.literal(target.getDisplayName()).withStyle(target.color, ChatFormatting.BOLD))
					.append(Component.literal(" 질병이 치료되었습니다!").withStyle(ChatFormatting.WHITE))
				: Component.literal("[Disabilities] ").withStyle(ChatFormatting.GREEN)
					.append(Component.literal("Cured ").withStyle(ChatFormatting.WHITE))
					.append(Component.literal(target.getDisplayName()).withStyle(target.color, ChatFormatting.BOLD))
					.append(Component.literal("!").withStyle(ChatFormatting.WHITE));
			player.sendSystemMessage(msg);
			ConditionStore.syncAll(player.level().getServer());
		}
	}

	private static void handleAdminToggleRequest(ServerPlayer sender, ModPackets.AdminToggleConditionPacket payload) {
		if (!Commands.hasPermission(Commands.LEVEL_GAMEMASTERS).test(sender.createCommandSourceStack())) {
			return;
		}
		ConditionType[] all = ConditionType.values();
		int idx = payload.conditionOrdinal();
		if (idx < 0 || idx >= all.length) {
			return;
		}
		ConditionType condition = all[idx];
		ServerPlayer target = sender.level().getServer().getPlayerList().getPlayer(payload.targetPlayerId());
		if (target == null) {
			return;
		}

		boolean alreadyHas = ConditionStore.has(target.getUUID(), condition);
		if (alreadyHas) {
			if (ConditionStore.revoke(target.getUUID(), condition)) {
				Component feedback = ModConfig.isKorean()
					? Component.literal("[Disabilities] ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal(target.getName().getString() + "에게서 ").withStyle(ChatFormatting.WHITE))
						.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD))
						.append(Component.literal(" 질병을 제거했습니다.").withStyle(ChatFormatting.WHITE))
					: Component.literal("[Disabilities] ").withStyle(ChatFormatting.GOLD)
						.append(Component.literal("Removed ").withStyle(ChatFormatting.WHITE))
						.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD))
						.append(Component.literal(" from " + target.getName().getString() + ".").withStyle(ChatFormatting.WHITE));
				sender.sendSystemMessage(feedback);
			}
		} else {
			if (ConditionStore.grant(target.getUUID(), condition)) {
				ConditionStore.notifyPlayer(target, condition, "received");
				if (!sender.getUUID().equals(target.getUUID())) {
					Component feedback = ModConfig.isKorean()
						? Component.literal("[Disabilities] ").withStyle(ChatFormatting.GOLD)
							.append(Component.literal(target.getName().getString() + "에게 ").withStyle(ChatFormatting.WHITE))
							.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD))
							.append(Component.literal(" 질병을 발현시켰습니다.").withStyle(ChatFormatting.WHITE))
						: Component.literal("[Disabilities] ").withStyle(ChatFormatting.GOLD)
							.append(Component.literal("Granted ").withStyle(ChatFormatting.WHITE))
							.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD))
							.append(Component.literal(" to " + target.getName().getString() + ".").withStyle(ChatFormatting.WHITE));
					sender.sendSystemMessage(feedback);
				}
			}
		}
		ConditionStore.syncAll(sender.level().getServer());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}

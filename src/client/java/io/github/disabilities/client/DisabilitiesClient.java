package io.github.disabilities.client;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.disabilities.ConditionAccess;
import io.github.disabilities.ConditionType;
import io.github.disabilities.Disabilities;
import io.github.disabilities.client.screen.ConditionGuideScreen;
import io.github.disabilities.client.screen.CureSelectionScreen;
import io.github.disabilities.config.ModConfig;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class DisabilitiesClient implements ClientModInitializer {
	private static KeyMapping toggleListKey;

	@Override
	public void onInitializeClient() {
		ModConfig.load();
		ModConfig.setClientLanguageSupplier(() -> {
			var mc = net.minecraft.client.Minecraft.getInstance();
			return mc != null && mc.getLanguageManager() != null ? mc.getLanguageManager().getSelected() : "en_us";
		});

		KeyMapping.Category category = KeyMapping.Category.register(Disabilities.id("disabilities"));
		toggleListKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
			"key.disabilities.toggle_list",
			InputConstants.Type.KEYBOARD,
			InputConstants.KEY_K,
			category
		));

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.SyncStatePacket.ID, (payload, context) ->
			context.client().execute(() -> {
				Map<UUID, EnumSet<ConditionType>> map = new HashMap<>();
				List<UUID> ids = payload.playerIds();
				List<Long> masks = payload.masks();
				int count = Math.min(ids.size(), masks.size());
				for (int i = 0; i < count; i++) {
					map.put(ids.get(i), ConditionAccess.decodeMask(masks.get(i)));
				}
				ConditionAccess.replaceClientCache(map);
				ClientConditionState.setTimer(payload.remainingTicks(), payload.timerEnabled());
			})
		);

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.EpisodePacket.ID, (payload, context) ->
			context.client().execute(() -> {
				if (payload.episodeType() == ModPackets.EpisodePacket.TYPE_SEIZURE) {
					ClientConditionState.setSeizureTicks(payload.durationTicks());
				} else if (payload.episodeType() == ModPackets.EpisodePacket.TYPE_VERTIGO) {
					ClientConditionState.setVertigo(payload.durationTicks(), payload.direction());
				}
			})
		);

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.CompulsionPacket.ID, (payload, context) ->
			context.client().execute(() -> ClientConditionState.setCompulsionLine(payload.promptText()))
		);

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.ConcealedSlotsPacket.ID, (payload, context) ->
			context.client().execute(() -> ClientConditionState.setConcealedSlotMask(payload.slotMask()))
		);

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OpenGuiPacket.ID, (payload, context) ->
			context.client().execute(() -> {
				if (payload.guiType() == ModPackets.OpenGuiPacket.GUI_GUIDE) {
					context.client().gui.setScreen(new ConditionGuideScreen(
						payload.isOp(), payload.playerIds(), payload.playerNames()));
				} else if (payload.guiType() == ModPackets.OpenGuiPacket.GUI_CURE) {
					context.client().gui.setScreen(new CureSelectionScreen(payload.handOrdinal()));
				}
			})
		);

		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientConditionState.reset());

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (toggleListKey.consumeClick()) {
				ClientConditionState.toggleListVisible();
			}
			ClientConditionTick.tick(client);
		});

		ClientSendMessageEvents.ALLOW_CHAT.register(message -> {
			if (ClientConditionState.hasLocal(ConditionType.MUTE)) {
				var mc = net.minecraft.client.Minecraft.getInstance();
				if (mc.player != null) {
					String msg = ModConfig.isKorean()
						? "함묵증 때문에 채팅으로 말할 수 없습니다."
						: "You are Mute and cannot speak in chat.";
					mc.player.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.RED));
				}
				return false;
			}
			return true;
		});
	}
}

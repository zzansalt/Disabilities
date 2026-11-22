package io.github.disabilities.item;

import io.github.disabilities.ConditionStore;
import io.github.disabilities.config.ModConfig;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

public class CureItem extends Item {
	public CureItem(Properties properties) {
		super(properties);
	}

	@Override
	public Component getName(ItemStack stack) {
		return Component.literal(ModConfig.isKorean() ? "치료제" : "Cure").withStyle(ChatFormatting.AQUA);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			if (ConditionStore.getActive(serverPlayer.getUUID()).isEmpty()) {
				String msg = ModConfig.isKorean()
					? "치료할 질병이 없습니다."
					: "You have no conditions to cure.";
				serverPlayer.sendSystemMessage(Component.literal(msg).withStyle(ChatFormatting.YELLOW), true);
				return InteractionResult.FAIL;
			}
			ConditionStore.syncAll(serverPlayer.level().getServer());
			ServerPlayNetworking.send(serverPlayer, new ModPackets.OpenGuiPacket(
				ModPackets.OpenGuiPacket.GUI_CURE, hand.ordinal(), false, List.of(), List.of()));
		}
		return InteractionResult.SUCCESS;
	}
}

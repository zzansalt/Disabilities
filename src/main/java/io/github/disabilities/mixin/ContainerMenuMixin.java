package io.github.disabilities.mixin;

import io.github.disabilities.ConditionAccess;
import io.github.disabilities.ConditionType;
import io.github.disabilities.logic.ConditionTickHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerMenu.class)
public abstract class ContainerMenuMixin {
	@Inject(method = "clicked", at = @At("HEAD"), cancellable = true)
	private void disabilities$handleSlotClick(int slotId, int button, ContainerInput input, Player player, CallbackInfo ci) {
		if (ConditionAccess.has(player, ConditionType.KLEPTOMANIA)) {
			if (input == ContainerInput.THROW || slotId == AbstractContainerMenu.SLOT_CLICKED_OUTSIDE) {
				ci.cancel();
				return;
			}
		}
		if (player instanceof ServerPlayer serverPlayer) {
			ConditionTickHandler.onContainerSlotClicked(serverPlayer, (AbstractContainerMenu) (Object) this, slotId, button, input);
		}
	}
}

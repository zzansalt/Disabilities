package io.github.disabilities.client.mixin;

import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractContainerScreen.class)
public abstract class DementiaContainerMixin {
	@Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
	private void disabilities$maskConcealedSlot(GuiGraphicsExtractor graphics, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (slot.hasItem() && ClientConditionState.isSlotConcealed(slot.container, slot.getContainerSlot())) {
			int x = slot.x;
			int y = slot.y;
			graphics.fill(x, y, x + 16, y + 16, 0xCC1A1A26);
			boolean prev = ClientConditionState.isDyslexiaExempt();
			ClientConditionState.setDyslexiaExempt(true);
			graphics.text(Minecraft.getInstance().font, "?", x + 5, y + 4, 0xFFBBBBCC, true);
			ClientConditionState.setDyslexiaExempt(prev);
			ci.cancel();
		}
	}
}

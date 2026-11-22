package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.client.hud.ModHudRenderer;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Hud.class)
public abstract class HudOverlayMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Unique
	private boolean disabilities$crosshairPushed;

	@Inject(method = "extractCameraOverlays", at = @At("TAIL"))
	private void disabilities$renderGlaucomaBehindUi(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ModHudRenderer.renderGlaucomaOverlay(graphics);
	}

	@Inject(method = "extractRenderState", at = @At("TAIL"))
	private void disabilities$renderDisabilitiesHud(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		ModHudRenderer.renderHud(graphics);
	}

	@Inject(method = "extractCrosshair", at = @At("HEAD"))
	private void disabilities$pushParkinsonsCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		this.disabilities$crosshairPushed = false;
		if (!ClientConditionState.hasLocal(ConditionType.PARKINSONS)) {
			return;
		}
		float dx = ClientConditionState.getParkinsonsYaw() * 4.5f;
		float dy = ClientConditionState.getParkinsonsPitch() * 4.5f;
		graphics.pose().pushMatrix();
		graphics.pose().translate(dx, dy);
		this.disabilities$crosshairPushed = true;
	}

	@Inject(method = "extractCrosshair", at = @At("RETURN"))
	private void disabilities$popParkinsonsCrosshair(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker, CallbackInfo ci) {
		if (this.disabilities$crosshairPushed) {
			graphics.pose().popMatrix();
			this.disabilities$crosshairPushed = false;
		}
	}

	@Inject(method = "extractSlot", at = @At("HEAD"), cancellable = true)
	private void disabilities$hideConcealedHotbarSlot(GuiGraphicsExtractor graphics, int x, int y, DeltaTracker delta, Player player, ItemStack stack, int seed, CallbackInfo ci) {
		if (stack.isEmpty() || player == null || !ClientConditionState.hasLocal(ConditionType.DEMENTIA)) {
			return;
		}
		Inventory inv = player.getInventory();
		for (int i = 0; i < 9; i++) {
			if (inv.getItem(i) == stack && ClientConditionState.isSlotConcealed(inv, i)) {
				graphics.fill(x, y, x + 16, y + 16, 0xCC1A1A26);
				boolean prev = ClientConditionState.isDyslexiaExempt();
				ClientConditionState.setDyslexiaExempt(true);
				graphics.text(this.minecraft.font, "?", x + 5, y + 4, 0xFFBBBBCC, true);
				ClientConditionState.setDyslexiaExempt(prev);
				ci.cancel();
				return;
			}
		}
	}
}

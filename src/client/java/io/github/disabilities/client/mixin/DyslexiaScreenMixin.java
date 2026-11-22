package io.github.disabilities.client.mixin;

import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.client.hud.ModHudRenderer;
import io.github.disabilities.client.screen.ConditionGuideScreen;
import io.github.disabilities.client.screen.CureSelectionScreen;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
public abstract class DyslexiaScreenMixin {
	@Unique
	private boolean disabilities$prevDyslexiaExempt;

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("HEAD"))
	private void disabilities$beginScreenRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		this.disabilities$prevDyslexiaExempt = ClientConditionState.isDyslexiaExempt();
		Object self = this;
		if (self instanceof PauseScreen || self instanceof OptionsScreen || self instanceof ChatScreen
			|| self instanceof ConditionGuideScreen || self instanceof CureSelectionScreen) {
			ClientConditionState.setDyslexiaExempt(true);
		}
	}

	@Inject(method = "extractRenderStateWithTooltipAndSubtitles", at = @At("TAIL"))
	private void disabilities$endScreenRender(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		ClientConditionState.setDyslexiaExempt(this.disabilities$prevDyslexiaExempt);
		ModHudRenderer.renderScreenOverlay(graphics, mouseX, mouseY);
	}
}

package io.github.disabilities.client.hud;

import io.github.disabilities.ConditionType;
import io.github.disabilities.Disabilities;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.config.ModConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class ModHudRenderer {
	private static final int ROW_HEIGHT = 11;
	private static final Identifier GLAUCOMA_TEXTURE = Disabilities.id("textures/gui/glaucoma_vignette.png");

	private ModHudRenderer() {
	}

	public static void renderHud(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden()) {
			return;
		}

		renderNarcolepsyOverlay(graphics, mc);
		renderCompulsionPrompt(graphics, mc);
		if (mc.gui.screen() == null) {
			renderConditionList(graphics, mc, -1, -1);
		}
	}

	public static void renderGlaucomaOverlay(GuiGraphicsExtractor graphics) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || !ClientConditionState.hasLocal(ConditionType.GLAUCOMA)) {
			return;
		}
		int w = graphics.guiWidth();
		int h = graphics.guiHeight();
		graphics.blit(RenderPipelines.GUI_TEXTURED, GLAUCOMA_TEXTURE, 0, 0, 0.0f, 0.0f, w, h, 256, 256, 256, 256);
	}

	public static void renderScreenOverlay(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player == null || mc.gui.hud.isHidden()) {
			return;
		}
		renderConditionList(graphics, mc, mouseX, mouseY);
	}

	private static void renderConditionList(GuiGraphicsExtractor graphics, Minecraft mc, int mouseX, int mouseY) {
		if (!ClientConditionState.isListVisible()) {
			return;
		}

		Font font = mc.font;
		int x = 6;
		int y = 6;

		boolean prevExempt = ClientConditionState.isDyslexiaExempt();
		ClientConditionState.setDyslexiaExempt(true);
		try {
			if (ClientConditionState.isTimerEnabled()) {
				int sec = (ClientConditionState.getRemainingTicks() + 19) / 20;
				int min = sec / 60;
				int rem = sec % 60;
				String timerText = ModConfig.isKorean()
					? String.format("다음 장애: %02d:%02d", min, rem)
					: String.format("Next Condition: %02d:%02d", min, rem);
				graphics.fill(x - 2, y - 2, x + font.width(timerText) + 4, y + 10, 0x80000000);
				graphics.text(font, timerText, x, y, 0xFFFFD166, true);
				y += ROW_HEIGHT + 3;
			}

			Set<ConditionType> active = ClientConditionState.localConditions();
			if (active.isEmpty()) {
				return;
			}

			boolean anosognosia = ClientConditionState.hasLocal(ConditionType.ANOSOGNOSIA);
			Iterable<ConditionType> shown = anosognosia ? List.of(ConditionType.ANOSOGNOSIA) : active;

			ConditionType hovered = null;
			for (ConditionType cond : shown) {
				Component label = Component.literal("• " + cond.getDisplayName()).withStyle(cond.color);
				int w = font.width(label);
				boolean isHover = mouseX >= x - 2 && mouseX <= x + w + 4 && mouseY >= y - 1 && mouseY <= y + ROW_HEIGHT - 1;
				graphics.fill(x - 2, y - 1, x + w + 4, y + ROW_HEIGHT - 1, isHover ? 0xA0333333 : 0x70000000);
				graphics.text(font, label, x, y, 0xFFFFFFFF, true);
				if (isHover) {
					hovered = cond;
				}
				y += ROW_HEIGHT;
			}

			if (hovered != null) {
				renderSimpleTooltip(graphics, font, hovered, mouseX, mouseY);
			}
		} finally {
			ClientConditionState.setDyslexiaExempt(prevExempt);
		}
	}

	private static void renderSimpleTooltip(GuiGraphicsExtractor graphics, Font font, ConditionType condition, int mouseX, int mouseY) {
		int maxWidth = 190;
		List<FormattedCharSequence> lines = new ArrayList<>();
		lines.add(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD).getVisualOrderText());
		lines.addAll(font.split(Component.literal(condition.getDescription()).withStyle(ChatFormatting.GRAY), maxWidth));

		int boxW = 0;
		for (FormattedCharSequence line : lines) {
			boxW = Math.max(boxW, font.width(line));
		}
		int boxH = lines.size() * 10 + 6;
		int tx = mouseX + 12;
		int ty = Math.max(4, mouseY - 4);
		int screenW = graphics.guiWidth();
		if (tx + boxW + 8 > screenW) {
			tx = Math.max(4, mouseX - boxW - 14);
		}

		graphics.fill(tx - 4, ty - 4, tx + boxW + 4, ty + boxH, 0xE0101018);
		int ly = ty;
		for (FormattedCharSequence line : lines) {
			graphics.text(font, line, tx, ly, 0xFFFFFFFF, true);
			ly += 10;
		}
	}

	private static void renderCompulsionPrompt(GuiGraphicsExtractor graphics, Minecraft mc) {
		String line = ClientConditionState.getCompulsionLine();
		if (line == null || line.isBlank()) {
			return;
		}
		Font font = mc.font;
		boolean prevExempt = ClientConditionState.isDyslexiaExempt();
		ClientConditionState.setDyslexiaExempt(true);
		try {
			int w = font.width(line);
			int cx = (graphics.guiWidth() - w) / 2;
			int cy = graphics.guiHeight() - 72;
			boolean isAlert = line.startsWith("OCD FAILED") || line.contains("too much")
				|| line.startsWith("강박 실패") || line.contains("초과");
			int border = isAlert ? 0xFFE53935 : 0xFF4F8CC9;
			int bg = isAlert ? 0xC87A0B0B : 0xC0102038;
			graphics.fill(cx - 7, cy - 4, cx + w + 7, cy + 12, border);
			graphics.fill(cx - 6, cy - 3, cx + w + 6, cy + 11, bg);
			graphics.text(font, line, cx, cy, 0xFFFFFFFF, true);
		} finally {
			ClientConditionState.setDyslexiaExempt(prevExempt);
		}
	}

	private static void renderNarcolepsyOverlay(GuiGraphicsExtractor graphics, Minecraft mc) {
		float alpha = ClientConditionState.getNarcolepsyOverlayAlpha();
		if (alpha <= 0.01f) {
			return;
		}
		int a = Math.clamp((int) (alpha * 255.0f), 0, 255);
		int color = (a << 24);
		graphics.fill(0, 0, graphics.guiWidth(), graphics.guiHeight(), color);

		if (alpha > 0.65f) {
			String text = "Zzz...";
			int w = mc.font.width(text);
			graphics.text(mc.font, text, (graphics.guiWidth() - w) / 2, graphics.guiHeight() / 2 - 4, (a << 24) | 0xCCCCCC, true);
		}
	}
}

package io.github.disabilities.client.screen;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.config.ModConfig;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CureSelectionScreen extends Screen {
	private static final int PER_PAGE = 5;

	private final int handOrdinal;
	private final List<ConditionType> activeSnapshot;
	private final boolean maskedByAnosognosia;
	private int page = 0;

	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;

	private final List<ConditionButtonEntry> buttonEntries = new ArrayList<>();

	private record ConditionButtonEntry(ConditionType condition, Button button) {
	}

	public CureSelectionScreen(int handOrdinal) {
		super(Component.literal(ModConfig.isKorean() ? "치료제" : "Cure"));
		this.handOrdinal = handOrdinal;
		this.maskedByAnosognosia = ClientConditionState.hasLocal(ConditionType.ANOSOGNOSIA);
		this.activeSnapshot = new ArrayList<>(ClientConditionState.localConditions());
		if (this.maskedByAnosognosia) {
			Collections.shuffle(this.activeSnapshot);
		}
	}

	@Override
	protected void init() {
		this.clearWidgets();
		this.buttonEntries.clear();

		boolean ko = ModConfig.isKorean();
		this.panelWidth = Math.min(310, this.width - 24);
		this.panelHeight = Math.min(230, this.height - 28);
		this.panelLeft = (this.width - this.panelWidth) / 2;
		this.panelTop = (this.height - this.panelHeight) / 2;

		int totalPages = Math.max(1, (this.activeSnapshot.size() + PER_PAGE - 1) / PER_PAGE);
		if (this.page >= totalPages) {
			this.page = totalPages - 1;
		}

		int start = this.page * PER_PAGE;
		int end = Math.min(this.activeSnapshot.size(), start + PER_PAGE);

		int btnX = this.panelLeft + 16;
		int btnW = this.panelWidth - 32;
		int y = this.panelTop + 36;

		for (int i = start; i < end; i++) {
			ConditionType condition = this.activeSnapshot.get(i);
			Component label;
			if (this.maskedByAnosognosia) {
				label = Component.literal(ko ? "치료: " : "Cure: ")
					.withStyle(ChatFormatting.WHITE)
					.append(Component.literal("???").withStyle(ChatFormatting.GRAY, ChatFormatting.BOLD));
			} else {
				label = Component.literal(ko ? "치료: " : "Cure: ")
					.withStyle(ChatFormatting.WHITE)
					.append(Component.literal(condition.getDisplayName()).withStyle(condition.color, ChatFormatting.BOLD));
			}
			Button btn = Button.builder(label, b -> {
				ClientPlayNetworking.send(new ModPackets.CureConditionPacket(condition.ordinal(), this.handOrdinal));
				this.onClose();
			}).bounds(btnX, y, btnW, 22).build();
			this.addRenderableWidget(btn);
			this.buttonEntries.add(new ConditionButtonEntry(condition, btn));
			y += 26;
		}

		int bottomY = this.panelTop + this.panelHeight - 26;
		if (totalPages > 1) {
			Button prev = Button.builder(Component.literal("◀"), b -> {
				if (this.page > 0) {
					this.page--;
					this.init();
				}
			}).bounds(this.panelLeft + 16, bottomY, 24, 18).build();
			prev.active = this.page > 0;
			this.addRenderableWidget(prev);

			Button next = Button.builder(Component.literal("▶"), b -> {
				if (this.page + 1 < totalPages) {
					this.page++;
					this.init();
				}
			}).bounds(this.panelLeft + 44, bottomY, 24, 18).build();
			next.active = this.page + 1 < totalPages;
			this.addRenderableWidget(next);
		}

		this.addRenderableWidget(Button.builder(Component.literal(ko ? "취소" : "Cancel"), b -> this.onClose())
			.bounds(this.panelLeft + this.panelWidth - 76, bottomY, 60, 18).build());
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean prevExempt = ClientConditionState.isDyslexiaExempt();
		ClientConditionState.setDyslexiaExempt(true);
		try {
			graphics.fill(0, 0, this.width, this.height, 0xA8061010);

			// Modal container with medical teal border
			graphics.fill(this.panelLeft - 1, this.panelTop - 1, this.panelLeft + this.panelWidth + 1, this.panelTop + this.panelHeight + 1, 0xFF2EA89A);
			graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xF0101A1D);

			// Header
			graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 28, 0xFF162C30);

			boolean ko = ModConfig.isKorean();
			String title = ko ? "치료제 - 치료할 질병 선택" : "Cure - Select a Condition to Heal";
			graphics.text(this.font, Component.literal(title).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD),
				this.panelLeft + 14, this.panelTop + 10, 0xFFFFFFFF, true);

			if (this.buttonEntries.isEmpty()) {
				String empty = ko ? "치료할 질병이 없습니다." : "You have no active conditions.";
				int ew = this.font.width(empty);
				graphics.text(this.font, empty, this.panelLeft + (this.panelWidth - ew) / 2, this.panelTop + 88, 0xFF9FB4B8, true);
			}

			super.extractRenderState(graphics, mouseX, mouseY, partialTick);

			// Description footer for hovered condition
			ConditionType hovered = null;
			for (ConditionButtonEntry entry : this.buttonEntries) {
				if (entry.button.isHovered()) {
					hovered = entry.condition;
					break;
				}
			}
			int descBoxTop = this.panelTop + this.panelHeight - 56;
			int descBoxBottom = this.panelTop + this.panelHeight - 31;
			graphics.fill(this.panelLeft + 14, descBoxTop, this.panelLeft + this.panelWidth - 14, descBoxBottom, 0xB00B1316);
			String footerText;
			if (this.maskedByAnosognosia) {
				footerText = ko
					? "??? (질병인식불능증으로 인해 어떤 질병인지 구분할 수 없습니다.)"
					: "??? (Anosognosia prevents you from identifying your conditions.)";
			} else {
				footerText = hovered != null
					? hovered.getDescription()
					: (ko ? "버튼에 마우스를 올리면 질병 설명이 표시됩니다." : "Hover over a condition to view its description.");
			}
			List<FormattedCharSequence> lines = this.font.split(
				Component.literal(footerText).withStyle(hovered != null && !this.maskedByAnosognosia ? ChatFormatting.WHITE : ChatFormatting.GRAY),
				this.panelWidth - 36
			);
			int ly = descBoxTop + 4;
			for (int i = 0; i < Math.min(2, lines.size()); i++) {
				graphics.text(this.font, lines.get(i), this.panelLeft + 18, ly, 0xFFE2E8F0, false);
				ly += 9;
			}
		} finally {
			ClientConditionState.setDyslexiaExempt(prevExempt);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

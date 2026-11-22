package io.github.disabilities.client.screen;

import io.github.disabilities.ConditionAccess;
import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import io.github.disabilities.config.ModConfig;
import io.github.disabilities.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class ConditionGuideScreen extends Screen {
	private static final int CARD_HEIGHT = 36;
	private static final int CARD_GAP = 4;

	private final boolean isOp;
	private final List<UUID> targetIds;
	private final List<String> targetNames;
	private int selectedTargetIndex = 0;

	private boolean activeOnly = false;
	private String searchQuery = "";
	private int scrollOffset = 0;

	private int panelLeft;
	private int panelTop;
	private int panelWidth;
	private int panelHeight;
	private int listTop;
	private int listBottom;

	private EditBox searchBox;
	private final List<RowToggleButton> rowButtons = new ArrayList<>();

	private record RowToggleButton(ConditionType condition, Button button) {
	}

	public ConditionGuideScreen(boolean isOp, List<UUID> targetIds, List<String> targetNames) {
		super(Component.literal(ModConfig.isKorean() ? "질병 도감" : "Disability Encyclopedia"));
		this.isOp = isOp && targetIds != null && !targetIds.isEmpty();
		this.targetIds = targetIds == null ? List.of() : List.copyOf(targetIds);
		this.targetNames = targetNames == null ? List.of() : List.copyOf(targetNames);
	}

	private UUID selectedTargetId() {
		if (this.isOp && !this.targetIds.isEmpty()) {
			int idx = Math.clamp(this.selectedTargetIndex, 0, this.targetIds.size() - 1);
			return this.targetIds.get(idx);
		}
		return this.minecraft != null && this.minecraft.player != null ? this.minecraft.player.getUUID() : null;
	}

	private String selectedTargetName() {
		if (this.isOp && !this.targetNames.isEmpty()) {
			int idx = Math.clamp(this.selectedTargetIndex, 0, this.targetNames.size() - 1);
			return this.targetNames.get(idx);
		}
		return "";
	}

	private Set<ConditionType> viewedActiveSet() {
		if (this.isOp) {
			UUID id = selectedTargetId();
			return id == null ? Set.of() : ConditionAccess.clientConditions(id);
		}
		if (ClientConditionState.hasLocal(ConditionType.ANOSOGNOSIA)) {
			return Set.of();
		}
		return ClientConditionState.localConditions();
	}

	@Override
	protected void init() {
		this.clearWidgets();
		this.rowButtons.clear();

		boolean ko = ModConfig.isKorean();
		this.panelWidth = Math.min(396, this.width - 20);
		this.panelHeight = Math.min(282, this.height - 20);
		this.panelLeft = (this.width - this.panelWidth) / 2;
		this.panelTop = (this.height - this.panelHeight) / 2;

		int searchY = this.panelTop + 32;
		int searchW = this.panelWidth - 46;
		this.searchBox = new EditBox(this.font, this.panelLeft + 10, searchY, searchW, 18,
			Component.literal(ko ? "검색" : "Search"));
		this.searchBox.setMaxLength(48);
		this.searchBox.setValue(this.searchQuery);
		this.searchBox.setHint(Component.literal(ko ? "질병 이름 또는 설명 검색..." : "Search name or description...")
			.withStyle(ChatFormatting.DARK_GRAY));
		this.searchBox.setResponder(text -> {
			this.searchQuery = text == null ? "" : text;
			this.scrollOffset = 0;
			rebuildRowButtons();
		});
		this.addRenderableWidget(this.searchBox);

		this.addRenderableWidget(Button.builder(Component.literal("✕"), btn -> {
			this.searchQuery = "";
			if (this.searchBox != null) {
				this.searchBox.setValue("");
			}
			this.scrollOffset = 0;
			rebuildRowButtons();
		}).bounds(this.panelLeft + 10 + searchW + 4, searchY, 22, 18).build());

		// OP Target Player Selector Bar
		if (this.isOp) {
			int barY = searchY + 22;
			if (this.targetIds.size() > 1) {
				this.addRenderableWidget(Button.builder(Component.literal("◀"), btn -> {
					this.selectedTargetIndex = (this.selectedTargetIndex - 1 + this.targetIds.size()) % this.targetIds.size();
					this.scrollOffset = 0;
					this.init();
				}).bounds(this.panelLeft + this.panelWidth - 58, barY, 22, 18).build());

				this.addRenderableWidget(Button.builder(Component.literal("▶"), btn -> {
					this.selectedTargetIndex = (this.selectedTargetIndex + 1) % this.targetIds.size();
					this.scrollOffset = 0;
					this.init();
				}).bounds(this.panelLeft + this.panelWidth - 34, barY, 22, 18).build());
			}
			this.listTop = barY + 22;
		} else {
			this.listTop = searchY + 22;
		}
		this.listBottom = this.panelTop + this.panelHeight - 30;

		rebuildRowButtons();

		boolean hideActiveStatus = !this.isOp && ClientConditionState.hasLocal(ConditionType.ANOSOGNOSIA);
		int activeCount = hideActiveStatus ? 0 : viewedActiveSet().size();

		String filterLabel = this.activeOnly
			? (ko ? "보유 중 (" + activeCount + ")" : "Active (" + activeCount + ")")
			: (ko ? "전체 (" + ConditionType.values().length + ")" : "All (" + ConditionType.values().length + ")");

		this.addRenderableWidget(Button.builder(Component.literal(filterLabel), btn -> {
			this.activeOnly = !this.activeOnly;
			this.scrollOffset = 0;
			this.init();
		}).bounds(this.panelLeft + 10, this.panelTop + this.panelHeight - 24, 96, 18).build());

		String langLabel = ko ? "언어: 한국어" : "Lang: EN";
		this.addRenderableWidget(Button.builder(Component.literal(langLabel), btn -> {
			ModConfig.setLanguage(ModConfig.isKorean() ? ModConfig.Language.EN : ModConfig.Language.KO);
			this.init();
		}).bounds(this.panelLeft + 112, this.panelTop + this.panelHeight - 24, 84, 18).build());

		this.addRenderableWidget(Button.builder(Component.literal("▲"), btn -> scrollBy(-1))
			.bounds(this.panelLeft + this.panelWidth - 118, this.panelTop + this.panelHeight - 24, 22, 18).build());

		this.addRenderableWidget(Button.builder(Component.literal("▼"), btn -> scrollBy(1))
			.bounds(this.panelLeft + this.panelWidth - 94, this.panelTop + this.panelHeight - 24, 22, 18).build());

		this.addRenderableWidget(Button.builder(Component.literal(ko ? "닫기" : "Close"), btn -> this.onClose())
			.bounds(this.panelLeft + this.panelWidth - 66, this.panelTop + this.panelHeight - 24, 56, 18).build());
	}

	private void rebuildRowButtons() {
		for (RowToggleButton rb : this.rowButtons) {
			this.removeWidget(rb.button);
		}
		this.rowButtons.clear();

		List<ConditionType> entries = displayedEntries();
		int rows = visibleRows();
		int maxOffset = Math.max(0, entries.size() - rows);
		if (this.scrollOffset > maxOffset) {
			this.scrollOffset = maxOffset;
		}

		if (!this.isOp) {
			return;
		}

		boolean ko = ModConfig.isKorean();
		UUID targetId = selectedTargetId();
		Set<ConditionType> activeSet = viewedActiveSet();
		int cardLeft = this.panelLeft + 10;
		int cardWidth = this.panelWidth - 28;
		int btnW = 46;
		int btnX = cardLeft + cardWidth - btnW - 5;
		int y = this.listTop;

		for (int i = 0; i < rows && (this.scrollOffset + i) < entries.size(); i++) {
			ConditionType cond = entries.get(this.scrollOffset + i);
			boolean active = activeSet.contains(cond);
			Component btnLabel = toggleLabel(active, ko);
			Button toggleBtn = Button.builder(btnLabel, b -> {
				if (targetId != null) {
					ClientPlayNetworking.send(new ModPackets.AdminToggleConditionPacket(targetId, cond.ordinal()));
				}
			}).bounds(btnX, y + 9, btnW, 18).build();
			this.addRenderableWidget(toggleBtn);
			this.rowButtons.add(new RowToggleButton(cond, toggleBtn));
			y += CARD_HEIGHT + CARD_GAP;
		}
	}

	private static Component toggleLabel(boolean active, boolean ko) {
		if (active) {
			return Component.literal(ko ? "제거" : "Remove").withStyle(ChatFormatting.RED);
		}
		return Component.literal(ko ? "발현" : "Grant").withStyle(ChatFormatting.GREEN);
	}

	private List<ConditionType> displayedEntries() {
		Set<ConditionType> activeSet = viewedActiveSet();
		String q = this.searchQuery == null ? "" : this.searchQuery.trim().toLowerCase(Locale.ROOT);
		String qNoSpace = q.replace(" ", "");

		List<ConditionType> result = new ArrayList<>();
		for (ConditionType cond : ConditionType.values()) {
			if (this.activeOnly && !activeSet.contains(cond)) {
				continue;
			}
			if (!q.isEmpty() && !matchesQuery(cond, q, qNoSpace)) {
				continue;
			}
			result.add(cond);
		}
		return result;
	}

	private static boolean matchesQuery(ConditionType cond, String q, String qNoSpace) {
		return cond.displayNameKo.toLowerCase(Locale.ROOT).contains(q)
			|| cond.displayNameKo.replace(" ", "").toLowerCase(Locale.ROOT).contains(qNoSpace)
			|| cond.displayName.toLowerCase(Locale.ROOT).contains(q)
			|| cond.name().toLowerCase(Locale.ROOT).contains(qNoSpace)
			|| cond.descriptionKo.toLowerCase(Locale.ROOT).contains(q)
			|| cond.description.toLowerCase(Locale.ROOT).contains(q);
	}

	private int visibleRows() {
		int avail = Math.max(CARD_HEIGHT, this.listBottom - this.listTop);
		return Math.max(1, avail / (CARD_HEIGHT + CARD_GAP));
	}

	private void scrollBy(int delta) {
		List<ConditionType> entries = displayedEntries();
		int maxOffset = Math.max(0, entries.size() - visibleRows());
		int next = Math.clamp(this.scrollOffset + delta, 0, maxOffset);
		if (next != this.scrollOffset) {
			this.scrollOffset = next;
			rebuildRowButtons();
		}
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (scrollY != 0.0) {
			scrollBy(scrollY > 0 ? -1 : 1);
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		boolean prevExempt = ClientConditionState.isDyslexiaExempt();
		ClientConditionState.setDyslexiaExempt(true);
		try {
			// Dim full background
			graphics.fill(0, 0, this.width, this.height, 0xA8080A10);

			// Main panel with border
			graphics.fill(this.panelLeft - 1, this.panelTop - 1, this.panelLeft + this.panelWidth + 1, this.panelTop + this.panelHeight + 1, 0xFF3E4B66);
			graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + this.panelHeight, 0xF0131722);

			// Header bar
			graphics.fill(this.panelLeft, this.panelTop, this.panelLeft + this.panelWidth, this.panelTop + 28, 0xFF1D2436);

			boolean ko = ModConfig.isKorean();
			boolean hideActive = !this.isOp && ClientConditionState.hasLocal(ConditionType.ANOSOGNOSIA);
			Set<ConditionType> activeSet = viewedActiveSet();
			List<ConditionType> entries = displayedEntries();

			String titleText = ko
				? "질병 도감 (" + entries.size() + "/" + ConditionType.values().length + "종)" + (this.isOp ? " [OP]" : "")
				: "Disability Encyclopedia (" + entries.size() + "/" + ConditionType.values().length + ")" + (this.isOp ? " [OP]" : "");
			graphics.text(this.font, Component.literal(titleText).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
				this.panelLeft + 12, this.panelTop + 10, 0xFFFFFFFF, true);

			String countBadge = ko
				? "보유 중: " + (hideActive ? "?" : activeSet.size())
				: "Active: " + (hideActive ? "?" : activeSet.size());
			int badgeW = this.font.width(countBadge);
			graphics.text(this.font, countBadge, this.panelLeft + this.panelWidth - badgeW - 12, this.panelTop + 10, 0xFFFF8888, true);

			// OP target bar
			if (this.isOp) {
				int barY = this.panelTop + 54;
				graphics.fill(this.panelLeft + 10, barY, this.panelLeft + this.panelWidth - 10, barY + 18, 0xCC182636);
				String targetLine = ko
					? "대상 플레이어: " + selectedTargetName() + " (" + (this.selectedTargetIndex + 1) + "/" + this.targetIds.size() + ")"
					: "Target Player: " + selectedTargetName() + " (" + (this.selectedTargetIndex + 1) + "/" + this.targetIds.size() + ")";
				graphics.text(this.font, Component.literal(targetLine).withStyle(ChatFormatting.AQUA),
					this.panelLeft + 16, barY + 5, 0xFFFFFFFF, true);

				for (RowToggleButton rb : this.rowButtons) {
					rb.button.setMessage(toggleLabel(activeSet.contains(rb.condition), ko));
				}
			}

			int rows = visibleRows();
			int maxOffset = Math.max(0, entries.size() - rows);
			if (this.scrollOffset > maxOffset) {
				this.scrollOffset = maxOffset;
			}

			int cardLeft = this.panelLeft + 10;
			int cardWidth = this.panelWidth - 28;
			int textWidth = this.isOp ? cardWidth - 62 : cardWidth - 14;

			if (entries.isEmpty()) {
				String empty = !this.searchQuery.isBlank()
					? (ko ? "검색 결과가 없습니다." : "No matching conditions found.")
					: (ko ? "표시할 질병이 없습니다." : "No conditions to display.");
				int ew = this.font.width(empty);
				graphics.text(this.font, empty, this.panelLeft + (this.panelWidth - ew) / 2, (this.listTop + this.listBottom) / 2 - 4, 0xFF99A2B2, true);
			} else {
				int y = this.listTop;
				for (int i = 0; i < rows && (this.scrollOffset + i) < entries.size(); i++) {
					ConditionType cond = entries.get(this.scrollOffset + i);
					boolean hasIt = activeSet.contains(cond);

					int bgColor = hasIt ? 0xE02A1820 : 0xD01B2030;
					graphics.fill(cardLeft, y, cardLeft + cardWidth, y + CARD_HEIGHT, bgColor);

					int accent = accentColor(cond.color);
					graphics.fill(cardLeft, y, cardLeft + 3, y + CARD_HEIGHT, accent);

					Component nameComp = Component.literal(cond.getDisplayName()).withStyle(cond.color, ChatFormatting.BOLD);
					graphics.text(this.font, nameComp, cardLeft + 8, y + 4, 0xFFFFFFFF, true);

					if (hasIt) {
						String tag = ko ? "[보유 중]" : "[ACTIVE]";
						int nameW = this.font.width(nameComp);
						int tagX = this.isOp ? (cardLeft + 14 + nameW) : (cardLeft + cardWidth - this.font.width(tag) - 6);
						graphics.text(this.font, tag, tagX, y + 4, 0xFFFF6B6B, true);
					}

					List<FormattedCharSequence> descLines = this.font.split(
						Component.literal(cond.getDescription()).withStyle(ChatFormatting.GRAY),
						textWidth
					);
					int dy = y + 16;
					for (int lineIdx = 0; lineIdx < Math.min(2, descLines.size()); lineIdx++) {
						graphics.text(this.font, descLines.get(lineIdx), cardLeft + 8, dy, 0xFFCBD5E1, false);
						dy += 9;
					}

					y += CARD_HEIGHT + CARD_GAP;
				}
			}

			// Scrollbar
			if (entries.size() > rows) {
				int trackX = this.panelLeft + this.panelWidth - 12;
				int trackTop = this.listTop;
				int trackHeight = this.listBottom - this.listTop;
				graphics.fill(trackX, trackTop, trackX + 4, trackTop + trackHeight, 0x802A3348);

				int thumbH = Math.max(18, (int) ((double) rows / entries.size() * trackHeight));
				int thumbY = trackTop + (int) ((double) this.scrollOffset / maxOffset * (trackHeight - thumbH));
				graphics.fill(trackX, thumbY, trackX + 4, thumbY + thumbH, 0xFF7B93C4);
			}

			super.extractRenderState(graphics, mouseX, mouseY, partialTick);
		} finally {
			ClientConditionState.setDyslexiaExempt(prevExempt);
		}
	}

	private static int accentColor(ChatFormatting fmt) {
		return switch (fmt) {
			case DARK_BLUE -> 0xFF0000AA;
			case DARK_GREEN -> 0xFF00AA00;
			case DARK_AQUA -> 0xFF00AAAA;
			case DARK_RED -> 0xFFAA0000;
			case DARK_PURPLE -> 0xFFAA00AA;
			case GOLD -> 0xFFFFAA00;
			case GRAY -> 0xFFAAAAAA;
			case DARK_GRAY -> 0xFF555555;
			case BLUE -> 0xFF5555FF;
			case GREEN -> 0xFF55FF55;
			case AQUA -> 0xFF55FFFF;
			case RED -> 0xFFFF5555;
			case LIGHT_PURPLE -> 0xFFFF55FF;
			case YELLOW -> 0xFFFFFF55;
			case WHITE -> 0xFFFFFFFF;
			default -> 0xFFAAAAAA;
		};
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}

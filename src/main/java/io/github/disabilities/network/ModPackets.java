package io.github.disabilities.network;

import io.github.disabilities.Disabilities;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ModPackets {
	private ModPackets() {
	}

	public record SyncStatePacket(List<UUID> playerIds, List<Long> masks, int remainingTicks, boolean timerEnabled)
		implements CustomPacketPayload {

		public static final Type<SyncStatePacket> ID = new Type<>(Disabilities.id("sync_state"));
		public static final StreamCodec<RegistryFriendlyByteBuf, SyncStatePacket> CODEC = StreamCodec.of(
			(buf, packet) -> {
				int size = Math.min(packet.playerIds.size(), packet.masks.size());
				buf.writeVarInt(size);
				for (int i = 0; i < size; i++) {
					buf.writeUUID(packet.playerIds.get(i));
					buf.writeLong(packet.masks.get(i));
				}
				buf.writeVarInt(packet.remainingTicks);
				buf.writeBoolean(packet.timerEnabled);
			},
			buf -> {
				int size = buf.readVarInt();
				List<UUID> ids = new ArrayList<>(size);
				List<Long> masks = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					ids.add(buf.readUUID());
					masks.add(buf.readLong());
				}
				int remaining = buf.readVarInt();
				boolean enabled = buf.readBoolean();
				return new SyncStatePacket(ids, masks, remaining, enabled);
			}
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record EpisodePacket(int episodeType, int durationTicks, int direction) implements CustomPacketPayload {
		public static final int TYPE_SEIZURE = 1;
		public static final int TYPE_VERTIGO = 2;

		public static final Type<EpisodePacket> ID = new Type<>(Disabilities.id("episode"));
		public static final StreamCodec<RegistryFriendlyByteBuf, EpisodePacket> CODEC = StreamCodec.of(
			(buf, packet) -> {
				buf.writeVarInt(packet.episodeType);
				buf.writeVarInt(packet.durationTicks);
				buf.writeVarInt(packet.direction);
			},
			buf -> new EpisodePacket(buf.readVarInt(), buf.readVarInt(), buf.readVarInt())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record CompulsionPacket(String promptText) implements CustomPacketPayload {
		public static final Type<CompulsionPacket> ID = new Type<>(Disabilities.id("compulsion"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CompulsionPacket> CODEC = StreamCodec.of(
			(buf, packet) -> buf.writeUtf(packet.promptText == null ? "" : packet.promptText),
			buf -> new CompulsionPacket(buf.readUtf())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record ConcealedSlotsPacket(long slotMask) implements CustomPacketPayload {
		public static final Type<ConcealedSlotsPacket> ID = new Type<>(Disabilities.id("concealed_slots"));
		public static final StreamCodec<RegistryFriendlyByteBuf, ConcealedSlotsPacket> CODEC = StreamCodec.of(
			(buf, packet) -> buf.writeLong(packet.slotMask),
			buf -> new ConcealedSlotsPacket(buf.readLong())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record OpenGuiPacket(int guiType, int handOrdinal, boolean isOp, List<UUID> playerIds, List<String> playerNames)
		implements CustomPacketPayload {
		public static final int GUI_GUIDE = 1;
		public static final int GUI_CURE = 2;

		public static final Type<OpenGuiPacket> ID = new Type<>(Disabilities.id("open_gui"));
		public static final StreamCodec<RegistryFriendlyByteBuf, OpenGuiPacket> CODEC = StreamCodec.of(
			(buf, packet) -> {
				buf.writeVarInt(packet.guiType);
				buf.writeVarInt(packet.handOrdinal);
				buf.writeBoolean(packet.isOp);
				int size = Math.min(packet.playerIds.size(), packet.playerNames.size());
				buf.writeVarInt(size);
				for (int i = 0; i < size; i++) {
					buf.writeUUID(packet.playerIds.get(i));
					buf.writeUtf(packet.playerNames.get(i));
				}
			},
			buf -> {
				int guiType = buf.readVarInt();
				int handOrdinal = buf.readVarInt();
				boolean isOp = buf.readBoolean();
				int size = buf.readVarInt();
				List<UUID> ids = new ArrayList<>(size);
				List<String> names = new ArrayList<>(size);
				for (int i = 0; i < size; i++) {
					ids.add(buf.readUUID());
					names.add(buf.readUtf());
				}
				return new OpenGuiPacket(guiType, handOrdinal, isOp, ids, names);
			}
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record CureConditionPacket(int conditionOrdinal, int handOrdinal) implements CustomPacketPayload {
		public static final Type<CureConditionPacket> ID = new Type<>(Disabilities.id("cure_condition"));
		public static final StreamCodec<RegistryFriendlyByteBuf, CureConditionPacket> CODEC = StreamCodec.of(
			(buf, packet) -> {
				buf.writeVarInt(packet.conditionOrdinal);
				buf.writeVarInt(packet.handOrdinal);
			},
			buf -> new CureConditionPacket(buf.readVarInt(), buf.readVarInt())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}

	public record AdminToggleConditionPacket(UUID targetPlayerId, int conditionOrdinal) implements CustomPacketPayload {
		public static final Type<AdminToggleConditionPacket> ID = new Type<>(Disabilities.id("admin_toggle_condition"));
		public static final StreamCodec<RegistryFriendlyByteBuf, AdminToggleConditionPacket> CODEC = StreamCodec.of(
			(buf, packet) -> {
				buf.writeUUID(packet.targetPlayerId);
				buf.writeVarInt(packet.conditionOrdinal);
			},
			buf -> new AdminToggleConditionPacket(buf.readUUID(), buf.readVarInt())
		);

		@Override
		public Type<? extends CustomPacketPayload> type() {
			return ID;
		}
	}
}

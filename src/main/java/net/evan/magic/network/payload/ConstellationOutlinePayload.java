package net.evan.magic.network.payload;

import java.util.List;
import java.util.UUID;
import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public record ConstellationOutlinePayload(List<UUID> entityUuids) implements CustomPayload {
	public static final CustomPayload.Id<ConstellationOutlinePayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "constellation_outline")
	);
	public static final PacketCodec<RegistryByteBuf, ConstellationOutlinePayload> CODEC = PacketCodec.tuple(
		Uuids.PACKET_CODEC.collect(PacketCodecs.toList()),
		ConstellationOutlinePayload::entityUuids,
		ConstellationOutlinePayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}


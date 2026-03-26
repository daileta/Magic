package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ConstellationWarningOverlayPayload(
	String message,
	int colorRgb,
	float scale,
	int fadeInTicks,
	int stayTicks,
	int fadeOutTicks
) implements CustomPayload {
	public static final CustomPayload.Id<ConstellationWarningOverlayPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "constellation_warning_overlay")
	);
	public static final PacketCodec<RegistryByteBuf, ConstellationWarningOverlayPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING,
		ConstellationWarningOverlayPayload::message,
		PacketCodecs.INTEGER,
		ConstellationWarningOverlayPayload::colorRgb,
		PacketCodecs.FLOAT,
		ConstellationWarningOverlayPayload::scale,
		PacketCodecs.INTEGER,
		ConstellationWarningOverlayPayload::fadeInTicks,
		PacketCodecs.INTEGER,
		ConstellationWarningOverlayPayload::stayTicks,
		PacketCodecs.INTEGER,
		ConstellationWarningOverlayPayload::fadeOutTicks,
		ConstellationWarningOverlayPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

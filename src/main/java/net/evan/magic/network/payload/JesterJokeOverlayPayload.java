package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record JesterJokeOverlayPayload(
	String message,
	int colorRgb,
	int fadeInTicks,
	int stayTicks,
	int fadeOutTicks
) implements CustomPayload {
	public static final CustomPayload.Id<JesterJokeOverlayPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "jester_joke_overlay")
	);
	public static final PacketCodec<RegistryByteBuf, JesterJokeOverlayPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING,
		JesterJokeOverlayPayload::message,
		PacketCodecs.INTEGER,
		JesterJokeOverlayPayload::colorRgb,
		PacketCodecs.INTEGER,
		JesterJokeOverlayPayload::fadeInTicks,
		PacketCodecs.INTEGER,
		JesterJokeOverlayPayload::stayTicks,
		PacketCodecs.INTEGER,
		JesterJokeOverlayPayload::fadeOutTicks,
		JesterJokeOverlayPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

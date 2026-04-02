package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record GreedDomainWarningOverlayPayload(
	String message,
	int colorRgb,
	int outlineColorRgb,
	float scale,
	int durationTicks,
	int lineSpacing
) implements CustomPayload {
	public static final CustomPayload.Id<GreedDomainWarningOverlayPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "greed_domain_warning_overlay")
	);
	public static final PacketCodec<RegistryByteBuf, GreedDomainWarningOverlayPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING,
		GreedDomainWarningOverlayPayload::message,
		PacketCodecs.INTEGER,
		GreedDomainWarningOverlayPayload::colorRgb,
		PacketCodecs.INTEGER,
		GreedDomainWarningOverlayPayload::outlineColorRgb,
		PacketCodecs.FLOAT,
		GreedDomainWarningOverlayPayload::scale,
		PacketCodecs.INTEGER,
		GreedDomainWarningOverlayPayload::durationTicks,
		PacketCodecs.INTEGER,
		GreedDomainWarningOverlayPayload::lineSpacing,
		GreedDomainWarningOverlayPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CelestialGamaRayTraceOverlayPayload(
	boolean active,
	String constellationId,
	float scale,
	int xOffset,
	int yOffset,
	float inputScale,
	int lineThickness,
	int nodeRadius,
	int cursorRadius,
	boolean lockCameraWhileTracing,
	int pathColorRgb,
	int progressColorRgb,
	int activeSegmentColorRgb,
	int startNodeColorRgb,
	int endNodeColorRgb,
	int successColorRgb,
	int failColorRgb,
	float toleranceRadius,
	float segmentCompletionRadius,
	String promptText,
	String chargingText
) implements CustomPayload {
	public static final CustomPayload.Id<CelestialGamaRayTraceOverlayPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "celestial_gama_ray_trace_overlay")
	);

	public static final PacketCodec<RegistryByteBuf, CelestialGamaRayTraceOverlayPayload> CODEC = PacketCodec.ofStatic(
		(buf, value) -> {
			buf.writeBoolean(value.active());
			PacketCodecs.STRING.encode(buf, value.constellationId());
			buf.writeFloat(value.scale());
			buf.writeInt(value.xOffset());
			buf.writeInt(value.yOffset());
			buf.writeFloat(value.inputScale());
			buf.writeInt(value.lineThickness());
			buf.writeInt(value.nodeRadius());
			buf.writeInt(value.cursorRadius());
			buf.writeBoolean(value.lockCameraWhileTracing());
			buf.writeInt(value.pathColorRgb());
			buf.writeInt(value.progressColorRgb());
			buf.writeInt(value.activeSegmentColorRgb());
			buf.writeInt(value.startNodeColorRgb());
			buf.writeInt(value.endNodeColorRgb());
			buf.writeInt(value.successColorRgb());
			buf.writeInt(value.failColorRgb());
			buf.writeFloat(value.toleranceRadius());
			buf.writeFloat(value.segmentCompletionRadius());
			PacketCodecs.STRING.encode(buf, value.promptText());
			PacketCodecs.STRING.encode(buf, value.chargingText());
		},
		buf -> new CelestialGamaRayTraceOverlayPayload(
			buf.readBoolean(),
			PacketCodecs.STRING.decode(buf),
			buf.readFloat(),
			buf.readInt(),
			buf.readInt(),
			buf.readFloat(),
			buf.readInt(),
			buf.readInt(),
			buf.readInt(),
			buf.readBoolean(),
			buf.readInt(),
			buf.readInt(),
			buf.readInt(),
			buf.readInt(),
			buf.readInt(),
			buf.readInt(),
			buf.readInt(),
			buf.readFloat(),
			buf.readFloat(),
			PacketCodecs.STRING.decode(buf),
			PacketCodecs.STRING.decode(buf)
		)
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}


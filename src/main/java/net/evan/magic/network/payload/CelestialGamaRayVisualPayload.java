package net.evan.magic.network.payload;

import java.util.UUID;
import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.Uuids;

public record CelestialGamaRayVisualPayload(
	UUID casterUuid,
	boolean active,
	String phase,
	int phaseStartTick,
	int phaseEndTick,
	double originX,
	double originY,
	double originZ,
	double directionX,
	double directionY,
	double directionZ,
	float lockedYaw,
	float lockedPitch
) implements CustomPayload {
	public static final CustomPayload.Id<CelestialGamaRayVisualPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "celestial_gama_ray_visual")
	);

	public static final PacketCodec<RegistryByteBuf, CelestialGamaRayVisualPayload> CODEC = PacketCodec.ofStatic(
		(buf, value) -> {
			Uuids.PACKET_CODEC.encode(buf, value.casterUuid());
			buf.writeBoolean(value.active());
			PacketCodecs.STRING.encode(buf, value.phase());
			buf.writeInt(value.phaseStartTick());
			buf.writeInt(value.phaseEndTick());
			buf.writeDouble(value.originX());
			buf.writeDouble(value.originY());
			buf.writeDouble(value.originZ());
			buf.writeDouble(value.directionX());
			buf.writeDouble(value.directionY());
			buf.writeDouble(value.directionZ());
			buf.writeFloat(value.lockedYaw());
			buf.writeFloat(value.lockedPitch());
		},
		buf -> new CelestialGamaRayVisualPayload(
			Uuids.PACKET_CODEC.decode(buf),
			buf.readBoolean(),
			PacketCodecs.STRING.decode(buf),
			buf.readInt(),
			buf.readInt(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readFloat(),
			buf.readFloat()
		)
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CelestialGamaRayTraceProgressPayload(String action) implements CustomPayload {
	public static final CustomPayload.Id<CelestialGamaRayTraceProgressPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "celestial_gama_ray_trace_progress")
	);

	public static final PacketCodec<RegistryByteBuf, CelestialGamaRayTraceProgressPayload> CODEC = PacketCodec.tuple(
		PacketCodecs.STRING,
		CelestialGamaRayTraceProgressPayload::action,
		CelestialGamaRayTraceProgressPayload::new
	);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}


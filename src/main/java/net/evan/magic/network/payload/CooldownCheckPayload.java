package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CooldownCheckPayload(int ignored) implements CustomPayload {
	public static final CustomPayload.Id<CooldownCheckPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "cooldown_check")
	);
	public static final PacketCodec<RegistryByteBuf, CooldownCheckPayload> CODEC =
		PacketCodec.tuple(PacketCodecs.INTEGER, CooldownCheckPayload::ignored, CooldownCheckPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}


package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record DomainClashInputPayload(int keyCode) implements CustomPayload {
	public static final CustomPayload.Id<DomainClashInputPayload> ID = new CustomPayload.Id<>(
		Identifier.of(Magic.MOD_ID, "domain_clash_input")
	);
	public static final PacketCodec<RegistryByteBuf, DomainClashInputPayload> CODEC =
		PacketCodec.tuple(PacketCodecs.INTEGER, DomainClashInputPayload::keyCode, DomainClashInputPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

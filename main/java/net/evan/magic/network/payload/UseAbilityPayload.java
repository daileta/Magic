package net.evan.magic.network.payload;

import net.evan.magic.Magic;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record UseAbilityPayload(int abilitySlot) implements CustomPayload {
	public static final CustomPayload.Id<UseAbilityPayload> ID = new CustomPayload.Id<>(Identifier.of(Magic.MOD_ID, "use_ability"));
	public static final PacketCodec<RegistryByteBuf, UseAbilityPayload> CODEC =
		PacketCodec.tuple(PacketCodecs.INTEGER, UseAbilityPayload::abilitySlot, UseAbilityPayload::new);

	@Override
	public Id<? extends CustomPayload> getId() {
		return ID;
	}
}

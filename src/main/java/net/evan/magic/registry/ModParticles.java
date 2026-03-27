package net.evan.magic.registry;

import net.evan.magic.Magic;
import net.evan.magic.particle.TollkeepersClaimVortexParticleEffect;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
	public static final ParticleType<TollkeepersClaimVortexParticleEffect> TOLLKEEPERS_CLAIM_VORTEX = Registry.register(
		Registries.PARTICLE_TYPE,
		Identifier.of(Magic.MOD_ID, "tollkeepers_claim_vortex"),
		FabricParticleTypes.complex(false, TollkeepersClaimVortexParticleEffect.CODEC, TollkeepersClaimVortexParticleEffect.PACKET_CODEC)
	);

	private ModParticles() {
	}

	public static void register() {
	}
}

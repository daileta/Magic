package net.evan.magic.registry;

import net.evan.magic.Magic;
import net.evan.magic.particle.AstralCataclysmBeamParticleEffect;
import net.evan.magic.particle.AstralCataclysmDownflowParticleEffect;
import net.evan.magic.particle.AstralCataclysmSpiralParticleEffect;
import net.evan.magic.particle.TollkeepersClaimVortexParticleEffect;
import net.fabricmc.fabric.api.particle.v1.FabricParticleTypes;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class ModParticles {
	public static final ParticleType<AstralCataclysmBeamParticleEffect> ASTRAL_CATACLYSM_BEAM = Registry.register(
		Registries.PARTICLE_TYPE,
		Identifier.of(Magic.MOD_ID, "astral_cataclysm_beam"),
		FabricParticleTypes.complex(false, AstralCataclysmBeamParticleEffect.CODEC, AstralCataclysmBeamParticleEffect.PACKET_CODEC)
	);
	public static final ParticleType<AstralCataclysmDownflowParticleEffect> ASTRAL_CATACLYSM_DOWNFLOW = Registry.register(
		Registries.PARTICLE_TYPE,
		Identifier.of(Magic.MOD_ID, "astral_cataclysm_downflow"),
		FabricParticleTypes.complex(false, AstralCataclysmDownflowParticleEffect.CODEC, AstralCataclysmDownflowParticleEffect.PACKET_CODEC)
	);
	public static final ParticleType<AstralCataclysmSpiralParticleEffect> ASTRAL_CATACLYSM_SPIRAL = Registry.register(
		Registries.PARTICLE_TYPE,
		Identifier.of(Magic.MOD_ID, "astral_cataclysm_spiral"),
		FabricParticleTypes.complex(false, AstralCataclysmSpiralParticleEffect.CODEC, AstralCataclysmSpiralParticleEffect.PACKET_CODEC)
	);
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


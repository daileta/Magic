package net.evan.magic.client.particle;

import net.evan.magic.registry.ModParticles;
import net.fabricmc.fabric.api.client.particle.v1.ParticleFactoryRegistry;

public final class MagicClientParticles {
	private static boolean initialized;

	private MagicClientParticles() {
	}

	public static void initialize() {
		if (initialized) {
			return;
		}

		ParticleFactoryRegistry.getInstance().register(ModParticles.ASTRAL_CATACLYSM_BEAM, AstralCataclysmBeamParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.ASTRAL_CATACLYSM_DOWNFLOW, AstralCataclysmDownflowParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.ASTRAL_CATACLYSM_SPIRAL, AstralCataclysmSpiralParticle.Factory::new);
		ParticleFactoryRegistry.getInstance().register(ModParticles.TOLLKEEPERS_CLAIM_VORTEX, TollkeepersClaimVortexParticle.Factory::new);
		initialized = true;
	}
}


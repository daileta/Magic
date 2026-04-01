package net.evan.magic.client.particle;

import net.evan.magic.particle.AstralCataclysmDownflowParticleEffect;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class AstralCataclysmDownflowParticle extends BillboardParticle {
	private final float baseScale;
	private final float baseAlpha;

	private AstralCataclysmDownflowParticle(
		ClientWorld world,
		double x,
		double y,
		double z,
		AstralCataclysmDownflowParticleEffect effect,
		SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.baseScale = effect.particleScale() * (0.92F + world.random.nextFloat() * 0.18F);
		this.baseAlpha = MathHelper.clamp(effect.alpha(), 0.0F, 1.0F);
		this.maxAge = Math.max(1, effect.maxAge() + world.random.nextInt(3));
		this.scale = this.baseScale;
		this.collidesWithWorld = false;
		this.gravityStrength = 0.0F;
		this.velocityMultiplier = 0.92F;
		double descentPerTick = Math.max(0.0, effect.descentPerTick());
		this.velocityX = world.random.nextGaussian() * 0.012;
		this.velocityY = -descentPerTick * (0.88 + world.random.nextDouble() * 0.24);
		this.velocityZ = world.random.nextGaussian() * 0.012;
		setColorFromRgb(effect.colorRgb());
		this.setAlpha(this.baseAlpha);
	}

	@Override
	public RenderType getRenderType() {
		return RenderType.PARTICLE_ATLAS_TRANSLUCENT;
	}

	@Override
	protected int getBrightness(float tint) {
		return 240;
	}

	@Override
	public void tick() {
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
		this.age++;
		if (this.age >= this.maxAge) {
			this.markDead();
			return;
		}

		this.velocityX *= this.velocityMultiplier;
		this.velocityZ *= this.velocityMultiplier;
		this.velocityY = Math.min(this.velocityY - 0.004, -0.08);
		this.move(this.velocityX, this.velocityY, this.velocityZ);
		float fadeProgress = this.age / (float) this.maxAge;
		this.scale = this.baseScale * (1.0F - fadeProgress * 0.18F);
		float alpha = fadeProgress < 0.18F
			? this.baseAlpha * MathHelper.clamp(fadeProgress / 0.18F, 0.0F, 1.0F)
			: this.baseAlpha * (1.0F - Math.max(0.0F, (fadeProgress - 0.18F) / 0.82F) * 0.78F);
		this.setAlpha(MathHelper.clamp(alpha, 0.0F, this.baseAlpha));
	}

	private void setColorFromRgb(int colorRgb) {
		float red = ((colorRgb >> 16) & 0xFF) / 255.0F;
		float green = ((colorRgb >> 8) & 0xFF) / 255.0F;
		float blue = (colorRgb & 0xFF) / 255.0F;
		this.setColor(red, green, blue);
	}

	public static final class Factory implements ParticleFactory<AstralCataclysmDownflowParticleEffect> {
		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
			AstralCataclysmDownflowParticleEffect effect,
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Random random
		) {
			return new AstralCataclysmDownflowParticle(world, x, y, z, effect, this.spriteProvider);
		}
	}
}

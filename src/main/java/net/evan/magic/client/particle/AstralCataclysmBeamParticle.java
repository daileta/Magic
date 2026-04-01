package net.evan.magic.client.particle;

import net.evan.magic.particle.AstralCataclysmBeamParticleEffect;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class AstralCataclysmBeamParticle extends BillboardParticle {
	private final float baseScale;
	private final float baseAlpha;
	private final float pulseOffset;

	private AstralCataclysmBeamParticle(
		ClientWorld world,
		double x,
		double y,
		double z,
		AstralCataclysmBeamParticleEffect effect,
		SpriteProvider spriteProvider
	) {
		super(world, x, y, z, spriteProvider.getFirst());
		this.baseScale = effect.particleScale() * (0.94F + world.random.nextFloat() * 0.16F);
		this.baseAlpha = MathHelper.clamp(effect.alpha(), 0.0F, 1.0F);
		this.pulseOffset = world.random.nextFloat() * 6.0F;
		this.maxAge = Math.max(1, effect.maxAge() + world.random.nextInt(2));
		this.scale = this.baseScale;
		this.collidesWithWorld = false;
		this.gravityStrength = 0.0F;
		this.velocityMultiplier = 0.82F;
		this.velocityX = world.random.nextGaussian() * 0.0035;
		this.velocityY = world.random.nextDouble() * 0.008;
		this.velocityZ = world.random.nextGaussian() * 0.0035;
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
		this.velocityY *= 0.88;
		this.velocityZ *= this.velocityMultiplier;
		this.move(this.velocityX, this.velocityY, this.velocityZ);
		float fadeProgress = this.age / (float) this.maxAge;
		float pulse = 0.88F + (float) Math.sin((this.age + this.pulseOffset) * 0.42F) * 0.12F;
		this.scale = this.baseScale * (0.96F + pulse * 0.08F);
		this.setAlpha(MathHelper.clamp(this.baseAlpha * pulse * (1.0F - fadeProgress * 0.52F), 0.0F, this.baseAlpha));
	}

	private void setColorFromRgb(int colorRgb) {
		float red = ((colorRgb >> 16) & 0xFF) / 255.0F;
		float green = ((colorRgb >> 8) & 0xFF) / 255.0F;
		float blue = (colorRgb & 0xFF) / 255.0F;
		this.setColor(red, green, blue);
	}

	public static final class Factory implements ParticleFactory<AstralCataclysmBeamParticleEffect> {
		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
			AstralCataclysmBeamParticleEffect effect,
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Random random
		) {
			return new AstralCataclysmBeamParticle(world, x, y, z, effect, this.spriteProvider);
		}
	}
}

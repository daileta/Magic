package net.evan.magic.client.particle;

import net.evan.magic.particle.AstralCataclysmSpiralParticleEffect;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class AstralCataclysmSpiralParticle extends BillboardParticle {
	private final double centerX;
	private final double centerY;
	private final double centerZ;
	private double localY;
	private double angle;
	private final double angularSpeed;
	private final double descentPerTick;
	private final double orbitRadius;
	private final double twistPerBlock;
	private final float baseScale;
	private final float baseAlpha;
	private final float pulseOffset;

	private AstralCataclysmSpiralParticle(
		ClientWorld world,
		AstralCataclysmSpiralParticleEffect effect,
		SpriteProvider spriteProvider
	) {
		super(world, effect.centerX(), effect.centerY() + effect.localY(), effect.centerZ(), spriteProvider.getFirst());
		this.centerX = effect.centerX();
		this.centerY = effect.centerY();
		this.centerZ = effect.centerZ();
		this.localY = effect.localY();
		this.angle = effect.angle();
		this.angularSpeed = effect.angularSpeed();
		this.descentPerTick = effect.descentPerTick();
		this.orbitRadius = effect.orbitRadius();
		this.twistPerBlock = effect.twistPerBlock();
		this.baseScale = effect.particleScale() * (0.9F + world.random.nextFloat() * 0.2F);
		this.baseAlpha = MathHelper.clamp(effect.alpha(), 0.0F, 1.0F);
		this.pulseOffset = world.random.nextFloat() * 5.0F;
		this.maxAge = Math.max(1, effect.maxAge() + world.random.nextInt(2));
		this.scale = this.baseScale;
		this.collidesWithWorld = false;
		this.gravityStrength = 0.0F;
		this.velocityMultiplier = 1.0F;
		setColorFromRgb(effect.colorRgb());
		this.setAlpha(this.baseAlpha);
		updatePosition();
		this.lastX = this.x;
		this.lastY = this.y;
		this.lastZ = this.z;
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
		if (this.age >= this.maxAge || this.localY < 0.0) {
			this.markDead();
			return;
		}

		this.localY -= this.descentPerTick;
		this.angle += this.angularSpeed;
		updatePosition();
		float fadeProgress = this.age / (float) this.maxAge;
		float pulse = 0.9F + (float) Math.sin((this.age + this.pulseOffset) * 0.36F) * 0.1F;
		this.scale = this.baseScale * (0.94F + pulse * 0.12F);
		this.setAlpha(MathHelper.clamp(this.baseAlpha * pulse * (1.0F - fadeProgress * 0.42F), 0.0F, this.baseAlpha));
	}

	private void updatePosition() {
		double theta = this.angle + this.localY * this.twistPerBlock;
		double animatedRadius = this.orbitRadius * (0.98 + Math.sin((this.age + this.pulseOffset) * 0.18) * 0.03);
		double nextX = this.centerX + animatedRadius * Math.cos(theta);
		double nextY = this.centerY + this.localY;
		double nextZ = this.centerZ + animatedRadius * Math.sin(theta);
		this.setPos(nextX, nextY, nextZ);
	}

	private void setColorFromRgb(int colorRgb) {
		float red = ((colorRgb >> 16) & 0xFF) / 255.0F;
		float green = ((colorRgb >> 8) & 0xFF) / 255.0F;
		float blue = (colorRgb & 0xFF) / 255.0F;
		this.setColor(red, green, blue);
	}

	public static final class Factory implements ParticleFactory<AstralCataclysmSpiralParticleEffect> {
		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
			AstralCataclysmSpiralParticleEffect effect,
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Random random
		) {
			return new AstralCataclysmSpiralParticle(world, effect, this.spriteProvider);
		}
	}
}


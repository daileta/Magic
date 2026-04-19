package net.evan.magic.client.particle;

import net.evan.magic.particle.TollkeepersClaimVortexParticleEffect;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleFactory;
import net.minecraft.client.particle.SpriteProvider;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

public final class TollkeepersClaimVortexParticle extends BillboardParticle {
	private final double centerX;
	private final double centerY;
	private final double centerZ;
	private double localY;
	private double angle;
	private final double spinSpeed;
	private final double twistPerBlock;
	private final double baseRadius;
	private final double straightHeight;
	private final double totalHeight;
	private final double outwardCurve;
	private final double risePerTick;

	private TollkeepersClaimVortexParticle(ClientWorld world, TollkeepersClaimVortexParticleEffect effect, SpriteProvider spriteProvider) {
		super(world, effect.centerX(), effect.centerY(), effect.centerZ(), spriteProvider.getFirst());
		this.centerX = effect.centerX();
		this.centerY = effect.centerY();
		this.centerZ = effect.centerZ();
		this.localY = effect.localY();
		this.angle = effect.angle();
		this.spinSpeed = effect.spinSpeed() * 0.5;
		this.twistPerBlock = effect.twistPerBlock();
		this.baseRadius = effect.baseRadius();
		this.straightHeight = effect.straightHeight();
		this.totalHeight = effect.totalHeight();
		this.outwardCurve = effect.outwardCurve();
		this.maxAge = Math.max(1, effect.maxAge());
		this.risePerTick = this.totalHeight / this.maxAge;
		this.scale = effect.particleScale();
		this.collidesWithWorld = false;
		this.gravityStrength = 0.0F;
		this.velocityMultiplier = 1.0F;
		this.setColor(0.58F, 0.4F, 0.1F);
		this.setAlpha(0.98F);
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
		this.localY += this.risePerTick;
		this.angle += this.spinSpeed;
		updatePosition();
		float lifeProgress = this.age / (float) this.maxAge;
		this.setAlpha(MathHelper.clamp(0.98F - lifeProgress * 0.35F, 0.0F, 0.98F));
		if (this.age >= this.maxAge || this.localY >= this.totalHeight) {
			this.markDead();
		}
	}

	private void updatePosition() {
		double clampedY = Math.min(this.localY, this.totalHeight);
		double radius = clampedY <= this.straightHeight
			? this.baseRadius
			: this.baseRadius + this.outwardCurve * MathHelper.square(clampedY - this.straightHeight);
		double theta = this.angle + clampedY * this.twistPerBlock;
		double nextX = this.centerX + radius * Math.cos(theta);
		double nextY = this.centerY + clampedY;
		double nextZ = this.centerZ + radius * Math.sin(theta);
		this.setPos(nextX, nextY, nextZ);
	}

	public static final class Factory implements ParticleFactory<TollkeepersClaimVortexParticleEffect> {
		private final SpriteProvider spriteProvider;

		public Factory(SpriteProvider spriteProvider) {
			this.spriteProvider = spriteProvider;
		}

		@Override
		public Particle createParticle(
			TollkeepersClaimVortexParticleEffect effect,
			ClientWorld world,
			double x,
			double y,
			double z,
			double velocityX,
			double velocityY,
			double velocityZ,
			Random random
		) {
			return new TollkeepersClaimVortexParticle(world, effect, this.spriteProvider);
		}
	}
}


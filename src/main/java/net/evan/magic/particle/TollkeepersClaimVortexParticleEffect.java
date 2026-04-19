package net.evan.magic.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.evan.magic.registry.ModParticles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public record TollkeepersClaimVortexParticleEffect(
	double centerX,
	double centerY,
	double centerZ,
	double localY,
	double angle,
	double spinSpeed,
	double twistPerBlock,
	double baseRadius,
	double straightHeight,
	double totalHeight,
	double outwardCurve,
	int maxAge,
	float particleScale
) implements ParticleEffect {
	public static final MapCodec<TollkeepersClaimVortexParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.DOUBLE.fieldOf("center_x").forGetter(TollkeepersClaimVortexParticleEffect::centerX),
			Codec.DOUBLE.fieldOf("center_y").forGetter(TollkeepersClaimVortexParticleEffect::centerY),
			Codec.DOUBLE.fieldOf("center_z").forGetter(TollkeepersClaimVortexParticleEffect::centerZ),
			Codec.DOUBLE.fieldOf("local_y").forGetter(TollkeepersClaimVortexParticleEffect::localY),
			Codec.DOUBLE.fieldOf("angle").forGetter(TollkeepersClaimVortexParticleEffect::angle),
			Codec.DOUBLE.fieldOf("spin_speed").forGetter(TollkeepersClaimVortexParticleEffect::spinSpeed),
			Codec.DOUBLE.fieldOf("twist_per_block").forGetter(TollkeepersClaimVortexParticleEffect::twistPerBlock),
			Codec.DOUBLE.fieldOf("base_radius").forGetter(TollkeepersClaimVortexParticleEffect::baseRadius),
			Codec.DOUBLE.fieldOf("straight_height").forGetter(TollkeepersClaimVortexParticleEffect::straightHeight),
			Codec.DOUBLE.fieldOf("total_height").forGetter(TollkeepersClaimVortexParticleEffect::totalHeight),
			Codec.DOUBLE.fieldOf("outward_curve").forGetter(TollkeepersClaimVortexParticleEffect::outwardCurve),
			Codec.INT.fieldOf("max_age").forGetter(TollkeepersClaimVortexParticleEffect::maxAge),
			Codec.FLOAT.fieldOf("particle_scale").forGetter(TollkeepersClaimVortexParticleEffect::particleScale)
		).apply(instance, TollkeepersClaimVortexParticleEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, TollkeepersClaimVortexParticleEffect> PACKET_CODEC = PacketCodec.ofStatic(
		(buf, value) -> {
			buf.writeDouble(value.centerX());
			buf.writeDouble(value.centerY());
			buf.writeDouble(value.centerZ());
			buf.writeDouble(value.localY());
			buf.writeDouble(value.angle());
			buf.writeDouble(value.spinSpeed());
			buf.writeDouble(value.twistPerBlock());
			buf.writeDouble(value.baseRadius());
			buf.writeDouble(value.straightHeight());
			buf.writeDouble(value.totalHeight());
			buf.writeDouble(value.outwardCurve());
			buf.writeInt(value.maxAge());
			buf.writeFloat(value.particleScale());
		},
		buf -> new TollkeepersClaimVortexParticleEffect(
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readDouble(),
			buf.readInt(),
			buf.readFloat()
		)
	);

	@Override
	public ParticleType<?> getType() {
		return ModParticles.TOLLKEEPERS_CLAIM_VORTEX;
	}
}


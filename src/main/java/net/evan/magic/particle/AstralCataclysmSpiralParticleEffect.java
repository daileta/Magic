package net.evan.magic.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.evan.magic.registry.ModParticles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public record AstralCataclysmSpiralParticleEffect(
	double centerX,
	double centerY,
	double centerZ,
	double localY,
	double angle,
	double angularSpeed,
	double descentPerTick,
	double orbitRadius,
	double twistPerBlock,
	int colorRgb,
	float alpha,
	float particleScale,
	int maxAge
) implements ParticleEffect {
	public static final MapCodec<AstralCataclysmSpiralParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.DOUBLE.fieldOf("center_x").forGetter(AstralCataclysmSpiralParticleEffect::centerX),
			Codec.DOUBLE.fieldOf("center_y").forGetter(AstralCataclysmSpiralParticleEffect::centerY),
			Codec.DOUBLE.fieldOf("center_z").forGetter(AstralCataclysmSpiralParticleEffect::centerZ),
			Codec.DOUBLE.fieldOf("local_y").forGetter(AstralCataclysmSpiralParticleEffect::localY),
			Codec.DOUBLE.fieldOf("angle").forGetter(AstralCataclysmSpiralParticleEffect::angle),
			Codec.DOUBLE.fieldOf("angular_speed").forGetter(AstralCataclysmSpiralParticleEffect::angularSpeed),
			Codec.DOUBLE.fieldOf("descent_per_tick").forGetter(AstralCataclysmSpiralParticleEffect::descentPerTick),
			Codec.DOUBLE.fieldOf("orbit_radius").forGetter(AstralCataclysmSpiralParticleEffect::orbitRadius),
			Codec.DOUBLE.fieldOf("twist_per_block").forGetter(AstralCataclysmSpiralParticleEffect::twistPerBlock),
			Codec.INT.fieldOf("color_rgb").forGetter(AstralCataclysmSpiralParticleEffect::colorRgb),
			Codec.FLOAT.fieldOf("alpha").forGetter(AstralCataclysmSpiralParticleEffect::alpha),
			Codec.FLOAT.fieldOf("particle_scale").forGetter(AstralCataclysmSpiralParticleEffect::particleScale),
			Codec.INT.fieldOf("max_age").forGetter(AstralCataclysmSpiralParticleEffect::maxAge)
		).apply(instance, AstralCataclysmSpiralParticleEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, AstralCataclysmSpiralParticleEffect> PACKET_CODEC = PacketCodec.ofStatic(
		(buf, value) -> {
			buf.writeDouble(value.centerX());
			buf.writeDouble(value.centerY());
			buf.writeDouble(value.centerZ());
			buf.writeDouble(value.localY());
			buf.writeDouble(value.angle());
			buf.writeDouble(value.angularSpeed());
			buf.writeDouble(value.descentPerTick());
			buf.writeDouble(value.orbitRadius());
			buf.writeDouble(value.twistPerBlock());
			buf.writeInt(value.colorRgb());
			buf.writeFloat(value.alpha());
			buf.writeFloat(value.particleScale());
			buf.writeInt(value.maxAge());
		},
		buf -> new AstralCataclysmSpiralParticleEffect(
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
			buf.readFloat(),
			buf.readFloat(),
			buf.readInt()
		)
	);

	@Override
	public ParticleType<?> getType() {
		return ModParticles.ASTRAL_CATACLYSM_SPIRAL;
	}
}


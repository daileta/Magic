package net.evan.magic.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.evan.magic.registry.ModParticles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public record AstralCataclysmBeamParticleEffect(
	int colorRgb,
	float alpha,
	float particleScale,
	int maxAge
) implements ParticleEffect {
	public static final MapCodec<AstralCataclysmBeamParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.INT.fieldOf("color_rgb").forGetter(AstralCataclysmBeamParticleEffect::colorRgb),
			Codec.FLOAT.fieldOf("alpha").forGetter(AstralCataclysmBeamParticleEffect::alpha),
			Codec.FLOAT.fieldOf("particle_scale").forGetter(AstralCataclysmBeamParticleEffect::particleScale),
			Codec.INT.fieldOf("max_age").forGetter(AstralCataclysmBeamParticleEffect::maxAge)
		).apply(instance, AstralCataclysmBeamParticleEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, AstralCataclysmBeamParticleEffect> PACKET_CODEC = PacketCodec.ofStatic(
		(buf, value) -> {
			buf.writeInt(value.colorRgb());
			buf.writeFloat(value.alpha());
			buf.writeFloat(value.particleScale());
			buf.writeInt(value.maxAge());
		},
		buf -> new AstralCataclysmBeamParticleEffect(
			buf.readInt(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readInt()
		)
	);

	@Override
	public ParticleType<?> getType() {
		return ModParticles.ASTRAL_CATACLYSM_BEAM;
	}
}


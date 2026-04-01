package net.evan.magic.particle;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.evan.magic.registry.ModParticles;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleType;

public record AstralCataclysmDownflowParticleEffect(
	int colorRgb,
	float alpha,
	float particleScale,
	int maxAge,
	double descentPerTick
) implements ParticleEffect {
	public static final MapCodec<AstralCataclysmDownflowParticleEffect> CODEC = RecordCodecBuilder.mapCodec(instance ->
		instance.group(
			Codec.INT.fieldOf("color_rgb").forGetter(AstralCataclysmDownflowParticleEffect::colorRgb),
			Codec.FLOAT.fieldOf("alpha").forGetter(AstralCataclysmDownflowParticleEffect::alpha),
			Codec.FLOAT.fieldOf("particle_scale").forGetter(AstralCataclysmDownflowParticleEffect::particleScale),
			Codec.INT.fieldOf("max_age").forGetter(AstralCataclysmDownflowParticleEffect::maxAge),
			Codec.DOUBLE.fieldOf("descent_per_tick").forGetter(AstralCataclysmDownflowParticleEffect::descentPerTick)
		).apply(instance, AstralCataclysmDownflowParticleEffect::new)
	);
	public static final PacketCodec<RegistryByteBuf, AstralCataclysmDownflowParticleEffect> PACKET_CODEC = PacketCodec.ofStatic(
		(buf, value) -> {
			buf.writeInt(value.colorRgb());
			buf.writeFloat(value.alpha());
			buf.writeFloat(value.particleScale());
			buf.writeInt(value.maxAge());
			buf.writeDouble(value.descentPerTick());
		},
		buf -> new AstralCataclysmDownflowParticleEffect(
			buf.readInt(),
			buf.readFloat(),
			buf.readFloat(),
			buf.readInt(),
			buf.readDouble()
		)
	);

	@Override
	public ParticleType<?> getType() {
		return ModParticles.ASTRAL_CATACLYSM_DOWNFLOW;
	}
}

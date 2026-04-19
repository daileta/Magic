package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.core.MagicPlayerData;
import net.evan.magic.magic.core.MagicSchool;
import net.evan.magic.mixin.ArmorStandEntityAccessorMixin;
import net.evan.magic.mixin.BlockDisplayEntityAccessorMixin;
import net.evan.magic.mixin.DisplayEntityAccessorMixin;
import net.evan.magic.network.payload.CelestialGamaRayTraceOverlayPayload;
import net.evan.magic.network.payload.ConstellationOutlinePayload;
import net.evan.magic.network.payload.ConstellationWarningOverlayPayload;
import net.evan.magic.network.payload.CelestialGamaRayVisualPayload;
import net.evan.magic.network.payload.JesterJokeOverlayPayload;
import net.evan.magic.particle.AstralCataclysmBeamParticleEffect;
import net.evan.magic.particle.AstralCataclysmDownflowParticleEffect;
import net.evan.magic.particle.AstralCataclysmSpiralParticleEffect;
import net.evan.magic.registry.ModItems;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.SharedConstants;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LightBlock;
import net.minecraft.block.VineBlock;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityPosition;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.BlockAttachedEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.mob.SlimeEntity;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.passive.PandaEntity;
import net.minecraft.entity.passive.ParrotEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntitiesDestroyS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityPositionSyncS2CPacket;
import net.minecraft.network.packet.s2c.play.EntitySpawnS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleFadeS2CPacket;
import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.network.packet.s2c.play.PositionFlag;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtHelper;
import net.minecraft.nbt.NbtList;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryEntryLookup;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.AffineTransformation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateType;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import org.joml.Quaternionf;
import org.joml.Vector3f;


abstract class JesterVisualRuntime extends JesterOutcomeRuntime {
	static void spawnComedicAssistantSlimeVisual(LivingEntity target, ServerWorld world) {
		if (COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualDurationTicks() <= 0 || COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSpawnHeight() <= 0.0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		SlimeEntity slime = new SlimeEntity(EntityType.SLIME, world);
		double startY = target.getY() + target.getHeight() + COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSpawnHeight();
		slime.refreshPositionAndAngles(target.getX(), startY, target.getZ(), target.getYaw(), 0.0F);
		slime.setBodyYaw(target.getYaw());
		slime.setHeadYaw(target.getYaw());
		slime.setAiDisabled(true);
		slime.setSilent(true);
		slime.setNoGravity(true);
		slime.setSize(COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualSize(), false);
		slime.setVelocity(Vec3d.ZERO);
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualDurationTicks(),
			viewers,
			0,
			new ComedicRewriteVisualFollowState(
				world.getRegistryKey(),
				target.getUuid(),
				new Vec3d[] { Vec3d.ZERO },
				ComedicRewriteVisualFollowMode.DROP_TO_ENTITY_TOP,
				COMEDIC_ASSISTANT_GIANT_SLIME_SLAM.visualFallSpeed()
			),
			slime
		);
	}

	static void spawnComedicAssistantPandaVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		if (COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks() <= 0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d travelDirection = normalizedHorizontalDirection(direction, null);
		Vec3d spawnPos = basePos.add(0.0, 0.1, 0.0).subtract(travelDirection.multiply(COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualSpawnDistance()));
		float yaw = horizontalYawFromDirection(travelDirection);
		PandaEntity panda = new PandaEntity(EntityType.PANDA, world);
		panda.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, yaw, 0.0F);
		panda.setBodyYaw(yaw);
		panda.setHeadYaw(yaw);
		panda.setAiDisabled(true);
		panda.setSilent(true);
		panda.setNoGravity(true);
		panda.setVelocity(travelDirection.multiply(COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualChargeVelocity()));
		int chargeTicks = COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualChargeVelocity() <= 0.0
			? Math.max(1, COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks() / 2)
			: Math.max(1, Math.min(COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks(), (int) Math.ceil((COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualSpawnDistance() * 2.0 + 1.0) / COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualChargeVelocity())));
		panda.setMainGene(PandaEntity.Gene.PLAYFUL);
		panda.setHiddenGene(PandaEntity.Gene.PLAYFUL);
		panda.setPlaying(true);
		panda.playingTicks = chargeTicks;
		queueComedicRewriteVisualCameo(world.getServer(), COMEDIC_ASSISTANT_PANDA_BOWLING_BALL.visualDurationTicks(), viewers, chargeTicks, null, panda);
	}

	static void spawnComedicAssistantParrotVisual(LivingEntity target, ServerWorld world) {
		if (COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualDurationTicks() <= 0 || COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount() <= 0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		List<Entity> parrots = new ArrayList<>();
		Vec3d[] offsets = new Vec3d[COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount()];
		for (int index = 0; index < COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount(); index++) {
			double angle = (Math.PI * 2.0 * index) / COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualCount();
			double offsetX = Math.cos(angle) * COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualRadius();
			double offsetZ = Math.sin(angle) * COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualRadius();
			Vec3d offset = new Vec3d(offsetX, COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualVerticalOffset() + (index % 2 == 0 ? 0.12 : 0.0), offsetZ);
			offsets[index] = offset;
			Vec3d spawnPos = basePos.add(offset);
			ParrotEntity parrot = new ParrotEntity(EntityType.PARROT, world);
			parrot.refreshPositionAndAngles(spawnPos.x, spawnPos.y, spawnPos.z, target.getYaw(), 0.0F);
			parrot.setBodyYaw(target.getYaw());
			parrot.setHeadYaw(target.getYaw());
			parrot.setAiDisabled(true);
			parrot.setSilent(true);
			parrot.setNoGravity(true);
			parrot.setVelocity(Vec3d.ZERO);
			parrot.setComponent(
				DataComponentTypes.PARROT_VARIANT,
				ParrotEntity.Variant.byIndex(target.getRandom().nextInt(ParrotEntity.Variant.values().length))
			);
			parrots.add(parrot);
		}
		queueComedicRewriteVisualCameo(
			world.getServer(),
			Math.max(COMEDIC_ASSISTANT_PARROT_KIDNAPPING.visualDurationTicks(), COMEDIC_ASSISTANT_PARROT_KIDNAPPING.maxCarryTicks()),
			viewers,
			0,
			new ComedicRewriteVisualFollowState(
				world.getRegistryKey(),
				target.getUuid(),
				offsets,
				ComedicRewriteVisualFollowMode.ENTITY_TOP,
				0.0
			),
			parrots.toArray(Entity[]::new)
		);
	}

	static void spawnComedicAssistantAcmeVisual(LivingEntity target, ServerWorld world) {
		if (COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks() <= 0 || COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight() <= 0.0) {
			return;
		}

		MinecraftServer server = world.getServer();
		if (server == null) {
			return;
		}

		DisplayEntity.BlockDisplayEntity anvil = new DisplayEntity.BlockDisplayEntity(EntityType.BLOCK_DISPLAY, world);
		double startY = target.getY() + target.getHeight() + COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight();
		anvil.refreshPositionAndAngles(target.getX(), startY, target.getZ(), 0.0F, 0.0F);
		((BlockDisplayEntityAccessorMixin) anvil).magic$setBlockState(Blocks.ANVIL.getDefaultState());
		((DisplayEntityAccessorMixin) anvil).magic$setTeleportDuration(1);
		((DisplayEntityAccessorMixin) anvil).magic$setInterpolationDuration(1);
		((DisplayEntityAccessorMixin) anvil).magic$setDisplayWidth(1.25F);
		((DisplayEntityAccessorMixin) anvil).magic$setDisplayHeight(1.25F);
		((DisplayEntityAccessorMixin) anvil).magic$setViewRange(1.0F);
		anvil.setNoGravity(true);
		anvil.setSilent(true);
		if (!world.spawnEntity(anvil)) {
			return;
		}
		COMEDIC_ASSISTANT_ACME_VISUALS.put(
			anvil.getUuid(),
			new ComedicAssistantAcmeVisualState(
				world.getRegistryKey(),
				anvil.getUuid(),
				target.getUuid(),
				server.getTicks() + COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks(),
				comedicAssistantAcmeDropSpeed()
			)
		);
	}

	static void spawnComedicAssistantCaneVisual(LivingEntity target, ServerWorld world, Vec3d direction) {
		if (COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks() <= 0) {
			return;
		}

		Vec3d basePos = new Vec3d(target.getX(), target.getY(), target.getZ());
		List<ServerPlayerEntity> viewers = comedicRewriteVisualViewers(world, basePos);
		if (viewers.isEmpty()) {
			return;
		}

		Vec3d travelDirection = normalizedHorizontalDirection(direction, null);
		Vec3d spawnPos = basePos.add(0.0, target.getHeight() * 0.45, 0.0).subtract(travelDirection.multiply(COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualSpawnDistance()));
		float yaw = horizontalYawFromDirection(travelDirection);
		ItemEntity cane = new ItemEntity(world, spawnPos.x, spawnPos.y, spawnPos.z, new ItemStack(Items.STICK));
		cane.setYaw(yaw);
		cane.setBodyYaw(yaw);
		cane.setHeadYaw(yaw);
		cane.setPitch(90.0F);
		cane.setNoGravity(true);
		cane.setSilent(true);
		cane.setVelocity(travelDirection.multiply(COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualChargeVelocity()));
		int chargeTicks = COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualChargeVelocity() <= 0.0
			? Math.max(1, COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks() / 2)
			: Math.max(1, Math.min(COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks(), (int) Math.ceil((COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualSpawnDistance() * 2.0 + 0.5) / COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualChargeVelocity())));
		queueComedicRewriteVisualCameo(
			world.getServer(),
			COMEDIC_ASSISTANT_GIANT_CANE_YANK.visualDurationTicks(),
			viewers,
			chargeTicks,
			null,
			new ComedicRewriteVisualRotationState(true, 0.0F, 30.0F),
			cane
		);
	}

	static void spawnComedicAssistantDivineLightningVisual(LivingEntity target, ServerWorld world, int strikeCount) {
		int visibleStrikeCount = Math.max(1, Math.min(4, strikeCount));
		for (int index = 0; index < visibleStrikeCount; index++) {
			double angle = (Math.PI * 2.0 * index) / visibleStrikeCount;
			double radius = target.getRandom().nextDouble() * COMEDIC_ASSISTANT_DIVINE_OVERREACTION.strikeRadius();
			double x = target.getX() + Math.cos(angle) * radius;
			double z = target.getZ() + Math.sin(angle) * radius;
			LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, world);
			lightning.setCosmetic(true);
			lightning.refreshPositionAfterTeleport(x, target.getY(), z);
			world.spawnEntity(lightning);
		}
	}

	static double comedicAssistantAcmeDropSpeed() {
		if (COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight() <= 0.0) {
			return 0.0;
		}
		double durationBasedSpeed = COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks() <= 1
			? COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight()
			: COMEDIC_ASSISTANT_ACME_DROP.visualDropHeight() / Math.max(1.0, COMEDIC_ASSISTANT_ACME_DROP.visualDurationTicks() * 0.45);
		return Math.max(1.35, durationBasedSpeed);
	}

	static void updateComedicAssistantAcmeVisuals(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, ComedicAssistantAcmeVisualState>> iterator = COMEDIC_ASSISTANT_ACME_VISUALS.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ComedicAssistantAcmeVisualState> entry = iterator.next();
			ComedicAssistantAcmeVisualState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity visualEntity = world.getEntity(state.visualEntityId());
			if (!(visualEntity instanceof DisplayEntity.BlockDisplayEntity anvil) || !anvil.isAlive()) {
				iterator.remove();
				continue;
			}

			if (currentTick >= state.endTick()) {
				anvil.discard();
				iterator.remove();
				continue;
			}

			Entity targetEntity = world.getEntity(state.targetEntityId());
			if (!(targetEntity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				anvil.discard();
				iterator.remove();
				continue;
			}

			double targetY = target.getY() + target.getHeight();
			double nextY = Math.max(targetY, anvil.getY() - state.dropSpeed());
			anvil.refreshPositionAfterTeleport(target.getX(), nextY, target.getZ());
		}
	}

	static void updateComedicAssistantCaneImpactStates(MinecraftServer server, int currentTick) {
		List<Map.Entry<UUID, ComedicAssistantCaneImpactState>> entries = new ArrayList<>(COMEDIC_ASSISTANT_CANE_IMPACT_STATES.entrySet());
		for (Map.Entry<UUID, ComedicAssistantCaneImpactState> entry : entries) {
			ComedicAssistantCaneImpactState state = entry.getValue();
			if (COMEDIC_ASSISTANT_CANE_IMPACT_STATES.get(entry.getKey()) != state) {
				continue;
			}
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick > state.endTick()) {
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick <= state.startTick()) {
				state.lastPosition(entityPosition(target));
				state.lastVelocity(target.getVelocity());
				state.lastSpeed(target.getVelocity().length());
				continue;
			}

			PlusUltraImpactHit impact = detectVelocityImpact(
				target,
				world,
				state.lastPosition(),
				state.lastVelocity(),
				state.lastSpeed(),
				COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageThreshold()
			);
			if (impact != null) {
				double impactSpeed = trackedImpactSpeed(target, state.lastPosition(), state.lastVelocity(), state.lastSpeed());
				double damageAmount = Math.min(
					COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageMax(),
					Math.max(0.0, impactSpeed - COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageThreshold()) * COMEDIC_ASSISTANT_GIANT_CANE_YANK.velocityDamageMultiplier()
				);
				if (damageAmount > 0.0) {
					dealTrackedMagicDamage(target, state.casterId(), world.getDamageSources().flyIntoWall(), (float) damageAmount);
					spawnComedicAssistantCaneImpactParticles(world, impact);
					playConfiguredSound(
						world,
						impact.position(),
						SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY,
						Math.max(0.0F, COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundVolume() * 0.9F),
						Math.max(0.1F, COMEDIC_ASSISTANT_GIANT_CANE_YANK.soundPitch() * 0.85F)
					);
				}
				COMEDIC_ASSISTANT_CANE_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick <= state.forceEndTick()) {
				applyForcedVelocity(target, state.forcedVelocity());
			}
			state.lastPosition(entityPosition(target));
			state.lastVelocity(target.getVelocity());
			state.lastSpeed(target.getVelocity().length());
		}
	}

	static void updatePlusUltraImpactStates(MinecraftServer server, int currentTick) {
		List<Map.Entry<UUID, PlusUltraImpactState>> entries = new ArrayList<>(PLUS_ULTRA_IMPACT_STATES.entrySet());
		for (Map.Entry<UUID, PlusUltraImpactState> entry : entries) {
			PlusUltraImpactState state = entry.getValue();
			if (PLUS_ULTRA_IMPACT_STATES.get(entry.getKey()) != state) {
				continue;
			}
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick > state.endTick()) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (state.impacted()) {
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			if (currentTick <= state.startTick()) {
				state.lastPosition(entityPosition(target));
				state.lastVelocity(target.getVelocity());
				state.lastSpeed(target.getVelocity().length());
				continue;
			}

			PlusUltraImpactHit impact = detectPlusUltraImpact(target, world, state);
			if (impact != null) {
				double impactSpeed = trackedImpactSpeed(target, state.lastPosition(), state.lastVelocity(), state.lastSpeed());
				double damageAmount = Math.min(
					PLUS_ULTRA_IMPACT_DAMAGE_MAX,
					Math.max(0.0, impactSpeed - PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD) * PLUS_ULTRA_IMPACT_DAMAGE_MULTIPLIER
				);
				if (damageAmount > 0.0) {
					dealTrackedMagicDamage(target, state.casterId(), world.getDamageSources().flyIntoWall(), (float) damageAmount);
					spawnPlusUltraImpactParticles(world, impact);
					playConfiguredSound(world, impact.position(), SoundEvents.ITEM_MACE_SMASH_GROUND_HEAVY, PLUS_ULTRA_IMPACT_SOUND_VOLUME, PLUS_ULTRA_IMPACT_SOUND_PITCH);
				}
				state.impacted(true);
				PLUS_ULTRA_IMPACT_STATES.remove(entry.getKey(), state);
				continue;
			}

			state.lastPosition(entityPosition(target));
			state.lastVelocity(target.getVelocity());
			state.lastSpeed(target.getVelocity().length());
		}
	}

	static PlusUltraImpactHit detectPlusUltraImpact(LivingEntity target, ServerWorld world, PlusUltraImpactState state) {
		return detectVelocityImpact(target, world, state.lastPosition(), state.lastVelocity(), state.lastSpeed(), PLUS_ULTRA_IMPACT_VELOCITY_THRESHOLD);
	}

	static double trackedImpactSpeed(LivingEntity target, Vec3d lastPosition, Vec3d lastVelocity, double lastSpeed) {
		Vec3d targetPosition = entityPosition(target);
		return Math.max(
			Math.max(lastSpeed, lastVelocity.length()),
			Math.max(target.getVelocity().length(), targetPosition.subtract(lastPosition).length())
		);
	}

	static PlusUltraImpactHit detectVelocityImpact(
		LivingEntity target,
		ServerWorld world,
		Vec3d lastPosition,
		Vec3d lastVelocity,
		double lastSpeed,
		double impactVelocityThreshold
	) {
		Vec3d targetPosition = entityPosition(target);
		Vec3d movement = targetPosition.subtract(lastPosition);
		Vec3d sweepVector = movement.lengthSquared() > 1.0E-4 ? movement : lastVelocity;
		if (sweepVector.lengthSquared() <= 1.0E-4) {
			return null;
		}

		double impactSpeed = trackedImpactSpeed(target, lastPosition, lastVelocity, lastSpeed);
		if (impactSpeed < impactVelocityThreshold) {
			return null;
		}

		double currentSpeed = Math.max(target.getVelocity().length(), movement.length());
		boolean collidedWithBlock = target.isInsideWall() || target.horizontalCollision || target.verticalCollision || target.groundCollision;
		boolean slowedHard = lastSpeed >= impactVelocityThreshold
			&& currentSpeed <= Math.max(0.2, lastSpeed * 0.35);
		Vec3d traceEndPos = movement.lengthSquared() > 1.0E-4 ? targetPosition : lastPosition.add(sweepVector);
		BlockHitResult raycastHit = findPlusUltraImpactBlockHit(world, target, lastPosition, traceEndPos, sweepVector);
		if (raycastHit != null) {
			return plusUltraImpactHit(world, raycastHit);
		}

		PlusUltraImpactHit sweptImpact = sweptPlusUltraImpactHit(target, world, lastPosition, traceEndPos, sweepVector);
		if (sweptImpact != null && (collidedWithBlock || slowedHard || movement.lengthSquared() > 1.0E-4)) {
			return sweptImpact;
		}

		if (!collidedWithBlock) {
			Box previousBox = target.getBoundingBox().offset(lastPosition.subtract(targetPosition));
			Box sweptBox = previousBox.stretch(traceEndPos.subtract(lastPosition)).expand(0.15);
			if (!world.getBlockCollisions(target, sweptBox).iterator().hasNext()) {
				if (!slowedHard) {
					return null;
				}
				PlusUltraImpactHit nearbyImpact = nearbyPlusUltraImpactHit(target, world, sweepVector);
				if (nearbyImpact == null) {
					return null;
				}
				return nearbyImpact;
			}
		}

		return fallbackPlusUltraImpactHit(target, world, sweepVector);
	}

	static PlusUltraImpactHit nearbyPlusUltraImpactHit(LivingEntity target, ServerWorld world, Vec3d sweepVector) {
		return bestPlusUltraImpactHitInBox(world, target.getBoundingBox().expand(0.35), target.getBoundingBox().getCenter(), sweepVector);
	}

	static PlusUltraImpactHit sweptPlusUltraImpactHit(LivingEntity target, ServerWorld world, Vec3d startPos, Vec3d endPos, Vec3d sweepVector) {
		Vec3d currentPosition = entityPosition(target);
		Box previousBox = target.getBoundingBox().offset(startPos.subtract(currentPosition));
		Box sweptBox = previousBox.stretch(endPos.subtract(startPos)).expand(0.2);
		return bestPlusUltraImpactHitInBox(world, sweptBox, target.getBoundingBox().getCenter(), sweepVector);
	}

	static PlusUltraImpactHit bestPlusUltraImpactHitInBox(ServerWorld world, Box searchBox, Vec3d referencePos, Vec3d sweepVector) {
		BlockPos minPos = BlockPos.ofFloored(searchBox.minX, searchBox.minY, searchBox.minZ);
		BlockPos maxPos = BlockPos.ofFloored(searchBox.maxX, searchBox.maxY, searchBox.maxZ);
		Vec3d direction = sweepVector.lengthSquared() > 1.0E-4 ? sweepVector.normalize() : Vec3d.ZERO;
		PlusUltraImpactHit bestHit = null;
		double bestDistanceSquared = Double.MAX_VALUE;
		for (int x = minPos.getX(); x <= maxPos.getX(); x++) {
			for (int y = minPos.getY(); y <= maxPos.getY(); y++) {
				for (int z = minPos.getZ(); z <= maxPos.getZ(); z++) {
					BlockPos candidatePos = new BlockPos(x, y, z);
					BlockState candidateState = world.getBlockState(candidatePos);
					if (candidateState.isAir() || candidateState.getCollisionShape(world, candidatePos).isEmpty()) {
						continue;
					}

					PlusUltraImpactHit candidateHit = plusUltraImpactHit(candidatePos, candidateState, referencePos, direction);
					Vec3d toCandidate = candidateHit.position().subtract(referencePos);
					if (direction.lengthSquared() > 1.0E-4 && toCandidate.lengthSquared() > 1.0E-4 && direction.dotProduct(toCandidate.normalize()) < -0.35) {
						continue;
					}

					double distanceSquared = referencePos.squaredDistanceTo(candidateHit.position());
					if (distanceSquared < bestDistanceSquared) {
						bestDistanceSquared = distanceSquared;
						bestHit = candidateHit;
					}
				}
			}
		}
		return bestHit;
	}

	static BlockHitResult findPlusUltraImpactBlockHit(
		ServerWorld world,
		LivingEntity target,
		Vec3d startPos,
		Vec3d endPos,
		Vec3d sweepVector
	) {
		double lowerSampleY = 0.1;
		double middleSampleY = MathHelper.clamp(target.getHeight() * 0.5, 0.2, Math.max(0.2, target.getHeight() - 0.15));
		double upperSampleY = Math.max(middleSampleY, target.getHeight() - 0.15);
		Vec3d horizontalSweep = new Vec3d(sweepVector.x, 0.0, sweepVector.z);
		Vec3d sideOffset = horizontalSweep.lengthSquared() > 1.0E-4
			? new Vec3d(-horizontalSweep.z, 0.0, horizontalSweep.x).normalize().multiply(Math.max(0.2, target.getWidth() * 0.45))
			: Vec3d.ZERO;

		BlockHitResult bestHit = null;
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, lowerSampleY, Vec3d.ZERO), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, middleSampleY, Vec3d.ZERO), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, middleSampleY, sideOffset), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, middleSampleY, sideOffset.multiply(-1.0)), startPos);
		bestHit = closerPlusUltraImpactHit(bestHit, plusUltraImpactRaycast(world, target, startPos, endPos, upperSampleY, Vec3d.ZERO), startPos);
		return bestHit;
	}

	static BlockHitResult closerPlusUltraImpactHit(BlockHitResult currentBest, BlockHitResult candidate, Vec3d startPos) {
		if (candidate == null) {
			return currentBest;
		}
		if (currentBest == null) {
			return candidate;
		}
		return startPos.squaredDistanceTo(candidate.getPos()) < startPos.squaredDistanceTo(currentBest.getPos()) ? candidate : currentBest;
	}

	static BlockHitResult plusUltraImpactRaycast(
		ServerWorld world,
		Entity entity,
		Vec3d startPos,
		Vec3d endPos,
		double yOffset,
		Vec3d lateralOffset
	) {
		Vec3d start = startPos.add(lateralOffset).add(0.0, yOffset, 0.0);
		Vec3d end = endPos.add(lateralOffset).add(0.0, yOffset, 0.0);
		if (start.squaredDistanceTo(end) <= 1.0E-6) {
			return null;
		}

		BlockHitResult hitResult = world.raycast(
			new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER, RaycastContext.FluidHandling.NONE, entity)
		);
		return hitResult.getType() == HitResult.Type.BLOCK ? hitResult : null;
	}

	static PlusUltraImpactHit plusUltraImpactHit(ServerWorld world, BlockHitResult hitResult) {
		BlockPos blockPos = hitResult.getBlockPos();
		BlockState blockState = world.getBlockState(blockPos);
		if (blockState.isAir()) {
			BlockPos offsetPos = blockPos.offset(hitResult.getSide());
			BlockState offsetState = world.getBlockState(offsetPos);
			if (!offsetState.isAir()) {
				blockPos = offsetPos;
				blockState = offsetState;
			}
		}

		if (blockState.isAir()) {
			blockState = Blocks.DIRT.getDefaultState();
		}
		return plusUltraImpactHit(blockPos, blockState, hitResult.getPos(), hitResult.getSide());
	}

	static PlusUltraImpactHit fallbackPlusUltraImpactHit(LivingEntity target, ServerWorld world, Vec3d sweepVector) {
		Vec3d center = target.getBoundingBox().getCenter();
		Vec3d direction = sweepVector.lengthSquared() > 1.0E-4 ? sweepVector.normalize() : Vec3d.ZERO;
		double probeDistance = Math.max(0.35, target.getWidth() * 0.5);
		PlusUltraImpactHit searchedHit = bestPlusUltraImpactHitInBox(
			world,
			target.getBoundingBox().stretch(direction.multiply(probeDistance)).expand(0.35),
			center,
			sweepVector
		);
		if (searchedHit != null) {
			return searchedHit;
		}

		Direction side = plusUltraImpactSide(center, BlockPos.ofFloored(center), direction);
		Vec3d impactPos = center.add(Vec3d.of(side.getVector()).multiply(Math.max(0.2, target.getWidth() * 0.45)));
		return new PlusUltraImpactHit(impactPos, Blocks.DIRT.getDefaultState(), side);
	}

	static PlusUltraImpactHit plusUltraImpactHit(BlockPos blockPos, BlockState blockState, Vec3d referencePos, Direction side) {
		BlockState resolvedState = blockState.isAir() ? Blocks.DIRT.getDefaultState() : blockState;
		Vec3d impactPos = plusUltraImpactSurfacePosition(blockPos, side, referencePos);
		return new PlusUltraImpactHit(impactPos, resolvedState, side);
	}

	static PlusUltraImpactHit plusUltraImpactHit(BlockPos blockPos, BlockState blockState, Vec3d referencePos, Vec3d sweepDirection) {
		return plusUltraImpactHit(blockPos, blockState, referencePos, plusUltraImpactSide(referencePos, blockPos, sweepDirection));
	}

	static Direction plusUltraImpactSide(Vec3d referencePos, BlockPos blockPos, Vec3d sweepDirection) {
		if (sweepDirection.lengthSquared() > 1.0E-4) {
			return Direction.getFacing(sweepDirection.x, sweepDirection.y, sweepDirection.z).getOpposite();
		}

		Vec3d blockCenter = Vec3d.ofCenter(blockPos);
		Vec3d outward = referencePos.subtract(blockCenter);
		if (outward.lengthSquared() > 1.0E-4) {
			return Direction.getFacing(outward.x, outward.y, outward.z);
		}
		return Direction.UP;
	}

	static Vec3d plusUltraImpactSurfacePosition(BlockPos blockPos, Direction side, Vec3d referencePos) {
		double minX = blockPos.getX();
		double minY = blockPos.getY();
		double minZ = blockPos.getZ();
		double maxX = minX + 1.0;
		double maxY = minY + 1.0;
		double maxZ = minZ + 1.0;
		double impactX = MathHelper.clamp(referencePos.x, minX, maxX);
		double impactY = MathHelper.clamp(referencePos.y, minY, maxY);
		double impactZ = MathHelper.clamp(referencePos.z, minZ, maxZ);
		switch (side) {
			case WEST -> impactX = minX;
			case EAST -> impactX = maxX;
			case DOWN -> impactY = minY;
			case UP -> impactY = maxY;
			case NORTH -> impactZ = minZ;
			case SOUTH -> impactZ = maxZ;
		}
		return new Vec3d(impactX, impactY, impactZ);
	}

	static void spawnPlusUltraImpactParticles(ServerWorld world, PlusUltraImpactHit impact) {
		spawnVelocityImpactParticles(
			world,
			impact,
			PLUS_ULTRA_IMPACT_PARTICLE_COUNT,
			PLUS_ULTRA_IMPACT_PARTICLE_SPREAD,
			PLUS_ULTRA_IMPACT_PARTICLE_SPEED,
			PLUS_ULTRA_IMPACT_DUST_PARTICLE_COUNT,
			PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPREAD,
			PLUS_ULTRA_IMPACT_DUST_PARTICLE_SPEED
		);
	}

	static void spawnComedicAssistantCaneImpactParticles(ServerWorld world, PlusUltraImpactHit impact) {
		spawnVelocityImpactParticles(
			world,
			impact,
			Math.max(10, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount() * 2),
			0.46,
			0.12,
			Math.max(8, COMEDIC_ASSISTANT_GIANT_CANE_YANK.particleCount() + 4),
			0.34,
			0.06
		);
	}

	static void spawnVelocityImpactParticles(
		ServerWorld world,
		PlusUltraImpactHit impact,
		int blockParticleCount,
		double blockParticleSpread,
		double blockParticleSpeed,
		int dustParticleCount,
		double dustParticleSpread,
		double dustParticleSpeed
	) {
		if (impact == null) {
			return;
		}

		BlockState particleState = impact.blockState().isAir() ? Blocks.DIRT.getDefaultState() : impact.blockState();
		Vec3d normal = Vec3d.of(impact.side().getVector());
		Vec3d emissionCenter = impact.position().add(normal.multiply(0.08));
		double impactX = emissionCenter.x;
		double impactY = emissionCenter.y;
		double impactZ = emissionCenter.z;
		double spreadX = impact.side().getAxis() == Direction.Axis.X ? 0.08 : blockParticleSpread;
		double spreadY = impact.side().getAxis() == Direction.Axis.Y ? 0.08 : Math.max(0.12, blockParticleSpread * 0.55);
		double spreadZ = impact.side().getAxis() == Direction.Axis.Z ? 0.08 : blockParticleSpread;
		world.spawnParticles(
			new BlockStateParticleEffect(ParticleTypes.BLOCK, particleState),
			impactX,
			impactY,
			impactZ,
			blockParticleCount,
			spreadX,
			spreadY,
			spreadZ,
			blockParticleSpeed
		);
		world.spawnParticles(
			ParticleTypes.CLOUD,
			impactX,
			impactY,
			impactZ,
			Math.max(4, blockParticleCount / 4),
			Math.max(0.04, spreadX * 0.85),
			Math.max(0.08, spreadY * 0.85),
			Math.max(0.04, spreadZ * 0.85),
			Math.max(0.02, blockParticleSpeed * 0.35)
		);
		world.spawnParticles(
			ParticleTypes.DUST_PLUME,
			impactX,
			impactY,
			impactZ,
			dustParticleCount,
			impact.side().getAxis() == Direction.Axis.X ? 0.08 : dustParticleSpread,
			impact.side().getAxis() == Direction.Axis.Y ? 0.08 : Math.max(0.12, dustParticleSpread * 0.45),
			impact.side().getAxis() == Direction.Axis.Z ? 0.08 : dustParticleSpread,
			dustParticleSpeed
		);
	}

	static void updateComedicAssistantCarries(MinecraftServer server, int currentTick) {
		Iterator<Map.Entry<UUID, ComedicAssistantParrotCarryState>> iterator = COMEDIC_ASSISTANT_PARROT_CARRY_STATES.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, ComedicAssistantParrotCarryState> entry = iterator.next();
			ComedicAssistantParrotCarryState state = entry.getValue();
			ServerWorld world = server.getWorld(state.dimension());
			if (world == null) {
				iterator.remove();
				continue;
			}

			Entity entity = world.getEntity(entry.getKey());
			if (!(entity instanceof LivingEntity target) || !isMagicTargetableEntity(target)) {
				iterator.remove();
				continue;
			}

			double remainingLift = Math.max(0.0, state.targetY() - target.getY());
			double upwardVelocity = Math.min(state.settings().upwardVelocity(), Math.max(0.45, remainingLift));
			double liftStep = Math.min(1.5, Math.max(0.2, upwardVelocity));
			if (
				currentTick >= state.endTick()
				|| target.getY() >= state.targetY() - 0.75
				|| !world.isSpaceEmpty(target, target.getBoundingBox().offset(0.0, liftStep, 0.0))
			) {
				releaseComedicAssistantCarry(target, state.settings());
				iterator.remove();
				continue;
			}

			Vec3d nextVelocity = new Vec3d(target.getVelocity().x * 0.18, upwardVelocity, target.getVelocity().z * 0.18);
			double nextY = Math.min(state.targetY(), target.getY() + upwardVelocity);
			forceTargetPositionAndVelocity(world, target, new Vec3d(target.getX(), nextY, target.getZ()), nextVelocity);
			target.fallDistance = 0.0F;
			if (state.settings().applyGlowing()) {
				refreshStatusEffect(target, StatusEffects.GLOWING, Math.max(2, state.settings().glowingDurationTicks()), 0, true, false, true);
			}
			if (currentTick >= state.nextParticleTick()) {
				spawnComedicAssistantItemParticles(
					world,
					new ItemStack(Items.FEATHER),
					new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ()),
					state.settings().particleCount(),
					0.45,
					0.08
				);
				state.nextParticleTick(currentTick + 4);
			}
			if (currentTick >= state.nextSoundTick()) {
				playConfiguredSound(
					world,
					new Vec3d(target.getX(), target.getY() + target.getHeight(), target.getZ()),
					SoundEvents.ENTITY_PARROT_FLY,
					state.settings().soundVolume(),
					state.settings().soundPitch()
				);
				state.nextSoundTick(currentTick + state.settings().flapSoundIntervalTicks());
			}
		}
	}

	static void releaseComedicAssistantCarry(LivingEntity target, ComedicAssistantParrotSettings settings) {
		double downwardVelocity = Math.max(0.0, settings.releaseDownwardVelocity());
		if (downwardVelocity <= 0.0) {
			return;
		}
		applyForcedVelocity(target, new Vec3d(target.getVelocity().x * 0.12, -downwardVelocity, target.getVelocity().z * 0.12));
	}

	static double resolveComedicAssistantCarryTargetY(LivingEntity target, ServerWorld world, double liftHeight) {
		double maxLiftHeight = Math.max(0.0, liftHeight);
		double topLimit = world.getTopYInclusive() - 1.0;
		double startingY = target.getY();
		double bestY = startingY;
		Box box = target.getBoundingBox();
		for (double offset = 1.0; offset <= maxLiftHeight; offset += 1.0) {
			double candidateY = Math.min(topLimit, startingY + offset);
			if (candidateY <= bestY) {
				break;
			}
			if (!world.isSpaceEmpty(target, box.offset(0.0, candidateY - startingY, 0.0))) {
				break;
			}
			bestY = candidateY;
			if (candidateY >= topLimit) {
				break;
			}
		}
		return bestY;
	}

	static List<ServerPlayerEntity> comedicRewriteVisualViewers(ServerWorld world, Vec3d origin) {
		List<ServerPlayerEntity> viewers = new ArrayList<>();
		double maxDistanceSquared = COMEDIC_REWRITE_VISUAL_VIEW_DISTANCE * COMEDIC_REWRITE_VISUAL_VIEW_DISTANCE;
		for (ServerPlayerEntity viewer : world.getPlayers()) {
			if (viewer.squaredDistanceTo(origin) <= maxDistanceSquared) {
				viewers.add(viewer);
			}
		}
		return viewers;
	}

	static List<ServerPlayerEntity> comedicRewriteVisualViewers(MinecraftServer server, List<UUID> viewerIds) {
		List<ServerPlayerEntity> viewers = new ArrayList<>(viewerIds.size());
		for (UUID viewerId : viewerIds) {
			ServerPlayerEntity viewer = server.getPlayerManager().getPlayer(viewerId);
			if (viewer != null) {
				viewers.add(viewer);
			}
		}
		return viewers;
	}

	static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		ComedicRewriteVisualFollowState followState,
		Entity... entities
	) {
		queueComedicRewriteVisualCameo(server, durationTicks, viewers, chargeDurationTicks, followState, null, entities);
	}

	static void queueComedicRewriteVisualCameo(
		MinecraftServer server,
		int durationTicks,
		List<ServerPlayerEntity> viewers,
		int chargeDurationTicks,
		ComedicRewriteVisualFollowState followState,
		ComedicRewriteVisualRotationState rotationState,
		Entity... entities
	) {
		if (server == null || durationTicks <= 0 || viewers.isEmpty() || entities.length == 0) {
			return;
		}

		int[] entityIds = new int[entities.length];
		for (int index = 0; index < entities.length; index++) {
			entityIds[index] = spawnPacketOnlyEntity(viewers, entities[index]);
		}

		List<UUID> viewerIds = new ArrayList<>(viewers.size());
		for (ServerPlayerEntity viewer : viewers) {
			viewerIds.add(viewer.getUuid());
		}
		int chargeEndTick = chargeDurationTicks > 0 && chargeDurationTicks < durationTicks ? server.getTicks() + chargeDurationTicks : 0;
		COMEDIC_REWRITE_VISUAL_CAMEOS.add(
			new ComedicRewriteVisualCameo(
				server.getTicks() + durationTicks,
				chargeEndTick,
				entityIds,
				List.copyOf(viewerIds),
				followState,
				rotationState,
				entities
			)
		);
	}

	static int spawnPacketOnlyEntity(List<ServerPlayerEntity> viewers, Entity entity) {
		EntitySpawnS2CPacket spawnPacket = new EntitySpawnS2CPacket(
			entity.getId(),
			entity.getUuid(),
			entity.getX(),
			entity.getY(),
			entity.getZ(),
			entity.getPitch(),
			entity.getYaw(),
			entity.getType(),
			0,
			entity.getVelocity(),
			entity.getHeadYaw()
		);
		List<DataTracker.SerializedEntry<?>> trackedValues = entity.getDataTracker().getChangedEntries();
		EntityTrackerUpdateS2CPacket trackerPacket = trackedValues == null || trackedValues.isEmpty()
			? null
			: new EntityTrackerUpdateS2CPacket(entity.getId(), trackedValues);
		EntityVelocityUpdateS2CPacket velocityPacket = entity.getVelocity().lengthSquared() <= 1.0E-5
			? null
			: new EntityVelocityUpdateS2CPacket(entity.getId(), entity.getVelocity());

		for (ServerPlayerEntity viewer : viewers) {
			viewer.networkHandler.sendPacket(spawnPacket);
			if (trackerPacket != null) {
				viewer.networkHandler.sendPacket(trackerPacket);
			}
			if (velocityPacket != null) {
				viewer.networkHandler.sendPacket(velocityPacket);
			}
		}
		return entity.getId();
	}
}



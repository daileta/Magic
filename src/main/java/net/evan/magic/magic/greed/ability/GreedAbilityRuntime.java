package net.evan.magic.magic.ability;
import net.evan.magic.magic.core.ability.MagicAbility;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import net.evan.magic.Magic;
import net.evan.magic.config.MagicConfig;
import net.evan.magic.magic.core.MagicPlayerData;
import net.evan.magic.magic.core.MagicSchool;
import net.evan.magic.particle.TollkeepersClaimVortexParticleEffect;
import net.evan.magic.registry.ModStatusEffects;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.RespawnAnchorBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.BowItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ItemStackParticleEffect;
import net.minecraft.particle.ParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;

public final class GreedAbilityRuntime extends GreedAbilityRequestHandler {
	private GreedAbilityRuntime() {
	}

	public static boolean handleAbilityRequest(ServerPlayerEntity player, MagicAbility ability) {
		if (ability.school() != MagicSchool.GREED) {
			return false;
		}

		int currentTick = currentTick(player);
		if (isAbilityUseLocked(player, currentTick)) {
			player.sendMessage(Text.translatable("message.magic.greed.ability_locked"), true);
			return true;
		}

		if (ability == MagicAbility.APPRAISERS_MARK) {
			handleAppraisersMarkRequest(player, currentTick);
			return true;
		}
		if (ability == MagicAbility.TOLLKEEPERS_CLAIM) {
			handleTollkeepersClaimRequest(player, currentTick);
			return true;
		}
		if (ability == MagicAbility.KINGS_DUES) {
			handleKingsDuesRequest(player, currentTick);
			return true;
		}
		if (ability == MagicAbility.BANKRUPTCY) {
			handleBankruptcyRequest(player, currentTick);
			return true;
		}

		return false;
	}
}




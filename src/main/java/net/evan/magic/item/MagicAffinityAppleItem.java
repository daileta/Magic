package net.evan.magic.item;

import net.evan.magic.magic.MagicPlayerData;
import net.evan.magic.magic.MagicSchool;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.world.World;

public final class MagicAffinityAppleItem extends Item {
	private final MagicSchool school;

	public MagicAffinityAppleItem(MagicSchool school, Settings settings) {
		super(settings);
		this.school = school;
	}

	@Override
	public ItemStack finishUsing(ItemStack stack, World world, LivingEntity user) {
		ItemStack result = super.finishUsing(stack, world, user);

		if (!world.isClient() && user instanceof ServerPlayerEntity player) {
			applyMagicAffinity(player);
		}

		return result;
	}

	private void applyMagicAffinity(PlayerEntity player) {
		MagicSchool currentSchool = MagicPlayerData.getSchool(player);

		if (currentSchool == MagicSchool.NONE) {
			MagicPlayerData.unlock(player, school);
			player.sendMessage(Text.translatable("message.magic.unlocked", school.displayName()), false);
			return;
		}

		if (currentSchool == school) {
			MagicPlayerData.refillMana(player);
			player.sendMessage(Text.translatable("message.magic.recharged", school.displayName()), false);
			return;
		}

		player.sendMessage(Text.translatable("message.magic.already_attuned", currentSchool.displayName()), false);
	}
}

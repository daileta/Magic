package net.evan.magic.magic.ability;

import net.evan.magic.magic.MagicSchool;
import net.minecraft.text.Text;

public enum MagicAbility {
	NONE(0, MagicSchool.NONE, "none"),
	BELOW_FREEZING(1, MagicSchool.FROST, "below_freezing"),
	ABSOLUTE_ZERO(2, MagicSchool.FROST, "absolute_zero"),
	PLANCK_HEAT(3, MagicSchool.FROST, "planck_heat"),
	FROST_DOMAIN_EXPANSION(5, MagicSchool.FROST, "frost_domain_expansion"),
	LOVE_AT_FIRST_SIGHT(1, MagicSchool.LOVE, "love_at_first_sight"),
	TILL_DEATH_DO_US_PART(2, MagicSchool.LOVE, "till_death_do_us_part"),
	MANIPULATION(3, MagicSchool.LOVE, "manipulation"),
	LOVE_DOMAIN_EXPANSION(5, MagicSchool.LOVE, "love_domain_expansion");

	private final int slot;
	private final MagicSchool school;
	private final String id;

	MagicAbility(int slot, MagicSchool school, String id) {
		this.slot = slot;
		this.school = school;
		this.id = id;
	}

	public int slot() {
		return slot;
	}

	public MagicSchool school() {
		return school;
	}

	public String id() {
		return id;
	}

	public Text displayName() {
		return Text.translatable("magic.ability." + id);
	}

	public static MagicAbility fromId(String id) {
		if (id == null) {
			return NONE;
		}

		for (MagicAbility ability : values()) {
			if (ability.id.equals(id)) {
				return ability;
			}
		}

		return NONE;
	}

	public static MagicAbility fromSlot(int slot) {
		for (MagicAbility ability : values()) {
			if (ability.slot == slot) {
				return ability;
			}
		}

		return NONE;
	}

	public static MagicAbility fromSlotForSchool(int slot, MagicSchool school) {
		for (MagicAbility ability : values()) {
			if (ability.slot == slot && ability.school == school) {
				return ability;
			}
		}

		return NONE;
	}
}

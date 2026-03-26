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
	MANIPULATION(3, MagicSchool.LOVE, "empty_embrace"),
	LOVE_DOMAIN_EXPANSION(5, MagicSchool.LOVE, "love_domain_expansion"),
	MARTYRS_FLAME(1, MagicSchool.BURNING_PASSION, "martyrs_flame"),
	SPOTLIGHT(1, MagicSchool.JESTER, "spotlight"),
	WITTY_ONE_LINER(2, MagicSchool.JESTER, "witty_one_liner"),
	COMEDIC_REWRITE(3, MagicSchool.JESTER, "comedic_rewrite"),
	CASSIOPEIA(1, MagicSchool.CONSTELLATION, "cassiopeia"),
	HERCULES_BURDEN_OF_THE_SKY(2, MagicSchool.CONSTELLATION, "hercules_burden_of_the_sky"),
	SAGITTARIUS_ASTRAL_ARROW(3, MagicSchool.CONSTELLATION, "sagittarius_astral_arrow"),
	ORIONS_GAMBIT(4, MagicSchool.CONSTELLATION, "orions_gambit"),
	ASTRAL_CATACLYSM(5, MagicSchool.CONSTELLATION, "astral_cataclysm"),
	APPRAISERS_MARK(1, MagicSchool.GREED, "appraisers_mark"),
	TOLLKEEPERS_CLAIM(2, MagicSchool.GREED, "tollkeepers_claim"),
	KINGS_DUES(3, MagicSchool.GREED, "kings_dues"),
	BANKRUPTCY(4, MagicSchool.GREED, "bankruptcy");

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

		if ("manipulation".equals(id)) {
			return MANIPULATION;
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

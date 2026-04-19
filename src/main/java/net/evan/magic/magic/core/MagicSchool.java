package net.evan.magic.magic.core;

import net.minecraft.text.Text;

public enum MagicSchool {
	NONE("none"),
	FROST("frost"),
	LOVE("love"),
	BURNING_PASSION("burning_passion"),
	JESTER("jester"),
	CONSTELLATION("constellation"),
	GREED("greed");

	private final String id;

	MagicSchool(String id) {
		this.id = id;
	}

	public String id() {
		return id;
	}

	public boolean isMagic() {
		return this != NONE;
	}

	public Text displayName() {
		return Text.translatable("magic.school." + id);
	}

	public static MagicSchool fromId(String id) {
		for (MagicSchool school : values()) {
			if (school.id.equals(id)) {
				return school;
			}
		}

		return NONE;
	}
}


package net.evan.magic.magic;

import net.minecraft.text.Text;

public enum MagicSchool {
	NONE("none"),
	FROST("frost"),
	LOVE("love");

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

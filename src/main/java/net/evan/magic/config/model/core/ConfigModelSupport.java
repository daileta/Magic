package net.evan.magic.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.evan.magic.Magic;
import net.evan.magic.magic.core.MagicSchool;
import net.evan.magic.magic.core.ability.MagicAbility;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.math.MathHelper;

abstract class ConfigModelSupport {
	protected static double normalizeCoinAmount(double value) {
		double clamped = Math.max(0.0, value);
		return Math.round(clamped * 100.0) / 100.0;
	}

	protected static String normalizeColorHex(String value, String fallback) {
		if (value == null) {
			return fallback;
		}

		String normalized = value.trim();
		if (normalized.isEmpty()) {
			return fallback;
		}

		if (!normalized.startsWith("#")) {
			normalized = "#" + normalized;
		}

		return normalized.length() == 7 ? normalized.toUpperCase() : fallback;
	}

	protected static Set<MagicAbility> parseAbilityIds(List<String> ids, String context) {
		EnumSet<MagicAbility> abilities = EnumSet.noneOf(MagicAbility.class);
		if (ids == null) {
			return abilities;
		}

		for (String id : ids) {
			if (id == null) {
				continue;
			}

			String normalized = id.trim().toLowerCase();
			if (normalized.isEmpty()) {
				continue;
			}

			MagicAbility ability = MagicAbility.fromId(normalized);
			if (ability == MagicAbility.NONE) {
				if (!"none".equals(normalized)) {
					Magic.LOGGER.warn("Ignoring unknown ability id '{}' in {}.", id, context);
				}
				continue;
			}

			abilities.add(ability);
		}

		return abilities;
	}

	protected static Map<UUID, Set<MagicAbility>> parsePlayerAbilityIds(Map<String, List<String>> raw, String context) {
		Map<UUID, Set<MagicAbility>> resolved = new HashMap<>();
		if (raw == null) {
			return resolved;
		}

		for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
			String uuidString = entry.getKey();
			if (uuidString == null || uuidString.isBlank()) {
				continue;
			}

			try {
				UUID playerId = UUID.fromString(uuidString.trim());
				Set<MagicAbility> abilities = parseAbilityIds(entry.getValue(), context + "." + uuidString);
				resolved.put(playerId, abilities);
			} catch (IllegalArgumentException exception) {
				Magic.LOGGER.warn("Ignoring invalid UUID '{}' in {}.", uuidString, context);
			}
		}

		return resolved;
	}

	protected static boolean supportsStageProgressionAccess(MagicSchool school) {
		return school == MagicSchool.FROST || school == MagicSchool.BURNING_PASSION;
	}

	protected static boolean isStageProgressionStageSupported(MagicSchool school, int stage) {
		return supportsStageProgressionAccess(school) && stage >= 2 && stage <= 3;
	}

	protected static String stageProgressionId(MagicSchool school, int stage) {
		return school.id() + ":" + stage;
	}

	protected static Set<StageProgressionLock> parseStageProgressionIds(List<String> ids, String context) {
		Set<StageProgressionLock> locks = new java.util.HashSet<>();
		if (ids == null) {
			return locks;
		}

		for (String id : ids) {
			if (id == null) {
				continue;
			}

			String normalized = id.trim().toLowerCase();
			if (normalized.isEmpty()) {
				continue;
			}

			int separatorIndex = normalized.indexOf(':');
			if (separatorIndex < 0) {
				MagicSchool school = MagicSchool.fromId(normalized);
				if (!supportsStageProgressionAccess(school)) {
					Magic.LOGGER.warn("Ignoring unsupported stage progression id '{}' in {}.", id, context);
					continue;
				}

				for (int stage = 2; stage <= 3; stage++) {
					locks.add(new StageProgressionLock(school, stage));
				}
				continue;
			}

			String schoolId = normalized.substring(0, separatorIndex).trim();
			String stageToken = normalized.substring(separatorIndex + 1).trim();
			MagicSchool school = MagicSchool.fromId(schoolId);
			if (!supportsStageProgressionAccess(school)) {
				Magic.LOGGER.warn("Ignoring unsupported stage progression school id '{}' in {}.", id, context);
				continue;
			}

			int stage;
			try {
				stage = Integer.parseInt(stageToken);
			} catch (NumberFormatException exception) {
				Magic.LOGGER.warn("Ignoring invalid stage progression id '{}' in {}.", id, context);
				continue;
			}

			if (!isStageProgressionStageSupported(school, stage)) {
				Magic.LOGGER.warn("Ignoring unsupported stage progression id '{}' in {}.", id, context);
				continue;
			}

			locks.add(new StageProgressionLock(school, stage));
		}

		return locks;
	}

	protected static Map<UUID, Set<StageProgressionLock>> parsePlayerStageProgressionIds(Map<String, List<String>> raw, String context) {
		Map<UUID, Set<StageProgressionLock>> resolved = new HashMap<>();
		if (raw == null) {
			return resolved;
		}

		for (Map.Entry<String, List<String>> entry : raw.entrySet()) {
			String uuidString = entry.getKey();
			if (uuidString == null || uuidString.isBlank()) {
				continue;
			}

			try {
				UUID playerId = UUID.fromString(uuidString.trim());
				Set<StageProgressionLock> locks = parseStageProgressionIds(entry.getValue(), context + "." + uuidString);
				resolved.put(playerId, locks);
			} catch (IllegalArgumentException exception) {
				Magic.LOGGER.warn("Ignoring invalid UUID '{}' in {}.", uuidString, context);
			}
		}

		return resolved;
	}

	protected static ArrayList<String> normalizeStringList(List<String> values, List<String> defaults) {
		ArrayList<String> normalized = new ArrayList<>();
		if (values != null) {
			for (String value : values) {
				if (value == null) {
					continue;
				}
				String trimmed = value.trim().toLowerCase(java.util.Locale.ROOT);
				if (trimmed.isBlank() || normalized.contains(trimmed)) {
					continue;
				}
				normalized.add(trimmed);
			}
		}
		if (!normalized.isEmpty()) {
			return normalized;
		}
		return new ArrayList<>(defaults);
	}

	protected static ArrayList<String> normalizeDisplayTextLines(List<String> values, List<String> defaults, int maxLines, int maxLineLength) {
		ArrayList<String> normalized = new ArrayList<>();
		boolean hasNonBlankLine = false;
		if (values != null) {
			for (String value : values) {
				if (value == null) {
					continue;
				}
				String normalizedValue = value.strip();
				if (normalizedValue.length() > maxLineLength) {
					normalizedValue = normalizedValue.substring(0, maxLineLength);
				}
				normalized.add(normalizedValue);
				hasNonBlankLine |= !normalizedValue.isBlank();
				if (normalized.size() >= Math.max(1, maxLines)) {
					break;
				}
			}
		}
		if (hasNonBlankLine) {
			return normalized;
		}
		return new ArrayList<>(defaults);
	}

	protected static ArrayList<String> normalizeBlockIdList(List<String> values, List<String> defaults) {
		return normalizeStringList(values, defaults);
	}

	protected static String normalizeSide(String value) {
		if (value == null) {
			return "south";
		}
		String normalized = value.trim().toLowerCase(java.util.Locale.ROOT);
		return switch (normalized) {
			case "north", "south", "east", "west" -> normalized;
			default -> "south";
		};
	}

	protected record StageProgressionLock(MagicSchool school, int stage) {
	}
}



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

public final class MagicConfig extends ConfigDataAndFrostModels {
	private static final TypeAdapter<Integer> FLEXIBLE_INTEGER_ADAPTER = new TypeAdapter<>() {
		@Override
		public void write(JsonWriter out, Integer value) throws IOException {
			if (value == null) {
				out.nullValue();
				return;
			}

			out.value(value);
		}

		@Override
		public Integer read(JsonReader in) throws IOException {
			JsonToken token = in.peek();
			if (token == JsonToken.NULL) {
				in.nextNull();
				return 0;
			}
			if (token != JsonToken.NUMBER && token != JsonToken.STRING) {
				throw new IOException("Expected numeric config value but found " + token);
			}

			String raw = in.nextString();
			try {
				BigDecimal rounded = new BigDecimal(raw).setScale(0, RoundingMode.HALF_UP);
				if (rounded.compareTo(BigDecimal.valueOf(Integer.MAX_VALUE)) > 0) {
					return Integer.MAX_VALUE;
				}
				if (rounded.compareTo(BigDecimal.valueOf(Integer.MIN_VALUE)) < 0) {
					return Integer.MIN_VALUE;
				}
				return rounded.intValueExact();
			} catch (NumberFormatException | ArithmeticException exception) {
				throw new IOException("Invalid integer config value: " + raw, exception);
			}
		}
	};

	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.registerTypeAdapter(Integer.class, FLEXIBLE_INTEGER_ADAPTER)
		.registerTypeAdapter(int.class, FLEXIBLE_INTEGER_ADAPTER)
		.create();

	private static final Path CONFIG_PATH = FabricLoader.getInstance()
		.getConfigDir()
		.resolve(Magic.MOD_ID)
		.resolve("magic-server-config.json");

	private static volatile MagicConfigData data = defaultData();

	private MagicConfig() {
	}

	public static void initialize() {
		reload();
	}

	public static boolean reload() {
		try {
			Files.createDirectories(CONFIG_PATH.getParent());

			if (!Files.exists(CONFIG_PATH)) {
				MagicConfigData defaults = defaultData();
				write(defaults);
				data = defaults;
				Magic.LOGGER.info("Created default magic config at {}", CONFIG_PATH);
				return true;
			}

			try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
				MagicConfigData loaded = GSON.fromJson(reader, MagicConfigData.class);
				if (loaded == null) {
					throw new IllegalStateException("Config file is empty.");
				}

				loaded.normalize();
				data = loaded;
				return true;
			}
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to load magic config at {}. Keeping previous values.", CONFIG_PATH, exception);
			return false;
		}
	}

	public static MagicConfigData get() {
		return data;
	}

	public static Path path() {
		return CONFIG_PATH;
	}

	public static synchronized boolean setPlayerAbilityLocked(Collection<UUID> playerIds, MagicAbility ability, boolean locked) {
		if (playerIds == null || playerIds.isEmpty() || ability == null || ability == MagicAbility.NONE || !ability.school().isMagic()) {
			return false;
		}

		try {
			MagicConfigData updated = copyOf(data);
			boolean changed = false;

			for (UUID playerId : playerIds) {
				if (playerId == null) {
					continue;
				}
				changed |= updated.abilityAccess.setPlayerAbilityLocked(playerId, ability, locked);
			}

			if (!changed) {
				return true;
			}

			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist ability {}={} overrides.", ability.id(), locked ? "locked" : "unlocked", exception);
			return false;
		}
	}

	public static synchronized boolean setPlayerStageProgressionLocked(Collection<UUID> playerIds, MagicSchool school, int stage, boolean locked) {
		if (playerIds == null || playerIds.isEmpty() || !isStageProgressionStageSupported(school, stage)) {
			return false;
		}

		try {
			MagicConfigData updated = copyOf(data);
			boolean changed = false;

			for (UUID playerId : playerIds) {
				if (playerId == null) {
					continue;
				}
				changed |= updated.abilityAccess.setPlayerStageProgressionLocked(playerId, school, stage, locked);
			}

			if (!changed) {
				return true;
			}

			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist {} stage {} progression locked={} overrides.", school.id(), stage, locked, exception);
			return false;
		}
	}

	public static synchronized boolean clearPlayerAbilityOverrides(Collection<UUID> playerIds) {
		if (playerIds == null || playerIds.isEmpty()) {
			return false;
		}

		try {
			MagicConfigData updated = copyOf(data);
			boolean changed = false;

			for (UUID playerId : playerIds) {
				if (playerId == null) {
					continue;
				}
				changed |= updated.abilityAccess.clearPlayerOverrides(playerId);
			}

			if (!changed) {
				return true;
			}

			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist player access override cleanup.", exception);
			return false;
		}
	}

	public static synchronized boolean setAstralCataclysmAllowMobTargets(boolean allowMobTargets) {
		try {
			MagicConfigData updated = copyOf(data);
			if (updated.constellationDomain.allowMobTargets == allowMobTargets) {
				return true;
			}

			updated.constellationDomain.allowMobTargets = allowMobTargets;
			updated.normalize();
			write(updated);
			data = updated;
			return true;
		} catch (Exception exception) {
			Magic.LOGGER.error("Failed to persist Astral Cataclysm mob targeting={} toggle.", allowMobTargets, exception);
			return false;
		}
	}

	private static MagicConfigData defaultData() {
		MagicConfigData defaults = new MagicConfigData();
		defaults.normalize();
		return defaults;
	}

	private static MagicConfigData copyOf(MagicConfigData source) {
		MagicConfigData copy = GSON.fromJson(GSON.toJson(source), MagicConfigData.class);
		if (copy == null) {
			copy = new MagicConfigData();
		}
		copy.normalize();
		return copy;
	}

	private static void write(MagicConfigData config) throws IOException {
		try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
			GSON.toJson(config, writer);
		}
	}
}



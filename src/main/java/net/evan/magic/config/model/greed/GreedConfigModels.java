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

public abstract class GreedConfigModels extends GreedDomainConfigModels {
	public static final class GreedConfig {
		public ManaSicknessConfig manaSickness = new ManaSicknessConfig();
		public AppraisersMarkConfig appraisersMark = new AppraisersMarkConfig();
		public TollkeepersClaimConfig tollkeepersClaim = new TollkeepersClaimConfig();
		public KingsDuesConfig kingsDues = new KingsDuesConfig();
		public BankruptcyConfig bankruptcy = new BankruptcyConfig();
		public GreedDomainConfig domain = new GreedDomainConfig();

		public void normalize() {
			if (manaSickness == null) {
				manaSickness = new ManaSicknessConfig();
			}
			if (appraisersMark == null) {
				appraisersMark = new AppraisersMarkConfig();
			}
			if (tollkeepersClaim == null) {
				tollkeepersClaim = new TollkeepersClaimConfig();
			}
			if (kingsDues == null) {
				kingsDues = new KingsDuesConfig();
			}
			if (bankruptcy == null) {
				bankruptcy = new BankruptcyConfig();
			}
			if (domain == null) {
				domain = new GreedDomainConfig();
			}

			manaSickness.normalize();
			appraisersMark.normalize();
			tollkeepersClaim.normalize();
			kingsDues.normalize();
			bankruptcy.normalize();
			domain.normalize();
		}
	}
}



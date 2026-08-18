package dev.evanklein.battlesoldiers.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.evanklein.battlesoldiers.BattleSoldiersMod;
import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.GearLevel;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;

/**
 * Server-side sandbox configuration edited through the in-game GUI.
 * Stores class spawn weights plus per-tier gear and supply overrides,
 * persisted as JSON in the Fabric config directory.
 */
public final class SoldierConfig {
	public static final String KEY_HELMET = "helmet";
	public static final String KEY_CHESTPLATE = "chestplate";
	public static final String KEY_LEGGINGS = "leggings";
	public static final String KEY_BOOTS = "boots";
	public static final String KEY_SWORD = "sword";
	public static final String KEY_AXE = "axe";
	public static final String KEY_BOW = "bow";
	public static final String KEY_SHIELD = "shield";
	public static final String KEY_SPEAR = "spear";
	public static final List<String> GEAR_KEYS = List.of(
			KEY_HELMET, KEY_CHESTPLATE, KEY_LEGGINGS, KEY_BOOTS,
			KEY_SWORD, KEY_AXE, KEY_BOW, KEY_SHIELD, KEY_SPEAR
	);

	public static final String SUPPLY_GOLDEN_APPLES = "golden_apples";
	public static final String SUPPLY_ENCHANTED_GOLDEN_APPLES = "enchanted_golden_apples";
	public static final String SUPPLY_COBWEBS = "cobwebs";
	public static final String SUPPLY_ARROWS = "arrows";
	public static final String SUPPLY_BUILDING_BLOCKS = "building_blocks";
	public static final String SUPPLY_TOTEMS = "totems";
	public static final String SUPPLY_SHIELD_CHANCE = "shield_chance";
	public static final String SUPPLY_POTION_CHANCE = "potion_chance";
	public static final List<String> SUPPLY_KEYS = List.of(
			SUPPLY_GOLDEN_APPLES, SUPPLY_ENCHANTED_GOLDEN_APPLES, SUPPLY_COBWEBS, SUPPLY_ARROWS,
			SUPPLY_BUILDING_BLOCKS, SUPPLY_TOTEMS, SUPPLY_SHIELD_CHANCE, SUPPLY_POTION_CHANCE
	);

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
	private static final String FILE_NAME = "battle-soldiers.json";
	private static volatile SoldierConfig instance = new SoldierConfig(null);

	private final Map<CombatRole, Double> roleWeights = new EnumMap<>(CombatRole.class);
	private final Map<GearLevel, TierConfig> tiers = new EnumMap<>(GearLevel.class);
	@Nullable
	private final MinecraftServer server;

	private SoldierConfig(@Nullable MinecraftServer server) {
		this.server = server;
		this.resetWeightsToDefaults();
	}

	public static void register() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> instance = load(server));
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> instance = new SoldierConfig(null));
	}

	public static SoldierConfig get() {
		return instance;
	}

	public static double defaultWeight(CombatRole role) {
		return switch (role) {
			case VANGUARD -> 38.0;
			case BRUTE -> 26.0;
			case RANGER -> 7.5;
			case TRAPPER -> 14.0;
			case DUELIST -> 3.5;
			case LANCER -> 2.0;
			case MEDIC -> 2.0;
			case ENGINEER -> 2.0;
			case ALCHEMIST -> 1.5;
			case ENDER_SKIRMISHER -> 1.5;
			case DEMOLITIONIST -> 2.0;
		};
	}

	public static String armorKey(EquipmentSlot slot) {
		return switch (slot) {
			case HEAD -> KEY_HELMET;
			case CHEST -> KEY_CHESTPLATE;
			case LEGS -> KEY_LEGGINGS;
			default -> KEY_BOOTS;
		};
	}

	public double weight(CombatRole role) {
		return this.roleWeights.getOrDefault(role, defaultWeight(role));
	}

	public void setWeight(CombatRole role, double value) {
		double clamped = Math.max(0.0, Math.min(100.0, Math.round(value * 10.0) / 10.0));
		this.roleWeights.put(role, clamped);
		this.save();
	}

	public double totalWeight() {
		double total = 0.0;
		for (CombatRole role : CombatRole.values()) {
			total += this.weight(role);
		}
		return total;
	}

	/** Share of spawns for this role when every role is eligible, as a percentage. */
	public double effectiveShare(CombatRole role) {
		double total = this.totalWeight();
		return total <= 0.0 ? 0.0 : this.weight(role) / total * 100.0;
	}

	public void resetWeightsToDefaults() {
		for (CombatRole role : CombatRole.values()) {
			this.roleWeights.put(role, defaultWeight(role));
		}
	}

	public void resetWeightsAndSave() {
		this.resetWeightsToDefaults();
		this.save();
	}

	public Optional<ItemStack> gearOverride(GearLevel gear, String key) {
		TierConfig tier = this.tiers.get(gear);
		if (tier == null) {
			return Optional.empty();
		}
		ItemStack stack = tier.gear.get(key);
		return stack == null || stack.isEmpty() ? Optional.empty() : Optional.of(stack);
	}

	/** Copy of the override for direct equipping, or {@link ItemStack#EMPTY} when unset. */
	public ItemStack gearOverrideCopy(GearLevel gear, String key) {
		return this.gearOverride(gear, key).map(ItemStack::copy).orElse(ItemStack.EMPTY);
	}

	public void setGearOverride(GearLevel gear, String key, ItemStack stack) {
		if (stack.isEmpty()) {
			this.clearGearOverride(gear, key);
			return;
		}
		this.tiers.computeIfAbsent(gear, ignored -> new TierConfig()).gear.put(key, stack.copy());
		this.save();
	}

	public void clearGearOverride(GearLevel gear, String key) {
		TierConfig tier = this.tiers.get(gear);
		if (tier != null) {
			tier.gear.remove(key);
			this.pruneTier(gear, tier);
			this.save();
		}
	}

	public OptionalInt supplyOverride(GearLevel gear, String key) {
		TierConfig tier = this.tiers.get(gear);
		if (tier == null) {
			return OptionalInt.empty();
		}
		Integer value = tier.supplies.get(key);
		return value == null ? OptionalInt.empty() : OptionalInt.of(value);
	}

	public int supplyOr(GearLevel gear, String key, int fallback) {
		return this.supplyOverride(gear, key).orElse(fallback);
	}

	public void setSupplyOverride(GearLevel gear, String key, int value) {
		int max = SUPPLY_SHIELD_CHANCE.equals(key) || SUPPLY_POTION_CHANCE.equals(key) ? 100 : 64;
		int clamped = Math.max(0, Math.min(max, value));
		this.tiers.computeIfAbsent(gear, ignored -> new TierConfig()).supplies.put(key, clamped);
		this.save();
	}

	public void clearSupplyOverride(GearLevel gear, String key) {
		TierConfig tier = this.tiers.get(gear);
		if (tier != null) {
			tier.supplies.remove(key);
			this.pruneTier(gear, tier);
			this.save();
		}
	}

	public void clearTier(GearLevel gear) {
		this.tiers.remove(gear);
		this.save();
	}

	public int tierOverrideCount(GearLevel gear) {
		TierConfig tier = this.tiers.get(gear);
		return tier == null ? 0 : tier.gear.size() + tier.supplies.size();
	}

	private void pruneTier(GearLevel gear, TierConfig tier) {
		if (tier.gear.isEmpty() && tier.supplies.isEmpty()) {
			this.tiers.remove(gear);
		}
	}

	public void save() {
		if (this.server == null) {
			return;
		}
		try {
			JsonObject root = new JsonObject();
			JsonObject weights = new JsonObject();
			for (CombatRole role : CombatRole.values()) {
				weights.addProperty(role.id(), this.weight(role));
			}
			root.add("role_weights", weights);

			DynamicOps<JsonElement> ops =
					this.server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
			JsonObject tiersJson = new JsonObject();
			for (Map.Entry<GearLevel, TierConfig> entry : this.tiers.entrySet()) {
				JsonObject tierJson = new JsonObject();
				JsonObject gearJson = new JsonObject();
				for (Map.Entry<String, ItemStack> gearEntry : entry.getValue().gear.entrySet()) {
					ItemStack.CODEC.encodeStart(ops, gearEntry.getValue())
							.resultOrPartial(error -> BattleSoldiersMod.LOGGER.warn(
									"Skipping unserializable gear override {}: {}", gearEntry.getKey(), error))
							.ifPresent(encoded -> gearJson.add(gearEntry.getKey(), encoded));
				}
				if (!gearJson.isEmpty()) {
					tierJson.add("gear", gearJson);
				}
				JsonObject suppliesJson = new JsonObject();
				for (Map.Entry<String, Integer> supplyEntry : entry.getValue().supplies.entrySet()) {
					suppliesJson.addProperty(supplyEntry.getKey(), supplyEntry.getValue());
				}
				if (!suppliesJson.isEmpty()) {
					tierJson.add("supplies", suppliesJson);
				}
				tiersJson.add(Integer.toString(entry.getKey().id()), tierJson);
			}
			root.add("tiers", tiersJson);

			Path path = configPath();
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(root));
		} catch (IOException | RuntimeException exception) {
			BattleSoldiersMod.LOGGER.error("Failed to save Battle Soldiers config", exception);
		}
	}

	private static SoldierConfig load(MinecraftServer server) {
		SoldierConfig config = new SoldierConfig(server);
		Path path = configPath();
		if (!Files.exists(path)) {
			return config;
		}
		try {
			JsonObject root = JsonParser.parseString(Files.readString(path)).getAsJsonObject();
			if (root.get("role_weights") instanceof JsonObject weights) {
				for (CombatRole role : CombatRole.values()) {
					JsonElement value = weights.get(role.id());
					if (value != null && value.isJsonPrimitive()) {
						double parsed = value.getAsDouble();
						config.roleWeights.put(
								role,
								Math.max(0.0, Math.min(100.0, Math.round(parsed * 10.0) / 10.0))
						);
					}
				}
			}
			DynamicOps<JsonElement> ops =
					server.registryAccess().createSerializationContext(JsonOps.INSTANCE);
			if (root.get("tiers") instanceof JsonObject tiersJson) {
				for (String tierId : tiersJson.keySet()) {
					GearLevel gear = GearLevel.byId(parseTierId(tierId));
					if (!(tiersJson.get(tierId) instanceof JsonObject tierJson)) {
						continue;
					}
					TierConfig tier = config.tiers.computeIfAbsent(gear, ignored -> new TierConfig());
					if (tierJson.get("gear") instanceof JsonObject gearJson) {
						for (String key : gearJson.keySet()) {
							if (!GEAR_KEYS.contains(key)) {
								continue;
							}
							ItemStack.CODEC.parse(ops, gearJson.get(key))
									.resultOrPartial(error -> BattleSoldiersMod.LOGGER.warn(
											"Skipping unreadable gear override {}: {}", key, error))
									.ifPresent(stack -> tier.gear.put(key, stack));
						}
					}
					if (tierJson.get("supplies") instanceof JsonObject suppliesJson) {
						for (String key : suppliesJson.keySet()) {
							if (SUPPLY_KEYS.contains(key) && suppliesJson.get(key).isJsonPrimitive()) {
								tier.supplies.put(key, suppliesJson.get(key).getAsInt());
							}
						}
					}
					config.pruneTier(gear, tier);
				}
			}
		} catch (IOException | RuntimeException exception) {
			BattleSoldiersMod.LOGGER.error("Failed to load Battle Soldiers config; using defaults", exception);
		}
		return config;
	}

	private static int parseTierId(String raw) {
		try {
			return Integer.parseInt(raw.toLowerCase(Locale.ROOT).trim());
		} catch (NumberFormatException ignored) {
			return 1;
		}
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	private static final class TierConfig {
		final Map<String, ItemStack> gear = new LinkedHashMap<>();
		final Map<String, Integer> supplies = new LinkedHashMap<>();
	}
}

package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

public final class TierLoadoutMenu extends SoldierMenu {
	private static final int SLOT_HEADER = 4;
	private static final int GEAR_ROW_START = 9;
	private static final int SUPPLY_ROW_START = 27;
	private static final int SLOT_BACK = 45;
	private static final int SLOT_RESET = 49;
	private static final int SLOT_CLOSE = 53;

	private final GearLevel tier;

	public TierLoadoutMenu(int syncId, Inventory playerInventory, GearLevel tier) {
		super(syncId, playerInventory, 6);
		this.tier = tier;
		this.refresh();
	}

	private record SupplyEntry(String key, Item icon, String label, boolean chance) {
	}

	private List<SupplyEntry> supplyEntries() {
		return List.of(
				new SupplyEntry(SoldierConfig.SUPPLY_GOLDEN_APPLES, Items.GOLDEN_APPLE, "Golden Apples", false),
				new SupplyEntry(SoldierConfig.SUPPLY_ENCHANTED_GOLDEN_APPLES, Items.ENCHANTED_GOLDEN_APPLE, "Enchanted Golden Apples", false),
				new SupplyEntry(SoldierConfig.SUPPLY_COBWEBS, Items.COBWEB, "Trapper Web Reserve", false),
				new SupplyEntry(SoldierConfig.SUPPLY_ARROWS, Items.ARROW, "Ranger Arrows", false),
				new SupplyEntry(SoldierConfig.SUPPLY_BUILDING_BLOCKS, Items.COBBLESTONE, "Building Blocks", false),
				new SupplyEntry(SoldierConfig.SUPPLY_TOTEMS, Items.TOTEM_OF_UNDYING, "Totems of Undying", false),
				new SupplyEntry(SoldierConfig.SUPPLY_SHIELD_CHANCE, Items.SHIELD, "Vanguard Shield Chance", true),
				new SupplyEntry(SoldierConfig.SUPPLY_POTION_CHANCE, Items.BREWING_STAND, "Utility Potion Chance", true)
		);
	}

	private String supplyDefaultText(String key) {
		int tierId = this.tier.id();
		return switch (key) {
			case SoldierConfig.SUPPLY_GOLDEN_APPLES -> tierId == 6 ? "5-7" : "2-3";
			case SoldierConfig.SUPPLY_ENCHANTED_GOLDEN_APPLES -> tierId == 6 ? "1-2" : "0";
			case SoldierConfig.SUPPLY_COBWEBS -> switch (tierId) {
				case 4 -> "6";
				case 5 -> "10";
				case 6 -> "14";
				default -> "4 (Trappers spawn at gear 4+)";
			};
			case SoldierConfig.SUPPLY_ARROWS -> "14-28 start, refills to 32";
			case SoldierConfig.SUPPLY_BUILDING_BLOCKS -> "role-based (" + (2 + tierId) + "-" + (18 + tierId * 3) + ")";
			case SoldierConfig.SUPPLY_TOTEMS -> switch (tierId) {
				case 4 -> "4% chance of 1";
				case 5 -> "8% chance of 1";
				case 6 -> "2-3 guaranteed";
				default -> "0";
			};
			case SoldierConfig.SUPPLY_SHIELD_CHANCE -> Math.round(this.tier.shieldChance() * 100.0F) + "%";
			case SoldierConfig.SUPPLY_POTION_CHANCE -> Math.round(this.tier.potionChance() * 100.0F) + "%";
			default -> "";
		};
	}

	private int supplyBaseline(String key) {
		int tierId = this.tier.id();
		return switch (key) {
			case SoldierConfig.SUPPLY_GOLDEN_APPLES -> tierId == 6 ? 6 : 2;
			case SoldierConfig.SUPPLY_ENCHANTED_GOLDEN_APPLES -> tierId == 6 ? 1 : 0;
			case SoldierConfig.SUPPLY_COBWEBS -> switch (tierId) {
				case 4 -> 6;
				case 5 -> 10;
				case 6 -> 14;
				default -> 4;
			};
			case SoldierConfig.SUPPLY_ARROWS -> 32;
			case SoldierConfig.SUPPLY_BUILDING_BLOCKS -> 12;
			case SoldierConfig.SUPPLY_TOTEMS -> tierId == 6 ? 2 : tierId >= 4 ? 1 : 0;
			case SoldierConfig.SUPPLY_SHIELD_CHANCE -> Math.round(this.tier.shieldChance() * 100.0F);
			case SoldierConfig.SUPPLY_POTION_CHANCE -> Math.round(this.tier.potionChance() * 100.0F);
			default -> 0;
		};
	}

	private String supplyDescription(String key) {
		return switch (key) {
			case SoldierConfig.SUPPLY_GOLDEN_APPLES -> "Healing gaps every soldier carries.";
			case SoldierConfig.SUPPLY_ENCHANTED_GOLDEN_APPLES -> "Emergency god apples.";
			case SoldierConfig.SUPPLY_COBWEBS -> "Trapper web stock (initial + refill floor). Non-Trappers always carry 3-5.";
			case SoldierConfig.SUPPLY_ARROWS -> "Ranger arrow stock (initial + refill floor).";
			case SoldierConfig.SUPPLY_BUILDING_BLOCKS -> "Exact block total for every role (towers, bridges, cover).";
			case SoldierConfig.SUPPLY_TOTEMS -> "Guaranteed totem count, replacing the tier's chance roll.";
			case SoldierConfig.SUPPLY_SHIELD_CHANCE -> "Chance a Vanguard spawns with an offhand shield.";
			case SoldierConfig.SUPPLY_POTION_CHANCE -> "Chance of a bonus utility potion (strength, speed, etc).";
			default -> "";
		};
	}

	@Override
	protected void refresh() {
		this.fillWith(filler());
		SoldierConfig config = SoldierConfig.get();

		int overrides = config.tierOverrideCount(this.tier);
		this.setSlot(SLOT_HEADER, button(
				SoldierMenus.tierIcon(this.tier),
				name("Tier " + this.tier.id() + " Loadout", ChatFormatting.AQUA),
				List.of(
						loreLine("Top row: gear pieces. Click one to"),
						loreLine("edit its material and enchantments."),
						loreLine("Middle row: supply counts and chances."),
						loreLine(""),
						loreLine("Overridden entries apply exactly, to"),
						loreLine("every tier-" + this.tier.id() + " soldier that uses them."),
						loreLine(""),
						loreLine(overrides + " active override" + (overrides == 1 ? "" : "s") + ".",
								overrides == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN)
				)
		));

		List<String> gearKeys = SoldierConfig.GEAR_KEYS;
		for (int index = 0; index < gearKeys.size(); index++) {
			this.setSlot(GEAR_ROW_START + index, this.gearEntry(config, gearKeys.get(index)));
		}

		List<SupplyEntry> supplies = this.supplyEntries();
		for (int index = 0; index < supplies.size(); index++) {
			this.setSlot(SUPPLY_ROW_START + index, this.supplyDisplay(config, supplies.get(index)));
		}

		this.setSlot(SLOT_BACK, backButton());
		this.setSlot(SLOT_RESET, button(
				Items.MILK_BUCKET,
				name("Reset tier " + this.tier.id() + " to defaults", ChatFormatting.RED),
				List.of(
						loreLine("Removes every gear and supply"),
						loreLine("override on this tier."),
						loreLine(""),
						loreLine("Shift-click to confirm.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	private ItemStack gearEntry(SoldierConfig config, String key) {
		ItemStack override = config.gearOverrideCopy(this.tier, key);
		boolean overridden = !override.isEmpty();
		ItemStack display = overridden ? override : SoldierMenus.defaultGearStack(this.tier, key);
		List<Component> lore = new ArrayList<>();
		lore.add(loreLine(SoldierMenus.gearKeyLabel(key) + " — "
				+ (overridden ? "OVERRIDE" : "Default"),
				overridden ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY));
		lore.add(loreLine(SoldierMenus.gearKeyUsage(key), ChatFormatting.GRAY));
		if (!overridden) {
			if (this.tier == GearLevel.SIX) {
				lore.add(loreLine("Tier 6 soldiers auto-enchant defaults.", ChatFormatting.DARK_GRAY));
			}
			if (SoldierConfig.KEY_BOW.equals(key)) {
				lore.add(loreLine("Default Ranger bows are always Power V.", ChatFormatting.DARK_GRAY));
			}
			lore.add(loreLine("Armor/weapons spawn with random wear.", ChatFormatting.DARK_GRAY));
		} else {
			lore.add(loreLine("Every soldier gets this exact item.", ChatFormatting.GRAY));
		}
		lore.add(loreLine(""));
		lore.add(loreLine("Left-click: edit item + enchants", ChatFormatting.YELLOW));
		if (overridden) {
			lore.add(loreLine("Right-click: remove override", ChatFormatting.YELLOW));
		}
		return withLore(display, lore);
	}

	private ItemStack supplyDisplay(SoldierConfig config, SupplyEntry entry) {
		OptionalInt override = config.supplyOverride(this.tier, entry.key());
		boolean overridden = override.isPresent();
		String valueText = overridden
				? override.getAsInt() + (entry.chance() ? "%" : "")
				: "default (" + this.supplyDefaultText(entry.key()) + ")";
		List<Component> lore = new ArrayList<>();
		lore.addAll(wrapLore(this.supplyDescription(entry.key()), ChatFormatting.GRAY));
		lore.add(loreLine(""));
		lore.add(loreLine(
				overridden ? "Override: " + valueText : "Using " + valueText,
				overridden ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY
		));
		int step = entry.chance() ? 5 : 1;
		int bigStep = entry.chance() ? 25 : 10;
		lore.add(loreLine(""));
		lore.add(loreLine("Left: +" + step + "   Right: −" + step, ChatFormatting.YELLOW));
		lore.add(loreLine("Shift+Left: +" + bigStep + "   Shift+Right: reset", ChatFormatting.YELLOW));

		ItemStack display = new ItemStack(entry.icon());
		if (overridden && !entry.chance()) {
			display.setCount(Math.max(1, Math.min(64, override.getAsInt())));
		}
		return button(
				display,
				name(
						entry.label() + ": " + (overridden
								? override.getAsInt() + (entry.chance() ? "%" : "")
								: "default"),
						overridden ? ChatFormatting.GREEN : ChatFormatting.WHITE
				),
				lore
		);
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		SoldierConfig config = SoldierConfig.get();
		if (slot >= GEAR_ROW_START && slot < GEAR_ROW_START + SoldierConfig.GEAR_KEYS.size()) {
			String key = SoldierConfig.GEAR_KEYS.get(slot - GEAR_ROW_START);
			if (rightClick) {
				config.clearGearOverride(this.tier, key);
				this.refresh();
			} else {
				GearLevel tier = this.tier;
				this.navigate(
						"Soldiers › Tier " + tier.id() + " › " + SoldierMenus.gearKeyLabel(key),
						(syncId, inventory) -> new ItemEditMenu(syncId, inventory, tier, key)
				);
			}
			return;
		}
		List<SupplyEntry> supplies = this.supplyEntries();
		if (slot >= SUPPLY_ROW_START && slot < SUPPLY_ROW_START + supplies.size()) {
			SupplyEntry entry = supplies.get(slot - SUPPLY_ROW_START);
			if (shift && rightClick) {
				config.clearSupplyOverride(this.tier, entry.key());
			} else {
				int step = entry.chance() ? 5 : 1;
				if (shift) {
					step = entry.chance() ? 25 : 10;
				}
				int current = config.supplyOverride(this.tier, entry.key())
						.orElse(this.supplyBaseline(entry.key()));
				config.setSupplyOverride(this.tier, entry.key(), current + (rightClick ? -step : step));
			}
			this.refresh();
			return;
		}
		switch (slot) {
			case SLOT_BACK -> this.navigate("Soldiers › Tier Loadouts", TierSelectMenu::new);
			case SLOT_RESET -> {
				if (shift) {
					config.clearTier(this.tier);
					this.refresh();
				}
			}
			case SLOT_CLOSE -> this.closeMenu();
			default -> {
			}
		}
	}
}

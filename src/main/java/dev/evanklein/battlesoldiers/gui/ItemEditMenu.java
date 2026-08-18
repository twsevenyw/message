package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PvP-Legacy-style item editor: the gear piece sits up top, every applicable
 * enchantment is laid out as leveled books below it, and clicking a book
 * applies (or removes) exactly that level. Arrows cycle the item material.
 */
public final class ItemEditMenu extends SoldierMenu {
	private static final int SLOT_PREV_MATERIAL = 0;
	private static final int SLOT_ITEM = 4;
	private static final int SLOT_NEXT_MATERIAL = 8;
	private static final int BOOKS_START = 9;
	private static final int BOOKS_END = 44;
	private static final int SLOT_BACK = 45;
	private static final int SLOT_CLEAR_ENCHANTS = 47;
	private static final int SLOT_REMOVE_OVERRIDE = 51;
	private static final int SLOT_CLOSE = 53;

	private final GearLevel tier;
	private final String gearKey;
	private final Map<Integer, BookEntry> bookSlots = new HashMap<>();

	private record BookEntry(Holder<Enchantment> enchantment, int level) {
	}

	public ItemEditMenu(int syncId, Inventory playerInventory, GearLevel tier, String gearKey) {
		super(syncId, playerInventory, 6);
		this.tier = tier;
		this.gearKey = gearKey;
		this.refresh();
	}

	private ItemStack currentStack() {
		ItemStack override = SoldierConfig.get().gearOverrideCopy(this.tier, this.gearKey);
		return override.isEmpty() ? SoldierMenus.defaultGearStack(this.tier, this.gearKey) : override;
	}

	private boolean hasOverride() {
		return SoldierConfig.get().gearOverride(this.tier, this.gearKey).isPresent();
	}

	private List<Item> materialCycle() {
		return switch (this.gearKey) {
			case SoldierConfig.KEY_HELMET -> List.of(
					Items.LEATHER_HELMET, Items.CHAINMAIL_HELMET, Items.COPPER_HELMET,
					Items.GOLDEN_HELMET, Items.IRON_HELMET, Items.DIAMOND_HELMET, Items.NETHERITE_HELMET
			);
			case SoldierConfig.KEY_CHESTPLATE -> List.of(
					Items.LEATHER_CHESTPLATE, Items.CHAINMAIL_CHESTPLATE, Items.COPPER_CHESTPLATE,
					Items.GOLDEN_CHESTPLATE, Items.IRON_CHESTPLATE, Items.DIAMOND_CHESTPLATE,
					Items.NETHERITE_CHESTPLATE
			);
			case SoldierConfig.KEY_LEGGINGS -> List.of(
					Items.LEATHER_LEGGINGS, Items.CHAINMAIL_LEGGINGS, Items.COPPER_LEGGINGS,
					Items.GOLDEN_LEGGINGS, Items.IRON_LEGGINGS, Items.DIAMOND_LEGGINGS,
					Items.NETHERITE_LEGGINGS
			);
			case SoldierConfig.KEY_BOOTS -> List.of(
					Items.LEATHER_BOOTS, Items.CHAINMAIL_BOOTS, Items.COPPER_BOOTS,
					Items.GOLDEN_BOOTS, Items.IRON_BOOTS, Items.DIAMOND_BOOTS, Items.NETHERITE_BOOTS
			);
			case SoldierConfig.KEY_SWORD -> List.of(
					Items.WOODEN_SWORD, Items.STONE_SWORD, Items.COPPER_SWORD, Items.GOLDEN_SWORD,
					Items.IRON_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD
			);
			case SoldierConfig.KEY_AXE -> List.of(
					Items.WOODEN_AXE, Items.STONE_AXE, Items.COPPER_AXE, Items.GOLDEN_AXE,
					Items.IRON_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE
			);
			case SoldierConfig.KEY_SPEAR -> List.of(
					Items.WOODEN_SPEAR, Items.STONE_SPEAR, Items.COPPER_SPEAR, Items.GOLDEN_SPEAR,
					Items.IRON_SPEAR, Items.DIAMOND_SPEAR, Items.NETHERITE_SPEAR
			);
			default -> List.of();
		};
	}

	@Override
	protected void refresh() {
		this.bookSlots.clear();
		this.fillWith(filler());
		ItemStack current = this.currentStack();
		boolean overridden = this.hasOverride();

		List<Component> itemLore = new ArrayList<>();
		itemLore.add(loreLine(
				overridden
						? "OVERRIDE — every tier-" + this.tier.id() + " soldier gets this exact item."
						: "Default item — click a book below to customize.",
				overridden ? ChatFormatting.GREEN : ChatFormatting.DARK_GRAY
		));
		itemLore.add(loreLine("Click an enchanted book to apply that", ChatFormatting.GRAY));
		itemLore.add(loreLine("level; click it again to remove it.", ChatFormatting.GRAY));
		this.setSlot(SLOT_ITEM, withLore(current.copy(), itemLore));

		if (!this.materialCycle().isEmpty()) {
			this.setSlot(SLOT_PREV_MATERIAL, button(
					Items.RED_CONCRETE,
					name("◀ Previous material", ChatFormatting.YELLOW),
					List.of(loreLine("Cycle the item material down."), loreLine("Enchantments are kept."))
			));
			this.setSlot(SLOT_NEXT_MATERIAL, button(
					Items.LIME_CONCRETE,
					name("Next material ▶", ChatFormatting.YELLOW),
					List.of(loreLine("Cycle the item material up."), loreLine("Enchantments are kept."))
			));
		}

		int slot = BOOKS_START;
		var enchantments = this.player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		var currentEnchants = current.getEnchantments();
		for (Holder.Reference<Enchantment> holder : enchantments.listElements().toList()) {
			if (holder.is(EnchantmentTags.CURSE) || !holder.value().canEnchant(current)) {
				continue;
			}
			int activeLevel = currentEnchants.getLevel(holder);
			int maxLevel = holder.value().getMaxLevel();
			for (int level = 1; level <= maxLevel && slot <= BOOKS_END; level++) {
				boolean active = activeLevel == level;
				ItemStack book = new ItemStack(Items.ENCHANTED_BOOK, level);
				Component bookName = Component.empty()
						.append(Enchantment.getFullname(holder, level))
						.copy()
						.withStyle(style -> style.withItalic(false));
				if (active) {
					bookName = Component.literal("✔ ")
							.withStyle(style -> style.withItalic(false))
							.withStyle(ChatFormatting.GREEN)
							.append(bookName);
				}
				this.setSlot(slot, button(
						book,
						bookName,
						List.of(loreLine(
								active ? "ACTIVE — click to remove." : "Click to apply this level.",
								active ? ChatFormatting.GREEN : ChatFormatting.YELLOW
						))
				));
				this.bookSlots.put(slot, new BookEntry(holder, level));
				slot++;
			}
		}

		this.setSlot(SLOT_BACK, backButton());
		this.setSlot(SLOT_CLEAR_ENCHANTS, button(
				Items.GRINDSTONE,
				name("Clear enchantments", ChatFormatting.RED),
				List.of(loreLine("Removes every enchantment from this item."))
		));
		this.setSlot(SLOT_REMOVE_OVERRIDE, button(
				Items.MILK_BUCKET,
				name("Remove override", ChatFormatting.RED),
				List.of(
						loreLine("Deletes this override so the tier"),
						loreLine("goes back to its default item.")
				)
		));
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		SoldierConfig config = SoldierConfig.get();
		BookEntry entry = this.bookSlots.get(slot);
		if (entry != null) {
			ItemStack updated = this.currentStack();
			int activeLevel = updated.getEnchantments().getLevel(entry.enchantment());
			EnchantmentHelper.updateEnchantments(updated, mutable -> {
				if (activeLevel == entry.level()) {
					mutable.removeIf(holder -> holder.equals(entry.enchantment()));
				} else {
					mutable.set(entry.enchantment(), entry.level());
				}
			});
			config.setGearOverride(this.tier, this.gearKey, updated);
			this.refresh();
			return;
		}
		switch (slot) {
			case SLOT_PREV_MATERIAL, SLOT_NEXT_MATERIAL -> {
				List<Item> cycle = this.materialCycle();
				if (cycle.isEmpty()) {
					return;
				}
				ItemStack current = this.currentStack();
				int index = cycle.indexOf(current.getItem());
				int step = slot == SLOT_NEXT_MATERIAL ? 1 : -1;
				int next = index < 0 ? 0 : Math.floorMod(index + step, cycle.size());
				ItemStack cycled = current.transmuteCopy(cycle.get(next));
				if (cycled.isDamageableItem()) {
					cycled.setDamageValue(0);
				}
				config.setGearOverride(this.tier, this.gearKey, cycled);
				this.refresh();
			}
			case SLOT_CLEAR_ENCHANTS -> {
				ItemStack cleared = this.currentStack();
				EnchantmentHelper.updateEnchantments(cleared, mutable -> mutable.removeIf(ignored -> true));
				config.setGearOverride(this.tier, this.gearKey, cleared);
				this.refresh();
			}
			case SLOT_REMOVE_OVERRIDE -> {
				config.clearGearOverride(this.tier, this.gearKey);
				this.refresh();
			}
			case SLOT_BACK -> {
				GearLevel tier = this.tier;
				this.navigate(
						"Soldiers › Tier " + tier.id() + " Loadout",
						(syncId, inventory) -> new TierLoadoutMenu(syncId, inventory, tier)
				);
			}
			case SLOT_CLOSE -> this.closeMenu();
			default -> {
			}
		}
	}
}

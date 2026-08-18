package dev.evanklein.battlesoldiers.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

import java.util.List;

public final class MainMenu extends SoldierMenu {
	private static final int SLOT_CLASSES = 10;
	private static final int SLOT_WEIGHTS = 12;
	private static final int SLOT_TIERS = 14;
	private static final int SLOT_SOLDIERS = 16;
	private static final int SLOT_CLOSE = 22;

	public MainMenu(int syncId, Inventory playerInventory) {
		super(syncId, playerInventory, 3);
		this.refresh();
	}

	@Override
	protected void refresh() {
		this.fillWith(filler());
		this.setSlot(SLOT_CLASSES, button(
				Items.WRITTEN_BOOK,
				name("Class Guide", ChatFormatting.GOLD),
				List.of(
						loreLine("Detailed breakdown of every combat"),
						loreLine("class: tactics, stats, and counters."),
						loreLine(""),
						loreLine("Click to open.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_WEIGHTS, button(
				Items.COMPARATOR,
				name("Spawn Chances", ChatFormatting.GOLD),
				List.of(
						loreLine("Edit the spawn percentage of every"),
						loreLine("class, core and specialist."),
						loreLine(""),
						loreLine("Click to open.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_TIERS, button(
				Items.NETHERITE_CHESTPLATE,
				name("Tier Loadouts", ChatFormatting.GOLD),
				List.of(
						loreLine("Customize gear, enchantments, and"),
						loreLine("supply counts for each gear tier 1-6."),
						loreLine(""),
						loreLine("Click to open.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_SOLDIERS, button(
				Items.SPYGLASS,
				name("Soldier Inspector", ChatFormatting.GOLD),
				List.of(
						loreLine("Open any live soldier's inventory"),
						loreLine("and equipment, updated in real time."),
						loreLine(""),
						loreLine("Click to open.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		switch (slot) {
			case SLOT_CLASSES -> this.navigate("Soldiers › Class Guide", ClassInfoMenu::new);
			case SLOT_WEIGHTS -> this.navigate("Soldiers › Spawn Chances", WeightsMenu::new);
			case SLOT_TIERS -> this.navigate("Soldiers › Tier Loadouts", TierSelectMenu::new);
			case SLOT_SOLDIERS -> this.navigate("Soldiers › Inspector", SoldierListMenu::new);
			case SLOT_CLOSE -> this.closeMenu();
			default -> {
			}
		}
	}
}

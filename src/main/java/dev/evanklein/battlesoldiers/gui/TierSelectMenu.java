package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class TierSelectMenu extends SoldierMenu {
	private static final int SLOT_HEADER = 4;
	private static final int FIRST_TIER_SLOT = 10;
	private static final int SLOT_BACK = 18;
	private static final int SLOT_CLOSE = 26;

	public TierSelectMenu(int syncId, Inventory playerInventory) {
		super(syncId, playerInventory, 3);
		this.refresh();
	}

	@Override
	protected void refresh() {
		this.fillWith(filler());
		this.setSlot(SLOT_HEADER, button(
				Items.SMITHING_TABLE,
				name("Tier Loadouts", ChatFormatting.GOLD),
				List.of(
						loreLine("Pick a gear tier to customize its"),
						loreLine("armor, weapons, enchantments, and"),
						loreLine("supply counts.")
				)
		));
		SoldierConfig config = SoldierConfig.get();
		for (GearLevel tier : GearLevel.values()) {
			int overrides = config.tierOverrideCount(tier);
			List<net.minecraft.network.chat.Component> lore = new ArrayList<>();
			lore.add(loreLine(switch (tier) {
				case ONE -> "Leather / wood baseline kit.";
				case TWO -> "Chainmail / stone kit.";
				case THREE -> "Iron kit.";
				case FOUR -> "Diamond kit, webs and totems unlock.";
				case FIVE -> "Netherite kit.";
				case SIX -> "Fully enchanted netherite endgame kit.";
			}));
			lore.add(loreLine(
					overrides == 0
							? "No overrides — using default loadout."
							: overrides + " active override" + (overrides == 1 ? "" : "s") + ".",
					overrides == 0 ? ChatFormatting.DARK_GRAY : ChatFormatting.GREEN
			));
			lore.add(loreLine(""));
			lore.add(loreLine("Click to edit.", ChatFormatting.YELLOW));
			this.setSlot(FIRST_TIER_SLOT + tier.id() - 1, button(
					SoldierMenus.tierIcon(tier),
					name("Gear Tier " + tier.id(), ChatFormatting.AQUA),
					lore
			));
		}
		this.setSlot(SLOT_BACK, backButton());
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		if (slot >= FIRST_TIER_SLOT && slot < FIRST_TIER_SLOT + 6) {
			GearLevel tier = GearLevel.byId(slot - FIRST_TIER_SLOT + 1);
			this.navigate(
					"Soldiers › Tier " + tier.id() + " Loadout",
					(syncId, inventory) -> new TierLoadoutMenu(syncId, inventory, tier)
			);
			return;
		}
		if (slot == SLOT_BACK) {
			this.navigate("Battle Soldiers", MainMenu::new);
		} else if (slot == SLOT_CLOSE) {
			this.closeMenu();
		}
	}
}

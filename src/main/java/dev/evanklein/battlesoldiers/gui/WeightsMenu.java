package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.ClassInfo;
import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class WeightsMenu extends SoldierMenu {
	private static final int SLOT_HEADER = 4;
	private static final int SLOT_BACK = 45;
	private static final int SLOT_RESET = 49;
	private static final int SLOT_CLOSE = 53;
	private static final int[] CORE_SLOTS = {20, 21, 22, 23};
	private static final int[] SPECIALIST_SLOTS = {37, 38, 39, 40, 41, 42, 43};

	private final Map<Integer, CombatRole> slotRoles = new HashMap<>();

	public WeightsMenu(int syncId, Inventory playerInventory) {
		super(syncId, playerInventory, 6);
		this.refresh();
	}

	@Override
	protected void refresh() {
		this.slotRoles.clear();
		this.fillWith(filler());
		SoldierConfig config = SoldierConfig.get();
		this.setSlot(SLOT_HEADER, button(
				Items.COMPARATOR,
				name("Spawn Chances", ChatFormatting.GOLD),
				List.of(
						loreLine("Weights are relative; the share column"),
						loreLine("shows the spawn % when every class"),
						loreLine("is eligible at the spawned gear tier."),
						loreLine("Ineligible classes redistribute their"),
						loreLine("share to the remaining classes."),
						loreLine(""),
						loreLine("Total weight: "
								+ ClassInfo.formatWeight(config.totalWeight()), ChatFormatting.AQUA),
						loreLine(""),
						loreLine("Left-click: +0.5   Right-click: −0.5", ChatFormatting.YELLOW),
						loreLine("Shift: ±5.0", ChatFormatting.YELLOW)
				)
		));

		int coreIndex = 0;
		int specialistIndex = 0;
		for (CombatRole role : CombatRole.values()) {
			int slot = role.isSpecialist()
					? SPECIALIST_SLOTS[specialistIndex++]
					: CORE_SLOTS[coreIndex++];
			this.slotRoles.put(slot, role);

			ClassInfo info = ClassInfo.of(role);
			List<Component> lore = new ArrayList<>();
			lore.add(loreLine(info.tagline(), ChatFormatting.DARK_GRAY));
			lore.add(loreLine(
					"Weight: " + ClassInfo.formatWeight(config.weight(role)),
					ChatFormatting.AQUA
			));
			lore.add(loreLine(
					"Share when all eligible: "
							+ ClassInfo.formatWeight(config.effectiveShare(role)) + "%",
					ChatFormatting.GREEN
			));
			lore.add(loreLine(info.availability(), ChatFormatting.DARK_GRAY));
			lore.add(loreLine(""));
			lore.add(loreLine("Left: +0.5  Right: −0.5  Shift: ±5", ChatFormatting.YELLOW));
			this.setSlot(slot, button(
					info.iconStack(),
					name(
							role.displayName() + " — "
									+ ClassInfo.formatWeight(config.effectiveShare(role)) + "%",
							role.nameColor(ChatFormatting.WHITE)
					),
					lore
			));
		}

		this.setSlot(SLOT_BACK, backButton());
		this.setSlot(SLOT_RESET, button(
				Items.MILK_BUCKET,
				name("Reset to defaults", ChatFormatting.RED),
				List.of(
						loreLine("Restores the default weights"),
						loreLine("(Ranger 7.5, Vanguard 38, Brute 26,"),
						loreLine("Trapper 14, specialists 1.5-3.5)."),
						loreLine(""),
						loreLine("Shift-click to confirm.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		CombatRole role = this.slotRoles.get(slot);
		SoldierConfig config = SoldierConfig.get();
		if (role != null) {
			double step = shift ? 5.0 : 0.5;
			double delta = rightClick ? -step : step;
			config.setWeight(role, config.weight(role) + delta);
			this.refresh();
			return;
		}
		switch (slot) {
			case SLOT_BACK -> this.navigate("Battle Soldiers", MainMenu::new);
			case SLOT_RESET -> {
				if (shift) {
					config.resetWeightsAndSave();
					this.refresh();
				}
			}
			case SLOT_CLOSE -> this.closeMenu();
			default -> {
			}
		}
	}
}

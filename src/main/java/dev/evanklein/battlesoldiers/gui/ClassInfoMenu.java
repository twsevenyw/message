package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.ClassInfo;
import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClassInfoMenu extends SoldierMenu {
	private static final int SLOT_HEADER = 4;
	private static final int SLOT_BACK = 45;
	private static final int SLOT_CLOSE = 53;
	private static final int[] CORE_SLOTS = {20, 21, 22, 23};
	private static final int[] SPECIALIST_SLOTS = {37, 38, 39, 40, 41, 42, 43};

	private final Map<Integer, CombatRole> slotRoles = new HashMap<>();

	public ClassInfoMenu(int syncId, Inventory playerInventory) {
		super(syncId, playerInventory, 6);
		this.refresh();
	}

	@Override
	protected void refresh() {
		this.slotRoles.clear();
		this.fillWith(filler());
		this.setSlot(SLOT_HEADER, button(
				Items.WRITTEN_BOOK,
				name("Class Guide", ChatFormatting.GOLD),
				List.of(
						loreLine("Top row: core classes (squad majority)."),
						loreLine("Bottom row: rare specialists, capped at"),
						loreLine("20% of a squad and gated by gear tier."),
						loreLine(""),
						loreLine("Squads of 1-2 only roll Vanguard,"),
						loreLine("Brute, or Duelist."),
						loreLine(""),
						loreLine("Click a class for a full chat breakdown.", ChatFormatting.YELLOW)
				)
		));

		int coreIndex = 0;
		int specialistIndex = 0;
		for (CombatRole role : CombatRole.values()) {
			int slot;
			if (role.isSpecialist()) {
				slot = SPECIALIST_SLOTS[specialistIndex++];
			} else {
				slot = CORE_SLOTS[coreIndex++];
			}
			this.slotRoles.put(slot, role);
			this.setSlot(slot, roleEntry(role));
		}
		this.setSlot(SLOT_BACK, backButton());
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	static ItemStack roleEntry(CombatRole role) {
		ClassInfo info = ClassInfo.of(role);
		SoldierConfig config = SoldierConfig.get();
		List<Component> lore = new ArrayList<>();
		lore.add(loreLine(info.tagline(), ChatFormatting.GRAY));
		lore.add(loreLine(info.stats(), ChatFormatting.DARK_AQUA));
		lore.add(loreLine(info.availability(), ChatFormatting.DARK_GRAY));
		lore.add(loreLine(
				"Spawn share: " + ClassInfo.formatWeight(config.effectiveShare(role)) + "%",
				ChatFormatting.GREEN
		));
		lore.add(loreLine(""));
		lore.add(loreLine("Click for the full breakdown in chat.", ChatFormatting.YELLOW));
		return button(
				info.iconStack(),
				name(role.displayName(), role.nameColor(ChatFormatting.WHITE)),
				lore
		);
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		CombatRole role = this.slotRoles.get(slot);
		if (role != null) {
			for (Component line : ClassInfo.detailLines(role)) {
				this.player.sendSystemMessage(line);
			}
			return;
		}
		if (slot == SLOT_BACK) {
			this.navigate("Battle Soldiers", MainMenu::new);
		} else if (slot == SLOT_CLOSE) {
			this.closeMenu();
		}
	}
}

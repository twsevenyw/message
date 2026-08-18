package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.ClassInfo;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import dev.evanklein.battlesoldiers.entity.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class SoldierListMenu extends SoldierMenu {
	private static final int ENTRIES_PER_PAGE = 45;
	private static final int SLOT_BACK = 45;
	private static final int SLOT_PREV = 48;
	private static final int SLOT_INFO = 49;
	private static final int SLOT_NEXT = 50;
	private static final int SLOT_CLOSE = 53;

	private final Map<Integer, UUID> slotSoldiers = new HashMap<>();
	private int page;

	public SoldierListMenu(int syncId, Inventory playerInventory) {
		super(syncId, playerInventory, 6);
		this.refresh();
	}

	private List<BattleSoldierEntity> collectSoldiers() {
		MinecraftServer server = this.player.level().getServer();
		List<BattleSoldierEntity> soldiers = new ArrayList<>();
		if (server == null) {
			return soldiers;
		}
		for (ServerLevel level : server.getAllLevels()) {
			level.getEntities(ModEntities.SOLDIER, Entity::isAlive, soldiers);
		}
		soldiers.sort(Comparator.comparingDouble(soldier ->
				soldier.level() == this.player.level()
						? soldier.distanceToSqr(this.player)
						: Double.MAX_VALUE
		));
		return soldiers;
	}

	@Override
	protected void refresh() {
		this.slotSoldiers.clear();
		this.clearGui();
		List<BattleSoldierEntity> soldiers = this.collectSoldiers();
		int maxPage = Math.max(0, (soldiers.size() - 1) / ENTRIES_PER_PAGE);
		this.page = Math.max(0, Math.min(this.page, maxPage));

		int start = this.page * ENTRIES_PER_PAGE;
		for (int index = 0; index < ENTRIES_PER_PAGE && start + index < soldiers.size(); index++) {
			BattleSoldierEntity soldier = soldiers.get(start + index);
			this.slotSoldiers.put(index, soldier.getUUID());
			this.setSlot(index, this.soldierEntry(soldier));
		}

		for (int slot = SLOT_BACK; slot <= SLOT_CLOSE; slot++) {
			this.setSlot(slot, filler());
		}
		this.setSlot(SLOT_BACK, backButton());
		if (this.page > 0) {
			this.setSlot(SLOT_PREV, button(
					Items.PAPER,
					name("◀ Previous page", ChatFormatting.YELLOW),
					List.of()
			));
		}
		this.setSlot(SLOT_INFO, button(
				Items.SPYGLASS,
				name(soldiers.size() + " active soldier" + (soldiers.size() == 1 ? "" : "s"),
						ChatFormatting.GOLD),
				List.of(
						loreLine("Sorted by distance to you."),
						loreLine("Page " + (this.page + 1) + "/" + (maxPage + 1)),
						loreLine(""),
						loreLine("Click a soldier to open its inventory.", ChatFormatting.YELLOW),
						loreLine("Click here to refresh the list.", ChatFormatting.YELLOW)
				)
		));
		if (this.page < maxPage) {
			this.setSlot(SLOT_NEXT, button(
					Items.PAPER,
					name("Next page ▶", ChatFormatting.YELLOW),
					List.of()
			));
		}
		this.setSlot(SLOT_CLOSE, closeButton());
	}

	private ItemStack soldierEntry(BattleSoldierEntity soldier) {
		List<Component> lore = new ArrayList<>();
		lore.add(loreLine(
				soldier.getSquad().displayName() + " squad — "
						+ soldier.getCombatRole().displayName() + " — Gear "
						+ soldier.getGearLevel().id(),
				ChatFormatting.GRAY
		));
		lore.add(loreLine(
				String.format(Locale.ROOT, "Health: %.1f/%.1f", soldier.getHealth(), soldier.getMaxHealth())
						+ (soldier.getAbsorptionAmount() > 0.0F
								? String.format(Locale.ROOT, " (+%.1f absorption)", soldier.getAbsorptionAmount())
								: ""),
				ChatFormatting.RED
		));
		if (soldier.level() == this.player.level()) {
			lore.add(loreLine(
					String.format(Locale.ROOT, "%.1f blocks away — %s",
							Math.sqrt(soldier.distanceToSqr(this.player)),
							soldier.blockPosition().toShortString()),
					ChatFormatting.DARK_GRAY
			));
		} else {
			lore.add(loreLine("In another dimension", ChatFormatting.DARK_GRAY));
		}
		lore.add(loreLine(""));
		lore.add(loreLine("Click to inspect inventory + gear.", ChatFormatting.YELLOW));

		Component displayName = soldier.getCustomName();
		return button(
				ClassInfo.of(soldier.getCombatRole()).iconStack(),
				displayName != null
						? displayName.copy().withStyle(style -> style.withItalic(false))
						: name(soldier.getCombatRole().displayName(), ChatFormatting.WHITE),
				lore
		);
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		UUID soldierId = this.slotSoldiers.get(slot);
		if (soldierId != null) {
			this.navigate(
					"Soldiers › Inspector",
					(syncId, inventory) -> new SoldierInventoryMenu(syncId, inventory, soldierId)
			);
			return;
		}
		switch (slot) {
			case SLOT_BACK -> this.navigate("Battle Soldiers", MainMenu::new);
			case SLOT_PREV -> {
				this.page = Math.max(0, this.page - 1);
				this.refresh();
			}
			case SLOT_INFO -> this.refresh();
			case SLOT_NEXT -> {
				this.page++;
				this.refresh();
			}
			case SLOT_CLOSE -> this.closeMenu();
			default -> {
			}
		}
	}
}

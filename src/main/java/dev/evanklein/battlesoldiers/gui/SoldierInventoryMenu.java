package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Live read-only view of a soldier's equipment and 36-slot inventory, laid out
 * like a player inventory. Refreshes automatically every second while open.
 */
public final class SoldierInventoryMenu extends SoldierMenu {
	private static final int SLOT_HELMET = 0;
	private static final int SLOT_MAINHAND = 5;
	private static final int SLOT_OFFHAND = 6;
	private static final int SLOT_STATUS = 8;
	private static final int INVENTORY_START = 9;
	private static final int SLOT_BACK = 45;
	private static final int SLOT_REFRESH = 49;
	private static final int SLOT_CLOSE = 53;

	private final UUID soldierId;

	public SoldierInventoryMenu(int syncId, Inventory playerInventory, UUID soldierId) {
		super(syncId, playerInventory, 6);
		this.soldierId = soldierId;
		this.refresh();
	}

	@Nullable
	private BattleSoldierEntity resolveSoldier() {
		MinecraftServer server = this.player.level().getServer();
		if (server == null) {
			return null;
		}
		for (ServerLevel level : server.getAllLevels()) {
			Entity entity = level.getEntity(this.soldierId);
			if (entity instanceof BattleSoldierEntity soldier && soldier.isAlive()) {
				return soldier;
			}
		}
		return null;
	}

	@Override
	public void refresh() {
		this.clearGui();
		BattleSoldierEntity soldier = this.resolveSoldier();
		if (soldier == null) {
			this.fillWith(filler());
			this.setSlot(SLOT_STATUS, button(
					Items.SKELETON_SKULL,
					name("Soldier is dead or unloaded", ChatFormatting.RED),
					List.of(loreLine("Go back to pick another soldier."))
			));
			this.setSlot(SLOT_BACK, backButton());
			this.setSlot(SLOT_CLOSE, closeButton());
			return;
		}

		this.setSlot(4, filler());
		this.setSlot(7, filler());
		this.setSlot(SLOT_HELMET, equipmentView(soldier, EquipmentSlot.HEAD, "Helmet"));
		this.setSlot(SLOT_HELMET + 1, equipmentView(soldier, EquipmentSlot.CHEST, "Chestplate"));
		this.setSlot(SLOT_HELMET + 2, equipmentView(soldier, EquipmentSlot.LEGS, "Leggings"));
		this.setSlot(SLOT_HELMET + 3, equipmentView(soldier, EquipmentSlot.FEET, "Boots"));
		this.setSlot(SLOT_MAINHAND, equipmentView(soldier, EquipmentSlot.MAINHAND, "Main hand"));
		this.setSlot(SLOT_OFFHAND, equipmentView(soldier, EquipmentSlot.OFFHAND, "Offhand"));
		this.setSlot(SLOT_STATUS, this.statusItem(soldier));

		SimpleContainer inventory = soldier.getSoldierInventory();
		for (int index = 0; index < inventory.getContainerSize() && index < 36; index++) {
			ItemStack stack = inventory.getItem(index);
			this.setSlot(INVENTORY_START + index, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
		}

		this.setSlot(SLOT_BACK, backButton());
		this.setSlot(SLOT_REFRESH, button(
				Items.CLOCK,
				name("Live view", ChatFormatting.AQUA),
				List.of(
						loreLine("Auto-refreshes every second."),
						loreLine("Click to refresh right now.", ChatFormatting.YELLOW)
				)
		));
		this.setSlot(SLOT_CLOSE, closeButton());
		for (int slot = SLOT_BACK + 1; slot < SLOT_CLOSE; slot++) {
			if (slot != SLOT_REFRESH) {
				this.setSlot(slot, filler());
			}
		}
	}

	private static ItemStack equipmentView(BattleSoldierEntity soldier, EquipmentSlot slot, String label) {
		ItemStack stack = soldier.getItemBySlot(slot);
		if (stack.isEmpty()) {
			ItemStack empty = filler();
			return button(
					empty,
					name("Empty " + label.toLowerCase(Locale.ROOT) + " slot", ChatFormatting.DARK_GRAY),
					List.of()
			);
		}
		ItemStack copy = stack.copy();
		List<Component> lore = new ArrayList<>();
		lore.add(loreLine("Slot: " + label, ChatFormatting.DARK_AQUA));
		if (copy.isDamageableItem()) {
			lore.add(loreLine(
					"Durability: " + (copy.getMaxDamage() - copy.getDamageValue()) + "/" + copy.getMaxDamage(),
					ChatFormatting.DARK_GRAY
			));
		}
		return withLore(copy, lore);
	}

	private ItemStack statusItem(BattleSoldierEntity soldier) {
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
		if (!soldier.getActiveEffects().isEmpty()) {
			lore.add(loreLine("Effects:", ChatFormatting.LIGHT_PURPLE));
			for (MobEffectInstance effect : soldier.getActiveEffects()) {
				lore.add(loreLine(
						" " + effect.getEffect().value().getDisplayName().getString()
								+ " " + (effect.getAmplifier() + 1)
								+ " (" + effect.getDuration() / 20 + "s)",
						ChatFormatting.LIGHT_PURPLE
				));
			}
		}
		lore.add(loreLine("Position: " + soldier.blockPosition().toShortString(), ChatFormatting.DARK_GRAY));
		lore.add(loreLine(
				"Crits: " + soldier.getCriticalHits()
						+ "  Shield raises: " + soldier.getReactiveShieldUses()
						+ "  Homing shots: " + soldier.getHomingShotsFired(),
				ChatFormatting.DARK_GRAY
		));
		Component displayName = soldier.getCustomName();
		return button(
				Items.NAME_TAG,
				displayName != null
						? displayName.copy().withStyle(style -> style.withItalic(false))
						: name("Battle Soldier", ChatFormatting.WHITE),
				lore
		);
	}

	@Override
	protected void onGuiClick(int slot, boolean rightClick, boolean shift) {
		switch (slot) {
			case SLOT_BACK -> this.navigate("Soldiers › Inspector", SoldierListMenu::new);
			case SLOT_REFRESH -> this.refresh();
			case SLOT_CLOSE -> this.closeMenu();
			default -> {
			}
		}
	}
}

package dev.evanklein.battlesoldiers.gui;

import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Entry points and shared services for the Battle Soldiers GUI suite. */
public final class SoldierMenus {
	private SoldierMenus() {
	}

	public interface Factory {
		SoldierMenu create(int syncId, Inventory playerInventory);
	}

	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.containerMenu instanceof SoldierInventoryMenu menu
						&& server.getTickCount() % 20 == 0) {
					menu.refresh();
				} else if (player.containerMenu instanceof SoldierListMenu menu
						&& server.getTickCount() % 40 == 0) {
					menu.refresh();
				}
			}
		});
	}

	public static void openMain(ServerPlayer player) {
		open(player, "Battle Soldiers", MainMenu::new);
	}

	public static void open(ServerPlayer player, String title, Factory factory) {
		player.openMenu(new SimpleMenuProvider(
				(syncId, inventory, ignored) -> factory.create(syncId, inventory),
				Component.literal(title)
						.withStyle(style -> style.withItalic(false))
						.withStyle(ChatFormatting.DARK_GRAY)
		));
	}

	public static ItemStack tierIcon(GearLevel tier) {
		ItemStack stack = new ItemStack(switch (tier) {
			case ONE -> Items.LEATHER_CHESTPLATE;
			case TWO -> Items.CHAINMAIL_CHESTPLATE;
			case THREE -> Items.IRON_CHESTPLATE;
			case FOUR -> Items.DIAMOND_CHESTPLATE;
			case FIVE, SIX -> Items.NETHERITE_CHESTPLATE;
		});
		stack.setCount(Math.max(1, tier.id()));
		return stack;
	}

	/** Representative default (non-overridden) gear stack for display and as an edit baseline. */
	public static ItemStack defaultGearStack(GearLevel tier, String key) {
		return switch (key) {
			case SoldierConfig.KEY_HELMET -> new ItemStack(tier.armor(EquipmentSlot.HEAD));
			case SoldierConfig.KEY_CHESTPLATE -> new ItemStack(tier.armor(EquipmentSlot.CHEST));
			case SoldierConfig.KEY_LEGGINGS -> new ItemStack(tier.armor(EquipmentSlot.LEGS));
			case SoldierConfig.KEY_BOOTS -> new ItemStack(tier.armor(EquipmentSlot.FEET));
			case SoldierConfig.KEY_SWORD -> new ItemStack(tier.meleeWeapon());
			case SoldierConfig.KEY_AXE -> new ItemStack(tier.axeWeapon());
			case SoldierConfig.KEY_BOW -> new ItemStack(Items.BOW);
			case SoldierConfig.KEY_SHIELD -> new ItemStack(Items.SHIELD);
			case SoldierConfig.KEY_SPEAR -> new ItemStack(tier.spearWeapon());
			default -> ItemStack.EMPTY;
		};
	}

	public static String gearKeyLabel(String key) {
		return switch (key) {
			case SoldierConfig.KEY_HELMET -> "Helmet";
			case SoldierConfig.KEY_CHESTPLATE -> "Chestplate";
			case SoldierConfig.KEY_LEGGINGS -> "Leggings";
			case SoldierConfig.KEY_BOOTS -> "Boots";
			case SoldierConfig.KEY_SWORD -> "Sword";
			case SoldierConfig.KEY_AXE -> "Axe";
			case SoldierConfig.KEY_BOW -> "Bow";
			case SoldierConfig.KEY_SHIELD -> "Shield";
			case SoldierConfig.KEY_SPEAR -> "Spear";
			default -> key;
		};
	}

	public static String gearKeyUsage(String key) {
		return switch (key) {
			case SoldierConfig.KEY_HELMET, SoldierConfig.KEY_CHESTPLATE,
					SoldierConfig.KEY_LEGGINGS, SoldierConfig.KEY_BOOTS ->
					"Worn by every soldier of this tier.";
			case SoldierConfig.KEY_SWORD -> "Main melee weapon for sword classes.";
			case SoldierConfig.KEY_AXE -> "Crit/shield-break axe for melee classes.";
			case SoldierConfig.KEY_BOW -> "Ranger main weapon (default: always Power V).";
			case SoldierConfig.KEY_SHIELD -> "Vanguard offhand shield.";
			case SoldierConfig.KEY_SPEAR -> "Lancer reach weapon.";
			default -> "";
		};
	}
}

package dev.evanklein.battlesoldiers.gui;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Base class for the server-side chest GUIs. All vanilla item movement is
 * disabled; clicks on the top container are routed to {@link #onGuiClick}.
 */
public abstract class SoldierMenu extends ChestMenu {
	protected final ServerPlayer player;

	protected SoldierMenu(int syncId, Inventory playerInventory, int rows) {
		super(
				rows == 3 ? MenuType.GENERIC_9x3 : MenuType.GENERIC_9x6,
				syncId,
				playerInventory,
				new SimpleContainer(rows * 9),
				rows
		);
		this.player = (ServerPlayer) playerInventory.player;
	}

	protected abstract void refresh();

	protected abstract void onGuiClick(int slot, boolean rightClick, boolean shift);

	protected int guiSize() {
		return this.getContainer().getContainerSize();
	}

	protected void setSlot(int slot, ItemStack stack) {
		if (slot >= 0 && slot < this.guiSize()) {
			this.getContainer().setItem(slot, stack);
		}
	}

	protected void clearGui() {
		for (int slot = 0; slot < this.guiSize(); slot++) {
			this.getContainer().setItem(slot, ItemStack.EMPTY);
		}
	}

	protected void fillWith(ItemStack filler) {
		for (int slot = 0; slot < this.guiSize(); slot++) {
			this.getContainer().setItem(slot, filler.copy());
		}
	}

	@Override
	public void clicked(int slotId, int button, ClickType clickType, Player clicker) {
		if (slotId >= 0
				&& slotId < this.guiSize()
				&& (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE)) {
			this.onGuiClick(slotId, button == 1, clickType == ClickType.QUICK_MOVE);
		}
		// Never run vanilla click logic; resync to undo any client-side prediction.
		this.sendAllDataToRemote();
	}

	@Override
	public ItemStack quickMoveStack(Player player, int slot) {
		return ItemStack.EMPTY;
	}

	@Override
	public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
		return false;
	}

	@Override
	public boolean stillValid(Player player) {
		return true;
	}

	protected void navigate(String title, SoldierMenus.Factory factory) {
		ServerPlayer target = this.player;
		later(target, () -> target.openMenu(new SimpleMenuProvider(
				(syncId, inventory, ignored) -> factory.create(syncId, inventory),
				name(title, ChatFormatting.DARK_GRAY)
		)));
	}

	protected void closeMenu() {
		ServerPlayer target = this.player;
		later(target, target::closeContainer);
	}

	private static void later(ServerPlayer player, Runnable task) {
		MinecraftServer server = player.level().getServer();
		if (server != null) {
			// schedule() enqueues even on the server thread, deferring past the current click packet.
			server.schedule(new TickTask(server.getTickCount(), task));
		}
	}

	// --- shared item styling helpers ---

	protected static Component name(String text, ChatFormatting... formats) {
		return Component.literal(text)
				.withStyle(style -> style.withItalic(false).withBold(false))
				.withStyle(formats);
	}

	protected static Component loreLine(String text) {
		return loreLine(text, ChatFormatting.GRAY);
	}

	protected static Component loreLine(String text, ChatFormatting... formats) {
		return Component.literal(text)
				.withStyle(style -> style.withItalic(false))
				.withStyle(formats);
	}

	protected static ItemStack button(Item item, Component displayName, List<Component> lore) {
		return button(new ItemStack(item), displayName, lore);
	}

	protected static ItemStack button(ItemStack stack, Component displayName, List<Component> lore) {
		stack.set(DataComponents.CUSTOM_NAME, displayName);
		if (!lore.isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(List.copyOf(lore)));
		}
		stack.set(
				DataComponents.TOOLTIP_DISPLAY,
				TooltipDisplay.DEFAULT.withHidden(DataComponents.ATTRIBUTE_MODIFIERS, true)
		);
		return stack;
	}

	/** Adds lore to a stack while preserving its natural item name and tooltip. */
	protected static ItemStack withLore(ItemStack stack, List<Component> lore) {
		if (!lore.isEmpty()) {
			stack.set(DataComponents.LORE, new ItemLore(List.copyOf(lore)));
		}
		return stack;
	}

	protected static ItemStack filler() {
		ItemStack pane = new ItemStack(Items.GRAY_STAINED_GLASS_PANE);
		pane.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true, new LinkedHashSet<>()));
		return pane;
	}

	protected static ItemStack backButton() {
		return button(
				Items.ARROW,
				name("← Back", ChatFormatting.YELLOW),
				List.of(loreLine("Return to the previous menu."))
		);
	}

	protected static ItemStack closeButton() {
		return button(
				Items.BARRIER,
				name("Close", ChatFormatting.RED),
				List.of(loreLine("Close this menu."))
		);
	}

	protected static List<Component> wrapLore(String text, ChatFormatting color) {
		List<Component> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (String word : text.split(" ")) {
			if (current.length() + word.length() + 1 > 38 && current.length() > 0) {
				lines.add(loreLine(current.toString(), color));
				current.setLength(0);
			}
			if (current.length() > 0) {
				current.append(' ');
			}
			current.append(word);
		}
		if (current.length() > 0) {
			lines.add(loreLine(current.toString(), color));
		}
		return lines;
	}
}

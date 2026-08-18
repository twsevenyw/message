package dev.evanklein.battlesoldiers.debug;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import dev.evanklein.battlesoldiers.entity.BattleSoldierEntity;
import dev.evanklein.battlesoldiers.entity.ModEntities;
import dev.evanklein.battlesoldiers.gui.SoldierMenus;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Headless test harness, only registered when the JVM is started with
 * {@code -Dbattlesoldiers.debug=true}. Spawns a fake player backed by an
 * embedded Netty channel so GUI menus can be opened and clicked from the
 * server console for automated verification.
 */
public final class DebugTools {
	private DebugTools() {
	}

	public static boolean enabled() {
		return Boolean.getBoolean("battlesoldiers.debug");
	}

	public static LiteralArgumentBuilder<CommandSourceStack> commandTree() {
		return Commands.literal("debug")
				.requires(Commands.hasPermission(Commands.LEVEL_OWNERS))
				.then(Commands.literal("join")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(DebugTools::joinFake)))
				.then(Commands.literal("open")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(DebugTools::openMenu)))
				.then(Commands.literal("click")
						.then(Commands.argument("name", StringArgumentType.word())
								.then(Commands.argument("slot", IntegerArgumentType.integer(0, 53))
										.then(Commands.argument("button", IntegerArgumentType.integer(0, 1))
												.then(Commands.argument("shift", IntegerArgumentType.integer(0, 1))
														.executes(DebugTools::clickMenu))))))
				.then(Commands.literal("menuinfo")
						.then(Commands.argument("name", StringArgumentType.word())
								.executes(DebugTools::menuInfo)))
				.then(Commands.literal("targets").executes(DebugTools::dumpTargets));
	}

	private static int dumpTargets(CommandContext<CommandSourceStack> context) {
		MinecraftServer server = context.getSource().getServer();
		java.util.List<BattleSoldierEntity> soldiers = new java.util.ArrayList<>();
		for (ServerLevel level : server.getAllLevels()) {
			level.getEntities(ModEntities.SOLDIER, entity -> entity.isAlive(), soldiers);
		}
		for (BattleSoldierEntity soldier : soldiers) {
			String targetName = soldier.getTarget() == null
					? "none"
					: soldier.getTarget().getName().getString();
			context.getSource().sendSystemMessage(Component.literal(
					"[debug] " + soldier.getName().getString() + " -> " + targetName));
		}
		context.getSource().sendSystemMessage(
				Component.literal("[debug] " + soldiers.size() + " soldiers dumped"));
		return soldiers.size();
	}

	private static int joinFake(CommandContext<CommandSourceStack> context) {
		MinecraftServer server = context.getSource().getServer();
		String fakeName = StringArgumentType.getString(context, "name");
		GameProfile profile = new GameProfile(
				UUID.nameUUIDFromBytes(("battlesoldiers:" + fakeName).getBytes(StandardCharsets.UTF_8)),
				fakeName
		);
		if (server.getPlayerList().getPlayer(profile.id()) != null) {
			context.getSource().sendSystemMessage(Component.literal("[debug] fake player already joined"));
			return 1;
		}
		ServerPlayer player = new ServerPlayer(
				server,
				server.overworld(),
				profile,
				ClientInformation.createDefault()
		);
		Connection connection = new Connection(PacketFlow.SERVERBOUND);
		new EmbeddedChannel(connection);
		server.getPlayerList().placeNewPlayer(
				connection,
				player,
				CommonListenerCookie.createInitial(profile, false)
		);
		context.getSource().sendSystemMessage(
				Component.literal("[debug] joined fake player " + fakeName + " at "
						+ player.blockPosition().toShortString())
		);
		return 1;
	}

	private static ServerPlayer fake(CommandContext<CommandSourceStack> context) {
		String fakeName = StringArgumentType.getString(context, "name");
		return context.getSource().getServer().getPlayerList().getPlayerByName(fakeName);
	}

	private static int openMenu(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = fake(context);
		if (player == null) {
			context.getSource().sendSystemMessage(Component.literal("[debug] no such fake player"));
			return 0;
		}
		SoldierMenus.openMain(player);
		context.getSource().sendSystemMessage(
				Component.literal("[debug] opened main menu: " + describeMenu(player.containerMenu))
		);
		return 1;
	}

	private static int clickMenu(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = fake(context);
		if (player == null) {
			context.getSource().sendSystemMessage(Component.literal("[debug] no such fake player"));
			return 0;
		}
		int slot = IntegerArgumentType.getInteger(context, "slot");
		int button = IntegerArgumentType.getInteger(context, "button");
		boolean shift = IntegerArgumentType.getInteger(context, "shift") == 1;
		AbstractContainerMenu menu = player.containerMenu;
		menu.clicked(slot, button, shift ? ClickType.QUICK_MOVE : ClickType.PICKUP, player);
		context.getSource().sendSystemMessage(
				Component.literal("[debug] clicked slot " + slot + " (button " + button
						+ ", shift " + shift + ") on " + describeMenu(menu))
		);
		return 1;
	}

	private static int menuInfo(CommandContext<CommandSourceStack> context) {
		ServerPlayer player = fake(context);
		if (player == null) {
			context.getSource().sendSystemMessage(Component.literal("[debug] no such fake player"));
			return 0;
		}
		AbstractContainerMenu menu = player.containerMenu;
		context.getSource().sendSystemMessage(
				Component.literal("[debug] menu=" + describeMenu(menu))
		);
		if (menu instanceof ChestMenu chest) {
			for (int slot = 0; slot < chest.getContainer().getContainerSize(); slot++) {
				ItemStack stack = chest.getContainer().getItem(slot);
				if (stack.isEmpty()) {
					continue;
				}
				StringBuilder line = new StringBuilder("[debug] slot ")
						.append(slot)
						.append(": ")
						.append(stack.getHoverName().getString())
						.append(" x")
						.append(stack.getCount());
				if (!stack.getEnchantments().isEmpty()) {
					line.append(" enchants=").append(stack.getEnchantments());
				}
				ItemLore lore = stack.get(DataComponents.LORE);
				if (lore != null && !lore.lines().isEmpty()) {
					line.append(" | lore: ");
					for (Component loreLine : lore.lines()) {
						line.append(loreLine.getString()).append(" ~ ");
					}
				}
				context.getSource().sendSystemMessage(Component.literal(line.toString()));
			}
		}
		return 1;
	}

	private static String describeMenu(AbstractContainerMenu menu) {
		return menu.getClass().getSimpleName() + "#" + menu.containerId;
	}
}

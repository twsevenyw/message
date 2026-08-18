package dev.evanklein.battlesoldiers.battle;

import dev.evanklein.battlesoldiers.config.SoldierConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Static combat-class documentation shared by {@code /soldiers info} and the config GUI. */
public record ClassInfo(
		CombatRole role,
		String tagline,
		String stats,
		List<String> details,
		String counter,
		String availability
) {
	public static ClassInfo of(CombatRole role) {
		return switch (role) {
			case VANGUARD -> new ClassInfo(
					role,
					"Shield-and-sword frontline anchor",
					"20 HP, standard speed",
					List.of(
							"Holds the front rank, takes melee reservations, and bodies space for the squad.",
							"Raises the shield reactively against melee windups, drawn bows, and incoming arrows.",
							"Swaps to the axe only to break an actively raised shield, then returns to the sword.",
							"Attempts predictive jump criticals (1.25x) and joins squad focus-fire rotations.",
							"Endlessly replenishes its sword, axe, and shield."
					),
					"Axe-break its shield or bait the guard, then punish the recovery.",
					"Core class, all gear tiers. Also rolled in tiny squads."
			);
			case BRUTE -> new ClassInfo(
					role,
					"Heavy bruiser fishing for axe crits",
					"22 HP, slightly slower",
					List.of(
							"Trades with the sword in neutral; the axe is reserved exclusively for crits.",
							"Swaps to the axe mid-air to land 1.5x jump criticals, then swaps back.",
							"Runs hit-window combos with sprint resets and feints.",
							"Pins targets against walls in solo duels and keeps relentless pressure."
					),
					"Back off during its jump windup and trade while the axe is still out.",
					"Core class, all gear tiers. Also rolled in tiny squads."
			);
			case RANGER -> new ClassInfo(
					role,
					"Tower archer with Power V and homing volleys",
					"18 HP, standard speed",
					List.of(
							"Builds a tower perch (3-6 layers by tier) and holds it instead of strafing off.",
							"Every bow is Power V at every gear tier, including replenished bows.",
							"Every third arrow is a no-gravity homing shot steered at your predicted center.",
							"Fires rapid anti-air volleys at anyone falling 5+ blocks (mace jumpers).",
							"Teleports to the squad backline instead of dropping out of range or taking fall damage.",
							"Replenishes arrows, blocks, and its bow; carries a backup melee weapon."
					),
					"Shield the third-shot timing, approach through cover, or collapse the tower.",
					"Core class, all gear tiers."
			);
			case TRAPPER -> new ClassInfo(
					role,
					"Web area-denial and pursuit control",
					"20 HP, slightly faster",
					List.of(
							"Predicts your movement and webs escape paths before you take them.",
							"Webs predicted mace-landing cells to cancel smash attacks.",
							"Unlimited web reserve that continuously replenishes.",
							"Finishes webbed targets with the axe; squad chains follow-ups on webbed enemies.",
							"Every non-Trapper also carries 3-5 backup webs with the same trap AI."
					),
					"Keep moving laterally, cut webs with a sword, and watch your landing zones.",
					"Core class, gear 4+."
			);
			case MEDIC -> new ClassInfo(
					role,
					"Backline combat medic",
					"18 HP, slightly slower",
					List.of(
							"Tracks wounded allies and applies strong healing and regeneration potions.",
							"Stays behind the frontline and repositions away from threats.",
							"Continuously replenishes its healing supplies."
					),
					"Focus it first: killing the Medic deletes the squad's sustain.",
					"Specialist, gear 3+. Squad specialist cap: 20%."
			);
			case ENGINEER -> new ClassInfo(
					role,
					"Battlefield fortifier",
					"22 HP, slowest class",
					List.of(
							"Builds two-block cover walls for allies under ranged fire.",
							"Places fall-canceling canopy blocks over squadmates during mace dives.",
							"Carries ladders and huge, continuously replenished block reserves."
					),
					"It is weak in the open; pull it away from its fortifications.",
					"Specialist, gear 3+. Squad specialist cap: 20%."
			);
			case LANCER -> new ClassInfo(
					role,
					"Reach-control spear skirmisher",
					"20 HP, standard speed",
					List.of(
							"Pokes with the kinetic spear at max reach and resets spacing between stabs.",
							"Punishes straight-line approaches and holds chokepoints.",
							"Continuously replenishes its spear."
					),
					"Slip inside its reach and stay glued to it.",
					"Specialist, gear 2+. Squad specialist cap: 20%."
			);
			case DUELIST -> new ClassInfo(
					role,
					"1v1 finesse fighter",
					"18 HP, fastest class",
					List.of(
							"Tuned for solo combat: tight cooldown strafes, feints, and combo windows.",
							"Uses wall-jump movement and immediate target scans in duels.",
							"Sword only; wins through timing rather than burst."
					),
					"Do not chase; let it overextend into your attack range.",
					"Specialist, all gear tiers. Also rolled in tiny squads."
			);
			case ALCHEMIST -> new ClassInfo(
					role,
					"Debuff opener",
					"18 HP, slightly slower",
					List.of(
							"Opens squad combo chains with splash poison, weakness, and slowness.",
							"Never splashes allies; lobs from behind the frontline.",
							"Squadmates prioritize targets it has debuffed.",
							"Continuously replenishes its splash potions."
					),
					"Dodge the throw arc and cleanse with milk if you carry it.",
					"Specialist, gear 4+. Squad specialist cap: 20%."
			);
			case ENDER_SKIRMISHER -> new ClassInfo(
					role,
					"Pearl flanker",
					"18 HP, faster",
					List.of(
							"Blinks behind distant or elevated targets with ender pearls.",
							"Punishes tower camping, kiting, and backpedaling.",
							"Continuously replenishes its pearls."
					),
					"Expect it behind you the moment you gain distance; fight it in tight spaces.",
					"Specialist, gear 4+. Squad specialist cap: 20%."
			);
			case DEMOLITIONIST -> new ClassInfo(
					role,
					"TNT area bomber",
					"22 HP, slower",
					List.of(
							"Plants short-fuse TNT at your position and retreats out of the blast.",
							"Aborts plants that would catch allies in the explosion.",
							"Fights with the axe between plants.",
							"Continuously replenishes its TNT and weapons."
					),
					"Use the fuse: knock the TNT away or keep the fight moving.",
					"Specialist, gear 4+. Squad specialist cap: 20%."
			);
		};
	}

	/** Full chat breakdown for {@code /soldiers info <class>} and GUI clicks. */
	public static List<Component> detailLines(CombatRole role) {
		ClassInfo info = of(role);
		SoldierConfig config = SoldierConfig.get();
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("== " + role.displayName() + " ==")
				.withStyle(role.nameColor(ChatFormatting.GREEN), ChatFormatting.BOLD));
		lines.add(Component.literal(info.tagline()).withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY));
		lines.add(Component.literal("Stats: ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(info.stats()).withStyle(ChatFormatting.WHITE)));
		lines.add(Component.literal("Availability: ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(info.availability()).withStyle(ChatFormatting.WHITE)));
		lines.add(Component.literal("Spawn weight: ").withStyle(ChatFormatting.GOLD)
				.append(Component.literal(formatWeight(config.weight(role))
								+ " (" + formatWeight(config.effectiveShare(role)) + "% share)")
						.withStyle(ChatFormatting.WHITE)));
		for (String detail : info.details()) {
			lines.add(Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
					.append(Component.literal(detail).withStyle(ChatFormatting.GRAY)));
		}
		lines.add(Component.literal("How to fight it: ").withStyle(ChatFormatting.RED)
				.append(Component.literal(info.counter()).withStyle(ChatFormatting.GRAY)));
		return lines;
	}

	/** Compact chat overview for {@code /soldiers info}. */
	public static List<Component> overviewLines() {
		SoldierConfig config = SoldierConfig.get();
		List<Component> lines = new ArrayList<>();
		lines.add(Component.literal("== Battle Soldiers — Classes ==")
				.withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
		lines.add(Component.literal("Core classes:").withStyle(ChatFormatting.GOLD));
		for (CombatRole role : CombatRole.values()) {
			if (!role.isSpecialist()) {
				lines.add(overviewLine(config, role));
			}
		}
		lines.add(Component.literal("Specialists (capped at 20% of a squad):")
				.withStyle(ChatFormatting.GOLD));
		for (CombatRole role : CombatRole.values()) {
			if (role.isSpecialist()) {
				lines.add(overviewLine(config, role));
			}
		}
		lines.add(Component.literal("Use ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal("/soldiers info <class>").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" for details or ").withStyle(ChatFormatting.GRAY))
				.append(Component.literal("/soldiers menu").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" for the config GUI.").withStyle(ChatFormatting.GRAY)));
		lines.add(Component.literal("Spawn a specific class with ").withStyle(ChatFormatting.GRAY)
				.append(Component.literal("/soldiers <count> <gear> <class>").withStyle(ChatFormatting.YELLOW))
				.append(Component.literal(" (bypasses gear gating and caps).").withStyle(ChatFormatting.GRAY)));
		return lines;
	}

	private static Component overviewLine(SoldierConfig config, CombatRole role) {
		ClassInfo info = of(role);
		return Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
				.append(Component.literal(role.displayName())
						.withStyle(role.nameColor(ChatFormatting.WHITE)))
				.append(Component.literal(" — " + formatWeight(config.effectiveShare(role)) + "% — ")
						.withStyle(ChatFormatting.DARK_GRAY))
				.append(Component.literal(info.tagline()).withStyle(ChatFormatting.GRAY));
	}

	public static String formatWeight(double value) {
		return value == Math.floor(value)
				? String.format(Locale.ROOT, "%.0f", value)
				: String.format(Locale.ROOT, "%.1f", value);
	}

	public ItemStack iconStack() {
		return switch (this.role) {
			case VANGUARD -> new ItemStack(Items.SHIELD);
			case BRUTE -> new ItemStack(Items.IRON_AXE);
			case RANGER -> new ItemStack(Items.BOW);
			case TRAPPER -> new ItemStack(Items.COBWEB);
			case MEDIC -> PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING);
			case ENGINEER -> new ItemStack(Items.LADDER);
			case LANCER -> new ItemStack(Items.IRON_SPEAR);
			case DUELIST -> new ItemStack(Items.DIAMOND_SWORD);
			case ALCHEMIST -> PotionContents.createItemStack(Items.SPLASH_POTION, Potions.POISON);
			case ENDER_SKIRMISHER -> new ItemStack(Items.ENDER_PEARL);
			case DEMOLITIONIST -> new ItemStack(Items.TNT);
		};
	}
}

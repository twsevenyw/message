package dev.evanklein.battlesoldiers.battle;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public enum CombatRole {
	VANGUARD("vanguard", "Vanguard"),
	BRUTE("brute", "Brute"),
	RANGER("ranger", "Ranger"),
	TRAPPER("trapper", "Trapper"),
	MEDIC("medic", "Medic"),
	ENGINEER("engineer", "Engineer"),
	LANCER("lancer", "Lancer"),
	DUELIST("duelist", "Duelist"),
	ALCHEMIST("alchemist", "Alchemist"),
	ENDER_SKIRMISHER("ender_skirmisher", "Ender Skirmisher"),
	DEMOLITIONIST("demolitionist", "Demolitionist");

	private final String id;
	private final String displayName;

	CombatRole(String id, String displayName) {
		this.id = id;
		this.displayName = displayName;
	}

	public String id() {
		return this.id;
	}

	public String displayName() {
		return this.displayName;
	}

	public boolean isArcher() {
		return this == RANGER;
	}

	public boolean isSpecialist() {
		return switch (this) {
			case MEDIC, ENGINEER, LANCER, DUELIST, ALCHEMIST, ENDER_SKIRMISHER, DEMOLITIONIST -> true;
			default -> false;
		};
	}

	public boolean isFrontline() {
		return switch (this) {
			case VANGUARD, BRUTE, TRAPPER, LANCER, DUELIST, ENDER_SKIRMISHER -> true;
			default -> false;
		};
	}

	public ChatFormatting nameColor(ChatFormatting squadColor) {
		return switch (this) {
			case MEDIC -> ChatFormatting.LIGHT_PURPLE;
			case ENGINEER -> ChatFormatting.YELLOW;
			case LANCER -> ChatFormatting.AQUA;
			case DUELIST -> ChatFormatting.WHITE;
			case ALCHEMIST -> ChatFormatting.DARK_PURPLE;
			case ENDER_SKIRMISHER -> ChatFormatting.DARK_AQUA;
			case DEMOLITIONIST -> ChatFormatting.DARK_RED;
			default -> squadColor;
		};
	}

	public static CombatRole byId(String id, boolean legacyArcher) {
		if (id != null) {
			String normalized = id.toLowerCase(Locale.ROOT);
			if ("swordsman".equals(normalized)) {
				return VANGUARD;
			}
			if ("axe_fighter".equals(normalized)) {
				return BRUTE;
			}
			if ("archer".equals(normalized)) {
				return RANGER;
			}
			for (CombatRole role : values()) {
				if (role.id.equals(normalized)) {
					return role;
				}
			}
		}
		return legacyArcher ? RANGER : VANGUARD;
	}
}

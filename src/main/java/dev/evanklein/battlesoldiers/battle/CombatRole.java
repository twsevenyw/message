package dev.evanklein.battlesoldiers.battle;

import java.util.Locale;

public enum CombatRole {
	VANGUARD("vanguard", "Vanguard"),
	BRUTE("brute", "Brute"),
	RANGER("ranger", "Ranger"),
	TRAPPER("trapper", "Trapper");

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

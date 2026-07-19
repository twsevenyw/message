package dev.evanklein.battlesoldiers.battle;

import java.util.Locale;

public enum CombatRole {
	SWORDSMAN("swordsman", "Swordsman"),
	AXE_FIGHTER("axe_fighter", "Axe Fighter"),
	ARCHER("archer", "Archer");

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
		return this == ARCHER;
	}

	public static CombatRole byId(String id, boolean legacyArcher) {
		if (id != null) {
			String normalized = id.toLowerCase(Locale.ROOT);
			for (CombatRole role : values()) {
				if (role.id.equals(normalized)) {
					return role;
				}
			}
		}
		return legacyArcher ? ARCHER : SWORDSMAN;
	}
}

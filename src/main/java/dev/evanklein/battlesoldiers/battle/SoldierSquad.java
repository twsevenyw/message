package dev.evanklein.battlesoldiers.battle;

import net.minecraft.ChatFormatting;

import java.util.Locale;

public enum SoldierSquad {
	TRAINING("training", "Training", ChatFormatting.GOLD),
	RED("red", "Red", ChatFormatting.RED),
	BLUE("blue", "Blue", ChatFormatting.BLUE);

	private final String id;
	private final String displayName;
	private final ChatFormatting color;

	SoldierSquad(String id, String displayName, ChatFormatting color) {
		this.id = id;
		this.displayName = displayName;
		this.color = color;
	}

	public String id() {
		return this.id;
	}

	public String displayName() {
		return this.displayName;
	}

	public ChatFormatting color() {
		return this.color;
	}

	public String teamName() {
		return "bs_" + this.id;
	}

	public boolean fights(SoldierSquad other) {
		return (this == RED && other == BLUE) || (this == BLUE && other == RED);
	}

	public static SoldierSquad byId(String id) {
		if (id == null) {
			return TRAINING;
		}

		String normalized = id.toLowerCase(Locale.ROOT);
		for (SoldierSquad squad : values()) {
			if (squad.id.equals(normalized)) {
				return squad;
			}
		}
		return TRAINING;
	}
}

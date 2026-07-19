package dev.evanklein.battlesoldiers.battle;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum GearLevel {
	ONE(
			1, Items.WOODEN_SWORD,
			Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS,
			20.0, 0.27, 1, 6, 2.0F, 0.75F, false
	),
	TWO(
			2, Items.STONE_SWORD,
			Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS,
			24.0, 0.29, 1, 10, 4.0F, 1.0F, true
	),
	THREE(
			3, Items.IRON_SWORD,
			Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS,
			28.0, 0.30, 2, 14, 8.0F, 1.25F, true
	),
	FOUR(
			4, Items.DIAMOND_SWORD,
			Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS,
			34.0, 0.32, 3, 20, 25.0F, 1.55F, true
	),
	FIVE(
			5, Items.NETHERITE_SWORD,
			Items.NETHERITE_HELMET, Items.NETHERITE_CHESTPLATE, Items.NETHERITE_LEGGINGS, Items.NETHERITE_BOOTS,
			42.0, 0.34, 4, 28, 55.0F, 1.9F, true
	);

	private final int id;
	private final Item meleeWeapon;
	private final Item helmet;
	private final Item chestplate;
	private final Item leggings;
	private final Item boots;
	private final double maxHealth;
	private final double movementSpeed;
	private final int goldenApples;
	private final int buildingBlocks;
	private final float maxBreakHardness;
	private final float miningPower;
	private final boolean archerEligible;

	GearLevel(
			int id,
			Item meleeWeapon,
			Item helmet,
			Item chestplate,
			Item leggings,
			Item boots,
			double maxHealth,
			double movementSpeed,
			int goldenApples,
			int buildingBlocks,
			float maxBreakHardness,
			float miningPower,
			boolean archerEligible
	) {
		this.id = id;
		this.meleeWeapon = meleeWeapon;
		this.helmet = helmet;
		this.chestplate = chestplate;
		this.leggings = leggings;
		this.boots = boots;
		this.maxHealth = maxHealth;
		this.movementSpeed = movementSpeed;
		this.goldenApples = goldenApples;
		this.buildingBlocks = buildingBlocks;
		this.maxBreakHardness = maxBreakHardness;
		this.miningPower = miningPower;
		this.archerEligible = archerEligible;
	}

	public int id() {
		return this.id;
	}

	public Item meleeWeapon() {
		return this.meleeWeapon;
	}

	public Item helmet() {
		return this.helmet;
	}

	public Item chestplate() {
		return this.chestplate;
	}

	public Item leggings() {
		return this.leggings;
	}

	public Item boots() {
		return this.boots;
	}

	public double maxHealth() {
		return this.maxHealth;
	}

	public double movementSpeed() {
		return this.movementSpeed;
	}

	public int goldenApples() {
		return this.goldenApples;
	}

	public int buildingBlocks() {
		return this.buildingBlocks;
	}

	public float maxBreakHardness() {
		return this.maxBreakHardness;
	}

	public float miningPower() {
		return this.miningPower;
	}

	public boolean archerEligible() {
		return this.archerEligible;
	}

	public static GearLevel byId(int id) {
		return switch (id) {
			case 2 -> TWO;
			case 3 -> THREE;
			case 4 -> FOUR;
			case 5 -> FIVE;
			default -> ONE;
		};
	}
}

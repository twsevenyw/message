package dev.evanklein.battlesoldiers.battle;

import net.minecraft.world.entity.EquipmentSlot;
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

	public Item axeWeapon() {
		return switch (this) {
			case ONE -> Items.WOODEN_AXE;
			case TWO -> Items.STONE_AXE;
			case THREE -> Items.IRON_AXE;
			case FOUR -> Items.DIAMOND_AXE;
			case FIVE -> Items.NETHERITE_AXE;
		};
	}

	public Item backupWeapon() {
		return switch (this) {
			case ONE -> Items.WOODEN_SWORD;
			case TWO -> Items.STONE_SWORD;
			case THREE -> Items.IRON_SWORD;
			case FOUR -> Items.IRON_SWORD;
			case FIVE -> Items.DIAMOND_SWORD;
		};
	}

	public Item lowerArmor(EquipmentSlot slot) {
		return switch (this) {
			case ONE -> armorForMaterial(slot, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
			case TWO -> armorForMaterial(slot, Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);
			case THREE -> armorForMaterial(slot, Items.CHAINMAIL_HELMET, Items.CHAINMAIL_CHESTPLATE, Items.CHAINMAIL_LEGGINGS, Items.CHAINMAIL_BOOTS);
			case FOUR -> armorForMaterial(slot, Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS);
			case FIVE -> armorForMaterial(slot, Items.DIAMOND_HELMET, Items.DIAMOND_CHESTPLATE, Items.DIAMOND_LEGGINGS, Items.DIAMOND_BOOTS);
		};
	}

	public Item armor(EquipmentSlot slot) {
		return armorForMaterial(slot, this.helmet, this.chestplate, this.leggings, this.boots);
	}

	public float archerChance() {
		return switch (this) {
			case ONE -> 0.0F;
			case TWO -> 0.18F;
			case THREE -> 0.24F;
			case FOUR -> 0.28F;
			case FIVE -> 0.34F;
		};
	}

	public float potionChance() {
		return switch (this) {
			case ONE -> 0.0F;
			case TWO -> 0.12F;
			case THREE -> 0.28F;
			case FOUR -> 0.46F;
			case FIVE -> 0.64F;
		};
	}

	public float totemChance() {
		return switch (this) {
			case ONE, TWO, THREE -> 0.0F;
			case FOUR -> 0.14F;
			case FIVE -> 0.32F;
		};
	}

	public float shieldChance() {
		return switch (this) {
			case ONE -> 0.15F;
			case TWO -> 0.65F;
			case THREE -> 0.78F;
			case FOUR -> 0.88F;
			case FIVE -> 0.94F;
		};
	}

	private static Item armorForMaterial(
			EquipmentSlot slot,
			Item helmet,
			Item chestplate,
			Item leggings,
			Item boots
	) {
		return switch (slot) {
			case HEAD -> helmet;
			case CHEST -> chestplate;
			case LEGS -> leggings;
			case FEET -> boots;
			default -> Items.AIR;
		};
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

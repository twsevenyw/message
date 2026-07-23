package dev.evanklein.battlesoldiers.entity;

import com.mojang.serialization.Codec;
import dev.evanklein.battlesoldiers.battle.BattleTeams;
import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.battle.SoldierSquad;
import dev.evanklein.battlesoldiers.battle.SquadCoordinator;
import dev.evanklein.battlesoldiers.entity.ai.BreachObstacleGoal;
import dev.evanklein.battlesoldiers.entity.ai.AlchemistDebuffGoal;
import dev.evanklein.battlesoldiers.entity.ai.AntiMaceCounterGoal;
import dev.evanklein.battlesoldiers.entity.ai.DemolitionistGoal;
import dev.evanklein.battlesoldiers.entity.ai.EnderSkirmisherGoal;
import dev.evanklein.battlesoldiers.entity.ai.EngineerFortifyGoal;
import dev.evanklein.battlesoldiers.entity.ai.EscapeBlockGoal;
import dev.evanklein.battlesoldiers.entity.ai.MedicSupportGoal;
import dev.evanklein.battlesoldiers.entity.ai.ObstructionAwareTargetGoal;
import dev.evanklein.battlesoldiers.entity.ai.ProjectileDodgeGoal;
import dev.evanklein.battlesoldiers.entity.ai.ResourceShareGoal;
import dev.evanklein.battlesoldiers.entity.ai.RangerElevationGoal;
import dev.evanklein.battlesoldiers.entity.ai.SoldierCombatGoal;
import dev.evanklein.battlesoldiers.entity.ai.SoldierWanderGoal;
import dev.evanklein.battlesoldiers.entity.ai.TacticalBuildGoal;
import dev.evanklein.battlesoldiers.entity.ai.TrapperWebGoal;
import dev.evanklein.battlesoldiers.entity.ai.UseCombatConsumableGoal;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class BattleSoldierEntity extends Monster implements RangedAttackMob {
	private static final String INVENTORY_TAG = "SoldierInventory";
	private static final int NO_SLOT = -1;

	private final SimpleContainer soldierInventory = new SimpleContainer(Inventory.INVENTORY_SIZE);
	private final LongSet placedBlocks = new LongOpenHashSet();

	private SoldierSquad squad = SoldierSquad.TRAINING;
	private GearLevel gearLevel = GearLevel.ONE;
	private CombatRole combatRole = CombatRole.VANGUARD;
	private boolean initialized;
	private boolean attackTelegraphed;
	private double criticalAttackMultiplier = 1.0;
	private int criticalHits;
	private int reactiveShieldUses;
	private int escapeBlocksPlaced;
	private int consumableCooldown;
	private int preparedConsumableSlot = NO_SLOT;
	private int rangerTowerCooldown;
	private int webTrapCooldown;
	@Nullable
	private BlockPos rangerPerchTop;
	private boolean rangerTowerSpent;
	private int rangerTargetlessTicks;
	private ItemStack activeConsumable = ItemStack.EMPTY;
	private ItemStack savedOffhand = ItemStack.EMPTY;

	public BattleSoldierEntity(EntityType<? extends BattleSoldierEntity> entityType, Level level) {
		super(entityType, level);
	}

	public static AttributeSupplier.Builder createSoldierAttributes() {
		return Monster.createMonsterAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.22)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 36.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new ProjectileDodgeGoal(this));
		this.goalSelector.addGoal(1, new AntiMaceCounterGoal(this));
		this.goalSelector.addGoal(1, new UseCombatConsumableGoal(this));
		this.goalSelector.addGoal(2, new MedicSupportGoal(this));
		this.goalSelector.addGoal(2, new EnderSkirmisherGoal(this));
		this.goalSelector.addGoal(2, new DemolitionistGoal(this));
		this.goalSelector.addGoal(2, new BreachObstacleGoal(this));
		this.goalSelector.addGoal(3, new AlchemistDebuffGoal(this));
		this.goalSelector.addGoal(3, new EngineerFortifyGoal(this));
		this.goalSelector.addGoal(3, new EscapeBlockGoal(this));
		this.goalSelector.addGoal(3, new RangerElevationGoal(this));
		this.goalSelector.addGoal(3, new TrapperWebGoal(this));
		this.goalSelector.addGoal(3, new TacticalBuildGoal(this));
		this.goalSelector.addGoal(4, new SoldierCombatGoal(this));
		this.goalSelector.addGoal(6, new ResourceShareGoal(this));
		this.goalSelector.addGoal(7, new SoldierWanderGoal(this, 0.9));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, LivingEntity.class, 10.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new ObstructionAwareTargetGoal<>(
				this,
				Player.class,
				1,
				(target, level) -> target instanceof Player player && this.isValidPlayerTarget(player)
		));
		this.targetSelector.addGoal(3, new ObstructionAwareTargetGoal<>(
				this,
				BattleSoldierEntity.class,
				1,
				(target, level) -> target instanceof BattleSoldierEntity soldier
						&& this.isValidSoldierTarget(soldier)
		));
	}

	public void initializeSoldier(SoldierSquad squad, GearLevel gearLevel) {
		SquadCoordinator.unregister(this);
		this.squad = squad;
		this.gearLevel = gearLevel;
		this.combatRole = this.chooseCombatRole();
		this.initialized = true;
		this.generateRandomLoadout();
		BattleTeams.assignSoldier(this);
		SquadCoordinator.heartbeat(this);
	}

	public void initializeSoldier(SoldierSquad squad, GearLevel gearLevel, boolean archer) {
		SquadCoordinator.unregister(this);
		this.squad = squad;
		this.gearLevel = gearLevel;
		this.combatRole = archer
				? CombatRole.RANGER
				: this.chooseMeleeRole();
		this.initialized = true;
		this.generateRandomLoadout();
		BattleTeams.assignSoldier(this);
		SquadCoordinator.heartbeat(this);
	}

	private CombatRole chooseCombatRole() {
		return SquadCoordinator.chooseRole(this, this.gearLevel);
	}

	private CombatRole chooseMeleeRole() {
		return this.getRandom().nextFloat() < 0.36F ? CombatRole.BRUTE : CombatRole.VANGUARD;
	}

	private void generateRandomLoadout() {
		this.soldierInventory.clearContent();
		this.setItemSlot(EquipmentSlot.MAINHAND, this.randomizedStack(this.primaryWeapon()));
		this.equipRandomArmor(EquipmentSlot.HEAD);
		this.equipRandomArmor(EquipmentSlot.CHEST);
		this.equipRandomArmor(EquipmentSlot.LEGS);
		this.equipRandomArmor(EquipmentSlot.FEET);
		this.setItemSlot(EquipmentSlot.OFFHAND, ItemStack.EMPTY);

		if (this.isArcher()) {
			this.addToInventory(this.randomizedStack(this.gearLevel.backupWeapon()));
			this.addToInventory(new ItemStack(Items.ARROW, 14 + this.getRandom().nextInt(15)));
			if (this.gearLevel.id() >= 4 && this.getRandom().nextFloat() < 0.22F) {
				this.addToInventory(new ItemStack(Items.SPECTRAL_ARROW, 3 + this.getRandom().nextInt(5)));
			}
		}
		if (this.combatRole == CombatRole.TRAPPER) {
			int webs = switch (this.gearLevel) {
				case SIX -> 12;
				case FIVE -> 8;
				case FOUR -> 5;
				default -> 0;
			};
			this.addToInventory(new ItemStack(Items.COBWEB, webs));
			this.addToInventory(this.randomizedStack(this.gearLevel.axeWeapon()));
		}
		if (this.combatRole == CombatRole.VANGUARD) {
			this.addToInventory(this.randomizedStack(this.gearLevel.axeWeapon()));
		}
		if (this.combatRole == CombatRole.BRUTE || this.combatRole == CombatRole.DEMOLITIONIST) {
			this.addToInventory(this.randomizedStack(this.gearLevel.meleeWeapon()));
		}
		switch (this.combatRole) {
			case MEDIC -> {
				this.addToInventory(PotionContents.createItemStack(Items.POTION, Potions.STRONG_HEALING));
				this.addToInventory(PotionContents.createItemStack(Items.POTION, Potions.HEALING));
				this.addToInventory(PotionContents.createItemStack(Items.POTION, Potions.REGENERATION));
			}
			case ENGINEER -> this.addToInventory(new ItemStack(Items.LADDER, 12 + this.gearLevel.id() * 2));
			case ALCHEMIST -> {
				this.addToInventory(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.POISON));
				this.addToInventory(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.WEAKNESS));
				this.addToInventory(PotionContents.createItemStack(Items.SPLASH_POTION, Potions.SLOWNESS));
			}
			case ENDER_SKIRMISHER ->
					this.addToInventory(new ItemStack(Items.ENDER_PEARL, 2 + this.gearLevel.id() / 2));
			case DEMOLITIONIST -> this.addToInventory(new ItemStack(Items.TNT, 2 + this.gearLevel.id() / 2));
			default -> {
			}
		}

		int blockCount = switch (this.combatRole) {
			case RANGER -> 10 + this.gearLevel.id() * 2;
			case TRAPPER -> 4 + this.gearLevel.id();
			case ENGINEER -> 18 + this.gearLevel.id() * 3;
			case DEMOLITIONIST -> 4 + this.gearLevel.id();
			default -> 2 + this.gearLevel.id();
		};
		int cobblestone = Math.max(1, (int) Math.ceil(blockCount * 0.65));
		this.addToInventory(new ItemStack(Items.COBBLESTONE, cobblestone));
		this.addToInventory(new ItemStack(Items.OAK_PLANKS, Math.max(1, blockCount - cobblestone)));

		int goldenApples = this.gearLevel == GearLevel.SIX
				? 5 + this.getRandom().nextInt(3)
				: 2 + this.getRandom().nextInt(2);
		this.addToInventory(new ItemStack(Items.GOLDEN_APPLE, goldenApples));
		if (this.gearLevel == GearLevel.SIX) {
			this.addToInventory(new ItemStack(Items.ENCHANTED_GOLDEN_APPLE, 1 + this.getRandom().nextInt(2)));
		}
		this.addToInventory(new ItemStack(Items.COOKED_BEEF, 2 + this.getRandom().nextInt(3 + this.gearLevel.id())));

		boolean carriesShield = this.combatRole == CombatRole.VANGUARD
				&& this.getRandom().nextFloat() < this.gearLevel.shieldChance();
		boolean carriesTotem = this.getRandom().nextFloat() < this.gearLevel.totemChance();
		int totemCount = this.gearLevel == GearLevel.SIX
				? 2 + this.getRandom().nextInt(2)
				: carriesTotem ? 1 : 0;
		if (carriesShield) {
			this.setItemSlot(EquipmentSlot.OFFHAND, this.randomizedStack(Items.SHIELD));
			for (int index = 0; index < totemCount; index++) {
				this.addToInventory(new ItemStack(Items.TOTEM_OF_UNDYING));
			}
		} else if (totemCount > 0) {
			this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			for (int index = 1; index < totemCount; index++) {
				this.addToInventory(new ItemStack(Items.TOTEM_OF_UNDYING));
			}
		}
		this.addRandomPotions();
		this.applyTierSixEnchantments();

		this.configureGuaranteedDrops();
		this.setCanPickUpLoot(true);
		this.setPersistenceRequired();
		this.updateAttributes(true);
		this.updateDisplayName();
	}

	private void equipRandomArmor(EquipmentSlot slot) {
		if (this.gearLevel == GearLevel.SIX) {
			this.setItemSlot(slot, this.randomizedStack(this.gearLevel.armor(slot)));
			return;
		}
		float equipChance = 0.72F + this.gearLevel.id() * 0.05F;
		if (this.getRandom().nextFloat() > equipChance) {
			this.setItemSlot(slot, ItemStack.EMPTY);
			return;
		}

		Item item = this.getRandom().nextFloat() < 0.27F
				? this.gearLevel.lowerArmor(slot)
				: this.gearLevel.armor(slot);
		this.setItemSlot(slot, this.randomizedStack(item));
	}

	private Item primaryWeapon() {
		return switch (this.combatRole) {
			case RANGER -> Items.BOW;
			case BRUTE, DEMOLITIONIST -> this.gearLevel.axeWeapon();
			case LANCER -> this.gearLevel.spearWeapon();
			default -> this.gearLevel.meleeWeapon();
		};
	}

	private ItemStack randomizedStack(Item item) {
		if (item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(item);
		if (stack.isDamageableItem() && this.gearLevel != GearLevel.SIX) {
			int wearRange = Math.max(1, stack.getMaxDamage() / 3);
			stack.setDamageValue(this.getRandom().nextInt(wearRange));
		}
		return stack;
	}

	private void addRandomPotions() {
		if (this.getRandom().nextFloat() >= this.gearLevel.potionChance()) {
			return;
		}

		this.addToInventory(this.createRandomPotion());
	}

	private ItemStack createRandomPotion() {
		Holder<Potion> potion = switch (this.getRandom().nextInt(4)) {
			case 0 -> Potions.STRENGTH;
			case 1 -> Potions.SWIFTNESS;
			case 2 -> Potions.FIRE_RESISTANCE;
			default -> Potions.HEALING;
		};
		return PotionContents.createItemStack(Items.POTION, potion);
	}

	private void applyTierSixEnchantments() {
		if (this.gearLevel != GearLevel.SIX || !(this.level() instanceof ServerLevel level)) {
			return;
		}

		HolderLookup.RegistryLookup<Enchantment> enchantments =
				level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		for (EquipmentSlot slot : EnumSet.of(
				EquipmentSlot.MAINHAND,
				EquipmentSlot.OFFHAND,
				EquipmentSlot.HEAD,
				EquipmentSlot.CHEST,
				EquipmentSlot.LEGS,
				EquipmentSlot.FEET
		)) {
			this.enchantTierSixStack(this.getItemBySlot(slot), enchantments);
		}
		for (int slot = 0; slot < this.soldierInventory.getContainerSize(); slot++) {
			this.enchantTierSixStack(this.soldierInventory.getItem(slot), enchantments);
		}
	}

	private void enchantTierSixStack(
			ItemStack stack,
			HolderLookup.RegistryLookup<Enchantment> enchantments
	) {
		if (stack.isEmpty() || !stack.isDamageableItem()) {
			return;
		}

		Holder<Enchantment> unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
		this.applyEnchant(stack, unbreaking, 3);
		if (stack.is(Items.BOW)) {
			this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.POWER), 5);
		} else if (stack.has(DataComponents.KINETIC_WEAPON)) {
			this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.SHARPNESS), 5);
		} else if (stack.is(Items.NETHERITE_SWORD)) {
			this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.SHARPNESS), 5);
		} else if (stack.is(Items.NETHERITE_AXE)) {
			this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.SHARPNESS), 5);
			this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.EFFICIENCY), 5);
		} else if (stack.is(Items.NETHERITE_HELMET)
				|| stack.is(Items.NETHERITE_CHESTPLATE)
				|| stack.is(Items.NETHERITE_LEGGINGS)
				|| stack.is(Items.NETHERITE_BOOTS)) {
			this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.PROTECTION), 4);
			if (stack.is(Items.NETHERITE_BOOTS)) {
				this.applyEnchant(stack, enchantments.getOrThrow(Enchantments.FEATHER_FALLING), 4);
			}
		}
	}

	private void applyEnchant(ItemStack stack, Holder<Enchantment> enchantment, int level) {
		if (enchantment.value().canEnchant(stack)) {
			stack.enchant(enchantment, level);
		}
	}

	private void configureGuaranteedDrops() {
		for (EquipmentSlot slot : EnumSet.of(
				EquipmentSlot.MAINHAND,
				EquipmentSlot.OFFHAND,
				EquipmentSlot.HEAD,
				EquipmentSlot.CHEST,
				EquipmentSlot.LEGS,
				EquipmentSlot.FEET
		)) {
			this.setGuaranteedDrop(slot);
		}
	}

	private void updateAttributes(boolean restoreHealth) {
		AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
		float previousHealth = this.getHealth();
		double classHealth = switch (this.combatRole) {
			case BRUTE, ENGINEER, DEMOLITIONIST -> 22.0;
			case RANGER, MEDIC, DUELIST, ALCHEMIST, ENDER_SKIRMISHER -> 18.0;
			case VANGUARD, TRAPPER, LANCER -> 20.0;
		};
		double classMovement = this.gearLevel.movementSpeed() + switch (this.combatRole) {
			case ENGINEER, DEMOLITIONIST -> -0.020;
			case BRUTE, MEDIC, ALCHEMIST -> -0.010;
			case RANGER, VANGUARD, LANCER -> 0.0;
			case TRAPPER -> 0.005;
			case DUELIST, ENDER_SKIRMISHER -> 0.015;
		};
		if (maxHealth != null) {
			maxHealth.setBaseValue(classHealth);
			this.setHealth(restoreHealth ? (float) classHealth : Math.min(previousHealth, (float) classHealth));
		}
		if (movement != null) {
			movement.setBaseValue(classMovement);
		}
	}

	private void updateDisplayName() {
		this.setCustomName(Component.literal(
				this.squad.displayName() + " " + this.combatRole.displayName() + " • Gear " + this.gearLevel.id()
		).withStyle(this.combatRole.nameColor(this.squad.color())));
		this.setCustomNameVisible(true);
	}

	public SoldierSquad getSquad() {
		return this.squad;
	}

	public GearLevel getGearLevel() {
		return this.gearLevel;
	}

	public CombatRole getCombatRole() {
		return this.combatRole;
	}

	public boolean isArcher() {
		return this.combatRole.isArcher();
	}

	public SimpleContainer getSoldierInventory() {
		return this.soldierInventory;
	}

	public int getCarriedItemCount() {
		int count = 0;
		for (int slot = 0; slot < this.soldierInventory.getContainerSize(); slot++) {
			if (!this.soldierInventory.getItem(slot).isEmpty()) {
				count++;
			}
		}
		return count;
	}

	public void setSquad(SoldierSquad squad) {
		SquadCoordinator.unregister(this);
		this.squad = squad;
		this.updateDisplayName();
		BattleTeams.assignSoldier(this);
		this.setTarget(null);
		SquadCoordinator.heartbeat(this);
	}

	public boolean isValidSoldierTarget(BattleSoldierEntity target) {
		return target != this && target.isAlive() && this.squad.fights(target.squad);
	}

	public boolean isValidPlayerTarget(Player player) {
		if (!player.isAlive() || player.isCreative() || player.isSpectator()) {
			return false;
		}

		return switch (this.squad) {
			case TRAINING -> BattleTeams.squadOf(player).orElse(null) != SoldierSquad.TRAINING;
			case RED, BLUE -> BattleTeams.squadOf(player)
					.map(this.squad::fights)
					.orElse(false);
		};
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		boolean valid = target instanceof BattleSoldierEntity soldier
				? this.isValidSoldierTarget(soldier)
				: target instanceof Player player && this.isValidPlayerTarget(player);
		return valid && super.canAttack(target);
	}

	public boolean isRangedThreat(LivingEntity target) {
		if (target instanceof BattleSoldierEntity soldier) {
			return soldier.isArcher() && soldier.getMainHandItem().is(Items.BOW);
		}
		return target.isHolding(Items.BOW)
				|| target.isHolding(Items.CROSSBOW)
				|| target.isHolding(Items.TRIDENT);
	}

	public double estimatedIncomingDamage(LivingEntity attacker) {
		double damage = attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
		if (attacker.getMainHandItem().is(Items.MACE) && attacker.fallDistance > 1.5F) {
			damage += Math.min(20.0, attacker.fallDistance * 2.5);
		}
		if (attacker.getMainHandItem().has(DataComponents.KINETIC_WEAPON)) {
			damage *= 1.25;
		}
		if (attacker.isUsingItem() && this.isRangedThreat(attacker)) {
			damage = Math.max(damage, 8.0);
		}
		return Math.max(1.0, damage);
	}

	public boolean isAttackTelegraphed() {
		return this.attackTelegraphed;
	}

	public void setAttackTelegraphed(boolean telegraphed) {
		this.attackTelegraphed = telegraphed;
	}

	public void recordReactiveShieldUse() {
		this.reactiveShieldUses++;
	}

	public int getReactiveShieldUses() {
		return this.reactiveShieldUses;
	}

	public int getCriticalHits() {
		return this.criticalHits;
	}

	public void recordEscapeBlock() {
		this.escapeBlocksPlaced++;
	}

	public int getEscapeBlocksPlaced() {
		return this.escapeBlocksPlaced;
	}

	public boolean hasIncomingProjectile(double radius) {
		if (!(this.level() instanceof ServerLevel level)) {
			return false;
		}

		AABB searchArea = this.getBoundingBox().inflate(radius);
		return !level.getEntitiesOfClass(Projectile.class, searchArea, projectile -> {
			if (projectile.isRemoved() || projectile.getDeltaMovement().lengthSqr() < 1.0E-4) {
				return false;
			}

			Entity owner = projectile.getOwner();
			if (owner == this || owner != null && this.isAlliedTo(owner)) {
				return false;
			}
			if (owner instanceof LivingEntity livingOwner
					&& (livingOwner instanceof Player || livingOwner instanceof BattleSoldierEntity)
					&& !this.canAttack(livingOwner)) {
				return false;
			}

			Vec3 toSoldier = this.getEyePosition().subtract(projectile.position());
			double distance = toSoldier.length();
			if (distance < 0.001) {
				return true;
			}
			double convergence = projectile.getDeltaMovement().normalize().dot(toSoldier.scale(1.0 / distance));
			if (convergence < 0.68) {
				return false;
			}

			double speed = projectile.getDeltaMovement().length();
			double ticksToImpact = distance / Math.max(0.05, speed);
			return ticksToImpact <= 12.0;
		}).isEmpty();
	}

	@Nullable
	public Vec3 findProjectileDodgePosition(double radius) {
		if (!(this.level() instanceof ServerLevel level)) {
			return null;
		}
		Projectile best = null;
		double bestTime = Double.MAX_VALUE;
		for (Projectile projectile : level.getEntitiesOfClass(
				Projectile.class,
				this.getBoundingBox().inflate(radius),
				projectile -> projectile != null && !projectile.isRemoved()
		)) {
			Entity owner = projectile.getOwner();
			if (owner == this || owner != null && this.isAlliedTo(owner)) {
				continue;
			}
			Vec3 velocity = projectile.getDeltaMovement().subtract(this.getDeltaMovement());
			if (velocity.lengthSqr() < 1.0E-4) {
				continue;
			}
			Vec3 relative = this.getBoundingBox().getCenter().subtract(projectile.position());
			double time = Mth.clamp(relative.dot(velocity) / velocity.lengthSqr(), 0.0, 12.0);
			double miss = relative.subtract(velocity.scale(time)).length();
			if (miss <= this.getBbWidth() + 0.8 && time < bestTime) {
				best = projectile;
				bestTime = time;
			}
		}
		if (best == null) {
			return null;
		}

		Vec3 velocity = best.getDeltaMovement();
		Vec3 side = new Vec3(-velocity.z, 0.0, velocity.x);
		if (side.lengthSqr() < 0.01) {
			return null;
		}
		side = side.normalize();
		for (double direction : new double[] {1.0, -1.0}) {
			Vec3 destination = this.position().add(side.scale(3.0 * direction));
			AABB box = this.getDimensions(this.getPose())
					.makeBoundingBox(destination.x, destination.y, destination.z);
			BlockPos feet = BlockPos.containing(destination);
			if (level.getWorldBorder().isWithinBounds(box)
					&& level.noCollision(this, box)
					&& !level.getBlockState(feet.below()).isAir()) {
				return destination;
			}
		}
		return null;
	}

	@Nullable
	public EndCrystal findNearestCrystal(double radius) {
		if (!(this.level() instanceof ServerLevel level)) {
			return null;
		}
		EndCrystal nearest = null;
		double nearestDistance = Double.MAX_VALUE;
		for (EndCrystal crystal : level.getEntitiesOfClass(
				EndCrystal.class,
				this.getBoundingBox().inflate(radius),
				crystal -> !crystal.isRemoved()
		)) {
			double distance = this.distanceToSqr(crystal);
			if (distance < nearestDistance) {
				nearest = crystal;
				nearestDistance = distance;
			}
		}
		return nearest;
	}

	public boolean canSafelyPopCrystal(EndCrystal crystal) {
		if (!(this.level() instanceof ServerLevel level) || this.distanceToSqr(crystal) < 100.0) {
			return false;
		}
		AABB blastArea = crystal.getBoundingBox().inflate(12.0);
		return level.getEntitiesOfClass(
				LivingEntity.class,
				blastArea,
				living -> living != this && living.isAlive() && this.isAlliedTo(living)
		).isEmpty();
	}

	public boolean hasFrontlineSupport(LivingEntity target) {
		return SquadCoordinator.hasFrontline(this, target);
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		this.shootArrowAt(target, power);
	}

	public void shootArrowAt(Entity target, float power) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}

		int arrowSlot = this.findInventorySlot(stack -> stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.ARROW));
		if (arrowSlot == NO_SLOT) {
			this.equipBackupMelee();
			return;
		}

		ItemStack bow = this.getMainHandItem();
		ItemStack ammunition = this.soldierInventory.removeItem(arrowSlot, 1);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammunition, power, bow);
		Vec3 aimPoint = target instanceof LivingEntity living
				? new Vec3(living.getX(), living.getY(0.3333333333333333), living.getZ())
				: target.getBoundingBox().getCenter();
		double x = aimPoint.x - this.getX();
		double y = aimPoint.y - arrow.getY();
		double z = aimPoint.z - this.getZ();
		double horizontal = Math.sqrt(x * x + z * z);
		Projectile.spawnProjectileUsingShoot(
				arrow,
				level,
				ammunition,
				x,
				y + horizontal * 0.2F,
				z,
				1.7F,
				12 - level.getDifficulty().getId() * 3
		);
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	private boolean switchMainHandFromInventory(Predicate<ItemStack> predicate) {
		int slot = this.findInventorySlot(predicate);
		if (slot == NO_SLOT) {
			return false;
		}

		ItemStack next = this.soldierInventory.removeItemNoUpdate(slot);
		ItemStack current = this.getMainHandItem().copy();
		this.setItemSlot(EquipmentSlot.MAINHAND, next);
		this.addToInventory(current);
		return true;
	}

	public boolean hasArrows() {
		return this.findInventorySlot(stack -> stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW)) != NO_SLOT;
	}

	public void equipSword() {
		if (!this.getMainHandItem().is(this.gearLevel.meleeWeapon())) {
			this.switchMainHandFromInventory(stack -> stack.is(this.gearLevel.meleeWeapon()));
		}
	}

	public void equipAxe() {
		if (!this.getMainHandItem().is(this.gearLevel.axeWeapon())) {
			this.switchMainHandFromInventory(stack -> stack.is(this.gearLevel.axeWeapon()));
		}
	}

	public void equipBackupMelee() {
		if (!this.getMainHandItem().is(this.gearLevel.backupWeapon())) {
			this.switchMainHandFromInventory(stack -> stack.is(this.gearLevel.backupWeapon()));
		}
	}

	public void equipBow() {
		if (!this.getMainHandItem().is(Items.BOW)) {
			this.switchMainHandFromInventory(stack -> stack.is(Items.BOW));
		}
	}

	public void equipSpear() {
		if (!this.getMainHandItem().is(this.gearLevel.spearWeapon())) {
			this.switchMainHandFromInventory(stack -> stack.is(this.gearLevel.spearWeapon()));
		}
	}

	public boolean hasInventoryItem(Item item) {
		return this.findInventorySlot(stack -> stack.is(item)) != NO_SLOT;
	}

	public int countInventoryItem(Item item) {
		return this.soldierInventory.countItem(item);
	}

	public boolean consumeInventoryItem(Item item) {
		int slot = this.findInventorySlot(stack -> stack.is(item));
		if (slot == NO_SLOT) {
			return false;
		}
		this.soldierInventory.removeItem(slot, 1);
		return true;
	}

	public int transferInventoryItem(Item item, BattleSoldierEntity recipient, int requested) {
		int moved = 0;
		while (moved < requested) {
			int slot = this.findInventorySlot(stack -> stack.is(item));
			if (slot == NO_SLOT) {
				break;
			}
			ItemStack taken = this.soldierInventory.removeItem(slot, 1);
			ItemStack remainder = recipient.soldierInventory.addItem(taken);
			if (!remainder.isEmpty()) {
				this.addToInventory(remainder);
				break;
			}
			moved++;
		}
		return moved;
	}

	@Nullable
	public BattleSoldierEntity findNearbyAlly(CombatRole role, double radius) {
		if (!(this.level() instanceof ServerLevel level)) {
			return null;
		}
		return level.getEntitiesOfClass(
				BattleSoldierEntity.class,
				this.getBoundingBox().inflate(radius),
				ally -> ally != this
						&& ally.isAlive()
						&& ally.getSquad() == this.squad
						&& ally.getCombatRole() == role
		).stream().findFirst().orElse(null);
	}

	@Nullable
	public LivingEntity findWoundedAlly(double radius, double healthFraction) {
		if (!(this.level() instanceof ServerLevel level)) {
			return null;
		}
		LivingEntity best = null;
		double bestScore = Double.MAX_VALUE;
		for (LivingEntity candidate : level.getEntitiesOfClass(
				LivingEntity.class,
				this.getBoundingBox().inflate(radius),
				candidate -> candidate != this
						&& candidate.isAlive()
						&& this.isAlliedTo(candidate)
						&& candidate.getHealth() < candidate.getMaxHealth() * healthFraction
		)) {
			double score = this.distanceToSqr(candidate) + candidate.getHealth() * 0.2;
			if (score < bestScore) {
				best = candidate;
				bestScore = score;
			}
		}
		return best;
	}

	public boolean applyInventoryPotion(
			Holder<Potion> potion,
			Item potionItem,
			LivingEntity recipient
	) {
		int slot = this.findInventorySlot(stack -> {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			return stack.is(potionItem) && contents != null && contents.is(potion);
		});
		if (slot == NO_SLOT) {
			return false;
		}
		ItemStack stack = this.soldierInventory.removeItem(slot, 1);
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (contents == null) {
			return false;
		}
		contents.applyToLivingEntity(recipient, 1.0F);
		if (this.level() instanceof ServerLevel level) {
			level.sendParticles(
					ParticleTypes.HEART,
					recipient.getX(),
					recipient.getY() + recipient.getBbHeight() * 0.5,
					recipient.getZ(),
					4,
					0.4,
					0.4,
					0.4,
					0.0
			);
		}
		return true;
	}

	public boolean applyDebuffFromInventory(
			Holder<Potion> potion,
			LivingEntity target,
			MobEffectInstance effect
	) {
		int slot = this.findInventorySlot(stack -> {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			return stack.is(Items.SPLASH_POTION) && contents != null && contents.is(potion);
		});
		if (slot == NO_SLOT || this.isAlliedTo(target)) {
			return false;
		}
		this.soldierInventory.removeItem(slot, 1);
		target.addEffect(effect, this);
		return true;
	}

	public boolean blinkBehindTarget(LivingEntity target) {
		if (!(this.level() instanceof ServerLevel level) || !this.consumeInventoryItem(Items.ENDER_PEARL)) {
			return false;
		}
		Vec3 look = target.getLookAngle();
		Vec3 horizontal = new Vec3(look.x, 0.0, look.z);
		if (horizontal.lengthSqr() < 0.01) {
			horizontal = new Vec3(1.0, 0.0, 0.0);
		}
		horizontal = horizontal.normalize();
		for (int side = 0; side < 3; side++) {
			Vec3 offset = switch (side) {
				case 1 -> new Vec3(-horizontal.z, 0.0, horizontal.x).scale(2.5);
				case 2 -> new Vec3(horizontal.z, 0.0, -horizontal.x).scale(2.5);
				default -> horizontal.scale(-2.5);
			};
			Vec3 destination = target.position().add(offset);
			AABB box = this.getDimensions(this.getPose())
					.makeBoundingBox(destination.x, destination.y, destination.z);
			BlockPos feet = BlockPos.containing(destination);
			if (!level.getWorldBorder().isWithinBounds(box)
					|| !level.noCollision(this, box)
					|| level.getBlockState(feet.below()).isAir()) {
				continue;
			}
			Entity teleported = this.teleport(new TeleportTransition(
					level,
					destination,
					Vec3.ZERO,
					target.getYRot() + 180.0F,
					this.getXRot(),
					TeleportTransition.DO_NOTHING
			));
			if (teleported != null) {
				teleported.resetFallDistance();
				this.getNavigation().stop();
				return true;
			}
		}
		this.addToInventory(new ItemStack(Items.ENDER_PEARL));
		return false;
	}

	public boolean plantTnt(int fuseTicks) {
		if (!(this.level() instanceof ServerLevel level)
				|| !level.getGameRules().get(GameRules.TNT_EXPLODES)
				|| !this.consumeInventoryItem(Items.TNT)) {
			return false;
		}
		PrimedTnt tnt = new PrimedTnt(level, this.getX(), this.getY(), this.getZ(), this);
		tnt.setFuse(fuseTicks);
		if (!level.addFreshEntity(tnt)) {
			this.addToInventory(new ItemStack(Items.TNT));
			return false;
		}
		level.playSound(null, this.blockPosition(), SoundEvents.TNT_PRIMED, SoundSource.BLOCKS, 1.0F, 1.0F);
		return true;
	}

	public boolean isHoldingAxe() {
		return this.getMainHandItem().is(this.gearLevel.axeWeapon());
	}

	public void markCriticalAttack(double multiplier) {
		this.criticalAttackMultiplier = Math.max(this.criticalAttackMultiplier, Math.max(1.0, multiplier));
	}

	@Override
	public boolean doHurtTarget(ServerLevel level, Entity target) {
		if (this.criticalAttackMultiplier <= 1.0) {
			return super.doHurtTarget(level, target);
		}

		double multiplier = this.criticalAttackMultiplier;
		this.criticalAttackMultiplier = 1.0;
		AttributeInstance attackDamage = this.getAttribute(Attributes.ATTACK_DAMAGE);
		if (attackDamage == null) {
			return super.doHurtTarget(level, target);
		}

		double baseDamage = attackDamage.getBaseValue();
		double totalDamage = attackDamage.getValue();
		attackDamage.setBaseValue(baseDamage + totalDamage * (multiplier - 1.0));
		try {
			boolean hit = super.doHurtTarget(level, target);
			if (hit) {
				this.criticalHits++;
			}
			return hit;
		} finally {
			attackDamage.setBaseValue(baseDamage);
		}
	}

	public boolean prepareCombatConsumable() {
		if (this.consumableCooldown > 0 || this.isUsingItem() || this.preparedConsumableSlot != NO_SLOT) {
			return false;
		}
		LivingEntity target = this.getTarget();
		double healthFraction = this.getHealth() / this.getMaxHealth();
		double missingHealth = 1.0 - healthFraction;
		double danger = missingHealth * 0.45;
		if (target != null) {
			double distance = this.distanceToSqr(target);
			danger += distance < 16.0 ? 0.30 : distance < 64.0 ? 0.18 : 0.05;
			if (SquadCoordinator.habits(this, target).ranged() >= 4) {
				danger += 0.12;
			}
		}
		danger = Mth.clamp(danger, 0.0, 1.0);

		double bestScore = 58.0;
		for (int slot = 0; slot < this.soldierInventory.getContainerSize(); slot++) {
			ItemStack stack = this.soldierInventory.getItem(slot);
			double score = this.consumableUtility(stack, target, healthFraction, missingHealth, danger);
			if (score > bestScore) {
				bestScore = score;
				this.preparedConsumableSlot = slot;
			}
		}
		return this.preparedConsumableSlot != NO_SLOT;
	}

	private double consumableUtility(
			ItemStack stack,
			@Nullable LivingEntity target,
			double healthFraction,
			double missingHealth,
			double danger
	) {
		if (stack.is(Items.GOLDEN_APPLE)) {
			return healthFraction <= 0.72
					? 50.0 + 40.0 * missingHealth + 25.0 * danger
					: 0.0;
		}
		if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
			return healthFraction < 0.30 || danger > 0.82
					? 95.0 + 20.0 * missingHealth
					: 10.0;
		}
		PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
		if (!stack.is(Items.POTION) || contents == null) {
			return 0.0;
		}
		if (contents.is(Potions.HEALING) || contents.is(Potions.STRONG_HEALING)) {
			return healthFraction <= 0.75
					? 55.0 + 45.0 * missingHealth + 30.0 * danger
					: 0.0;
		}
		if (contents.is(Potions.FIRE_RESISTANCE)) {
			return this.isOnFire() && !this.hasEffect(MobEffects.FIRE_RESISTANCE) ? 100.0 : 0.0;
		}
		if (contents.is(Potions.STRENGTH)) {
			return target != null
					&& !this.hasEffect(MobEffects.STRENGTH)
					&& this.distanceToSqr(target) <= 144.0
					? 62.0 + danger * 12.0
					: 0.0;
		}
		if (contents.is(Potions.SWIFTNESS)) {
			return target != null
					&& !this.hasEffect(MobEffects.SPEED)
					&& this.distanceToSqr(target) >= 64.0
					? 60.0 + danger * 10.0
					: 0.0;
		}
		return 0.0;
	}

	public boolean hasPreparedCombatConsumable() {
		return this.preparedConsumableSlot >= 0
				&& this.preparedConsumableSlot < this.soldierInventory.getContainerSize()
				&& !this.soldierInventory.getItem(this.preparedConsumableSlot).isEmpty();
	}

	public void beginCombatConsumable() {
		if (!this.hasPreparedCombatConsumable()) {
			return;
		}

		this.savedOffhand = this.getOffhandItem().copy();
		ItemStack consumable = this.soldierInventory.removeItem(this.preparedConsumableSlot, 1);
		this.activeConsumable = consumable.copy();
		this.setItemSlot(EquipmentSlot.OFFHAND, consumable);
		this.startUsingItem(InteractionHand.OFF_HAND);
	}

	public void finishCombatConsumable() {
		if (this.activeConsumable.isEmpty()) {
			this.preparedConsumableSlot = NO_SLOT;
			return;
		}

		ItemStack remaining = this.getOffhandItem().copy();
		boolean consumed = !ItemStack.isSameItemSameComponents(remaining, this.activeConsumable);
		if (this.isUsingItem()) {
			this.stopUsingItem();
		}

		if (!consumed && !remaining.isEmpty()) {
			this.addToInventory(remaining);
		} else if (remaining.is(Items.GLASS_BOTTLE)) {
			this.addToInventory(remaining);
		}

		this.setItemSlot(EquipmentSlot.OFFHAND, this.savedOffhand);
		this.savedOffhand = ItemStack.EMPTY;
		this.activeConsumable = ItemStack.EMPTY;
		this.preparedConsumableSlot = NO_SLOT;
		this.consumableCooldown = consumed ? 20 * 12 : 40;
	}

	private int findPotionSlot(Holder<Potion> potion) {
		return this.findInventorySlot(stack -> {
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			return stack.is(Items.POTION) && contents != null && contents.is(potion);
		});
	}

	private void maintainOffhandEquipment() {
		if (this.isUsingItem()) {
			return;
		}

		LivingEntity threat = this.getTarget();
		boolean predictedLethal = threat != null
				&& this.estimatedIncomingDamage(threat)
						>= this.getHealth() + this.getAbsorptionAmount();
		if ((this.getHealth() <= this.getMaxHealth() * 0.3F || predictedLethal)
				&& !this.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			int totemSlot = this.findInventorySlot(stack -> stack.is(Items.TOTEM_OF_UNDYING));
			if (totemSlot != NO_SLOT) {
				this.addToInventory(this.getOffhandItem().copy());
				this.setItemSlot(EquipmentSlot.OFFHAND, this.soldierInventory.removeItem(totemSlot, 1));
				return;
			}
		}

		if (this.getHealth() > this.getMaxHealth() * 0.3F
				&& !predictedLethal
				&& this.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			int shieldSlot = this.findInventorySlot(stack -> stack.is(Items.SHIELD));
			if (shieldSlot != NO_SLOT) {
				this.addToInventory(this.getOffhandItem().copy());
				this.setItemSlot(EquipmentSlot.OFFHAND, this.soldierInventory.removeItemNoUpdate(shieldSlot));
				return;
			}
		}

		if (!this.getOffhandItem().isEmpty()) {
			return;
		}

		int totemSlot = this.findInventorySlot(stack -> stack.is(Items.TOTEM_OF_UNDYING));
		if (totemSlot != NO_SLOT) {
			this.setItemSlot(EquipmentSlot.OFFHAND, this.soldierInventory.removeItem(totemSlot, 1));
			return;
		}

		if (!this.getMainHandItem().is(Items.BOW)) {
			int shieldSlot = this.findInventorySlot(stack -> stack.is(Items.SHIELD));
			if (shieldSlot != NO_SLOT) {
				this.setItemSlot(EquipmentSlot.OFFHAND, this.soldierInventory.removeItemNoUpdate(shieldSlot));
			}
		}
	}

	public int getRangerTowerCooldown() {
		return this.rangerTowerCooldown;
	}

	public void setRangerTowerCooldown(int ticks) {
		this.rangerTowerCooldown = Math.max(0, ticks);
	}

	public boolean hasSpentRangerTowerThisEngagement() {
		return this.rangerTowerSpent;
	}

	public void recordRangerTowerBlock(BlockPos top) {
		this.rangerPerchTop = top.immutable();
		this.rangerTowerSpent = true;
		this.rangerTargetlessTicks = 0;
	}

	public boolean isHoldingRangerPerch() {
		if (this.combatRole != CombatRole.RANGER || this.rangerPerchTop == null) {
			return false;
		}
		BlockState perchState = this.level().getBlockState(this.rangerPerchTop);
		if (!this.placedBlocks.contains(this.rangerPerchTop.asLong())
				|| !perchState.isCollisionShapeFullBlock(this.level(), this.rangerPerchTop)) {
			return false;
		}
		double centerX = this.rangerPerchTop.getX() + 0.5;
		double centerZ = this.rangerPerchTop.getZ() + 0.5;
		double deltaX = this.getX() - centerX;
		double deltaZ = this.getZ() - centerZ;
		return deltaX * deltaX + deltaZ * deltaZ <= 1.0
				&& this.getY() >= this.rangerPerchTop.getY() + 0.75
				&& this.getY() <= this.rangerPerchTop.getY() + 2.5;
	}

	public boolean shouldHoldRangerPerch() {
		return this.isHoldingRangerPerch();
	}

	public int getWebTrapCooldown() {
		return this.webTrapCooldown;
	}

	public void setWebTrapCooldown(int ticks) {
		this.webTrapCooldown = Math.max(0, ticks);
	}

	public boolean placePillarBlock(BlockPos pos) {
		boolean placed = this.placeTacticalBlock(pos);
		if (placed) {
			this.recordRangerTowerBlock(pos);
		}
		return placed;
	}

	public boolean hasCobwebs() {
		return this.findInventorySlot(stack -> stack.is(Items.COBWEB)) != NO_SLOT;
	}

	public boolean canPlaceCobweb(BlockPos pos, LivingEntity intendedVictim) {
		if (!(this.level() instanceof ServerLevel level)
				|| !level.getGameRules().get(GameRules.MOB_GRIEFING)
				|| !level.mayInteract(this, pos)
				|| !level.getWorldBorder().isWithinBounds(pos)
				|| !level.getBlockState(pos).canBeReplaced()
				|| !Blocks.COBWEB.defaultBlockState().canSurvive(level, pos)
				|| !this.hasCobwebs()
				|| this.placedBlocks.size() >= 48) {
			return false;
		}

		AABB safetyArea = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos)).inflate(1.0);
		return level.getEntitiesOfClass(
				LivingEntity.class,
				safetyArea,
				entity -> entity != intendedVictim && entity != this && this.isAlliedTo(entity)
		).isEmpty();
	}

	public boolean placeCobwebTrap(BlockPos pos, LivingEntity intendedVictim) {
		if (!(this.level() instanceof ServerLevel level) || !this.canPlaceCobweb(pos, intendedVictim)) {
			return false;
		}
		int webSlot = this.findInventorySlot(stack -> stack.is(Items.COBWEB));
		BlockState cobweb = Blocks.COBWEB.defaultBlockState();
		if (webSlot == NO_SLOT || !level.setBlock(pos, cobweb, 3)) {
			return false;
		}

		this.soldierInventory.removeItem(webSlot, 1);
		this.placedBlocks.add(pos.asLong());
		SoundType sounds = cobweb.getSoundType();
		level.playSound(null, pos, sounds.getPlaceSound(), SoundSource.BLOCKS, sounds.getVolume(), sounds.getPitch());
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(this, cobweb));
		this.swing(InteractionHand.MAIN_HAND);
		return true;
	}

	public boolean canBuild() {
		if (!(this.level() instanceof ServerLevel level)) {
			return false;
		}
		return this.findBuildingBlockSlot() != NO_SLOT
				&& this.placedBlocks.size() < 32
				&& level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	public boolean canPlaceTacticalBlock(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level)
				|| !this.canBuild()
				|| !level.mayInteract(this, pos)
				|| !level.getWorldBorder().isWithinBounds(pos)
				|| !level.getBlockState(pos).isAir()) {
			return false;
		}

		BlockState placementState = this.getBuildingBlockState();
		AABB blockBox = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos));
		return !placementState.isAir()
				&& placementState.canSurvive(level, pos)
				&& level.getEntities(this, blockBox).isEmpty();
	}

	public boolean placeTacticalBlock(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level) || !this.canPlaceTacticalBlock(pos)) {
			return false;
		}

		int blockSlot = this.findBuildingBlockSlot();
		BlockState placementState = this.getBuildingBlockState();
		if (blockSlot == NO_SLOT || placementState.isAir() || !level.setBlock(pos, placementState, 3)) {
			return false;
		}

		this.soldierInventory.removeItem(blockSlot, 1);
		this.placedBlocks.add(pos.asLong());
		SoundType sounds = placementState.getSoundType();
		level.playSound(
				null,
				pos,
				sounds.getPlaceSound(),
				SoundSource.BLOCKS,
				sounds.getVolume(),
				sounds.getPitch()
		);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(this, placementState));
		this.swing(InteractionHand.MAIN_HAND);
		return true;
	}

	private int findBuildingBlockSlot() {
		return this.findInventorySlot(stack -> stack.is(Items.COBBLESTONE) || stack.is(Items.OAK_PLANKS));
	}

	private BlockState getBuildingBlockState() {
		int slot = this.findBuildingBlockSlot();
		if (slot == NO_SLOT) {
			return Blocks.AIR.defaultBlockState();
		}
		return this.soldierInventory.getItem(slot).is(Items.OAK_PLANKS)
				? Blocks.OAK_PLANKS.defaultBlockState()
				: Blocks.COBBLESTONE.defaultBlockState();
	}

	public boolean canBreakBlock(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level)
				|| !level.getGameRules().get(GameRules.MOB_GRIEFING)
				|| !level.mayInteract(this, pos)
				|| !level.getWorldBorder().isWithinBounds(pos)) {
			return false;
		}

		BlockState state = level.getBlockState(pos);
		float hardness = state.getDestroySpeed(level, pos);
		return !state.isAir()
				&& hardness >= 0.0F
				&& hardness <= this.gearLevel.maxBreakHardness()
				&& !state.hasBlockEntity()
				&& !state.is(Blocks.BEDROCK)
				&& !state.is(Blocks.BARRIER);
	}

	public int getBreakTicks(BlockState state, BlockPos pos) {
		float hardness = Math.max(0.1F, state.getDestroySpeed(this.level(), pos));
		return Math.max(10, Math.min(180, (int) Math.ceil(hardness * 28.0F / this.gearLevel.miningPower())));
	}

	public void onBlockBreached(BlockState state) {
		if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.STONE)) {
			this.addToInventory(new ItemStack(Items.COBBLESTONE));
		} else if (state.is(BlockTags.PLANKS)) {
			this.addToInventory(new ItemStack(Items.OAK_PLANKS));
		} else if (state.is(BlockTags.DIRT)) {
			this.addToInventory(new ItemStack(Items.DIRT));
		}
	}

	private int findInventorySlot(Predicate<ItemStack> predicate) {
		for (int slot = 0; slot < this.soldierInventory.getContainerSize(); slot++) {
			ItemStack stack = this.soldierInventory.getItem(slot);
			if (!stack.isEmpty() && predicate.test(stack)) {
				return slot;
			}
		}
		return NO_SLOT;
	}

	private void addToInventory(ItemStack stack) {
		if (!stack.isEmpty()) {
			this.soldierInventory.addItem(stack);
		}
	}

	private void maintainCriticalClassSupplies() {
		switch (this.combatRole) {
			case RANGER -> {
				this.ensureRoleWeapon(Items.BOW);
				this.ensureInventoryCount(Items.ARROW, 32);
				this.ensureInventoryCount(Items.COBBLESTONE, 16);
				this.ensureInventoryCount(Items.OAK_PLANKS, 8);
			}
			case TRAPPER -> {
				int webs = switch (this.gearLevel) {
					case ONE, TWO, THREE -> 4;
					case FOUR -> 6;
					case FIVE -> 10;
					case SIX -> 14;
				};
				this.ensureInventoryCount(Items.COBWEB, webs);
			}
			case MEDIC -> {
				this.ensurePotionCount(Items.POTION, Potions.STRONG_HEALING, 2);
				this.ensurePotionCount(Items.POTION, Potions.HEALING, 1);
				this.ensurePotionCount(Items.POTION, Potions.REGENERATION, 1);
			}
			case ENGINEER -> {
				this.ensureInventoryCount(Items.COBBLESTONE, 24);
				this.ensureInventoryCount(Items.OAK_PLANKS, 12);
				this.ensureInventoryCount(Items.LADDER, 16);
			}
			case LANCER -> this.ensureRoleWeapon(this.gearLevel.spearWeapon());
			case DUELIST -> this.ensureRoleWeapon(this.gearLevel.meleeWeapon());
			case ALCHEMIST -> {
				this.ensurePotionCount(Items.SPLASH_POTION, Potions.POISON, 2);
				this.ensurePotionCount(Items.SPLASH_POTION, Potions.WEAKNESS, 2);
				this.ensurePotionCount(Items.SPLASH_POTION, Potions.SLOWNESS, 2);
			}
			case ENDER_SKIRMISHER -> this.ensureInventoryCount(Items.ENDER_PEARL, 5);
			case DEMOLITIONIST -> {
				this.ensureInventoryCount(Items.TNT, 5);
				this.ensureRoleWeapon(this.gearLevel.axeWeapon());
				this.ensureRoleWeapon(this.gearLevel.meleeWeapon());
			}
			case VANGUARD -> {
				this.ensureRoleWeapon(this.gearLevel.meleeWeapon());
				this.ensureRoleWeapon(this.gearLevel.axeWeapon());
				this.ensureRoleWeapon(Items.SHIELD);
			}
			case BRUTE -> {
				this.ensureRoleWeapon(this.gearLevel.meleeWeapon());
				this.ensureRoleWeapon(this.gearLevel.axeWeapon());
			}
		}
	}

	private void ensureRoleWeapon(Item item) {
		if (this.getMainHandItem().is(item)
				|| this.getOffhandItem().is(item)
				|| this.hasInventoryItem(item)) {
			return;
		}
		ItemStack stack = this.randomizedStack(item);
		if (this.gearLevel == GearLevel.SIX && this.level() instanceof ServerLevel level) {
			this.enchantTierSixStack(
					stack,
					level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
			);
		}
		this.addToInventory(stack);
	}

	private void ensureInventoryCount(Item item, int minimum) {
		int missing = minimum - this.countInventoryItem(item);
		if (missing <= 0) {
			return;
		}
		if (new ItemStack(item).getMaxStackSize() > 1) {
			this.addToInventory(new ItemStack(item, missing));
		} else {
			for (int index = 0; index < missing; index++) {
				this.addToInventory(new ItemStack(item));
			}
		}
	}

	private void ensurePotionCount(Item item, Holder<Potion> potion, int minimum) {
		int count = 0;
		for (int slot = 0; slot < this.soldierInventory.getContainerSize(); slot++) {
			ItemStack stack = this.soldierInventory.getItem(slot);
			PotionContents contents = stack.get(DataComponents.POTION_CONTENTS);
			if (stack.is(item) && contents != null && contents.is(potion)) {
				count += stack.getCount();
			}
		}
		for (int index = count; index < minimum; index++) {
			this.addToInventory(PotionContents.createItemStack(item, potion));
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			if (this.tickCount % 5 == Math.floorMod(this.getId(), 5)) {
				this.maintainCriticalClassSupplies();
			}
			if (this.tickCount % 4 == Math.floorMod(this.getId(), 4)) {
				SquadCoordinator.heartbeat(this);
				LivingEntity sharedTarget = SquadCoordinator.sharedTarget(this);
				if (sharedTarget != null
						&& (this.getTarget() == null
								|| !this.getTarget().getUUID().equals(sharedTarget.getUUID()))) {
					this.setTarget(sharedTarget);
				}
			}
			if (this.consumableCooldown > 0) {
				this.consumableCooldown--;
			}
			if (this.rangerTowerCooldown > 0) {
				this.rangerTowerCooldown--;
			}
			if (this.webTrapCooldown > 0) {
				this.webTrapCooldown--;
			}
			if (this.combatRole == CombatRole.RANGER) {
				if (this.getTarget() == null) {
					this.rangerTargetlessTicks++;
					if (this.rangerTargetlessTicks >= 100 && !this.isHoldingRangerPerch()) {
						this.rangerTowerSpent = false;
						this.rangerPerchTop = null;
					}
				} else {
					this.rangerTargetlessTicks = 0;
				}
			}
			this.maintainOffhandEquipment();
		}
	}

	@Override
	public SpawnGroupData finalizeSpawn(
			ServerLevelAccessor level,
			DifficultyInstance difficulty,
			EntitySpawnReason spawnReason,
			@Nullable SpawnGroupData spawnData
	) {
		SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnReason, spawnData);
		if (!this.initialized) {
			this.initializeSoldier(SoldierSquad.TRAINING, GearLevel.ONE);
		}
		return result;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("SoldierSquad", this.squad.id());
		output.putInt("GearLevel", this.gearLevel.id());
		output.putString("CombatRole", this.combatRole.id());
		output.putBoolean("Archer", this.isArcher());
		output.putBoolean("Initialized", this.initialized);
		output.putInt("ConsumableCooldown", this.consumableCooldown);
		output.putInt("RangerTowerCooldown", this.rangerTowerCooldown);
		output.putInt("WebTrapCooldown", this.webTrapCooldown);
		output.putBoolean("RangerTowerSpent", this.rangerTowerSpent);
		if (this.rangerPerchTop != null) {
			output.store("RangerPerchTop", BlockPos.CODEC, this.rangerPerchTop);
		}
		output.putInt("GoldenApples", this.soldierInventory.countItem(Items.GOLDEN_APPLE));
		output.putInt(
				"BuildingBlocks",
				this.soldierInventory.countItem(Items.COBBLESTONE)
						+ this.soldierInventory.countItem(Items.OAK_PLANKS)
		);
		ContainerHelper.saveAllItems(output.child(INVENTORY_TAG), this.soldierInventory.getItems());
		ValueOutput.TypedOutputList<Long> blocks = output.list("PlacedBlocks", Codec.LONG);
		for (long pos : this.placedBlocks) {
			blocks.add(pos);
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput input) {
		super.readAdditionalSaveData(input);
		this.squad = SoldierSquad.byId(input.getStringOr("SoldierSquad", SoldierSquad.TRAINING.id()));
		this.gearLevel = GearLevel.byId(input.getIntOr("GearLevel", 1));
		boolean legacyArcher = input.getBooleanOr("Archer", false);
		this.combatRole = CombatRole.byId(input.getStringOr("CombatRole", ""), legacyArcher);
		this.initialized = input.getBooleanOr("Initialized", true);
		this.consumableCooldown = Math.max(
				0,
				input.getIntOr("ConsumableCooldown", input.getIntOr("AppleCooldown", 0))
		);
		this.rangerTowerCooldown = Math.max(0, input.getIntOr("RangerTowerCooldown", 0));
		this.webTrapCooldown = Math.max(0, input.getIntOr("WebTrapCooldown", 0));
		this.rangerTowerSpent = input.getBooleanOr("RangerTowerSpent", false);
		this.rangerPerchTop = input.read("RangerPerchTop", BlockPos.CODEC).orElse(null);

		this.soldierInventory.clearContent();
		ContainerHelper.loadAllItems(input.childOrEmpty(INVENTORY_TAG), this.soldierInventory.getItems());
		if (this.soldierInventory.isEmpty()) {
			int legacyApples = Math.max(0, input.getIntOr("GoldenApples", 0));
			int legacyBlocks = Math.max(0, input.getIntOr("BuildingBlocks", 0));
			this.addToInventory(new ItemStack(Items.GOLDEN_APPLE, legacyApples));
			this.addToInventory(new ItemStack(Items.COBBLESTONE, legacyBlocks));
			if (this.isArcher()) {
				this.addToInventory(this.randomizedStack(this.gearLevel.backupWeapon()));
				this.addToInventory(new ItemStack(Items.ARROW, 24));
			}
		}

		this.placedBlocks.clear();
		input.listOrEmpty("PlacedBlocks", Codec.LONG).forEach(value -> this.placedBlocks.add(value.longValue()));
		this.configureGuaranteedDrops();
		this.updateAttributes(false);
		this.updateDisplayName();
	}

	@Override
	protected void dropEquipment(ServerLevel level) {
		super.dropEquipment(level);
		for (ItemStack stack : this.soldierInventory.removeAllItems()) {
			if (!stack.isEmpty()) {
				this.spawnAtLocation(level, stack);
			}
		}
	}

	@Override
	public boolean shouldDropExperience() {
		return false;
	}

	@Override
	protected int getBaseExperienceReward(ServerLevel level) {
		return 0;
	}

	@Override
	public void remove(Entity.RemovalReason reason) {
		SquadCoordinator.unregister(this);
		if (reason == Entity.RemovalReason.KILLED
				|| reason == Entity.RemovalReason.DISCARDED
				|| reason == Entity.RemovalReason.CHANGED_DIMENSION) {
			this.cleanupTacticalBlocks();
			BattleTeams.removeSoldier(this);
		}
		super.remove(reason);
	}

	public void cleanupTacticalBlocks() {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}

		for (long packedPos : this.placedBlocks) {
			BlockPos pos = BlockPos.of(packedPos);
			BlockState state = level.getBlockState(pos);
			if (state.is(Blocks.COBBLESTONE)
					|| state.is(Blocks.OAK_PLANKS)
					|| state.is(Blocks.COBWEB)) {
				level.removeBlock(pos, false);
			}
		}
		this.placedBlocks.clear();
	}
}

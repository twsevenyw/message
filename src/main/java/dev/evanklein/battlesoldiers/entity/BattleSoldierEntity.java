package dev.evanklein.battlesoldiers.entity;

import com.mojang.serialization.Codec;
import dev.evanklein.battlesoldiers.battle.BattleTeams;
import dev.evanklein.battlesoldiers.battle.CombatRole;
import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.battle.SoldierSquad;
import dev.evanklein.battlesoldiers.entity.ai.BreachObstacleGoal;
import dev.evanklein.battlesoldiers.entity.ai.ObstructionAwareTargetGoal;
import dev.evanklein.battlesoldiers.entity.ai.SoldierMeleeAttackGoal;
import dev.evanklein.battlesoldiers.entity.ai.TacticalBuildGoal;
import dev.evanklein.battlesoldiers.entity.ai.UseCombatConsumableGoal;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
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
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.function.Predicate;

public class BattleSoldierEntity extends Zombie implements RangedAttackMob {
	private static final String INVENTORY_TAG = "SoldierInventory";
	private static final int NO_SLOT = -1;

	private final RangedBowAttackGoal<BattleSoldierEntity> bowGoal =
			new RangedBowAttackGoal<>(this, 1.08, 22, 20.0F);
	private final SoldierMeleeAttackGoal meleeGoal = new SoldierMeleeAttackGoal(this);
	private final SimpleContainer soldierInventory = new SimpleContainer(Inventory.INVENTORY_SIZE);
	private final LongSet placedBlocks = new LongOpenHashSet();

	private SoldierSquad squad = SoldierSquad.TRAINING;
	private GearLevel gearLevel = GearLevel.ONE;
	private CombatRole combatRole = CombatRole.SWORDSMAN;
	private boolean initialized;
	private int consumableCooldown;
	private int preparedConsumableSlot = NO_SLOT;
	private int weaponSwitchCooldown;
	private ItemStack activeConsumable = ItemStack.EMPTY;
	private ItemStack savedOffhand = ItemStack.EMPTY;

	public BattleSoldierEntity(EntityType<? extends BattleSoldierEntity> entityType, Level level) {
		super(entityType, level);
		this.reassessWeaponGoal();
	}

	public static AttributeSupplier.Builder createSoldierAttributes() {
		return Zombie.createAttributes()
				.add(Attributes.MAX_HEALTH, 24.0)
				.add(Attributes.MOVEMENT_SPEED, 0.30)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 48.0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new UseCombatConsumableGoal(this));
		this.goalSelector.addGoal(2, new BreachObstacleGoal(this));
		this.goalSelector.addGoal(3, new TacticalBuildGoal(this));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, LivingEntity.class, 10.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new ObstructionAwareTargetGoal<>(
				this,
				Player.class,
				5,
				(target, level) -> target instanceof Player player && this.isValidPlayerTarget(player)
		));
		this.targetSelector.addGoal(3, new ObstructionAwareTargetGoal<>(
				this,
				BattleSoldierEntity.class,
				5,
				(target, level) -> target instanceof BattleSoldierEntity soldier
						&& this.isValidSoldierTarget(soldier)
		));
	}

	public void initializeSoldier(SoldierSquad squad, GearLevel gearLevel) {
		this.squad = squad;
		this.gearLevel = gearLevel;
		this.combatRole = this.chooseCombatRole();
		this.initialized = true;
		this.generateRandomLoadout();
		BattleTeams.assignSoldier(this);
	}

	public void initializeSoldier(SoldierSquad squad, GearLevel gearLevel, boolean archer) {
		this.squad = squad;
		this.gearLevel = gearLevel;
		this.combatRole = archer && gearLevel.archerEligible()
				? CombatRole.ARCHER
				: this.chooseMeleeRole();
		this.initialized = true;
		this.generateRandomLoadout();
		BattleTeams.assignSoldier(this);
	}

	private CombatRole chooseCombatRole() {
		if (this.gearLevel.archerEligible() && this.getRandom().nextFloat() < this.gearLevel.archerChance()) {
			return CombatRole.ARCHER;
		}
		return this.chooseMeleeRole();
	}

	private CombatRole chooseMeleeRole() {
		return this.getRandom().nextFloat() < 0.34F ? CombatRole.AXE_FIGHTER : CombatRole.SWORDSMAN;
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
			this.addToInventory(new ItemStack(Items.ARROW, 18 + this.getRandom().nextInt(25)));
			if (this.gearLevel.id() >= 4 && this.getRandom().nextFloat() < 0.22F) {
				this.addToInventory(new ItemStack(Items.SPECTRAL_ARROW, 4 + this.getRandom().nextInt(9)));
			}
		}

		int blockCount = Math.max(3, this.gearLevel.buildingBlocks() / 2)
				+ this.getRandom().nextInt(this.gearLevel.buildingBlocks() + 1);
		int cobblestone = Math.max(1, (int) Math.ceil(blockCount * 0.65));
		this.addToInventory(new ItemStack(Items.COBBLESTONE, cobblestone));
		this.addToInventory(new ItemStack(Items.OAK_PLANKS, Math.max(1, blockCount - cobblestone)));

		float appleChance = 0.22F + this.gearLevel.id() * 0.11F;
		if (this.getRandom().nextFloat() < appleChance) {
			int apples = 1 + this.getRandom().nextInt(Math.max(1, this.gearLevel.goldenApples()));
			this.addToInventory(new ItemStack(Items.GOLDEN_APPLE, apples));
		}
		this.addToInventory(new ItemStack(Items.COOKED_BEEF, 2 + this.getRandom().nextInt(3 + this.gearLevel.id())));

		boolean carriesShield = this.getRandom().nextFloat() < this.gearLevel.shieldChance();
		boolean carriesTotem = this.getRandom().nextFloat() < this.gearLevel.totemChance();
		if (carriesTotem) {
			this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.TOTEM_OF_UNDYING));
			if (carriesShield) {
				this.addToInventory(new ItemStack(Items.SHIELD));
			}
		} else if (carriesShield && !this.isArcher()) {
			this.setItemSlot(EquipmentSlot.OFFHAND, this.randomizedStack(Items.SHIELD));
		} else if (carriesShield) {
			this.addToInventory(this.randomizedStack(Items.SHIELD));
		}

		if (this.gearLevel == GearLevel.FIVE && this.getRandom().nextFloat() < 0.08F) {
			this.addToInventory(new ItemStack(Items.TOTEM_OF_UNDYING));
		}
		this.addRandomPotions();

		this.configureGuaranteedDrops();
		this.setCanPickUpLoot(true);
		this.setPersistenceRequired();
		this.updateAttributes();
		this.updateDisplayName();
		this.reassessWeaponGoal();
	}

	private void equipRandomArmor(EquipmentSlot slot) {
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
			case ARCHER -> Items.BOW;
			case AXE_FIGHTER -> this.gearLevel.axeWeapon();
			case SWORDSMAN -> this.gearLevel.meleeWeapon();
		};
	}

	private ItemStack randomizedStack(Item item) {
		if (item == Items.AIR) {
			return ItemStack.EMPTY;
		}
		ItemStack stack = new ItemStack(item);
		if (stack.isDamageableItem()) {
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
		if (this.gearLevel.id() >= 4 && this.getRandom().nextFloat() < 0.36F) {
			this.addToInventory(this.createRandomPotion());
		}
	}

	private ItemStack createRandomPotion() {
		Holder<Potion> potion = switch (this.getRandom().nextInt(5)) {
			case 0 -> Potions.STRENGTH;
			case 1 -> Potions.SWIFTNESS;
			case 2 -> Potions.REGENERATION;
			case 3 -> Potions.FIRE_RESISTANCE;
			default -> Potions.HEALING;
		};
		return PotionContents.createItemStack(Items.POTION, potion);
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

	private void updateAttributes() {
		AttributeInstance maxHealth = this.getAttribute(Attributes.MAX_HEALTH);
		AttributeInstance movement = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (maxHealth != null) {
			maxHealth.setBaseValue(this.gearLevel.maxHealth());
			this.setHealth((float) this.gearLevel.maxHealth());
		}
		if (movement != null) {
			movement.setBaseValue(this.gearLevel.movementSpeed());
		}
	}

	private void updateDisplayName() {
		this.setCustomName(Component.literal(
				this.squad.displayName() + " " + this.combatRole.displayName() + " • Gear " + this.gearLevel.id()
		).withStyle(this.squad.color()));
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
		this.squad = squad;
		this.updateDisplayName();
		BattleTeams.assignSoldier(this);
		this.setTarget(null);
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

	public void reassessWeaponGoal() {
		if (this.level().isClientSide() || this.meleeGoal == null || this.bowGoal == null) {
			return;
		}
		this.goalSelector.removeGoal(this.meleeGoal);
		this.goalSelector.removeGoal(this.bowGoal);
		if (this.isArcher() && this.getMainHandItem().is(Items.BOW) && this.hasArrows()) {
			this.goalSelector.addGoal(4, this.bowGoal);
		} else {
			this.goalSelector.addGoal(4, this.meleeGoal);
		}
	}

	@Override
	public void onEquipItem(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack) {
		super.onEquipItem(slot, oldStack, newStack);
		if (slot == EquipmentSlot.MAINHAND && !this.level().isClientSide()) {
			this.reassessWeaponGoal();
		}
	}

	@Override
	public void performRangedAttack(LivingEntity target, float power) {
		if (!(this.level() instanceof ServerLevel level)) {
			return;
		}

		int arrowSlot = this.findInventorySlot(stack -> stack.is(Items.SPECTRAL_ARROW) || stack.is(Items.ARROW));
		if (arrowSlot == NO_SLOT) {
			this.switchArcherToMelee();
			return;
		}

		ItemStack bow = this.getMainHandItem();
		ItemStack ammunition = this.soldierInventory.removeItem(arrowSlot, 1);
		AbstractArrow arrow = ProjectileUtil.getMobArrow(this, ammunition, power, bow);
		double x = target.getX() - this.getX();
		double y = target.getY(0.3333333333333333) - arrow.getY();
		double z = target.getZ() - this.getZ();
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

	private void updateAdaptiveCombatEquipment() {
		if (!this.isArcher() || this.weaponSwitchCooldown-- > 0) {
			return;
		}
		this.weaponSwitchCooldown = 8;

		LivingEntity target = this.getTarget();
		if (target == null || !target.isAlive()) {
			return;
		}

		double distance = this.distanceToSqr(target);
		if (this.getMainHandItem().is(Items.BOW) && (distance <= 16.0 || !this.hasArrows())) {
			this.switchArcherToMelee();
		} else if (!this.getMainHandItem().is(Items.BOW) && distance >= 64.0 && this.hasArrows()) {
			this.switchMainHandFromInventory(stack -> stack.is(Items.BOW));
		}
	}

	private void switchArcherToMelee() {
		this.switchMainHandFromInventory(stack -> stack.is(this.gearLevel.backupWeapon()));
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
		this.reassessWeaponGoal();
		return true;
	}

	private boolean hasArrows() {
		return this.findInventorySlot(stack -> stack.is(Items.ARROW) || stack.is(Items.SPECTRAL_ARROW)) != NO_SLOT;
	}

	public boolean prepareCombatConsumable() {
		if (this.consumableCooldown > 0 || this.isUsingItem() || this.preparedConsumableSlot != NO_SLOT) {
			return false;
		}

		if (this.getHealth() <= this.getMaxHealth() * 0.5F) {
			this.preparedConsumableSlot = this.findInventorySlot(stack -> stack.is(Items.GOLDEN_APPLE));
			if (this.preparedConsumableSlot == NO_SLOT) {
				this.preparedConsumableSlot = this.findPotionSlot(Potions.HEALING);
			}
			if (this.preparedConsumableSlot == NO_SLOT) {
				this.preparedConsumableSlot = this.findPotionSlot(Potions.REGENERATION);
			}
		}

		LivingEntity target = this.getTarget();
		if (this.preparedConsumableSlot == NO_SLOT && this.isOnFire() && !this.hasEffect(MobEffects.FIRE_RESISTANCE)) {
			this.preparedConsumableSlot = this.findPotionSlot(Potions.FIRE_RESISTANCE);
		}
		if (this.preparedConsumableSlot == NO_SLOT
				&& target != null
				&& !this.hasEffect(MobEffects.STRENGTH)
				&& this.distanceToSqr(target) <= 144.0) {
			this.preparedConsumableSlot = this.findPotionSlot(Potions.STRENGTH);
		}
		if (this.preparedConsumableSlot == NO_SLOT
				&& target != null
				&& !this.hasEffect(MobEffects.SPEED)
				&& this.distanceToSqr(target) >= 64.0) {
			this.preparedConsumableSlot = this.findPotionSlot(Potions.SWIFTNESS);
		}

		return this.preparedConsumableSlot != NO_SLOT;
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

		if (this.getHealth() <= this.getMaxHealth() * 0.3F
				&& !this.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
			int totemSlot = this.findInventorySlot(stack -> stack.is(Items.TOTEM_OF_UNDYING));
			if (totemSlot != NO_SLOT) {
				this.addToInventory(this.getOffhandItem().copy());
				this.setItemSlot(EquipmentSlot.OFFHAND, this.soldierInventory.removeItem(totemSlot, 1));
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

	public boolean canBuild() {
		if (!(this.level() instanceof ServerLevel level)) {
			return false;
		}
		return this.findBuildingBlockSlot() != NO_SLOT
				&& this.placedBlocks.size() < 12
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

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide()) {
			if (this.consumableCooldown > 0) {
				this.consumableCooldown--;
			}
			this.updateAdaptiveCombatEquipment();
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
		this.updateDisplayName();
		this.reassessWeaponGoal();
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
	public void remove(Entity.RemovalReason reason) {
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
			if (state.is(Blocks.COBBLESTONE) || state.is(Blocks.OAK_PLANKS)) {
				level.removeBlock(pos, false);
			}
		}
		this.placedBlocks.clear();
	}
}

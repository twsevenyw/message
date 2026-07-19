package dev.evanklein.battlesoldiers.entity;

import com.mojang.serialization.Codec;
import dev.evanklein.battlesoldiers.battle.BattleTeams;
import dev.evanklein.battlesoldiers.battle.GearLevel;
import dev.evanklein.battlesoldiers.battle.SoldierSquad;
import dev.evanklein.battlesoldiers.entity.ai.BreachObstacleGoal;
import dev.evanklein.battlesoldiers.entity.ai.TacticalBuildGoal;
import dev.evanklein.battlesoldiers.entity.ai.UseGoldenAppleGoal;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RangedBowAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.ZombieAttackGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class BattleSoldierEntity extends Zombie implements RangedAttackMob {
	private final RangedBowAttackGoal<BattleSoldierEntity> bowGoal =
			new RangedBowAttackGoal<>(this, 1.05, 24, 18.0F);
	private final ZombieAttackGoal meleeGoal = new ZombieAttackGoal(this, 1.15, true);
	private final LongSet placedBlocks = new LongOpenHashSet();

	private SoldierSquad squad = SoldierSquad.TRAINING;
	private GearLevel gearLevel = GearLevel.ONE;
	private boolean archer;
	private boolean initialized;
	private int goldenApples;
	private int buildingBlocks;
	private int appleCooldown;
	private ItemStack savedOffhand = ItemStack.EMPTY;

	public BattleSoldierEntity(EntityType<? extends BattleSoldierEntity> entityType, Level level) {
		super(entityType, level);
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
		this.goalSelector.addGoal(1, new UseGoldenAppleGoal(this));
		this.goalSelector.addGoal(2, new BreachObstacleGoal(this));
		this.goalSelector.addGoal(3, new TacticalBuildGoal(this));
		this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.9));
		this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, LivingEntity.class, 10.0F));
		this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));

		this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
				this,
				Player.class,
				5,
				true,
				false,
				(target, level) -> this.isValidPlayerTarget(target)
		));
		this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
				this,
				BattleSoldierEntity.class,
				5,
				true,
				false,
				(target, level) -> this.isValidSoldierTarget(target)
		));

		this.reassessWeaponGoal();
	}

	public void initializeSoldier(SoldierSquad squad, GearLevel gearLevel, boolean archer) {
		this.squad = squad;
		this.gearLevel = gearLevel;
		this.archer = archer && gearLevel.archerEligible();
		this.goldenApples = gearLevel.goldenApples();
		this.buildingBlocks = gearLevel.buildingBlocks();
		this.initialized = true;
		this.applyGear();
		BattleTeams.assignSoldier(this);
	}

	private void applyGear() {
		this.setItemSlot(
				EquipmentSlot.MAINHAND,
				new ItemStack(this.archer ? Items.BOW : this.gearLevel.meleeWeapon())
		);
		this.setItemSlot(EquipmentSlot.HEAD, new ItemStack(this.gearLevel.helmet()));
		this.setItemSlot(EquipmentSlot.CHEST, new ItemStack(this.gearLevel.chestplate()));
		this.setItemSlot(EquipmentSlot.LEGS, new ItemStack(this.gearLevel.leggings()));
		this.setItemSlot(EquipmentSlot.FEET, new ItemStack(this.gearLevel.boots()));
		this.restoreDefensiveItem();

		for (EquipmentSlot slot : EnumSet.of(
				EquipmentSlot.MAINHAND,
				EquipmentSlot.OFFHAND,
				EquipmentSlot.HEAD,
				EquipmentSlot.CHEST,
				EquipmentSlot.LEGS,
				EquipmentSlot.FEET
		)) {
			this.setDropChance(slot, 0.0F);
		}

		this.setCanPickUpLoot(true);
		this.setPersistenceRequired();
		this.updateAttributes();
		this.updateDisplayName();
		this.reassessWeaponGoal();
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
		String role = this.archer ? "Archer" : "Soldier";
		this.setCustomName(Component.literal(
				this.squad.displayName() + " " + role + " • Gear " + this.gearLevel.id()
		).withStyle(this.squad.color()));
		this.setCustomNameVisible(true);
	}

	public SoldierSquad getSquad() {
		return this.squad;
	}

	public GearLevel getGearLevel() {
		return this.gearLevel;
	}

	public boolean isArcher() {
		return this.archer;
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

	public void reassessWeaponGoal() {
		if (this.level().isClientSide()) {
			return;
		}
		this.goalSelector.removeGoal(this.meleeGoal);
		this.goalSelector.removeGoal(this.bowGoal);
		if (this.archer && this.getMainHandItem().is(Items.BOW)) {
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

		ItemStack bow = this.getItemInHand(ProjectileUtil.getWeaponHoldingHand(this, Items.BOW));
		ItemStack ammunition = this.getProjectile(bow);
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
				1.6F,
				14 - level.getDifficulty().getId() * 4
		);
		this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
	}

	public boolean canUseGoldenApple() {
		return this.goldenApples > 0
				&& this.appleCooldown <= 0
				&& !this.isUsingItem()
				&& this.getHealth() <= this.getMaxHealth() * 0.5F;
	}

	public void beginGoldenApple() {
		this.savedOffhand = this.getOffhandItem().copy();
		this.setItemSlot(EquipmentSlot.OFFHAND, new ItemStack(Items.GOLDEN_APPLE));
		this.startUsingItem(InteractionHand.OFF_HAND);
	}

	public void finishGoldenApple() {
		boolean consumed = !this.getOffhandItem().is(Items.GOLDEN_APPLE);
		if (this.isUsingItem()) {
			this.stopUsingItem();
		}
		if (consumed) {
			this.goldenApples = Math.max(0, this.goldenApples - 1);
			this.appleCooldown = 20 * 12;
		} else {
			this.appleCooldown = 40;
		}
		this.setItemSlot(EquipmentSlot.OFFHAND, this.savedOffhand);
		this.savedOffhand = ItemStack.EMPTY;
	}

	private void restoreDefensiveItem() {
		ItemStack offhand = !this.archer && this.gearLevel.id() >= 2
				? new ItemStack(Items.SHIELD)
				: ItemStack.EMPTY;
		this.setItemSlot(EquipmentSlot.OFFHAND, offhand);
	}

	public boolean canBuild() {
		if (!(this.level() instanceof ServerLevel level)) {
			return false;
		}
		return this.buildingBlocks > 0
				&& this.placedBlocks.size() < 32
				&& level.getGameRules().get(GameRules.MOB_GRIEFING);
	}

	public boolean canPlaceTacticalBlock(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level)
				|| !this.canBuild()
				|| !level.mayInteract(this, pos)
				|| !level.getBlockState(pos).isAir()) {
			return false;
		}

		BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();
		AABB blockBox = AABB.unitCubeFromLowerCorner(Vec3.atLowerCornerOf(pos));
		return cobblestone.canSurvive(level, pos)
				&& level.getEntities(this, blockBox).isEmpty();
	}

	public boolean placeTacticalBlock(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level) || !this.canPlaceTacticalBlock(pos)) {
			return false;
		}

		BlockState cobblestone = Blocks.COBBLESTONE.defaultBlockState();
		if (!level.setBlock(pos, cobblestone, 3)) {
			return false;
		}

		this.buildingBlocks--;
		this.placedBlocks.add(pos.asLong());
		SoundType sounds = cobblestone.getSoundType();
		level.playSound(
				null,
				pos,
				sounds.getPlaceSound(),
				SoundSource.BLOCKS,
				sounds.getVolume(),
				sounds.getPitch()
		);
		level.gameEvent(GameEvent.BLOCK_PLACE, pos, GameEvent.Context.of(this, cobblestone));
		this.swing(InteractionHand.MAIN_HAND);
		return true;
	}

	public boolean canBreakBlock(BlockPos pos) {
		if (!(this.level() instanceof ServerLevel level)
				|| !level.getGameRules().get(GameRules.MOB_GRIEFING)
				|| !level.mayInteract(this, pos)) {
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
		if (state.is(Blocks.COBBLESTONE) || state.is(BlockTags.DIRT) || state.is(BlockTags.PLANKS)) {
			this.buildingBlocks = Math.min(this.gearLevel.buildingBlocks() + 8, this.buildingBlocks + 1);
		}
	}

	@Override
	public void tick() {
		super.tick();
		if (!this.level().isClientSide() && this.appleCooldown > 0) {
			this.appleCooldown--;
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
			this.initializeSoldier(SoldierSquad.TRAINING, GearLevel.ONE, false);
		}
		return result;
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput output) {
		super.addAdditionalSaveData(output);
		output.putString("SoldierSquad", this.squad.id());
		output.putInt("GearLevel", this.gearLevel.id());
		output.putBoolean("Archer", this.archer);
		output.putBoolean("Initialized", this.initialized);
		output.putInt("GoldenApples", this.goldenApples);
		output.putInt("BuildingBlocks", this.buildingBlocks);
		output.putInt("AppleCooldown", this.appleCooldown);
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
		this.archer = input.getBooleanOr("Archer", false);
		this.initialized = input.getBooleanOr("Initialized", true);
		this.goldenApples = Math.max(0, input.getIntOr("GoldenApples", this.gearLevel.goldenApples()));
		this.buildingBlocks = Math.max(0, input.getIntOr("BuildingBlocks", this.gearLevel.buildingBlocks()));
		this.appleCooldown = Math.max(0, input.getIntOr("AppleCooldown", 0));
		this.placedBlocks.clear();
		input.listOrEmpty("PlacedBlocks", Codec.LONG).forEach(this.placedBlocks::add);
		this.updateDisplayName();
		this.reassessWeaponGoal();
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
			if (level.getBlockState(pos).is(Blocks.COBBLESTONE)) {
				level.removeBlock(pos, false);
			}
		}
		this.placedBlocks.clear();
	}
}

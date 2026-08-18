package dev.evanklein.battlesoldiers.entity.ai;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import org.jetbrains.annotations.Nullable;

public final class ObstructionAwareTargetGoal<T extends LivingEntity> extends NearestAttackableTargetGoal<T> {
	public ObstructionAwareTargetGoal(
			Mob mob,
			Class<T> targetType,
			int randomInterval,
			@Nullable TargetingConditions.Selector selector
	) {
		super(mob, targetType, randomInterval, false, false, selector);
		this.targetConditions = TargetingConditions.forCombat()
				.range(this.getFollowDistance())
				.ignoreLineOfSight()
				.selector(selector);
	}
}

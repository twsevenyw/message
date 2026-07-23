package dev.evanklein.battlesoldiers.battle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public final class HomingArrowController {
	private static final Map<MinecraftServer, Map<ResourceKey<Level>, Map<UUID, Tracking>>> TRACKED =
			Collections.synchronizedMap(new WeakHashMap<>());

	private HomingArrowController() {
	}

	public static void register() {
		ServerTickEvents.END_WORLD_TICK.register(HomingArrowController::tickWorld);
		ServerLifecycleEvents.SERVER_STOPPED.register(TRACKED::remove);
	}

	public static void track(ServerLevel level, AbstractArrow arrow, LivingEntity target) {
		arrow.setNoGravity(true);
		TRACKED.computeIfAbsent(level.getServer(), ignored -> new HashMap<>())
				.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
				.put(arrow.getUUID(), new Tracking(target.getUUID(), level.getGameTime() + 120));
	}

	private static void tickWorld(ServerLevel level) {
		Map<ResourceKey<Level>, Map<UUID, Tracking>> server = TRACKED.get(level.getServer());
		if (server == null) {
			return;
		}
		Map<UUID, Tracking> arrows = server.get(level.dimension());
		if (arrows == null || arrows.isEmpty()) {
			return;
		}

		Iterator<Map.Entry<UUID, Tracking>> iterator = arrows.entrySet().iterator();
		while (iterator.hasNext()) {
			Map.Entry<UUID, Tracking> entry = iterator.next();
			Entity arrowEntity = level.getEntity(entry.getKey());
			Entity targetEntity = level.getEntity(entry.getValue().targetId());
			if (!(arrowEntity instanceof AbstractArrow arrow)
					|| arrow.isRemoved()
					|| !(targetEntity instanceof LivingEntity target)
					|| !target.isAlive()
					|| level.getGameTime() > entry.getValue().expiresAt()) {
				if (arrowEntity instanceof AbstractArrow arrow) {
					arrow.setNoGravity(false);
				}
				iterator.remove();
				continue;
			}

			Vec3 aimPoint = target.getBoundingBox().getCenter()
					.add(target.getDeltaMovement().scale(1.5));
			Vec3 direction = aimPoint.subtract(arrow.position());
			if (direction.lengthSqr() < 0.01) {
				continue;
			}
			double speed = Math.max(2.2, arrow.getDeltaMovement().length());
			arrow.setDeltaMovement(direction.normalize().scale(speed));
			arrow.setNoGravity(true);
			arrow.hasImpulse = true;
		}
	}

	private record Tracking(UUID targetId, long expiresAt) {
	}
}

package dev.evanklein.battlesoldiers;

import dev.evanklein.battlesoldiers.battle.BattleTeams;
import dev.evanklein.battlesoldiers.battle.HomingArrowController;
import dev.evanklein.battlesoldiers.battle.SquadCoordinator;
import dev.evanklein.battlesoldiers.command.SoldierCommands;
import dev.evanklein.battlesoldiers.config.SoldierConfig;
import dev.evanklein.battlesoldiers.entity.ModEntities;
import dev.evanklein.battlesoldiers.gui.SoldierMenus;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BattleSoldiersMod implements ModInitializer {
	public static final String MOD_ID = "battle_soldiers";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModEntities.register();
		BattleTeams.register();
		SquadCoordinator.register();
		HomingArrowController.register();
		SoldierConfig.register();
		SoldierMenus.register();
		SoldierCommands.register();
		LOGGER.info("Battle Soldiers initialized");
	}
}

package com.lenefun.thecost;

import com.lenefun.thecost.altar.ModBlockEntities;
import com.lenefun.thecost.altar.ModBlocks;
import com.lenefun.thecost.altar.ModScreenHandlers;
import com.lenefun.thecost.command.SanityCommand;
import com.lenefun.thecost.entity.ModEntities;
import com.lenefun.thecost.entity.deerclops.DeerclopsSpawnManager;
import com.lenefun.thecost.entity.deerclops.DeerclopsTraumaManager;
import com.lenefun.thecost.entity.deerclops.DeerclopsAuraManager;
import com.lenefun.thecost.entity.deerclops.DeerclopsTemperatureEffects;
import com.lenefun.thecost.entity.witness.ManagedWitnessManager;
import com.lenefun.thecost.item.ModItems;
import com.lenefun.thecost.limb.penalty.LimbPenaltyManager;
import com.lenefun.thecost.sanity.SanityNetworking;
import com.lenefun.thecost.sanity.combat.AscendedCombatManager;
import com.lenefun.thecost.sanity.environment.SanityEnvironmentManager;
import com.lenefun.thecost.sanity.rejection.WorldRejectionManager;
import com.lenefun.thecost.season.SeasonCalendarManager;
import com.lenefun.thecost.season.terrain.WinterTerrainManager;
import com.lenefun.thecost.sound.ModSounds;
import com.lenefun.thecost.temperature.TemperatureManager;
import com.lenefun.thecost.temperature.TemperatureNetworking;
import net.fabricmc.api.ModInitializer;

import net.minecraft.util.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

public class TheCost implements ModInitializer {
	public static final String MOD_ID = "thecost";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		GeckoLib.initialize();
		ModItems.register();
		ModBlocks.register();
		ModBlockEntities.register();
		ModScreenHandlers.register();
		ModSounds.register();
		ModEntities.register();
		DeerclopsSpawnManager.registerServerEvents();
		DeerclopsTraumaManager.registerServerEvents();
		DeerclopsAuraManager.registerServerEvents();
		DeerclopsTemperatureEffects.registerServerEvents();
		ManagedWitnessManager.registerServerEvents();
		LimbPenaltyManager.registerServerEvents();
		SanityNetworking.registerServerEvents();
		AscendedCombatManager.registerServerEvents();
		SanityEnvironmentManager.registerServerEvents();
		WorldRejectionManager.registerServerEvents();
		SeasonCalendarManager.registerServerEvents();
		WinterTerrainManager.registerServerEvents();
		TemperatureNetworking.registerServerEvents();
		TemperatureManager.registerServerEvents();
		SanityCommand.register();
		LOGGER.info("The Cost initialized");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}

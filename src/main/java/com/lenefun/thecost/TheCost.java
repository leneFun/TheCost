package com.lenefun.thecost;

import com.lenefun.thecost.altar.ModBlockEntities;
import com.lenefun.thecost.altar.ModBlocks;
import com.lenefun.thecost.altar.ModScreenHandlers;
import com.lenefun.thecost.command.SanityCommand;
import com.lenefun.thecost.entity.ModEntities;
import com.lenefun.thecost.entity.witness.ManagedWitnessManager;
import com.lenefun.thecost.limb.penalty.LimbPenaltyManager;
import com.lenefun.thecost.sanity.SanityNetworking;
import com.lenefun.thecost.sanity.combat.AscendedCombatManager;
import com.lenefun.thecost.sanity.environment.SanityEnvironmentManager;
import com.lenefun.thecost.sanity.rejection.WorldRejectionManager;
import com.lenefun.thecost.sound.ModSounds;
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
		ModBlocks.register();
		ModBlockEntities.register();
		ModScreenHandlers.register();
		ModSounds.register();
		ModEntities.register();
		ManagedWitnessManager.registerServerEvents();
		LimbPenaltyManager.registerServerEvents();
		SanityNetworking.registerServerEvents();
		AscendedCombatManager.registerServerEvents();
		SanityEnvironmentManager.registerServerEvents();
		WorldRejectionManager.registerServerEvents();
		SanityCommand.register();
		LOGGER.info("The Cost initialized");
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}

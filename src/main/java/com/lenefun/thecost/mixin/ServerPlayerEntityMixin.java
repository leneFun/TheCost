package com.lenefun.thecost.mixin;

import com.lenefun.thecost.entity.witness.ManagedWitnessManager;
import com.lenefun.thecost.limb.LimbData;
import com.lenefun.thecost.sanity.SanityData;
import com.lenefun.thecost.sanity.SanityHolder;
import com.lenefun.thecost.sanity.SanityNetworking;
import com.lenefun.thecost.sanity.SanityRestrictions;
import com.lenefun.thecost.sanity.SanityStage;
import com.lenefun.thecost.sanity.combat.AscendedCombatManager;
import com.mojang.datafixers.util.Either;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin implements SanityHolder {
	@Unique
	private int thecost$sanity = SanityData.DEFAULT_VALUE;

	@Override
	public int thecost$getSanity() {
		return thecost$sanity;
	}

	@Override
	public void thecost$setSanity(int sanity) {
		int clampedSanity = SanityData.clamp(sanity);
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

		if (thecost$sanity == clampedSanity) {
			AscendedCombatManager.reconcile(player);
			ManagedWitnessManager.onSanityChanged(player);
			return;
		}

		SanityStage oldStage = SanityStage.fromValue(thecost$sanity);
		SanityStage newStage = SanityStage.fromValue(clampedSanity);

		thecost$sanity = clampedSanity;
		AscendedCombatManager.reconcile(player);
		ManagedWitnessManager.onSanityChanged(player);
		SanityNetworking.sync(player);
		if (oldStage != newStage && player.networkHandler != null) {
			player.sendMessage(Text.translatable(newStage.getTransitionTranslationKey()), true);
		}
	}

	@Override
	public void thecost$setSanitySilently(int sanity) {
		thecost$sanity = SanityData.clamp(sanity);
	}

	@Inject(method = "trySleep", at = @At("HEAD"), cancellable = true)
	private void thecost$preventLowSanitySleep(
			BlockPos position,
			CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> callbackInfo) {
		ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;
		if (!SanityRestrictions.isLowSanityRestricted(player)) {
			return;
		}

		player.sendMessage(Text.translatable(SanityRestrictions.SLEEP_BLOCKED_TRANSLATION_KEY), true);
		callbackInfo.setReturnValue(Either.left(PlayerEntity.SleepFailureReason.OTHER_PROBLEM));
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void thecost$writeSanity(NbtCompound nbt, CallbackInfo callbackInfo) {
		nbt.putInt(SanityData.NBT_KEY, thecost$sanity);
		LimbData.writeNbt((ServerPlayerEntity) (Object) this, nbt);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void thecost$readSanity(NbtCompound nbt, CallbackInfo callbackInfo) {
		thecost$sanity = nbt.contains(SanityData.NBT_KEY)
				? SanityData.clamp(nbt.getInt(SanityData.NBT_KEY))
				: SanityData.DEFAULT_VALUE;
		LimbData.readNbt((ServerPlayerEntity) (Object) this, nbt);
	}
}

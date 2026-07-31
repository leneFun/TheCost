package com.lenefun.thecost.mixin;

import com.lenefun.thecost.sanity.SanityData;
import com.lenefun.thecost.sanity.SanityHolder;
import com.lenefun.thecost.sanity.SanityNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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

		if (thecost$sanity == clampedSanity) {
			return;
		}

		thecost$sanity = clampedSanity;
		SanityNetworking.sync((ServerPlayerEntity) (Object) this);
	}

	@Inject(method = "writeCustomDataToNbt", at = @At("TAIL"))
	private void thecost$writeSanity(NbtCompound nbt, CallbackInfo callbackInfo) {
		nbt.putInt(SanityData.NBT_KEY, thecost$sanity);
	}

	@Inject(method = "readCustomDataFromNbt", at = @At("TAIL"))
	private void thecost$readSanity(NbtCompound nbt, CallbackInfo callbackInfo) {
		thecost$sanity = nbt.contains(SanityData.NBT_KEY)
				? SanityData.clamp(nbt.getInt(SanityData.NBT_KEY))
				: SanityData.DEFAULT_VALUE;
	}
}

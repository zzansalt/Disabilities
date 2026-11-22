package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class ParkinsonsAimMixin {
	@Shadow
	public abstract Entity getCameraEntity();

	@Unique
	private boolean disabilities$aimShifted;
	@Unique
	private float disabilities$savedYRot;
	@Unique
	private float disabilities$savedXRot;
	@Unique
	private float disabilities$savedYRotO;
	@Unique
	private float disabilities$savedXRotO;

	@Inject(method = "pick", at = @At("HEAD"))
	private void disabilities$applyAimJitter(float partialTick, CallbackInfo ci) {
		this.disabilities$aimShifted = false;
		if (!ClientConditionState.hasLocal(ConditionType.PARKINSONS)) {
			return;
		}
		Entity cam = this.getCameraEntity();
		if (cam == null) {
			return;
		}
		float dy = ClientConditionState.getParkinsonsYaw();
		float dp = ClientConditionState.getParkinsonsPitch();
		if (Math.abs(dy) < 0.001f && Math.abs(dp) < 0.001f) {
			return;
		}
		this.disabilities$savedYRot = cam.getYRot();
		this.disabilities$savedXRot = cam.getXRot();
		this.disabilities$savedYRotO = cam.yRotO;
		this.disabilities$savedXRotO = cam.xRotO;

		float newX = Math.clamp(this.disabilities$savedXRot + dp, -90.0f, 90.0f);
		float newXO = Math.clamp(this.disabilities$savedXRotO + dp, -90.0f, 90.0f);
		cam.setYRot(this.disabilities$savedYRot + dy);
		cam.setXRot(newX);
		cam.yRotO = this.disabilities$savedYRotO + dy;
		cam.xRotO = newXO;
		this.disabilities$aimShifted = true;
	}

	@Inject(method = "pick", at = @At("RETURN"))
	private void disabilities$restoreAimJitter(float partialTick, CallbackInfo ci) {
		if (!this.disabilities$aimShifted) {
			return;
		}
		Entity cam = this.getCameraEntity();
		if (cam != null) {
			cam.setYRot(this.disabilities$savedYRot);
			cam.setXRot(this.disabilities$savedXRot);
			cam.yRotO = this.disabilities$savedYRotO;
			cam.xRotO = this.disabilities$savedXRotO;
		}
		this.disabilities$aimShifted = false;
	}
}

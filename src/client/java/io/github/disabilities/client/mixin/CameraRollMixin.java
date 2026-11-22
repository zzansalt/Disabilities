package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.Camera;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraRollMixin {
	@Shadow
	@Final
	private static Vector3fc FORWARDS;
	@Shadow
	@Final
	private static Vector3fc UP;
	@Shadow
	@Final
	private static Vector3fc LEFT;

	@Shadow
	@Final
	private Quaternionf rotation;
	@Shadow
	@Final
	private Vector3f forwards;
	@Shadow
	@Final
	private Vector3f up;
	@Shadow
	@Final
	private Vector3f left;

	@Inject(method = "setRotation", at = @At("TAIL"))
	private void disabilities$applyCameraRoll(float yRot, float xRot, CallbackInfo ci) {
		float rollDegrees = 0.0f;

		if (ClientConditionState.hasLocal(ConditionType.SCOLIOSIS)) {
			rollDegrees += 18.0f;
		}

		int vertigo = ClientConditionState.getVertigoTicks();
		if (vertigo > 0) {
			float phase = (System.currentTimeMillis() % 100000L) * 0.004f;
			float intensity = Math.min(1.0f, vertigo / 40.0f);
			rollDegrees += (float) Math.sin(phase) * 45.0f * intensity * ClientConditionState.getVertigoDirection();
		}

		if (Math.abs(rollDegrees) > 0.01f) {
			this.rotation.rotateZ((float) Math.toRadians(rollDegrees));
			FORWARDS.rotate(this.rotation, this.forwards);
			UP.rotate(this.rotation, this.up);
			LEFT.rotate(this.rotation, this.left);
		}
	}
}

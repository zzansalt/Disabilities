package io.github.disabilities.client.mixin;

import io.github.disabilities.ConditionType;
import io.github.disabilities.client.ClientConditionState;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardInput.class)
public abstract class PlayerInputMixin extends ClientInput {
	@Inject(method = "tick", at = @At("TAIL"))
	private void disabilities$modifyPlayerInput(CallbackInfo ci) {
		ClientInputAccessor accessor = (ClientInputAccessor) this;

		if (ClientConditionState.isSeizing() || ClientConditionState.isAsleep()) {
			this.keyPresses = Input.EMPTY;
			accessor.disabilities$setMoveVector(Vec2.ZERO);
			return;
		}

		if (ClientConditionState.hasLocal(ConditionType.CEREBRAL_PALSY)) {
			boolean fwd = this.keyPresses.forward() || ClientConditionState.isCpForward();
			boolean back = this.keyPresses.backward() || ClientConditionState.isCpBackward();
			boolean left = this.keyPresses.left() || ClientConditionState.isCpLeft();
			boolean right = this.keyPresses.right() || ClientConditionState.isCpRight();
			boolean jump = this.keyPresses.jump() || ClientConditionState.pollCpJump();
			boolean shift = this.keyPresses.shift() || ClientConditionState.pollCpSneak();
			boolean sprint = this.keyPresses.sprint();

			this.keyPresses = new Input(fwd, back, left, right, jump, shift, sprint);
			float x = (left ? 1.0f : 0.0f) - (right ? 1.0f : 0.0f);
			float z = (fwd ? 1.0f : 0.0f) - (back ? 1.0f : 0.0f);
			Vec2 vec = new Vec2(x, z);
			if (vec.lengthSquared() > 1.0f) {
				vec = vec.normalized();
			}
			accessor.disabilities$setMoveVector(vec);
		}
	}
}

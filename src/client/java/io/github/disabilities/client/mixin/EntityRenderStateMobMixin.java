package io.github.disabilities.client.mixin;

import io.github.disabilities.client.util.MobFlagDuck;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityRenderState.class)
public abstract class EntityRenderStateMobMixin implements MobFlagDuck {
	@Unique
	private boolean disabilities$livingMob;

	@Override
	public boolean disabilities$isLivingMob() {
		return this.disabilities$livingMob;
	}

	@Override
	public void disabilities$setLivingMob(boolean flag) {
		this.disabilities$livingMob = flag;
	}
}

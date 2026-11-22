package io.github.disabilities.client.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Minecraft.class)
public interface MinecraftActionInvoker {
	@Invoker("startAttack")
	boolean disabilities$invokeStartAttack();

	@Invoker("startUseItem")
	void disabilities$invokeStartUseItem();
}

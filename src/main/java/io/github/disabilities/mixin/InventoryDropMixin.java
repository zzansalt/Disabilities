package io.github.disabilities.mixin;

import io.github.disabilities.ConditionAccess;
import io.github.disabilities.ConditionType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Inventory.class)
public abstract class InventoryDropMixin {
	@Shadow
	@Final
	public Player player;

	@Inject(method = "removeFromSelected", at = @At("HEAD"), cancellable = true)
	private void disabilities$blockDropWhenKleptomaniac(boolean all, CallbackInfoReturnable<ItemStack> cir) {
		if (ConditionAccess.has(this.player, ConditionType.KLEPTOMANIA)) {
			cir.setReturnValue(ItemStack.EMPTY);
		}
	}
}

package org.hidde2727.DiscordPlugin.mixins;

import net.minecraft.server.PlayerConfigEntry;
import org.hidde2727.DiscordPlugin.Fabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(net.minecraft.server.dedicated.DedicatedPlayerManager.class)
public class WhitelistMixin {
    @Inject(method="isWhitelisted", at=@At(value="HEAD"), cancellable=true)
    void isAllowed(PlayerConfigEntry playerConfigEntry, CallbackInfoReturnable<Boolean> cir) {
        boolean letThrough = Fabric.plugin.OnPlayerPreLogin(
                playerConfigEntry.name(),
                playerConfigEntry.id().toString().replaceAll("-", "")
        );
        if(!letThrough) {
            cir.setReturnValue(false);
            cir.cancel();
        }
        // It is allowed, don't touch the event
    }
}

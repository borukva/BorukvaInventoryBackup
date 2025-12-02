package ua.fiv.mixin;

import ua.fiv.ModInit;
import ua.fiv.actor.BActorMessages;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class OnPlayerLogoutMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLogoutMixin(ServerPlayerEntity player, CallbackInfo ci){
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.SavePlayerDataOnPlayerLogout(player));
    }

}

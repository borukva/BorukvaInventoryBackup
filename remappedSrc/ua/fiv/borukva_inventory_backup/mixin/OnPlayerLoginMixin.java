package ua.fiv.borukva_inventory_backup.mixin;

import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.actor.BActorMessages;
import net.minecraft.network.ClientConnection;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ConnectedClientData;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerManager.class)
public class OnPlayerLoginMixin {
    @Inject(method = "onPlayerConnect", at = @At("TAIL"))
    private void onPlayerConnectMixin(ClientConnection connection, ServerPlayerEntity player,
                                    ConnectedClientData clientData, CallbackInfo ci){
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.SavePlayerDataOnPlayerConnect(player));
    }
}

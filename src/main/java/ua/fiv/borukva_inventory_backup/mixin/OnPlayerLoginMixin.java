package ua.fiv.borukva_inventory_backup.mixin;

import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.actor.BActorMessages;
import ua.fiv.borukva_inventory_backup.actor.PlayerSnapshot;
import net.minecraft.network.Connection;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class OnPlayerLoginMixin {
    @Inject(method = "placeNewPlayer", at = @At("TAIL"))
    private void onPlayerConnectMixin(Connection connection, ServerPlayer player,
                                    CommonListenerCookie clientData, CallbackInfo ci){
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.SavePlayerDataOnPlayerConnect(PlayerSnapshot.capture(player)));
    }
}

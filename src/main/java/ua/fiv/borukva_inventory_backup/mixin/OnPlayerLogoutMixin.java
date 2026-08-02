package ua.fiv.borukva_inventory_backup.mixin;

import ua.fiv.borukva_inventory_backup.ModInit;
import ua.fiv.borukva_inventory_backup.actor.BActorMessages;
import ua.fiv.borukva_inventory_backup.actor.PlayerSnapshot;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class OnPlayerLogoutMixin {
    @Inject(method = "remove", at = @At("HEAD"))
    private void onPlayerLogoutMixin(ServerPlayer player, CallbackInfo ci){
        ModInit.getDatabaseManagerActor().tell(
                new BActorMessages.SavePlayerDataOnPlayerLogout(PlayerSnapshot.capture(player)));
    }

}

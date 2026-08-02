package ua.fiv.borukva_inventory_backup.mixin;

import ua.fiv.borukva_inventory_backup.ModInit;

import ua.fiv.borukva_inventory_backup.actor.BActorMessages;
import ua.fiv.borukva_inventory_backup.actor.PlayerSnapshot;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public class OnPlayerDeathMixin {
    @Inject(method = "die", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource source, CallbackInfo ci) {
            ServerPlayer player = (ServerPlayer) (Object) this;

            ModInit.getDatabaseManagerActor().tell(
                    new BActorMessages.SavePlayerDataOnPlayerDeath(
                            PlayerSnapshot.capture(player), source.getMsgId()));
    }
}

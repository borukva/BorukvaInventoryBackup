package ua.fiv.borukva_inventory_backup.mixin;

import ua.fiv.borukva_inventory_backup.ModInit;

import ua.fiv.borukva_inventory_backup.actor.BActorMessages;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayerEntity.class)
public class OnPlayerDeathMixin {
    @Inject(method = "onDeath", at = @At("HEAD"))
    private void onPlayerDeath(DamageSource source, CallbackInfo ci) {
            ServerPlayerEntity player = (ServerPlayerEntity) (Object) this;

            ModInit.getDatabaseManagerActor().tell(
                    new BActorMessages.SavePlayerDataOnPlayerDeath(player, source));
    }
}

package ua.fiv.mixin;

import ua.fiv.ModInit;

import ua.fiv.actor.BActorMessages;
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

package ua.fiv.borukva_inventory_backup.actor;

import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ActorMessageBoundaryTest {
    @Test
    void lifecycleDatabaseMessagesCarryOnlyDetachedSnapshots() {
        assertDetached(BActorMessages.SavePlayerDataOnPlayerDeath.class);
        assertDetached(BActorMessages.SavePlayerDataOnPlayerConnect.class);
        assertDetached(BActorMessages.SavePlayerDataOnPlayerLogout.class);
        assertDetached(BActorMessages.SavePlayerDataOnPlayerRestore.class);
        assertDetached(BActorMessages.SavePendingTrinkets.class);
        assertDetached(BActorMessages.ApplyPendingTrinkets.class);

        Set<Class<?>> snapshotTypes = Set.of(String.class, int.class);
        for (RecordComponent component : PlayerSnapshot.class.getRecordComponents()) {
            assertTrue(snapshotTypes.contains(component.getType()), component.toString());
        }
    }

    private static void assertDetached(Class<? extends Record> messageType) {
        for (RecordComponent component : messageType.getRecordComponents()) {
            assertFalse(ServerPlayer.class.isAssignableFrom(component.getType()), component.toString());
            assertFalse(Tag.class.isAssignableFrom(component.getType()), component.toString());
        }
    }
}

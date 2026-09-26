package ua.fiv.borukva_inventory_backup.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrinketEntryTest {
    @Test
    void recordsWithoutTrinketDataStayUnknown() {
        assertNull(TrinketEntry.parse(null, null));
    }

    @Test
    void emptySnapshotMeansNoTrinketsWorn() {
        assertTrue(TrinketEntry.parse("[]", null).isEmpty());
    }

    @Test
    void unreadableSnapshotIsTreatedAsUnknown() {
        assertNull(TrinketEntry.parse("[", null));
    }
}

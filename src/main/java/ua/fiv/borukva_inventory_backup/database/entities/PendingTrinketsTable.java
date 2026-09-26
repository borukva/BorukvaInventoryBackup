package ua.fiv.borukva_inventory_backup.database.entities;

import com.j256.ormlite.field.DataType;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.table.DatabaseTable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Trinkets restored to an offline player. Trinkets keeps its own format in the
 * player file, so instead of editing it they are put on at the next login.
 */
@Getter
@Setter
@NoArgsConstructor
@DatabaseTable(tableName = "pending_trinkets_table")
public class PendingTrinketsTable {

    @DatabaseField(generatedId = true)
    private int id;

    @DatabaseField(dataType = DataType.STRING)
    private String name;

    @DatabaseField
    private String date;

    @DatabaseField(dataType = DataType.LONG_STRING)
    private String trinkets;

    public PendingTrinketsTable(String name, String date, String trinkets) {
        this.name = name;
        this.date = date;
        this.trinkets = trinkets;
    }
}

package ua.fiv.borukva_inventory_backup.config;

import com.google.gson.annotations.SerializedName;

public class ModConfig {

    @SerializedName("max_records")
    public int maxRecords = 100;

    @SerializedName("database_type")
    public String databaseType = "H2";

    @SerializedName("database_name")
    public String databaseName = "borukva_inventory_backup";

    @SerializedName("c_database")
    public String c_database = "Not used if H2 selected as database type";

    @SerializedName("database_url")
    public String databaseUrl = "localhost:3306";

    @SerializedName("database_user")
    public String databaseUser = "user";

    @SerializedName("database_password")
    public String databasePassword = "password";
}
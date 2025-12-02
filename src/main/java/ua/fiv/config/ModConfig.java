package ua.fiv.config;

import com.google.gson.annotations.SerializedName;

public class ModConfig {

    @SerializedName("max_records")
    public int maxRecords = 100;

    @SerializedName("database_type")
    public String databaseType = "H2";

    @SerializedName("database_url")
    public String databaseUrl = "localhost:3306";

    @SerializedName("database_user")
    public String databaseUser = "user";

    @SerializedName("database_password")
    public String databasePassword = "password";
}
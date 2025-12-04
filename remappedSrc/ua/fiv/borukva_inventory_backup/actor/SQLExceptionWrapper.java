package ua.fiv.borukva_inventory_backup.actor;

import java.sql.SQLException;

public class SQLExceptionWrapper extends RuntimeException {
    public SQLExceptionWrapper(SQLException e){
        super(e);
    }
}
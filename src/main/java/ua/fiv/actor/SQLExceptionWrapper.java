package ua.fiv.actor;

import java.sql.SQLException;

public class SQLExceptionWrapper extends RuntimeException {
    public SQLExceptionWrapper(SQLException e){
        super(e);
    }
}
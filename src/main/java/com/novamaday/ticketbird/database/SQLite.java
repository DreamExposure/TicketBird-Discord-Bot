package com.novamaday.ticketbird.database;

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class SQLite extends Database {
    private final String dbLocation;

    /**
     * Creates a new SQLite instance
     *
     * @param dbLocation Location of the Database (Must end in .db)
     */
    public SQLite(String dbLocation) {
        this.dbLocation = dbLocation;
    }

    @Override
    public Connection openConnection() throws SQLException, ClassNotFoundException {
        if (checkConnection())
            return connection;


        File dataFolder = new File("sqlite-db/");
        if (!dataFolder.exists())
            dataFolder.mkdirs();


        File file = new File(dataFolder, dbLocation);
        if (!(file.exists())) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                System.out.println("Unable to create database!");
            }
        }
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + dataFolder + "/" + dbLocation);
        return connection;
    }
}

package com.novamaday.ticketbird.database;

import com.novamaday.ticketbird.logger.Logger;
import com.novamaday.ticketbird.objects.bot.BotSettings;
import com.novamaday.ticketbird.objects.guild.GuildSettings;

import java.sql.*;

@SuppressWarnings({"SqlResolve", "UnusedReturnValue", "SqlNoDataSourceInspection"})
public class DatabaseManager {
    private static DatabaseManager instance;
    private DatabaseInfo databaseInfo;

    private DatabaseManager() {
    } //Prevent initialization.

    /**
     * Gets the instance of the {@link DatabaseManager}.
     *
     * @return The instance of the {@link DatabaseManager}
     */
    public static DatabaseManager getManager() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Connects to the MySQL server specified.
     */
    public void connectToMySQL() {
        try {
            MySQL mySQL = new MySQL(BotSettings.SQL_HOST.get(), BotSettings.SQL_PORT.get(), BotSettings.SQL_DB.get(), BotSettings.SQL_PREFIX.get(), BotSettings.SQL_USER.get(), BotSettings.SQL_PASSWORD.get());

            Connection mySQLConnection = mySQL.openConnection();
            databaseInfo = new DatabaseInfo(mySQL, mySQLConnection, mySQL.getPrefix());
            System.out.println("Connected to MySQL database!");
        } catch (Exception e) {
            System.out.println("Failed to connect to MySQL database! Is it properly configured?");
            e.printStackTrace();
            Logger.getLogger().exception(null, "Connecting to MySQL server failed.", e, this.getClass());
        }
    }

    /**
     * Disconnects from the MySQL server if still connected.
     */
    public void disconnectFromMySQL() {
        if (databaseInfo != null) {
            try {
                databaseInfo.getMySQL().closeConnection();
                System.out.println("Successfully disconnected from MySQL Database!");
            } catch (SQLException e) {
                Logger.getLogger().exception(null, "Disconnecting from MySQL failed.", e, this.getClass());
                System.out.println("MySQL Connection may not have closed properly! Data may be invalidated!");
            }
        }
    }

    /**
     * Creates all required tables in the database if they do not exist.
     */
    public void createTables() {
        try {
            Statement statement = databaseInfo.getConnection().createStatement();
            String settingsTableName = String.format("%sguild_settings", databaseInfo.getPrefix());
            String createSettingsTable = "CREATE TABLE IF NOT EXISTS " + settingsTableName +
                    "(GUILD_ID VARCHAR(255) not NULL, " +
                    " LANG VARCHAR(255) not NULL, " +
                    " PREFIX VARCHAR(255) not NULL, " +
                    " PATRON_GUILD BOOLEAN not NULL, " +
                    " DEV_GUILD BOOLEAN not NULL, " +
                    " AWAITING_CATEGORY LONG not NULL, " +
                    " RESPONDED_CATEGORY LONG not NULL, " +
                    " HOLD_CATEGORY LONG not NULL, " +
                    " CLOSE_CATEGORY LONG not NULL, " +
                    " PRIMARY KEY (GUILD_ID))";
            statement.executeUpdate(createSettingsTable);
            statement.close();
            System.out.println("Successfully created needed tables in MySQL database!");
        } catch (SQLException e) {
            System.out.println("Failed to created database tables! Something must be wrong.");
            Logger.getLogger().exception(null, "Creating MySQL tables failed", e, this.getClass());
            e.printStackTrace();
        }
    }

    public boolean updateSettings(GuildSettings settings) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sguild_settings", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + String.valueOf(settings.getGuildID()) + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (!hasStuff || res.getString("GUILD_ID") == null) {
                    //Data not present, add to DB.
                    String insertCommand = "INSERT INTO " + dataTableName +
                            "(GUILD_ID, LANG, PREFIX, PATRON_GUILD, DEV_GUILD, AWAITING_CATEGORY, RESPONDED_CATEGORY, HOLD_CATEGORY, CLOSE_CATEGORY)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(insertCommand);
                    ps.setString(1, String.valueOf(settings.getGuildID()));
                    ps.setString(2, settings.getLang());
                    ps.setString(3, settings.getPrefix());
                    ps.setBoolean(4, settings.isPatronGuild());
                    ps.setBoolean(5, settings.isDevGuild());
                    ps.setLong(6, settings.getAwaitingCategory());
                    ps.setLong(7, settings.getRespondedCategory());
                    ps.setLong(8, settings.getHoldCategory());
                    ps.setLong(9, settings.getCloseCategory());


                    ps.executeUpdate();
                    ps.close();
                    statement.close();
                } else {
                    //Data present, update.
                    String update = "UPDATE " + dataTableName
                            + " SET LANG = ?, PREFIX = ?, PATRON_GUILD = ?, " +
                            " AWAITNG_CATEGORY = ?, RESPONDED_CATEGORY = ?, HOLD_CATEGORY = ?, CLOSE_CATEGORY = ?, " +
                            "DEV_GUILD = ? WHERE GUILD_ID = ?";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(update);

                    ps.setString(1, settings.getLang());
                    ps.setString(2, settings.getPrefix());
                    ps.setBoolean(3, settings.isPatronGuild());
                    ps.setBoolean(4, settings.isDevGuild());
                    ps.setLong(5, settings.getAwaitingCategory());
                    ps.setLong(6, settings.getRespondedCategory());
                    ps.setLong(7, settings.getHoldCategory());
                    ps.setLong(8, settings.getCloseCategory());
                    ps.setString(9, String.valueOf(settings.getGuildID()));

                    ps.executeUpdate();

                    ps.close();
                    statement.close();
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            Logger.getLogger().exception(null, "Failed to update/insert guild settings.", e, this.getClass());
            e.printStackTrace();
        }
        return false;
    }


    public GuildSettings getSettings(long guildId) {
        GuildSettings settings = new GuildSettings(guildId);
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sguild_settings", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + String.valueOf(guildId) + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (hasStuff && res.getString("GUILD_ID") != null) {
                    settings.setLang(res.getString("LANG"));
                    settings.setPrefix(res.getString("PREFIX"));
                    settings.setPatronGuild(res.getBoolean("PATRON_GUILD"));
                    settings.setDevGuild(res.getBoolean("DEV_GUILD"));

                    settings.setAwaitingCategory(res.getLong("AWAITING_CATEGORY"));
                    settings.setRespondedCategory(res.getLong("RESPONDED_CATEGORY"));
                    settings.setHoldCategory(res.getLong("HOLD_CATEGORY"));
                    settings.setCloseCategory(res.getLong("CLOSE_CATEGORY"));

                    statement.close();
                } else {
                    //Data not present.
                    statement.close();
                    return settings;
                }
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Guild Settings.", e, this.getClass());
        }
        return settings;
    }
}
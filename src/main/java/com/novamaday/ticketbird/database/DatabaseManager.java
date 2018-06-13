package com.novamaday.ticketbird.database;

import com.novamaday.ticketbird.logger.Logger;
import com.novamaday.ticketbird.objects.bot.BotSettings;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.objects.guild.Project;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

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
            String projectTableName = String.format("%sprojects", databaseInfo.getPrefix());
            String ticketTableName = String.format("%stickets", databaseInfo.getPrefix());
            String createSettingsTable = "CREATE TABLE IF NOT EXISTS " + settingsTableName +
                    "(GUILD_ID LONG not NULL, " +
                    " LANG VARCHAR(255) not NULL, " +
                    " PREFIX VARCHAR(16) not NULL, " +
                    " PATRON_GUILD BOOLEAN not NULL, " +
                    " DEV_GUILD BOOLEAN not NULL, " +
                    " AWAITING_CATEGORY LONG not NULL, " +
                    " RESPONDED_CATEGORY LONG not NULL, " +
                    " HOLD_CATEGORY LONG not NULL, " +
                    " CLOSE_CATEGORY LONG not NULL, " +
                    " STAFF LONGTEXT not NULL, " +
                    " PRIMARY KEY (GUILD_ID))";
            String createProjectsTable = "CREATE TABLE IF NOT EXISTS " + projectTableName +
                    "(GUILD_ID LONG not NULL, " +
                    " PROJECT_NAME LONGTEXT not NULL, " +
                    " PROJECT_PREFIX VARCHAR(16) not NULL, " +
                    " PRIMARY KEY (GUILD_ID, PROJECT_NAME))";
            String createTicketsTable = "CREATE TABLE IF NOT EXISTS " + ticketTableName +
                    "(GUILD_ID LONG not NULL, " +
                    " NUMBER INTEGER not NULL, " +
                    " PROJECT LONGTEXT not NULL, " +
                    " CREATOR LONG not NULL, " +
                    " CHANNEL LONG not NULL, " +
                    " CATEGORY LONG not NULL, " +
                    " PRIMARY KEY(GUILD_ID, NUMBER))";
            statement.executeUpdate(createSettingsTable);
            statement.executeUpdate(createProjectsTable);
            statement.executeUpdate(createTicketsTable);
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

                if (!hasStuff || res.getLong("GUILD_ID") > 0) {
                    //Data not present, add to DB.
                    String insertCommand = "INSERT INTO " + dataTableName +
                            "(GUILD_ID, LANG, PREFIX, PATRON_GUILD, DEV_GUILD, AWAITING_CATEGORY, RESPONDED_CATEGORY, HOLD_CATEGORY, CLOSE_CATEGORY, STAFF)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(insertCommand);
                    ps.setLong(1, settings.getGuildID());
                    ps.setString(2, settings.getLang());
                    ps.setString(3, settings.getPrefix());
                    ps.setBoolean(4, settings.isPatronGuild());
                    ps.setBoolean(5, settings.isDevGuild());
                    ps.setLong(6, settings.getAwaitingCategory());
                    ps.setLong(7, settings.getRespondedCategory());
                    ps.setLong(8, settings.getHoldCategory());
                    ps.setLong(9, settings.getCloseCategory());
                    ps.setString(10, settings.getStaffString());


                    ps.executeUpdate();
                    ps.close();
                    statement.close();
                } else {
                    //Data present, update.
                    String update = "UPDATE " + dataTableName
                            + " SET LANG = ?, PREFIX = ?, PATRON_GUILD = ?, DEV_GUILD = ?, " +
                            " AWAITING_CATEGORY = ?, RESPONDED_CATEGORY = ?, HOLD_CATEGORY = ?, CLOSE_CATEGORY = ?, " +
                            " STAFF = ? WHERE GUILD_ID = ?";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(update);

                    ps.setString(1, settings.getLang());
                    ps.setString(2, settings.getPrefix());
                    ps.setBoolean(3, settings.isPatronGuild());
                    ps.setBoolean(4, settings.isDevGuild());
                    ps.setLong(5, settings.getAwaitingCategory());
                    ps.setLong(6, settings.getRespondedCategory());
                    ps.setLong(7, settings.getHoldCategory());
                    ps.setLong(8, settings.getCloseCategory());
                    ps.setString(9, settings.getStaffString());
                    ps.setLong(10, settings.getGuildID());

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

    public boolean updateProject(Project project) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sprojects", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + project.getGuildId() + "' AND PROJECT_NAME = '" + project.getName() + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (!hasStuff || res.getString("GUILD_ID") == null) {
                    //Data not present, add to DB.
                    String insertCommand = "INSERT INTO " + dataTableName +
                            "(GUILD_ID, PROJECT_NAME, PROJECT_PREFIX) VALUES (?, ?, ?);";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(insertCommand);
                    ps.setLong(1, project.getGuildId());
                    ps.setString(2, project.getName());
                    ps.setString(3, project.getPrefix());

                    ps.executeUpdate();
                    ps.close();
                    statement.close();
                } else {
                    //Data present, update.
                    String update = "UPDATE " + dataTableName + " SET PROJECT_PREFIX = ? WHERE GUILD_ID = ? AND PROJECT_NAME = ?";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(update);

                    ps.setString(1, project.getPrefix());
                    ps.setLong(2, project.getGuildId());
                    ps.setString(3, project.getName());

                    ps.executeUpdate();

                    ps.close();
                    statement.close();
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            Logger.getLogger().exception(null, "Failed to update/insert project settings.", e, this.getClass());
            e.printStackTrace();
        }
        return false;
    }

    public GuildSettings getSettings(long guildId) {
        GuildSettings settings = new GuildSettings(guildId);
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sguild_settings", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + guildId + "';";
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

                    settings.setStaffFromString(res.getString("STAFF"));

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

    public Project getProject(long guildId, String projectName) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sprojects", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + guildId + "' AND PROJECT_NAME = '" + projectName + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (hasStuff && res.getLong("GUILD_ID") > 0 && res.getString("PROJECT_NAME") != null) {
                    Project project = new Project(guildId, projectName);
                    project.setPrefix(res.getString("PROJECT_PREFIX"));

                    statement.close();

                    return project;
                } else {
                    //Data not present.
                    statement.close();
                }
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Project.", e, this.getClass());
        }
        return null;
    }

    public List<Project> getAllProjects(long guildId) {
        List<Project> projects = new ArrayList<>();

        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sprojects", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + guildId + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();


                while (res.next()) {
                    Project project = new Project(guildId, res.getString("PROJECT_NAME"));
                    project.setPrefix(res.getString("PROJECT_PREFIX"));

                    projects.add(project);
                }
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Projects for guild.", e, this.getClass());
        }

        return projects;
    }

    public boolean removeProject(long guildId, String projectName) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sprojects", databaseInfo.getPrefix());

                String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?";

                PreparedStatement preparedStmt = databaseInfo.getConnection().prepareStatement(query);

                preparedStmt.setLong(1, guildId);
                preparedStmt.setString(2, projectName);

                preparedStmt.execute();
                preparedStmt.close();
                return true;
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to delete project.", e, this.getClass());
        }
        return false;
    }
}
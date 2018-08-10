package org.dreamexposure.ticketbird.database;

import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.api.UserAPIAccount;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Project;
import org.dreamexposure.ticketbird.objects.guild.Ticket;

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
            String apiTableName = String.format("%sapi", databaseInfo.getPrefix());
            String createSettingsTable = "CREATE TABLE IF NOT EXISTS " + settingsTableName +
                    "(GUILD_ID VARCHAR(255) not NULL, " +
                    " LANG VARCHAR(255) not NULL, " +
                    " PREFIX VARCHAR(16) not NULL, " +
                    " PATRON_GUILD BOOLEAN not NULL, " +
                    " DEV_GUILD BOOLEAN not NULL, " +
                    " AWAITING_CATEGORY LONG not NULL, " +
                    " RESPONDED_CATEGORY LONG not NULL, " +
                    " HOLD_CATEGORY LONG not NULL, " +
                    " CLOSE_CATEGORY LONG not NULL, " +
                    " SUPPORT_CHANNEL LONG not NULL, " +
                    " STATIC_MESSAGE LONG not NULL, " +
                    " NEXT_ID INTEGER not NULL, " +
                    " STAFF LONGTEXT not NULL, " +
                    " CLOSED_TOTAL INTEGER not NULL, " +
                    " PRIMARY KEY (GUILD_ID))";
            String createProjectsTable = "CREATE TABLE IF NOT EXISTS " + projectTableName +
                    "(GUILD_ID VARCHAR(255) not NULL, " +
                    " PROJECT_NAME LONGTEXT not NULL, " +
                    " PROJECT_PREFIX VARCHAR(16) not NULL)";
            String createTicketsTable = "CREATE TABLE IF NOT EXISTS " + ticketTableName +
                    "(GUILD_ID VARCHAR(255) not NULL, " +
                    " NUMBER INTEGER not NULL, " +
                    " PROJECT LONGTEXT not NULL, " +
                    " CREATOR LONG not NULL, " +
                    " CHANNEL LONG not NULL, " +
                    " CATEGORY LONG not NULL, " +
                    " LAST_ACTIVITY LONG not NULL)";
            String createAPITable = "CREATE TABLE IF NOT EXISTS " + apiTableName +
                    " (USER_ID varchar(255) not NULL, " +
                    " API_KEY varchar(64) not NULL, " +
                    " BLOCKED BOOLEAN not NULL, " +
                    " TIME_ISSUED LONG not NULL, " +
                    " USES INT not NULL, " +
                    " PRIMARY KEY (USER_ID, API_KEY))";
            statement.executeUpdate(createSettingsTable);
            statement.executeUpdate(createProjectsTable);
            statement.executeUpdate(createTicketsTable);
            statement.executeUpdate(createAPITable);
            statement.close();
            System.out.println("Successfully created needed tables in MySQL database!");
        } catch (SQLException e) {
            System.out.println("Failed to created database tables! Something must be wrong.");
            Logger.getLogger().exception(null, "Creating MySQL tables failed", e, this.getClass());
            e.printStackTrace();
        }
    }

    public boolean updateAPIAccount(UserAPIAccount acc) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String tableName = String.format("%sapi", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + tableName + " WHERE API_KEY = '" + String.valueOf(acc.getAPIKey()) + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (!hasStuff || res.getString("API_KEY") == null) {
                    //Data not present, add to DB.
                    String insertCommand = "INSERT INTO " + tableName +
                            "(USER_ID, API_KEY, BLOCKED, TIME_ISSUED, USES)" +
                            " VALUES (?, ?, ?, ?, ?);";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(insertCommand);
                    ps.setString(1, acc.getUserId());
                    ps.setString(2, acc.getAPIKey());
                    ps.setBoolean(3, acc.isBlocked());
                    ps.setLong(4, acc.getTimeIssued());
                    ps.setInt(5, acc.getUses());

                    ps.executeUpdate();
                    ps.close();
                    statement.close();
                } else {
                    //Data present, update.
                    String update = "UPDATE " + tableName
                            + " SET USER_ID = ?, BLOCKED = ?,"
                            + " USES = ? WHERE API_KEY = ?";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(update);

                    ps.setString(1, acc.getUserId());
                    ps.setBoolean(2, acc.isBlocked());
                    ps.setInt(3, acc.getUses());
                    ps.setString(4, acc.getAPIKey());

                    ps.executeUpdate();

                    ps.close();
                    statement.close();
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.getLogger().exception(null, "Failed to update API account", e, this.getClass());
        }
        return false;
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
                            "(GUILD_ID, LANG, PREFIX, PATRON_GUILD, DEV_GUILD, AWAITING_CATEGORY, RESPONDED_CATEGORY, HOLD_CATEGORY, CLOSE_CATEGORY, SUPPORT_CHANNEL, STATIC_MESSAGE, NEXT_ID, STAFF, CLOSED_TOTAL)" +
                            " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(insertCommand);
                    ps.setString(1, settings.getGuildID() + "");
                    ps.setString(2, settings.getLang());
                    ps.setString(3, settings.getPrefix());
                    ps.setBoolean(4, settings.isPatronGuild());
                    ps.setBoolean(5, settings.isDevGuild());
                    ps.setLong(6, settings.getAwaitingCategory());
                    ps.setLong(7, settings.getRespondedCategory());
                    ps.setLong(8, settings.getHoldCategory());
                    ps.setLong(9, settings.getCloseCategory());
                    ps.setLong(10, settings.getSupportChannel());
                    ps.setLong(11, settings.getStaticMessage());
                    ps.setInt(12, settings.getNextId());
                    ps.setString(13, settings.getStaffString());
                    ps.setInt(14, settings.getTotalClosed());


                    ps.executeUpdate();
                    ps.close();
                    statement.close();
                } else {
                    //Data present, update.
                    String update = "UPDATE " + dataTableName
                            + " SET LANG = ?, PREFIX = ?, PATRON_GUILD = ?, DEV_GUILD = ?, " +
                            " AWAITING_CATEGORY = ?, RESPONDED_CATEGORY = ?, HOLD_CATEGORY = ?, " +
                            " CLOSE_CATEGORY = ?, SUPPORT_CHANNEL = ?, STATIC_MESSAGE = ?, " +
                            " NEXT_ID = ?, STAFF = ?, CLOSED_TOTAL = ? WHERE GUILD_ID = ?";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(update);

                    ps.setString(1, settings.getLang());
                    ps.setString(2, settings.getPrefix());
                    ps.setBoolean(3, settings.isPatronGuild());
                    ps.setBoolean(4, settings.isDevGuild());
                    ps.setLong(5, settings.getAwaitingCategory());
                    ps.setLong(6, settings.getRespondedCategory());
                    ps.setLong(7, settings.getHoldCategory());
                    ps.setLong(8, settings.getCloseCategory());
                    ps.setLong(9, settings.getSupportChannel());
                    ps.setLong(10, settings.getStaticMessage());
                    ps.setInt(11, settings.getNextId());
                    ps.setString(12, settings.getStaffString());
                    ps.setInt(13, settings.getTotalClosed());
                    ps.setString(14, settings.getGuildID() + "");

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
                    ps.setString(1, project.getGuildId() + "");
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
                    ps.setString(2, project.getGuildId() + "");
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

    public boolean updateTicket(Ticket ticket) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%stickets", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + ticket.getGuildId() + "' AND NUMBER = '" + ticket.getNumber() + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (!hasStuff || res.getString("GUILD_ID") == null) {
                    //Data not present, add to DB.
                    String insertCommand = "INSERT INTO " + dataTableName +
                            "(GUILD_ID, NUMBER, PROJECT, CREATOR, CHANNEL, CATEGORY, LAST_ACTIVITY) VALUES (?, ?, ?, ?, ?, ?, ?);";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(insertCommand);
                    ps.setString(1, ticket.getGuildId() + "");
                    ps.setInt(2, ticket.getNumber());
                    ps.setString(3, ticket.getProject());
                    ps.setLong(4, ticket.getCreator());
                    ps.setLong(5, ticket.getChannel());
                    ps.setLong(6, ticket.getCategory());
                    ps.setLong(7, ticket.getLastActivity());

                    ps.executeUpdate();
                    ps.close();
                    statement.close();
                } else {
                    //Data present, update.
                    String update = "UPDATE " + dataTableName + " SET PROJECT = ?, CREATOR = ?, CHANNEL = ?, CATEGORY = ?, LAST_ACTIVITY = ? WHERE GUILD_ID = ? AND NUMBER = ?";
                    PreparedStatement ps = databaseInfo.getConnection().prepareStatement(update);

                    ps.setString(1, ticket.getProject());
                    ps.setLong(2, ticket.getCreator());
                    ps.setLong(3, ticket.getChannel());
                    ps.setLong(4, ticket.getCategory());
                    ps.setLong(5, ticket.getLastActivity());
                    ps.setString(6, ticket.getGuildId() + "");
                    ps.setInt(7, ticket.getNumber());

                    ps.executeUpdate();

                    ps.close();
                    statement.close();
                }
                return true;
            }
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            Logger.getLogger().exception(null, "Failed to update/insert ticket data.", e, this.getClass());
            e.printStackTrace();
        }
        return false;
    }

    public UserAPIAccount getAPIAccount(String APIKey) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sapi", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE API_KEY = '" + APIKey + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (hasStuff && res.getString("API_KEY") != null) {
                    UserAPIAccount account = new UserAPIAccount();
                    account.setAPIKey(APIKey);
                    account.setUserId(res.getString("USER_ID"));
                    account.setBlocked(res.getBoolean("BLOCKED"));
                    account.setTimeIssued(res.getLong("TIME_ISSUED"));
                    account.setUses(res.getInt("USES"));

                    statement.close();

                    return account;
                } else {
                    //Data not present.
                    statement.close();
                    return null;
                }
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get API Account.", e, this.getClass());
        }
        return null;
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
                    settings.setSupportChannel(res.getLong("SUPPORT_CHANNEL"));
                    settings.setStaticMessage(res.getLong("STATIC_MESSAGE"));

                    settings.setNextId(res.getInt("NEXT_ID"));

                    settings.setTotalClosed(res.getInt("CLOSED_TOTAL"));

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

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?;";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                statement.setString(1, guildId + "");
                statement.setString(2, projectName);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (hasStuff && res.getString("GUILD_ID") != null && res.getString("PROJECT_NAME") != null) {
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

    public Ticket getTicket(long guildId, int number) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%stickets", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + guildId + "' AND NUMBER = '" + number + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                boolean hasStuff = res.next();

                if (hasStuff && res.getString("GUILD_ID") != null && res.getInt("NUMBER") > 0) {
                    Ticket ticket = new Ticket(guildId, number);
                    ticket.setProject(res.getString("PROJECT"));
                    ticket.setCreator(res.getLong("CREATOR"));
                    ticket.setChannel(res.getLong("CHANNEL"));
                    ticket.setCategory(res.getLong("CATEGORY"));
                    ticket.setLastActivity(res.getLong("LAST_ACTIVITY"));

                    statement.close();

                    return ticket;
                } else {
                    //Data not present.
                    statement.close();
                }
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Ticket Data.", e, this.getClass());
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

    public List<Ticket> getAllTickets(long guildId) {
        List<Ticket> tickets = new ArrayList<>();

        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%stickets", databaseInfo.getPrefix());

                String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = '" + guildId + "';";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();


                while (res.next()) {
                    Ticket ticket = new Ticket(guildId, res.getInt("NUMBER"));
                    ticket.setProject(res.getString("PROJECT"));
                    ticket.setCreator(res.getLong("CREATOR"));
                    ticket.setChannel(res.getLong("CHANNEL"));
                    ticket.setCategory(res.getLong("CATEGORY"));
                    ticket.setLastActivity(res.getLong("LAST_ACTIVITY"));

                    tickets.add(ticket);
                }
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Tickets for guild.", e, this.getClass());
        }

        return tickets;
    }

    public int getTotalTicketCount() {
        int amount = -1;
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String ticketTableName = String.format("%stickets", databaseInfo.getPrefix());

                String query = "SELECT COUNT(*) FROM " + ticketTableName + ";";
                PreparedStatement statement = databaseInfo.getConnection().prepareStatement(query);
                ResultSet res = statement.executeQuery();

                if (res.next())
                    amount = res.getInt(1);
                else
                    amount = 0;


                res.close();
                statement.close();
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get ticket count", e, this.getClass());
        }
        return amount;
    }

    public boolean removeProject(long guildId, String projectName) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%sprojects", databaseInfo.getPrefix());

                String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?";

                PreparedStatement preparedStmt = databaseInfo.getConnection().prepareStatement(query);

                preparedStmt.setString(1, guildId + "");
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

    public boolean removeTicket(long guildId, int number) {
        try {
            if (databaseInfo.getMySQL().checkConnection()) {
                String dataTableName = String.format("%stickets", databaseInfo.getPrefix());

                String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";

                PreparedStatement preparedStmt = databaseInfo.getConnection().prepareStatement(query);

                preparedStmt.setString(1, guildId + "");
                preparedStmt.setInt(2, number);

                preparedStmt.execute();
                preparedStmt.close();
                return true;
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to delete ticket.", e, this.getClass());
        }
        return false;
    }
}
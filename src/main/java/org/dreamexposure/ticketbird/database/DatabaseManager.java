package org.dreamexposure.ticketbird.database;

import org.dreamexposure.novautils.database.DatabaseInfo;
import org.dreamexposure.novautils.database.DatabaseSettings;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.api.UserAPIAccount;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Project;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.flywaydb.core.Flyway;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import discord4j.core.object.util.Snowflake;

@SuppressWarnings({"SqlResolve", "UnusedReturnValue", "SqlNoDataSourceInspection", "Duplicates"})
public class DatabaseManager {
    private static DatabaseManager instance;
    private DatabaseInfo masterInfo;
    private DatabaseInfo slaveInfo;

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
            DatabaseSettings masterSettings = new DatabaseSettings(BotSettings.SQL_MASTER_HOST.get(), BotSettings.SQL_MASTER_PORT.get(), BotSettings.SQL_DB.get(), BotSettings.SQL_MASTER_USER.get(), BotSettings.SQL_MASTER_PASS.get(), BotSettings.SQL_PREFIX.get());
            DatabaseSettings slaveSettings = new DatabaseSettings(BotSettings.SQL_SLAVE_HOST.get(), BotSettings.SQL_SLAVE_PORT.get(), BotSettings.SQL_DB.get(), BotSettings.SQL_SLAVE_USER.get(), BotSettings.SQL_SLAVE_PASS.get(), BotSettings.SQL_PREFIX.get());

            masterInfo = org.dreamexposure.novautils.database.DatabaseManager.connectToMySQL(masterSettings);
            slaveInfo = org.dreamexposure.novautils.database.DatabaseManager.connectToMySQL(slaveSettings);
            System.out.println("Connected to MySQL database!");
        } catch (Exception e) {
            System.out.println("Failed to connect to MySQL database! Is it properly configured?");
            e.printStackTrace();
            Logger.getLogger().exception(null, "Connecting to MySQL server failed.", e, true, this.getClass());
        }
    }

    /**
     * Disconnects from the MySQL server if still connected.
     */
    @SuppressWarnings("unused")
    public void disconnectFromMySQL() {
        try {
            org.dreamexposure.novautils.database.DatabaseManager.disconnectFromMySQL(masterInfo);
            org.dreamexposure.novautils.database.DatabaseManager.disconnectFromMySQL(slaveInfo);
            System.out.println("Successfully disconnected from MySQL Database!");
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Disconnecting from MySQL failed.", e, true, this.getClass());
            System.out.println("MySQL Connection may not have closed properly! Data may be invalidated!");
        }
    }

    public void handleMigrations() {
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("prefix", BotSettings.SQL_PREFIX.get());

        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(masterInfo.getSource())
                    .cleanDisabled(true)
                    .baselineOnMigrate(true)
                    .table(BotSettings.SQL_PREFIX.get() + "schema_history")
                    .placeholders(placeholders)
                    .load();
            int sm = flyway.migrate();
            Logger.getLogger().debug("Migrations Successful, " + sm + " migrations applied!", true);
        } catch (Exception e) {
            Logger.getLogger().exception(null, "Migrations Failure", e, true, getClass());
            System.exit(2);
        }
    }

    public boolean updateAPIAccount(UserAPIAccount acc) {
        try (final Connection masterConnection = masterInfo.getSource().getConnection()) {
            String tableName = String.format("%sapi", masterInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + tableName + " WHERE API_KEY = ?";
            Connection slaveConnection = slaveInfo.getSource().getConnection();
            PreparedStatement statement = slaveConnection.prepareStatement(query);
            statement.setString(1, acc.getAPIKey());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("API_KEY") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + tableName +
                        "(USER_ID, API_KEY, BLOCKED, TIME_ISSUED, USES)" +
                        " VALUES (?, ?, ?, ?, ?);";
                PreparedStatement ps = masterConnection.prepareStatement(insertCommand);
                ps.setString(1, acc.getUserId());
                ps.setString(2, acc.getAPIKey());
                ps.setBoolean(3, acc.isBlocked());
                ps.setLong(4, acc.getTimeIssued());
                ps.setInt(5, acc.getUses());

                ps.executeUpdate();
                ps.close();
                statement.close();
                slaveConnection.close();
            } else {
                //Data present, update.
                String update = "UPDATE " + tableName
                        + " SET USER_ID = ?, BLOCKED = ?,"
                        + " USES = ? WHERE API_KEY = ?";
                PreparedStatement ps = masterConnection.prepareStatement(update);

                ps.setString(1, acc.getUserId());
                ps.setBoolean(2, acc.isBlocked());
                ps.setInt(3, acc.getUses());
                ps.setString(4, acc.getAPIKey());

                ps.executeUpdate();

                ps.close();
                statement.close();
                slaveConnection.close();
            }
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            Logger.getLogger().exception(null, "Failed to update API account", e, true, this.getClass());
        }
        return false;
    }

    public boolean updateSettings(GuildSettings settings) {
        try (final Connection masterConnection = masterInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sguild_settings", masterInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            Connection slaveConnection = slaveInfo.getSource().getConnection();
            PreparedStatement statement = slaveConnection.prepareStatement(query);
            statement.setString(1, settings.getGuildID().asString());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("GUILD_ID") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + dataTableName +
                        "(GUILD_ID, LANG, PREFIX, PATRON_GUILD, DEV_GUILD, USE_PROJECTS, AWAITING_CATEGORY, RESPONDED_CATEGORY, HOLD_CATEGORY, CLOSE_CATEGORY, SUPPORT_CHANNEL, STATIC_MESSAGE, NEXT_ID, STAFF, CLOSED_TOTAL)" +
                        " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                PreparedStatement ps = masterConnection.prepareStatement(insertCommand);
                ps.setString(1, settings.getGuildID().asString());
                ps.setString(2, settings.getLang());
                ps.setString(3, settings.getPrefix());
                ps.setBoolean(4, settings.isPatronGuild());
                ps.setBoolean(5, settings.isDevGuild());
                ps.setBoolean(6, settings.isUseProjects());
                ps.setLong(7, settings.getAwaitingCategory().asLong());
                ps.setLong(8, settings.getRespondedCategory().asLong());
                ps.setLong(9, settings.getHoldCategory().asLong());
                ps.setLong(10, settings.getCloseCategory().asLong());
                ps.setLong(11, settings.getSupportChannel().asLong());
                ps.setLong(12, settings.getStaticMessage().asLong());
                ps.setInt(13, settings.getNextId());
                ps.setString(14, settings.getStaffString());
                ps.setInt(15, settings.getTotalClosed());


                ps.executeUpdate();
                ps.close();
                statement.close();
                slaveConnection.close();
            } else {
                //Data present, update.
                String update = "UPDATE " + dataTableName
                        + " SET LANG = ?, PREFIX = ?, PATRON_GUILD = ?, DEV_GUILD = ?, USE_PROJECTS = ?, " +
                        " AWAITING_CATEGORY = ?, RESPONDED_CATEGORY = ?, HOLD_CATEGORY = ?, " +
                        " CLOSE_CATEGORY = ?, SUPPORT_CHANNEL = ?, STATIC_MESSAGE = ?, " +
                        " NEXT_ID = ?, STAFF = ?, CLOSED_TOTAL = ? WHERE GUILD_ID = ?";
                PreparedStatement ps = masterConnection.prepareStatement(update);

                ps.setString(1, settings.getLang());
                ps.setString(2, settings.getPrefix());
                ps.setBoolean(3, settings.isPatronGuild());
                ps.setBoolean(4, settings.isDevGuild());
                ps.setBoolean(5, settings.isUseProjects());
                ps.setLong(6, settings.getAwaitingCategory().asLong());
                ps.setLong(7, settings.getRespondedCategory().asLong());
                ps.setLong(8, settings.getHoldCategory().asLong());
                ps.setLong(9, settings.getCloseCategory().asLong());
                ps.setLong(10, settings.getSupportChannel().asLong());
                ps.setLong(11, settings.getStaticMessage().asLong());
                ps.setInt(12, settings.getNextId());
                ps.setString(13, settings.getStaffString());
                ps.setInt(14, settings.getTotalClosed());
                ps.setString(15, settings.getGuildID().asString());

                ps.executeUpdate();

                ps.close();
                statement.close();
                slaveConnection.close();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            Logger.getLogger().exception(null, "Failed to update/insert guild settings.", e, true, this.getClass());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProject(Project project) {
        try (final Connection masterConnection = masterInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", masterInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?";
            Connection slaveConnection = slaveInfo.getSource().getConnection();
            PreparedStatement statement = slaveConnection.prepareStatement(query);
            statement.setString(1, project.getGuildId().asString());
            statement.setString(2, project.getName());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("GUILD_ID") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + dataTableName +
                        "(GUILD_ID, PROJECT_NAME, PROJECT_PREFIX) VALUES (?, ?, ?);";
                PreparedStatement ps = masterConnection.prepareStatement(insertCommand);
                ps.setString(1, project.getGuildId().asString());
                ps.setString(2, project.getName());
                ps.setString(3, project.getPrefix());

                ps.executeUpdate();
                ps.close();
                statement.close();
                slaveConnection.close();
            } else {
                //Data present, update.
                String update = "UPDATE " + dataTableName + " SET PROJECT_PREFIX = ? WHERE GUILD_ID = ? AND PROJECT_NAME = ?";
                PreparedStatement ps = masterConnection.prepareStatement(update);

                ps.setString(1, project.getPrefix());
                ps.setString(2, project.getGuildId().asString());
                ps.setString(3, project.getName());

                ps.executeUpdate();

                ps.close();
                statement.close();
                slaveConnection.close();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            Logger.getLogger().exception(null, "Failed to update/insert project settings.", e, true, this.getClass());
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTicket(Ticket ticket) {
        try (final Connection masterConnection = masterInfo.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", masterInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";
            Connection slaveConnection = slaveInfo.getSource().getConnection();
            PreparedStatement statement = slaveConnection.prepareStatement(query);
            statement.setString(1, ticket.getGuildId().asString());
            statement.setInt(2, ticket.getNumber());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("GUILD_ID") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + dataTableName +
                        "(GUILD_ID, NUMBER, PROJECT, CREATOR, CHANNEL, CATEGORY, LAST_ACTIVITY) VALUES (?, ?, ?, ?, ?, ?, ?);";
                PreparedStatement ps = masterConnection.prepareStatement(insertCommand);
                ps.setString(1, ticket.getGuildId().asString());
                ps.setInt(2, ticket.getNumber());
                ps.setString(3, ticket.getProject());
                ps.setLong(4, ticket.getCreator().asLong());
                ps.setLong(5, ticket.getChannel().asLong());
                ps.setLong(6, ticket.getCategory().asLong());
                ps.setLong(7, ticket.getLastActivity());

                ps.executeUpdate();
                ps.close();
                statement.close();
                slaveConnection.close();
            } else {
                //Data present, update.
                String update = "UPDATE " + dataTableName + " SET PROJECT = ?, CREATOR = ?, CHANNEL = ?, CATEGORY = ?, LAST_ACTIVITY = ? WHERE GUILD_ID = ? AND NUMBER = ?";
                PreparedStatement ps = masterConnection.prepareStatement(update);

                ps.setString(1, ticket.getProject());
                ps.setLong(2, ticket.getCreator().asLong());
                ps.setLong(3, ticket.getChannel().asLong());
                ps.setLong(4, ticket.getCategory().asLong());
                ps.setLong(5, ticket.getLastActivity());
                ps.setString(6, ticket.getGuildId().asString());
                ps.setInt(7, ticket.getNumber());

                ps.executeUpdate();

                ps.close();
                statement.close();
                slaveConnection.close();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            Logger.getLogger().exception(null, "Failed to update/insert ticket data.", e, true, this.getClass());
            e.printStackTrace();
        }
        return false;
    }

    public UserAPIAccount getAPIAccount(String APIKey) {
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sapi", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE API_KEY = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, APIKey);

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
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get API Account.", e, true, this.getClass());
        }
        return null;
    }

    public GuildSettings getSettings(Snowflake guildId) {
        GuildSettings settings = new GuildSettings(guildId);
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sguild_settings", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (hasStuff && res.getString("GUILD_ID") != null) {
                settings.setLang(res.getString("LANG"));
                settings.setPrefix(res.getString("PREFIX"));
                settings.setPatronGuild(res.getBoolean("PATRON_GUILD"));
                settings.setDevGuild(res.getBoolean("DEV_GUILD"));
                settings.setUseProjects(res.getBoolean("USE_PROJECTS"));

                settings.setAwaitingCategory(Snowflake.of(res.getLong("AWAITING_CATEGORY")));
                settings.setRespondedCategory(Snowflake.of(res.getLong("RESPONDED_CATEGORY")));
                settings.setHoldCategory(Snowflake.of(res.getLong("HOLD_CATEGORY")));
                settings.setCloseCategory(Snowflake.of(res.getLong("CLOSE_CATEGORY")));
                settings.setSupportChannel(Snowflake.of(res.getLong("SUPPORT_CHANNEL")));
                settings.setStaticMessage(Snowflake.of(res.getLong("STATIC_MESSAGE")));

                settings.setNextId(res.getInt("NEXT_ID"));

                settings.setTotalClosed(res.getInt("CLOSED_TOTAL"));

                settings.setStaffFromString(res.getString("STAFF"));

                statement.close();
            } else {
                //Data not present.
                statement.close();
                return settings;
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Guild Settings.", e, true, this.getClass());
        }
        return settings;
    }

    public Project getProject(Snowflake guildId, String projectName) {
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());
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
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Project.", e, true, this.getClass());
        }
        return null;
    }

    public Ticket getTicket(Snowflake guildId, int number) {
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());
            statement.setInt(2, number);

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (hasStuff && res.getString("GUILD_ID") != null && res.getInt("NUMBER") > 0) {
                Ticket ticket = new Ticket(guildId, number);
                ticket.setProject(res.getString("PROJECT"));
                ticket.setCreator(Snowflake.of(res.getLong("CREATOR")));
                ticket.setChannel(Snowflake.of(res.getLong("CHANNEL")));
                ticket.setCategory(Snowflake.of(res.getLong("CATEGORY")));
                ticket.setLastActivity(res.getLong("LAST_ACTIVITY"));

                statement.close();

                return ticket;
            } else {
                //Data not present.
                statement.close();
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Ticket Data.", e, true, this.getClass());
        }
        return null;
    }

    public Ticket getTicket(Snowflake guildId, Snowflake channelId) {
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND CHANNEL = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());
            statement.setLong(2, channelId.asLong());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (hasStuff && res.getString("GUILD_ID") != null && res.getInt("NUMBER") > 0) {
                Ticket ticket = new Ticket(guildId, res.getInt("NUMBER"));
                ticket.setProject(res.getString("PROJECT"));
                ticket.setCreator(Snowflake.of(res.getLong("CREATOR")));
                ticket.setCategory(Snowflake.of(res.getLong("CATEGORY")));
                ticket.setLastActivity(res.getLong("LAST_ACTIVITY"));

                statement.close();

                return ticket;
            } else {
                //Data not present.
                statement.close();
            }
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Ticket Data.", e, true, this.getClass());
        }
        return null;
    }

    public List<Project> getAllProjects(Snowflake guildId) {
        List<Project> projects = new ArrayList<>();

        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());

            ResultSet res = statement.executeQuery();


            while (res.next()) {
                Project project = new Project(guildId, res.getString("PROJECT_NAME"));
                project.setPrefix(res.getString("PROJECT_PREFIX"));

                projects.add(project);
            }

            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Projects for guild.", e, true, this.getClass());
        }

        return projects;
    }

    public List<Ticket> getAllTickets(Snowflake guildId) {
        List<Ticket> tickets = new ArrayList<>();

        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", slaveInfo.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());

            ResultSet res = statement.executeQuery();

            while (res.next()) {
                Ticket ticket = new Ticket(guildId, res.getInt("NUMBER"));
                ticket.setProject(res.getString("PROJECT"));
                ticket.setCreator(Snowflake.of(res.getLong("CREATOR")));
                ticket.setChannel(Snowflake.of(res.getLong("CHANNEL")));
                ticket.setCategory(Snowflake.of(res.getLong("CATEGORY")));
                ticket.setLastActivity(res.getLong("LAST_ACTIVITY"));

                tickets.add(ticket);
            }

            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get Tickets for guild.", e, true, this.getClass());
        }

        return tickets;
    }

    public int getTotalTicketCount() {
        int amount = -1;
        try (final Connection connection = slaveInfo.getSource().getConnection()) {
            String ticketTableName = String.format("%stickets", slaveInfo.getSettings().getPrefix());

            String query = "SELECT COUNT(*) FROM " + ticketTableName + ";";
            PreparedStatement statement = connection.prepareStatement(query);
            ResultSet res = statement.executeQuery();

            if (res.next())
                amount = res.getInt(1);
            else
                amount = 0;


            res.close();
            statement.close();
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to get ticket count", e, true, this.getClass());
        }
        return amount;
    }

    public boolean removeProject(Snowflake guildId, String projectName) {
        try (final Connection connection = masterInfo.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", masterInfo.getSettings().getPrefix());

            String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?";

            PreparedStatement preparedStmt = connection.prepareStatement(query);

            preparedStmt.setString(1, guildId.asString());
            preparedStmt.setString(2, projectName);

            preparedStmt.execute();
            preparedStmt.close();
            return true;
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to delete project.", e, true, this.getClass());
        }
        return false;
    }

    public boolean removeTicket(Snowflake guildId, int number) {
        try (final Connection connection = masterInfo.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", masterInfo.getSettings().getPrefix());

            String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";

            PreparedStatement preparedStmt = connection.prepareStatement(query);

            preparedStmt.setString(1, guildId.asString());
            preparedStmt.setInt(2, number);

            preparedStmt.execute();
            preparedStmt.close();
            return true;
        } catch (SQLException e) {
            Logger.getLogger().exception(null, "Failed to delete ticket.", e, true, this.getClass());
        }
        return false;
    }
}

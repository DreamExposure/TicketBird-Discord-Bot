package org.dreamexposure.ticketbird.database;

import discord4j.common.util.Snowflake;
import org.dreamexposure.novautils.database.DatabaseInfo;
import org.dreamexposure.novautils.database.DatabaseSettings;
import org.dreamexposure.ticketbird.conf.BotSettings;
import org.dreamexposure.ticketbird.extensions.MutableListKt;
import org.dreamexposure.ticketbird.object.GuildSettings;
import org.dreamexposure.ticketbird.object.Project;
import org.dreamexposure.ticketbird.object.Ticket;
import org.dreamexposure.ticketbird.utils.GlobalVars;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings({"SqlResolve", "UnusedReturnValue", "SqlNoDataSourceInspection", "Duplicates"})
public class DatabaseManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;
    private DatabaseInfo info;

    private DatabaseManager() {
        connectToMySQL();
    }

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
    private void connectToMySQL() {
        try {
            DatabaseSettings settings = new DatabaseSettings(BotSettings.SQL_HOST.get(), BotSettings.SQL_PORT.get(),
                BotSettings.SQL_DB.get(), BotSettings.SQL_USER.get(), BotSettings.SQL_PASS.get(),
                BotSettings.SQL_PREFIX.get());

            info = org.dreamexposure.novautils.database.DatabaseManager.connectToMySQL(settings);
            System.out.println("Connected to MySQL database!");
        } catch (Exception e) {
            System.out.println("Failed to connect to MySQL database! Is it properly configured?");
            e.printStackTrace();
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Connecting to MySQL server failed.", e);
        }
    }

    /**
     * Disconnects from the MySQL server if still connected.
     */
    public void disconnectFromMySQL() {
        try {
            org.dreamexposure.novautils.database.DatabaseManager.disconnectFromMySQL(info);
            System.out.println("Successfully disconnected from MySQL Database!");
        } catch (Exception e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Disconnecting from MySQL failed.", e);
            System.out.println("MySQL Connection may not have closed properly! Data may be invalidated!");
        }
    }

    public boolean updateSettings(GuildSettings settings) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%sguild_settings", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, settings.getGuildId().asString());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("GUILD_ID") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + dataTableName +
                    "(GUILD_ID, LANG, PREFIX, PATRON_GUILD, DEV_GUILD, USE_PROJECTS, AWAITING_CATEGORY, RESPONDED_CATEGORY, HOLD_CATEGORY, CLOSE_CATEGORY, SUPPORT_CHANNEL, STATIC_MESSAGE, NEXT_ID, STAFF, CLOSED_TOTAL)" +
                    " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
                PreparedStatement ps = connection.prepareStatement(insertCommand);
                ps.setString(1, settings.getGuildId().asString());
                ps.setString(2, settings.getLang());
                ps.setString(3, settings.getPrefix());
                ps.setBoolean(4, settings.getPatronGuild());
                ps.setBoolean(5, settings.getDevGuild());
                ps.setBoolean(6, settings.getUseProjects());

                if (settings.getAwaitingCategory() != null)
                    ps.setLong(7, settings.getAwaitingCategory().asLong());
                else ps.setLong(7, 0);

                if (settings.getRespondedCategory() != null)
                    ps.setLong(8, settings.getRespondedCategory().asLong());
                else ps.setLong(8, 0);

                if (settings.getHoldCategory() != null)
                    ps.setLong(9, settings.getHoldCategory().asLong());
                else ps.setLong(9, 0);

                if (settings.getCloseCategory() != null)
                    ps.setLong(10, settings.getCloseCategory().asLong());
                else ps.setLong(10, 0);

                if (settings.getSupportChannel() != null)
                    ps.setLong(11, settings.getSupportChannel().asLong());
                else ps.setLong(11, 0);

                if (settings.getStaticMessage() != null)
                    ps.setLong(12, settings.getStaticMessage().asLong());
                else ps.setLong(12, 0);

                ps.setInt(13, settings.getNextId());
                ps.setString(14, MutableListKt.asStringList(settings.getStaff()));
                ps.setInt(15, settings.getTotalClosed());


                ps.executeUpdate();
                ps.close();
                statement.close();
            } else {
                //Data present, update.
                String update = "UPDATE " + dataTableName
                    + " SET LANG = ?, PREFIX = ?, PATRON_GUILD = ?, DEV_GUILD = ?, USE_PROJECTS = ?, " +
                    " AWAITING_CATEGORY = ?, RESPONDED_CATEGORY = ?, HOLD_CATEGORY = ?, " +
                    " CLOSE_CATEGORY = ?, SUPPORT_CHANNEL = ?, STATIC_MESSAGE = ?, " +
                    " NEXT_ID = ?, STAFF = ?, CLOSED_TOTAL = ? WHERE GUILD_ID = ?";
                PreparedStatement ps = connection.prepareStatement(update);

                ps.setString(1, settings.getLang());
                ps.setString(2, settings.getPrefix());
                ps.setBoolean(3, settings.getPatronGuild());
                ps.setBoolean(4, settings.getDevGuild());
                ps.setBoolean(5, settings.getUseProjects());

                if (settings.getAwaitingCategory() != null)
                    ps.setLong(6, settings.getAwaitingCategory().asLong());
                else ps.setLong(6, 0);

                if (settings.getRespondedCategory() != null)
                    ps.setLong(7, settings.getRespondedCategory().asLong());
                else ps.setLong(7, 0);

                if (settings.getHoldCategory() != null)
                    ps.setLong(8, settings.getHoldCategory().asLong());
                else ps.setLong(8, 0);

                if (settings.getCloseCategory() != null)
                    ps.setLong(9, settings.getCloseCategory().asLong());
                else ps.setLong(9, 0);

                if (settings.getSupportChannel() != null)
                    ps.setLong(10, settings.getSupportChannel().asLong());
                else ps.setLong(10, 0);

                if (settings.getStaticMessage() != null)
                    ps.setLong(11, settings.getStaticMessage().asLong());
                else ps.setLong(11, 0);

                ps.setInt(12, settings.getNextId());
                ps.setString(13, MutableListKt.asStringList(settings.getStaff()));
                ps.setInt(14, settings.getTotalClosed());
                ps.setString(15, settings.getGuildId().asString());

                ps.executeUpdate();

                ps.close();
                statement.close();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to update/insert guild settings.", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateProject(Project project) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, project.getGuildId().asString());
            statement.setString(2, project.getName());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("GUILD_ID") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + dataTableName +
                    "(GUILD_ID, PROJECT_NAME, PROJECT_PREFIX) VALUES (?, ?, ?);";
                PreparedStatement ps = connection.prepareStatement(insertCommand);
                ps.setString(1, project.getGuildId().asString());
                ps.setString(2, project.getName());
                ps.setString(3, project.getPrefix());

                ps.executeUpdate();
                ps.close();
                statement.close();
            } else {
                //Data present, update.
                String update = "UPDATE " + dataTableName + " SET PROJECT_PREFIX = ? WHERE GUILD_ID = ? AND PROJECT_NAME = ?";
                PreparedStatement ps = connection.prepareStatement(update);

                ps.setString(1, project.getPrefix());
                ps.setString(2, project.getGuildId().asString());
                ps.setString(3, project.getName());

                ps.executeUpdate();

                ps.close();
                statement.close();
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to update/insert project settings.", e);
            e.printStackTrace();
        }
        return false;
    }

    public boolean updateTicket(Ticket ticket) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, ticket.getGuildId().asString());
            statement.setInt(2, ticket.getNumber());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (!hasStuff || res.getString("GUILD_ID") == null) {
                //Data not present, add to DB.
                String insertCommand = "INSERT INTO " + dataTableName +
                    "(GUILD_ID, NUMBER, PROJECT, CREATOR, CHANNEL, CATEGORY, LAST_ACTIVITY) VALUES (?, ?, ?, ?, ?, ?, ?);";
                PreparedStatement ps = connection.prepareStatement(insertCommand);
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
            } else {
                //Data present, update.
                String update = "UPDATE " + dataTableName + " SET PROJECT = ?, CREATOR = ?, CHANNEL = ?, CATEGORY = ?, LAST_ACTIVITY = ? WHERE GUILD_ID = ? AND NUMBER = ?";
                PreparedStatement ps = connection.prepareStatement(update);

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
            }
            return true;
        } catch (SQLException e) {
            System.out.println("Failed to input data into database! Error Code: 00101");
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to update/insert ticket data.", e);
            e.printStackTrace();
        }
        return false;
    }

    public GuildSettings getSettings(Snowflake guildId) {
        GuildSettings settings = new GuildSettings(guildId);
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%sguild_settings", info.getSettings().getPrefix());

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

                //Check if these actually exist or not...
                if (res.getLong("AWAITING_CATEGORY") != 0)
                    settings.setAwaitingCategory(Snowflake.of(res.getLong("AWAITING_CATEGORY")));
                if (res.getLong("RESPONDED_CATEGORY") != 0)
                    settings.setRespondedCategory(Snowflake.of(res.getLong("RESPONDED_CATEGORY")));
                if (res.getLong("HOLD_CATEGORY") != 0)
                    settings.setHoldCategory(Snowflake.of(res.getLong("HOLD_CATEGORY")));
                if (res.getLong("CLOSE_CATEGORY") != 0)
                    settings.setCloseCategory(Snowflake.of(res.getLong("CLOSE_CATEGORY")));
                if (res.getLong("SUPPORT_CHANNEL") != 0)
                    settings.setSupportChannel(Snowflake.of(res.getLong("SUPPORT_CHANNEL")));
                if (res.getLong("STATIC_MESSAGE") != 0)
                    settings.setStaticMessage(Snowflake.of(res.getLong("STATIC_MESSAGE")));

                settings.setNextId(res.getInt("NEXT_ID"));

                settings.setTotalClosed(res.getInt("CLOSED_TOTAL"));

                settings.getStaff().addAll(MutableListKt.setFromString(res.getString("STAFF")));

                statement.close();
            } else {
                //Data not present.
                statement.close();
                return settings;
            }
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get Guild Settings.", e);
        }
        return settings;
    }

    public Project getProject(Snowflake guildId, String projectName) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?;";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());
            statement.setString(2, projectName);
            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (hasStuff && res.getString("GUILD_ID") != null && res.getString("PROJECT_NAME") != null) {
                Project project = new Project(guildId, projectName, res.getString("PROJECT_PREFIX"));

                statement.close();

                return project;
            } else {
                //Data not present.
                statement.close();
            }
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get Project.", e);
        }
        return null;
    }

    public Ticket getTicket(Snowflake guildId, int number) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());
            statement.setInt(2, number);

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (hasStuff && res.getString("GUILD_ID") != null && res.getInt("NUMBER") > 0) {
                Ticket ticket = new Ticket(
                    guildId,
                    number,
                    res.getString("PROJECT"),
                    Snowflake.of(res.getLong("CREATOR")),
                    Snowflake.of(res.getLong("CHANNEL")),
                    Snowflake.of(res.getLong("CATEGORY")),
                    res.getLong("LAST_ACTIVITY")
                );

                statement.close();

                return ticket;
            } else {
                //Data not present.
                statement.close();
            }
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get Ticket Data.", e);
        }
        return null;
    }

    public Ticket getTicket(Snowflake guildId, Snowflake channelId) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ? AND CHANNEL = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());
            statement.setLong(2, channelId.asLong());

            ResultSet res = statement.executeQuery();

            boolean hasStuff = res.next();

            if (hasStuff && res.getString("GUILD_ID") != null && res.getInt("NUMBER") > 0) {
                Ticket ticket = new Ticket(
                    guildId,
                    res.getInt("NUMBER"),
                    res.getString("PROJECT"),
                    Snowflake.of(res.getLong("CREATOR")),
                    Snowflake.of(res.getLong("CHANNEL")),
                    Snowflake.of(res.getLong("CATEGORY")),
                    res.getLong("LAST_ACTIVITY")
                );

                statement.close();

                return ticket;
            } else {
                //Data not present.
                statement.close();
            }
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get Ticket Data.", e);
        }
        return null;
    }

    public List<Project> getAllProjects(Snowflake guildId) {
        List<Project> projects = new ArrayList<>();

        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());

            ResultSet res = statement.executeQuery();


            while (res.next()) {
                Project project = new Project(guildId, res.getString("PROJECT_NAME"), res.getString("PROJECT_PREFIX"));

                projects.add(project);
            }

            res.close();
            statement.close();
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get Projects for guild.", e);
        }

        return projects;
    }

    public List<Ticket> getAllTickets(Snowflake guildId) {
        List<Ticket> tickets = new ArrayList<>();

        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", info.getSettings().getPrefix());

            String query = "SELECT * FROM " + dataTableName + " WHERE GUILD_ID = ?";
            PreparedStatement statement = connection.prepareStatement(query);
            statement.setString(1, guildId.asString());

            ResultSet res = statement.executeQuery();

            while (res.next()) {
                Ticket ticket = new Ticket(
                    guildId,
                    res.getInt("NUMBER"),
                    res.getString("PROJECT"),
                    Snowflake.of(res.getLong("CREATOR")),
                    Snowflake.of(res.getLong("CHANNEL")),
                    Snowflake.of(res.getLong("CATEGORY")),
                    res.getLong("LAST_ACTIVITY")
                );

                tickets.add(ticket);
            }

            res.close();
            statement.close();
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get Tickets for guild.", e);
        }

        return tickets;
    }

    public int getTotalTicketCount() {
        int amount = -1;
        try (final Connection connection = info.getSource().getConnection()) {
            String ticketTableName = String.format("%stickets", info.getSettings().getPrefix());

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
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to get ticket count", e);
        }
        return amount;
    }

    public boolean removeProject(Snowflake guildId, String projectName) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%sprojects", info.getSettings().getPrefix());

            String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND PROJECT_NAME = ?";

            PreparedStatement preparedStmt = connection.prepareStatement(query);

            preparedStmt.setString(1, guildId.asString());
            preparedStmt.setString(2, projectName);

            preparedStmt.execute();
            preparedStmt.close();
            return true;
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to delete project.", e);
        }
        return false;
    }

    public boolean removeTicket(Snowflake guildId, int number) {
        try (final Connection connection = info.getSource().getConnection()) {
            String dataTableName = String.format("%stickets", info.getSettings().getPrefix());

            String query = "DELETE FROM " + dataTableName + " WHERE GUILD_ID = ? AND NUMBER = ?";

            PreparedStatement preparedStmt = connection.prepareStatement(query);

            preparedStmt.setString(1, guildId.asString());
            preparedStmt.setInt(2, number);

            preparedStmt.execute();
            preparedStmt.close();
            return true;
        } catch (SQLException e) {
            LOGGER.error(GlobalVars.INSTANCE.getDEFAULT(), "Failed to delete ticket.", e);
        }
        return false;
    }
}

package com.novamaday.ticketbird.module.command;

import com.novamaday.ticketbird.Main;
import com.novamaday.ticketbird.database.DatabaseManager;
import com.novamaday.ticketbird.message.MessageManager;
import com.novamaday.ticketbird.objects.command.CommandInfo;
import com.novamaday.ticketbird.objects.guild.GuildSettings;
import com.novamaday.ticketbird.objects.guild.Project;
import com.novamaday.ticketbird.utils.GlobalVars;
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent;
import sx.blah.discord.util.EmbedBuilder;

import java.util.ArrayList;

public class ProjectCommand implements ICommand {

    /**
     * Gets the command this Object is responsible for.
     *
     * @return The command this Object is responsible for.
     */
    @Override
    public String getCommand() {
        return "Project";
    }

    /**
     * Gets the short aliases of the command this object is responsible for.
     * </br>
     * This will return an empty ArrayList if none are present
     *
     * @return The aliases of the command.
     */
    @Override
    public ArrayList<String> getAliases() {
        return new ArrayList<>();
    }

    /**
     * Gets the info on the command (not sub command) to be used in help menus.
     *
     * @return The command info.
     */
    @Override
    public CommandInfo getCommandInfo() {
        CommandInfo info = new CommandInfo("Project");
        info.setDescription("Used to configure TicketBird Projects");
        info.setExample("=Project <function> <value>");

        info.getSubCommands().put("add", "Adds a new project to your TicketBird.");
        info.getSubCommands().put("remove", "Removes a project from  your TicketBird.");
        info.getSubCommands().put("list", "Lists all active projects/services for tickets.");

        return info;
    }

    /**
     * Issues the command this Object is responsible for.
     *
     * @param args  The command arguments.
     * @param event The event received.
     * @return <code>true</code> if successful, else <code>false</code>.
     */
    @Override
    public Boolean issueCommand(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        if (args.length < 1) {
            //Too few args
            MessageManager.sendMessage(MessageManager.getMessage("Notification.Args.Few", settings), event);
        } else {
            switch (args[0].toLowerCase()) {
                case "add":
                    //=project add <name> <prefix>
                    moduleAdd(args, event, settings);
                    break;
                case "remove":
                    //=project remove <name>
                    moduleRemove(args, event, settings);
                    break;
                case "list":
                    //=project list
                    moduleList(event, settings);
                    break;
                default:
                    MessageManager.sendMessage(MessageManager.getMessage("Notification.Args.Invalid", settings), event);
                    break;
            }
        }
        return false;
    }

    private void moduleAdd(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        //=project add <name> <prefix>
        if (args.length == 3) {
            String name = args[1];
            String prefix = args[2];
            if (args[2].length() > 16)
                prefix = prefix.substring(0, 15); //Cap at 16 just to prevent people from being dumb.

            if (DatabaseManager.getManager().getProject(event.getGuild().getLongID(), name) == null) {
                Project project = new Project(event.getGuild().getLongID(), name);
                project.setPrefix(prefix);
                DatabaseManager.getManager().updateProject(project);

                MessageManager.sendMessage(MessageManager.getMessage("Project.Add.Success", settings), event);
            } else {
                MessageManager.sendMessage(MessageManager.getMessage("Project.Add.Already", settings), event);
            }
        } else {
            MessageManager.sendMessage(MessageManager.getMessage("Notification.Args.Few", settings), event);
        }
    }

    private void moduleRemove(String[] args, MessageReceivedEvent event, GuildSettings settings) {
        //=project remove <name>
        if (args.length == 2) {
            String name = args[1];
            if (DatabaseManager.getManager().getProject(event.getGuild().getLongID(), name) != null) {
                DatabaseManager.getManager().removeProject(event.getGuild().getLongID(), name);
                MessageManager.sendMessage(MessageManager.getMessage("Project.Remove.Success", settings), event);
            } else {
                MessageManager.sendMessage(MessageManager.getMessage("Project.Remove.Not", settings), event);
            }
        } else {
            MessageManager.sendMessage(MessageManager.getMessage("Notification.Args.Few", settings), event);
        }
    }

    private void moduleList(MessageReceivedEvent event, GuildSettings settings) {
        EmbedBuilder em = new EmbedBuilder();

        em.withAuthorIcon(Main.getClient().getGuildByID(GlobalVars.serverId).getIconURL());
        em.withAuthorName("TicketBird");
        em.withTitle("All Current Projects/Services");

        for (Project p : DatabaseManager.getManager().getAllProjects(settings.getGuildID())) {
            em.appendField(p.getName(), p.getPrefix(), true);
        }

        em.withFooterText("List of all Projects with their prefix for tickets.");

        em.withColor(GlobalVars.embedColor);

        MessageManager.sendMessage(em.build(), event);
    }
}
package org.dreamexposure.ticketbird.module.command;

import discord4j.core.event.domain.message.MessageCreateEvent;
import discord4j.core.spec.EmbedCreateSpec;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.command.CommandInfo;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Project;
import org.dreamexposure.ticketbird.utils.GlobalVars;

import java.util.ArrayList;
import java.util.function.Consumer;

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
        info.setExample("=Project <function> (value)");

        info.getSubCommands().put("add", "Adds a new project to your TicketBird.");
        info.getSubCommands().put("remove", "Removes a project from  your TicketBird.");
        info.getSubCommands().put("list", "Lists all active projects/services for tickets.");
        info.getSubCommands().put("toggle", "Toggles whether or not to use projects for sorting.");

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
    public boolean issueCommand(String[] args, MessageCreateEvent event, GuildSettings settings) {
        //noinspection OptionalGetWithoutIsPresent
        if (!settings.getStaff().contains(event.getMember().get().getId())) {
            MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Perm.CONTROL_ROLE", settings), event);
            return false;
        }
        if (args.length < 1) {
            //Too few args
            MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Few", settings), event);
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
                case "toggle":
                    //=project toggle
                    moduleToggle(event, settings);
                    break;
                default:
                    MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Invalid", settings), event);
                    break;
            }
        }
        return false;
    }

    private void moduleAdd(String[] args, MessageCreateEvent event, GuildSettings settings) {
        //=project add <name> <prefix>
        if (args.length == 3) {
            String name = args[1];
            String prefix = args[2];
            if (args[2].length() > 16)
                prefix = prefix.substring(0, 15); //Cap at 16 just to prevent people from being dumb.

            if (DatabaseManager.getManager().getProject(settings.getGuildID(), name) == null) {
                Project project = new Project(settings.getGuildID(), name);
                project.setPrefix(prefix);
                DatabaseManager.getManager().updateProject(project);

                MessageManager.sendMessageAsync(MessageManager.getMessage("Project.Add.Success", settings), event);
            } else {
                MessageManager.sendMessageAsync(MessageManager.getMessage("Project.Add.Already", settings), event);
            }
        } else {
            MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Few", settings), event);
        }
    }

    private void moduleRemove(String[] args, MessageCreateEvent event, GuildSettings settings) {
        //=project remove <name>
        if (args.length == 2) {
            String name = args[1];
            if (DatabaseManager.getManager().getProject(settings.getGuildID(), name) != null) {
                DatabaseManager.getManager().removeProject(settings.getGuildID(), name);
                MessageManager.sendMessageAsync(MessageManager.getMessage("Project.Remove.Success", settings), event);
            } else {
                MessageManager.sendMessageAsync(MessageManager.getMessage("Project.Remove.Not", settings), event);
            }
        } else {
            MessageManager.sendMessageAsync(MessageManager.getMessage("Notification.Args.Few", settings), event);
        }
    }

    private void moduleList(MessageCreateEvent event, GuildSettings settings) {
        Consumer<EmbedCreateSpec> embed = spec -> {
            spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);
            spec.setTitle("All Current Projects/Services");

            for (Project p : DatabaseManager.getManager().getAllProjects(settings.getGuildID())) {
                spec.addField(p.getName(), p.getPrefix(), true);
            }

            spec.setFooter("List of all Projects with their prefix for tickets.", null);

            spec.setColor(GlobalVars.embedColor);
        };

        MessageManager.sendMessageAsync(embed, event);
    }

    private void moduleToggle(MessageCreateEvent event, GuildSettings settings) {
        settings.setUseProjects(!settings.isUseProjects());
        DatabaseManager.getManager().updateSettings(settings);

        MessageManager.sendMessageAsync("Projects have been toggled to: " + settings.isUseProjects(), event);
    }
}

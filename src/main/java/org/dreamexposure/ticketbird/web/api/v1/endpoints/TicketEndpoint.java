package org.dreamexposure.ticketbird.web.api.v1.endpoints;

import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.message.ChannelManager;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.guild.GuildSettings;
import org.dreamexposure.ticketbird.objects.guild.Project;
import org.dreamexposure.ticketbird.objects.guild.Ticket;
import org.dreamexposure.ticketbird.objects.web.AuthenticationState;
import org.dreamexposure.ticketbird.utils.GeneralUtils;
import org.dreamexposure.ticketbird.web.api.utils.Authentication;
import org.dreamexposure.ticketbird.web.api.utils.ResponseUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sx.blah.discord.handle.obj.IChannel;
import sx.blah.discord.handle.obj.IGuild;
import sx.blah.discord.handle.obj.IUser;
import sx.blah.discord.handle.obj.Permissions;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.EnumSet;

@RestController
@RequestMapping("/api/v1/ticket")
public class TicketEndpoint {
    @PostMapping(value = "/create", produces = "application/json")
    public static String createTicket(HttpServletRequest request, HttpServletResponse response, @RequestBody String requestBody) {
        //Authenticate...
        AuthenticationState authState = Authentication.authenticate(request);
        if (!authState.isSuccess()) {
            response.setStatus(authState.getStatus());
            response.setContentType("application/json");
            return authState.toJson();
        }

        //Okay, now handle actual request.
        try {
            JSONObject rBody = new JSONObject(requestBody);
            if (rBody.has("project") && rBody.has("reason") && rBody.has("guild-id")) {
                IGuild guild = Main.getClient().getGuildByID(rBody.getLong("guild-id"));

                if (guild != null) {
                    Project project = DatabaseManager.getManager().getProject(guild.getLongID(), rBody.getString("project"));

                    if (project != null) {
                        GuildSettings settings = DatabaseManager.getManager().getSettings(guild.getLongID());

                        int ticketNumber = settings.getNextId();
                        settings.setNextId(ticketNumber + 1);
                        DatabaseManager.getManager().updateSettings(settings);


                        IChannel channel = ChannelManager.createChannel("ticket-" + ticketNumber, guild);
                        channel.changeCategory(guild.getCategoryByID(settings.getAwaitingCategory()));

                        //Set channel permissions...
                        EnumSet<Permissions> toAdd = EnumSet.noneOf(Permissions.class);
                        toAdd.add(Permissions.MENTION_EVERYONE);
                        toAdd.add(Permissions.ATTACH_FILES);
                        toAdd.add(Permissions.EMBED_LINKS);
                        toAdd.add(Permissions.SEND_MESSAGES);
                        toAdd.add(Permissions.READ_MESSAGES);
                        toAdd.add(Permissions.READ_MESSAGE_HISTORY);

                        EnumSet<Permissions> toRemove = EnumSet.allOf(Permissions.class);

                        IUser creator = null;
                        if (rBody.has("creator-id")) {
                            creator = guild.getUserByID(rBody.getLong("creator-id"));
                        }

                        channel.overrideRolePermissions(guild.getEveryoneRole(), EnumSet.noneOf(Permissions.class), toRemove);
                        if (creator != null)
                            channel.overrideUserPermissions(creator, toAdd, EnumSet.noneOf(Permissions.class));

                        for (long uid : settings.getStaff()) {
                            channel.overrideUserPermissions(guild.getUserByID(uid), toAdd, EnumSet.noneOf(Permissions.class));
                        }

                        //Register ticket in database.
                        Ticket ticket = new Ticket(guild.getLongID(), ticketNumber);
                        ticket.setChannel(channel.getLongID());
                        ticket.setCategory(settings.getAwaitingCategory());
                        if (creator != null)
                            ticket.setCreator(creator.getLongID());
                        ticket.setLastActivity(System.currentTimeMillis());

                        DatabaseManager.getManager().updateTicket(ticket);

                        //Send message
                        String msgOr = MessageManager.getMessage("Ticket.Open", settings);
                        String msg;
                        if (creator != null)
                            msg = msgOr.replace("%creator%", creator.mention(true)).replace("%content%", rBody.getString("reason"));
                        else
                            msg = msgOr.replace("%creator%", "").replace("%content%", rBody.getString("reason"));

                        MessageManager.sendMessage(msg, channel);

                        //Check some other stuffs....
                        if (rBody.has("description"))
                            MessageManager.sendMessage("Further info: ```" + rBody.getString("description") + "```", channel);

                        if (rBody.has("link"))
                            MessageManager.sendMessage("Link: " + rBody.getString("link"), channel);

                        //Set response....
                        response.setContentType("application/json");
                        response.setStatus(200);
                        JSONObject bod = new JSONObject();
                        bod.put("Message", "Success!");
                        bod.put("Ticket", ticketNumber);

                        //Lets update the static message!
                        GeneralUtils.updateStaticMessage(guild, settings);


                        return bod.toString();
                    } else {
                        response.setContentType("application/json");
                        response.setStatus(404);
                        return ResponseUtils.getJsonResponseMessage("Project not Found");
                    }
                } else {
                    response.setContentType("application/json");
                    response.setStatus(404);
                    return ResponseUtils.getJsonResponseMessage("Guild not Found");
                }
            } else {
                response.setContentType("application/json");
                response.setStatus(400);
                return ResponseUtils.getJsonResponseMessage("Bad Request");
            }
        } catch (JSONException e) {
            e.printStackTrace();

            response.setContentType("application/json");
            response.setStatus(400);
            return ResponseUtils.getJsonResponseMessage("Bad Request");
        } catch (Exception e) {
            Logger.getLogger().exception(null, "[WEB-API] Internal get guild settings error", e, TicketEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }
}
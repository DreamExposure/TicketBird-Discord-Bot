package org.dreamexposure.ticketbird.web.api.v1.endpoints;

import discord4j.core.object.PermissionOverwrite;
import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.Member;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.util.Permission;
import discord4j.core.object.util.PermissionSet;
import discord4j.core.object.util.Snowflake;
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
import reactor.core.publisher.Mono;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings({"ConstantConditions", "Duplicates"})
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
                Guild guild = Main.getClient().getGuildById(Snowflake.of(rBody.getLong("guild-id"))).block();

                if (guild != null) {
                    Project project = DatabaseManager.getManager().getProject(guild.getId(), rBody.getString("project"));

                    if (project != null) {
                        GuildSettings settings = DatabaseManager.getManager().getSettings(guild.getId());

                        int ticketNumber = settings.getNextId();
                        settings.setNextId(ticketNumber + 1);
                        DatabaseManager.getManager().updateSettings(settings);


                        TextChannel channel = ChannelManager.createChannel("ticket-" + ticketNumber, "", settings.getAwaitingCategory(), guild);

                        //Set channel permissions...
                        PermissionSet toAdd = PermissionSet
                                .of(Permission.MENTION_EVERYONE,
                                        Permission.ATTACH_FILES,
                                        Permission.EMBED_LINKS,
                                        Permission.SEND_MESSAGES,
                                        Permission.READ_MESSAGE_HISTORY,
                                        Permission.VIEW_CHANNEL);
                        PermissionSet toRemove = PermissionSet.all();

                        Member creator = null;
                        if (rBody.has("creator-id")) {
                            creator = guild.getMemberById(Snowflake.of(rBody.getLong("creator-id"))).onErrorResume(e -> Mono.empty()).block();
                        }

                        //Handle permissions for everyone
                        PermissionOverwrite forEveryone = PermissionOverwrite.forRole(guild.getEveryoneRole().block().getId(), PermissionSet.none(), toRemove);

                        channel.addRoleOverwrite(guild.getEveryoneRole().block().getId(),forEveryone, "New Ticket Created, deny everyone access").subscribe();

                        //Handle permissions for ticket creator
                        if (creator != null) {
                            PermissionOverwrite forCreator = PermissionOverwrite.forMember(creator.getId(), toAdd, PermissionSet.none());

                            channel.addMemberOverwrite(creator.getId(), forCreator, "New Ticket Created, allow creator access").subscribe();
                        }


                        //Handle permissions for ticketbird staff
                        for (Snowflake uid : settings.getStaff()) {
                            if (guild.getMemberById(uid).onErrorResume(e -> Mono.empty()).block() != null) {
                                PermissionOverwrite overwrite = PermissionOverwrite.forMember(uid, toAdd, PermissionSet.none());

                                channel.addMemberOverwrite(uid, overwrite, "New Ticket Created, allow staff access").subscribe();
                            }
                        }

                        //Register ticket in database.
                        Ticket ticket = new Ticket(guild.getId(), ticketNumber);
                        ticket.setChannel(channel.getId());
                        ticket.setCategory(settings.getAwaitingCategory());
                        if (creator != null)
                            ticket.setCreator(creator.getId());
                        ticket.setLastActivity(System.currentTimeMillis());

                        DatabaseManager.getManager().updateTicket(ticket);

                        //Send message
                        String msgOr = MessageManager.getMessage("Ticket.Open", settings);
                        String msg;
                        if (creator != null)
                            msg = msgOr.replace("%creator%", creator.getMention()).replace("%content%", rBody.getString("reason"));
                        else
                            msg = msgOr.replace("%creator%", "").replace("%content%", rBody.getString("reason"));

                        MessageManager.sendMessageAsync(msg, channel);

                        //Check some other stuffs....
                        if (rBody.has("description"))
                            MessageManager.sendMessageAsync("Further info: ```" + rBody.getString("description") + "```", channel);

                        if (rBody.has("link"))
                            MessageManager.sendMessageAsync("Link: " + rBody.getString("link"), channel);

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
            Logger.getLogger().exception(null, "[WEB-API] Internal get guild settings error", e, true, TicketEndpoint.class);

            response.setContentType("application/json");
            response.setStatus(500);
            return ResponseUtils.getJsonResponseMessage("Internal Server Error");
        }
    }
}
package org.dreamexposure.ticketbird.logger;

import discord4j.core.object.entity.Guild;
import discord4j.core.object.entity.TextChannel;
import discord4j.core.object.entity.User;
import discord4j.core.spec.EmbedCreateSpec;
import org.dreamexposure.ticketbird.Main;
import org.dreamexposure.ticketbird.message.MessageManager;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.function.Consumer;

public class Logger {
    private static Logger instance;
    private String exceptionsFile;
    private String apiFile;
    private String debugFile;

    private Logger() {
    } //Prevent initialization

    public static Logger getLogger() {
        if (instance == null) {
            instance = new Logger();
        }
        return instance;
    }

    public void init() {
        //Create files...
        String timestamp = new SimpleDateFormat("dd-MM-yyyy-hh.mm.ss").format(System.currentTimeMillis());

        exceptionsFile = BotSettings.LOG_FOLDER.get() + "/" + timestamp + "-exceptions.log";
        apiFile = BotSettings.LOG_FOLDER.get() + "/" + timestamp + "-api.log";
        debugFile = BotSettings.LOG_FOLDER.get() + "/" + timestamp + "-debug.log";

        try {
            PrintWriter exceptions = new PrintWriter(exceptionsFile, "UTF-8");
            exceptions.println("INIT --- " + timestamp + " ---");
            exceptions.close();

            PrintWriter api = new PrintWriter(apiFile, "UTF-8");
            api.println("INIT --- " + timestamp + " ---");
            api.close();

            PrintWriter debug = new PrintWriter(debugFile, "UTF-8");
            debug.println("INIT --- " + timestamp + " ---");
            debug.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void exception(User author, String message, Exception e, Class clazz) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String error = sw.toString(); // stack trace as a string
        pw.close();
        try {
            sw.close();
        } catch (IOException e1) {
            //Can ignore silently...
        }

        if (Main.getClient().getSelf().block() != null && Main.getClient().isConnected()) {
            String shortError = error;
            if (error.length() > 1250)
                shortError = error.substring(0, 1250);


            Consumer<EmbedCreateSpec> embed = spec -> {
                spec.setAuthor("TicketBird", GlobalVars.siteUrl, GlobalVars.iconUrl);

                if (author != null) {
                    spec.addField("Author", author.getUsername(), false);
                    spec.setThumbnail(author.getAvatarUrl());
                }
                spec.setColor(GlobalVars.embedColor);
                spec.setFooter(clazz.getName(), null);

                //Send to discord!
                spec.addField("Time", timeStamp, true);
                if (e.getMessage() != null) {
                    if (e.getMessage().length() > 1024)
                        spec.addField("Exception", e.getMessage().substring(0, 1024), true);
                    else
                        spec.addField("Exception", e.getMessage(), true);
                }
                if (message != null)
                    spec.addField("Message", message, true);
            };


            //Get DisCal guild and channel..
            Guild guild = Main.getClient().getGuildById(GlobalVars.serverId).block();
            if (guild != null) {
                TextChannel channel = (TextChannel) guild.getChannelById(GlobalVars.errorLogId).block();

                if (channel != null)
                    MessageManager.sendMessageAsync("```" + shortError + "```", embed, channel);
            }
        }

        try {
            FileWriter exceptions = new FileWriter(exceptionsFile, true);
            exceptions.write("ERROR --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            if (author != null) {
                exceptions.write("user: " + author.getUsername() + "#" + author.getDiscriminator() + GlobalVars.lineBreak);
            }
            if (message != null) {
                exceptions.write("message: " + message + GlobalVars.lineBreak);
            }
            exceptions.write(error + GlobalVars.lineBreak);
            exceptions.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void debug(User author, String message, String info, Class clazz) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());


        //ALWAYS LOG TO FILE!
        try {
            FileWriter file = new FileWriter(debugFile, true);
            file.write("DEBUG --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            if (author != null) {
                file.write("user: " + author.getUsername() + "#" + author.getDiscriminator() + GlobalVars.lineBreak);
            }
            if (message != null) {
                file.write("message: " + message + GlobalVars.lineBreak);
            }
            if (info != null) {
                file.write("info: " + info + GlobalVars.lineBreak);
            }
            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void debug(String message) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            FileWriter file = new FileWriter(debugFile, true);
            file.write("DEBUG --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            if (message != null) {
                file.write("info: " + message + GlobalVars.lineBreak);
            }
            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void api(String message) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            FileWriter file = new FileWriter(apiFile, true);
            file.write("API --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            file.write("info: " + message + GlobalVars.lineBreak);
            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void api(String message, String ip) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            FileWriter file = new FileWriter(apiFile, true);
            file.write("API --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            file.write("info: " + message + GlobalVars.lineBreak);
            file.write("IP: " + ip + GlobalVars.lineBreak);
            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }

    public void api(String message, String ip, String host, String endpoint) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            FileWriter file = new FileWriter(apiFile, true);
            file.write("API --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            file.write("info: " + message + GlobalVars.lineBreak);
            file.write("IP: " + ip + GlobalVars.lineBreak);
            file.write("Host: " + host + GlobalVars.lineBreak);
            file.write("Endpoint: " + endpoint + GlobalVars.lineBreak);
            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }
    }
}
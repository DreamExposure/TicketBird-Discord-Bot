package org.dreamexposure.ticketbird.logger;

import club.minnced.discord.webhook.WebhookClient;
import club.minnced.discord.webhook.send.WebhookEmbed;
import club.minnced.discord.webhook.send.WebhookEmbedBuilder;
import discord4j.core.object.entity.User;
import org.dreamexposure.ticketbird.TicketBird;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.utils.GlobalVars;

import javax.annotation.Nullable;
import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;

@SuppressWarnings({"Duplicates", "rawtypes"})
public class Logger {
    private static Logger instance;
    private WebhookClient debugClient;
    private WebhookClient exceptionClient;
    private WebhookClient statusClient;

    private String exceptionsFile;
    private String apiFile;
    private String debugFile;

    private Logger() {
    } //Prevent initialization

    public static Logger getLogger() {
        if (instance == null)
            instance = new Logger();
        return instance;
    }

    public void init() {
        //Create webhook clients.
        if (BotSettings.USE_WEBHOOKS.get().equalsIgnoreCase("true")) {
            debugClient = WebhookClient.withUrl(BotSettings.DEBUG_WEBHOOK.get());
            exceptionClient = WebhookClient.withUrl(BotSettings.ERROR_WEBHOOK.get());
            statusClient = WebhookClient.withUrl(BotSettings.STATUS_WEBHOOK.get());
        }

        //Create files...
        String timestamp = new SimpleDateFormat("dd-MM-yyyy-hh.mm.ss").format(System.currentTimeMillis());

        new File(BotSettings.LOG_FOLDER.get()).mkdirs();

        exceptionsFile = BotSettings.LOG_FOLDER.get() + "/exceptions.log";
        apiFile = BotSettings.LOG_FOLDER.get() + "/api.log";
        debugFile = BotSettings.LOG_FOLDER.get() + "/debug.log";

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

    public void exception(@Nullable User author, @Nullable String message, Exception e, boolean postWebhook, Class clazz) {
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

        //ALWAYS LOG TO FILE!
        try {
            FileWriter exceptions = new FileWriter(exceptionsFile, true);
            exceptions.write("ERROR --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            if (author != null)
                exceptions.write("user: " + author.getUsername() + "#" + author.getDiscriminator() + GlobalVars.lineBreak);

            if (message != null)
                exceptions.write("message: " + message + GlobalVars.lineBreak);

            exceptions.write("Class:" + clazz.getName() + GlobalVars.lineBreak);

            exceptions.write(error + GlobalVars.lineBreak);
            exceptions.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        //Post to webhook if wanted.
        if (BotSettings.USE_WEBHOOKS.get().equalsIgnoreCase("true") && postWebhook) {
            if (error.length() > 1500)
                error = error.substring(0, 1500);

            WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle("Exception", null))
                    .addField(new WebhookEmbed
                            .EmbedField(true, "Shard Index", TicketBird.getShardIndex() + ""))
                    .addField(new WebhookEmbed
                            .EmbedField(false, "Class", clazz.getName()))
                    .setDescription(error)
                    .setColor(GlobalVars.embedColor.getRGB())
                    .setTimestamp(Instant.now());

            if (author != null) {
                builder.setAuthor(new WebhookEmbed
                        .EmbedAuthor(author.getUsername(), author.getAvatarUrl(), null));
            }
            if (message != null) {
                builder.addField(new WebhookEmbed.EmbedField(false, "Message", message));
            }

            exceptionClient.send(builder.build());
        }
    }

    public void debug(@Nullable User author, String message, @Nullable String info, boolean postWebhook, Class clazz) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());
        //ALWAYS LOG TO FILE!
        try {
            FileWriter file = new FileWriter(debugFile, true);
            file.write("DEBUG --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            if (author != null)
                file.write("user: " + author.getUsername() + "#" + author.getDiscriminator() + GlobalVars.lineBreak);

            if (message != null)
                file.write("message: " + message + GlobalVars.lineBreak);

            if (info != null)
                file.write("info: " + info + GlobalVars.lineBreak);

            file.write("Class: " + clazz.getName() + GlobalVars.lineBreak);

            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        //Post to webhook if wanted.
        if (BotSettings.USE_WEBHOOKS.get().equalsIgnoreCase("true") && postWebhook) {
            WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle("Debug", null))
                    .addField(new WebhookEmbed
                            .EmbedField(true, "Shard Index", TicketBird.getShardIndex() + ""))
                    .setDescription(message)
                    .setColor(GlobalVars.embedColor.getRGB())
                    .setTimestamp(Instant.now());

            if (author != null) {
                builder.setAuthor(new WebhookEmbed
                        .EmbedAuthor(author.getUsername(), author.getAvatarUrl(), null));
            }
            if (info != null) {
                builder.addField(new WebhookEmbed.EmbedField(false, "Info", info));
            }

            debugClient.send(builder.build());
        }
    }

    public void debug(String message, boolean postWebhook) {
        String timeStamp = new SimpleDateFormat("yyyy/MM/dd-HH:mm:ss").format(Calendar.getInstance().getTime());

        try {
            FileWriter file = new FileWriter(debugFile, true);
            file.write("DEBUG --- " + timeStamp + " ---" + GlobalVars.lineBreak);
            if (message != null)
                file.write("info: " + message + GlobalVars.lineBreak);

            file.close();
        } catch (IOException io) {
            io.printStackTrace();
        }

        //Post to webhook if wanted.
        if (BotSettings.USE_WEBHOOKS.get().equalsIgnoreCase("true") && postWebhook) {
            WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle("Debug", null))
                    .addField(new WebhookEmbed
                            .EmbedField(true, "Shard Index", TicketBird.getShardIndex() + ""))
                    .setDescription(message)
                    .setColor(GlobalVars.embedColor.getRGB())
                    .setTimestamp(Instant.now());

            debugClient.send(builder.build());
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

    public void status(String message, @Nullable String info) {
        //Post to webhook if wanted.
        if (BotSettings.USE_WEBHOOKS.get().equalsIgnoreCase("true")) {
            WebhookEmbedBuilder builder = new WebhookEmbedBuilder()
                    .setTitle(new WebhookEmbed.EmbedTitle("Status", null))
                    .addField(new WebhookEmbed
                            .EmbedField(true, "Shard Index", TicketBird.getShardIndex() + ""))
                    .setDescription(message)
                    .setColor(GlobalVars.embedColor.getRGB())
                    .setTimestamp(Instant.now());

            if (info != null) {
                builder.addField(new WebhookEmbed
                        .EmbedField(false, "Info", info));
            }

            statusClient.send(builder.build());
        }
    }
}

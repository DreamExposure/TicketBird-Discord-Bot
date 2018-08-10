package org.dreamexposure.ticketbird.web.spark;

import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.api.UserAPIAccount;
import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.dreamexposure.ticketbird.web.api.v1.endpoints.TicketEndpoint;
import spark.ModelAndView;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class SparkManager {
    @SuppressWarnings("ThrowableNotThrown")
    public static void initSpark() {
        if (BotSettings.RUN_API.get().equalsIgnoreCase("true")) {
            port(Integer.valueOf(BotSettings.PORT.get()));

            staticFileLocation("/web/public"); // Main site location

            notFound(((request, response) -> {
                response.redirect("/", 301);
                return response.body();
            }));

            //Register the API Endpoints
            before("/api/*", (request, response) -> {
                if (!request.requestMethod().equalsIgnoreCase("POST")) {
                    Logger.getLogger().api("Denied '" + request.requestMethod() + "' access", request.ip());
                    halt(405, "Method not allowed");
                }
                //Requires "Authorization Header
                if (request.headers().contains("Authorization")) {
                    String key = request.headers("Authorization");
                    UserAPIAccount acc = DatabaseManager.getManager().getAPIAccount(key);
                    if (acc != null) {
                        if (acc.isBlocked()) {
                            Logger.getLogger().api("Attempted to use blocked API Key: " + acc.getAPIKey(), request.ip());
                            halt(401, "Unauthorized");
                        } else {
                            //Everything checks out!
                            acc.setUses(acc.getUses() + 1);
                            DatabaseManager.getManager().updateAPIAccount(acc);
                        }
                    } else {
                        Logger.getLogger().api("Attempted to use invalid API Key: " + key, request.ip());
                        halt(401, "Unauthorized");
                    }
                } else {
                    Logger.getLogger().api("Attempted to use API without authorization header", request.ip());
                    halt(400, "Bad Request");
                }
                //Only accept json because its easier to parse and handle.
                if (!request.contentType().equalsIgnoreCase("application/json")) {
                    halt(400, "Bad Request");
                }
            });

            //API endpoints
            path("/api/v1", () -> {
                before("/*", (q, a) -> Logger.getLogger().api("Received API Call", q.ip(), q.host(), q.pathInfo()));
                path("/ticket", () -> {
                    post("/create", TicketEndpoint::createTicket);
                });
            });

            Map<String, Object> m = new HashMap<>();
            m.put("loggedIn", false);
            m.put("client", BotSettings.ID.get());
            m.put("year", LocalDate.now().getYear());

            //Templates and pages...
            get("/", (rq, rs) -> new ModelAndView(m, "pages/index"), new ThymeleafTemplateEngine());
            get("/home", (rq, rs) -> new ModelAndView(m, "pages/index"), new ThymeleafTemplateEngine());
            get("/about", (rq, rs) -> new ModelAndView(m, "pages/about"), new ThymeleafTemplateEngine());
            get("/commands", (rq, rs) -> new ModelAndView(m, "pages/commands"), new ThymeleafTemplateEngine());
            get("/setup", (rq, rs) -> new ModelAndView(m, "pages/setup"), new ThymeleafTemplateEngine());
            get("/policy/privacy", (rq, rs) -> new ModelAndView(m, "pages/policy/privacy"), new ThymeleafTemplateEngine());
        }
    }
}

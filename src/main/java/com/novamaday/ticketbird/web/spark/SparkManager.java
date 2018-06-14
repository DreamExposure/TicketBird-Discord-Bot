package com.novamaday.ticketbird.web.spark;

import com.novamaday.ticketbird.objects.bot.BotSettings;
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

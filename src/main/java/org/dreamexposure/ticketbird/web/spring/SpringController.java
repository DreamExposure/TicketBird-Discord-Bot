package org.dreamexposure.ticketbird.web.spring;

import org.dreamexposure.ticketbird.objects.bot.BotSettings;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"unchecked", "unused"})
@Controller
public class SpringController {
    private static Map m = new HashMap();


    public static void makeModel() {
        m.put("loggedIn", false);
        m.put("client", BotSettings.ID.get());
        m.put("year", LocalDate.now().getYear());
    }

    //Main pages
    @RequestMapping(value = {"/", "/home"})
    public String home(Map<String, Object> model, HttpServletRequest req) {
        model.putAll(m);
        return "index";
    }

    @RequestMapping("/about")
    public String about(Map<String, Object> model, HttpServletRequest req) {
        model.clear();
        model.putAll(m);
        return "about";
    }

    @RequestMapping("/commands")
    public String commands(Map<String, Object> model, HttpServletRequest req) {
        model.clear();
        model.putAll(m);
        return "commands";
    }

    @RequestMapping("/setup")
    public String setup(Map<String, Object> model, HttpServletRequest req) {
        model.clear();
        model.putAll(m);
        return "setup";
    }

    //Policy pages
    @RequestMapping("/policy/privacy")
    public String privacyPolicy(Map<String, Object> model, HttpServletRequest req) {
        model.clear();
        model.putAll(m);
        return "policy/privacy";
    }
}
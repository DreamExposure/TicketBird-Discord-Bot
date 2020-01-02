package org.dreamexposure.ticketbird.web.api.utils;

import org.dreamexposure.ticketbird.database.DatabaseManager;
import org.dreamexposure.ticketbird.logger.Logger;
import org.dreamexposure.ticketbird.objects.api.UserAPIAccount;
import org.dreamexposure.ticketbird.objects.web.AuthenticationState;

import javax.servlet.http.HttpServletRequest;

public class Authentication {
    public static AuthenticationState authenticate(HttpServletRequest request) {
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            Logger.getLogger().api("Denied '" + request.getMethod() + "' access", request.getRemoteAddr());
            return new AuthenticationState(false).setStatus(405).setReason("Method not allowed");
        }
        //Requires "Authorization Header
        if (request.getHeader("Authorization") != null) {
            String key = request.getHeader("Authorization");

                UserAPIAccount acc = DatabaseManager.getManager().getAPIAccount(key);
                if (acc != null) {
                    if (acc.isBlocked()) {
                        Logger.getLogger().api("Attempted to use blocked API Key: " + acc.getAPIKey(), request.getRemoteAddr());

                        return new AuthenticationState(false).setStatus(401).setReason("Unauthorized");
                    } else {
                        //Everything checks out!
                        acc.setUses(acc.getUses() + 1);
                        DatabaseManager.getManager().updateAPIAccount(acc);

                        return new AuthenticationState(true).setStatus(200).setReason("Success");
                    }
                } else {
                    Logger.getLogger().api("Attempted to use invalid API Key: " + key, request.getRemoteAddr());
                    return new AuthenticationState(false).setStatus(401).setReason("Unauthorized");
                }
        } else {
            Logger.getLogger().api("Attempted to use API without authorization header", request.getRemoteAddr());
            return new AuthenticationState(false).setStatus(400).setReason("Bad Request");
        }
    }
}

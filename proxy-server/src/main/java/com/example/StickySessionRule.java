package com.example;

import com.netflix.loadbalancer.Server;
import com.netflix.loadbalancer.ZoneAvoidanceRule;
import com.netflix.zuul.context.RequestContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author Alejandro Duarte.
 */
public class StickySessionRule extends ZoneAvoidanceRule {

    public static final String COOKIE_NAME = StickySessionRule.class.getSimpleName();

    
    
    @Override
    public Server choose(Object key) {
        Optional<Cookie> cookie = getCookie();

        if (cookie.isPresent()) {
            Cookie hash = cookie.get();
            List<Server> servers = getLoadBalancer().getReachableServers();
            Optional<Server> serverFound = servers.stream()
                    .filter(s -> hash.getValue().equals("" + s.hashCode()))
                    .findFirst();

            if (serverFound.isPresent()) {
                return serverFound.get();
            }
        }

        return addServer(key);
    }

    private Optional<Cookie> getCookie() {
        HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        if (request != null) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                return Arrays.stream(cookies)
                        .filter(c -> c.getName().equals(COOKIE_NAME))
                        .findFirst();
            }
        }

        return Optional.empty();
    }

    private Server addServer(Object key) {
        Server server = super.choose(key);
        HttpServletResponse response = RequestContext.getCurrentContext().getResponse();
        if (response != null) {          
            Cookie newCookie = new Cookie(COOKIE_NAME, "" + server.hashCode());
            newCookie.setPath("/");
            response.addCookie(newCookie);
        }
        return server;
    }

}

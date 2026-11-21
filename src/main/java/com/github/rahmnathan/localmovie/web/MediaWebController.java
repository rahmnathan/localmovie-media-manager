package com.github.rahmnathan.localmovie.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class MediaWebController {

    // Serves index.html for SPA client-side routing.
    // Excludes /localmovie and /admin API prefixes via regex.
    // More specific API controller paths take precedence regardless.
    @RequestMapping(
            method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS},
            value = { "/", "/{segment:(?!localmovie$|admin$)[\\w\\-]+}", "/{segment:(?!localmovie|admin)[\\w\\-]+}/{*path}" }
    )
    public String index() {
        return "index.html";
    }
}
package com.github.rahmnathan.localmovie.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

@Controller
public class MediaWebController {

    @RequestMapping(
            method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.OPTIONS},
            value = { "/", "/{x:[\\w\\-]+}", "/{x:^(?!localmovie$).*$}/**/{y:[\\w\\-]+}" }
    )
    public String index() {
        return "index.html";
    }
}
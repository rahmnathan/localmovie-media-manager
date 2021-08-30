package com.github.rahmnathan.localmovie.web.webapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class MediaWebController {

    @RequestMapping(value = { "/", "/{x:[\\w\\-]+}", "/{x:^(?!localmovie$).*$}/**/{y:[\\w\\-]+}" })
    public String index() {
        return "index.html";
    }
}
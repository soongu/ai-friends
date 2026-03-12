package kr.spartaclub.aifriends.common.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;


@Controller
@Slf4j
public class RouteController  {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/soulmate/new")
    public String soulmateNew() {
        return "soulmate-new";
    }

}

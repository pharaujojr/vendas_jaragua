package com.example.vendasjaragua.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @GetMapping("/usuarios.html")
    public String usuarios() {
        return "usuarios";
    }
}

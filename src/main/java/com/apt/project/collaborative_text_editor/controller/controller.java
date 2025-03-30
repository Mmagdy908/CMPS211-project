package com.apt.project.collaborative_text_editor.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class controller {
    @GetMapping("/")
    public String hello(){
        return "Hello from the server";
    }
}

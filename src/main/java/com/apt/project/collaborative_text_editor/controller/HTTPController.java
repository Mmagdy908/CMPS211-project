package com.apt.project.collaborative_text_editor.controller;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.apt.project.collaborative_text_editor.Utility;

@RestController
public class HTTPController {
    int ids=0;

    @GetMapping("/")
    public String hello(){
        return "Hello from the server";
    }

    @PostMapping("/users")
    @CrossOrigin(origins = "*")
    public int create( String username) throws IOException {
        return ids++;
    }
}

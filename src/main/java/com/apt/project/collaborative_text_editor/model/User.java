package com.apt.project.collaborative_text_editor.model;


import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class User   {
    private int id;
    private String username;
    private int cursorPosition;
}

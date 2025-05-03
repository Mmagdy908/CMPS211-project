package com.apt.project.collaborative_text_editor.model;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
public class User  implements Serializable {
    private String id;
    private String username;
    private int cursorPosition;
}

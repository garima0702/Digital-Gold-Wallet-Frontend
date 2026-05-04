package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping({"/", "/home"})
    public String home(Model model) {

        // Just pass names for now (images in static/images)
        model.addAttribute("members", new String[]{
                "ashu", "vinayak", "shivam", "shruti", "garima"
        });

        return "index";
    }
}
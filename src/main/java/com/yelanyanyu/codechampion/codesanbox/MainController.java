package com.yelanyanyu.codechampion.codesanbox;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author yelanyanyu@zjxu.edu.cn
 * @version 1.0
 */
@RestController("/")
public class MainController {
    @GetMapping("/index")
    public String index() {
        return "index";
    }
}

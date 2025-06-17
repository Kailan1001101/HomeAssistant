package com.homeassistant.ai.controller;

import com.homeassistant.ai.dto.chatgptDTO;
import com.homeassistant.ai.service.chatgptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/chat")
public class chatgptController {

    private final chatgptService chatservice;
    public chatgptController(chatgptService chatservice){
        this.chatservice = chatservice;
    }

    @PostMapping
    public ResponseEntity<String> chat(@RequestBody chatgptDTO message) {
        return ResponseEntity.ok(chatservice.sendMessage(message.getContent()));
    }
}

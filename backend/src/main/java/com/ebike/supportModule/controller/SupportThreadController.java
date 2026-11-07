package com.ebike.supportModule.controller;
import com.ebike.supportModule.service.SupportThreadService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/support/tickets/{id}/messages")
public class SupportThreadController {
 private final SupportThreadService service;public SupportThreadController(SupportThreadService service){this.service=service;}
 public record Message(String content){}
 @GetMapping public Object messages(Authentication a,@PathVariable long id){return service.messages(a.getName(),id);}
 @PostMapping public void reply(Authentication a,@PathVariable long id,@RequestBody Message body){service.reply(a.getName(),id,body.content());}
}

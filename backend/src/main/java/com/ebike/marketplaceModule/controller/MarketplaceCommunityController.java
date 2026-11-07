package com.ebike.marketplaceModule.controller;
import com.ebike.marketplaceModule.service.MarketplaceCommunityService;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/marketplace/community")
public class MarketplaceCommunityController {
 private final MarketplaceCommunityService service;
 public MarketplaceCommunityController(MarketplaceCommunityService service){this.service=service;}
 public record Content(String content,UUID parentId){}
 public record Report(String type,String target,String reason,String description){}
 public record Resolution(String action,String note){}
 @GetMapping("/comments/{id}") public Map<String,Object> comments(@PathVariable UUID id,Authentication a,@RequestParam(defaultValue="0") int page){return service.comments(id,a==null?null:a.getName(),page);}
 @PostMapping("/comments/{id}") public void add(@PathVariable UUID id,Authentication a,@RequestBody Content body){service.addComment(a.getName(),id,body.parentId(),body.content());}
 @PostMapping("/comment/{id}/{action}") public void change(@PathVariable UUID id,@PathVariable String action,Authentication a,@RequestBody(required=false) Content body){service.commentAction(a.getName(),id,action,body==null?null:body.content());}
 @GetMapping("/comments-mine") public List<Map<String,Object>> myComments(Authentication a){return service.myComments(a.getName());}
 @GetMapping("/reviews-mine") public List<Map<String,Object>> reviews(Authentication a){return service.myReviews(a.getName());}
 @PostMapping("/reviews/{id}/reply") public void reply(Authentication a,@PathVariable UUID id,@RequestBody Content body){service.reviewReply(a.getName(),id,body.content());}
 @GetMapping("/blocks") public List<Map<String,Object>> blocks(Authentication a){return service.blocks(a.getName());}
 @PostMapping("/blocks/{id}") public void block(Authentication a,@PathVariable long id,@RequestParam(defaultValue="true") boolean enabled){service.block(a.getName(),id,enabled);}
 @PostMapping("/reports") public void report(Authentication a,@RequestBody Report body){service.report(a.getName(),body.type(),body.target(),body.reason(),body.description());}
 @GetMapping("/reports") public List<Map<String,Object>> reports(Authentication a,@RequestParam(defaultValue="false") boolean staff){return service.reports(a.getName(),staff);}
 @PostMapping("/reports/{id}/resolve") public void resolve(Authentication a,@PathVariable long id,@RequestBody Resolution body){service.resolveReport(a.getName(),id,body.action(),body.note());}
}

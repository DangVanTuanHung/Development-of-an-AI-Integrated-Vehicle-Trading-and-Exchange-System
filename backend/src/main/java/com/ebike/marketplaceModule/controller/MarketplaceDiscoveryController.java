package com.ebike.marketplaceModule.controller;
import com.ebike.marketplaceModule.service.MarketplaceDiscoveryService;
import java.util.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/marketplace/discovery")
public class MarketplaceDiscoveryController {
 private final MarketplaceDiscoveryService service;
 public MarketplaceDiscoveryController(MarketplaceDiscoveryService service){this.service=service;}
 public record Saved(String name,Map<String,String> filters){}
 public record Compare(List<UUID> ids){}
 @GetMapping("/search") public Object search(@RequestParam Map<String,String> filters,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="12") int size){return service.search(filters,page,size);}
 @GetMapping("/compare") public Object compare(@RequestParam List<UUID> ids){return service.compare(ids);}
 @GetMapping("/saved") public Object saved(Authentication a){return service.saved(a.getName());}
 @PostMapping("/saved") public void save(Authentication a,@RequestBody Saved body){service.save(a.getName(),body.name(),body.filters()==null?Map.of():body.filters());}
 @DeleteMapping("/saved/{id}") public void remove(Authentication a,@PathVariable long id){service.remove(a.getName(),id);}
 @PostMapping("/recent/{id}") public void view(Authentication a,@PathVariable UUID id){service.view(a.getName(),id);}
 @GetMapping("/recent") public Object recent(Authentication a){return service.recent(a.getName());}
 @PostMapping("/comparison") public void saveComparison(Authentication a,@RequestBody Compare body){if(body.ids()==null)throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST);service.saveComparison(a.getName(),body.ids());}
 @GetMapping("/comparison") public Object comparison(Authentication a){return service.comparison(a.getName());}
}

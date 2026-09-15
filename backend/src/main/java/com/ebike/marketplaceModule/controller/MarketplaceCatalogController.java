package com.ebike.marketplaceModule.controller;

import java.util.*;
import com.ebike.adminModule.entity.AdminAuditLog;
import com.ebike.adminModule.repository.AdminAuditLogRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/admin/vehicle-catalog")
@PreAuthorize("hasRole('ADMIN')")
public class MarketplaceCatalogController {
 private final JdbcTemplate jdbc;
 private final AdminAuditLogRepository audit;
 public MarketplaceCatalogController(JdbcTemplate jdbc, AdminAuditLogRepository audit) {this.jdbc=jdbc;this.audit=audit;}
 private String table(String kind) {
  return switch(kind) {case "categories" -> "marketplace.vehicle_categories";case "brands" -> "marketplace.vehicle_brands";case "models" -> "marketplace.vehicle_models";default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND);};
 }
 @GetMapping("/{kind}") public List<Map<String,Object>> list(@PathVariable String kind) {return jdbc.queryForList("select * from "+table(kind)+" order by name");}
 public record Entry(Long id,String name,String slug,Boolean active,Long brandId,Long categoryId) {}
 @PostMapping("/{kind}") @Transactional
 public Map<String,Object> save(Authentication auth,@PathVariable String kind,@RequestBody Entry entry) {
  String table=table(kind);
  if(entry.name()==null || entry.name().isBlank() || entry.name().length()>120 || entry.slug()==null || !entry.slug().matches("[a-z0-9]+(?:-[a-z0-9]+)*") || entry.slug().length()>140 || entry.active()==null)
   throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tên tối đa 120 ký tự, mã gồm chữ thường, số và dấu gạch ngang");
  Long id=entry.id();
  if("models".equals(kind)) {
   if(entry.brandId()==null || entry.categoryId()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Chọn hãng và danh mục");
   Integer valid=jdbc.queryForObject("select count(*) from marketplace.vehicle_brands b cross join marketplace.vehicle_categories c where b.id=? and c.id=? and b.active and c.active",Integer.class,entry.brandId(),entry.categoryId());
   if(valid==null || valid==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Hãng hoặc danh mục đã ngừng sử dụng");
   if(id==null) id=jdbc.queryForObject("insert into "+table+"(name,slug,active,brand_id,category_id) values (?,?,?,?,?) returning id",Long.class,entry.name().trim(),entry.slug(),entry.active(),entry.brandId(),entry.categoryId());
   else if(jdbc.update("update "+table+" set name=?,slug=?,active=?,brand_id=?,category_id=? where id=?",entry.name().trim(),entry.slug(),entry.active(),entry.brandId(),entry.categoryId(),id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  } else {
   if(id==null) id=jdbc.queryForObject("insert into "+table+"(name,slug,active) values (?,?,?) returning id",Long.class,entry.name().trim(),entry.slug(),entry.active());
   else if(jdbc.update("update "+table+" set name=?,slug=?,active=? where id=?",entry.name().trim(),entry.slug(),entry.active(),id)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
  }
  AdminAuditLog log=new AdminAuditLog();log.setActor(auth.getName());log.setAction("CATALOG_SAVE");log.setTarget(kind+" #"+id+" "+entry.name());log.setIpAddress("system");audit.save(log);
  return Map.of("id",id);
 }
}

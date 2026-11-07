package com.ebike.marketplaceModule.service;
import static com.ebike.marketplaceModule.service.TradePolicy.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceDiscoveryService {
 private final JdbcTemplate db;private final MarketplaceTransactionService users;private final MarketplaceListingService listings;private final ObjectMapper json;
 public MarketplaceDiscoveryService(JdbcTemplate db,MarketplaceTransactionService users,MarketplaceListingService listings,ObjectMapper json){this.db=db;this.users=users;this.listings=listings;this.json=json;}
 private String encode(Object o){try{return json.writeValueAsString(o);}catch(Exception e){throw bad("Bộ lọc không hợp lệ.");}}
 public Map<String,String> decode(String s){try{return json.readValue(s,new TypeReference<Map<String,String>>(){});}catch(Exception e){throw bad("Bộ lọc không hợp lệ.");}}
 private String where(Map<String,String> f,List<Object> values){
  StringBuilder sql=new StringBuilder(" where l.status='PUBLISHED'");
  Map<String,String> exact=Map.of("categoryId","l.category_id","brandId","l.brand_id","modelId","l.model_id","province","l.province","condition","l.condition","fuelType","d.fuel_type");
  for(var entry:exact.entrySet()){String value=f.get(entry.getKey());if(value==null||value.isBlank())continue;if(value.length()>120)throw bad("Bộ lọc quá dài.");sql.append(" and ").append(entry.getValue()).append("=?");if(entry.getKey().endsWith("Id")){try{values.add(Long.parseLong(value));}catch(Exception e){throw bad("Mã bộ lọc không hợp lệ.");}}else values.add(value);}
  for(String key:List.of("minPrice","maxPrice","minYear","maxYear","maxMileage")){
   String value=f.get(key);if(value==null||value.isBlank())continue;BigDecimal number;try{number=new BigDecimal(value);if(number.signum()<0||number.precision()>18||number.scale()>2)throw new IllegalArgumentException();}catch(Exception e){throw bad("Giá/năm/quãng đường không hợp lệ.");}
   sql.append(" and ").append(key.endsWith("Price")?"l.price":key.endsWith("Year")?"d.manufacture_year":"d.mileage_km").append(key.startsWith("min")?">=?":"<=?");values.add(number);
  }
  String query=f.get("q");if(query!=null&&!query.isBlank()){if(query.length()>200)throw bad("Từ khóa quá dài.");sql.append(" and (concat_ws(' ',l.title,l.description,b.name,m.name) ilike ?)");values.add("%"+query.trim().replace("\\","\\\\").replace("%","\\%").replace("_","\\_")+"%");}
  return sql.toString();
 }
 private String from(){return " from marketplace.vehicle_listings l left join marketplace.vehicle_details d on d.listing_id=l.id left join marketplace.vehicle_brands b on b.id=l.brand_id left join marketplace.vehicle_models m on m.id=l.model_id";}
 @Transactional(readOnly=true)
 public Map<String,Object> search(Map<String,String> filters,int page,int size){
  if(page<0||page>10000||size<1||size>24)throw bad("Trang không hợp lệ.");var args=new ArrayList<Object>();String predicate=where(filters,args);
  long total=db.queryForObject("select count(*)"+from()+predicate,Long.class,args.toArray());
  String order=switch(filters.getOrDefault("sort","newest")){case "priceAsc"->"l.price asc";case "priceDesc"->"l.price desc";case "mileage"->"d.mileage_km asc nulls last";case "year"->"d.manufacture_year desc nulls last";case "newest"->"l.published_at desc nulls last";default->throw bad("Sắp xếp không hợp lệ.");};
  args.add(size);args.add(page*size);
  var ids=db.queryForList("select l.public_id"+from()+predicate+" order by "+order+",l.id desc limit ? offset ?",UUID.class,args.toArray());
  return Map.of("items",ids.stream().map(listings::detail).toList(),"total",total,"page",page,"hasMore",(long)(page+1)*size<total);
 }
 @Transactional
 public void save(String username,String name,Map<String,String> filters){
  long actor=users.actor(username);if(name==null||name.isBlank()||name.length()>100)throw bad("Tên tìm kiếm từ 1 đến 100 ký tự.");
  var clean=new LinkedHashMap<String,String>();for(String key:List.of("q","categoryId","brandId","modelId","province","condition","fuelType","minPrice","maxPrice","minYear","maxYear","maxMileage","sort"))if(filters.get(key)!=null&&!filters.get(key).isBlank())clean.put(key,filters.get(key));
  search(clean,0,1);if(db.queryForObject("select count(*) from marketplace.saved_searches where user_id=?",Integer.class,actor)>=20)throw conflict("Tối đa 20 tìm kiếm đã lưu.");
  db.update("insert into marketplace.saved_searches(user_id,name,filters) values (?,?,?::jsonb)",actor,name.trim(),encode(clean));
 }
 @Transactional(readOnly=true)
 public List<Map<String,Object>> saved(String username){var rows=db.queryForList("select id,name,filters::text,created_at as \"createdAt\" from marketplace.saved_searches where user_id=? order by id desc",users.actor(username));for(var row:rows)row.put("filters",decode((String)row.get("filters")));return rows;}
 @Transactional
 public void remove(String username,long id){if(db.update("delete from marketplace.saved_searches where id=? and user_id=?",id,users.actor(username))==0)throw forbidden();}
 @Transactional
 public void view(String username,UUID id){long actor=users.actor(username);var car=users.one("select id from marketplace.vehicle_listings where public_id=? and status in ('PUBLISHED','RESERVED','SOLD','EXCHANGED')",id);db.update("insert into marketplace.recent_views(user_id,listing_id) values (?,?) on conflict(user_id,listing_id) do update set viewed_at=now()",actor,car.get("id"));db.update("delete from marketplace.recent_views where user_id=? and listing_id not in (select listing_id from marketplace.recent_views where user_id=? order by viewed_at desc limit 30)",actor,actor);}
 @Transactional(readOnly=true)
 public Object recent(String username){return db.queryForList("select l.public_id from marketplace.recent_views r join marketplace.vehicle_listings l on l.id=r.listing_id where r.user_id=? and l.status in ('PUBLISHED','RESERVED','SOLD','EXCHANGED') order by r.viewed_at desc limit 30",UUID.class,users.actor(username)).stream().map(listings::detail).toList();}
 @Transactional(readOnly=true)
 public Object compare(List<UUID> ids){if(ids.size()>3)throw bad("Chọn tối đa 3 xe.");return ids.stream().distinct().map(id->{users.one("select id from marketplace.vehicle_listings where public_id=? and status in ('PUBLISHED','RESERVED','SOLD','EXCHANGED')",id);return listings.detail(id);}).toList();}
 @Transactional
 public void saveComparison(String username,List<UUID> ids){compare(ids);db.update("insert into marketplace.comparison_lists(user_id,listing_ids) values (?,?::jsonb) on conflict(user_id) do update set listing_ids=excluded.listing_ids,updated_at=now()",users.actor(username),encode(ids));}
 @Transactional(readOnly=true)
 public Object comparison(String username){var rows=db.queryForList("select listing_ids::text from marketplace.comparison_lists where user_id=?",String.class,users.actor(username));if(rows.isEmpty())return List.of();try{return json.readValue(rows.get(0),new TypeReference<List<String>>(){});}catch(Exception ex){return List.of();}}
 public List<UUID> newMatches(Map<String,String> filters,Object created,Object searchId){var args=new ArrayList<Object>();String predicate=where(filters,args);args.add(created);args.add(searchId);return db.queryForList("select l.public_id"+from()+predicate+" and l.published_at>? and not exists(select 1 from marketplace.search_alerts a where a.search_id=? and a.listing_id=l.id) order by l.published_at,l.id limit 100",UUID.class,args.toArray());}
}

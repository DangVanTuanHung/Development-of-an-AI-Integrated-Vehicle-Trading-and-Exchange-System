package com.ebike.marketplaceModule.controller;

import static com.ebike.marketplaceModule.service.TradePolicy.*;
import com.ebike.marketplaceModule.service.MarketplaceTransactionService;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.*;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/marketplace/transactions/{id}/evidence")
public class TradeEvidenceController {
    private final MarketplaceTransactionService trades;private final JdbcTemplate jdbc;private final Path root;
    public TradeEvidenceController(MarketplaceTransactionService trades,JdbcTemplate jdbc,@Value("${app.marketplace.storage.root}") String root){this.trades=trades;this.jdbc=jdbc;this.root=Path.of(root).toAbsolutePath().normalize().resolve("trade-evidence");}
    @PostMapping(consumes="multipart/form-data") @Transactional
    public Map<String,Object> upload(Authentication auth,@PathVariable UUID id,@RequestPart("file") MultipartFile file) throws java.io.IOException {
        long user=trades.actor(auth.getName());var tx=trades.one("select * from marketplace.transactions where public_id=? for update",id);
        if(!trades.participant(tx,user))throw forbidden();if(TERMINAL.contains(tx.get("status")))throw conflict("Giao dịch đã kết thúc.");
        if(file.isEmpty()||file.getSize()>10*1024*1024)throw bad("Chọn ảnh/PDF không quá 10 MB.");
        if(jdbc.queryForObject("select count(*) from marketplace.transaction_evidence where transaction_id=?",Integer.class,tx.get("id"))>=8)throw bad("Mỗi giao dịch tối đa 8 tệp bằng chứng.");
        byte[] bytes=file.getBytes();String mime=file.getContentType();String extension;
        if("image/png".equals(mime)&&bytes.length>8&&bytes[0]==(byte)137&&bytes[1]==80&&bytes[2]==78&&bytes[3]==71)extension=".png";
        else if("image/jpeg".equals(mime)&&bytes.length>3&&bytes[0]==(byte)255&&bytes[1]==(byte)216&&bytes[2]==(byte)255)extension=".jpg";
        else if("image/webp".equals(mime)&&bytes.length>12&&new String(bytes,0,4,StandardCharsets.US_ASCII).equals("RIFF")&&new String(bytes,8,4,StandardCharsets.US_ASCII).equals("WEBP"))extension=".webp";
        else if("application/pdf".equals(mime)&&bytes.length>5&&new String(bytes,0,5,StandardCharsets.US_ASCII).equals("%PDF-"))extension=".pdf";
        else throw bad("Tệp phải là ảnh JPEG/PNG/WebP hoặc PDF hợp lệ.");
        UUID evidence=UUID.randomUUID();String key=evidence+extension;String name=Objects.toString(file.getOriginalFilename(),"Bang-chung").replaceAll("[\\p{Cntrl}/\\\\]","_");if(name.length()>200)name=name.substring(0,200);
        Files.createDirectories(root);Path target=root.resolve(key);
        try{
            Files.write(target,bytes,StandardOpenOption.CREATE_NEW);
            jdbc.update("insert into marketplace.transaction_evidence(public_id,transaction_id,uploaded_by,file_key,file_name,mime_type,file_size) values (?,?,?,?,?,?,?)",evidence,tx.get("id"),user,key,name,mime,bytes.length);
            trades.event(tx.get("id"),user,(String)tx.get("status"),(String)tx.get("status"),"EVIDENCE_UPLOADED","Bổ sung ảnh/biên bản giao dịch.");
        }catch(RuntimeException|java.io.IOException ex){Files.deleteIfExists(target);throw ex;}
        return Map.of("publicId",evidence,"fileName",name);
    }
    @GetMapping("/{evidence}") @Transactional(readOnly=true)
    public ResponseEntity<Resource> file(Authentication auth,@PathVariable UUID id,@PathVariable UUID evidence){
        trades.detail(auth.getName(),id);
        var row=trades.one("select e.* from marketplace.transaction_evidence e join marketplace.transactions t on t.id=e.transaction_id where t.public_id=? and e.public_id=?",id,evidence);
        Path path=root.resolve((String)row.get("file_key")).normalize();if(!path.startsWith(root)||!Files.isRegularFile(path))throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename((String)row.get("file_name"),StandardCharsets.UTF_8).build().toString()).contentType(MediaType.parseMediaType((String)row.get("mime_type"))).body(new FileSystemResource(path));
    }
}

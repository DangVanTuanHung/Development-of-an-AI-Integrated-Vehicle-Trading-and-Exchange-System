package com.ebike.authModule.controller;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth/profile")
public class AuthAvatarController {
    private final UserRepository users;
    private final Path storageRoot;

    public AuthAvatarController(UserRepository users, @Value("${app.marketplace.storage.root}") String storageRoot) {
        this.users = users;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @PostMapping(value = "/avatar", consumes = "multipart/form-data")
    @Transactional
    public Map<String, String> upload(Authentication auth, @RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Vui lòng chọn ảnh");
        if (file.getSize() > 5L * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Ảnh đại diện không được quá 5 MB");
        String mime = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = switch (mime) {
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Chỉ hỗ trợ JPEG, PNG và WebP");
        };
        try {
            String name = "avatar-" + UUID.randomUUID() + extension;
            Files.createDirectories(storageRoot);
            Files.write(storageRoot.resolve(name), file.getBytes(), StandardOpenOption.CREATE_NEW);
            String url = "/api/v1/media/marketplace/" + name;
            User user = users.findByUsername(auth.getName()).orElseThrow();
            user.setAvatarUrl(url);
            users.save(user);
            return Map.of("avatarUrl", url);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể lưu ảnh đại diện", ex); }
    }
}

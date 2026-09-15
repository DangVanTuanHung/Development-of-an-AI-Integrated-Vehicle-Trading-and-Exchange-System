package com.ebike.marketplaceModule.service;

import com.ebike.authModule.entity.User;
import com.ebike.authModule.repository.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MarketplaceConversationService {
    private final JdbcTemplate jdbc;
    private final UserRepository users;
    private final Path storageRoot;
    private final ObjectMapper objectMapper;
    private final Map<String, Long> typingUsers = new ConcurrentHashMap<>();

    public MarketplaceConversationService(JdbcTemplate jdbc, UserRepository users,
            @Value("${app.marketplace.storage.root}") String storageRoot, ObjectMapper objectMapper) {
        this.jdbc = jdbc;
        this.users = users;
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Map<String, Object> open(String username, UUID listingPublicId) {
        User buyer = requireUser(username);
        Map<String, Object> listing = one("select id, seller_id, title from marketplace.vehicle_listings where public_id=?", listingPublicId);
        long listingId = ((Number) listing.get("id")).longValue();
        long sellerId = ((Number) listing.get("seller_id")).longValue();
        if (sellerId == buyer.getId()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Bạn không thể nhắn tin cho chính mình");

        List<Map<String, Object>> existing = jdbc.queryForList("""
            select c.id, c.public_id as "publicId", l.title as "listingTitle"
            from marketplace.conversations c
            join marketplace.vehicle_listings l on l.id=c.listing_id
            where c.listing_id=? and c.conversation_type='LISTING'
              and exists(select 1 from marketplace.conversation_members m where m.conversation_id=c.id and m.user_id=?)
              and exists(select 1 from marketplace.conversation_members m where m.conversation_id=c.id and m.user_id=?)
            limit 1
            """, listingId, buyer.getId(), sellerId);
        if (!existing.isEmpty()) return existing.get(0);

        UUID publicId = UUID.randomUUID();
        Long conversationId = jdbc.queryForObject(
            "insert into marketplace.conversations(public_id, listing_id, conversation_type) values (?, ?, 'LISTING') returning id",
            Long.class, publicId, listingId
        );
        jdbc.update("insert into marketplace.conversation_members(conversation_id,user_id,member_role) values (?,?,'BUYER'), (?,?,'SELLER')",
            conversationId, buyer.getId(), conversationId, sellerId);
        return Map.of("id", conversationId, "publicId", publicId, "listingTitle", listing.get("title"));
    }

    @Transactional
    public List<Map<String, Object>> messages(String username, UUID conversationPublicId) {
        User user = requireUser(username);
        Long conversationId = requireMember(user, conversationPublicId);
        List<Map<String, Object>> messages = jdbc.queryForList("""
            select m.public_id as "publicId", m.sender_id as "senderId", coalesce(u.first_name || ' ' || u.last_name, u.username, 'Hệ thống') as "senderName",
                   m.message_type as "messageType", m.content, m.attachment_key as "attachmentUrl",
                   m.metadata->>'fileName' as "fileName", m.metadata->>'mimeType' as "mimeType",
                   m.metadata->>'latitude' as "latitude", m.metadata->>'longitude' as "longitude",
                   exists(select 1 from marketplace.conversation_members cm where cm.conversation_id=m.conversation_id and cm.user_id<>m.sender_id and cm.last_read_at>=m.created_at) as "read",
                   m.created_at as "createdAt"
            from marketplace.messages m left join ebike_auth.users u on u.id=m.sender_id
            where m.conversation_id=? and m.deleted_at is null order by m.created_at asc
            """, conversationId);
        jdbc.update("update marketplace.conversation_members set last_read_at=now() where conversation_id=? and user_id=?",
            conversationId, user.getId());
        return messages;
    }

    public List<Map<String, Object>> conversations(String username) {
        User user = requireUser(username);
        return jdbc.queryForList("""
            select c.public_id as "publicId", l.public_id as "listingPublicId", l.title as "listingTitle",
                   coalesce(nullif(trim(other_user.first_name || ' ' || other_user.last_name), ''), other_user.username) as "otherName",
                   other_user.avatar_url as "otherAvatarUrl", vi.public_url as "listingImageUrl", l.price as "listingPrice",
                   c.last_message_at as "lastMessageAt",
                   (select m.content from marketplace.messages m where m.conversation_id=c.id and m.deleted_at is null order by m.created_at desc limit 1) as "lastMessage",
                   (select count(*) from marketplace.messages m
                    where m.conversation_id=c.id and m.deleted_at is null and m.sender_id<>?
                      and m.created_at>coalesce(mine.last_read_at, timestamp with time zone 'epoch')) as "unreadCount"
            from marketplace.conversations c
            join marketplace.conversation_members mine on mine.conversation_id=c.id and mine.user_id=?
            join marketplace.conversation_members other_member on other_member.conversation_id=c.id and other_member.user_id<>?
            join ebike_auth.users other_user on other_user.id=other_member.user_id
            left join marketplace.vehicle_listings l on l.id=c.listing_id
            left join lateral (select public_url from marketplace.vehicle_images where listing_id=l.id and moderation_status='APPROVED' order by primary_image desc, sort_order limit 1) vi on true
            order by coalesce(c.last_message_at,c.created_at) desc
            """, user.getId(), user.getId(), user.getId());
    }

    @Transactional
    public Map<String, Object> send(String username, UUID conversationPublicId, String content) {
        User sender = requireUser(username);
        Long conversationId = requireMember(sender, conversationPublicId);
        String clean = content == null ? "" : content.trim();
        if (clean.isEmpty() || clean.length() > 2000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tin nhắn phải từ 1 đến 2000 ký tự");
        UUID publicId = UUID.randomUUID();
        Long id = jdbc.queryForObject("""
            insert into marketplace.messages(public_id,conversation_id,sender_id,message_type,content)
            values (?,?,?,'TEXT',?) returning id
            """, Long.class, publicId, conversationId, sender.getId(), clean);
        jdbc.update("update marketplace.conversations set last_message_at=now() where id=?", conversationId);
        return one("""
            select m.public_id as "publicId", m.sender_id as "senderId", coalesce(u.first_name || ' ' || u.last_name, u.username) as "senderName",
                   m.message_type as "messageType", m.content, m.created_at as "createdAt"
            from marketplace.messages m join ebike_auth.users u on u.id=m.sender_id where m.id=?
            """, id);
    }

    public void setTyping(String username, UUID conversationPublicId, boolean typing) {
        User user = requireUser(username);
        Long conversationId = requireMember(user, conversationPublicId);
        String key = conversationId + ":" + user.getId();
        if (typing) typingUsers.put(key, System.currentTimeMillis() + 4000);
        else typingUsers.remove(key);
    }

    public Map<String, Boolean> typingStatus(String username, UUID conversationPublicId) {
        User user = requireUser(username);
        Long conversationId = requireMember(user, conversationPublicId);
        long now = System.currentTimeMillis();
        typingUsers.entrySet().removeIf(entry -> entry.getValue() < now);
        String ownKey = conversationId + ":" + user.getId();
        boolean typing = typingUsers.entrySet().stream().anyMatch(entry -> entry.getKey().startsWith(conversationId + ":") && !entry.getKey().equals(ownKey));
        return Map.of("typing", typing);
    }

    @Transactional
    public Map<String, Object> sendAttachment(String username, UUID conversationPublicId, MultipartFile file) {
        User sender = requireUser(username);
        Long conversationId = requireMember(sender, conversationPublicId);
        if (file == null || file.isEmpty()) throw badRequest("Vui lòng chọn tệp");
        if (file.getSize() > 20L * 1024 * 1024) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Tệp không được quá 20 MB");
        String mime = file.getContentType() == null ? "application/octet-stream" : file.getContentType().toLowerCase(Locale.ROOT);
        String type = mime.startsWith("image/") ? "IMAGE" : mime.startsWith("video/") ? "VIDEO" : "DOCUMENT";
        String extension = file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
            ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf('.')).replaceAll("[^a-zA-Z0-9.]", "") : "";
        try {
            String name = "chat-" + UUID.randomUUID() + extension;
            Files.createDirectories(storageRoot);
            Files.write(storageRoot.resolve(name), file.getBytes(), StandardOpenOption.CREATE_NEW);
            String url = "/api/v1/media/marketplace/" + name;
            String metadata = objectMapper.writeValueAsString(Map.of("fileName", file.getOriginalFilename() == null ? "Tệp đính kèm" : file.getOriginalFilename(), "mimeType", mime, "size", file.getSize()));
            Long id = jdbc.queryForObject("insert into marketplace.messages(public_id,conversation_id,sender_id,message_type,content,attachment_key,metadata) values (?,?,?, ?,NULL,?,?::jsonb) returning id",
                Long.class, UUID.randomUUID(), conversationId, sender.getId(), type, url, metadata);
            jdbc.update("update marketplace.conversations set last_message_at=now() where id=?", conversationId);
            return message(id);
        } catch (ResponseStatusException ex) { throw ex; }
        catch (Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể gửi tệp", ex); }
    }

    @Transactional
    public Map<String, Object> sendLocation(String username, UUID conversationPublicId, double latitude, double longitude) {
        User sender = requireUser(username);
        Long conversationId = requireMember(sender, conversationPublicId);
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) throw badRequest("Vị trí không hợp lệ");
        try {
            String metadata = objectMapper.writeValueAsString(Map.of("latitude", latitude, "longitude", longitude));
            Long id = jdbc.queryForObject("insert into marketplace.messages(public_id,conversation_id,sender_id,message_type,content,metadata) values (?,?,?,'LOCATION','Đã gửi một vị trí',?::jsonb) returning id",
                Long.class, UUID.randomUUID(), conversationId, sender.getId(), metadata);
            jdbc.update("update marketplace.conversations set last_message_at=now() where id=?", conversationId);
            return message(id);
        } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Không thể gửi vị trí", ex); }
    }

    private Map<String, Object> message(Long id) {
        return one("""
            select m.public_id as "publicId", m.sender_id as "senderId", coalesce(u.first_name || ' ' || u.last_name, u.username) as "senderName",
                   m.message_type as "messageType", m.content, m.attachment_key as "attachmentUrl", m.metadata->>'fileName' as "fileName",
                   m.metadata->>'mimeType' as "mimeType", m.metadata->>'latitude' as "latitude", m.metadata->>'longitude' as "longitude",
                   exists(select 1 from marketplace.conversation_members cm where cm.conversation_id=m.conversation_id and cm.user_id<>m.sender_id and cm.last_read_at>=m.created_at) as "read",
                   m.created_at as "createdAt"
            from marketplace.messages m join ebike_auth.users u on u.id=m.sender_id where m.id=?
            """, id);
    }

    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    private Long requireMember(String username, UUID publicId) {
        return requireMember(requireUser(username), publicId);
    }

    private Long requireMember(User user, UUID publicId) {
        List<Long> ids = jdbc.queryForList("""
            select c.id from marketplace.conversations c join marketplace.conversation_members m on m.conversation_id=c.id
            where c.public_id=? and m.user_id=?
            """, Long.class, publicId, user.getId());
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Bạn không thuộc hội thoại này");
        return ids.get(0);
    }

    private User requireUser(String username) { return users.findByUsername(username).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)); }
    private Map<String, Object> one(String sql, Object... args) { List<Map<String, Object>> rows=jdbc.queryForList(sql,args); if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND); return rows.get(0); }
}

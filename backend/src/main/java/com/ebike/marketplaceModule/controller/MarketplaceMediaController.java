package com.ebike.marketplaceModule.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/media/marketplace")
public class MarketplaceMediaController {
    private final Path storageRoot;
    public MarketplaceMediaController(@Value("${app.marketplace.storage.root}") String storageRoot) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
    }

    @GetMapping("/{filename:[a-zA-Z0-9._-]+}")
    public ResponseEntity<Resource> image(@PathVariable String filename) {
        try {
            Path file = storageRoot.resolve(filename).normalize();
            if (!file.startsWith(storageRoot) || !Files.isRegularFile(file))
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found");
            String detected = Files.probeContentType(file);
            MediaType mediaType = detected == null ? MediaType.APPLICATION_OCTET_STREAM : MediaType.parseMediaType(detected);
            return ResponseEntity.ok().cacheControl(CacheControl.maxAge(java.time.Duration.ofDays(7)))
                .contentType(mediaType).body(new UrlResource(file.toUri()));
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Image not found", ex);
        }
    }
}

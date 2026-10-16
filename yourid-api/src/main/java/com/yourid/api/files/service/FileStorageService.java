package com.yourid.api.files.service;

import com.yourid.api.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage local des fichiers uploades (images de couverture, logos, bannieres).
 * Les fichiers sont ecrits sous app.uploads.dir et servis statiquement via
 * WebConfig sur /uploads/** -- a remplacer par un stockage objet (S3-compatible)
 * si le volume grandit, sans changer l interface publique de ce service.
 */

@Service
@Slf4j
public class FileStorageService {

    private static final Set<String> ALLOWED_TYPES = Set.of("image/jpeg", "image/png", "image/webp");
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024; // 5 Mo

    @Value("${app.uploads.dir}")
    private String uploadsDir;

    public String store(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("Le fichier est vide.");
        }
        if (!ALLOWED_TYPES.contains(file.getContentType())) {
            throw new BadRequestException("Type de fichier non autorise. Formats acceptes : JPEG, PNG, WEBP.");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Le fichier depasse la taille maximale autorisee (5 Mo).");
        }

        try {
            Path directory = Path.of(uploadsDir);
            Files.createDirectories(directory);

            String extension = extractExtension(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            Path destination = directory.resolve(filename);

            file.transferTo(destination);

            return "/uploads/" + filename;
        } catch (IOException e) {
            log.error("Echec de l ecriture du fichier uploade : {}", e.getMessage());
            throw new BadRequestException("Impossible d enregistrer le fichier.");
        }
    }

    private String extractExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "";
        }
        return originalFilename.substring(originalFilename.lastIndexOf("."));
    }
}

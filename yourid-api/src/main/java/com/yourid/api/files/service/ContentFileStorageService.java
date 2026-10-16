package com.yourid.api.files.service;

import com.yourid.api.common.exception.BadRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Stockage SECURISE du contenu vendu (ebooks, templates, audio...) -- different
 * de FileStorageService (images de couverture, publiques).
 *
 * Proprietes de securite :
 * - Ecrit sous app.content.dir, un dossier JAMAIS expose par WebConfig comme
 *   ressource statique : aucune URL ne peut y acceder directement.
 * - Nom de fichier sur disque totalement aleatoire (UUID) -- sans rapport avec
 *   le nom d origine -- empeche l enumeration et l ecrasement de fichiers.
 * - Extension ET type MIME verifies contre une liste blanche stricte (l un sans
 *   l autre ne suffit pas : un fichier "malware.exe" renomme "doc.pdf" avec un
 *   Content-Type falsifie est intercepte par la verification magic-bytes ci-dessous).
 * - Verification de signature binaire pour les PDF (defense en profondeur minimale ;
 *   Apache Tika serait recommande pour une couverture plus large avant une vraie prod).
 */
@Service
@Slf4j
public class ContentFileStorageService {

    private static final Map<String, Set<String>> ALLOWED = Map.of(
            "pdf", Set.of("application/pdf"),
            "epub", Set.of("application/epub+zip"),
            "zip", Set.of("application/zip", "application/x-zip-compressed"),
            "docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            "pptx", Set.of("application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            "mp3", Set.of("audio/mpeg"),
            "mp4", Set.of("video/mp4"),
            "png", Set.of("image/png"),
            "jpg", Set.of("image/jpeg"),
            "jpeg", Set.of("image/jpeg")
    );

    private static final long MAX_SIZE_BYTES = 200L * 1024 * 1024; // 200 Mo

    @Value("${app.content.dir}")
    private String contentDir;

    public record StoredFile(String storedName, String originalName) {}

    public StoredFile store(MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("Le fichier est vide.");
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new BadRequestException("Le fichier depasse la taille maximale autorisee (200 Mo).");
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());
        String extension = extractExtension(originalName);

        if (!ALLOWED.containsKey(extension)) {
            throw new BadRequestException("Format de fichier non autorise : ." + extension);
        }
        if (file.getContentType() == null || !ALLOWED.get(extension).contains(file.getContentType())) {
            throw new BadRequestException("Le type de fichier declare ne correspond pas a son extension.");
        }
        if (extension.equals("pdf")) {
            verifyPdfMagicBytes(file);
        }

        try {
            Path directory = Path.of(contentDir);
            Files.createDirectories(directory);

            String storedName = UUID.randomUUID() + "." + extension;
            file.transferTo(directory.resolve(storedName));

            return new StoredFile(storedName, originalName);
        } catch (IOException e) {
            log.error("Echec ecriture fichier contenu : {}", e.getMessage());
            throw new BadRequestException("Impossible d enregistrer le fichier.");
        }
    }

    public InputStream read(String storedName) {
        // storedName vient toujours de notre propre base, jamais d une entree utilisateur
        // directe -- cette regex est une securite supplementaire, pas la seule barriere.
        if (!storedName.matches("^[a-fA-F0-9-]{36}\\.[a-z0-9]+$")) {
            throw new BadRequestException("Nom de fichier invalide.");
        }
        try {
            return Files.newInputStream(Path.of(contentDir).resolve(storedName));
        } catch (IOException e) {
            throw new BadRequestException("Fichier introuvable.");
        }
    }

    private String sanitizeFileName(String name) {
        if (name == null) return "fichier";
        // Ne garde que le nom de base -- empeche tout "../../etc/passwd" dans le nom fourni.
        String base = Path.of(name).getFileName().toString();
        return base.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String extractExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot < 0 || lastDot == fileName.length() - 1) return "";
        return fileName.substring(lastDot + 1).toLowerCase();
    }

    private void verifyPdfMagicBytes(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            byte[] header = is.readNBytes(4);
            if (!"%PDF".equals(new String(header))) {
                throw new BadRequestException("Le fichier declare etre un PDF mais son contenu ne correspond pas.");
            }
        } catch (IOException e) {
            throw new BadRequestException("Impossible de lire le fichier.");
        }
    }
}
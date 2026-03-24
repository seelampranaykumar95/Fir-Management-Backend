package com.fir.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.fir.model.EvidenceFile;
import com.fir.model.Fir;
import com.fir.model.User;
import com.fir.model.UserRole;
import com.fir.repository.EvidenceFileRepository;
import com.fir.repository.FirAssignmentRepository;
import com.fir.repository.FirRepository;

@Service
public class EvidenceService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/pdf",
            "image/jpeg",
            "image/jpg",
            "image/png");

    private final EvidenceFileRepository evidenceFileRepository;
    private final FirRepository firRepository;
    private final FirAssignmentRepository firAssignmentRepository;
    private final Path uploadPath;

    public EvidenceService(
            EvidenceFileRepository evidenceFileRepository,
            FirRepository firRepository,
            FirAssignmentRepository firAssignmentRepository,
            @Value("${app.upload.dir:uploads}") String uploadDir) {
        this.evidenceFileRepository = evidenceFileRepository;
        this.firRepository = firRepository;
        this.firAssignmentRepository = firAssignmentRepository;
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not initialize upload directory", ex);
        }
    }

    public EvidenceFile uploadEvidence(Long firId, User actor, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Uploaded file is empty");
        }
        validateFileType(file);

        Fir fir = firRepository.findById(firId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + firId));
        validateEvidenceAccess(fir, actor);

        String originalFileName = Optional.ofNullable(file.getOriginalFilename()).orElse("evidence-file");
        String normalizedFileName = Paths.get(originalFileName).getFileName().toString().replace(" ", "_");
        String storedFileName = UUID.randomUUID() + "_" + normalizedFileName;
        Path targetPath = uploadPath.resolve(storedFileName);

        try {
            file.transferTo(targetPath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to store file", ex);
        }

        EvidenceFile evidenceFile = new EvidenceFile();
        evidenceFile.setFir(fir);
        evidenceFile.setUploadedByUser(actor);
        evidenceFile.setFileName(normalizedFileName);
        evidenceFile.setFileType(Optional.ofNullable(file.getContentType()).orElse("application/octet-stream"));
        evidenceFile.setFileSize(file.getSize());
        evidenceFile.setStoragePath(targetPath.toString());
        return evidenceFileRepository.save(evidenceFile);
    }

    public List<EvidenceFile> getEvidenceByFirId(Long firId, User actor) {
        Fir fir = firRepository.findById(firId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "FIR not found with id: " + firId));
        validateEvidenceAccess(fir, actor);
        return evidenceFileRepository.findByFirIdOrderByCreatedAtDesc(firId);
    }

    public EvidenceFile getEvidenceById(Long evidenceId, User actor) {
        EvidenceFile evidenceFile = evidenceFileRepository.findById(evidenceId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Evidence file not found with id: " + evidenceId));
        validateEvidenceAccess(evidenceFile.getFir(), actor);
        return evidenceFile;
    }

    public byte[] downloadEvidence(Long evidenceId, User actor) {
        EvidenceFile evidenceFile = getEvidenceById(evidenceId, actor);
        Path evidencePath = Paths.get(evidenceFile.getStoragePath()).toAbsolutePath().normalize();
        if (!evidencePath.startsWith(uploadPath)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Evidence file path is not allowed");
        }
        if (!Files.exists(evidencePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stored evidence file is missing");
        }
        try {
            return Files.readAllBytes(evidencePath);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to read stored file", ex);
        }
    }

    private void validateEvidenceAccess(Fir fir, User actor) {
        if (actor.getRole() == UserRole.ADMIN) {
            return;
        }
        if (actor.getRole() == UserRole.OFFICER) {
            firAssignmentRepository.findByFirIdAndAssignedToOfficerIdAndActiveTrue(fir.getId(), actor.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.FORBIDDEN, "FIR is not actively assigned to this officer"));
            return;
        }
        if (actor.getRole() == UserRole.CITIZEN) {
            if (fir.getFiledBy() != null && actor.getId().equals(fir.getFiledBy().getId())) {
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only access evidence for your own FIR");
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not allowed to access evidence for this FIR");
    }

    private void validateFileType(MultipartFile file) {
        String contentType = Optional.ofNullable(file.getContentType())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
        String fileName = Optional.ofNullable(file.getOriginalFilename())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
        boolean supportedType = ALLOWED_CONTENT_TYPES.contains(contentType)
                || fileName.endsWith(".pdf")
                || fileName.endsWith(".png")
                || fileName.endsWith(".jpg")
                || fileName.endsWith(".jpeg");
        if (!supportedType) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, JPG, JPEG, and PNG files are allowed");
        }
    }
}


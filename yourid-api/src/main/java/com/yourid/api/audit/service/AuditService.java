package com.yourid.api.audit.service;

import com.yourid.api.audit.entity.AuditLog;
import com.yourid.api.audit.repository.AuditLogRepository;
import com.yourid.api.users.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Trace les actions sensibles (connexion, changement de mot de passe, retrait...).
 * PAS ENCORE BRANCHE dans les autres services -- a appeler explicitement depuis
 * les points du code qui meritent une trace d audit (exercice de comprehension).
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void log(User user, String action, String entity, UUID entityId, String ipAddress, String userAgent) {
        AuditLog auditLog = AuditLog.builder()
                .user(user)
                .action(action)
                .entity(entity)
                .entityId(entityId)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .build();
        auditLogRepository.save(auditLog);
    }
}

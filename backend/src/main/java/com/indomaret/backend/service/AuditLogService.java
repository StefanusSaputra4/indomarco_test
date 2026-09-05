package com.indomaret.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.indomaret.backend.entity.AuditLog;
import com.indomaret.backend.entity.User;
import com.indomaret.backend.repository.AuditLogRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void logChange(User user, String entityName, Long entityId, String action, Object oldValue, Object newValue) {
        try {
            String oldJson = oldValue != null ? objectMapper.writeValueAsString(oldValue) : null;
            String newJson = newValue != null ? objectMapper.writeValueAsString(newValue) : null;

            AuditLog logEntry = new AuditLog();
            logEntry.setUser(user);
            logEntry.setEntityName(entityName);
            logEntry.setEntityId(entityId);
            logEntry.setAction(action);
            logEntry.setOldValue(oldJson);
            logEntry.setNewValue(newJson);

            auditLogRepository.save(logEntry);
            log.info("Audit Log saved: [{}] on {} ID: {}", action, entityName, entityId);
        } catch (JsonProcessingException e) {
            log.error("Gagal men-serialize nilai audit log ke JSON", e);
        }
    }
}

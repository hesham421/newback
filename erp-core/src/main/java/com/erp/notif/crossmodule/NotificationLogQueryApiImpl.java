package com.erp.notif.crossmodule;

import com.erp.notif.entity.NotificationLog;
import com.erp.notif.repository.NotificationLogRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationLogQueryApiImpl implements NotificationLogQueryApi {

    private final NotificationLogRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<DispatchLogRecord> findByRecipientModuleAndReference(
            Long recipientId, String moduleCode, String referenceType) {

        List<NotificationLog> logs = repository.findAll(
            (root, query, cb) -> cb.and(
                cb.equal(root.get("recipientId"), recipientId),
                cb.equal(root.get("moduleCode"), moduleCode),
                cb.equal(root.get("referenceType"), referenceType)),
            Sort.by(Sort.Direction.DESC, "createdAt"));

        return logs.stream()
            .map(entity -> new DispatchLogRecord(
                entity.getId(),
                entity.getTemplateFk() == null ? null : entity.getTemplateFk().getTemplateCode(),
                entity.getNotificationStatusId()))
            .toList();
    }
}

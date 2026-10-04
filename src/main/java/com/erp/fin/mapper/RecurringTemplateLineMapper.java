package com.erp.fin.mapper;

import com.erp.fin.dto.RecurringTemplateLineCreateRequest;
import com.erp.fin.dto.RecurringTemplateLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.RecurringTemplate;
import com.erp.fin.entity.RecurringTemplateLine;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-012 (RecurringTemplateLine) — build-create-mapper, no MapStruct.
 * Child shape: every FK the line needs ({@code parent}, {@code account}, optional
 * {@code dimensionValue}) arrives already resolved as a compile-time-safe parameter (A.4.2,
 * SH.3).
 */
@Component
public class RecurringTemplateLineMapper {

    public RecurringTemplateLine toEntity(RecurringTemplateLineCreateRequest request,
                                          RecurringTemplate parent,
                                          Integer lineNo,
                                          Account account,
                                          DimensionValue dimensionValue) {
        if (request == null) {
            return null;
        }
        return RecurringTemplateLine.builder()
            .recurringTemplate(parent)
            .lineNo(lineNo)
            .account(account)
            .amount(request.getAmount())
            .directionCode(request.getDirectionCode())
            .dimensionValue(dimensionValue)
            .build();
    }

    public RecurringTemplateLineResponse toResponse(RecurringTemplateLine entity) {
        if (entity == null) {
            return null;
        }
        return RecurringTemplateLineResponse.builder()
            .recurringTemplateLinePk(entity.getRecurringTemplateLinePk())
            .recurringTemplateId(entity.getRecurringTemplate() == null
                ? null : entity.getRecurringTemplate().getRecurringTemplatePk())
            .lineNo(entity.getLineNo())
            .accountId(entity.getAccount() == null ? null : entity.getAccount().getAccountPk())
            .amount(entity.getAmount())
            .directionCode(entity.getDirectionCode())
            .dimensionValueId(entity.getDimensionValue() == null
                ? null : entity.getDimensionValue().getDimensionValuePk())
            .createdAt(entity.getCreatedAt())
            .build();
    }
}

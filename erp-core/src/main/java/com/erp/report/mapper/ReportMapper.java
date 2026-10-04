package com.erp.report.mapper;

import com.erp.report.ReportAuthorities;
import com.erp.report.ReportColumn;
import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import com.erp.report.dto.ReportColumnResponse;
import com.erp.report.dto.ReportDefinitionResponse;
import com.erp.report.dto.ReportParamResponse;
import com.erp.report.dto.ReportRunResponse;
import java.util.List;
import org.springframework.stereotype.Component;

/** Maps the reporting SPI types to the API DTOs (erp-core step 11). Manual and null-safe. */
@Component
public class ReportMapper {

    public ReportDefinitionResponse toDefinitionResponse(ReportProvider provider) {
        if (provider == null) {
            return null;
        }
        List<ReportParam> params = provider.params() == null ? List.of() : provider.params();
        return ReportDefinitionResponse.builder()
            .code(provider.code())
            .moduleCode(provider.moduleCode())
            .titleAr(provider.titleAr())
            .titleEn(provider.titleEn())
            .authority(ReportAuthorities.of(provider))
            .params(params.stream().map(this::toParamResponse).toList())
            .build();
    }

    public ReportParamResponse toParamResponse(ReportParam param) {
        if (param == null) {
            return null;
        }
        return ReportParamResponse.builder()
            .name(param.name())
            .type(param.type() == null ? null : param.type().name())
            .required(param.required())
            .lookupKey(param.lookupKey())
            .labelAr(param.labelAr())
            .labelEn(param.labelEn())
            .build();
    }

    public ReportColumnResponse toColumnResponse(ReportColumn column) {
        if (column == null) {
            return null;
        }
        return ReportColumnResponse.builder()
            .key(column.key())
            .type(column.type() == null ? null : column.type().name())
            .labelAr(column.labelAr())
            .labelEn(column.labelEn())
            .build();
    }

    /** A run or export result; {@code page}/{@code size} are {@code null} for an export. */
    public ReportRunResponse toRunResponse(String code, ReportResult result, Integer page, Integer size) {
        if (result == null) {
            return null;
        }
        return ReportRunResponse.builder()
            .code(code)
            .columns(result.columns().stream().map(this::toColumnResponse).toList())
            .rows(result.rows())
            .totals(result.totals())
            .page(page)
            .size(size)
            .totalRows(result.totalRows())
            .build();
    }
}

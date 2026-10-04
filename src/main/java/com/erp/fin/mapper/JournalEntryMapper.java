package com.erp.fin.mapper;

import com.erp.fin.dto.JournalEntryCreateRequest;
import com.erp.fin.dto.JournalEntryResponse;
import com.erp.fin.dto.JournalLineCreateRequest;
import com.erp.fin.dto.JournalLineDimensionCreateRequest;
import com.erp.fin.dto.JournalLineDimensionResponse;
import com.erp.fin.dto.JournalLineResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import com.erp.fin.entity.JournalEntry;
import com.erp.fin.entity.JournalLine;
import com.erp.fin.entity.JournalLineDimension;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for the ENT-FIN-004 / ENT-FIN-005 / ENT-FIN-006 posting aggregate —
 * build-create-mapper, no MapStruct. One mapper for the whole aggregate rather than three: the
 * lines and their dimension tags are never addressable on their own (the plan's ENTITY REGISTRY
 * gives both "create (with header/line), read"), so they have no independent create request,
 * service or endpoint to map for.
 *
 * <p>Every FK ({@code fiscalYear}, {@code period}, each line's {@code account}, each tag's
 * {@code dimension}/{@code dimensionValue}) arrives already resolved from the service (A.4.2,
 * SH.3) — the mapper issues no query and makes no decision.
 *
 * <p>{@code docNo} is a parameter, not a request field: it is produced by
 * {@code JournalDocNoGenerator} in the service and assigned once (CORE.md "Numbering"). The
 * entity is built in the {@code DRAFT} state its own {@code @Builder.Default} declares; the
 * service calls {@code JournalEntry.post(...)} only after every guard has returned.
 */
@Component
public class JournalEntryMapper {

    /**
     * Builds the whole in-memory DRAFT aggregate. {@code resolvedLines} carries, per submitted
     * line and in submission order, the account and the resolved dimension/value pairs the
     * service looked up and validated.
     */
    public JournalEntry toEntity(JournalEntryCreateRequest request,
                                 String docNo,
                                 FiscalYear fiscalYear,
                                 FiscalPeriod period,
                                 List<ResolvedLine> resolvedLines) {
        if (request == null) {
            return null;
        }
        JournalEntry entry = JournalEntry.builder()
            .docNo(docNo)
            .docDate(request.getDocDate())
            .fiscalYear(fiscalYear)
            .period(period)
            .journalTypeCode(request.getJournalTypeCode())
            .statusCode(JournalEntry.STATUS_DRAFT)
            .descriptionAr(request.getDescriptionAr())
            .descriptionEn(request.getDescriptionEn())
            .lines(new ArrayList<>())
            .build();

        List<ResolvedLine> safeLines = resolvedLines == null ? List.of() : resolvedLines;
        int lineNo = 1;
        for (ResolvedLine resolved : safeLines) {
            entry.getLines().add(toLineEntity(resolved, entry, lineNo));
            lineNo++;
        }
        return entry;
    }

    private JournalLine toLineEntity(ResolvedLine resolved, JournalEntry entry, int lineNo) {
        JournalLineCreateRequest request = resolved.request();
        JournalLine line = JournalLine.builder()
            .journalEntry(entry)
            .lineNo(lineNo)
            .account(resolved.account())
            .amount(request.getAmount())
            .directionCode(request.getDirectionCode())
            .isRemainderFl(Boolean.FALSE)
            .descriptionAr(request.getDescriptionAr())
            .descriptionEn(request.getDescriptionEn())
            .dimensions(new ArrayList<>())
            .build();

        for (ResolvedDimension resolvedDimension : resolved.dimensions()) {
            line.getDimensions().add(JournalLineDimension.builder()
                .journalLine(line)
                .dimension(resolvedDimension.dimension())
                .dimensionValue(resolvedDimension.dimensionValue())
                .build());
        }
        return line;
    }

    /**
     * The system-built variant of {@link #toEntity} — the one used by every SVC-API-INT path that
     * assembles an entry from something other than a submitted {@code JournalEntryCreateRequest}:
     * the event build (API-FIN-020), the recurring/reversing template run (API-FIN-014), the
     * allocation run (API-FIN-017), the reversal (API-FIN-021) and the year-end closing and
     * opening entries (API-FIN-027).
     *
     * <p>Same contract as the request-driven overload: every FK arrives already resolved and
     * validated from the service (A.4.2, SH.3), the mapper issues no query and makes no decision,
     * {@code docNo} is a parameter produced by {@code JournalDocNoGenerator}, and the aggregate is
     * built in the {@code DRAFT} state — the service calls {@code JournalEntry.post(...)} only
     * after every guard has returned.
     *
     * <p>{@code lineNo} is assigned from the built list's order, exactly as the request-driven
     * overload assigns it from submission order. {@code isRemainderFl} is carried per line, since
     * a rule-driven or allocation-driven build has one (RULE-FIN-010) where a manual entry never
     * does.
     */
    public JournalEntry toEntity(BuiltEntry built) {
        if (built == null) {
            return null;
        }
        JournalEntry entry = JournalEntry.builder()
            .docNo(built.docNo())
            .docDate(built.docDate())
            .fiscalYear(built.fiscalYear())
            .period(built.period())
            .journalTypeCode(built.journalTypeCode())
            .statusCode(JournalEntry.STATUS_DRAFT)
            .eventReference(built.eventReference())
            .descriptionAr(built.descriptionAr())
            .descriptionEn(built.descriptionEn())
            .lines(new ArrayList<>())
            .build();

        List<BuiltLine> safeLines = built.lines() == null ? List.of() : built.lines();
        int lineNo = 1;
        for (BuiltLine builtLine : safeLines) {
            entry.getLines().add(toBuiltLineEntity(builtLine, entry, lineNo));
            lineNo++;
        }
        return entry;
    }

    private JournalLine toBuiltLineEntity(BuiltLine built, JournalEntry entry, int lineNo) {
        JournalLine line = JournalLine.builder()
            .journalEntry(entry)
            .lineNo(lineNo)
            .account(built.account())
            .amount(built.amount())
            .directionCode(built.directionCode())
            .isRemainderFl(built.remainder())
            .descriptionAr(built.descriptionAr())
            .descriptionEn(built.descriptionEn())
            .dimensions(new ArrayList<>())
            .build();

        List<BuiltDimension> dimensions =
            built.dimensions() == null ? List.of() : built.dimensions();
        for (BuiltDimension dimension : dimensions) {
            line.getDimensions().add(JournalLineDimension.builder()
                .journalLine(line)
                .dimension(dimension.dimension())
                .dimensionValue(dimension.dimensionValue())
                .build());
        }
        return line;
    }

    /**
     * {@code lines} is passed in rather than read off the entity: the response is crafted straight
     * after the single {@code save}, where the aggregate's own in-memory list is authoritative and
     * the {@code @Formula} {@code lineCount} has not been recomputed by a re-read.
     */
    public JournalEntryResponse toResponse(JournalEntry entity, List<JournalLineResponse> lines) {
        if (entity == null) {
            return null;
        }
        List<JournalLineResponse> safeLines = lines == null ? List.of() : lines;
        return JournalEntryResponse.builder()
            .journalEntryPk(entity.getJournalEntryPk())
            .docNo(entity.getDocNo())
            .docDate(entity.getDocDate())
            .fiscalYearId(entity.getFiscalYear() == null
                ? null : entity.getFiscalYear().getFiscalYearPk())
            .periodId(entity.getPeriod() == null ? null : entity.getPeriod().getFiscalPeriodPk())
            .journalTypeCode(entity.getJournalTypeCode())
            .statusCode(entity.getStatusCode())
            .eventReference(entity.getEventReference())
            .originalEntryId(entity.getOriginalEntry() == null
                ? null : entity.getOriginalEntry().getJournalEntryPk())
            .reversalEntryId(entity.getReversalEntry() == null
                ? null : entity.getReversalEntry().getJournalEntryPk())
            .descriptionAr(entity.getDescriptionAr())
            .descriptionEn(entity.getDescriptionEn())
            .postedAt(entity.getPostedAt())
            .lineCount(safeLines.size())
            .lines(safeLines)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    /**
     * API-FIN-018 — the SEARCH projection of an entry: the same {@link JournalEntryResponse}
     * contract, header fields only. {@code lineCount} is read from ENT-FIN-004's {@code @Formula}
     * count rather than from a loaded collection (A.1.19), and {@code lines} is empty: a page of
     * entries must not drag every line set with it, and REQ-FIN-027 asks for matching entries
     * "with their full status detail", which is header-level. API-FIN-022 is the endpoint that
     * returns the nested lines and their dimensions, through
     * {@link #toResponse(JournalEntry, List)}.
     */
    public JournalEntryResponse toSummaryResponse(JournalEntry entity) {
        if (entity == null) {
            return null;
        }
        JournalEntryResponse response = toResponse(entity, List.of());
        response.setLineCount(entity.getLineCount() != null ? entity.getLineCount() : 0);
        return response;
    }

    public JournalLineResponse toLineResponse(JournalLine entity) {
        if (entity == null) {
            return null;
        }
        List<JournalLineDimensionResponse> dimensions = new ArrayList<>();
        if (entity.getDimensions() != null) {
            for (JournalLineDimension dimension : entity.getDimensions()) {
                dimensions.add(toLineDimensionResponse(dimension));
            }
        }
        return JournalLineResponse.builder()
            .journalLinePk(entity.getJournalLinePk())
            .journalEntryId(entity.getJournalEntry() == null
                ? null : entity.getJournalEntry().getJournalEntryPk())
            .lineNo(entity.getLineNo())
            .accountId(entity.getAccount() == null ? null : entity.getAccount().getAccountPk())
            .amount(entity.getAmount())
            .directionCode(entity.getDirectionCode())
            .isRemainderFl(Boolean.TRUE.equals(entity.getIsRemainderFl()))
            .descriptionAr(entity.getDescriptionAr())
            .descriptionEn(entity.getDescriptionEn())
            .dimensions(dimensions)
            .createdAt(entity.getCreatedAt())
            .build();
    }

    public JournalLineDimensionResponse toLineDimensionResponse(JournalLineDimension entity) {
        if (entity == null) {
            return null;
        }
        return JournalLineDimensionResponse.builder()
            .journalLineDimensionPk(entity.getJournalLineDimensionPk())
            .journalLineId(entity.getJournalLine() == null
                ? null : entity.getJournalLine().getJournalLinePk())
            .dimensionId(entity.getDimension() == null
                ? null : entity.getDimension().getDimensionPk())
            .dimensionValueId(entity.getDimensionValue() == null
                ? null : entity.getDimensionValue().getDimensionValuePk())
            .build();
    }

    /**
     * One submitted line together with the entities the service resolved and validated for it —
     * the mapper's input contract, so the mapper itself never queries (SH.3).
     */
    public record ResolvedLine(JournalLineCreateRequest request,
                               Account account,
                               List<ResolvedDimension> dimensions) {
    }

    /** One submitted dimension tag with both sides resolved by the service. */
    public record ResolvedDimension(JournalLineDimensionCreateRequest request,
                                    Dimension dimension,
                                    DimensionValue dimensionValue) {
    }

    /**
     * A complete entry as a SVC-API-INT path assembled it — the input contract of
     * {@link #toEntity(BuiltEntry)}, so that mapper too never queries and never decides (SH.3).
     * {@code eventReference} is {@code null} for every source but the event build.
     */
    public record BuiltEntry(String docNo,
                             LocalDate docDate,
                             FiscalYear fiscalYear,
                             FiscalPeriod period,
                             String journalTypeCode,
                             String eventReference,
                             String descriptionAr,
                             String descriptionEn,
                             List<BuiltLine> lines) {
    }

    /** One built posting line; {@code amount} is always positive, the side is {@code direction}. */
    public record BuiltLine(Account account,
                            BigDecimal amount,
                            String directionCode,
                            boolean remainder,
                            String descriptionAr,
                            String descriptionEn,
                            List<BuiltDimension> dimensions) {
    }

    /** One built dimension tag, both sides resolved by the service. */
    public record BuiltDimension(Dimension dimension, DimensionValue dimensionValue) {
    }
}

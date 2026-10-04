package com.erp.fin.repository;

import com.erp.fin.entity.RecurringTemplateLine;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-012 (RecurringTemplateLine). Module-internal (A.2.3).
 *
 * <p>Written with QR-FIN-018 (create template with lines, API-FIN-013) through the inherited
 * {@code save}/{@code saveAll}.
 */
@Repository
public interface RecurringTemplateLineRepository
    extends JpaRepository<RecurringTemplateLine, Long>,
            JpaSpecificationExecutor<RecurringTemplateLine> {

    /**
     * QR-FIN-019 — the template's line set, read at run time (API-FIN-014) to build the entry.
     * {@code JOIN FETCH} on the parent avoids N+1 (A.2.6). Ordered by {@code lineNo} so the
     * generated entry's lines keep the template's own order.
     */
    @Query("SELECT l FROM RecurringTemplateLine l JOIN FETCH l.recurringTemplate "
        + "WHERE l.recurringTemplate.recurringTemplatePk = :recurringTemplatePk "
        + "ORDER BY l.lineNo ASC")
    List<RecurringTemplateLine> findByRecurringTemplatePk(
        @Param("recurringTemplatePk") Long recurringTemplatePk);

    /**
     * QR-FIN-017's companion batch read for API-FIN-012: the lines of a whole PAGE of templates in
     * one query, instead of {@link #findByRecurringTemplatePk} once per row (A.2.6).
     * {@code RecurringTemplateResponse} reports a {@code lineCount}, so the search cannot omit
     * them.
     */
    @Query("SELECT l FROM RecurringTemplateLine l JOIN FETCH l.recurringTemplate "
        + "WHERE l.recurringTemplate.recurringTemplatePk IN :recurringTemplatePks "
        + "ORDER BY l.lineNo ASC")
    List<RecurringTemplateLine> findByRecurringTemplatePkIn(
        @Param("recurringTemplatePks") Collection<Long> recurringTemplatePks);
}

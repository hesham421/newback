package com.erp.fin.repository;

import com.erp.fin.entity.JournalEntry;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-004 (JournalEntry). Module-internal (A.2.3).
 *
 * <p>QR coverage served by inherited methods, so no mapping is declared for them:
 * <ul>
 *   <li><b>QR-FIN-023</b> (search journal entries, API-FIN-018) — inherited
 *       {@code findAll(Specification, Pageable)} plus the shared {@code SpecBuilder}/
 *       {@code PageableBuilder} layer at the service.</li>
 *   <li><b>QR-FIN-024 / QR-FIN-025 / QR-FIN-034</b> (build a manual, event or reversal entry) —
 *       inherited {@code save}, which cascades header → lines → line dimensions in the single
 *       transaction CORE.md mandates.</li>
 *   <li><b>QR-FIN-033</b> (post: DRAFT → POSTED, set {@code postedAt}) — inherited {@code save}
 *       on the managed DRAFT instance after {@code JournalEntryDomain}'s guards and
 *       {@code JournalEntry.post(...)}.</li>
 *   <li><b>QR-FIN-036</b> (the current open period a reversal falls back to, RULE-FIN-012) — an
 *       ENT-FIN-008 read, resolved through {@code FiscalPeriodRepository}'s inherited
 *       {@code findAll(Specification, Pageable)} and the shared {@code SpecBuilder} (status
 *       {@code OPEN}); no new method and no change to that earlier sub's file is required.</li>
 * </ul>
 *
 * <p><b>RULE-FIN-016.</b> No UPDATE or DELETE mapping is declared here — not a {@code @Modifying}
 * query, not a derived {@code deleteBy...}. A POSTED entry therefore has no code path that could
 * edit or remove it; correction is only ever a reversal (QR-FIN-034).
 *
 * <p>Every method below fetches or counts; none decides. The decisions live in
 * {@code JournalEntryDomain} (A.2.9's counterpart rule).
 */
@Repository
public interface JournalEntryRepository
    extends JpaRepository<JournalEntry, Long>,
            JpaSpecificationExecutor<JournalEntry> {

    /**
     * QR-FIN-026 — RULE-FIN-004's fact: has this event reference already produced an entry? The
     * service passes the answer into {@code JournalEntryDomain.create(...)}, which owns the
     * decision. v2: this is the friendly pre-check; {@code UQ_FIN_JOURNAL_ENTRY_EVENT_REF} is the
     * guarantee, its refusal translated by {@code FinPersistenceRefusalAdvisor} (ADR-FIN-030).
     * API: API-FIN-020.
     */
    boolean existsByEventReference(String eventReference);

    /**
     * QR-FIN-037 — read one entry with its lines (API-FIN-022, and the same read QR-FIN-034's
     * reversal build starts from). {@code JOIN FETCH} avoids the N+1 a derived query would cause
     * (A.2.6); the lines' own dimension rows are read separately, by
     * {@code JournalLineDimensionRepository.findByJournalEntryPk(...)}, rather than as a second
     * collection fetch in the same query.
     */
    @Query("SELECT e FROM JournalEntry e LEFT JOIN FETCH e.lines l "
        + "WHERE e.journalEntryPk = :journalEntryPk ORDER BY l.lineNo ASC")
    Optional<JournalEntry> findOneWithLines(@Param("journalEntryPk") Long journalEntryPk);

    /**
     * ALIGN-BE — API-FIN-021's reversal lock (QR-FIN-035's fact, taken under contention).
     *
     * <p>RULE-FIN-013's "an entry may be reversed only once" reads
     * {@code REVERSAL_ENTRY_ID} (DBF-FIN-043) and then writes it. No FIN entity carries
     * {@code @Version}, so two concurrent reversals of the same entry both read it as {@code null},
     * both post a mirror, and the second {@code save(original)} silently overwrites the first
     * link — one original, two POSTED mirrors, one orphaned, and a net ledger effect of
     * {@code −(original)} instead of zero. This {@code PESSIMISTIC_WRITE} read (SQL
     * {@code SELECT ... FOR UPDATE}) makes the read-then-write atomic: the second request blocks
     * until the first commits, is then granted the lock on the row's current version, sees
     * {@code REVERSAL_ENTRY_ID} set and is rejected by
     * {@code JournalEntryDomain.assertCanReverse(true)} with {@code FIN-409-ALREADY-REVERSED} —
     * the same answer a sequential double reversal already gets.
     *
     * <p>Header only, deliberately: {@code FOR UPDATE} cannot be applied to the nullable side of
     * the outer join {@link #findOneWithLines(Long)} performs, and the lines are not what the
     * guard reads. The reversal flow takes this lock first, then loads the lines through the
     * existing fetch query, which returns the same managed instance.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM JournalEntry e WHERE e.journalEntryPk = :journalEntryPk")
    Optional<JournalEntry> lockForReversal(@Param("journalEntryPk") Long journalEntryPk);
}

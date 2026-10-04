package com.erp.fin.numbering;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The FIN-local journal document-number generator CORE.md's "Numbering" section mandates.
 *
 * <p><b>Format</b> — {@code JV-{fiscalYearCode}-{NNNNNN}}: the literal prefix {@code JV-}, the
 * owning fiscal year's {@code code} ({@code FIN_FISCAL_YEAR.code}, DBF-FIN-066, {@code
 * VARCHAR(10)}), {@code -}, then a zero-padded 6-digit counter starting at {@code 000001} — e.g.
 * {@code JV-2026-000123}. Worst case 20 characters, inside {@code doc_no VARCHAR(30)}.
 *
 * <p><b>Counter scope</b> — per {@code fiscalYearId}, restarting at {@code 000001} each fiscal
 * year. One counter for every journal type: manual, event-built, recurring, allocation, reversal
 * and the year-end closing/opening entries all draw from it, never segmented by
 * {@code journalTypeCode}. {@code UQ_FIN_JOURNAL_ENTRY_YEAR_DOCNO (fiscal_year_id, doc_no)} backs
 * the guarantee at the database.
 *
 * <p><b>Deliberately FIN-local, and deliberately not a Spring bean.</b> CORE.md records the
 * decision to keep this out of {@code com.erp.common} until a second real consumer appears. It is
 * also a plain, dependency-free class rather than a {@code @Component} holding a repository: the
 * service already owns repository access (A.5 / A.0.3's spirit), so it reads the fiscal year's
 * current highest {@code docNo} itself and passes it in here as a plain argument. This class
 * performs no I/O and holds no state.
 *
 * <p><b>Concurrency.</b> {@link #next(String, String)} is a pure function of the highest committed
 * {@code docNo} the caller observed. Two concurrent creates in the same fiscal year can therefore
 * observe the same predecessor and compute the same number; the second INSERT is then rejected by
 * {@code UQ_FIN_JOURNAL_ENTRY_YEAR_DOCNO} and its whole transaction rolls back, surfacing through
 * the shared {@code GlobalExceptionHandler} as a clean 409 with no partial write. The unique
 * constraint is the authoritative backstop by design (CORE.md): a duplicate or corrupted number
 * can never be persisted, only a losing request retried.
 */
public final class JournalDocNoGenerator {

    /** The literal document-number prefix for every journal entry. */
    public static final String PREFIX = "JV-";

    /** Width of the zero-padded counter segment. */
    public static final int COUNTER_WIDTH = 6;

    private static final String COUNTER_FORMAT = "%0" + COUNTER_WIDTH + "d";

    private JournalDocNoGenerator() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * The next document number for a fiscal year.
     *
     * @param fiscalYearCode         the owning fiscal year's {@code code}
     * @param highestExistingDocNo   the greatest {@code docNo} already committed for that fiscal
     *                               year, or {@code null} when the year has no entry yet
     * @return the next document number, {@code JV-{fiscalYearCode}-{NNNNNN}}
     */
    public static String next(String fiscalYearCode, String highestExistingDocNo) {
        return format(fiscalYearCode, counterOf(fiscalYearCode, highestExistingDocNo) + 1L);
    }

    /** Renders one counter value in the canonical format. */
    public static String format(String fiscalYearCode, long counter) {
        return PREFIX + fiscalYearCode + "-" + String.format(COUNTER_FORMAT, counter);
    }

    /**
     * Reads the counter out of an existing document number. Returns {@code 0} when there is no
     * predecessor, or when the stored value does not match this generator's own format — the only
     * producer of {@code docNo} is this class, so a mismatch cannot arise from normal operation,
     * and falling back to {@code 0} keeps the unique constraint (never a silent overwrite) as the
     * arbiter.
     */
    public static long counterOf(String fiscalYearCode, String docNo) {
        if (docNo == null || fiscalYearCode == null) {
            return 0L;
        }
        Pattern pattern = Pattern.compile(
            "^" + Pattern.quote(PREFIX + fiscalYearCode + "-") + "(\\d{1," + 18 + "})$");
        Matcher matcher = pattern.matcher(docNo);
        if (!matcher.matches()) {
            return 0L;
        }
        return Long.parseLong(matcher.group(1));
    }
}

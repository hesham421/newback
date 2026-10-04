package com.erp.fin.mapper;

import com.erp.fin.domain.JournalEntryDomain;
import com.erp.fin.dto.AccountBalanceGroupResponse;
import com.erp.fin.dto.AccountBalanceRowResponse;
import com.erp.fin.dto.AccountLedgerResponse;
import com.erp.fin.dto.AccountLedgerRowResponse;
import com.erp.fin.dto.DimensionReportRowResponse;
import com.erp.fin.entity.Account;
import com.erp.fin.repository.AccountBalanceView;
import com.erp.fin.repository.AccountLedgerLineView;
import com.erp.fin.repository.DimensionBalanceView;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Projection-to-DTO mapper for FIN's five reports (API-FIN-028 … API-FIN-032). Manual mapping, no
 * repository and no service call — the service hands it the rows QR-FIN-042 / QR-FIN-043 /
 * QR-FIN-044 returned and it shapes them.
 *
 * <p><b>Why the arithmetic is here.</b> POL-FIN-002 ("present each account's balance sign
 * according to its account type's normal balance") is a presentation policy over fields already
 * read, not an "is this operation allowed?" decision — so it is neither a domain-class rule
 * (CORE.md's placement criterion) nor a business-rule conditional A.5.18 would push out of the
 * service. Turning an unsigned debit/credit pair plus a {@code natureCode} into a signed balance
 * is a field transformation, which is exactly what a mapper owns.
 *
 * <p><b>Two different questions, deliberately kept apart.</b> Which COLUMN a balance sits in is
 * decided by the net movement's own side, through the existing
 * {@link JournalEntryDomain#directionOfNet(BigDecimal)} — never by the account's nature. That is
 * what makes API-FIN-029 balance: the nets of all accounts sum to zero because every posted entry
 * balanced (RULE-FIN-006), so placing each net on its own side makes the two column totals equal
 * by construction (POL-FIN-008). Placing them by nature instead would misfile an abnormal balance
 * and break the identity. The account's nature answers the other question — the SIGN of
 * {@code signedBalance} (POL-FIN-002).
 */
@Component
public class ReportMapper {

    /**
     * API-FIN-028 — the ledger, with its running balance accumulated in the order the query
     * returned (oldest first). Every figure comes from the POSTED lines just read; nothing is
     * loaded from a stored balance (POL-FIN-009).
     */
    public AccountLedgerResponse toAccountLedger(Account account,
                                                 LocalDate fromDate,
                                                 LocalDate toDate,
                                                 Long dimensionId,
                                                 Long dimensionValueId,
                                                 List<AccountLedgerLineView> lines) {
        if (account == null) {
            return null;
        }
        List<AccountLedgerLineView> safeLines = lines == null ? List.of() : lines;

        List<AccountLedgerRowResponse> rows = new ArrayList<>(safeLines.size());
        BigDecimal debitTotal = BigDecimal.ZERO;
        BigDecimal creditTotal = BigDecimal.ZERO;
        BigDecimal runningBalance = BigDecimal.ZERO;

        for (AccountLedgerLineView line : safeLines) {
            BigDecimal amount = line.getAmount() == null ? BigDecimal.ZERO : line.getAmount();
            boolean debit = JournalEntryDomain.DIRECTION_DEBIT.equalsIgnoreCase(
                line.getDirectionCode());
            debitTotal = debit ? debitTotal.add(amount) : debitTotal;
            creditTotal = debit ? creditTotal : creditTotal.add(amount);

            BigDecimal signedAmount = signedAgainstNature(
                account.getNatureCode(), debit ? amount : BigDecimal.ZERO,
                debit ? BigDecimal.ZERO : amount);
            runningBalance = runningBalance.add(signedAmount);

            rows.add(AccountLedgerRowResponse.builder()
                .journalEntryId(line.getJournalEntryId())
                .docNo(line.getDocNo())
                .docDate(line.getDocDate())
                .journalTypeCode(line.getJournalTypeCode())
                .eventReference(line.getEventReference())
                .journalLineId(line.getJournalLineId())
                .lineNo(line.getLineNo())
                .amount(amount)
                .directionCode(line.getDirectionCode())
                .signedAmount(signedAmount)
                .runningBalance(runningBalance)
                .descriptionAr(line.getDescriptionAr())
                .descriptionEn(line.getDescriptionEn())
                .build());
        }

        return AccountLedgerResponse.builder()
            .accountId(account.getAccountPk())
            .accountCode(account.getCode())
            .accountNameAr(account.getNameAr())
            .accountNameEn(account.getNameEn())
            .accountTypeCode(account.getAccountTypeCode())
            .natureCode(account.getNatureCode())
            .fromDate(fromDate)
            .toDate(toDate)
            .dimensionId(dimensionId)
            .dimensionValueId(dimensionValueId)
            .debitTotal(debitTotal)
            .creditTotal(creditTotal)
            .closingBalance(runningBalance)
            .rows(rows)
            .build();
    }

    /**
     * QR-FIN-043's row, shaped once and reused by API-FIN-029, API-FIN-030 and API-FIN-031 — the
     * three reports read the same aggregation, so they share this mapping too.
     */
    public AccountBalanceRowResponse toAccountBalanceRow(AccountBalanceView view) {
        if (view == null) {
            return null;
        }
        BigDecimal debitTotal = zeroIfNull(view.getDebitTotal());
        BigDecimal creditTotal = zeroIfNull(view.getCreditTotal());
        BigDecimal net = debitTotal.subtract(creditTotal);
        boolean debitSide = JournalEntryDomain.DIRECTION_DEBIT.equals(
            JournalEntryDomain.directionOfNet(net));

        return AccountBalanceRowResponse.builder()
            .accountId(view.getAccountId())
            .accountCode(view.getAccountCode())
            .accountNameAr(view.getAccountNameAr())
            .accountNameEn(view.getAccountNameEn())
            .accountTypeCode(view.getAccountTypeCode())
            .natureCode(view.getNatureCode())
            .debitTotal(debitTotal)
            .creditTotal(creditTotal)
            .debitBalance(debitSide ? net : BigDecimal.ZERO)
            .creditBalance(debitSide ? BigDecimal.ZERO : net.negate())
            .signedBalance(signedAgainstNature(view.getNatureCode(), debitTotal, creditTotal))
            .build();
    }

    /** API-FIN-030 / API-FIN-031 — one {@code accountTypeCode} group and its signed total. */
    public AccountBalanceGroupResponse toGroup(String accountTypeCode,
                                               List<AccountBalanceRowResponse> rows) {
        List<AccountBalanceRowResponse> safeRows = rows == null ? List.of() : rows;
        return AccountBalanceGroupResponse.builder()
            .accountTypeCode(accountTypeCode)
            .rows(safeRows)
            .groupTotal(sumSignedBalances(safeRows))
            .build();
    }

    /** API-FIN-032 — one account × dimension-value row (QR-FIN-044, POL-FIN-011). */
    public DimensionReportRowResponse toDimensionReportRow(DimensionBalanceView view) {
        if (view == null) {
            return null;
        }
        BigDecimal debitTotal = zeroIfNull(view.getDebitTotal());
        BigDecimal creditTotal = zeroIfNull(view.getCreditTotal());

        return DimensionReportRowResponse.builder()
            .accountId(view.getAccountId())
            .accountCode(view.getAccountCode())
            .accountNameAr(view.getAccountNameAr())
            .accountNameEn(view.getAccountNameEn())
            .natureCode(view.getNatureCode())
            .dimensionId(view.getDimensionId())
            .dimensionValueId(view.getDimensionValueId())
            .dimensionValueCode(view.getDimensionValueCode())
            .dimensionValueNameAr(view.getDimensionValueNameAr())
            .dimensionValueNameEn(view.getDimensionValueNameEn())
            .debitTotal(debitTotal)
            .creditTotal(creditTotal)
            .signedBalance(signedAgainstNature(view.getNatureCode(), debitTotal, creditTotal))
            .build();
    }

    /** Sums a group's or a whole statement's signed balances (POL-FIN-002). */
    public BigDecimal sumSignedBalances(List<AccountBalanceRowResponse> rows) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows == null) {
            return total;
        }
        for (AccountBalanceRowResponse row : rows) {
            total = total.add(zeroIfNull(row.getSignedBalance()));
        }
        return total;
    }

    /** Sums a trial-balance column, so API-FIN-029 can report the POL-FIN-008 identity. */
    public BigDecimal sumDebitBalances(List<AccountBalanceRowResponse> rows) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows == null) {
            return total;
        }
        for (AccountBalanceRowResponse row : rows) {
            total = total.add(zeroIfNull(row.getDebitBalance()));
        }
        return total;
    }

    /** Sums the other trial-balance column; see {@link #sumDebitBalances(List)}. */
    public BigDecimal sumCreditBalances(List<AccountBalanceRowResponse> rows) {
        BigDecimal total = BigDecimal.ZERO;
        if (rows == null) {
            return total;
        }
        for (AccountBalanceRowResponse row : rows) {
            total = total.add(zeroIfNull(row.getCreditBalance()));
        }
        return total;
    }

    /**
     * POL-FIN-002 — a debit-natured account (assets, expenses) carries a positive balance when its
     * debits exceed its credits; a credit-natured one (liabilities, equity, revenue) when its
     * credits exceed its debits. DEBIT_CREDIT is a two-value lookup (SRS A6) validated against MDL
     * before anything was written (XM-FIN-001), so the mapping is total: DEBIT means one sign,
     * anything else — necessarily CREDIT — the other.
     */
    private BigDecimal signedAgainstNature(String natureCode,
                                           BigDecimal debitTotal,
                                           BigDecimal creditTotal) {
        BigDecimal net = zeroIfNull(debitTotal).subtract(zeroIfNull(creditTotal));
        return JournalEntryDomain.DIRECTION_DEBIT.equalsIgnoreCase(natureCode)
            ? net
            : net.negate();
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

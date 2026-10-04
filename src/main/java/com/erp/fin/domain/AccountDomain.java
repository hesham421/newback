package com.erp.fin.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.fin.entity.Account;
import com.erp.fin.exception.FinErrorCodes;
import java.util.Optional;

/**
 * Domain companion for ENT-FIN-001 (Account), carrying the two SRS rules CORE.md assigns to it:
 *
 * <ul>
 *   <li><b>RULE-FIN-001</b> — an account with sub-accounts cannot accept direct posting. Two
 *       catalog rows, one per trigger: {@code FIN-409-PARENT-NOT-LEAF-ELIGIBLE} when a child is
 *       added under a parent still marked leaf (API-FIN-002), and {@code FIN-409-HAS-CHILDREN}
 *       when an account that already has children is marked leaf (API-FIN-003).</li>
 *   <li><b>RULE-FIN-007</b> — a posting line's account must be a leaf, active account
 *       ({@code FIN-409-NOT-POSTABLE-ACCOUNT}, API-FIN-019/020/014/017).</li>
 * </ul>
 *
 * <p>Placement note: DATA-DOM-MASTER.md annotates RULE-FIN-001 with "owner layer: service", but
 * CORE.md mandates a domain class for every rule answering "is this operation allowed?" and
 * gov-enforce-backend-contract A.5.18 makes a business-rule {@code if} inlined in a service an
 * automatic rejection. CORE.md wins; the rules live here.
 *
 * <p>The service resolves the has-children fact via QR-FIN-006
 * ({@code existsByParentAccount_AccountPk}) and passes it in as a plain argument — this class
 * never touches a repository (A.0.3) and never calls another module (A.0.6).
 */
public final class AccountDomain {

    /** ACCOUNT_TYPE (SRS A6) — a result account, zeroed by year-end close (REQ-FIN-036). */
    public static final String ACCOUNT_TYPE_REVENUE = "REVENUE";

    /** ACCOUNT_TYPE (SRS A6) — a result account, zeroed by year-end close (REQ-FIN-036). */
    public static final String ACCOUNT_TYPE_EXPENSE = "EXPENSE";

    private final Long accountPk;
    private final String code;
    private final boolean leaf;
    private final boolean active;

    private AccountDomain(Long accountPk, String code, boolean leaf, boolean active) {
        this.accountPk = accountPk;
        this.code = code;
        this.leaf = leaf;
        this.active = active;
    }

    /**
     * API-FIN-002 (create account): RULE-FIN-001 at the create trigger. A new account placed
     * under a parent that is still marked as accepting direct posting is rejected — the parent
     * must be demoted first ({@code Account.markAsParent()}).
     *
     * @param code               the submitted account code
     * @param leaf               whether the new account itself accepts direct posting
     * @param parentIsLeaf       whether the chosen parent is currently marked leaf; pass
     *                           {@code false} for a root account (no parent)
     * @throws LocalizedException {@code FIN-409-PARENT-NOT-LEAF-ELIGIBLE}
     */
    public static AccountDomain create(String code, boolean leaf, boolean parentIsLeaf) {
        if (parentIsLeaf) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_PARENT_NOT_LEAF_ELIGIBLE, code);
        }
        return new AccountDomain(null, code, leaf, true);
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static AccountDomain from(Account entity) {
        return new AccountDomain(entity.getAccountPk(),
            entity.getCode(),
            Boolean.TRUE.equals(entity.getIsLeafFl()),
            Boolean.TRUE.equals(entity.getIsActiveFl()));
    }

    /**
     * API-FIN-003 (update account): RULE-FIN-001 at the update trigger. Decision only — the
     * service calls {@code Account.markAsLeaf()} after this returns.
     *
     * @param hasChildAccounts QR-FIN-006's answer, resolved by the service
     * @throws LocalizedException {@code FIN-409-HAS-CHILDREN}
     */
    public void assertCanBeMarkedLeaf(boolean hasChildAccounts) {
        if (hasChildAccounts) {
            throw new LocalizedException(Status.CONFLICT,
                FinErrorCodes.FIN_409_HAS_CHILDREN, code);
        }
    }

    /**
     * API-FIN-003 (update account): the complete RULE-FIN-001 update-trigger decision, including
     * whether the rule applies at all. Added by SVC-API-CRUD so that the service never has to
     * write {@code if (requestedLeaf) { ... }} itself — a trigger conditional in a service body is
     * exactly what gov-enforce-backend-contract A.5.18 rejects, and
     * {@link #assertCanBeMarkedLeaf(boolean)} alone cannot express "only when the request asks for
     * leaf". It delegates to that method, so the rule itself still has one implementation.
     *
     * @param requestedLeaf    the {@code isLeafFl} value the update request carries
     * @param hasChildAccounts QR-FIN-006's answer, resolved by the service
     * @throws LocalizedException {@code FIN-409-HAS-CHILDREN}
     */
    public void assertLeafFlagChangeAllowed(boolean requestedLeaf, boolean hasChildAccounts) {
        if (requestedLeaf) {
            assertCanBeMarkedLeaf(hasChildAccounts);
        }
    }

    /**
     * RULE-FIN-007 — a journal line may only target a leaf, active account. Decision only; the
     * service calls this once per line before the entry is built (API-FIN-019, API-FIN-020,
     * API-FIN-014, API-FIN-017).
     *
     * @throws LocalizedException {@code FIN-409-NOT-POSTABLE-ACCOUNT}
     */
    public void assertPostable() {
        checkPostable().ifPresent(detail -> {
            throw new LocalizedException(Status.CONFLICT, detail.errorCode(), detail.args());
        });
    }

    /**
     * RULE-FIN-007, non-throwing sibling of {@link #assertPostable()} — the single implementation
     * of the rule; {@code assertPostable} delegates here, so the throwing API is unchanged for
     * every existing caller. It exists so the manual-entry path (REQ-FIN-015 / AC-FIN-015) can
     * evaluate every line's account without aborting and report all failures together. Same code
     * and same message argument as the throwing form, so each entry localizes identically.
     *
     * @return {@code FIN-409-NOT-POSTABLE-ACCOUNT} with the account code, or empty when postable
     */
    public Optional<ErrorDetail> checkPostable() {
        if (!leaf || !active) {
            return Optional.of(
                ErrorDetail.of(FinErrorCodes.FIN_409_NOT_POSTABLE_ACCOUNT, code));
        }
        return Optional.empty();
    }

    /**
     * REQ-FIN-036 / POL-FIN-010 — is this an ACCOUNT_TYPE that year-end close zeroes into
     * Retained Earnings (a result account), rather than one whose balance carries forward as the
     * next year's opening balance (a balance-sheet account)? The two sets partition SRS A6's
     * ACCOUNT_TYPE list exactly: {@code REVENUE}/{@code EXPENSE} are result accounts,
     * {@code ASSET}/{@code LIABILITY}/{@code EQUITY} are balance-sheet accounts.
     *
     * <p>Added by SVC-API-INT: API-FIN-027 has to split the year's accounts into those two sets
     * and the classification is a business decision, so it cannot be an {@code if} in the service
     * body (A.5.18). Static, and taking the plain code, so no field has to be added to this
     * class's existing factories.
     *
     * @param accountTypeCode the ACCOUNT_TYPE code the account carries (DBF-FIN-005)
     */
    public static boolean isResultAccountType(String accountTypeCode) {
        return ACCOUNT_TYPE_REVENUE.equalsIgnoreCase(accountTypeCode)
            || ACCOUNT_TYPE_EXPENSE.equalsIgnoreCase(accountTypeCode);
    }

    public Long getAccountPk() {
        return accountPk;
    }

    public String getCode() {
        return code;
    }

    public boolean isLeaf() {
        return leaf;
    }

    public boolean isActive() {
        return active;
    }
}

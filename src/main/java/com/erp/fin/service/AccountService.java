package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.AccountDomain;
import com.erp.fin.dto.AccountCreateRequest;
import com.erp.fin.dto.AccountSearchRequest;
import com.erp.fin.dto.AccountResponse;
import com.erp.fin.dto.AccountUpdateRequest;
import com.erp.fin.entity.Account;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.AccountMapper;
import com.erp.fin.repository.AccountRepository;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Orchestration layer for ENT-FIN-001 (Account) — API-FIN-002, API-FIN-003 and API-FIN-004
 * (SVC-API-CRUD.md). Load → delegate → persist → return; every "is this operation allowed?"
 * decision belongs to {@link AccountDomain} (CORE.md, A.5.18).
 *
 * <p>No caching annotations: FIN's approved cache register is empty and
 * gov-enforce-caching-rules bars caching financial data outright.
 *
 * <p>SVC-API-SEARCH added API-FIN-001 (search accounts) to this same service, with the A.5.6
 * {@code ALLOWED_SORT_FIELDS} whitelist it requires.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountService {

    /**
     * A.5.6 — the only fields API-FIN-001 may filter or sort by. It covers the plan's named
     * filters ({@code code}, {@code nameAr}/{@code nameEn}, {@code accountTypeCode},
     * {@code isActiveFl}) plus the flat scalar columns of ENT-FIN-001 a client can usefully order
     * by. {@code parentAccountId} is absent on purpose: it is an association, not a flat path, and
     * {@code SpecBuilder} resolves a field as {@code root.get(field)}.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "accountPk", "code", "nameAr", "nameEn", "accountTypeCode", "natureCode",
        "isLeafFl", "isActiveFl", "createdAt");

    private final AccountRepository repository;
    private final AccountMapper mapper;
    private final FinLookupValidationService lookupValidation;

    /**
     * API-FIN-002 — validate the two lookup-backed codes (XM-FIN-001), reject a duplicate code
     * (QR-FIN-005), then delegate RULE-FIN-001's create trigger to
     * {@link AccountDomain#create(String, boolean, boolean)}: a new sub-account may not be placed
     * under a parent that is still marked as accepting direct posting
     * ({@code FIN-409-PARENT-NOT-LEAF-ELIGIBLE}) — that parent must be demoted first.
     *
     * <p>The account-code uniqueness check sits here rather than in {@link AccountDomain} on
     * purpose: {@code FIN-409-ACCOUNT-DUP} is a PLATFORM-STD catalog row backed by
     * {@code UQ_FIN_ACCOUNT_CODE}, not an SRS RULE, and gov-enforce-error-handling's own service
     * pattern places a uniqueness check in the service ("Every uniqueness check throws
     * LocalizedException(Status.ALREADY_EXISTS, ...)"). The SRS rules that do exist for this
     * entity — RULE-FIN-001 and RULE-FIN-007 — are delegated.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNTS_CREATE)")
    public ServiceResult<AccountResponse> create(AccountCreateRequest request) {
        log.info("Creating Account with code: {}", request.getCode());

        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_ACCOUNT_TYPE, request.getAccountTypeCode());
        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_DEBIT_CREDIT, request.getNatureCode());

        if (repository.existsByCode(request.getCode())) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                FinErrorCodes.FIN_409_ACCOUNT_DUP, request.getCode());
        }

        Account parent = resolveParent(request.getParentAccountId());

        AccountDomain.create(request.getCode(),
            Boolean.TRUE.equals(request.getIsLeafFl()),
            parent != null && Boolean.TRUE.equals(parent.getIsLeafFl()));

        Account saved = repository.save(mapper.toEntity(request, parent));
        log.info("Created Account ID: {}", saved.getAccountPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-FIN-003 — RULE-FIN-001 at the update trigger: an account that already has sub-accounts
     * (QR-FIN-006) may not be marked as accepting direct posting
     * ({@code FIN-409-HAS-CHILDREN}). Both the "does the rule apply here?" and the "is it
     * satisfied?" halves are delegated to
     * {@link AccountDomain#assertLeafFlagChangeAllowed(boolean, boolean)}, so no business-rule
     * conditional appears in this body.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNTS_UPDATE)")
    public ServiceResult<AccountResponse> update(Long id, AccountUpdateRequest request) {
        log.info("Updating Account ID: {}", id);

        Account entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, id));

        boolean hasChildAccounts = repository.existsByParentAccount_AccountPk(id);
        AccountDomain.from(entity).assertLeafFlagChangeAllowed(
            Boolean.TRUE.equals(request.getIsLeafFl()), hasChildAccounts);

        mapper.updateEntityFromRequest(entity, request);

        Account saved = repository.save(entity);
        log.info("Updated Account ID: {}", saved.getAccountPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-FIN-004 — soft deactivation only (QR-FIN-004): no rule beyond existence, and the flag
     * moves through the entity's own {@code deactivate()} helper, never a direct assignment.
     * REQ-FIN-019 subsequently rejects any posting to the account, through
     * {@code AccountDomain.assertPostable()}.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNTS_UPDATE)")
    public ServiceResult<AccountResponse> deactivate(Long id) {
        log.info("Deactivating Account ID: {}", id);

        Account entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, id));

        entity.deactivate();

        Account saved = repository.save(entity);
        log.info("Deactivated Account ID: {}", saved.getAccountPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * Resolves the optional parent FK. A {@code null} id means a root account; a non-null id that
     * resolves to nothing is {@code FIN-404-ACCOUNT}. This is FK resolution, not a rule — the
     * decision it feeds belongs to {@link AccountDomain#create}.
     */
    private Account resolveParent(Long parentAccountId) {
        if (parentAccountId == null) {
            return null;
        }
        return repository.findById(parentAccountId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, parentAccountId));
    }

    /**
     * API-FIN-001 — QR-FIN-001, a criteria search over ENT-FIN-001. Read-only, so it carries no
     * business rule and nothing to delegate: load → map → return. An empty result is a success,
     * per CORE.md's search contract.
     *
     * <p>SVC-API-SEARCH.md says "join NONE", and that held until the {@code parentAccountId}
     * filter was added 2026-09-19: scoping a level of SCR-FIN-001's tree to its parent is a
     * predicate over the self-reference {@code parentAccount}, so the one join below is ANDed in
     * when — and only when — the caller sends that filter. Sending none leaves the query exactly
     * as it was.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNTS_VIEW)")
    public ServiceResult<Page<AccountResponse>> search(AccountSearchRequest searchRequest) {
        log.debug("Searching Account");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<Account> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(Set.of()));

        Long parentAccountId = searchRequest.getParentAccountId();
        if (parentAccountId != null) {
            Specification<Account> parentSpec = (root, query, cb) ->
                cb.equal(root.get("parentAccount").get("accountPk"), parentAccountId);
            spec = parentSpec.and(spec);
        }

        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
    }
}

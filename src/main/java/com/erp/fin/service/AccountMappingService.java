package com.erp.fin.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.fin.domain.AccountMappingDomain;
import com.erp.fin.dto.AccountMappingCreateRequest;
import com.erp.fin.dto.AccountMappingResponse;
import com.erp.fin.dto.AccountMappingSearchRequest;
import com.erp.fin.dto.AccountMappingUpdateRequest;
import com.erp.fin.entity.Account;
import com.erp.fin.entity.AccountMapping;
import com.erp.fin.exception.FinErrorCodes;
import com.erp.fin.mapper.AccountMappingMapper;
import com.erp.fin.repository.AccountMappingRepository;
import com.erp.fin.repository.AccountRepository;
import jakarta.persistence.criteria.JoinType;
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
 * Orchestration layer for ENT-FIN-015 (AccountMapping, v2) — API-FIN-039, API-FIN-040 and
 * API-FIN-041 (SVC-API-CRUD.md) plus API-FIN-038 (SVC-API-SEARCH.md). Load → delegate → persist
 * → return; every decision belongs to {@link AccountMappingDomain}. No caching annotations: FIN's
 * approved cache register is empty. There is no activate (ADR-FIN-024) and no delete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AccountMappingService {

    /**
     * A.5.6 — API-FIN-038's filter/sort whitelist: SCR-REQ-FIN-013 §B2's own columns, plus the pk
     * and {@code createdAt} every FIN search admits. The account's code and names are a joined
     * row (ADR-FIN-031), which the flat shared builders cannot filter or sort, so they are absent.
     */
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "accountMappingPk", "eventTypeCode", "businessFieldCode", "businessValue", "isActiveFl",
        "createdAt");

    private final AccountMappingRepository repository;
    private final AccountRepository accountRepository;
    private final AccountMappingMapper mapper;
    private final FinLookupValidationService lookupValidation;

    /**
     * API-FIN-039 — lookup codes (XM-FIN-001) → RULE-FIN-024 → the account (QR-FIN-060) →
     * RULE-FIN-023 → QR-FIN-052 → RULE-FIN-022 inside {@code AccountMappingDomain.create} →
     * persist, flushed (QR-FIN-051). The PAYMENT_METHOD membership fact is read for every create;
     * the Domain ignores it unless the field is PAYMENT_METHOD.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNT_MAPPINGS_CREATE)")
    public ServiceResult<AccountMappingResponse> create(AccountMappingCreateRequest request) {
        log.info("Creating AccountMapping for event type: {} field: {}",
            request.getEventTypeCode(), request.getBusinessFieldCode());

        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_ACCOUNTING_EVENT_TYPE, request.getEventTypeCode());
        lookupValidation.assertValidCode(
            FinLookupValidationService.KEY_FIN_EVENT_BUSINESS_FIELD, request.getBusinessFieldCode());

        // ADR-FIN-039 — the key is checked exactly as the store will hold it: blanks trimmed.
        String businessValue = request.getBusinessValue() == null
            ? null : request.getBusinessValue().trim();

        AccountMappingDomain.assertBusinessValueDefined(request.getBusinessFieldCode(),
            lookupValidation.isActiveCode(FinLookupValidationService.KEY_PAYMENT_METHOD,
                businessValue));

        Account account = resolveAccount(request.getAccountId());
        AccountMappingDomain.assertAccountEligible(
            Boolean.TRUE.equals(account.getIsLeafFl()), Boolean.TRUE.equals(account.getIsActiveFl()));

        boolean activeMappingExists = repository
            .existsByEventTypeCodeAndBusinessFieldCodeAndBusinessValueAndIsActiveFlTrue(
                request.getEventTypeCode(), request.getBusinessFieldCode(), businessValue);
        AccountMappingDomain.create(request.getEventTypeCode(), request.getBusinessFieldCode(),
            businessValue, account.getAccountPk(), activeMappingExists);

        // Flushed at once so a concurrent duplicate is refused inside this call and
        // FinPersistenceRefusalAdvisor answers FIN-409-MAPPING-DUP (ADR-FIN-030).
        AccountMapping saved = repository.saveAndFlush(mapper.toEntity(request, account));
        log.info("Created AccountMapping ID: {}", saved.getAccountMappingPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    /**
     * API-FIN-040 — the account only (ADR-FIN-024): load (FIN-404-ACCOUNT-MAPPING) → the new
     * account (QR-FIN-060) → RULE-FIN-023 → update (QR-FIN-053). No active-state gate: the SRS
     * states none.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNT_MAPPINGS_UPDATE)")
    public ServiceResult<AccountMappingResponse> update(Long id,
                                                        AccountMappingUpdateRequest request) {
        log.info("Updating AccountMapping ID: {}", id);

        AccountMapping entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT_MAPPING, id));

        Account account = resolveAccount(request.getAccountId());
        AccountMappingDomain.assertAccountEligible(
            Boolean.TRUE.equals(account.getIsLeafFl()), Boolean.TRUE.equals(account.getIsActiveFl()));

        mapper.updateEntityFromRequest(entity, request, account);

        AccountMapping saved = repository.save(entity);
        log.info("Updated AccountMapping ID: {}", saved.getAccountMappingPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /**
     * API-FIN-041 — soft deactivation only (QR-FIN-054): no rule beyond existence, an already
     * inactive mapping is not an error (the API-FIN-004 precedent), and the flag moves through the
     * entity's own {@code deactivate()} helper. From the next event on, RULE-FIN-020 no longer
     * resolves it.
     */
    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNT_MAPPINGS_UPDATE)")
    public ServiceResult<AccountMappingResponse> deactivate(Long id) {
        log.info("Deactivating AccountMapping ID: {}", id);

        AccountMapping entity = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT_MAPPING, id));

        entity.deactivate();

        AccountMapping saved = repository.save(entity);
        log.info("Deactivated AccountMapping ID: {}", saved.getAccountMappingPk());

        return ServiceResult.success(mapper.toResponse(saved), Status.UPDATED);
    }

    /** QR-FIN-060 — FK resolution, not a rule: an unknown id is FIN-404-ACCOUNT. */
    private Account resolveAccount(Long accountId) {
        return accountRepository.findById(accountId)
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, FinErrorCodes.FIN_404_ACCOUNT, accountId));
    }

    /**
     * API-FIN-038 — QR-FIN-050, criteria search over ENT-FIN-015, read-only; an empty page is
     * success (AC-FIN-059). The lifted {@code accountId} is ANDed in as an explicit join predicate
     * only when supplied. The account row the response's display columns come from is fetched
     * with the page in one SQL (A.2.6) — the fetch is skipped on the count query, whose
     * {@code Long} result type cannot carry it.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.permission.PermissionConstants)"
        + ".PERM_FIN_ACCOUNT_MAPPINGS_VIEW)")
    public ServiceResult<Page<AccountMappingResponse>> search(
            AccountMappingSearchRequest searchRequest) {
        log.debug("Searching AccountMapping");

        FinSearchSupport.assertSortAllowed(searchRequest.getSortField(), ALLOWED_SORT_FIELDS);

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        SetAllowedFields allowedFields = new SetAllowedFields(ALLOWED_SORT_FIELDS);
        Specification<AccountMapping> spec = SpecBuilder.build(commonRequest, allowedFields,
            FinSearchSupport.temporalFieldConverter(Set.of()));

        Long accountId = searchRequest.getAccountId();
        if (accountId != null) {
            Specification<AccountMapping> accountSpec = (root, query, cb) ->
                cb.equal(root.get("account").get("accountPk"), accountId);
            spec = accountSpec.and(spec);
        }

        Specification<AccountMapping> fetchAccount = (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("account", JoinType.LEFT);
            }
            return cb.conjunction();
        };
        spec = fetchAccount.and(spec);

        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
    }
}

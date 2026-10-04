package com.erp.sequence.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.PageableBuilder;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.sequence.domain.NumberSeriesDomain;
import com.erp.sequence.domain.ResetPolicy;
import com.erp.sequence.dto.NumberSeriesCreateRequest;
import com.erp.sequence.dto.NumberSeriesResponse;
import com.erp.sequence.dto.NumberSeriesSearchRequest;
import com.erp.sequence.dto.NumberSeriesUpdateRequest;
import com.erp.sequence.entity.NumberSeries;
import com.erp.sequence.exception.SequenceErrorCodes;
import com.erp.sequence.mapper.NumberSeriesMapper;
import com.erp.sequence.repository.NumberSeriesRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Admin API of the number series ({@code /api/v1/sequence/series}, erp-core step 09), always within the
 * caller's tenant (Hibernate's tenant discriminator). A series is one code; each of its periods is a row.
 * {@link #update}, {@link #activate} and {@link #deactivate} address any row of a code and apply to every
 * row of that code, so the configuration stays the same across periods. The counter is never edited here
 * (only {@link NumberAllocationService} moves it), and there is no delete: issued numbers must never be
 * reissued, so a series is deactivated instead.
 *
 * <p>No caching: {@code CORE_NUMBER_SERIES} is transactional (its counter moves on every allocation) and is
 * not on the caching approved-register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NumberSeriesService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id", "code", "periodKey", "createdAt", "updatedAt"
    );

    private static final Set<String> ALLOWED_FILTER_FIELDS = Set.of(
        "id", "code", "periodKey", "isActive", "createdAt", "updatedAt"
    );

    private final NumberSeriesRepository repository;
    private final NumberSeriesMapper mapper;
    private final ObjectProvider<Clock> clock;

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sequence.permission.SequencePermissions).PERM_SEQUENCE_SERIES_MANAGE)")
    public ServiceResult<NumberSeriesResponse> create(NumberSeriesCreateRequest request) {
        log.info("Creating number series with code: {}", request.getCode());

        String code = NumberAllocationService.normalize(request.getCode());
        ResetPolicy policy = request.getResetPolicy() != null ? request.getResetPolicy() : ResetPolicy.YEARLY;
        String pattern = request.getPattern() != null ? request.getPattern() : NumberSeries.DEFAULT_PATTERN;

        NumberSeriesDomain.create(code, request.getPrefix(), pattern, policy, repository.existsByCode(code));

        NumberSeries entity = mapper.toEntity(request, policy.periodKey(LocalDate.now(clock())));
        NumberSeries saved = repository.save(entity);
        log.info("Created number series ID: {}, code: {}", saved.getId(), saved.getCode());

        return ServiceResult.success(mapper.toResponse(saved), Status.CREATED);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sequence.permission.SequencePermissions).PERM_SEQUENCE_SERIES_VIEW)")
    public ServiceResult<NumberSeriesResponse> getById(Long id) {
        log.debug("Fetching number series ID: {}", id);
        return ServiceResult.success(mapper.toResponse(find(id)));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sequence.permission.SequencePermissions).PERM_SEQUENCE_SERIES_VIEW)")
    public ServiceResult<Page<NumberSeriesResponse>> search(NumberSeriesSearchRequest searchRequest) {
        log.debug("Searching number series");

        SearchRequest commonRequest = searchRequest.toCommonSearchRequest();
        Specification<NumberSeries> spec = SpecBuilder.build(commonRequest,
            new SetAllowedFields(ALLOWED_FILTER_FIELDS), DefaultFieldValueConverter.INSTANCE);
        Pageable pageable = PageableBuilder.from(commonRequest, ALLOWED_SORT_FIELDS);

        return ServiceResult.success(repository.findAll(spec, pageable).map(mapper::toResponse));
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sequence.permission.SequencePermissions).PERM_SEQUENCE_SERIES_MANAGE)")
    public ServiceResult<NumberSeriesResponse> update(Long id, NumberSeriesUpdateRequest request) {
        log.info("Updating number series ID: {}", id);

        NumberSeries target = find(id);
        NumberSeriesDomain.from(target).assertCanChangePattern(request.getPattern());

        List<NumberSeries> rows = repository.findAllByCodeOrderByIdAsc(target.getCode());
        rows.forEach(row -> mapper.updateEntityFromRequest(row, request));
        repository.saveAllAndFlush(rows);
        log.info("Updated number series code: {} ({} period rows)", target.getCode(), rows.size());

        return ServiceResult.success(mapper.toResponse(target), Status.UPDATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sequence.permission.SequencePermissions).PERM_SEQUENCE_SERIES_MANAGE)")
    public ServiceResult<NumberSeriesResponse> activate(Long id) {
        log.info("Activating number series ID: {}", id);

        NumberSeries target = find(id);
        List<NumberSeries> rows = repository.findAllByCodeOrderByIdAsc(target.getCode());
        rows.forEach(NumberSeries::activate);
        repository.saveAllAndFlush(rows);

        return ServiceResult.success(mapper.toResponse(target), Status.UPDATED);
    }

    @Transactional
    @PreAuthorize("hasAuthority(T(com.erp.sequence.permission.SequencePermissions).PERM_SEQUENCE_SERIES_MANAGE)")
    public ServiceResult<NumberSeriesResponse> deactivate(Long id) {
        log.info("Deactivating number series ID: {}", id);

        NumberSeries target = find(id);
        List<NumberSeries> rows = repository.findAllByCodeOrderByIdAsc(target.getCode());
        rows.forEach(NumberSeries::deactivate);
        repository.saveAllAndFlush(rows);

        return ServiceResult.success(mapper.toResponse(target), Status.UPDATED);
    }

    private NumberSeries find(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, SequenceErrorCodes.NUMBER_SERIES_NOT_FOUND, id));
    }

    private Clock clock() {
        return clock.getIfAvailable(Clock::systemDefaultZone);
    }
}

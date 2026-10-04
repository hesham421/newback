package com.erp.sec.report;

import com.erp.common.search.DefaultFieldValueConverter;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.common.search.SearchRequest;
import com.erp.common.search.SetAllowedFields;
import com.erp.common.search.SpecBuilder;
import com.erp.report.ColumnType;
import com.erp.report.ParamType;
import com.erp.report.ReportAuthorities;
import com.erp.report.ReportColumn;
import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import com.erp.sec.entity.User;
import com.erp.sec.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core reference report {@code SEC_USER_LIST} (erp-core step 11): the tenant's user accounts, staff and
 * customer, filtered by realm, status, active flag and creation date. Queried through
 * {@link UserRepository} with {@link SpecBuilder} — tenant-filtered by Hibernate, no native SQL. The
 * password hash is never a column.
 */
@Component
@RequiredArgsConstructor
public class SecUserListReport implements ReportProvider {

    public static final String CODE = "SEC_USER_LIST";
    public static final String MODULE = "SEC";
    /** {@code SEC:REPORT:SEC_USER_LIST}. */
    public static final String AUTHORITY = ReportAuthorities.of(MODULE, CODE);

    private static final Set<String> FILTER_FIELDS = Set.of("realm", "statusCode", "isActiveFl", "createdAt");

    private static final List<ReportColumn> COLUMNS = List.of(
        new ReportColumn("username", ColumnType.STRING, "اسم المستخدم", "Username"),
        new ReportColumn("email", ColumnType.STRING, "البريد الإلكتروني", "E-mail"),
        new ReportColumn("fullNameAr", ColumnType.STRING, "الاسم بالعربية", "Name (Arabic)"),
        new ReportColumn("fullNameEn", ColumnType.STRING, "الاسم بالإنجليزية", "Name (English)"),
        new ReportColumn("realm", ColumnType.STRING, "النطاق", "Realm"),
        new ReportColumn("status", ColumnType.STRING, "الحالة", "Status"),
        new ReportColumn("active", ColumnType.BOOLEAN, "نشط", "Active"),
        new ReportColumn("lastLoginAt", ColumnType.DATETIME, "آخر دخول", "Last login"),
        new ReportColumn("createdAt", ColumnType.DATETIME, "تاريخ الإنشاء", "Created at"));

    private final UserRepository userRepository;

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String moduleCode() {
        return MODULE;
    }

    @Override
    public String titleAr() {
        return "قائمة المستخدمين";
    }

    @Override
    public String titleEn() {
        return "User list";
    }

    @Override
    public List<ReportParam> params() {
        return List.of(
            ReportParam.of("realm", ParamType.STRING, false, "النطاق (STAFF / CUSTOMER)", "Realm (STAFF / CUSTOMER)"),
            ReportParam.of("status", ParamType.STRING, false, "الحالة", "Status"),
            ReportParam.of("activeOnly", ParamType.BOOLEAN, false, "النشطون فقط", "Active only"),
            ReportParam.of("createdFrom", ParamType.DATE, false, "أنشئ من تاريخ", "Created from"),
            ReportParam.of("createdTo", ParamType.DATE, false, "أنشئ حتى تاريخ", "Created to"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.sec.report.SecUserListReport).AUTHORITY)")
    public ReportResult run(Map<String, Object> params, Pageable page) {
        List<SearchFilter> filters = new ArrayList<>();
        addFilter(filters, "realm", SearchOperator.EQUALS, upper(params.get("realm")));
        addFilter(filters, "statusCode", SearchOperator.EQUALS, upper(params.get("status")));
        if (Boolean.TRUE.equals(params.get("activeOnly"))) {
            addFilter(filters, "isActiveFl", SearchOperator.EQUALS, Boolean.TRUE);
        }
        if (params.get("createdFrom") instanceof LocalDate from) {
            addFilter(filters, "createdAt", SearchOperator.GREATER_THAN_OR_EQUAL, startOf(from));
        }
        if (params.get("createdTo") instanceof LocalDate to) {
            addFilter(filters, "createdAt", SearchOperator.LESS_THAN, startOf(to.plusDays(1)));
        }
        Specification<User> spec = SpecBuilder.build(SearchRequest.builder().filters(filters).build(),
            new SetAllowedFields(FILTER_FIELDS), DefaultFieldValueConverter.INSTANCE);

        Page<User> users = userRepository.findAll(spec,
            PageRequest.of(page.getPageNumber(), page.getPageSize(), Sort.by("username").and(Sort.by("realm"))));
        return new ReportResult(COLUMNS, users.getContent().stream().map(SecUserListReport::toRow).toList(),
            Map.of("users", users.getTotalElements()), users.getTotalElements());
    }

    private static Map<String, Object> toRow(User user) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("username", user.getUsername());
        row.put("email", user.getEmail());
        row.put("fullNameAr", user.getFullNameAr());
        row.put("fullNameEn", user.getFullNameEn());
        row.put("realm", user.getRealm());
        row.put("status", user.getStatusCode());
        row.put("active", user.getIsActiveFl());
        row.put("lastLoginAt", user.getLastLoginAt());
        row.put("createdAt", user.getCreatedAt());
        return row;
    }

    private static void addFilter(List<SearchFilter> filters, String field, SearchOperator operator, Object value) {
        if (value != null) {
            filters.add(SearchFilter.builder().field(field).operator(operator).value(value).build());
        }
    }

    private static String upper(Object value) {
        return value == null ? null : value.toString().toUpperCase(java.util.Locale.ROOT);
    }

    private static Instant startOf(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}

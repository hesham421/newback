package com.erp.report.registry;

import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Every {@link ReportProvider} bean of the application, collected once at startup (erp-core step 11).
 * The registry refuses an invalid catalog by failing the startup with a message naming the offending
 * provider classes:
 * <ul>
 *   <li>two providers with the same {@code code()};</li>
 *   <li>a code that is not upper snake case of 2..40 characters (it becomes the registry action code,
 *       {@code SEC_ACTION_REG.ACTION_CODE VARCHAR(40)});</li>
 *   <li>a module code that is not upper case of 1..10 characters ({@code SEC_MODULE_REG.CODE});</li>
 *   <li>a blank title, a parameter without name or type, a duplicate parameter name, or a
 *       {@code LOOKUP} parameter without lookup key.</li>
 * </ul>
 * Startup infrastructure, not a request path: a broken catalog is a programming error of the module
 * or application that contributed it, so it is an {@link IllegalStateException}, not a
 * {@code LocalizedException} (same precedent as {@code FileStorageAutoConfiguration}).
 */
@Component
@Slf4j
public class ReportRegistry {

    private static final Pattern CODE = Pattern.compile("^[A-Z][A-Z0-9_]{1,39}$");
    private static final Pattern MODULE_CODE = Pattern.compile("^[A-Z][A-Z0-9_]{0,9}$");

    private final Map<String, ReportProvider> providers;

    @Autowired
    public ReportRegistry(ObjectProvider<ReportProvider> providers) {
        this(providers.orderedStream().toList());
    }

    /** Builds and validates the registry from an explicit list (also used by unit tests). */
    public ReportRegistry(Collection<? extends ReportProvider> providers) {
        Map<String, ReportProvider> byCode = new LinkedHashMap<>();
        for (ReportProvider provider : providers) {
            validate(provider);
            ReportProvider previous = byCode.putIfAbsent(provider.code(), provider);
            if (previous != null) {
                throw new IllegalStateException("Duplicate report code '" + provider.code() + "': contributed by "
                    + previous.getClass().getName() + " and " + provider.getClass().getName());
            }
        }
        List<ReportProvider> sorted = byCode.values().stream()
            .sorted(Comparator.comparing(ReportProvider::moduleCode).thenComparing(ReportProvider::code)).toList();
        Map<String, ReportProvider> ordered = new LinkedHashMap<>();
        sorted.forEach(provider -> ordered.put(provider.code(), provider));
        this.providers = java.util.Collections.unmodifiableMap(ordered);
        log.info("Report registry: {} report(s) {}", this.providers.size(), this.providers.keySet());
    }

    /** Every registered report, ordered by module code, then code. */
    public List<ReportProvider> all() {
        return List.copyOf(providers.values());
    }

    /** The report with {@code code}, if registered. */
    public Optional<ReportProvider> find(String code) {
        return code == null ? Optional.empty() : Optional.ofNullable(providers.get(code));
    }

    private static void validate(ReportProvider provider) {
        String source = provider.getClass().getName();
        require(provider.code() != null && CODE.matcher(provider.code()).matches(), source,
            "code() must be upper snake case of 2..40 characters (was '" + provider.code() + "')");
        require(provider.moduleCode() != null && MODULE_CODE.matcher(provider.moduleCode()).matches(), source,
            "moduleCode() must be upper case of 1..10 characters (was '" + provider.moduleCode() + "')");
        require(notBlank(provider.titleAr()) && notBlank(provider.titleEn()), source,
            "titleAr() and titleEn() must not be blank");
        List<ReportParam> params = provider.params() == null ? List.of() : provider.params();
        Set<String> names = new HashSet<>();
        for (ReportParam param : params) {
            require(param != null && notBlank(param.name()) && param.type() != null, source,
                "every parameter needs a name and a type");
            require(names.add(param.name()), source, "duplicate parameter '" + param.name() + "'");
            require(param.type() != com.erp.report.ParamType.LOOKUP || notBlank(param.lookupKey()), source,
                "LOOKUP parameter '" + param.name() + "' needs a lookupKey");
        }
    }

    private static void require(boolean condition, String source, String message) {
        if (!condition) {
            throw new IllegalStateException("Invalid report provider " + source + ": " + message);
        }
    }

    private static boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }
}

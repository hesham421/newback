package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Guards the core migration chain ({@code classpath:db/migration/core}, see its {@code README.md}):
 * <ul>
 *   <li>core owns versions {@code V1..V999}; {@code V1000+} belong to applications, so no core
 *       script may carry a version of 1000 or more;</li>
 *   <li>every core script is named {@code V<n>__<module>_<slug>.sql} with {@code <module>} one of
 *       the core modules {@value #MODULES} and a lower-case snake-case slug;</li>
 *   <li>no two core scripts share a version.</li>
 * </ul>
 * The folder's {@code README.md} is the only other file allowed there.
 */
class MigrationNamingTest {

    static final String MODULES = "core|cu|mdl|sec|file|notif|tenant|audit|events|sequence|report";

    private static final Pattern FILE_NAME = Pattern.compile(
        "^V([1-9][0-9]*)__(" + MODULES + ")_([a-z0-9]+(?:_[a-z0-9]+)*)\\.sql$");

    private static final int FIRST_APPLICATION_VERSION = 1000;

    private static final String CORE_LOCATION_PATTERN = "classpath*:db/migration/core/*";

    @Test
    void everyCoreScriptIsNamedVnModuleSlug_withAVersionBelow1000_andUnique() throws IOException {
        List<String> fileNames = coreFileNames();
        assertThat(fileNames).as("core migration files found on the classpath").isNotEmpty();

        List<String> violations = new ArrayList<>();
        Map<Integer, String> byVersion = new HashMap<>();
        for (String fileName : fileNames) {
            if (fileName.equals("README.md")) {
                continue;
            }
            Matcher matcher = FILE_NAME.matcher(fileName);
            if (!matcher.matches()) {
                violations.add(fileName + ": not V<n>__<module>_<slug>.sql with module in {" + MODULES + "}");
                continue;
            }
            int version = Integer.parseInt(matcher.group(1));
            if (version >= FIRST_APPLICATION_VERSION) {
                violations.add(fileName + ": version " + version + " is in the application range (V1000+); "
                    + "core owns V1..V999");
            }
            String previous = byVersion.put(version, fileName);
            if (previous != null) {
                violations.add(fileName + ": version " + version + " is already used by " + previous);
            }
        }
        assertThat(violations).as("core migration naming violations").isEmpty();
    }

    @Test
    void theNamingRuleItself_rejectsApplicationVersionsAndUnknownModules() {
        assertThat(FILE_NAME.matcher("V7__sec_seed.sql").matches()).isTrue();
        assertThat(FILE_NAME.matcher("V10__tenant_schema.sql").matches()).isTrue();
        assertThat(FILE_NAME.matcher("V14__sequence_and_settings.sql").matches()).isTrue();
        assertThat(FILE_NAME.matcher("V1000__x.sql").matches()).isFalse();
        assertThat(FILE_NAME.matcher("V12__fin_journal.sql").matches()).isFalse();
        assertThat(FILE_NAME.matcher("V3__sec_Seed.sql").matches()).isFalse();
        assertThat(FILE_NAME.matcher("V03__sec_seed.sql").matches()).isFalse();
        assertThat(FILE_NAME.matcher("V3_sec_seed.sql").matches()).isFalse();
    }

    private static List<String> coreFileNames() throws IOException {
        List<String> names = new ArrayList<>();
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources(CORE_LOCATION_PATTERN)) {
            String name = resource.getFilename();
            if (name != null && !name.isEmpty()) {
                names.add(name);
            }
        }
        return names;
    }
}

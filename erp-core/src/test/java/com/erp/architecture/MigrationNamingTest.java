package com.erp.architecture;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 *   <li>no two core scripts share a version;</li>
 *   <li>erp-core step 12 — <b>additive only</b>: a core script after {@code V9} (the step-04 squash) contains
 *       no {@code DROP TABLE}, {@code DROP COLUMN}, {@code RENAME} or {@code ALTER [COLUMN] <col> [SET DATA] TYPE}
 *       (checked on the SQL with comments stripped and string literals blanked, so a word inside a comment or
 *       a seed value never counts).</li>
 * </ul>
 * The folder's {@code README.md} is the only other file allowed there. Moved from {@code com.erp.autoconfigure}
 * to {@code com.erp.architecture} in step 12, next to the other build-enforced library rules.
 */
class MigrationNamingTest {

    static final String MODULES = "core|cu|mdl|sec|file|notif|tenant|audit|events|sequence|report";

    private static final Pattern FILE_NAME = Pattern.compile(
        "^V([1-9][0-9]*)__(" + MODULES + ")_([a-z0-9]+(?:_[a-z0-9]+)*)\\.sql$");

    private static final int FIRST_APPLICATION_VERSION = 1000;

    private static final String CORE_LOCATION_PATTERN = "classpath*:db/migration/core/*";

    /** The last version of the step-04 squashed baseline; every later core script is additive only. */
    static final int LAST_BASELINE_VERSION = 9;

    /** Non-additive DDL (case-insensitive, on comment-free SQL with string literals blanked). */
    static final Pattern NON_ADDITIVE = Pattern.compile(
        "\\bDROP\\s+TABLE\\b"
            + "|\\bDROP\\s+COLUMN\\b"
            + "|\\bRENAME\\b"
            + "|\\bALTER\\s+(?:COLUMN\\s+)?(?:\"[^\"]+\"|[A-Za-z_][A-Za-z0-9_$]*)\\s+(?:SET\\s+DATA\\s+)?TYPE\\b",
        Pattern.CASE_INSENSITIVE);

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

    @Test
    void coreScriptsAfterTheBaseline_areAdditiveOnly() throws IOException {
        List<String> violations = new ArrayList<>();
        int checked = 0;
        for (Resource resource : new PathMatchingResourcePatternResolver().getResources(CORE_LOCATION_PATTERN)) {
            String name = resource.getFilename();
            Matcher matcher = name == null ? null : FILE_NAME.matcher(name);
            if (matcher == null || !matcher.matches() || Integer.parseInt(matcher.group(1)) <= LAST_BASELINE_VERSION) {
                continue;
            }
            checked++;
            String sql;
            try (InputStream in = resource.getInputStream()) {
                sql = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            Matcher statement = NON_ADDITIVE.matcher(stripCommentsAndLiterals(sql));
            while (statement.find()) {
                violations.add(name + ": non-additive DDL '" + statement.group() + "' (core scripts after V"
                    + LAST_BASELINE_VERSION + " are additive only: fix forward with a new table/column instead)");
            }
        }
        assertThat(checked).as("core scripts after V" + LAST_BASELINE_VERSION).isPositive();
        assertThat(violations).as("additive-only violations in the core chain").isEmpty();
    }

    @Test
    void theAdditiveGuardItself_flagsDropsRenamesAndTypeChanges_butNotCommentsOrLiterals() {
        assertThat(nonAdditive("DROP TABLE x;")).isTrue();
        assertThat(nonAdditive("drop table if exists x;")).isTrue();
        assertThat(nonAdditive("ALTER TABLE x DROP COLUMN y;")).isTrue();
        assertThat(nonAdditive("ALTER TABLE x RENAME COLUMN a TO b;")).isTrue();
        assertThat(nonAdditive("ALTER TABLE x RENAME TO y;")).isTrue();
        assertThat(nonAdditive("ALTER TABLE x ALTER COLUMN y TYPE BIGINT;")).isTrue();
        assertThat(nonAdditive("ALTER TABLE x ALTER COLUMN \"Y\" SET DATA TYPE TEXT;")).isTrue();
        assertThat(nonAdditive("ALTER TABLE x ALTER y TYPE TEXT;")).isTrue();

        assertThat(nonAdditive("ALTER TABLE x ADD COLUMN y BIGINT DEFAULT 0;")).isFalse();
        assertThat(nonAdditive("ALTER TABLE x ALTER COLUMN y DROP DEFAULT;")).isFalse();
        assertThat(nonAdditive("ALTER TABLE x ALTER COLUMN y DROP NOT NULL;")).isFalse();
        assertThat(nonAdditive("CREATE TABLE x (ID BIGINT, TYPE VARCHAR(10));")).isFalse();
        assertThat(nonAdditive("-- no rename, drop or type change\nCREATE INDEX i ON x (y);")).isFalse();
        assertThat(nonAdditive("/* DROP TABLE x; */ CREATE INDEX i ON x (y);")).isFalse();
        assertThat(nonAdditive("INSERT INTO x (NAME) VALUES ('Rename file -- or DROP TABLE it');")).isFalse();
        assertThat(nonAdditive("INSERT INTO x (NAME) VALUES ('it''s'); DROP TABLE y;")).isTrue();
    }

    private static boolean nonAdditive(String sql) {
        return NON_ADDITIVE.matcher(stripCommentsAndLiterals(sql)).find();
    }

    /**
     * Removes {@code -- line} and {@code /* block *}{@code /} comments and blanks the content of single-quoted
     * string literals ({@code ''} escapes kept inside the literal), so only SQL keywords remain.
     */
    static String stripCommentsAndLiterals(String sql) {
        StringBuilder out = new StringBuilder(sql.length());
        int i = 0;
        while (i < sql.length()) {
            char c = sql.charAt(i);
            if (c == '-' && i + 1 < sql.length() && sql.charAt(i + 1) == '-') {
                while (i < sql.length() && sql.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && i + 1 < sql.length() && sql.charAt(i + 1) == '*') {
                int end = sql.indexOf("*/", i + 2);
                i = end < 0 ? sql.length() : end + 2;
                out.append(' ');
            } else if (c == '\'') {
                i++;
                while (i < sql.length()) {
                    if (sql.charAt(i) == '\'') {
                        if (i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                            i += 2;
                            continue;
                        }
                        break;
                    }
                    i++;
                }
                i++;
                out.append("''");
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
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

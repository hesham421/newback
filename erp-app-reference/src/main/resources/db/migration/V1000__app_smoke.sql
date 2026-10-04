-- Reference application's own first migration. Application scripts live in
-- classpath:db/migration and are numbered V1000+; erp-core's chain (V1..V999) is
-- prepended from classpath:db/migration/core by ErpCoreFlywayAutoConfiguration.
CREATE TABLE APP_SMOKE (
    ID BIGINT PRIMARY KEY
);

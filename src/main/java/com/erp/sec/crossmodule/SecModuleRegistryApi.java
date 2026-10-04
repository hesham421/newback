package com.erp.sec.crossmodule;

/**
 * SEC's cross-module surface for XM-MDL-001 (INT-C.md): a narrow, read-model-free existence check
 * consumed by MDL's {@code LookupTypeService.create()} to enforce RULE-MDL-001 ("a lookup type
 * registration whose owner module code has no ModuleRegistry row in SEC is rejected"). Direct
 * Spring interface injection, never loopback HTTP — same shape as {@link SecUserDirectoryApi},
 * whose own javadoc states that doctrine explicitly. INT-C.md's literal "REST call —
 * GET /api/v1/sec/registry?moduleCode={code}" wording is superseded by that established doctrine;
 * this interface is the documented deviation.
 *
 * <p>Returns a primitive, not an entity or DTO — nothing to leak across the module boundary.
 */
public interface SecModuleRegistryApi {

    /**
     * RULE-MDL-001 / QR-MDL-012 — true when {@code moduleCode} has an active
     * {@code SEC_MODULE_REG} row. Consumed only from MDL's service layer, never from a controller.
     */
    boolean isModuleActive(String moduleCode);
}

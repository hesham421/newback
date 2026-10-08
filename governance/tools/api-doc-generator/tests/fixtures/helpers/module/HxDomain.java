package com.erp.hx.domain;

public final class HxDomain {

    private static final HxTransitions TRANSITIONS = new HxTransitions(Map.of(
        "NEW", Set.of("DONE")), HxErrorCodes.HX_422_BAD_TRANSITION);

    private final String status;

    private HxDomain(String status) {
        this.status = status;
    }

    public static HxDomain create(String code, String name, boolean codeTaken) {
        HxRules.assertNotBlank(HxErrorCodes.HX_400_NAME_REQUIRED, code, name);
        HxRules.assertUnique(codeTaken, HxErrorCodes.HX_409_CODE_DUP, code);
        return new HxDomain("NEW");
    }

    public static HxDomain from(Item item) {
        return new HxDomain(item.getStatus());
    }

    public void assertCanMoveTo(String to) {
        TRANSITIONS.assertAllowed(status, to);
    }
}

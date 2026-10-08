package com.erp.common.idempotency;

@Component
@RequiredArgsConstructor
public class HxIdempotentResponses {

    private final HxResponder responder;

    public <T> ResponseEntity<ApiResponse<T>> craftResponse(String key, Object request, Supplier<ServiceResult<T>> action) {
        if (key == null) {
            return responder.craftResponse(action.get());
        }
        HxKeyDomain.assertKeyValid(key);
        if (lostTwice(key)) {
            throw new LocalizedException(Status.CONFLICT, HxKeyErrorCodes.HX_KEY_CONFLICT);
        }
        HxKeyDomain.from(key).assertReplayableFor(request);
        return responder.craftResponse(action.get());
    }

    private boolean lostTwice(String key) {
        return key.isEmpty();
    }
}

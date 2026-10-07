package com.erp.hx.service;

public class HxService {

    private static final Set<String> OWNED_KEYS = Set.of("HX_KIND");

    private final HxRepository repository;

    public ServiceResult<ItemResponse> create(ItemCreateRequest request) {
        HxDomain.create(request.getCode(), request.getName(), repository.existsByCode(request.getCode()));
        return ServiceResult.success(null);
    }

    public ServiceResult<ItemResponse> move(Long id, String to) {
        Item item = repository.findById(id)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, HxErrorCodes.HX_404_ITEM, id));
        HxDomain.from(item).assertCanMoveTo(to);
        return ServiceResult.success(null);
    }

    public ServiceResult<List<String>> lookup(String key) {
        return ServiceResult.success(HxLookups.read(key, OWNED_KEYS, HxErrorCodes.HX_404_LOOKUP_KEY,
            normalized -> List.of()));
    }

    public ServiceResult<Page<ItemResponse>> search(ItemSearchRequest request) {
        FieldValueConverter converter = converter();
        return ServiceResult.success(null);
    }

    private FieldValueConverter converter() {
        return new HxInstantConverter(Set.of("createdAt"));
    }
}

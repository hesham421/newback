package com.erp.common.search;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchRequest {

    @Builder.Default
    private List<SearchFilter> filters = List.of();

    private String sortField;

    @Builder.Default
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    @Builder.Default
    private int page = 0;

    @Builder.Default
    private int size = 20;
}

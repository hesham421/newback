package com.erp.common.search;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchFilter {

    private String field;
    private SearchOperator operator;
    private Object value;
}

package com.friendchat.common.dto;

import lombok.Builder;
import lombok.Value;
import org.springframework.data.domain.Page;

import java.util.List;

@Value
@Builder
public class PageResponse<T> {
    List<T> data;
    int     page;
    int     size;
    long    total;
    boolean hasMore;

    public static <T> PageResponse<T> of(Page<T> page) {
        return PageResponse.<T>builder()
                .data(page.getContent()).page(page.getNumber())
                .size(page.getSize()).total(page.getTotalElements())
                .hasMore(page.hasNext()).build();
    }
}

package com.example.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PaginationResult<T> {

    private final List<T> items;
    private final int currentPage;
    private final int pageSize;
    private final long totalItems;
    private final long totalPages;

    public PaginationResult(
            List<T> items,
            int currentPage,
            int pageSize,
            long totalItems
    ) {
        this.items = Collections.unmodifiableList(
                new ArrayList<>(items)
        );
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.totalItems = totalItems;
        this.totalPages =
                totalItems / pageSize
                        + (totalItems % pageSize == 0 ? 0 : 1);
    }

    public List<T> getItems() {
        return items;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getTotalItems() {
        return totalItems;
    }

    public long getTotalPages() {
        return totalPages;
    }
}
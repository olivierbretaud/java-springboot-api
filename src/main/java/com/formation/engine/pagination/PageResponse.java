package com.formation.engine.pagination;

import java.util.List;

import org.springframework.data.domain.Page;

import lombok.Getter;

@Getter
public class PageResponse<T> {

    private final List<T> contenu;
    private final int page;
    private final int size;
    private final long totalElements;
    private final int totalPages;
    private final boolean dernierePage;

    public PageResponse(Page<T> page) {
        this.contenu = page.getContent();
        this.page = page.getNumber();
        this.size = page.getSize();
        this.totalElements = page.getTotalElements();
        this.totalPages = page.getTotalPages();
        this.dernierePage = page.isLast();
    }
}

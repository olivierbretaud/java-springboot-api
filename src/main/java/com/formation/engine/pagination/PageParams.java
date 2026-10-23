package com.formation.engine.pagination;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class PageParams {

    private int page = 0;
    private int size = 10;
    private String sort = "id";
    @NonNull
    private String direction = "ASC";

    @NonNull
    @SuppressWarnings("null")
    public Pageable toPageable() {
        String sortField = (sort != null && !sort.isBlank()) ? sort : "id";
        Sort.Direction dir = Sort.Direction.fromOptionalString(direction).orElse(Sort.Direction.ASC);
        return PageRequest.of(page, size, Sort.by(dir, sortField));
    }
}

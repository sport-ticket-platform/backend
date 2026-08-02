package com.reservation.dto;

import java.util.List;

public record PageResult<T>(
        List<T> data,
        int current_page,
        int page_size,
        long total_elements,
        int total_pages,
        boolean is_first,
        boolean is_last
) {}
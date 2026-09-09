package com.inklusport.reports.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class PagedUsersResponse {

    private List<Map<String, Object>> content = new ArrayList<>();
    private long totalElements;
    private int totalPages;
    private int number;
    private int size;
    private boolean first = true;
    private boolean last = true;
}

package com.sala.challenge.dto;

import io.ebean.PagedList;
import lombok.Getter;
import lombok.Setter;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Getter
@Setter
@Schema(description = "Generic paginated response envelope used by all list endpoints")
public class PagedResultDTO<T> {

    @Schema(description = "Current page of items")
    private List<T> list;

    @Schema(description = "Total number of rows matching the query across all pages", example = "243")
    private long totalRows;

    @Schema(description = "Total number of available pages based on pageSize", example = "3")
    private int totalPages;

    @Schema(description = "Maximum number of items returned per page", example = "100")
    private int pageSize = 100;

    @Schema(description = "Current page number (1-based)", example = "1")
    private int page = 1;

    public static <T, R> PagedResultDTO<R> of(PagedList<T> list, Function<? super T, ? extends R> mapper) {
        PagedResultDTO<R> pr = new PagedResultDTO<>();
        pr.setList(list.getList().stream().map(mapper).collect(Collectors.toList()));
        pr.setPage(list.getPageIndex() + 1);
        pr.setTotalPages(list.getTotalPageCount());
        pr.setTotalRows(list.getTotalCount());
        pr.setPageSize(list.getPageSize());
        return pr;
    }
}

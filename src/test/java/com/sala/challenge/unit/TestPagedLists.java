package com.sala.challenge.unit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import io.ebean.PagedList;

public final class TestPagedLists {

    private TestPagedLists() {
    }

    public static <T> PagedList<T> empty() {
        @SuppressWarnings("unchecked")
        PagedList<T> pagedList = mock(PagedList.class);
        when(pagedList.getList()).thenReturn(List.of());
        when(pagedList.getPageIndex()).thenReturn(0);
        when(pagedList.getTotalPageCount()).thenReturn(0);
        when(pagedList.getTotalCount()).thenReturn(0);
        when(pagedList.getPageSize()).thenReturn(100);
        return pagedList;
    }
}

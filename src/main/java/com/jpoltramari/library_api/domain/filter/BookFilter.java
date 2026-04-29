package com.jpoltramari.library_api.domain.filter;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookFilter {

    private String title;
    private String genre;
    private String authorName;
    private Boolean available;
}

package com.jpoltramari.library_api.application.command.bookcopy;

import com.jpoltramari.library_api.domain.enums.CopyStatus;

public record UpdateBookCopyCommand(
        CopyStatus status,
        String location,
        Boolean active
) {}

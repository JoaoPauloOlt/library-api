package com.jpoltramari.library_api.infrastructure.security;

/**
 * Compile-time constants for {@link org.springframework.security.access.prepost.PreAuthorize}
 * and endpoint authorization rules.
 */
public final class SecurityExpressions {

    public static final String BOOK_READ = "hasAuthority('BOOK_READ')";
    public static final String BOOK_CREATE = "hasAuthority('BOOK_CREATE')";
    public static final String BOOK_UPDATE = "hasAuthority('BOOK_UPDATE')";
    public static final String BOOK_DELETE = "hasAuthority('BOOK_DELETE')";

    public static final String AUTHOR_READ = "hasAuthority('AUTHOR_READ')";
    public static final String AUTHOR_CREATE = "hasAuthority('AUTHOR_CREATE')";
    public static final String AUTHOR_UPDATE = "hasAuthority('AUTHOR_UPDATE')";
    public static final String AUTHOR_DELETE = "hasAuthority('AUTHOR_DELETE')";

    public static final String BOOK_COPY_READ = "hasAuthority('BOOK_COPY_READ')";
    public static final String BOOK_COPY_CREATE = "hasAuthority('BOOK_COPY_CREATE')";
    public static final String BOOK_COPY_UPDATE = "hasAuthority('BOOK_COPY_UPDATE')";
    public static final String BOOK_COPY_DELETE = "hasAuthority('BOOK_COPY_DELETE')";

    public static final String LOAN_CREATE = "hasAuthority('LOAN_CREATE')";
    public static final String LOAN_APPROVE = "hasAuthority('LOAN_APPROVE')";
    public static final String LOAN_WITHDRAW = "hasAuthority('LOAN_WITHDRAW')";
    public static final String LOAN_RETURN = "hasAuthority('LOAN_RETURN')";
    public static final String LOAN_CANCEL = "hasAuthority('LOAN_CANCEL')";
    public static final String LOAN_READ_ALL = "hasAuthority('LOAN_READ_ALL')";

    public static final String USER_ADMIN = "hasAuthority('USER_ADMIN')";

    private SecurityExpressions() {
    }
}

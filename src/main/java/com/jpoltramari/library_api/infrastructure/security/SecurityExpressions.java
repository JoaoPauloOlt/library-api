package com.jpoltramari.library_api.infrastructure.security;

/**
 * Compile-time constants for {@link org.springframework.security.access.prepost.PreAuthorize}.
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

    public static final String CREATE_LOAN = "hasAuthority('CREATE_LOAN')";
    public static final String APPROVE_LOAN = "hasAuthority('APPROVE_LOAN')";
    public static final String WITHDRAW_LOAN = "hasAuthority('WITHDRAW_LOAN')";
    public static final String RETURN_BOOK = "hasAuthority('RETURN_BOOK')";
    public static final String CANCEL_LOAN = "hasAuthority('CANCEL_LOAN')";

    public static final String USER_ADMIN = "hasAuthority('USER_ADMIN')";
    public static final String LOAN_MANAGE = "hasAuthority('LOAN_MANAGE')";

    private SecurityExpressions() {
    }
}

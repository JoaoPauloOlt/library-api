CREATE TABLE book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(100) NOT NULL UNIQUE,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(100),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT chk_book_copy_status
        CHECK (
            status IN (
                       'AVAILABLE',
                       'LOANED',
                       'MAINTENANCE',
                       'LOST',
                       'DISCARDED'
                )
            ),

    CONSTRAINT fk_book_copy_book
        FOREIGN KEY (book_id)
            REFERENCES books(id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_book_copies_book_id ON book_copies(book_id);

CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,

    user_id BIGINT NOT NULL,
    book_copy_id BIGINT NOT NULL,

    status VARCHAR(20) NOT NULL,

    request_date TIMESTAMP NOT NULL,
    approval_date TIMESTAMP,
    withdrawable_date TIMESTAMP,
    due_date TIMESTAMP,
    return_date TIMESTAMP,

    CONSTRAINT chk_loan_status
        CHECK (
            status IN (
                       'REQUESTED',
                       'APPROVED',
                       'ACTIVE',
                       'RETURNED',
                       'LATE',
                       'CANCELED'
                )
            ),

    CONSTRAINT fk_loans_user
        FOREIGN KEY (user_id)
            REFERENCES users(id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_loans_book_copy
        FOREIGN KEY (book_copy_id)
            REFERENCES book_copies(id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_loans_user_id ON loans(user_id);
CREATE INDEX idx_loans_book_copy_id ON loans(book_copy_id);
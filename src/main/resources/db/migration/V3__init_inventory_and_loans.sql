CREATE TABLE book_copies (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    barcode VARCHAR(100) UNIQUE NOT NULL,
    status VARCHAR(30) NOT NULL,
    location VARCHAR(100),

    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

CREATE INDEX idx_book_copies_book_id ON book_copies(book_id);


CREATE TABLE loans (
    id BIGSERIAL PRIMARY KEY,

    status VARCHAR(20) NOT NULL,

    request_date TIMESTAMP NOT NULL,
    approval_date TIMESTAMP,
    withdrawable_date TIMESTAMP,
    due_date TIMESTAMP,
    return_date TIMESTAMP,

    user_id BIGINT NOT NULL,
    book_copy_id BIGINT NOT NULL,

    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE RESTRICT,
    FOREIGN KEY (book_copy_id) REFERENCES book_copies(id) ON DELETE RESTRICT
);

CREATE INDEX idx_loans_user_id ON loans(user_id);
CREATE INDEX idx_loans_book_copy_id ON loans(book_copy_id);
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) UNIQUE NOT NULL,
                       password VARCHAR(255) NOT NULL,
                       telephone VARCHAR(20) NOT NULL,
                       status VARCHAR(20) NOT NULL,
                       date_register TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE groups (
                        id BIGSERIAL PRIMARY KEY,
                        name VARCHAR(50) UNIQUE NOT NULL,
                        description VARCHAR(50)
);

CREATE TABLE permissions (
                             id BIGSERIAL PRIMARY KEY,
                             name VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE group_permissions (
                                   group_id BIGINT,
                                   permission_id BIGINT,
                                   PRIMARY KEY (group_id, permission_id),
                                   FOREIGN KEY (group_id) REFERENCES groups(id),
                                   FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE user_groups (
                             user_id BIGINT,
                             group_id BIGINT,
                             PRIMARY KEY (user_id, group_id),
                             FOREIGN KEY (user_id) REFERENCES users(id),
                             FOREIGN KEY (group_id) REFERENCES groups(id)
);

CREATE TABLE authors (
                         id BIGSERIAL PRIMARY KEY,
                         name VARCHAR(255) NOT NULL,
                         nationality VARCHAR(255) NOT NULL
);

CREATE TABLE books (
                       id BIGSERIAL PRIMARY KEY,
                       isbn VARCHAR(13) UNIQUE NOT NULL,
                       title VARCHAR(150) NOT NULL,
                       genre VARCHAR(50) NOT NULL,
                       total_quantity SMALLINT NOT NULL,
                       available_quantity SMALLINT NOT NULL,
                       author_id BIGINT NOT NULL,
                       FOREIGN KEY (author_id) REFERENCES authors(id)
);

CREATE TABLE loans (
                       id BIGSERIAL PRIMARY KEY,
                       status VARCHAR(20) NOT NULL,
                       request_date TIMESTAMP NOT NULL,
                       approval_date TIMESTAMP,
                       withdrawable_date TIMESTAMP,
                       due_date TIMESTAMP,
                       return_date TIMESTAMP,
                       book_id BIGINT NOT NULL,
                       user_id BIGINT NOT NULL,
                       FOREIGN KEY (book_id) REFERENCES books(id),
                       FOREIGN KEY (user_id) REFERENCES users(id)
);
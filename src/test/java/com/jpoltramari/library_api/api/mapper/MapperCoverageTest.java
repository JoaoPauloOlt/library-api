package com.jpoltramari.library_api.api.mapper;

import com.jpoltramari.library_api.api.dto.author.AuthorInput;
import com.jpoltramari.library_api.api.dto.book.BookInput;
import com.jpoltramari.library_api.api.dto.book.BookUpdateInput;
import com.jpoltramari.library_api.api.dto.bookcopy.BookCopyModel;
import com.jpoltramari.library_api.api.dto.loan.LoanModel;
import com.jpoltramari.library_api.api.dto.user.UserInput;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.enums.Genre;
import com.jpoltramari.library_api.domain.enums.LoanStatus;
import com.jpoltramari.library_api.domain.enums.UserStatus;
import com.jpoltramari.library_api.domain.model.Author;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.model.Group;
import com.jpoltramari.library_api.domain.model.Loan;
import com.jpoltramari.library_api.domain.model.User;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MapperCoverageTest {

    private final AuthorMapper authorMapper = Mappers.getMapper(AuthorMapper.class);
    private final BookMapper bookMapper = Mappers.getMapper(BookMapper.class);
    {
        ReflectionTestUtils.setField(bookMapper, "authorMapper", authorMapper);
    }
    private final BookCopyMapper copyMapper = Mappers.getMapper(BookCopyMapper.class);
    private final LoanMapper loanMapper = Mappers.getMapper(LoanMapper.class);
    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void shouldMapAuthorAndIgnoreManagedFields() {
        Author author = new Author();
        author.setId(7L);
        author.setName("George Orwell");
        author.setNationality("British");

        var model = authorMapper.toModel(author);
        assertThat(model.id()).isEqualTo(7L);
        assertThat(model.name()).isEqualTo("George Orwell");
        assertThat(model.nationality()).isEqualTo("British");

        Author created = authorMapper.toEntity(new AuthorInput("Aldous Huxley", "British"));
        assertThat(created.getId()).isNull();
        assertThat(created.getName()).isEqualTo("Aldous Huxley");
        assertThat(created.getNationality()).isEqualTo("British");
        assertThat(created.getBooks()).isEmpty();

        authorMapper.update(
                new com.jpoltramari.library_api.api.dto.author.AuthorUpdateInput("Eric Blair", null),
                created
        );
        assertThat(created.getName()).isEqualTo("Eric Blair");
        assertThat(created.getNationality()).isEqualTo("British");
    }

    @Test
    void shouldMapBookAndCalculateCopyMetrics() {
        Book book = new Book();
        book.setId(1L);
        book.setIsbn("9780451524935");
        book.setTitle("1984");
        book.setGenre(Genre.CLASSIC);
        book.setDescription("Dystopian novel");
        book.setCoverUrl("https://example.com/1984.jpg");
        book.setLoanCount(12L);

        Author author = new Author();
        author.setId(2L);
        author.setName("George Orwell");
        author.setNationality("British");
        book.setAuthors(new HashSet<>(Set.of(author)));

        BookCopy available = copy(1L, book, CopyStatus.AVAILABLE, true);
        BookCopy loaned = copy(2L, book, CopyStatus.LOANED, true);
        BookCopy inactiveAvailable = copy(3L, book, CopyStatus.AVAILABLE, false);
        book.setCopies(new HashSet<>(Set.of(available, loaned, inactiveAvailable)));

        var model = bookMapper.toModel(book);
        assertThat(model.id()).isEqualTo(1L);
        assertThat(model.isbn()).isEqualTo("9780451524935");
        assertThat(model.title()).isEqualTo("1984");
        assertThat(model.genre()).isEqualTo("CLASSIC");
        assertThat(model.description()).isEqualTo("Dystopian novel");
        assertThat(model.coverUrl()).isEqualTo("https://example.com/1984.jpg");
        assertThat(model.totalCopies()).isEqualTo(3L);
        assertThat(model.availableCopies()).isEqualTo(1L);
        assertThat(model.loanCount()).isEqualTo(12L);
        assertThat(model.authors()).hasSize(1);

        Book created = bookMapper.toEntity(
                new BookInput(
                        "9780140449136",
                        "The Odyssey",
                        Genre.ADVENTURE,
                        "https://example.com/odyssey.jpg",
                        2,
                        List.of(2L)
                )
        );
        assertThat(created.getId()).isNull();
        assertThat(created.getTitle()).isEqualTo("The Odyssey");
        assertThat(created.getGenre()).isEqualTo(Genre.ADVENTURE);
        assertThat(created.getAuthors()).isEmpty();
        assertThat(created.getCopies()).isEmpty();
        assertThat(created.getLoanCount()).isNull();

        bookMapper.update(
                new BookUpdateInput(
                        null,
                        "Nineteen Eighty-Four",
                        null,
                        null,
                        null,
                        null
                ),
                created
        );
        assertThat(created.getTitle()).isEqualTo("Nineteen Eighty-Four");
        assertThat(created.getGenre()).isEqualTo(Genre.ADVENTURE);
    }

    @Test
    void shouldMapBookCopyWithNestedBookData() {
        Book book = new Book();
        book.setId(10L);
        book.setTitle("1984");

        BookCopy copy = copy(20L, book, CopyStatus.AVAILABLE, true);
        copy.setBarcode("BC-1984-001");
        copy.setLocation("Shelf A-01");

        BookCopyModel model = copyMapper.toModel(copy);
        assertThat(model.id()).isEqualTo(20L);
        assertThat(model.barcode()).isEqualTo("BC-1984-001");
        assertThat(model.status()).isEqualTo("AVAILABLE");
        assertThat(model.location()).isEqualTo("Shelf A-01");
        assertThat(model.active()).isTrue();
        assertThat(model.bookId()).isEqualTo(10L);
        assertThat(model.bookTitle()).isEqualTo("1984");
    }

    @Test
    void shouldMapLoanWithCompleteNestedData() {
        Author author = new Author();
        author.setName("George Orwell");

        Book book = new Book();
        book.setTitle("1984");
        book.setCoverUrl("https://example.com/1984.jpg");
        book.setAuthors(new HashSet<>(Set.of(author)));

        BookCopy copy = copy(20L, book, CopyStatus.LOANED, true);

        User user = new User();
        user.setName("Library User");

        Loan loan = new Loan();
        loan.setId(30L);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setRequestDate(LocalDateTime.now().minusDays(2));
        loan.setApprovalDate(LocalDateTime.now().minusDays(1));
        loan.setWithdrawableDate(LocalDateTime.now().minusHours(20));
        loan.setDueDate(LocalDateTime.now().plusDays(5));
        loan.setReturnDate(null);
        loan.setBookCopy(copy);
        loan.setUser(user);

        LoanModel model = loanMapper.toModel(loan);
        assertThat(model.id()).isEqualTo(30L);
        assertThat(model.status()).isEqualTo("ACTIVE");
        assertThat(model.bookTitle()).isEqualTo("1984");
        assertThat(model.bookCoverUrl()).isEqualTo("https://example.com/1984.jpg");
        assertThat(model.bookAuthors()).containsExactly("George Orwell");
        assertThat(model.userName()).isEqualTo("Library User");
        assertThat(model.withdrawDate()).isEqualTo(loan.getWithdrawableDate());

        Loan empty = new Loan();
        empty.setStatus(null);
        empty.setBookCopy(null);
        empty.setUser(null);
        LoanModel emptyModel = loanMapper.toModel(empty);
        assertThat(emptyModel.status()).isNull();
        assertThat(emptyModel.bookTitle()).isNull();
        assertThat(emptyModel.bookCoverUrl()).isNull();
        assertThat(emptyModel.bookAuthors()).isEmpty();
        assertThat(emptyModel.userName()).isNull();

        BookCopy withoutBook = new BookCopy();
        empty.setBookCopy(withoutBook);
        emptyModel = loanMapper.toModel(empty);
        assertThat(emptyModel.bookTitle()).isNull();
        assertThat(emptyModel.bookCoverUrl()).isNull();
        assertThat(emptyModel.bookAuthors()).isEmpty();
    }

    @Test
    void shouldMapUserGroupsAndUpdatesWithoutTouchingProtectedFields() {
        User user = new User();
        user.setId(1L);
        user.setName("João");
        user.setEmail("joao@example.com");
        user.setTelephone("11999999999");
        user.setPassword("encoded");
        user.setStatus(UserStatus.ACTIVE);
        user.setGroups(new HashSet<>());
        user.getGroups().add(group("USER"));
        user.getGroups().add(group("LIBRARIAN"));

        var model = userMapper.toModel(user);
        assertThat(model.id()).isEqualTo(1L);
        assertThat(model.name()).isEqualTo("João");
        assertThat(model.email()).isEqualTo("joao@example.com");
        assertThat(model.telephone()).isEqualTo("11999999999");
        assertThat(model.status()).isEqualTo("ACTIVE");
        assertThat(model.groups()).containsExactlyInAnyOrder("USER", "LIBRARIAN");

        user.setGroups(null);
        assertThat(userMapper.toModel(user).groups()).isEmpty();

        User created = userMapper.toEntity(
                new UserInput("New User", "new@example.com", "11988888888", "StrongPassword123!")
        );
        assertThat(created.getId()).isNull();
        assertThat(created.getName()).isEqualTo("New User");
        assertThat(created.getEmail()).isEqualTo("new@example.com");
        assertThat(created.getTelephone()).isEqualTo("11988888888");
        assertThat(created.getPassword()).isEqualTo("StrongPassword123!");
        assertThat(created.getStatus()).isNull();
        assertThat(created.getGroups()).isEmpty();

        created.setPassword("old-password");
        created.setStatus(UserStatus.ACTIVE);
        userMapper.update(
                new UserInput("Updated User", null, null, null),
                created
        );
        assertThat(created.getName()).isEqualTo("Updated User");
        assertThat(created.getPassword()).isEqualTo("old-password");
        assertThat(created.getStatus()).isEqualTo(UserStatus.ACTIVE);

        assertThat(userMapper.mapStatus(UserStatus.BLOCKED)).isEqualTo("BLOCKED");
        assertThat(userMapper.mapStatus(null)).isNull();
    }

    private BookCopy copy(Long id, Book book, CopyStatus status, boolean active) {
        BookCopy copy = new BookCopy();
        copy.setId(id);
        copy.setBook(book);
        copy.setStatus(status);
        copy.setActive(active);
        return copy;
    }

    private Group group(String name) {
        Group group = new Group();
        group.setName(name);
        return group;
    }
}

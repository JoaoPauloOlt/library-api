package com.jpoltramari.library_api.application.service;

import com.jpoltramari.library_api.application.command.bookcopy.CreateBookCopyCommand;
import com.jpoltramari.library_api.application.command.bookcopy.UpdateBookCopyCommand;
import com.jpoltramari.library_api.domain.enums.CopyStatus;
import com.jpoltramari.library_api.domain.exception.BookNotFoundException;
import com.jpoltramari.library_api.domain.exception.EntityNotFoundException;
import com.jpoltramari.library_api.domain.model.Book;
import com.jpoltramari.library_api.domain.model.BookCopy;
import com.jpoltramari.library_api.domain.repository.BookCopyRepository;
import com.jpoltramari.library_api.domain.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookCopyService {

    private final BookRepository bookRepository;
    private final BookCopyRepository repository;

    public List<BookCopy> findAllByBook(Long bookId){
        validateBookExists(bookId);
        return repository.findAllByBookId(bookId);
    }

    public BookCopy findOrFail(Long id){
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Book copy not found."));
    }

    @Transactional
    public BookCopy create(Long bookId, CreateBookCopyCommand command){
        if (!bookId.equals(command.bookId())) {
            throw new com.jpoltramari.library_api.domain.exception.BusinessException(
                    "Book ID in path does not match book ID in request.");
        }

        Book book = bookRepository.findById(command.bookId())
                .orElseThrow(() -> new BookNotFoundException(command.bookId()));

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setLocation(command.location());
        copy.setStatus(CopyStatus.AVAILABLE);
        copy.setActive(true);
        copy.setBarcode(generateBarcode());

        return repository.save(copy);
    }

    @Transactional
    public BookCopy update(Long bookId, Long id, UpdateBookCopyCommand command) {
        BookCopy copy = findOrFailForBook(bookId, id);

        if (command.status() != null) {
            copy.setStatus(command.status());
        }
        if (command.location() != null) {
            copy.setLocation(command.location());
        }
        if (command.active() != null) {
            copy.setActive(command.active());
        }

        return repository.save(copy);
    }

    @Transactional
    public void delete(Long bookId, Long id) {
        BookCopy copy = findOrFailForBook(bookId, id);
        repository.delete(copy);
    }

    @Transactional
    public BookCopy changeStatus(Long bookId, Long id, CopyStatus status) {
        BookCopy copy = findOrFailForBook(bookId, id);
        copy.setStatus(status);
        return repository.save(copy);
    }

    public long totalQuantity(Long bookId) {
        validateBookExists(bookId);
        return repository.countByBookId(bookId);
    }

    public long availableQuantity(Long bookId) {
        validateBookExists(bookId);
        return repository.countByBookIdAndStatus(bookId, CopyStatus.AVAILABLE);
    }

    public BookCopy findOrFailForBook(Long bookId, Long id) {
        validateBookExists(bookId);
        return repository.findByIdAndBookId(id, bookId)
                .orElseThrow(() -> new EntityNotFoundException("Book copy not found."));
    }

    private void validateBookExists(Long bookId){
        if (!bookRepository.existsById(bookId)){
            throw new BookNotFoundException(bookId);
        }
    }

    private String generateBarcode(){
        return "BK-" + UUID.randomUUID();
    }
}

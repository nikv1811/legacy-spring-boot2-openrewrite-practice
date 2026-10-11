package com.nikhil.legacy.repository;

import com.nikhil.legacy.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BookRepository extends JpaRepository<Book, Long> {
}

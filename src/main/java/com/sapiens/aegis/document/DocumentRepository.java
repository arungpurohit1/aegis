package com.sapiens.aegis.document;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Persistence layer for documents. save / findById / existsById / deleteById
 * come from Spring Data itself — Hibernate implements this interface at
 * startup, backed by whatever datasource the active profile configures.
 *
 * JpaRepository.findAll(Pageable) (inherited, no override needed) replaces
 * the old findAllByOrderByCreatedAtDescIdAsc derived query: a Pageable
 * already carries page/size/sort, so the caller decides the ordering
 * instead of it being baked into the method name.
 *
 * findByTitleContainingIgnoreCase is a derived query too: Spring Data parses
 * the method name into "WHERE lower(title) LIKE lower('%<term>%')", still
 * honoring whatever Pageable is passed.
 */
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    Page<Document> findByTitleContainingIgnoreCase(String title, Pageable pageable);
}

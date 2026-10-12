package com.sapiens.aegis.document;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity: a knowledge article. This is the object we store, and the one later
 * milestones will chunk, embed (pgvector) and search.
 *
 * Mapped to a database table (H2 today, PostgreSQL once one is available —
 * see application-dev.properties). The id is application-assigned (no
 * @GeneratedValue): Document.create() already hands out a UUID, so Hibernate
 * is told "insert exactly this identity", not "generate one for me".
 */
@Entity
@Table(name = "documents")
public class Document {

    /** Titles longer than this are rejected (mirrored by @Size on the DTOs). */
    public static final int MAX_TITLE_LENGTH = 200;

    @Id
    private UUID id;

    @Column(nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @Column(columnDefinition = "text")
    private String content;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Document() {
        // required by JPA/Hibernate to build proxies and rehydrate rows; not for application code
    }

    public Document(UUID id, String title, String content, Instant createdAt, Instant updatedAt) {
        this.id = Objects.requireNonNull(id, "id");
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    /** Factory for a brand-new document: assigns a fresh identity and timestamps. */
    public static Document create(String title, String content) {
        Instant now = Instant.now();
        return new Document(UUID.randomUUID(), title, content, now, now);
    }

    /**
     * Applies an edit. Kept as a method (rather than public setters) so every
     * update bumps updatedAt and no caller can forget to.
     */
    public void update(String title, String content) {
        this.title = title;
        this.content = content;
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    /** Entities are equal by identity, not by attribute values. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document other)) {
            return false;
        }
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }

    @Override
    public String toString() {
        return "Document{id=" + id + ", title='" + title + "'}";
    }
}

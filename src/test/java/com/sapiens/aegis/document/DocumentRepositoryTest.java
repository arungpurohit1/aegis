package com.sapiens.aegis.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * @DataJpaTest boots only the JPA slice (repository + entity manager, no
 * web layer) against an in-memory H2 database, and wraps each test in a
 * transaction that's rolled back afterwards — so tests never see each
 * other's data despite sharing one Spring context.
 */
@DataJpaTest
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository repository;

    private Document saveWithCreatedAt(String title, Instant createdAt) {
        return repository.save(
            new Document(UUID.randomUUID(), title, "body", createdAt, createdAt));
    }

    @Test
    void findAllOrdersByRequestedSort() {
        Instant now = Instant.now();
        Document older = saveWithCreatedAt("older", now.minus(2, ChronoUnit.MINUTES));
        Document newer = saveWithCreatedAt("newer", now);

        Sort newestFirst = Sort.by(Sort.Direction.DESC, "createdAt");
        assertThat(repository.findAll(PageRequest.of(0, 10, newestFirst)).getContent())
            .extracting(Document::getId)
            .containsExactly(newer.getId(), older.getId());
    }

    @Test
    void findAllPagesResults() {
        for (int i = 0; i < 3; i++) {
            saveWithCreatedAt("doc" + i, Instant.now().minusSeconds(i));
        }

        assertThat(repository.findAll(PageRequest.of(0, 2)).getContent()).hasSize(2);
        assertThat(repository.findAll(PageRequest.of(0, 2)).getTotalElements()).isEqualTo(3);
    }

    @Test
    void saveOverwritesSameId() {
        Document doc = repository.save(Document.create("title", "body"));

        doc.update("edited", "edited body");
        repository.save(doc);

        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(doc.getId()))
            .get()
            .extracting(Document::getTitle)
            .isEqualTo("edited");
    }

    @Test
    void findByIdIsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void existsByIdReflectsDeletion() {
        Document doc = repository.save(Document.create("title", "body"));

        assertThat(repository.existsById(doc.getId())).isTrue();

        repository.deleteById(doc.getId());

        assertThat(repository.existsById(doc.getId())).isFalse();
    }
}

package com.sapiens.aegis.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * DocumentRepository is now a Spring Data interface, not a class we can
 * `new` up, so the service is tested against a Mockito mock instead of a
 * real in-memory repository. That isolates DocumentService's business rules
 * (404-on-missing, delegating to the paged query) from JPA/H2 entirely —
 * that coverage lives in DocumentRepositoryTest.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository repository;

    private DocumentService service;

    @BeforeEach
    void setUp() {
        service = new DocumentService(repository);
    }

    @Test
    void createSavesAndReturnsTheDocument() {
        when(repository.save(any(Document.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Document created = service.create("Title", "Body");

        assertThat(created.getTitle()).isEqualTo("Title");
        assertThat(created.getContent()).isEqualTo("Body");
        verify(repository).save(any(Document.class));
    }

    @Test
    void getByIdReturnsWhatTheRepositoryHas() {
        Document doc = Document.create("Title", "Body");
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));

        assertThat(service.getById(doc.getId())).isEqualTo(doc);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getById(id))
            .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void listAllDelegatesToTheRepositoryWhenNoQuery() {
        Document doc = Document.create("Title", "Body");
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(doc), pageable, 1));

        assertThat(service.listAll(null, pageable).getContent()).containsExactly(doc);
    }

    @Test
    void listAllIsEmptyWhenTheRepositoryIs() {
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        assertThat(service.listAll(null, pageable)).isEmpty();
    }

    @Test
    void listAllUsesTitleSearchWhenQueryGiven() {
        Document doc = Document.create("Matching Title", "Body");
        Pageable pageable = PageRequest.of(0, 20);
        when(repository.findByTitleContainingIgnoreCase("match", pageable))
            .thenReturn(new PageImpl<>(List.of(doc), pageable, 1));

        assertThat(service.listAll("match", pageable).getContent()).containsExactly(doc);
    }

    @Test
    void updateAppliesEditsAndSaves() {
        Document doc = Document.create("old", "old body");
        when(repository.findById(doc.getId())).thenReturn(Optional.of(doc));
        when(repository.save(doc)).thenReturn(doc);

        Document updated = service.update(doc.getId(), "new", "new body");

        assertThat(updated.getId()).isEqualTo(doc.getId());
        assertThat(updated.getTitle()).isEqualTo("new");
        assertThat(updated.getContent()).isEqualTo("new body");
    }

    @Test
    void updateThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, "t", "c"))
            .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void deleteRemovesWhenPresent() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(true);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void deleteThrowsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> service.delete(id))
            .isInstanceOf(DocumentNotFoundException.class);
        verify(repository, never()).deleteById(any());
    }
}

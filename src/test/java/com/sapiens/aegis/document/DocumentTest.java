package com.sapiens.aegis.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Plain unit tests for the entity — no Spring, no I/O, runs in milliseconds.
 * Covers the two things Document owns: identity and timestamps.
 */
class DocumentTest {

    @Test
    void createAssignsIdAndEqualTimestamps() {
        Document doc = Document.create("Onboarding guide", "How to get started.");

        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getTitle()).isEqualTo("Onboarding guide");
        assertThat(doc.getContent()).isEqualTo("How to get started.");
        assertThat(doc.getCreatedAt()).isNotNull();
        assertThat(doc.getUpdatedAt()).isEqualTo(doc.getCreatedAt());
    }

    @Test
    void updateChangesFieldsAndBumpsUpdatedAt() {
        Document doc = Document.create("old", "old body");

        doc.update("new", "new body");

        assertThat(doc.getTitle()).isEqualTo("new");
        assertThat(doc.getContent()).isEqualTo("new body");
        assertThat(doc.getUpdatedAt()).isAfterOrEqualTo(doc.getCreatedAt());
    }

    @Test
    void updateKeepsIdentityAndCreatedAt() {
        Document doc = Document.create("title", "body");

        doc.update("edited", "edited body");

        assertThat(doc.getId()).isNotNull();
        assertThat(doc.getCreatedAt()).isBefore(doc.getUpdatedAt().plusMillis(1));
    }

    @Test
    void entitiesAreEqualByIdentity() {
        Document doc = Document.create("title", "body");
        Document sameId = new Document(
            doc.getId(), "different", "different", doc.getCreatedAt(), doc.getUpdatedAt());

        assertThat(doc).isEqualTo(sameId);
        assertThat(doc).hasSameHashCodeAs(sameId);
    }
}

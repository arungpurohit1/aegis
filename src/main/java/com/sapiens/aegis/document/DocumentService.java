package com.sapiens.aegis.document;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business logic for documents. The controller handles HTTP, the repository
 * handles storage, and everything in between happens here — including deciding
 * that a missing id is an error (DocumentNotFoundException) rather than an empty
 * Optional the web layer has to interpret.
 *
 * @Transactional on the write methods gives each use case one database
 * transaction instead of one per repository call — update() in particular
 * does a read then a write, and those should commit (or fail) together.
 */
@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository repository;

    public DocumentService(DocumentRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public Document create(String title, String content) {
        Document saved = repository.save(Document.create(title, content));
        log.info("Created document id={} title='{}'", saved.getId(), saved.getTitle());
        return saved;
    }

    public Document getById(UUID id) {
        return repository.findById(id)
            .orElseThrow(() -> new DocumentNotFoundException(id));
    }

    /** query is an optional title filter; blank/null means "no filter". */
    public Page<Document> listAll(String query, Pageable pageable) {
        Page<Document> documents = (query == null || query.isBlank())
            ? repository.findAll(pageable)
            : repository.findByTitleContainingIgnoreCase(query, pageable);
        log.debug("Listed {} of {} document(s), page {}", documents.getNumberOfElements(),
            documents.getTotalElements(), documents.getNumber());
        return documents;
    }

    @Transactional
    public Document update(UUID id, String title, String content) {
        Document document = getById(id);      // 404s if the id is unknown
        document.update(title, content);      // entity bumps updatedAt
        Document saved = repository.save(document);
        log.info("Updated document id={}", saved.getId());
        return saved;
    }

    @Transactional
    public void delete(UUID id) {
        // JpaRepository.deleteById() is void, not boolean like the old in-memory
        // version — so the missing-id check has to happen ourselves, first.
        if (!repository.existsById(id)) {
            throw new DocumentNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("Deleted document id={}", id);
    }
}

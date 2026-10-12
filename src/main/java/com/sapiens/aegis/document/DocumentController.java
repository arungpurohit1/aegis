package com.sapiens.aegis.document;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.SortDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sapiens.aegis.document.dto.CreateDocumentRequest;
import com.sapiens.aegis.document.dto.DocumentResponse;
import com.sapiens.aegis.document.dto.PageResponse;
import com.sapiens.aegis.document.dto.UpdateDocumentRequest;

import jakarta.validation.Valid;

/**
 * Web layer for documents: maps HTTP to the service and DTOs to/from entities.
 * It holds no business logic and no try/catch — errors are handled centrally by
 * ApiExceptionHandler.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

    private final DocumentService service;

    public DocumentController(DocumentService service) {
        this.service = service;
    }

    /** 201 Created + a Location header pointing at the new resource. */
    @PostMapping
    public ResponseEntity<DocumentResponse> create(@Valid @RequestBody CreateDocumentRequest request) {
        Document document = service.create(request.title(), request.content());
        URI location = URI.create("/api/v1/documents/" + document.getId());
        return ResponseEntity.created(location).body(DocumentResponse.from(document));
    }

    /**
     * page/size/sort come from query params, e.g. ?page=0&size=20&sort=title,asc.
     * The defaults below (size 20, sorted newest-first) apply when a caller
     * omits them, matching the un-paginated endpoint's old behavior. q is an
     * optional case-insensitive title filter, still paginated.
     */
    @GetMapping
    public PageResponse<DocumentResponse> list(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20)
            @SortDefault.SortDefaults({
                @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
                @SortDefault(sort = "id", direction = Sort.Direction.ASC)
            })
            Pageable pageable) {
        return PageResponse.from(service.listAll(q, pageable).map(DocumentResponse::from));
    }

    @GetMapping("/{id}")
    public DocumentResponse getById(@PathVariable UUID id) {
        return DocumentResponse.from(service.getById(id));
    }

    @PutMapping("/{id}")
    public DocumentResponse update(@PathVariable UUID id,
                                   @Valid @RequestBody UpdateDocumentRequest request) {
        return DocumentResponse.from(service.update(id, request.title(), request.content()));
    }

    /** 204 No Content — success, and nothing left to return. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}

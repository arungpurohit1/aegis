package com.sapiens.aegis.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Web-layer tests: real HTTP semantics (status codes, headers, JSON bodies) driven
 * through MockMvc, without starting a network server. This is the layer the M3
 * tests never covered — status codes and validation only exist here.
 *
 * The in-memory repository is shared across tests in this context, so assertions
 * are written to be independent of what other tests created.
 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser // these tests are about documents, not auth; see JwtAuthenticationTest
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper json;

    private static final String CREATE_BODY = """
        {"title":"Onboarding guide","content":"How to get started."}
        """;

    /** POSTs a document and returns its generated id. */
    private String createDocument(String title, String content) throws Exception {
        String body = json.writeValueAsString(Map.of("title", title, "content", content));
        MvcResult result = mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isCreated())
            .andReturn();
        return json.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void createReturns201WithLocationAndBody() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
            .andExpect(status().isCreated())
            .andExpect(header().exists("Location"))
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.title").value("Onboarding guide"))
            .andExpect(jsonPath("$.createdAt").isNotEmpty());
    }

    @Test
    void createEchoesCallerCorrelationId() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/documents")
                .header("X-Correlation-Id", "caller-supplied-id")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
            .andExpect(status().isCreated())
            .andExpect(header().string("X-Correlation-Id", "caller-supplied-id"))
            .andReturn();

        // and when the caller sends none, we generate one
        assertThat(result.getResponse().getHeader("X-Correlation-Id")).isNotBlank();
        mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(CREATE_BODY))
            .andExpect(header().exists("X-Correlation-Id"));
    }

    @Test
    void createRejectsBlankFieldsWith400AndFieldErrors() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"  ","content":""}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.fieldErrors.title").value("title must not be blank"))
            .andExpect(jsonPath("$.fieldErrors.content").value("content must not be blank"))
            .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    @Test
    void createRejectsOverlongTitle() throws Exception {
        String longTitle = "x".repeat(Document.MAX_TITLE_LENGTH + 1);

        mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                    "title", longTitle, "content", "body"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").value("title must be at most 200 characters"));
    }

    @Test
    void getByIdReturnsDocument() throws Exception {
        String id = createDocument("Fetch me", "body");

        mockMvc.perform(get("/api/v1/documents/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("Fetch me"));
    }

    @Test
    void getByUnknownIdReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/documents/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value(containsString("not found")))
            .andExpect(jsonPath("$.correlationId").isNotEmpty());
    }

    /** A malformed UUID is the client's mistake — 400, not a 500. */
    @Test
    void getByMalformedIdReturns400() throws Exception {
        mockMvc.perform(get("/api/v1/documents/not-a-uuid"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void listContainsCreatedDocument() throws Exception {
        String id = createDocument("Listed doc", "body");

        mockMvc.perform(get("/api/v1/documents"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[?(@.id == '" + id + "')]").exists())
            .andExpect(jsonPath("$.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    void listRespectsPageSize() throws Exception {
        createDocument("doc a", "body");
        createDocument("doc b", "body");

        mockMvc.perform(get("/api/v1/documents").param("page", "0").param("size", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.size").value(1))
            .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void listFiltersByTitleCaseInsensitively() throws Exception {
        // A unique-per-run token, not "Policy FAQ": the DB isn't reset between
        // test runs (no @Transactional/@DataJpaTest here), so a fixed title
        // would collide with rows earlier runs already left behind.
        String token = "Zx" + UUID.randomUUID();
        String matchingId = createDocument(token + " FAQ", "body");
        createDocument("Unrelated Doc", "body");

        mockMvc.perform(get("/api/v1/documents").param("q", token.toLowerCase()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()").value(1))
            .andExpect(jsonPath("$.content[0].id").value(matchingId));
    }

    @Test
    void updateReplacesFields() throws Exception {
        String id = createDocument("before", "before body");

        mockMvc.perform(put("/api/v1/documents/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"after","content":"after body"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(id))
            .andExpect(jsonPath("$.title").value("after"))
            .andExpect(jsonPath("$.content").value("after body"));

        mockMvc.perform(get("/api/v1/documents/{id}", id))
            .andExpect(jsonPath("$.title").value("after"));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        mockMvc.perform(put("/api/v1/documents/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"t","content":"c"}
                    """))
            .andExpect(status().isNotFound());
    }

    @Test
    void updateValidatesBody() throws Exception {
        String id = createDocument("valid", "body");

        mockMvc.perform(put("/api/v1/documents/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"title":"","content":"body"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.fieldErrors.title").isNotEmpty());
    }

    @Test
    void deleteReturns204ThenGetReturns404() throws Exception {
        String id = createDocument("delete me", "body");

        mockMvc.perform(delete("/api/v1/documents/{id}", id))
            .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/documents/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        mockMvc.perform(delete("/api/v1/documents/{id}", UUID.randomUUID()))
            .andExpect(status().isNotFound());
    }

    @Test
    void malformedJsonReturns400NotA500() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void missingBodyReturns400NotA500() throws Exception {
        mockMvc.perform(post("/api/v1/documents").contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }

    @Test
    void wrongContentTypeReturns415NotA500() throws Exception {
        mockMvc.perform(post("/api/v1/documents")
                .contentType(MediaType.TEXT_PLAIN)
                .content(CREATE_BODY))
            .andExpect(status().isUnsupportedMediaType());
    }
}

package com.example.chatbot;

import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseCreateParams;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    // System prompt kept generic on purpose - this is a general-purpose
    // conversational assistant, not a specialized/domain agent.
    private static final String SYSTEM_PROMPT =
        "You are a helpful, friendly general-purpose assistant. Answer clearly and concisely.";

    private final OpenAIClient openAIClient;
    private final String deploymentName;

    public ChatController(OpenAIClient openAIClient,
                           @Value("${azure.openai.deployment-name}") String deploymentName) {
        this.openAIClient = openAIClient;
        this.deploymentName = deploymentName;
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
        // LOGGING CONTROL: we log that a request occurred and its length,
        // never the message content itself, since chat content could
        // contain anything the user types (potential PII).
        log.info("Chat request received, length={}", request.message().length());

        try {
            ResponseCreateParams params = ResponseCreateParams.builder()
                .model(deploymentName)
                .instructions(SYSTEM_PROMPT)
                .input(request.message())
                .build();

            String reply = openAIClient.responses().create(params).output().stream()
                .flatMap(item -> item.message().stream())
                .flatMap(message -> message.content().stream())
                .flatMap(content -> content.outputText().stream())
                .map(outputText -> outputText.text())
                .findFirst()
                .orElse("");

            return ResponseEntity.ok(new ChatResponse(reply));
        } catch (Exception e) {
            // ERROR HANDLING CONTROL: log the real exception server-side for
            // diagnosis, but never leak stack traces, provider error bodies,
            // or internal details to the client.
            log.error("Chat completion failed", e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(new ChatResponse("Sorry, something went wrong talking to the model. Please try again."));
        }
    }
}

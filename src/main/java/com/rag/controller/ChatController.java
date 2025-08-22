package com.rag.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

import org.springframework.web.bind.annotation.RequestBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final OpenAiChatModel openAIChatModel;
    // private final OllamaChatModel ollamaChatModel;

    private final VectorStore vectorStore;
    private final ChatMemory chatMemory;

    public ChatController(OpenAiChatModel openAIChatModel, VectorStore vectorStore, ChatMemory chatMemory) {
        if (openAIChatModel == null || vectorStore == null || chatMemory == null) {
            throw new IllegalArgumentException("Dependencies cannot be null");
        }
        this.openAIChatModel = openAIChatModel;
        this.vectorStore = vectorStore;
        this.chatMemory = chatMemory;
    }

    @PostMapping
    public Flux<String> chat(@RequestBody String message) {
        logger.info("Received message: {}", message);
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("Message cannot be null or empty");
        }

        return Flux.create(emitter -> {
            try {
                logger.info("Starting chat processing for message: {}", message);
                logger.info("Initializing chat model and setting up client...");
                
                ChatClient chatClient = ChatClient.builder(openAIChatModel).build();
                logger.info("Chat model initialized successfully");
                chatClient.prompt()
                        .advisors(
                            // MessageChatMemoryAdvisor.builder(chatMemory).build(),
                            QuestionAnswerAdvisor.builder(vectorStore).build()
                        )
                        .user(message)
                        .stream()
                        .content()
                        .doOnNext(response -> {
                            if (response != null) {
                                emitter.next(response);
                            } else {
                                logger.warn("LLM returned an empty response");
                                emitter.next("");
                            }
                        })
                        .doOnError(error -> {
                            logger.error("Chat stream error occurred: {}", error.getMessage(), error);
                            emitter.next("[ERROR]"); // Send error signal
                            emitter.error(error);
                        })
                        .doOnComplete(() -> {
                            logger.info("Chat stream completed successfully");
                            emitter.next("[DONE]"); // Send completion signal
                            emitter.complete();
                        })
                        .subscribe();
            } catch (Exception e) {
                logger.error("An unexpected error occurred: {}", e.getMessage(), e);
                emitter.next("[ERROR]"); // Send error signal
                emitter.error(e);
            }
        }, FluxSink.OverflowStrategy.BUFFER);
    }

}

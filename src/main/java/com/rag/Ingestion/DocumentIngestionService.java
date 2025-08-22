package com.rag.Ingestion;

import java.util.List;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionService implements CommandLineRunner {

    @Value("classpath:/pdf/Narovatar Project Report.pdf")
    private Resource resource;

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) throws Exception {
        // Read the pdf
        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        // Split the pdf contents into chunks
        TextSplitter textSplitter = new TokenTextSplitter();
        List<Document> documents = textSplitter.split(tikaDocumentReader.read());
        // Store the chunks in the vector database
        vectorStore.accept(documents);
    }
    
}

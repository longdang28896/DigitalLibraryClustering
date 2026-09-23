package com.dmml.library.service;

import com.dmml.library.model.Document;
import com.dmml.library.util.PhysicalDocumentManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class DocumentRepository {

    private static DocumentRepository instance;
    private final Map<Integer, Document> documentMap = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(1);
    private final Gson gson = new Gson();

    private DocumentRepository() {
        loadSeedData();
    }

    public static synchronized DocumentRepository getInstance() {
        if (instance == null) {
            instance = new DocumentRepository();
        }
        return instance;
    }

    private void loadSeedData() {
        // 1. Load English Newsgroup Seed Data
        loadJsonData(Paths.get("data", "20newsgroups_sample.json"), "TXT");

        // 2. Load Vietnamese Seed Academic Documents
        loadJsonData(Paths.get("data", "sample_vietnamese_docs.json"), null);

        // 3. Initialize Physical Repository on disk (generate real .pdf, .docx, .txt files)
        List<Document> toInit = documentMap.values().stream()
                .filter(d -> d.getId() >= 1000 || "PDF".equalsIgnoreCase(d.getFileFormat()) || "DOCX".equalsIgnoreCase(d.getFileFormat()))
                .collect(Collectors.toList());
        PhysicalDocumentManager.initializeRepository(toInit);

        System.out.println("Digital Library Catalog initialized with " + documentMap.size() + " documents (" + toInit.size() + " physical files).");
    }

    private void loadJsonData(Path path, String defaultFormat) {
        if (Files.exists(path)) {
            try (var reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<Document>>() {}.getType();
                List<Document> docs = gson.fromJson(reader, listType);
                if (docs != null) {
                    for (Document doc : docs) {
                        if (doc.getFileFormat() == null && defaultFormat != null) {
                            doc.setFileFormat(defaultFormat);
                        }
                        documentMap.put(doc.getId(), doc);
                        if (doc.getId() >= idGenerator.get()) {
                            idGenerator.set(doc.getId() + 1);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading " + path + ": " + e.getMessage());
            }
        }
    }

    public static final Comparator<Document> VI_FIRST_COMPARATOR = (d1, d2) -> {
        boolean v1 = "VI".equalsIgnoreCase(d1.getLanguage());
        boolean v2 = "VI".equalsIgnoreCase(d2.getLanguage());
        if (v1 && !v2) return -1; // Vietnamese documents always prioritized at the top
        if (!v1 && v2) return 1;
        return Integer.compare(d1.getId(), d2.getId());
    };

    public List<Document> findAll() {
        List<Document> list = new ArrayList<>(documentMap.values());
        list.sort(VI_FIRST_COMPARATOR);
        return list;
    }

    public Optional<Document> findById(int id) {
        return Optional.ofNullable(documentMap.get(id));
    }

    public Document save(Document document) {
        if (document.getId() <= 0) {
            document.setId(idGenerator.getAndIncrement());
        }
        if (document.getFilePath() == null || !new File(document.getFilePath()).exists()) {
            File f = PhysicalDocumentManager.generatePhysicalFile(document);
            if (f != null && f.exists()) {
                document.setFilePath(f.getAbsolutePath());
            }
        }
        documentMap.put(document.getId(), document);
        return document;
    }

    public Document update(Document doc) {
        if (doc != null && doc.getId() > 0) {
            documentMap.put(doc.getId(), doc);
            return doc;
        }
        return null;
    }

    public boolean reassignCluster(int docId, int newClusterId, String newTopic) {
        Document doc = documentMap.get(docId);
        if (doc != null) {
            doc.setClusterId(newClusterId);
            if (newTopic != null && !newTopic.trim().isEmpty()) {
                doc.setTopic(newTopic);
            }
            doc.setManuallyAssigned(true);
            return true;
        }
        return false;
    }

    public boolean deleteById(int id) {
        return documentMap.remove(id) != null;
    }

    public List<Document> findByClusterId(int clusterId) {
        return documentMap.values().stream()
                .filter(d -> d.getClusterId() == clusterId)
                .collect(Collectors.toList());
    }

    public List<Document> search(String query, Integer clusterId) {
        return search(query, clusterId, "ALL");
    }

    public List<Document> search(String query, Integer clusterId, String languageFilter) {
        String q = (query != null) ? query.toLowerCase().trim() : "";
        String lang = (languageFilter != null) ? languageFilter.toUpperCase() : "ALL";

        return documentMap.values().stream()
                .filter(d -> {
                    // Filter by cluster
                    boolean matchesCluster = (clusterId == null || clusterId < 0 || d.getClusterId() == clusterId);
                    if (!matchesCluster) return false;

                    // Filter by language
                    if (!"ALL".equals(lang)) {
                        String docLang = d.getLanguage() != null ? d.getLanguage().toUpperCase() : "EN";
                        if (!lang.equals(docLang)) return false;
                    }

                    // Filter by keyword query
                    if (q.isEmpty()) return true;
                    return (d.getTitle() != null && d.getTitle().toLowerCase().contains(q))
                            || (d.getAuthor() != null && d.getAuthor().toLowerCase().contains(q))
                            || (d.getSummary() != null && d.getSummary().toLowerCase().contains(q))
                            || (d.getTopic() != null && d.getTopic().toLowerCase().contains(q));
                })
                .sorted(VI_FIRST_COMPARATOR)
                .collect(Collectors.toList());
    }

    public int count() {
        return documentMap.size();
    }
}

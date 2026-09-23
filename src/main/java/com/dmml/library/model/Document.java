package com.dmml.library.model;

import com.dmml.library.util.DocumentFileReader;

public class Document {
    private int id;
    private String title;
    private String author;
    private String originalCategory;
    private String topic;
    private String summary;
    private String content;
    private int clusterId = -1;
    private double similarityScore; // For recommendations
    private String fileFormat = "TEXT"; // PDF, DOCX, TXT, JSON, MANUAL
    private String language = "EN"; // "VI" or "EN"
    private boolean manuallyAssigned = false; // Flag to indicate if user manually edited/assigned cluster
    private String filePath; // Optional absolute path if imported from disk

    public Document() {
    }

    public Document(int id, String title, String author, String topic, String summary, String content) {
        this.id = id;
        this.title = title;
        this.author = author;
        this.topic = topic;
        this.summary = summary;
        this.content = content;
        this.language = DocumentFileReader.detectLanguage(title + " " + content);
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getOriginalCategory() {
        return originalCategory;
    }

    public void setOriginalCategory(String originalCategory) {
        this.originalCategory = originalCategory;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        if (content != null && (language == null || language.isEmpty())) {
            this.language = DocumentFileReader.detectLanguage(content);
        }
    }

    public int getClusterId() {
        return clusterId;
    }

    public void setClusterId(int clusterId) {
        this.clusterId = clusterId;
    }

    public double getSimilarityScore() {
        return similarityScore;
    }

    public void setSimilarityScore(double similarityScore) {
        this.similarityScore = similarityScore;
    }

    public String getFileFormat() {
        return fileFormat != null ? fileFormat : "TEXT";
    }

    public void setFileFormat(String fileFormat) {
        this.fileFormat = fileFormat;
    }

    public String getLanguage() {
        if (language == null || language.isEmpty()) {
            language = DocumentFileReader.detectLanguage((title != null ? title : "") + " " + (content != null ? content : ""));
        }
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isManuallyAssigned() {
        return manuallyAssigned;
    }

    public void setManuallyAssigned(boolean manuallyAssigned) {
        this.manuallyAssigned = manuallyAssigned;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}

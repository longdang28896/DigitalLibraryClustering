package com.dmml.library.ml.nlp;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Text Preprocessor supporting both English and Vietnamese documents:
 * 1. Unicode-aware character cleaning (preserves Vietnamese accents and diacritics)
 * 2. English & Vietnamese Stopword filtering
 * 3. Conditional English Porter Stemming (does not mangle Vietnamese words)
 * 4. N-gram / Compound term enhancement for Vietnamese terms
 */
public class TextPreprocessor {

    private final Set<String> stopWords = new HashSet<>();
    private final PorterStemmer stemmer = new PorterStemmer();
    // Unicode letter pattern matching all alphabetic characters (Latin, Vietnamese diacritics, etc.)
    private static final Pattern NON_UNICODE_LETTERS = Pattern.compile("[^\\p{L}\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");

    public TextPreprocessor() {
        loadStopWords();
    }

    private void loadStopWords() {
        // 1. Load English Stopwords
        loadStopwordsFromFile(Paths.get("data", "stopwords_en.txt"));

        // 2. Load Vietnamese Stopwords
        loadStopwordsFromFile(Paths.get("data", "stopwords_vi.txt"));

        // 3. Fallback defaults if set is empty
        if (stopWords.isEmpty()) {
            String[] defaults = {
                "a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", 
                "as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", 
                "by", "can", "cannot", "could", "did", "do", "does", "doing", "down", "during", "each", 
                "few", "for", "from", "further", "had", "has", "have", "having", "he", "her", "here", 
                "hers", "herself", "him", "himself", "his", "how", "i", "if", "in", "into", "is", "it", 
                "its", "itself", "me", "more", "most", "my", "myself", "no", "nor", "not", "of", "off", 
                "on", "once", "only", "or", "other", "ought", "our", "ours", "ourselves", "out", "over", 
                "own", "same", "she", "should", "so", "some", "such", "than", "that", "the", "their", 
                "theirs", "them", "themselves", "then", "there", "these", "they", "this", "those", 
                "through", "to", "too", "under", "until", "up", "very", "was", "we", "were", "what", 
                "when", "where", "which", "while", "who", "whom", "why", "with", "would", "you", "your", 
                "yours", "yourself", "yourselves", "article", "writes", "subject", "lines", "organization",
                // Core Vietnamese fallbacks
                "và", "là", "của", "những", "các", "có", "được", "trong", "một", "cho", "này", "với", "về",
                "như", "để", "tại", "từ", "khi", "đã", "sẽ", "người", "ra", "theo", "đến", "vào", "lại", "trên"
            };
            stopWords.addAll(Arrays.asList(defaults));
        }
    }

    private void loadStopwordsFromFile(Path path) {
        if (Files.exists(path)) {
            try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String word = line.trim().toLowerCase();
                    if (!word.isEmpty() && !word.startsWith("#")) {
                        stopWords.add(word);
                    }
                }
            } catch (Exception e) {
                System.err.println("Error reading stopwords file " + path + ": " + e.getMessage());
            }
        }
    }

    /**
     * Preprocesses raw document text:
     * 1. Lowercases (Locale.ROOT / UTF-8)
     * 2. Replaces non-letter characters with space
     * 3. Tokenizes by whitespace
     * 4. Filters out stopwords and short tokens (< 2 chars for Vietnamese, < 3 for English)
     * 5. Applies Porter Stemming ONLY to pure ASCII English tokens
     * 6. Generates high-value bigrams for adjacent Vietnamese terms
     *
     * @param rawText raw document text
     * @return list of processed tokens
     */
    public List<String> preprocess(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String cleaned = NON_UNICODE_LETTERS.matcher(rawText.toLowerCase()).replaceAll(" ");
        String[] tokens = MULTI_SPACE.split(cleaned.trim());

        List<String> result = new ArrayList<>(tokens.length * 2);
        List<String> validTokensForBigram = new ArrayList<>();

        for (String token : tokens) {
            token = token.trim();
            if (token.isEmpty() || stopWords.contains(token)) {
                continue;
            }

            boolean isAscii = isPureAscii(token);
            if (isAscii) {
                if (token.length() >= 3) {
                    String stemmed = stemmer.stemWord(token);
                    if (stemmed.length() >= 3 && !stopWords.contains(stemmed)) {
                        result.add(stemmed);
                        validTokensForBigram.add(stemmed);
                    }
                }
            } else {
                // Vietnamese word (contains diacritics or non-ASCII)
                if (token.length() >= 2) {
                    result.add(token);
                    validTokensForBigram.add(token);
                }
            }
        }

        // Add adjacent bigrams for richer semantic clustering of Vietnamese compound phrases
        for (int i = 0; i < validTokensForBigram.size() - 1; i++) {
            String t1 = validTokensForBigram.get(i);
            String t2 = validTokensForBigram.get(i + 1);
            if (!isPureAscii(t1) || !isPureAscii(t2)) {
                result.add(t1 + "_" + t2);
            }
        }

        return result;
    }

    private boolean isPureAscii(String word) {
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) > 127) {
                return false;
            }
        }
        return true;
    }

    public Set<String> getStopWords() {
        return stopWords;
    }
}

package org.example.ch2;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.deeplearning4j.models.embeddings.learning.impl.elements.SkipGram;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.nd4j.common.io.ClassPathResource;

import java.nio.file.Paths;

public class Word2VecSynonyms {
    // Does not work
    public static void main(String[] args) throws Exception {
        var word2Vec = getWord2Vec("ch2/billboard_lyrics_1964-2015.csv");

        var words = new String[] {"guitar", "love", "rock"};
        for (var word : words) {
            var lst = word2Vec.wordsNearest(word, 2);
            System.out.println("2 Words closest to '" + word + "': " + lst);
        }

        var path = Paths.get("target/idx/ch2/songs_word_2_vec");
        var directory = FSDirectory.open(path);

        var indexTimeAnalyzer = getIndexTimeSynonymAnalyzer(word2Vec, 0.35);
        writeDocuments(directory, indexTimeAnalyzer);

        var searchTimeAnalyzer = getSearchTimeAnalyzer();
        search(directory, searchTimeAnalyzer);
    }

    private static Word2Vec getWord2Vec(String filename) throws Exception {
        var filePath = new ClassPathResource(filename)
                .getFile()
                .getAbsolutePath();

        var iterator = new BasicLineIterator(filePath);

        // only extract the lyrics part
        iterator.setPreProcessor((SentencePreProcessor) Word2VecSynonyms::sentencePreProcessor);
        // skip csv headers
        iterator.nextSentence();

        var word2Vec = new Word2Vec.Builder()
                .layerSize(100)
                .windowSize(5)
                .iterate(iterator)
                //.elementsLearningAlgorithm(new CBOW<>())
                .elementsLearningAlgorithm(new SkipGram<>())
                .build();
        word2Vec.fit();

        return word2Vec;
    }

    private static String sentencePreProcessor(String sentence) {
        var row = sentence.split(",");
        if (row.length < 5) return null;
        var song = row[1];
        var artist = row[2];
        var lyrics = row[4];
        var newSentence = (song + " " + artist + " " + lyrics).replace("\"", "").strip();
        if (newSentence.isBlank()) return null;
        return newSentence;
    }

    private static Analyzer getIndexTimeSynonymAnalyzer(Word2Vec word2Vec, double minAccuracy) throws Exception {
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                var tokenizer = new WhitespaceTokenizer();
                var synFilter = new Word2VecSynonymsFilter(tokenizer, word2Vec, minAccuracy);
                return new TokenStreamComponents(tokenizer, synFilter);
            }
        };
    }

    private static void writeDocuments(Directory directory, Analyzer analyzer) throws Exception {
        var aeroplaneDoc = new Document();
        var fieldStore = Field.Store.YES;
        aeroplaneDoc.add(new TextField("title", "Aeroplane", fieldStore));
        aeroplaneDoc.add(new TextField("author", "Red Hot Chili Peppers", fieldStore));
        aeroplaneDoc.add(new TextField("year", "1995", fieldStore));
        aeroplaneDoc.add(new TextField("album", "One Hot Minute", fieldStore));
        aeroplaneDoc.add(new TextField("text", "I like pleasure spiked with pain and music is my aeroplane ...", fieldStore));

        var config = new IndexWriterConfig(analyzer);
        var writer = new IndexWriter(directory, config);
        writer.addDocument(aeroplaneDoc);
        writer.commit();
    }

    private static Analyzer getSearchTimeAnalyzer() { return new WhitespaceAnalyzer(); }

    private static void search(Directory directory, Analyzer analyzer) throws Exception {
        var reader = DirectoryReader.open(directory);
        var searcher = new IndexSearcher(reader);
        var parser = new QueryParser("text", analyzer);
        var query = parser.parse("plane");
        var hits = searcher.search(query, 10);
        for (int i = 0; i < hits.scoreDocs.length; i++) {
            var scoreDoc = hits.scoreDocs[i];
            var doc = searcher.doc(scoreDoc.doc);
            System.out.println(doc.get("title") + " by " + doc.get("author"));
        }
    }
}

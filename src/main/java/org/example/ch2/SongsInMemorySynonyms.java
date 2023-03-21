package org.example.ch2;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymMap;
import org.apache.lucene.document.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.CharsRef;

import java.nio.file.Paths;

public class SongsInMemorySynonyms {
    public static void main(String[] args) throws Exception {
        var path = Paths.get("target/idx/ch2/songs");
        var directory = FSDirectory.open(path);

        var indexTimeAnalyzer = getIndexTimeSynonymAnalyzer();
        writeDocuments(directory, indexTimeAnalyzer);

        var searchTimeAnalyzer = getSearchTimeAnalyzer();
        search(directory, searchTimeAnalyzer);
    }

    private static Analyzer getIndexTimeSynonymAnalyzer() throws Exception {
        var builder = new SynonymMap.Builder();
        builder.add(new CharsRef("aeroplane"), new CharsRef("plane"), true);
        var map = builder.build();
        return new Analyzer() {
            @Override
            protected TokenStreamComponents createComponents(String fieldName) {
                var tokenizer = new WhitespaceTokenizer();
                var synFilter = new SynonymGraphFilter(tokenizer, map, true);
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

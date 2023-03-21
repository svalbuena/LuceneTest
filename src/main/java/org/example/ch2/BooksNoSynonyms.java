package org.example.ch2;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
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

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;

public class BooksNoSynonyms {
    public static void main(String[] args) throws Exception {
        var path = Paths.get("target/idx/ch2/books");
        var directory = FSDirectory.open(path);

        var analyzer = getIndexTimeAnalyzer();
        writeDocuments(directory, analyzer);

        search(directory);
    }

    private static Analyzer getIndexTimeAnalyzer() {
        var perFieldAnalyzers = new HashMap<String, Analyzer>();
        var stopWords = new CharArraySet(Arrays.asList("a", "an", "the"), true);
        perFieldAnalyzers.put("pages", new StopAnalyzer(stopWords));
        perFieldAnalyzers.put("title", new WhitespaceAnalyzer());
        return new PerFieldAnalyzerWrapper(new EnglishAnalyzer(), perFieldAnalyzers);
    }

    private static void writeDocuments(Directory directory, Analyzer analyzer) throws Exception {
        var config = new IndexWriterConfig(analyzer);
        var writer = new IndexWriter(directory, config);

        var doc1 = new Document();
        doc1.add(new TextField("title", "Deep learning for search", Field.Store.YES));
        doc1.add(new TextField("page", "Living in the information age ...", Field.Store.YES));

        var doc2 = new Document();
        doc2.add(new TextField("title", "Relevant search", Field.Store.YES));
        doc2.add(new TextField("page", "Getting a search engine to behave can be maddening ...", Field.Store.YES));

        writer.addDocument(doc1);
        writer.addDocument(doc2);

        writer.commit();
        writer.close();
    }

    private static void search(Directory directory) throws Exception {
        var parser = new QueryParser("title", new WhitespaceAnalyzer());
        var query = parser.parse("+Deep +search");

        var reader = DirectoryReader.open(directory);
        var searcher = new IndexSearcher(reader);
        var hits = searcher.search(query, 10);

        for (int i = 0; i < hits.scoreDocs.length; i++) {
            var scoreDoc = hits.scoreDocs[i];
            var doc = reader.document(scoreDoc.doc);
            System.out.println(doc.get("title") + " : " + scoreDoc.score);
        }
    }
}

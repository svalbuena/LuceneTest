package org.example.ch2;

import org.deeplearning4j.models.embeddings.learning.impl.elements.CBOW;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.BasicLineIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.nd4j.common.io.ClassPathResource;

import java.io.FileInputStream;
import java.util.stream.Stream;

public class Word2VecSynonyms {
    public static void main(String[] args) throws Exception {
        var filePath = new ClassPathResource("ch2/billboard_lyrics_1964-2015.csv")
                .getFile()
                .getAbsolutePath();

        var iterator = new BasicLineIterator(filePath);

        // only extract the lyrics part
        iterator.setPreProcessor((SentencePreProcessor) Word2VecSynonyms::sentencePreProcessor);
        // skip csv headers
        iterator.nextSentence();

        var vec = new Word2Vec.Builder()
                .layerSize(100)
                .windowSize(5)
                .iterate(iterator)
                .elementsLearningAlgorithm(new CBOW<>())
                .build();
        vec.fit();

        var words = new String[] {"guitar", "love", "rock"};
        for (var word : words) {
            var lst = vec.wordsNearest(word, 2);
            System.out.println("2 Words closest to '" + word + "': " + lst);
        }
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
}
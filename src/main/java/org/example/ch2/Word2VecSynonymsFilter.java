package org.example.ch2;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.synonym.SynonymFilter;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.CharsRef;
import org.apache.lucene.util.CharsRefBuilder;
import org.deeplearning4j.models.word2vec.Word2Vec;

public final class Word2VecSynonymsFilter extends TokenFilter {

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    private final PositionIncrementAttribute positionIncrementAttribute = addAttribute(PositionIncrementAttribute.class);

    private final Word2Vec word2Vec;
    private final double minAccuracy;
    private final List<PendingOutput> outputs = new LinkedList<>();
    private int positions = 0;

    public Word2VecSynonymsFilter(TokenStream input, Word2Vec word2Vec, double minAccuracy) {
        super(input);
        this.word2Vec = word2Vec;
        this.minAccuracy = minAccuracy;
    }

    @Override
    public boolean incrementToken() throws IOException {
        if (!outputs.isEmpty()) {
            PendingOutput output = outputs.remove(0);

            restoreState(output.state);

            termAtt.copyBuffer(output.charsRef.chars, output.charsRef.offset, output.charsRef.length);

            typeAtt.setType(SynonymFilter.TYPE_SYNONYM);
            positionIncrementAttribute.setPositionIncrement(output.positionIncrement);
            return true;
        }

        if (!SynonymFilter.TYPE_SYNONYM.equals(typeAtt.type())) {
            positions++;
            positionIncrementAttribute.setPositionIncrement(positions);
            var word = new String(termAtt.buffer()).trim();
            var list = word2Vec.similarWordsInVocabTo(word, minAccuracy);
            var i = 0;
            for (var syn : list) {
                if (i == 2) {
                    break;
                }
                if (!syn.equals(word)) {
                    var charsRefBuilder = new CharsRefBuilder();
                    var cr = charsRefBuilder.append(syn).get();

                    var state = captureState();
                    outputs.add(new PendingOutput(state, cr, positions));
                    i++;
                }
            }
        }
        return !outputs.isEmpty() || input.incrementToken();
    }

    @Override
    public void end() throws IOException {
        super.end();
        outputs.clear();
        positions = 0;
    }

    private record PendingOutput(State state, CharsRef charsRef, int positionIncrement) {}
}
package com.utoronto.ece1778.probo.Utils;

import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExtractSentences {

    private String paragraph;
    private List<String> sentences;


    public ExtractSentences(String paragraph) {
        this.paragraph = paragraph;
        this.extractSentences();
    }

    private void extractSentences() {

        BreakIterator iterator = BreakIterator.getSentenceInstance(Locale.US);
        sentences = new ArrayList<>();

        iterator.setText(paragraph);

        int lastIndex = iterator.first();

        while (lastIndex != BreakIterator.DONE) {
            int firstIndex = lastIndex;
            lastIndex = iterator.next();

            if (lastIndex != BreakIterator.DONE) {
                String sentence = paragraph.substring(firstIndex, lastIndex);

                sentences.add(sentence);

            }
        }

    }

    public String getParagraph() {
        return paragraph;
    }

    public void setParagraph(String paragraph) {
        this.paragraph = paragraph;
    }

    public void setSentences(List<String> sentences) {
        this.sentences = sentences;
    }

    public List<String> getSentences() {
        return sentences;

    }
}



package dquang.wordnet.main.entity;

import java.util.List;

public class WordForm {

    private String lemma;
    private List<String> synsets;
    private List<String> meanings;

    public String getLemma() {
        return lemma;
    }

    public WordForm setLemma(String lemma) {
        this.lemma = lemma;
        return this;
    }

    public List<String> getSynsets() {
        return synsets;
    }

    public WordForm setSynsets(List<String> synsets) {
        this.synsets = synsets;
        return this;
    }

    public List<String> getMeanings() {
        return meanings;
    }

    public WordForm setMeanings(List<String> meanings) {
        this.meanings = meanings;
        return this;
    }

    @Override
    public String toString() {
        return "WordForm{" +
                "lemma='" + lemma + '\'' +
                ", synsets=" + synsets +
                ", meanings=" + meanings +
                '}';
    }
}

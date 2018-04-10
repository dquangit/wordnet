package dquang.wordnet.main.entity;

import java.util.List;

public class SynsetMeaning {

    private Synset synset;
    private List<String> meanings;
    private String caseName;

    private SynsetMeaning() {
    }

    public Synset getSynset() {
        return synset;
    }

    private SynsetMeaning setSynset(Synset synset) {
        this.synset = synset;
        return this;
    }

    public List<String> getMeanings() {
        return meanings;
    }

    public SynsetMeaning setMeanings(List<String> meanings) {
        this.meanings = meanings;
        return this;
    }

    public String getCaseName() {
        return caseName;
    }

    public SynsetMeaning setCaseName(String caseName) {
        this.caseName = caseName;
        return this;
    }

    public static SynsetMeaning newInstance(Synset synset) {
        return new SynsetMeaning()
                .setSynset(synset);
    }

    @Override
    public String toString() {
        String synsetId = synset.getSynsetId();
        String words = listToString(synset.getWords());
        String meaning = listToString(meanings);
        String gloss = synset.getGloss();
        return synsetId + "|" + words + "|" + caseName + "|" + meaning + "|" + gloss;
    }

    private String listToString(List<String> stringList) {
        if (stringList.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringList.forEach(string -> {
            stringBuilder.append(string);
            stringBuilder.append(", ");
        });
        String result = stringBuilder.toString();
        return result.substring(0, result.lastIndexOf(","));
    }
}

package dquang.wordnet.main.out;

public class ExcelOutput {

    private String synsetId;
    private String words;
    private String caseName;
    private String meanings;
    private String gloss;

    public ExcelOutput() {
    }

    public String getSynsetId() {
        return synsetId;
    }

    public ExcelOutput setSynsetId(String synsetId) {
        this.synsetId = synsetId;
        return this;
    }

    public String getWords() {
        return words;
    }

    public ExcelOutput setWords(String words) {
        this.words = words;
        return this;
    }

    public String getCaseName() {
        return caseName;
    }

    public ExcelOutput setCaseName(String caseName) {
        this.caseName = caseName;
        return this;
    }

    public String getMeanings() {
        return meanings;
    }

    public ExcelOutput setMeanings(String meanings) {
        this.meanings = meanings;
        return this;
    }

    public String getGloss() {
        return gloss;
    }

    public ExcelOutput setGloss(String gloss) {
        this.gloss = gloss;
        return this;
    }
}

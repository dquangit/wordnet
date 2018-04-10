package dquang.wordnet.main.entity;

import java.util.List;

public class Synset {

    private String synsetId;
    private List<String> words;
    private String gloss;
    private List<String> parents;
    private List<String> children;

    public String getSynsetId() {
        return synsetId;
    }

    public Synset setSynsetId(String synsetId) {
        this.synsetId = synsetId;
        return this;
    }

    public List<String> getWords() {
        return words;
    }

    public Synset setWords(List<String> words) {
        this.words = words;
        return this;
    }

    public String getGloss() {
        return gloss;
    }

    public Synset setGloss(String gloss) {
        this.gloss = gloss;
        return this;
    }

    public boolean hasParent() {
        return (parents != null && !parents.isEmpty());
    }

    public boolean hasChild() {
        return (children != null && !children.isEmpty());
    }

    public List<String> getParents() {
        return parents;
    }

    public Synset setParents(List<String> parents) {
        this.parents = parents;
        return this;
    }

    public List<String> getChildren() {
        return children;
    }

    public Synset setChildren(List<String> children) {
        this.children = children;
        return this;
    }

    @Override
    public String toString() {
        return "Synset{" +
                "synsetId='" + synsetId + '\'' +
                ", words=" + words +
                ", gloss='" + gloss + '\'' +
                '}';
    }
}

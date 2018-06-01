package dquang.wordnet.main.data;

import dquang.wordnet.main.entity.Synset;
import dquang.wordnet.main.entity.WordForm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Stream;

public class WordnetData {

    private static WordnetData instance;

    private static final String DATA_PATH = "data/data.noun";
    private static final String INDEX_PATH = "data/index.noun";
    private static final String EV_DICT_PATH = "data/ev.txt";
    private static final String MONTHS_PATH = "data/months.txt";
    private static final String SPECIAL_PATH = "data/special.txt";
    private static final String SYNC_DICT_PATH = "data/synDict.txt";
    private Map<String, WordForm> wordForms;
    private Map<String, Synset> synsets;
    private Map<String, List<String>> dictionary;
    private Map<String, Set<String>> meaningWordFormMap;
    private Map<String, List<String>> specialCaseMap;
    private Map<String, List<String>> syncDictMap;

    protected WordnetData() {

    }

    public static synchronized WordnetData getInstance() {
        if (instance == null) {
            instance = new WordnetData();
        }
        return instance;
    }

    /**
     * Load synsets and wordform.
     */
    public WordnetData loadData() throws IOException {
        dictionary = loadDictionary();
        wordForms = loadWordForm();
        synsets = loadSynset();
        loadSpecialCases();
        loadSyncDict();
        return this;
    }

    /**
     * Load data file to list WordForm.
     * Path: data/index.noun
     *
     * @return
     */
    private Map<String, WordForm> loadWordForm() throws IOException {
        Map<String, WordForm> wordFormMap = new HashMap<>();
        List<String> fileContent = loadFile(INDEX_PATH);
        for (String line : fileContent) {
            if (line.startsWith("  ")) {
                continue;
            }

            WordForm wordForm = wordFormFromString(line);
            wordForm.setMeanings(dictionary.get(wordForm.getLemma()));
            wordFormMap.put(wordForm.getLemma(), wordForm);
        }
        return wordFormMap;
    }

    /**
     * Load data from dictionary file.
     * Line start by "@" contain word.
     * Line start by "*" contain kind of word. But in this project only contain noun.
     * Line start by "-" contain one or more meanings of word above.
     *
     * @return Map with key is word and value is list meanings of key.
     * @throws IOException
     */
    private Map<String, List<String>> loadDictionary() throws IOException {
        dictionary = new HashMap<>();
        meaningWordFormMap = new HashMap<>();
        List<String> dictionaryFile = loadFile(EV_DICT_PATH);
        String currentWord = "";
        for (String line : dictionaryFile) {
            String standardLine = line.trim();
            if (standardLine.startsWith("@")) {
                String word = standardLine.replace("@","");
                currentWord = word.replace(" ", "_");
                currentWord = currentWord.trim();
                dictionary.put(currentWord, new ArrayList<>());
                continue;
            }

            if (line.startsWith("-")) {
                List<String> currentListMeaning = dictionary.get(currentWord);
                String meaning = line.substring(1);
                meaning = meaning.trim();
                if (meaning.isEmpty()) {
                    continue;
                }
                currentListMeaning.add(meaning.trim());
                dictionary.put(currentWord, currentListMeaning);
            }
        }
        return dictionary;
    }

    private void loadSpecialCases() throws IOException {
        specialCaseMap = new HashMap<>();
        List<String> specailFileLines = loadFile(SPECIAL_PATH);
        specailFileLines.forEach(this::parseSpecialCaseLine);
    }

    private void parseSpecialCaseLine(String line) {
        String[] lineArray = line.split("\\|");
        String synsetId = lineArray[0];
        String meaningArray = lineArray[2];
        String[] meaningList = meaningArray.split(",");
        List<String> meanings = new ArrayList<>();
        for (String meaning : meaningList) {
            meanings.add(meaning.trim());
        }
        specialCaseMap.put(synsetId, meanings);
    }

    private Map<String, List<String>> loadMonths() throws IOException {
        Map<String, List<String>> monthsMap = new HashMap<>();
        List<String> monthsFileContent = loadFile(MONTHS_PATH);
        int fileLineQuantity = monthsFileContent.size();
        for (int index = 0; index < fileLineQuantity - 1; index +=2) {
            List<String> listMeaning = new ArrayList<>();
            String monthLine = monthsFileContent.get(index).replace("@", "").trim();
            String[] monthLineArray = monthLine.split(",");
            String month = monthLineArray[0].trim();
            String monthStandFor = monthLineArray[1].trim();
            String meaning = monthsFileContent.get(index + 1).replace("-", "").trim();
            listMeaning.add(meaning);
            monthsMap.put(month, listMeaning);
            monthsMap.put(monthStandFor, listMeaning);
        }
        return monthsMap;
    }

    /**
     * Parse line in data/index.noun to WordForm.
     * One line equivalent one WordForm.
     * Information separated by space.
     *
     * First element display WordForm id.
     * 3th element display quantity of words in this WordForm (n).
     * Last n elements display synset id that WordForm belong.
     *
     * @param string one line in data/index.noun
     * @return
     */
    private WordForm wordFormFromString(String string) {
        String[] elements = string.split(" ");
        WordForm wordForm = new WordForm();
        wordForm.setLemma(elements[0].trim());
        int synsetQuantity = Integer.valueOf(elements[2]);
        int synsetOffsetStartPosition = elements.length - synsetQuantity;
        List<String> synsets = new ArrayList<>();
        for (int index = synsetOffsetStartPosition; index < elements.length; index ++) {
            synsets.add(elements[index].trim());
        }
        wordForm.setSynsets(synsets);
        return wordForm;
    }

    /**
     *  Load all synsets to synset map.
     *  Path: data/data.noun
     *
     * @return
     */
    public Map<String, Synset> loadSynset() throws IOException {
        List<String> fileLines = loadFile(DATA_PATH);
        Map<String, Synset> result = new HashMap<>();
        for (String line  : fileLines) {
            if (line.startsWith("  ")) {
                continue;
            }
            Synset synset = synsetFromString(line);
            result.put(synset.getSynsetId(), synset);
        }
        return result;
    }

    /**
     * Parse Synset from line in data/data.noun.
     * One line equivalent one Synset.
     * Information separated by space.
     *
     * First element display synset id.
     * 4th element display quantity of words in this synset (hexadecimal).
     * 2 next 2 elements display word and lex_id. But we will skip lex_id because not use in this project.
     * The others described in Wordnet documentation.
     * A gloss is represented as a vertical bar (|).
     * Followed by a text string that continues until the end of the line.
     *
     * @see <a href="https://wordnet.princeton.edu/documentation/wndb5wn">Wordnet File Format</a>
     *
     * @param string line in data file
     * @return
     */
    private Synset synsetFromString(String string) {
        String[] elements = string.split(" ");
        int wordQuantity = Integer.parseInt(elements[3], 16);
        int wordEndPosition = 3 + wordQuantity * 2;
        List<String> words = new ArrayList<>();
        if (wordQuantity > 0) {
            for (int index = 4; index < wordEndPosition; index += 2) {
                String word = elements[index];
                words.add(word.trim());
            }
        }

        String[] dataSynsetGlossSplit = string.split("\\|");
        String gloss = dataSynsetGlossSplit[1].trim();

        List<String> children = new ArrayList<>();
        List<String> parents = new ArrayList<>();
        for (int index = 0; index < elements.length; index ++) {
            String element = elements[index];
            if (element.equals("@")) {
                index ++;
                children.add(elements[index].trim());
                continue;
            }

            if (element.equals("~")) {
                index ++;
                parents.add(elements[index].trim());
            }
        }

        return new Synset()
                .setGloss(gloss)
                .setSynsetId(elements[0])
                .setWords(words)
                .setParents(parents)
                .setChildren(children);
    }

    /**
     * Load text file from disk.
     *
     * @param path link to text file
     * @return list of String, an element equivalent one line in text file
     * @throws IOException
     */
    private List<String> loadFile(String path) throws IOException {
        Path filePath = Paths.get(path);
        Stream<String> stringStream = Files.lines(filePath);
        List<String> result = new ArrayList<>();
        stringStream.forEach(result::add);
        return result;
    }

    public List<WordForm> getWordFormBySynset(Synset synset) {
        List<WordForm> wordFormList = new ArrayList<>();
        for (String lemma : synset.getWords()) {
            if (lemma != null && !lemma.isEmpty()) {
                wordFormList.add(wordForms.get(lemma));
            }
        }
        return wordFormList;
    }

    public List<Synset> getSynsetListByWordForm(WordForm wordForm) {
        if (wordForm == null) {
            return new ArrayList<>();
        }
        List<Synset> synsetList = new ArrayList<>();
        for (String synsetId : wordForm.getSynsets()) {
            if (synsets.get(synsetId) != null) {
                synsetList.add(synsets.get(synsetId));
            }
        }
        return synsetList;
    }

    private void loadSyncDict() throws IOException {
        syncDictMap = new HashMap<>();
        List<String> synDictLines = loadFile(SYNC_DICT_PATH);
        synDictLines.forEach(this::parseSyncDictLine);
    }

    private void parseSyncDictLine(String line) {
        String[] elements = line.split(",");
        for (String word : elements) {
            List<String> syncWordList = new ArrayList<>();
            for (String syncWord : elements) {
                if (!syncWord.equals(word)) {
                    syncWordList.add(syncWord.trim());
                }
            }
            syncDictMap.put(word, syncWordList);
        }
    }

    public List<String> getSyncWords(String word) {
        List<String> result = syncDictMap.get(word);
        return processResult(result);
    }

    public List<String> getWordMeaning(String word) {
        List<String> result = dictionary.get(word);
        return processResult(result);
    }

    public WordForm getWordFormByLemma(String lemma) {
        return wordForms.get(lemma);
    }

    public Synset getSynsetById(String synsetId) {
        return synsets.get(synsetId);
    }

    public Map<String, WordForm> getWordForms() {
        return wordForms;
    }

    public Map<String, Synset> getSynsets() {
        return synsets;
    }

    public List<String> getListWordByMeaning(String meaning) {
        if (meaningWordFormMap.get(meaning) == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(meaningWordFormMap.get(meaning));
    }

    public List<Synset> getNearestSynsets(Synset synset) {
        Set<String> nearestSynsetIds = new HashSet<>();
        if (synset.hasParent()) {
            nearestSynsetIds.addAll(synset.getParents());
        }

        if (synset.hasChild()) {
            nearestSynsetIds.addAll(synset.getChildren());
        }

        Set<Synset> result = new HashSet<>();
        for (String synsetId : nearestSynsetIds) {
            result.add(getSynsetById(synsetId));
        }
        return new ArrayList<>(result);
    }

    public List<String> getSpecialCaseMeaning(String synsetId) {
        List<String> result = specialCaseMap.get(synsetId);
        return processResult(result);
    }

    private <T> List<T> processResult(List<T> result) {
        if (result == null) {
            return new ArrayList<>();
        }
        return result;
    }
}

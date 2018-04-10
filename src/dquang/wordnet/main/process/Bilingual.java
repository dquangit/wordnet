package dquang.wordnet.main.process;

import com.sun.istack.internal.NotNull;
import dquang.wordnet.main.data.WordnetData;
import dquang.wordnet.main.entity.BilingualCase;
import dquang.wordnet.main.entity.Synset;
import dquang.wordnet.main.entity.SynsetMeaning;
import dquang.wordnet.main.entity.WordForm;

import java.util.*;

public class Bilingual {

    private WordnetData wordnetData;
    private List<BilingualCase> caseList;

    public Bilingual() {
        wordnetData = WordnetData.getInstance();
        caseList = initAllCases();
    }

    /**
     * Get all synset's meanings.
     *
     * @param synset could not be null.
     * @return all synset's meanings if can translate.
     *         Otherwise return an instance if SynsetMeaning that's case "Cannot translate"
     *         And meanings are original words.
     */
    public SynsetMeaning getSynsetMeaning(Synset synset) {
        if (synset == null) {
            return null;
        }
        for (BilingualCase bilingualCase : caseList) {
            SynsetMeaning result = bilingualCase.getMeaning(synset);
            if (result != null) {
                return result;
            }
        }
        return cannotTranslate(synset);
    }

    /**
     * Init all cases follow priority.
     * Translate processing runs case by case till result not null.
     *
     * @return list contain all cases.
     */
    private List<BilingualCase> initAllCases() {
        List<BilingualCase> caseList = new ArrayList<>();
        BilingualCase caseSpecial = this::getSpecialCaseMeaning;
        BilingualCase case1 = this::getSimilarMeaningInAllWordForm;
        BilingualCase case2 = this::getSynsetContainSingleValueWordFormMeaning;
        BilingualCase case31 = this::getMaxRepeatMeaningInWordForms;
        BilingualCase case32 = this::getMeaningFromNearestSynsets;
        BilingualCase case33 = this::getMeaningFromGloss;
        BilingualCase case32B1 = this::getMeaningFromSynonymousNearestSynsets;
        BilingualCase case33B1 = this::getMeaningFromSynonymousNounsInGloss;
        caseList.add(caseSpecial);
        caseList.add(case1);
        caseList.add(case2);
        caseList.add(case31);
        caseList.add(case32);
        caseList.add(case33);
        caseList.add(case32B1);
        caseList.add(case33B1);
        return caseList;
    }

    /**
     * Base on special case dictionary, if input belong dictionary, get meanings.
     * If synset is special case but cannot get meanings, keep original.
     *
     * @param synset not null.
     * @return meanings in dictionary or original words.
     */
    private SynsetMeaning getSpecialCaseMeaning(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        if (isSynsetSpecialCase(synset)) {
            List<String> meanings = wordnetData.getSpecialCaseMeaning(synset.getSynsetId());
            if (meanings.isEmpty()) {
                return originalSynset(synset);
            }
            return synsetMeaning
                    .setMeanings(meanings)
                    .setCaseName("Special case");
        }
        return null;
    }


    /**
     * Called when ran through all bilingual case but cannot get synset's meanings.
     *
     * @param synset not null.
     * @return an instance if SynsetMeaning that's case "Cannot translate",
     *          and meanings are original words.
     */
    private SynsetMeaning cannotTranslate(Synset synset) {
        return SynsetMeaning.newInstance(synset)
                .setMeanings(synset.getWords())
                .setCaseName("Cannot translated");
    }

    /**
     * Called if synset is special case but cannot get meanings in special case dictionary.
     *
     * @param synset not null.
     * @return an instance if SynsetMeaning that's case "Original",
     *          and meanings are original words.
     */
    private SynsetMeaning originalSynset(Synset synset) {
        return SynsetMeaning.newInstance(synset)
                .setMeanings(synset.getWords())
                .setCaseName("Original");
    }

    /**
     * Case 1. This case has highest priority (except Special case).
     * Find words appear in all word form meanings.
     * Make it mean of synset.
     *
     * @param synset not null.
     * @return meanings appear in all word form meanings of synset.
     */
    private SynsetMeaning getSimilarMeaningInAllWordForm(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        if (synset.getWords().size() >= 2) {
            List<String> listWord = synset.getWords();
            Map<String, Integer> meaningCounter = countMeaning(synset);
            int maxCounter = getMaxRepeatTime(meaningCounter);
            if (maxCounter >= listWord.size()) {
                List<String> result = getWordsHasMaximumRepeatTimes(meaningCounter);
                if (!result.isEmpty()) {
                    synsetMeaning.setMeanings(result).setCaseName("Case 1");
                    return synsetMeaning;
                }
            }
        }
        return null;
    }

    /**
     * Case 2. Find "single value" word form in synset.
     * If synset has 1 single value word form, make meaning of that word form is meaning of synset.
     * If synset has more than 1 single value word form, find similar meanings between them,
     * Make these meanings become synset meanings.
     *
     * @param synset not null.
     * @return meanings of synset contain single value word form.
     */
    private SynsetMeaning getSynsetContainSingleValueWordFormMeaning(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        if (synset.getWords().size() >= 1) {
            List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
            List<WordForm> singleValueWordFormList = new ArrayList<>();
            for (WordForm wordForm : wordForms) {
                if (wordForm == null) {
                    continue;
                }
                if (isWordFormSingleValue(wordForm)) {
                    singleValueWordFormList.add(wordForm);
                }
            }
            synsetMeaning.setCaseName("Case 2");
            if (singleValueWordFormList.size() == 1) {
                return synsetMeaning
                        .setMeanings(
                                wordnetData.getWordMeaning(
                                        singleValueWordFormList.get(0).getLemma()));
            }

            if (singleValueWordFormList.size() > 1) {
                Map<String, Integer> meaningCounter = countMeaning(singleValueWordFormList);
                if (getMaxRepeatTime(meaningCounter) >= 2) {
                    List<String> result = getWordsHasMaximumRepeatTimes(meaningCounter);
                    if (!result.isEmpty()) {
                        synsetMeaning
                                .setMeanings(result);
                        return synsetMeaning;
                    }
                }
            }

        }
        return null;
    }

    /**
     * Case 3. Make words have maximum number of occurrences become meaning of synset.
     * If cannot find words have maximum number, then calculate all meanings sdice,
     * Get pairs have highest sdice.
     *
     * @param synset
     * @return
     */
    private SynsetMeaning getMaxRepeatMeaningInWordForms(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        if (synset.getWords().size() >= 2) {
            // Case 3.1 A
            Map<String, Integer> meaningCounter = countMeaning(synset);
            int maxMeaningCounter = getMaxRepeatTime(meaningCounter);
            if (maxMeaningCounter > 1) {
                List<String> result = getWordsHasMaximumRepeatTimes(meaningCounter);
                if (!result.isEmpty()) {
                    synsetMeaning
                            .setMeanings(result)
                            .setCaseName("Case 3.1A");
                    return synsetMeaning;
                }
            }

            // Case 3.1 B
            if (maxMeaningCounter == 1) {
                List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
                List<String> result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, wordForms);
                if (!result.isEmpty()) {
                    synsetMeaning
                            .setCaseName("3.1B")
                            .setMeanings(result);
                    return synsetMeaning;
                }

                result = getMeaningFromSynonymousDict(wordForms);
                if (!result.isEmpty()) {
                    synsetMeaning
                            .setCaseName("3.1.B1")
                            .setMeanings(result);
                    return synsetMeaning;
                }
            }
        }

        return null;
    }

    /**
     * Get all nearest synsets, and make all common of them become synset's meanings.
     * If cannot find words have maximum number, then calculate all meanings sdice,
     * Get pairs have highest sdice.
     *
     * @param synset not null.
     * @return common meanings of nearest synsets.
     */
    private SynsetMeaning getMeaningFromNearestSynsets(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        if (synset.getWords().size() > 0) {
            Set<String> result = new HashSet<>();
            List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
            List<Synset> nearestSynsets = wordnetData.getNearestSynsets(synset);
            List<WordForm> wordFormListInNearestSynsets = new ArrayList<>();
            for (Synset nearestSynset : nearestSynsets) {
                wordFormListInNearestSynsets.addAll(wordnetData.getWordFormBySynset(nearestSynset));
            }

            for (WordForm wordForm : wordForms) {
                for (WordForm nearestWordform : wordFormListInNearestSynsets) {
                    result.addAll(getCommonMeaningOfWordforms(wordForm, nearestWordform));
                }
            }

            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("3.2A")
                        .setMeanings(new ArrayList<>(result));
                return synsetMeaning;
            }

            result.addAll(MeaningSDice.getMeaningListBaseOnSDice(wordForms, wordFormListInNearestSynsets));
            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("3.2B")
                        .setMeanings(new ArrayList<>(result));
                return synsetMeaning;
            }
        }
        return null;
    }

    /**
     * Get all nouns from synset's gloss.
     * Get all their meanings and find common meanings.
     * If cannot then find common base on largest sdice.
     *
     * @param synset
     * @return
     */
    private SynsetMeaning getMeaningFromGloss(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        if (synset.getWords().size() > 0) {
            List<WordForm> nounWordFormList = getNounWordFormListFromGloss(synset);
            List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
            Set<String> resultSet = new HashSet<>();
            for (WordForm wordForm : wordForms) {
                for (WordForm nounWordform : nounWordFormList) {
                    resultSet.addAll(getCommonMeaningOfWordforms(wordForm, nounWordform));
                }
            }

            if (!resultSet.isEmpty()) {
                synsetMeaning.setCaseName("Case 3.3A")
                        .setMeanings(new ArrayList<>(resultSet));
                return synsetMeaning;
            }

            List<String> result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, nounWordFormList);
            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("Case 3.3B")
                        .setMeanings(new ArrayList<>(resultSet));
                return synsetMeaning;
            }
        }
        return null;
    }

    /**
     *
     * @param synset
     * @return
     */
    private SynsetMeaning getMeaningFromSynonymousNearestSynsets(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        List<Synset> nearestSynsets = wordnetData.getNearestSynsets(synset);
        List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
        List<WordForm> wordFormListInNearestSynsets = new ArrayList<>();
        for (Synset nearestSynset : nearestSynsets) {
            wordFormListInNearestSynsets.addAll(wordnetData.getWordFormBySynset(nearestSynset));
        }

        List<WordForm> syncWordForm = getSyncWordFormList(wordFormListInNearestSynsets);
        List<String> result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, syncWordForm);
        if (!result.isEmpty()) {
            synsetMeaning.setCaseName("Case 3.2.B1")
                    .setMeanings(result);
            return synsetMeaning;
        }
        return null;
    }

    private SynsetMeaning getMeaningFromSynonymousNounsInGloss(Synset synset) {
        SynsetMeaning synsetMeaning = SynsetMeaning.newInstance(synset);
        List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
        List<WordForm> nounWordFormList = getNounWordFormListFromGloss(synset);
        List<WordForm> syncWordForm = getSyncWordFormList(nounWordFormList);
        List<String> result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, syncWordForm);
        if (!result.isEmpty()) {
            synsetMeaning.setCaseName("Case 3.3.B1")
                    .setMeanings(result);
            return synsetMeaning;
        }
        return null;
    }

    /**/
    private List<WordForm> getSyncWordFormList(List<WordForm> wordForms) {
        List<WordForm> syncWordForms = new ArrayList<>();
        for (WordForm wordForm : wordForms) {
            List<String> meanings = getWordFormMeanings(wordForm);
            for (String meaning : meanings) {
                List<String> syncWords = wordnetData.getSyncWords(meaning);
                if (syncWords.isEmpty()) {
                    continue;
                }
                WordForm synWordForm = new WordForm()
                        .setMeanings(syncWords);
                syncWordForms.add(synWordForm);
            }
        }
        return syncWordForms;
    }

    private boolean isNoun(String word) {
        return !(wordnetData.getWordFormByLemma(word) == null);
    }

    private List<String> getCommonMeaningOfWordforms(WordForm wordForm1, WordForm wordForm2) {
        if (wordForm1 == null || wordForm2 == null) {
            return new ArrayList<>();
        }
        List<String> meaningsList1 = getWordFormMeanings(wordForm1);
        List<String> meaningsList2 = getWordFormMeanings(wordForm2);
        List<String> result = new ArrayList<>();
        for (String meaning1 : meaningsList1) {
            for (String meaning2 : meaningsList2) {
                if (meaning1.equals(meaning2)) {
                    result.add(meaning1);
                }
            }
        }
        return result;
    }

    private List<String> getWordFormMeanings(WordForm wordForm) {
        if (wordForm == null) {
            return new ArrayList<>();
        }
        List<String> meaningLines = wordForm.getMeanings();
        Set<String> result = new HashSet<>();
        if (meaningLines == null) {
            return new ArrayList<>();
        }
        for (String meaningLine : meaningLines) {
            String[] meanings =  meaningLine.trim().split(",");
            for (String meaning : meanings) {
                result.add(meaning.trim());
            }
        }
        return new ArrayList<>(result);
    }

    private Map<String, Integer> countMeaning(Synset synset) {
        List<WordForm> wordFormList = wordnetData.getWordFormBySynset(synset);
        return countMeaning(wordFormList);
    }

    private Map<String, Integer> countMeaning(List<WordForm> wordFormList) {
        HashMap<String, Integer> meaningCounter = new HashMap<>();
        if (wordFormList == null) {
            return meaningCounter;
        }

        List<Set<String>> meaningsSet = new ArrayList<>();
        for (WordForm wordForm : wordFormList) {

            if (wordForm == null) {
                continue;
            }
            List<String> meaningList = wordForm.getMeanings();
            if (meaningList == null) {
                continue;
            }

            Set<String> meaningSet = new HashSet<>();
            for (String meaningLine : meaningList) {
                String[] meaningLineArray = meaningLine.split(",");
                for (String meaning : meaningLineArray) {
                    String standardWord = meaning.trim();
                    meaningSet.add(standardWord);
                }
            }
            meaningsSet.add(meaningSet);
        }

        for (Set<String> set : meaningsSet) {
            for (String meaning : set) {
                Integer counter = meaningCounter.get(meaning);
                if (counter == null) {
                    meaningCounter.put(meaning, 1);
                } else {
                    counter ++;
                    meaningCounter.put(meaning, counter);
                }
            }
        }
        return meaningCounter;
    }

    /**
     * Check a WordForm is Single Value WordForm.
     *
     * @param wordForm
     * @return true if WordForm belong to only one synset, and has only one Vietnamese meaning line.
     */
    private boolean isWordFormSingleValue(WordForm wordForm) {
        List<Synset> listSynsetBelong = wordnetData.getSynsetListByWordForm(wordForm);
        List<String> meanings = wordnetData.getWordMeaning(wordForm.getLemma());
        if (meanings == null || listSynsetBelong == null) {
            return false;
        }
        return meanings.size() == 1 && listSynsetBelong.size() == 1;
    }

    public boolean isSynsetSpecialCase(@NotNull Synset synset) {
        for (String word : synset.getWords()) {
            if (Character.isUpperCase(word.charAt(0))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Find all nouns in gloss.
     * Which each nouns, get corresponding word form.
     *
     * @param synset need to find in gloss.
     * @return list noun word form in gloss.
     */
    private List<WordForm> getNounWordFormListFromGloss(Synset synset) {
        String gloss = synset.getGloss().trim().replace("\"", "");
        String[] glossSplitted = gloss.split(" ");
        Map<String, Boolean> synsetWordMap = new HashMap<>();
        for (String word : synset.getWords()) {
            synsetWordMap.put(word, true);
        }

        List<WordForm> nounWordFormList = new ArrayList<>();
        for (String word : glossSplitted) {
            if (isNoun(word) && synsetWordMap.get(word) == null) {
                nounWordFormList.add(wordnetData.getWordFormByLemma(word));
            }
        }
        return nounWordFormList;
    }

    /**
     * Find in Map all key maximum repeat time.
     *
     * @param counter map counter repeat time of each word.
     * @return list string has maximum repeat time.
     */
    private List<String> getWordsHasMaximumRepeatTimes(Map<String, Integer> counter) {
        int maxValue = getMaxRepeatTime(counter);
        List<String> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counter.entrySet()) {
            if ((entry.getValue() == maxValue) && (!entry.getKey().isEmpty())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    /**
     * Find maximum repeat time of all words.
     *
     * @param counter map that count repeat time of each word.
     * @return maximum repeat time.
     */
    private int getMaxRepeatTime(Map<String, Integer> counter) {
        int maxValue = 0;
        Set<Map.Entry<String, Integer>> counterEntrySet = counter.entrySet();
        for (Map.Entry<String, Integer> entry : counterEntrySet) {
            if (entry.getValue() >= maxValue) {
                maxValue = entry.getValue();
            }
        }
        return maxValue;
    }

    /**
     * Find all synonymous words and get their meanings,
     * Find common meanings between input's meanings and all synonymous words.
     * If common meanings is empty, find common meaning between all synonymous words.
     *
     * @param wordForms not null.
     * @return common meanings of input's meaning and synonymous, or between all synonymous.
     */
    private List<String> getMeaningFromSynonymousDict(List<WordForm> wordForms) {
        List<WordForm> syncWordForms = getSyncWordFormList(wordForms);
        List<String> result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, syncWordForms);
        if (result.isEmpty()) {
            result = MeaningSDice.getMeaningListBaseOnSDice(syncWordForms, syncWordForms);
        }
        return result;
    }
}

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
        BilingualCase caseSpecial = this::getSpecialCaseMeaning;
        BilingualCase case1 = this::getSimilarMeaningInAllWordForm;
        BilingualCase case2 = this::getSynsetContainSingleValueWordFormMeaning;
        BilingualCase case31 = this::getMaxRepeatMeaningInWordForms;
        BilingualCase case32 = this::getMeaningFromNearestSynsets;
        BilingualCase case33 = this::getMeaningFromGloss;
        BilingualCase case32B1 = this::getMeaningFromSynonymousNearestSynsets;
        BilingualCase case33B1 = this::getMeaningFromSynonymousNounsInGloss;
        return new ArrayList<>(
                Arrays.asList(
                        caseSpecial, case1, case2, case31,
                        case32, case33, case32B1, case33B1));
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
                List<String> result = new ArrayList<>();
                if (getMaxRepeatTime(meaningCounter) == 1) {
                    for (WordForm wordForm : singleValueWordFormList) {
                        result.addAll(wordForm.getMeanings());
                    }
                } else {
                    result = getWordsHasMaximumRepeatTimes(meaningCounter);
                }
                if (!result.isEmpty()) {
                    synsetMeaning
                            .setMeanings(result);
                    return synsetMeaning;
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

                List<WordForm> synonymousWordForms = getSynonymousWordFormList(wordForms);
//                System.out.println(wordForms);
//                System.out.println(synonymousWordForms);
                result = getSimilarMeanings(wordForms, synonymousWordForms);

                if (result.isEmpty()) {
                    result = getMeaningFromSynonymousDict(synset);
                }

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
            List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
            List<Synset> nearestSynsets = wordnetData.getNearestSynsets(synset);
            List<WordForm> wordFormListInNearestSynsets = new ArrayList<>();
            for (Synset nearestSynset : nearestSynsets) {
                wordFormListInNearestSynsets.addAll(wordnetData.getWordFormBySynset(nearestSynset));
            }

            List<String> result = getSimilarMeanings(wordForms, wordFormListInNearestSynsets);

            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("3.2A")
                        .setMeanings(result);
                return synsetMeaning;
            }
            for (WordForm wordForm : wordFormListInNearestSynsets) {
                if (wordForm != null)
                System.out.println(wordForm.getLemma() + ": " + wordForm.getMeanings());
            }
            result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, wordFormListInNearestSynsets);
            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("3.2B")
                        .setMeanings(result);
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
//            Set<String> resultSet = new HashSet<>();
//            for (WordForm wordForm : wordForms) {
//                for (WordForm nounWordform : nounWordFormList) {
//                    resultSet.addAll(getCommonMeaningOfWordforms(wordForm, nounWordform));
//                }
//            }
//            System.out.println(wordForms);
//            for (WordForm wordForm : nounWordFormList) {
//                System.out.println(wordForm.getLemma() + ": " + wordForm.getMeanings());
//            }
//            System.out.println();
            List<String> result = getSimilarMeanings(wordForms, nounWordFormList);

            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("Case 3.3A")
                        .setMeanings(result);
                return synsetMeaning;
            }

            result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, nounWordFormList);
            if (!result.isEmpty()) {
                synsetMeaning.setCaseName("Case 3.3B")
                        .setMeanings(result);
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

        List<WordForm> synonymousWordForm = getSynonymousWordFormList(wordForms);
        List<WordForm> synonymousNearestWordForm = getSynonymousWordFormList(wordFormListInNearestSynsets);

        List<String> result = getSimilarMeanings(wordForms, synonymousNearestWordForm);

        if (result.isEmpty()) {
            result = getSimilarMeanings(synonymousWordForm, wordFormListInNearestSynsets);
        }

        if (result.isEmpty()) {
            result = getSimilarMeanings(synonymousWordForm, synonymousNearestWordForm);
        }

        if (result.isEmpty()) {
            result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, synonymousNearestWordForm);
        }

        if (result.isEmpty()) {
            result = MeaningSDice.getMeaningListBaseOnSDice(synonymousWordForm, wordFormListInNearestSynsets);
        }

        if (result.isEmpty()) {
            result = MeaningSDice.getMeaningListBaseOnSDice(synonymousWordForm, synonymousNearestWordForm);
        }

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
        List<WordForm> nounWordFormListFromGloss = getNounWordFormListFromGloss(synset);
        List<WordForm> synonymousWordForm = getSynonymousWordFormList(wordForms);
        List<WordForm> synonymousWordFormFromGloss = getSynonymousWordFormList(nounWordFormListFromGloss);
//        System.out.println("SYNSET INFORMATION");
//        System.out.println(synset);
//        System.out.println(nounWordFormListFromGloss);
//        System.out.println("END INFORMATION");
        List<String> result = getSimilarMeanings(wordForms, synonymousWordFormFromGloss);

        if (result.isEmpty()) {
//            System.out.println("fuck 1");
            result = getSimilarMeanings(synonymousWordForm, nounWordFormListFromGloss);
        }

        if (result.isEmpty()) {
//            System.out.println("fuck 2");
            result = getSimilarMeanings(synonymousWordForm, synonymousWordFormFromGloss);
        }

        if (result.isEmpty()) {
//            System.out.println("fuck 3");
            result = MeaningSDice.getMeaningListBaseOnSDice(wordForms, synonymousWordFormFromGloss);
        }

        if (result.isEmpty()) {
//            System.out.println("fuck 4");
            result = MeaningSDice.getMeaningListBaseOnSDice(synonymousWordForm, nounWordFormListFromGloss);
        }

        if (result.isEmpty()) {
//            System.out.println("fuck 4");
            result = MeaningSDice.getMeaningListBaseOnSDice(synonymousWordForm, synonymousWordFormFromGloss);
        }

        if (!result.isEmpty()) {
            synsetMeaning.setCaseName("Case 3.3.B1")
                    .setMeanings(result);
            return synsetMeaning;
        }
        return null;
    }

    /**/
    private List<WordForm> getSynonymousWordFormList(List<WordForm> wordForms) {
        List<WordForm> syncWordForms = new ArrayList<>();
        for (WordForm wordForm : wordForms) {
            List<String> meanings = getWordFormMeanings(wordForm);
            Set<String> synonymousMeanings = new HashSet<>();
            for (String meaning : meanings) {
                List<String> syncWords = wordnetData.getSyncWords(meaning);
                if (syncWords.isEmpty()) {
                    continue;
                }
                synonymousMeanings.addAll(syncWords);
            }

            if (synonymousMeanings.isEmpty()) {
                continue;
            }

            WordForm synonymousWordForm = new WordForm()
                    .setMeanings(new ArrayList<>(synonymousMeanings))
                    .setLemma(wordForm.getLemma());
            syncWordForms.add(synonymousWordForm);

        }
        return syncWordForms;
    }

    private boolean isNoun(String word) {
        WordForm wordForm = wordnetData.getWordFormByLemma(word);
        return (wordForm != null) || (!wordnetData.getWordMeaning(word).isEmpty());
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
                if (meaning1.isEmpty() || meaning2.isEmpty()) {
                    continue;
                }
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
        List<String> meaningLines = wordnetData.getWordMeaning(wordForm.getLemma());
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
        List<String> meanings = wordForm.getMeanings();
        return ((meanings.size() == 1) && (listSynsetBelong.size() == 1));
    }

    private boolean isSynsetSpecialCase(@NotNull Synset synset) {
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
        String gloss = synset.getGloss().trim()
                .replace("\"", "")
                .replace(",","")
                .replace(";", "")
                .replace("(", "")
                .replace(")", "");
        String[] glossSplitted = gloss.split(" ");

        List<WordForm> nounWordFormList = new ArrayList<>();
        for (String word : glossSplitted) {
            word = word.trim();
            if (isNoun(word) && !synset.getWords().contains(word)) {
                WordForm wordForm = wordnetData.getWordFormByLemma(word);
                if (wordForm == null) {
                    wordForm = new WordForm()
                            .setLemma(word)
                            .setMeanings(wordnetData.getWordMeaning(word));
                }
                nounWordFormList.add(wordForm);
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
    private List<String> getMeaningFromSynonymousDict(Synset synset) {
        List<WordForm> wordForms = wordnetData.getWordFormBySynset(synset);
        List<WordForm> synonymousWordFormList = getSynonymousWordFormList(wordForms);

        List<MeaningSDice> meaningSDices = new ArrayList<>();
        float maxSdice = 0;
        for (WordForm wordForm1 : wordForms) {
            for (WordForm wordForm2 : synonymousWordFormList) {
                if (wordForm1 == null || wordForm2 == null) {
                    continue;
                }
                if (!wordForm1.getLemma().equals(wordForm2.getLemma())) {
                    MeaningSDice meaningSDice = MeaningSDice
                            .getMeaningListBaseOnSDice(wordForm1, wordForm2);
                    meaningSDices.add(meaningSDice);
                    if (maxSdice < meaningSDice.getSdice()) {
                        maxSdice = meaningSDice.getSdice();
                    }
                }
            }
        }
        Set<String> result = new HashSet<>();
        for (MeaningSDice meaningSDice : meaningSDices) {
            if (meaningSDice.getSdice() == maxSdice) {
                result.addAll(meaningSDice.getListMeaning());
            }
        }
        return new ArrayList<>(result);
    }

    private List<String> getSimilarMeanings(List<WordForm> firstWordForm, List<WordForm> secondWordForm) {
        List<String> result = new ArrayList<>();
        for (WordForm first : firstWordForm) {
            for (WordForm second : secondWordForm) {
                if (first == null || second == null) {
                    continue;
                }
                if (!first.getLemma().equals(second.getLemma())) {
                    result.addAll(getCommonMeaningOfWordforms(first, second));
                }
            }
        }
        return result;
    }
}

package dquang.wordnet.main.process;

import com.sun.istack.internal.NotNull;
import dquang.wordnet.main.entity.WordForm;

import java.util.*;

public class MeaningSDice {

    private static final float THRESHOLD = 0.65f;

    private float sdice;
    private List<String> listMeaning;

    public MeaningSDice() {
    }

    public float getSdice() {
        return sdice;
    }

    public MeaningSDice setSdice(float sdice) {
        this.sdice = sdice;
        return this;
    }

    public List<String> getListMeaning() {
        return listMeaning;
    }

    public MeaningSDice setListMeaning(List<String> listMeaning) {
        this.listMeaning = listMeaning;
        return this;
    }

    public static List<String> getMeaningListBaseOnSDice(@NotNull List<WordForm> firstList,
                                                         @NotNull List<WordForm> secondList) {
        if (firstList == null || secondList == null) {
            return new ArrayList<>();
        }
        PriorityQueue<MeaningSDice> meaningSDicesQueue = new PriorityQueue<>(
                (o1, o2) -> o1.getSdice() < o2.getSdice() ? 1 : -1);
        for (WordForm wordForm1 : firstList) {
            for (WordForm wordForm2 : secondList) {
                if (wordForm1 == null || wordForm2 == null) {
                    continue;
                }
                if (!wordForm1.equals(wordForm2)) {
                    MeaningSDice meaningSDice = MeaningSDice
                            .getMeaningListBaseOnSDice(wordForm1, wordForm2);
                    meaningSDicesQueue.add(meaningSDice);
                }
            }
        }
        Set<String> result = new HashSet<>();
        if (!meaningSDicesQueue.isEmpty()) {
            float maxSdice = 0;
            while (!meaningSDicesQueue.isEmpty()) {
                MeaningSDice meaningSDice = meaningSDicesQueue.remove();
                if (maxSdice > meaningSDice.getSdice()) {
                    break;
                }
                maxSdice = meaningSDice.getSdice();
                result.addAll(meaningSDice.getListMeaning());
            }
        }
        return new ArrayList<>(result);
    }

    public static MeaningSDice getMeaningListBaseOnSDice(@NotNull WordForm wordForm1, @NotNull WordForm wordForm2) {
        List<String> meaningList1 = listMeaningFromWordForm(wordForm1);
        List<String> meaningList2 = listMeaningFromWordForm(wordForm2);
        float maxSdice = 0;
        Stack<String> maxSdiceMeaningStack = new Stack<>();
        for (String meaning1 : meaningList1) {
            for (String meaning2 : meaningList2) {
                if (meaning1.equals(meaning2)) {
                    continue;
                }
                float sdice = sdice(meaning1, meaning2);
                if (sdice < maxSdice) {
                    continue;
                }
                if (sdice > maxSdice) {
                    maxSdiceMeaningStack.clear();
                    maxSdice = sdice;
                }
                maxSdiceMeaningStack.push(meaning1);
            }
        }
        if (maxSdice <= THRESHOLD) {
            return new MeaningSDice()
                    .setSdice(0)
                    .setListMeaning(new ArrayList<>());
        }
        List<String> meanings = new ArrayList<>(maxSdiceMeaningStack);
        return new MeaningSDice()
                .setListMeaning(meanings)
                .setSdice(maxSdice);
    }

    private static List<String> listMeaningFromWordForm(WordForm wordForm) {
        List<String> meaningLines = wordForm.getMeanings();
        if (meaningLines == null) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (String meaningLine : meaningLines) {
            String[] wordInLine = meaningLine.split(",");
            for (String word : wordInLine) {
                result.add(word.trim());
            }
        }
        return result;
    }

    public static float sdice(String word1, String word2) {
        String[] word1Array = word1.trim().split(" ");
        String[] word2Array = word2.trim().split(" ");
        List<String> listEqualWord = new ArrayList<>();
        for (String subWord1 : word1Array) {
            for (String subWord2 : word2Array) {
                if (subWord1.equals(subWord2)) {
                    listEqualWord.add(subWord1);
                }
            }
        }
        float sdice = 2*(listEqualWord.size())*1.0f/(word1Array.length + word2Array.length);
//        System.out.println(word1 + " - " + word2 + " " + sdice);
        return sdice;
    }
}

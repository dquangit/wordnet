package dquang.wordnet.main;

import dquang.wordnet.main.data.WordnetData;
import dquang.wordnet.main.entity.Synset;
import dquang.wordnet.main.entity.SynsetMeaning;
import dquang.wordnet.main.entity.WordForm;
import dquang.wordnet.main.out.ExportToXlsx;
import dquang.wordnet.main.process.Bilingual;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Main {

    public static void main(String[] args) throws Exception {
        WordnetData.getInstance().loadData();
//        translateByWord("paper profit");
//        translateByWord("paper loss");
//        translateSynsetId("05778254");
        writeData();
    }

    public static void gotoXY() {
        char escCode = 0x1B;
        int row = 0;
        int column = 0;
        System.out.print(String.format("%c[%d;%df", escCode, row, column));
    }

    private static void translateByWord(String word) {
        WordForm wordForm = WordnetData.getInstance().getWordFormByLemma(word);
        Bilingual bilingual = new Bilingual();
        List<Synset> synsets = WordnetData.getInstance().getSynsetListByWordForm(wordForm);
        synsets.forEach(new Consumer<Synset>() {
            @Override
            public void accept(Synset synset) {
                System.out.println(bilingual.getSynsetMeaning(synset));
            }
        });
    }

    public static void translateSynsetId(String synsetId) {
        Synset synset = WordnetData.getInstance().getSynsetById(synsetId);
        System.out.println(new Bilingual().getSynsetMeaning(synset));
    }

    private static void writeData() throws IOException {
        Bilingual bilingual = new Bilingual();
        Path path = Paths.get("data/out.txt");
        BufferedWriter writer = Files.newBufferedWriter(path);
        WordnetData.getInstance().getSynsets().forEach((s, synset) -> {
            try {
                SynsetMeaning meaningList = bilingual.getSynsetMeaning(synset);
                if (meaningList != null)
                writer.write(meaningList.toString() + "\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        writer.close();
    }
}

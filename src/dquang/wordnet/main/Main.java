package dquang.wordnet.main;

import dquang.wordnet.main.data.WordnetData;
import dquang.wordnet.main.entity.Synset;
import dquang.wordnet.main.entity.SynsetMeaning;
import dquang.wordnet.main.entity.WordForm;
import dquang.wordnet.main.process.Bilingual;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

public class Main {

    public static void main(String[] args) throws Exception {
        long tStart = System.currentTimeMillis();
        WordnetData wordnetData = WordnetData.getInstance().loadData();
//        Synset synset = wordnetData.getSynsetById("00413462");
        Bilingual bilingual = new Bilingual();
//        System.out.println(bilingual.getSynsetMeaning(synset));
//        System.out.println(bilingual.getSynsetMeaning(synset));
        Path path = Paths.get("output/result.txt");
        BufferedWriter writer = Files.newBufferedWriter(path);
        wordnetData.getSynsets().forEach(new BiConsumer<String, Synset>() {
            @Override
            public void accept(String s, Synset synset) {
                try {
                   SynsetMeaning meaningList = bilingual.getSynsetMeaning(synset);
                   writer.write(meaningList.toString() + "\n");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

    }

    public static void gotoXY() {
        char escCode = 0x1B;
        int row = 0; int column = 0;
        System.out.print(String.format("%c[%d;%df",escCode,row,column));
    }
}

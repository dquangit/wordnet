package dquang.wordnet.main.out;

import dquang.wordnet.main.entity.SynsetMeaning;
import org.jxls.common.Context;
import org.jxls.util.JxlsHelper;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import static com.sun.org.apache.xerces.internal.utils.SecuritySupport.getResourceAsStream;

public class ExportToXlsx {

    private static final String PATH = "output/result.xlsx";

    public static void ExportToExcel(List<SynsetMeaning> meaningList) throws IOException {
        List<ExcelOutput> output = listOutputFromMeaningList(meaningList);
        InputStream is = getResourceAsStream("result.xlsx");
        OutputStream os = new FileOutputStream(PATH);
        Context context = new Context();
        context.putVar("Output", output);
        JxlsHelper.getInstance().processTemplate(is, os, context);
    }

    private static List<ExcelOutput> listOutputFromMeaningList(List<SynsetMeaning> meaningList) {
        List<ExcelOutput> result = new ArrayList<>();
        for (SynsetMeaning synsetMeaning : meaningList) {
            result.add(excelOutputFromSynsetMeaning(synsetMeaning));
        }
        return result;
    }

    private static ExcelOutput excelOutputFromSynsetMeaning(SynsetMeaning synsetMeaning) {
        String meaningString = synsetMeaning.toString();
        String[] meaningStringArray = meaningString.split("\\|");
        return new ExcelOutput()
                .setSynsetId(meaningStringArray[0])
                .setWords(meaningStringArray[1])
                .setCaseName(meaningStringArray[2])
                .setGloss(meaningStringArray[3]);
    }

}

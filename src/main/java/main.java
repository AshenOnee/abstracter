import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import abstracter.Abstracter;
import abstracter.Word;


public class main {
    private static Abstracter abstracter = new Abstracter();

    public static void main(String[] args) throws IOException {
        File inputFile = new File("input.txt");
        BufferedReader br = new BufferedReader(new FileReader(inputFile));

        String str;
        String title = br.readLine();
        StringBuilder text = new StringBuilder();

        while ((str = br.readLine()) != null)
            text.append(str);

        ArrayList<String> compressedSentences = abstracter.DoAbstract(title, text.toString(), 0.5);
        StringBuilder compressedText = new StringBuilder();
        for(String sentence : compressedSentences){
            compressedText.append(sentence).append(" ");
        }

        ArrayList<String> sentences = abstracter.SplitOnSentences(text.toString());
        ArrayList<ArrayList<Word>> sentencesLemmas = abstracter.GetSentencesLemmas(sentences);

        String[][] rules = {
                {"П", "С"}
        };

        HashMap countPhrases = abstracter.CalcCountPhrases(sentencesLemmas, sentences, rules);
        System.out.println(countPhrases);

        File outputFile = new File("output.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(outputFile));
        bufferedWriter.write(compressedText.toString());
        bufferedWriter.close();
    }
}

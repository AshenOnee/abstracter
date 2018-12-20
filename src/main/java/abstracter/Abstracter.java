package abstracter;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.ru.RussianAnalyzer;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Abstracter {

//    private LinkedHashMap<String, Double> weightWords;
//    private LinkedHashMap<String, Double> weightSentences;

    private LuceneMorphology instance;

    private String[][] rules;

    public Abstracter(){
        rules = new String[][]{
//                {"С", "C"},
                {"П", "С"}
        };
    }

   /**
    * Используется для получения реферата текста
    * @param title Заголовок текста
    * @param text Исходный текст
    * @param compression Степень сжатия текста
    * @return Наиболее значимые предложения
    */
    public ArrayList<String> DoAbstract(String title, String text, double compression) throws IOException {

        instance = new RussianLuceneMorphology();

        ArrayList<String> sentences = SplitOnSentences(text);
        ArrayList<ArrayList<Word>> sentencesLemmas = GetSentencesLemmas(sentences);
        LinkedHashMap countPhrases = CalcCountPhrases(sentencesLemmas, sentences, rules);

//        LinkedHashMap<String, Integer> countWords = new LinkedHashMap<>();
//        LinkedHashMap<String, Integer> countWordsTitle = new LinkedHashMap<>();

        ArrayList<Word> titleLemmas = GetSentenceLemmas(title);
        LinkedHashMap<String, Double> lemmas = GetCountLemmas(sentencesLemmas);
        lemmas = CorrectWeightLemmas(lemmas, titleLemmas);

        LinkedHashMap sentencesWeights = GetWeightSentences(sentencesLemmas, sentences, lemmas, countPhrases);
        ArrayList compresedSentences = CompressSentences(sentences, sentencesWeights, compression);

//        CalcWeightWords();
//        CalcWeightSentences(sentences);
        return compresedSentences;
    }


    /**
     * Используется для получения словосочентаний
     * @param sentencesLemmas Леммы предложений
     * @param sentences Предложения
     * @return Словосочетания и частота встречаемости словосочетаний в тексте
     */
    public LinkedHashMap CalcCountPhrases(ArrayList<ArrayList<Word>> sentencesLemmas, ArrayList<String> sentences, String[][] rules) throws IOException {
        LinkedHashMap<String, Integer> countPhrases = new LinkedHashMap<>();

        for(int i = 0; i < sentencesLemmas.size(); i++){
            for(int j = 0; j < sentencesLemmas.get(i).size(); j++){
                for(String[] rule: rules){
                    Morphology morphology1 = null;
                    for (Morphology lem : sentencesLemmas.get(i).get(j).getLemmas()){
                        if(lem.getPartOfSpeech().equals(rule[0])){
                            morphology1 = lem;
                            break;
                        }
                    }

                    if (morphology1 != null && j + 1 < sentencesLemmas.get(i).size()){
                        Morphology morphology2 = null;
                        for (Morphology lem : sentencesLemmas.get(i).get(j + 1).getLemmas()){
                            if(lem.getPartOfSpeech().equals(rule[1])){
                                morphology2 = lem;
                                break;
                            }
                        }

                        if(morphology2 != null){
                            String str = sentencesLemmas.get(i).get(j).getWord() + " " + sentencesLemmas.get(i).get(j + 1).getWord();
                            if(sentences.get(i).toLowerCase().contains(str)){
                                String phrase = morphology1.getBase() + " " + morphology2.getBase();

                                if(countPhrases.containsKey(phrase)){
                                    countPhrases.put(phrase, countPhrases.get(phrase) + 1);
                                }
                                else {
                                    countPhrases.put(phrase, 1);
                                }
                            }
                        }
                    }
                }
            }
        }
        return countPhrases;
    }

    /**
     * Используется для получения леммы слова
     * @param word Слово
     * @return Ламма слова
     */
    public Word GetWordLemma(String word){
        word = word.toLowerCase();
        Word resultWord = new Word(word);
        if (instance.checkString(word)) {
            List<String> info = instance.getMorphInfo(word);
            for(String lemm : info){
                Morphology morphology = new Morphology();

                String[] mas = lemm.split("\\|");
                morphology.setBase(mas[0]);

                mas[1] = mas[1].replaceFirst(" ", "\n");
                mas = mas[1].split("\n");

                mas[1] = mas[1].replaceFirst(" ", "\n");
                mas = mas[1].split("\n");
                morphology.setPartOfSpeech(mas[0]);

                if(mas.length > 1)
                    morphology.setMorphology(mas[1]);

                resultWord.getLemmas().add(morphology);
            }
            return resultWord;
        }
        else return null;
    }

    /**
     * Используется для получения лемм предложения
     * @param sentence Предложение
     * @return Леммы из предложения
     */
    public ArrayList<Word> GetSentenceLemmas(String sentence) throws IOException {
        ArrayList<Word> list = new ArrayList<>();

        RussianAnalyzer russianAnalyzer = new RussianAnalyzer();
        Tokenizer source = new StandardTokenizer();
        source.setReader(new StringReader(sentence));
        TokenStream tokenStream = new StandardFilter(source);
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, russianAnalyzer.getStopwordSet());

        CharTermAttribute  termAttr = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        while (tokenStream.incrementToken()) {
            String str = termAttr.toString();
            Word word = GetWordLemma(str);
            if(word != null)
                list.add(word);
        }

        tokenStream.end();
        tokenStream.close();
        return list;
    }

    /**
     * Используется для получения лемм текста
     * @param sentences Текст разбитый на предложения
     * @return Леммы из предложений
     */
    public ArrayList<ArrayList<Word>> GetSentencesLemmas(ArrayList<String> sentences) throws IOException {
        ArrayList<ArrayList<Word>> resultSentencesLemmas = new ArrayList<>();
        for(String sentence : sentences){
            ArrayList<Word> sentenceLemmas = GetSentenceLemmas(sentence);
            resultSentencesLemmas.add(sentenceLemmas);
        }
        return resultSentencesLemmas;
    }

    /**
     * Используется для разбиения текста на предложения
     * @param text Исходный текст
     * @return Текст разбитый на предложения
     */
    public ArrayList<String> SplitOnSentences(String text) {
        String regex = "[А-ЯЁ][\\S\\s]+?(?:[\\S][^А-ЯЁ. ]){1,}(?:\\.+|[?!])(?!(\\s*[а-яёa-z)\\-\"«0-9.]))";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        ArrayList<String> sentences = new ArrayList<>();
        while (matcher.find())
            sentences.add(matcher.group(0));

        return sentences;
    }


    /**
     * Используется для разбиения текста на слова
     * @param text Исходный текст
     * @return Текст разбитый на cлова в виде потока
     */
    public TokenStream GetTokenStream(String text) {
        RussianAnalyzer russianAnalyzer = new RussianAnalyzer();
        TokenStream tokenStream = new StandardFilter(russianAnalyzer.tokenStream(null, new StringReader(text)));
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, russianAnalyzer.getStopwordSet());
        return tokenStream;
    }

//    private HashMap CalcCount(ArrayList<ArrayList<Word>> sentencesLemmas) {
//
//        HashMap<Word, Integer> result = new HashMap<>();
//
//        for(ArrayList<Word> sentenceLemmas : sentencesLemmas){
//            for(Word lemma : sentenceLemmas){
//                for(Morphology morphology : lemma.getLemms()){
//
//                    for (Map.Entry<Word, Integer> entry : result.entrySet()) {
//                        for(Morphology morphology1 : entry.getKey().getLemms()){
//                            if(morphology.getBase() == morphology1.getBase());
//                        }
//                    }
//                }
//
//            }
//        }
//
//
//        return result;
////        try{
////            CharTermAttribute  termAttr = tokenStream.addAttribute(CharTermAttribute.class);
////            tokenStream.reset();
////            while (tokenStream.incrementToken()) {
////                if(dictionary.containsKey(termAttr.toString())){
////                    dictionary.put(termAttr.toString(), dictionary.get(termAttr.toString()) + 1);
////                }
////                else {
////                    dictionary.put(termAttr.toString(), 1);
////                }
////            }
////            tokenStream.end();
////            tokenStream.close();
////        }catch (Exception ignored){}
//    }


    private LinkedHashMap<String, Double> GetCountLemmas(ArrayList<ArrayList<Word>> sentencesLemmas){
        LinkedHashMap<String, Double> lemmas = new LinkedHashMap<>();
        for(ArrayList<Word> sentenceLemmas : sentencesLemmas) {
            for (Word lemma : sentenceLemmas) {
                for (Morphology morphology : lemma.getLemmas()) {
                    if(lemmas.containsKey(morphology.getBase()))
                        lemmas.put(morphology.getBase(), lemmas.get(morphology.getBase()) + 1.0);
                    else
                        lemmas.put(morphology.getBase(), 1.0);
                }
            }
        }
        return lemmas;
    }


//    private void CalcWeightWords() {
//        weightWords = new LinkedHashMap<>();
//        countWords.forEach((k, v) ->{
//            double result = (double)v;
//            if (countWordsTitle.containsKey(k))
//                result = result * (countWordsTitle.get(k) + 1);
//            weightWords.put(k, result);
//        });
//    }


    private LinkedHashMap<String, Double> CorrectWeightLemmas( LinkedHashMap<String, Double> lemmas, ArrayList<Word> titleLemmas){
        for(Word titleLemma : titleLemmas){
            for(Morphology morphology : titleLemma.getLemmas()){
                if(lemmas.containsKey(morphology.getBase())){
                    lemmas.put(morphology.getBase(), lemmas.get(morphology.getBase()) * 2.0);
                    break;
                }
            }
        }
        return lemmas;
    }

    private LinkedHashMap<ArrayList<Word>, Double> GetWeightSentences(
            ArrayList<ArrayList<Word>> sentencesLemmas,
            ArrayList<String> sentences,
            LinkedHashMap<String, Double> lemmas,
            LinkedHashMap<String, Integer> phrases){

        LinkedHashMap<ArrayList<Word>, Double> result = new LinkedHashMap<>();
        int i = 0;
        for(ArrayList<Word> sentenceLemmas : sentencesLemmas) {
            result.put(sentenceLemmas, 0.0);
            for (Word lemma : sentenceLemmas) {
                double max = 0.0;
                for (Morphology morphology : lemma.getLemmas()) {
                    if(lemmas.get(morphology.getBase()) > max)
                        max = lemmas.get(morphology.getBase());
                }
                double weight = max;
                result.put(sentenceLemmas, result.get(sentenceLemmas) + weight);
            }

            for (Map.Entry<String, Integer> entry : phrases.entrySet()) {
               if(sentences.get(i).contains(entry.getKey())){
                   double weight = entry.getValue() * 10;
                   result.put(sentenceLemmas, result.get(sentenceLemmas) + weight);
               }
            }

            i++;
        }
        return  result;
    }

//    private void CalcWeightSentences(ArrayList<String> sentences) {
//        weightSentences = new LinkedHashMap<>();
//
//        sentences.forEach(sentence -> {
//            int countWords = 0;
//            double weightSentence = 0;
//            RussianAnalyzer russianAnalyzer = new RussianAnalyzer();
//            TokenStream tokenStream = new StandardFilter(russianAnalyzer.tokenStream(null, new StringReader(sentence)));
//            tokenStream = new LowerCaseFilter(tokenStream);
//            tokenStream = new RussianLightStemFilter(tokenStream);
//
//            try{
//                CharTermAttribute  termAttr = tokenStream.addAttribute(CharTermAttribute.class);
//                tokenStream.reset();
//                while (tokenStream.incrementToken()) {
//                    countWords++;
//
//                    if(weightWords.containsKey(termAttr.toString())){
//                        weightSentence += weightWords.get(termAttr.toString());
//                    }
//                }
//                tokenStream.end();
//                tokenStream.close();
//                weightSentences.put(sentence, weightSentence/countWords);
//            }catch (Exception ignored){}
//        });
//    }

    private ArrayList<String> CompressSentences(ArrayList<String> sentences, LinkedHashMap<ArrayList<Word>, Double> sentencesWeights, double compression){
        int count = (int)(Math.round(compression * sentences.size()));
        if(count == 0) count = 1;
        ArrayList<String> selectedSentences = new ArrayList<>();

        while (count > 0) {
            int i = 0;
            double max = 0;
            int index = 0;
            for (Map.Entry<ArrayList<Word>, Double> entry : sentencesWeights.entrySet()) {
                if(entry.getValue() >= max && !selectedSentences.contains(sentences.get(i))){
                    index = i;
                    max = entry.getValue();
                }
                i++;
            }
            selectedSentences.add(sentences.get(index));
            count--;
        }

        ArrayList<String> result = new ArrayList<>();
        for(String sentence : sentences){
            if(selectedSentences.contains(sentence))
                result.add(sentence);
        }
        return result;
    }
}

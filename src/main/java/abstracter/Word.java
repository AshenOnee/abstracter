package abstracter;

import java.util.ArrayList;
import java.util.List;

public class Word {
    private String word;
    private List<Morphology> lemmas;

    Word(String word){
        this.word = word;
        lemmas = new ArrayList<>();
    }

    String getWord() {
        return word;
    }

    List<Morphology> getLemmas() {
        return lemmas;
    }
}

package com.bayoumi.models.azkar;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class TimedZekrDTO {
    private int order;
    private String content;
    private int count;
    private String countDescription;
    private String fadl;
    private String source;
    private int type;
    private String audio;
    private String hadithText;
    private String explanationOfHadithVocabulary;
    private String translation;
    private String transliteration;

    // Getters and setters

    @JsonProperty("order")
    public int getOrder() {
        return order;
    }

    @JsonProperty("order")
    public void setOrder(int order) {
        this.order = order;
    }

    @JsonProperty("content")
    public String getContent() {
        return content;
    }

    @JsonProperty("content")
    public void setContent(String content) {
        this.content = content;
    }

    @JsonProperty("count")
    public int getCount() {
        return count;
    }

    @JsonProperty("count")
    public void setCount(int count) {
        this.count = count;
    }

    @JsonProperty("count_description")
    public String getCountDescription() {
        return countDescription;
    }

    @JsonProperty("count_description")
    public void setCountDescription(String countDescription) {
        this.countDescription = countDescription;
    }

    @JsonProperty("fadl")
    public String getFadl() {
        return fadl;
    }

    @JsonProperty("fadl")
    public void setFadl(String fadl) {
        this.fadl = fadl;
    }

    @JsonProperty("source")
    public String getSource() {
        return source;
    }

    @JsonProperty("source")
    public void setSource(String source) {
        this.source = source;
    }

    @JsonProperty("type")
    public int getType() {
        return type;
    }

    @JsonProperty("type")
    public void setType(int type) {
        this.type = type;
    }

    @JsonProperty("audio")
    public String getAudio() {
        return audio;
    }

    @JsonProperty("audio")
    public void setAudio(String audio) {
        this.audio = audio;
    }

    @JsonProperty("hadith_text")
    public String getHadithText() {
        return hadithText;
    }

    @JsonProperty("hadith_text")
    public void setHadithText(String hadithText) {
        this.hadithText = hadithText;
    }

    @JsonProperty("explanation_of_hadith_vocabulary")
    public String getExplanationOfHadithVocabulary() {
        return explanationOfHadithVocabulary;
    }

    @JsonProperty("explanation_of_hadith_vocabulary")
    public void setExplanationOfHadithVocabulary(String explanationOfHadithVocabulary) {
        this.explanationOfHadithVocabulary = explanationOfHadithVocabulary;
    }

    @JsonProperty("translation")
    public String getTranslation() {
        return translation;
    }

    @JsonProperty("translation")
    public void setTranslation(String translation) {
        this.translation = translation;
    }

    @JsonProperty("transliteration")
    public String getTransliteration() {
        return transliteration;
    }

    @JsonProperty("transliteration")
    public void setTransliteration(String transliteration) {
        this.transliteration = transliteration;
    }

    @Override
    public String toString() {
        return "TimedZekrDTO{" +
                "order=" + order +
                ", content='" + content + '\'' +
                ", count=" + count +
                ", countDescription='" + countDescription + '\'' +
                ", fadl='" + fadl + '\'' +
                ", source='" + source + '\'' +
                ", type=" + type +
                ", audio='" + audio + '\'' +
                ", hadithText='" + hadithText + '\'' +
                ", explanationOfHadithVocabulary='" + explanationOfHadithVocabulary + '\'' +
                ", translation='" + translation + '\'' +
                ", transliteration='" + transliteration + '\'' +
                '}';
    }


}

package edu.brockport.voiceassistant;

import lombok.Getter;

import java.util.Date;

@Getter
public class DateInfo {

    private final String name;
    private final Date date;
    private final Integer similarity;

    DateInfo(String name, Date date, int similarity) {
        this.name = name;
        this.date = date;
        this.similarity = similarity;
    }

    protected String getCleanEventName(){
        return name.replaceAll("Day \\d", "").replaceAll("[ ][(]\\d[)]", "");
    }
}
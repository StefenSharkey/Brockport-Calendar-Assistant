package edu.brockport.voiceassistant;

public enum Tense {

    PAST("past"),
    NOTPAST("notpast");

    public final String label;

    private Tense(String label) {
        this.label = label;
    }
}

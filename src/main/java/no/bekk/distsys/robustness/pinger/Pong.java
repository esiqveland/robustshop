package no.bekk.distsys.robustness.pinger;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Pong {

    @JsonProperty
    private String answer;

    public String getAnswer() {
        return answer;
    }

    public Pong withAnswer(String answer) {
        this.answer = answer;
        return this;
    }

}

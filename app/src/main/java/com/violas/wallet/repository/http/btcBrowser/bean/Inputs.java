
package com.violas.wallet.repository.http.btcBrowser.bean;;
import java.util.List;


public class Inputs {

    private String prev_hash;
    private int output_index;
    private long output_value;
    private long sequence;
    private String script_type;
    private long age;
    private List<String> witness;
    public void setPrev_hash(String prev_hash) {
        this.prev_hash = prev_hash;
    }
    public String getPrev_hash() {
        return prev_hash;
    }

    public void setOutput_index(int output_index) {
        this.output_index = output_index;
    }
    public int getOutput_index() {
        return output_index;
    }

    public void setOutput_value(long output_value) {
        this.output_value = output_value;
    }
    public long getOutput_value() {
        return output_value;
    }

    public void setSequence(long sequence) {
        this.sequence = sequence;
    }
    public long getSequence() {
        return sequence;
    }

    public void setScript_type(String script_type) {
        this.script_type = script_type;
    }
    public String getScript_type() {
        return script_type;
    }

    public void setAge(long age) {
        this.age = age;
    }
    public long getAge() {
        return age;
    }

    public void setWitness(List<String> witness) {
        this.witness = witness;
    }
    public List<String> getWitness() {
        return witness;
    }

}
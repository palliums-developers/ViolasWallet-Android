
package com.violas.wallet.repository.http.bitcoinChainApi.bean;;
import java.util.List;


public class Outputs {

    private long value;
    private String script;
    private String spent_by;
    private List<String> addresses;
    private String script_type;
    public void setValue(long value) {
        this.value = value;
    }
    public long getValue() {
        return value;
    }

    public void setScript(String script) {
        this.script = script;
    }
    public String getScript() {
        return script;
    }

    public void setSpent_by(String spent_by) {
        this.spent_by = spent_by;
    }
    public String getSpent_by() {
        return spent_by;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }
    public List<String> getAddresses() {
        return addresses;
    }

    public void setScript_type(String script_type) {
        this.script_type = script_type;
    }
    public String getScript_type() {
        return script_type;
    }

}
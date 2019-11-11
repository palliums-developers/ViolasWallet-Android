
package com.violas.wallet.repository.http.bitcoinChainApi.bean;;
import java.util.List;
import java.util.Date;


public class TXBeanCypher {

    private String block_hash;
    private long block_height;
    private int block_index;
    private String hash;
    private String hex;
    private List<String> addresses;
    private long total;
    private int fees;
    private int size;
    private String preference;
    private String relayed_by;
    private Date confirmed;
    private Date received;
    private int ver;
    private long lock_time;
    private boolean double_spend;
    private int vin_sz;
    private int vout_sz;
    private int confirmations;
    private int confidence;
    private List<Inputs> inputs;
    private List<Outputs> outputs;
    public void setBlock_hash(String block_hash) {
        this.block_hash = block_hash;
    }
    public String getBlock_hash() {
        return block_hash;
    }

    public void setBlock_height(long block_height) {
        this.block_height = block_height;
    }
    public long getBlock_height() {
        return block_height;
    }

    public void setBlock_index(int block_index) {
        this.block_index = block_index;
    }
    public int getBlock_index() {
        return block_index;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }
    public String getHash() {
        return hash;
    }

    public void setHex(String hex) {
        this.hex = hex;
    }
    public String getHex() {
        return hex;
    }

    public void setAddresses(List<String> addresses) {
        this.addresses = addresses;
    }
    public List<String> getAddresses() {
        return addresses;
    }

    public void setTotal(long total) {
        this.total = total;
    }
    public long getTotal() {
        return total;
    }

    public void setFees(int fees) {
        this.fees = fees;
    }
    public int getFees() {
        return fees;
    }

    public void setSize(int size) {
        this.size = size;
    }
    public int getSize() {
        return size;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }
    public String getPreference() {
        return preference;
    }

    public void setRelayed_by(String relayed_by) {
        this.relayed_by = relayed_by;
    }
    public String getRelayed_by() {
        return relayed_by;
    }

    public void setConfirmed(Date confirmed) {
        this.confirmed = confirmed;
    }
    public Date getConfirmed() {
        return confirmed;
    }

    public void setReceived(Date received) {
        this.received = received;
    }
    public Date getReceived() {
        return received;
    }

    public void setVer(int ver) {
        this.ver = ver;
    }
    public int getVer() {
        return ver;
    }

    public void setLock_time(long lock_time) {
        this.lock_time = lock_time;
    }
    public long getLock_time() {
        return lock_time;
    }

    public void setDouble_spend(boolean double_spend) {
        this.double_spend = double_spend;
    }
    public boolean getDouble_spend() {
        return double_spend;
    }

    public void setVin_sz(int vin_sz) {
        this.vin_sz = vin_sz;
    }
    public int getVin_sz() {
        return vin_sz;
    }

    public void setVout_sz(int vout_sz) {
        this.vout_sz = vout_sz;
    }
    public int getVout_sz() {
        return vout_sz;
    }

    public void setConfirmations(int confirmations) {
        this.confirmations = confirmations;
    }
    public int getConfirmations() {
        return confirmations;
    }

    public void setConfidence(int confidence) {
        this.confidence = confidence;
    }
    public int getConfidence() {
        return confidence;
    }

    public void setInputs(List<Inputs> inputs) {
        this.inputs = inputs;
    }
    public List<Inputs> getInputs() {
        return inputs;
    }

    public void setOutputs(List<Outputs> outputs) {
        this.outputs = outputs;
    }
    public List<Outputs> getOutputs() {
        return outputs;
    }

}
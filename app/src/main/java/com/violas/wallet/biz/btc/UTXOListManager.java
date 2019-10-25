package com.violas.wallet.biz.btc;

import android.util.Log;

import com.violas.wallet.repository.http.btcBrowser.bean.UTXO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class UTXOListManager {
    private static final ExecutorService EXECUTOR_UTXO_SERVICE = Executors.newSingleThreadExecutor();

    /**
     * UTXO 查询私钥余额的游标
     */
    private int mAddressCursor = 0;

    /**
     * 所有当前 UTXO 列表金额
     */
    private BigDecimal mAllAmount = new BigDecimal("0");

    /**
     * 准备要用的 UTXO 列表金额
     */
    private BigDecimal mUseAmount = new BigDecimal("0");

    /**
     * 想要拼凑的目标金额
     */
    private BigDecimal mTargetAmount = new BigDecimal("0");

    /**
     * 优先消费的 UTXO
     */
    private List<UTXO> mCustomUTXOList = Collections.synchronizedList(new LinkedList<>());

    /**
     * 可用的地址
     */
    private final List<String> mAddressList;

    /**
     * 所有可用 UTXO 列表
     */
    private Map<String, UTXO> mUTXOList = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * 准备使用的 UTXO 列表
     */
    private Map<String, UTXO> mUseUTXOList = Collections.synchronizedMap(new LinkedHashMap<>());


    public int getAddressCursor() {
        return mAddressCursor;
    }

    public static String generateKey(UTXO utxo) {
        return utxo.getTxid() + "_" + utxo.getVout();
    }

    public static String generateKey(String utxo, int index) {
        return utxo + "_" + index;
    }

    public static String getUtxoId(String utxoKey) {
        return utxoKey.split("_")[0];
    }

    public static String getUtxoVout(String utxoKey) {
        return utxoKey.split("_")[0];
    }

    /**
     * 可用的私钥
     */
    public UTXOListManager(List<String> AddressList) {
        mAddressList = AddressList;
    }

    public UTXOListManager(String address) {
        mAddressList = new ArrayList<>(1);
        mAddressList.add(address);
    }

    public UTXOListManager(List<String> AddressList, LinkedHashMap<String, UTXO> useUTXOList) {
        mAddressList = AddressList;
        for (String str : useUTXOList.keySet()) {
            mUTXOList.put(generateKey(useUTXOList.get(str)), useUTXOList.get(str));
        }
    }

    public void putUseUTXO(LinkedHashMap<String, UTXO> useUTXOList) {
        for (String str : useUTXOList.keySet()) {
            mUTXOList.put(generateKey(useUTXOList.get(str)), useUTXOList.get(str));
        }
    }

    /**
     * 检查可用余额有没有到达目标金额
     *
     * @return
     */
    public boolean checkBalance() {
        return mUseUTXOList.size() != 0;
    }

    public BigDecimal getTargetAmount() {
        return mTargetAmount;
    }

    public BigDecimal getUseAmount() {
        return mUseAmount;
    }


    /**
     * 获取 UTXO 列表
     *
     * @return
     */
    public List<UTXO> getUTXOList() {
        return new ArrayList<>(mUseUTXOList.values());
    }

    public void setCustomUTXOList(List<UTXO> customUTXOList) {
        mCustomUTXOList = customUTXOList;
        getForceAllAmount();
    }

    /**
     * 获取用到的 Custom UTXO 列表
     *
     * @return
     */
    public List<UTXO> getUseCustomUTXOList() {
        List<UTXO> utxos = new ArrayList<>();
        for (UTXO item : mCustomUTXOList) {
            UTXO utxo = mUseUTXOList.get(generateKey(item));
            if (utxo != null) {
                utxos.add(utxo);
            }
        }
        return utxos;
    }

    /**
     * 根据地址获取 UTXO 列表
     *
     * @return
     */
    public List<UTXO> getUTXOListByAddress(String Address) {
        ArrayList<UTXO> arrayList = new ArrayList<>();
        for (UTXO utxo : mUTXOList.values()) {
            if (utxo.getAddress().equals(arrayList)) {
                arrayList.add(utxo);
            }
        }
        return arrayList;
    }

    /**
     * 根据 UTXO ID 获取相应 UTXO
     *
     * @param utxoId
     * @return
     */
    public UTXO getUTXOById(String utxoId, int index) {
        UTXO userUtxo = mUseUTXOList.get(generateKey(utxoId, index));
        if (userUtxo == null) {
            userUtxo = mUTXOList.get(generateKey(utxoId, index));
        }
        if (userUtxo == null) {
            for (UTXO item : mCustomUTXOList) {
                if (item.getTxid().equals(utxoId) && item.getVout() == index) {
                    userUtxo = item;
                    return userUtxo;
                }
            }
        }
        return userUtxo;
    }

    /**
     * 尝试获取相应金额的 UTXO，可能获取的多，也可能会不够。
     *
     * @param amount 金额
     */
    public List<UTXO> togetherUTXO(double amount) {
        Map<String, UTXO> utxoList = new LinkedHashMap<>();
        getUTXOWhitNetwork(amount);

        mUseUTXOList.clear();
        mUseAmount = new BigDecimal("0");
        if (mAllAmount.compareTo(new BigDecimal(amount + "")) >= 0) {
            BigDecimal currentAmount = new BigDecimal("0");

            for (UTXO utxo : mCustomUTXOList) {
                utxoList.put(generateKey(utxo), utxo);
                currentAmount = currentAmount.add(new BigDecimal(utxo.getAmount() + ""));
                if (currentAmount.compareTo(new BigDecimal(amount + "")) >= 0) {
                    mUseUTXOList.putAll(utxoList);
                    mUseAmount = currentAmount;
                    return new ArrayList<>(mUseUTXOList.values());
                }
            }

            for (UTXO utxo : mUTXOList.values()) {
                utxoList.put(generateKey(utxo), utxo);
                currentAmount = currentAmount.add(new BigDecimal(utxo.getAmount() + ""));
                if (currentAmount.compareTo(new BigDecimal(amount + "")) >= 0) {
                    mUseUTXOList.putAll(utxoList);
                    mUseAmount = currentAmount;
                    return new ArrayList<>(mUseUTXOList.values());
                }
            }

            return new ArrayList<>(mUseUTXOList.values());
        } else {
            //余额不足
            return new ArrayList<>(mUseUTXOList.values());
        }
    }

    /**
     * 在网络环境下获取可用 UTXO
     *
     * @param amount 目标金额
     */
    private void getUTXOWhitNetwork(double amount) {
        mAllAmount = getForceAllAmount();
        if (mAllAmount.compareTo(new BigDecimal(amount + "")) >= 0) {
            return;
        }
        do {
            List<String> addressList = getNextSearch(mAddressCursor);
            mAddressCursor += addressList.size();
        } while (mAddressCursor < mAddressList.size()
                && mAllAmount.compareTo(new BigDecimal(amount + "")) < 0);
    }

    /**
     * 获取所有 UTXO 列表的金额
     * 可能价格不是特别
     *
     * @return 金额
     */
    public BigDecimal getAllAmount() {
        if (mAllAmount.compareTo(new BigDecimal("0")) != 1) {
            return getForceAllAmount();
        }
        return mAllAmount;
    }

    /**
     * 获取当前 UTXO 列表的金额
     * 强制刷新价格，效率不如 getAllAmount()
     *
     * @return 金额
     */
    public BigDecimal getForceAllAmount() {
        mAllAmount = new BigDecimal("0");
        for (UTXO utxo : mCustomUTXOList) {
            mAllAmount = mAllAmount.add(new BigDecimal(utxo.getAmount() + ""));
        }
        for (String next : mUTXOList.keySet()) {
            mAllAmount = mAllAmount.add(new BigDecimal(mUTXOList.get(next).getAmount() + ""));
        }
        return mAllAmount;
    }

    private List<String> getNextSearch(int cursor) {
        List<String> addressList = new ArrayList<>(2);
        String[] address = mAddressList.toArray(new String[0]);
        for (int i = cursor; (i < address.length && i < cursor + 2); i++) {
            Log.e("===", "search address " + address[i]);
            addressList.add(address[i]);
        }
        return addressList;
    }
}

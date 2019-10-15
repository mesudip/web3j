package org.web3j.tx.response;

public interface TransactionCallBack extends TransactionReceiptCallback {
    void transactionHash(String hash);
}

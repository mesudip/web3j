package org.web3j.tx.exceptions;

import org.web3j.protocol.core.methods.response.TransactionReceipt;

/**
 * Exception thrown when transaction receipt is received with return code 0x0
 * The transaction receipt can still be received using the {@link FailedReceiptException#getTransactionReceipt()}
 */
public class FailedReceiptException extends Exception {
    private final TransactionReceipt transactionReceipt;
    public FailedReceiptException(String message, TransactionReceipt transactionReceipt){
        super(message);
        this.transactionReceipt=transactionReceipt;
    }
    public TransactionReceipt getTransactionReceipt(){
        return transactionReceipt;
    }
}

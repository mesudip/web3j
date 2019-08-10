package org.web3j.tx.interactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.TransactionCallBack;
import org.web3j.utils.Async;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Interactions before submitting a transaction
 * You can override gasPrice and gasLimit obtained from the gasProvider here before sending the transactions.
 */
public final class InteractiveGetTransactionHash implements RemoteCall<InteractiveGetTransactionReceipt> {
    private static final Logger logger= LoggerFactory.getLogger(InteractiveGetTransactionHash.class);
    BigInteger gasPrice;
    BigInteger gasLimit;
    private final String data;
    private final BigInteger weiValue;
    private final String to;
    private final TransactionManager transactionManager;

    public InteractiveGetTransactionHash(TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger weiValue) {
        this.transactionManager = transactionManager;
        this.gasLimit = gasLimit;
        this.gasPrice = gasPrice;
        this.data = data;
        this.weiValue = weiValue;
        this.to = to;
    }

    public void setGasPriceInGWei(float price) {
        gasPrice = (BigDecimal.valueOf(price * 1_000_000_000).setScale(0, RoundingMode.FLOOR)).toBigInteger();
    }

    public InteractiveGetTransactionHash setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
        return this;
    }

    public InteractiveGetTransactionHash setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
        return this;
    }


    /**
     * This should be deprecated so that send() should not be used for getting TransactionReceipt directly.
     * Later the deprecation will be remed and the function should return an instance of InteractiveGetTransactionReceipt
     * @return
     * @throws IOException
     */

    public InteractiveGetTransactionReceipt send() throws IOException{
        return transactionManager.sendTransactionInteractive(this, to, data, weiValue, gasPrice, gasLimit);
    }

    /**
     * Perform the
     * @param callBack
     */
    public void sendAsync(TransactionCallBack callBack){
        Async.submit(()->{ send(callBack);});
    }

    /**
     * same as send() but the code about what to do after send is passed as parameter.
     * Note that this methods blocks the current thread until the all the operation is not complete.
     * @param callBack
     */
    public void send(TransactionCallBack callBack){
        try {
            InteractiveGetTransactionReceipt send = this.send();
            String transactionHash=send.getTransactionHash();
            try {
                callBack.transactionHash(transactionHash);
            }catch (Exception ex){
                logger.error("Unexpected exception on transactionHash callback",ex);
            }
            // send For callback will not throw exception.
            send.send(callBack);

        }catch (Exception ex){
            callBack.exception(ex);
        }
    }
}
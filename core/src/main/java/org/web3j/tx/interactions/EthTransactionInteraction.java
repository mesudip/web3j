package org.web3j.tx.interactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.methods.response.EthTransaction;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.response.TransactionCallBack;
import org.web3j.utils.Async;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * Interactions before submitting a transaction
 * You can override
 *  - gasPrice and gasLimit  instead of using from the gasProvicer
 *  - nounce
 */
public final class EthTransactionInteraction implements RemoteCall<EthTransactionReceiptInteraction> {
    private static final Logger logger= LoggerFactory.getLogger(EthTransactionInteraction.class);
    BigInteger gasPrice;
    BigInteger gasLimit;
    private final String data;
    private final BigInteger weiValue;
    private final String to;
    private final TransactionManager transactionManager;

    public EthTransactionInteraction(TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit, String to, String data, BigInteger weiValue) {
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

    public EthTransactionInteraction setGasPrice(BigInteger gasPrice) {
        this.gasPrice = gasPrice;
        return this;
    }

    public EthTransactionInteraction setGasLimit(BigInteger gasLimit) {
        this.gasLimit = gasLimit;
        return this;
    }

    public EthTransactionReceiptInteraction submit() throws  IOException{
        return transactionManager.submitTransaction(gasPrice, gasLimit,to, data, weiValue);
    }

    /**
     * This should be deprecated so that send() should not be used for getting TransactionReceipt directly.
     * Later the deprecation will be remed and the function should return an instance of EthTransactionReceiptInteraction
     * @return
     * @throws IOException
     * @Deprecated Deprecated in favor of .submit().waitForReceipt()
     */
    public EthTransactionReceiptInteraction send() throws IOException{
        return transactionManager.submitTransaction(gasPrice, gasLimit,to, data, weiValue);
    }

    /**
     * Perform the
     * @param callBack
     */
    public void sendAsync(TransactionCallBack callBack){
        Async.submit(()->{ submit(callBack);});
    }

    public String getTxData(){
        return data;
    }
    /**
     * same as send() but the code about what to do after send is passed as parameter.
     * Note that this methods blocks the current thread until the all the operation is not complete.
     * @param callBack
     */
    public void submit(TransactionCallBack callBack){
        try {
            EthTransactionReceiptInteraction send = this.send();
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
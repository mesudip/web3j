package org.web3j.tx.interactions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.web3j.crypto.Pair;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.EthSendTransaction;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.exceptions.TransactionException;
import org.web3j.tx.exceptions.FailedReceiptException;
import org.web3j.tx.response.TransactionReceiptCallback;
import org.web3j.tx.response.TransactionReceiptProcessor;
import org.web3j.utils.Async;
import org.web3j.utils.Numeric;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Optional;


public class EthTransactionReceiptInteraction implements RemoteCall<TransactionReceipt> {

    Logger logger = LoggerFactory.getLogger(EthTransactionReceiptInteraction.class);
    private final EthSendTransaction ethSendTransaction;
    private final TransactionReceiptProcessor transactionReceiptProcessor;
    private final EthTransactionInteraction ethTransactionInteraction;

    public EthTransactionReceiptInteraction(EthSendTransaction ethSendTransaction, TransactionReceiptProcessor transactionReceiptProcessor, EthTransactionInteraction ethTransactionInteraction) {
        this.ethSendTransaction = ethSendTransaction;
        this.transactionReceiptProcessor = transactionReceiptProcessor;
        this.ethTransactionInteraction = ethTransactionInteraction;
    }

    @Override
    public TransactionReceipt send() throws InterruptedException, IOException, TransactionException, FailedReceiptException {
        return waitForReceipt();
    }

    /**
     * Send Async request and expect callback.
     * This method returns immediately submitting the task to a background thread.
     * All the callbacks are handled by another thread.
     *
     * @param callback
     */
    public void sendAsync(TransactionReceiptCallback callback) {
        Async.submit(() -> {
            try {
                send(callback);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Use a callback mechanism to handle events.
     * Note that everything will be blocking and all the callbacks will be called on current thread.
     *
     * @param callback
     */
    public void send(TransactionReceiptCallback callback) throws InterruptedException {
        try {
            Pair<? extends TransactionReceipt, Optional<Response.Error>> optionalPair = waitAndProcessResponse();
            try {
                callback.success(optionalPair.getFirst(), optionalPair.getSecond());
                // this is important!
                // Once I had a condition where success function threw Runtime exception on last line.
                // then the exception handler was called,
                // This resulted both onSuccess and onFailure condition to be triggered.
            } catch (Exception ex) {
                logger.error("Callback function threw Unexpected Exception", ex);
            }
        } catch ( IOException | TransactionException e) {
            callback.exception(e);
        }
    }

    public TransactionReceipt waitForReceipt() throws IOException, TransactionException, InterruptedException, FailedReceiptException {
        Pair<? extends TransactionReceipt, Optional<Response.Error>> optionalPair = waitAndProcessResponse();
        if(optionalPair.getSecond().isPresent()){
            throw new FailedReceiptException(optionalPair.getSecond().get().getMessage(),optionalPair.getFirst());
        }
        else return optionalPair.getFirst();
    }

    public boolean hasError() {
        return ethSendTransaction.getError() != null;
    }

    public Response.Error getError() {
        return ethSendTransaction.getError();
    }

    public String getTransactionHash() {
        return ethSendTransaction.getTransactionHash();
    }

    public BigInteger getGasPrice() {
        return ethTransactionInteraction.gasPrice;
    }

    public BigInteger getGasLimit() {
        return ethTransactionInteraction.gasLimit;
    }

    private  Pair<? extends TransactionReceipt, Optional<Response.Error>> waitAndProcessResponse() throws TransactionException, InterruptedException, IOException {
        Pair<? extends TransactionReceipt, Optional<Response.Error>> optionalPair = transactionReceiptProcessor.waitForTransactionReceiptResponse(ethSendTransaction.getTransactionHash());
        if (!optionalPair.getSecond().isPresent() && !optionalPair.getFirst().isStatusOK()) {
            TransactionReceipt first = optionalPair.getFirst();
            Response.Error error = new Response.Error();
            error.setCode(Numeric.decodeQuantity(first.getStatus()).intValue());
            error.setData(first.getStatus());
            if (ethTransactionInteraction.gasPrice.equals(first.getGasUsed())) {
                error.setMessage("Transaction has failed with status: " + first.getStatus() + ".(not enough gas?)");
            } else {
                error.setMessage("Transaction has failed with status: " + first.getStatus()+ ". Gas used: "+first.getGasUsed());
            }
            return new Pair<>(first, Optional.of(error));
        }
        return optionalPair;
    }
}

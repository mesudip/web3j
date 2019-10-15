package org.web3j.tx.interactions;


import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;

import java.io.IOException;
import java.math.BigInteger;

/**
 * This class is created for making a ethCall.
 * Before sending the call the block number at which the call is to be executed can be set.
 *
 * @param <T> Type of response returned by the call
 */
public class EthCallInteraction<T> implements RemoteCall<T> {
    public interface CallResponseParser<T>{
        T processSendResponse( String data) throws ContractCallException;
    }

    protected final String data;
    protected final String to;
    protected final TransactionManager transactionManager;
    private DefaultBlockParameter blockParameter= DefaultBlockParameterName.LATEST;
    private   final CallResponseParser<T> callResponseParser;

    public EthCallInteraction(String data, String to, TransactionManager transactionManager, CallResponseParser<T> callResponseParser) {
        this.data = data;
        this.to = to;
        this.transactionManager = transactionManager;
        this.callResponseParser=callResponseParser;
    }

    /**
     * Set the block number at which the call is to be executed.
     */
    public EthCallInteraction<T> atBlockNumber(BigInteger blockNumber){
        blockParameter= DefaultBlockParameter.valueOf(blockNumber);
        return this;
    }

    /**
     * Set the block number at which the call is to be executed.
     */
    public EthCallInteraction<T> atBlockNumber(long blockNumber){
        return atBlockNumber(BigInteger.valueOf(blockNumber));
    }

    public T send() throws IOException, ContractCallException {
        String s = transactionManager.sendCall(to, data, blockParameter);
        if(s==null){
            throw new ContractCallException("Empty value (0x) returned from contract");
        }
        return callResponseParser.processSendResponse(s);
    }

}


/*
 * Copyright 2019 Web3 Labs LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.web3j.tx.response;

import java.io.IOException;
import java.util.Optional;

import org.web3j.crypto.Pair;
import org.web3j.protocol.core.Response;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.eea.Eea;
import org.web3j.protocol.exceptions.TransactionException;

public class PollingPrivateTransactionReceiptProcessor extends PrivateTransactionReceiptProcessor {
    private final long sleepDuration;
    private final int attempts;

    public PollingPrivateTransactionReceiptProcessor(Eea eea, long sleepDuration, int attempts) {
        super(eea);
        this.sleepDuration = sleepDuration;
        this.attempts = attempts;
    }

    @Override
    public Pair<TransactionReceipt, Optional<Response.Error>> waitForTransactionReceiptResponse(String transactionHash) throws IOException, TransactionException {
        Pair<? extends Optional<? extends TransactionReceipt>, Optional<Response.Error>> receiptOptional = sendTransactionReceiptAcceptError(transactionHash);
        for (int i = 0; i < attempts; i++) {
            if (!receiptOptional.getFirst().isPresent()) {
                try {
                    Thread.sleep(sleepDuration);
                } catch (InterruptedException e) {
                    throw new TransactionException(e);
                }
                receiptOptional = sendTransactionReceiptAcceptError(transactionHash);
            } else {
                return new Pair<>(receiptOptional.getFirst().get(),receiptOptional.getSecond());
            }
        }
        throw new TransactionException(
                "Transaction receipt was not generated after "
                        + ((sleepDuration * attempts) / 1000
                        + " seconds for transaction: "
                        + transactionHash),
                transactionHash);
    }

    @Deprecated
    @Override
    public TransactionReceipt waitForTransactionReceipt(String transactionHash)
            throws IOException, TransactionException {

        Pair<TransactionReceipt, Optional<Response.Error>> optionalPair = waitForTransactionReceiptResponse(transactionHash);
        if(optionalPair.getSecond().isPresent()){
            Response.Error error = optionalPair.getSecond().get();
                throw new TransactionException(
                        "Error processing request: " + error.getMessage());
        }
        return optionalPair.getFirst();
    }
}

package org.web3j.tx.interactions;

import org.web3j.tx.TransactionManager;

public class RawResponseEthCallInteraction extends EthCallInteraction<String> {

    public RawResponseEthCallInteraction(String data, String to, TransactionManager transactionManager) {
        super(data, to, transactionManager, (data1)->{return data1;});
    }
}

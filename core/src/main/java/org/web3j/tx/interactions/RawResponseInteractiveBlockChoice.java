package org.web3j.tx.interactions;

import org.web3j.tx.TransactionManager;
import org.web3j.tx.exceptions.ContractCallException;

public class RawResponseInteractiveBlockChoice extends InteractiveBlockChoice<String>{

    public RawResponseInteractiveBlockChoice(String data, String to, TransactionManager transactionManager) {
        super(data, to, transactionManager, (data1)->{return data1;});
    }
}

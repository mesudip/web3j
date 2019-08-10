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
package org.web3j.protocol.scenarios;

import java.math.BigInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;
import org.junit.Test;

import org.web3j.generated.HumanStandardToken;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.response.TransactionReceipt;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.web3j.generated.HumanStandardToken.ApprovalEventResponse;
import static org.web3j.generated.HumanStandardToken.TransferEventResponse;
import static org.web3j.generated.HumanStandardToken.deploy;
import static org.web3j.tx.TransactionManager.DEFAULT_POLLING_FREQUENCY;

/** Generated HumanStandardToken integration test for all supported scenarios. */
public class HumanStandardTokenGeneratedIT extends Scenario {

    @Test
    public void testContract() throws Exception {
        BigInteger aliceQty = BigInteger.valueOf(1_000_000);
        final String aliceAddress = ALICE.getAddress();
        final String bobAddress = BOB.getAddress();

        HumanStandardToken contract =
                deploy(
                                web3j,
                                ALICE,
                                GAS_PRICE,
                                GAS_LIMIT,
                                aliceQty,
                                "web3j tokens",
                                BigInteger.valueOf(18),
                                "w3j$")
                        .send();

        assertTrue(contract.isValid());

        assertThat(contract.totalSupply().send(), equalTo(aliceQty));

        assertThat(contract.balanceOf(ALICE.getAddress()).send(), equalTo(aliceQty));

        CountDownLatch transferEventCountDownLatch = new CountDownLatch(2);
        Disposable transferEventSubscription =
                contract.transferEventFlowable(
                                DefaultBlockParameterName.EARLIEST,
                                DefaultBlockParameterName.LATEST)
                        .subscribe(
                                transferEventResponse -> transferEventCountDownLatch.countDown());

        CountDownLatch approvalEventCountDownLatch = new CountDownLatch(1);
        Disposable approvalEventSubscription =
                contract.approvalEventFlowable(
                                DefaultBlockParameterName.EARLIEST,
                                DefaultBlockParameterName.LATEST)
                        .subscribe(
                                transferEventResponse -> transferEventCountDownLatch.countDown());

        // transfer tokens
        BigInteger transferQuantity = BigInteger.valueOf(100_000);

        TransactionReceipt aliceTransferReceipt =
                contract.transfer(BOB.getAddress(), transferQuantity).send().send();

        TransferEventResponse aliceTransferEventValues =
                contract.getTransferEvents(aliceTransferReceipt).get(0);

        assertThat(aliceTransferEventValues._from, equalTo(aliceAddress));
        assertThat(aliceTransferEventValues._to, equalTo(bobAddress));
        assertThat(aliceTransferEventValues._value, equalTo(transferQuantity));

        aliceQty = aliceQty.subtract(transferQuantity);

        BigInteger bobQty = BigInteger.ZERO;
        bobQty = bobQty.add(transferQuantity);

        assertThat(contract.balanceOf(ALICE.getAddress()).send(), equalTo(aliceQty));
        assertThat(contract.balanceOf(BOB.getAddress()).send(), equalTo(bobQty));

        // set an allowance
        assertThat(contract.allowance(aliceAddress, bobAddress).send(), equalTo(BigInteger.ZERO));

        transferQuantity = BigInteger.valueOf(50);
        TransactionReceipt approveReceipt =
                contract.approve(BOB.getAddress(), transferQuantity).send().send();

        ApprovalEventResponse approvalEventValues =
                contract.getApprovalEvents(approveReceipt).get(0);

        assertThat(approvalEventValues._owner, equalTo(aliceAddress));
        assertThat(approvalEventValues._spender, equalTo(bobAddress));
        assertThat(approvalEventValues._value, equalTo(transferQuantity));

        assertThat(contract.allowance(aliceAddress, bobAddress).send(), equalTo(transferQuantity));

        // perform a transfer as Bob
        transferQuantity = BigInteger.valueOf(25);

        // Bob requires his own contract instance
        HumanStandardToken bobsContract =
                HumanStandardToken.load(
                        contract.getContractAddress(), web3j, BOB, STATIC_GAS_PROVIDER);

        TransactionReceipt bobTransferReceipt =
                bobsContract.transferFrom(aliceAddress, bobAddress, transferQuantity).send().send();

        TransferEventResponse bobTransferEventValues =
                contract.getTransferEvents(bobTransferReceipt).get(0);
        assertThat(bobTransferEventValues._from, equalTo(aliceAddress));
        assertThat(bobTransferEventValues._to, equalTo(bobAddress));
        assertThat(bobTransferEventValues._value, equalTo(transferQuantity));

        aliceQty = aliceQty.subtract(transferQuantity);
        bobQty = bobQty.add(transferQuantity);

        assertThat(contract.balanceOf(aliceAddress).send(), equalTo(aliceQty));
        assertThat(contract.balanceOf(bobAddress).send(), equalTo(bobQty));

        transferEventCountDownLatch.await(DEFAULT_POLLING_FREQUENCY, TimeUnit.MILLISECONDS);
        approvalEventCountDownLatch.await(DEFAULT_POLLING_FREQUENCY, TimeUnit.MILLISECONDS);

        approvalEventSubscription.dispose();
        transferEventSubscription.dispose();
        Thread.sleep(1000);
        assertTrue(approvalEventSubscription.isDisposed());
        assertTrue(transferEventSubscription.isDisposed());
    }
}

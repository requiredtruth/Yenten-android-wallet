package com.yentenandroidwallet.wallet.wallets;

import com.yentenandroidwallet.core.BRCoreTransaction;
import com.yentenandroidwallet.core.ethereum.BREthereumTransaction;
import com.yentenandroidwallet.wallet.abstracts.BaseTransaction;

import java.math.BigDecimal;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/22/18.
 * Copyright (c) 2018 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class CryptoTransaction implements BaseTransaction {

    private BRCoreTransaction mCoreTx;
    private BREthereumTransaction mEtherTx;

    public CryptoTransaction(Object transaction) {
        if (transaction instanceof BRCoreTransaction) mCoreTx = (BRCoreTransaction) transaction;
        else if (transaction instanceof BREthereumTransaction)
            mEtherTx = (BREthereumTransaction) transaction;
    }

    @Override
    public BigDecimal getTxSize() {
        if (mCoreTx != null) return new BigDecimal(mCoreTx.getSize());
        else if (mEtherTx != null) return new BigDecimal(0);
        else return null;
    }

    @Override
    public BigDecimal getTxStandardFee() {
        if (mCoreTx != null) return new BigDecimal(mCoreTx.getStandardFee());
        else if (mEtherTx != null) return new BigDecimal(0);
        else return null;
    }

    @Override
    public BRCoreTransaction getCoreTx() {
        return mCoreTx;
    }

    @Override
    public BREthereumTransaction getEtherTx() {
        return mEtherTx;
    }
}

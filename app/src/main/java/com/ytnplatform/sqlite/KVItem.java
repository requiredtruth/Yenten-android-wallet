package com.ytnplatform.sqlite;


import android.util.Log;

import com.ytnplatform.kvstore.CompletionObject;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 1/13/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class KVItem {
    public static final String TAG = KVItem.class.getName();

    public long version;
    public long remoteVersion;
    public String key;
    public byte[] value;
    public long time;
    public int deleted;
    public CompletionObject.RemoteKVStoreError err;

    public KVItem(long version, long remoteVersion, String key, byte[] value, long time, int deleted, CompletionObject.RemoteKVStoreError err) {
        this.version = version;
        this.remoteVersion = remoteVersion;
        this.key = key;
        this.value = value;
        this.time = time;
        this.deleted = deleted;
        this.err = err;
    }

    public KVItem(long version, long remoteVersion, String key, byte[] value, long time, int deleted) {
        this.version = version;
        this.remoteVersion = remoteVersion;
        this.key = key;
        this.value = value;
        this.time = time;
        this.deleted = deleted;
    }

    public void printValues() {
        Log.e(TAG, "KVItem values:");
        Log.e(TAG, "version: " + version);
        Log.e(TAG, "remoteVersion: " + remoteVersion);
        Log.e(TAG, "key: " + key);
        Log.e(TAG, "value.length: " + value.length);
        Log.e(TAG, "time: " + time);
        Log.e(TAG, "deleted: " + deleted);
    }

    private KVItem() {
    }

}

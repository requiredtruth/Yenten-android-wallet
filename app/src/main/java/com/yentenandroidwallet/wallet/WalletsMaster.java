package com.yentenandroidwallet.wallet;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.yentenandroidwallet.YentenApp;
import com.yentenandroidwallet.R;
import com.yentenandroidwallet.core.BRCoreKey;
import com.yentenandroidwallet.core.BRCoreMasterPubKey;
import com.yentenandroidwallet.presenter.customviews.BRDialogView;
import com.yentenandroidwallet.tools.animation.BRAnimator;
import com.yentenandroidwallet.tools.animation.BRDialog;
import com.yentenandroidwallet.tools.manager.BRReportsManager;
import com.yentenandroidwallet.tools.manager.BRSharedPrefs;
import com.yentenandroidwallet.tools.security.BRKeyStore;
import com.yentenandroidwallet.tools.threads.executor.BRExecutor;
import com.yentenandroidwallet.tools.util.BRConstants;
import com.yentenandroidwallet.tools.util.Bip39Reader;
import com.yentenandroidwallet.tools.util.TrustedNode;
import com.yentenandroidwallet.tools.util.Utils;
import com.yentenandroidwallet.wallet.abstracts.BaseWalletManager;
import com.yentenandroidwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.yentenandroidwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.yentenandroidwallet.wallet.wallets.etherium.WalletEthManager;
import com.ytnplatform.entities.WalletInfo;
import com.ytnplatform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
 * Copyright (c) 2016 breadwallet LLC
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

public class WalletsMaster {
    private static final String TAG = WalletsMaster.class.getName();

    private static WalletsMaster instance;

    private List<BaseWalletManager> mWallets = new ArrayList<>();

    private WalletsMaster(Context app) {
    }

    public synchronized static WalletsMaster getInstance(Context app) {
        if (instance == null) {
            instance = new WalletsMaster(app);
        }
        return instance;
    }

    public List<BaseWalletManager> getAllWallets() {
        return mWallets;
    }

    //return the needed wallet for the iso
    public BaseWalletManager getWalletByIso(Context app, String iso) {
//        Log.d(TAG, "getWalletByIso() Getting wallet by ISO -> " + iso);
        if (Utils.isNullOrEmpty(iso))
            throw new RuntimeException("getWalletByIso with iso = null, Cannot happen!");
        if (iso.equalsIgnoreCase("YTN"))
            return WalletBitcoinManager.getInstance(app);
        //if (iso.equalsIgnoreCase("BCH"))
        //    return WalletBchManager.getInstance(app);
        //if (iso.equalsIgnoreCase("ETH"))
        //    return WalletEthManager.getInstance(app);
        return null;
    }

    public BaseWalletManager getCurrentWallet(Context app) {
        return getWalletByIso(app, BRSharedPrefs.getCurrentWalletIso(app));
    }

    //get the total fiat balance held in all the wallets in the smallest unit (e.g. cents)
    public BigDecimal getAggregatedFiatBalance(Context app) {
        BigDecimal totalBalance = new BigDecimal(0);
        for (BaseWalletManager wallet : mWallets) {
            totalBalance = totalBalance.add(wallet.getFiatBalance(app));
        }
        return totalBalance;
    }

    public synchronized boolean generateRandomSeed(final Context ctx) {
        SecureRandom sr = new SecureRandom();
        final String[] words;
        List<String> list;
        String languageCode = Locale.getDefault().getLanguage();
        if (languageCode == null) languageCode = "en";
        list = Bip39Reader.bip39List(ctx, languageCode);
        words = list.toArray(new String[list.size()]);
        final byte[] randomSeed = sr.generateSeed(16);
        if (words.length != 2048) {
            BRReportsManager.reportBug(new IllegalArgumentException("the list is wrong, size: " + words.length), true);
            return false;
        }
        if (randomSeed.length != 16)
            throw new NullPointerException("failed to create the seed, seed length is not 128: " + randomSeed.length);
        byte[] paperKeyBytes = BRCoreMasterPubKey.generatePaperKey(randomSeed, words);
        if (paperKeyBytes == null || paperKeyBytes.length == 0) {
            BRReportsManager.reportBug(new NullPointerException("failed to encodeSeed"), true);
            return false;
        }
        String[] splitPhrase = new String(paperKeyBytes).split(" ");
        if (splitPhrase.length != 12) {
            BRReportsManager.reportBug(new NullPointerException("phrase does not have 12 words:" + splitPhrase.length + ", lang: " + languageCode), true);
            return false;
        }
        boolean success = false;
        try {
            success = BRKeyStore.putPhrase(paperKeyBytes, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            return false;
        }
        if (!success) return false;
        byte[] phrase;
        try {
            phrase = BRKeyStore.getPhrase(ctx, 0);
        } catch (UserNotAuthenticatedException e) {
            throw new RuntimeException("Failed to retrieve the phrase even though at this point the system auth was asked for sure.");
        }
        if (Utils.isNullOrEmpty(phrase)) throw new NullPointerException("phrase is null!!");
        if (phrase.length == 0) throw new RuntimeException("phrase is empty");
        byte[] seed = BRCoreKey.getSeedFromPhrase(phrase);
        if (seed == null || seed.length == 0) throw new RuntimeException("seed is null");
        byte[] authKey = BRCoreKey.getAuthPrivKeyForAPI(seed);
        if (authKey == null || authKey.length == 0) {
            BRReportsManager.reportBug(new IllegalArgumentException("authKey is invalid"), true);
        }
        BRKeyStore.putAuthKey(authKey, ctx);
        int walletCreationTime = (int) (System.currentTimeMillis() / 1000);
        BRKeyStore.putWalletCreationTime(walletCreationTime, ctx);
        final WalletInfo info = new WalletInfo();
        info.creationDate = walletCreationTime;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                KVStoreManager.getInstance().putWalletInfo(ctx, info); //push the creation time to the kv store
            }
        });

        //store the serialized in the KeyStore
        byte[] pubKey = new BRCoreMasterPubKey(paperKeyBytes, true).serialize();
        BRKeyStore.putMasterPublicKey(pubKey, ctx);

        return true;

    }

    public boolean isIsoCrypto(Context app, String iso) {
        if(iso.equalsIgnoreCase("eth")) return true;
        if(iso.equalsIgnoreCase("btc")) return true;
        if(iso.equalsIgnoreCase("bch")) return true;
        for (BaseWalletManager w : mWallets) {
            if (w.getIso(app).equalsIgnoreCase(iso)) return true;
        }
        return false;
    }

    public boolean wipeKeyStore(Context context) {
        Log.d(TAG, "wipeKeyStore");
        return BRKeyStore.resetWalletKeyStore(context);
    }

    /**
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(ctx, 0);
                //if not authenticated, an error will be thrown and returned false, so no worry about mistakenly removing the wallet
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (UserNotAuthenticatedException e) {
                return false;
            }

        }
        return false;
    }

    public boolean noWalletForPlatform(Context ctx) {
        byte[] pubkey = BRKeyStore.getMasterPublicKey(ctx);
        return pubkey == null || pubkey.length == 0;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
//        return keyguardManager.isKeyguardSecure();
        return true;
    }

    public boolean isNetworkAvailable(Context ctx) {
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    public void wipeWalletButKeystore(final Context ctx) {
        Log.d(TAG, "wipeWalletButKeystore");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                for (BaseWalletManager wallet : mWallets) {
                    wallet.wipeData(ctx);
                }
//                wipeAll(ctx);
            }
        });
    }

    public void wipeAll(Context app) {
        wipeKeyStore(app);
        wipeWalletButKeystore(app);
    }

    public void refreshBalances(Context app) {
        for (BaseWalletManager wallet : mWallets) {
            wallet.refreshCachedBalance(app);
        }

    }

    public void initWallets(final Context app) {
        if (!mWallets.contains(WalletBitcoinManager.getInstance(app)))
            mWallets.add(WalletBitcoinManager.getInstance(app));
        for (BaseWalletManager wm : mWallets) {
            if (wm != null) setSpendingLimitIfNotSet(app, wm);
        }
//                }
        //        if (!mWallets.contains(WalletBchManager.getInstance(app)))
//            mWallets.add(WalletBchManager.getInstance(app));
//        if (!mWallets.contains(WalletEthManager.getInstance(app))) {
//            BaseWalletManager ethWallet = WalletEthManager.getInstance(app);
//            mWallets.add(ethWallet);
//
//            if (ethWallet != null) {
//                YentenApp.generateWalletId();

//            }
//        }
    }

    private void setSpendingLimitIfNotSet(final Context app, final BaseWalletManager wm) {
        if (app == null) return;

        BigDecimal limit = BRKeyStore.getTotalLimit(app, wm.getIso(app));
        if (limit.compareTo(new BigDecimal(0)) == 0) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
                    BigDecimal totalSpent = wallet == null ? new BigDecimal(0) : wallet.getTotalSent(app);
                    BigDecimal totalLimit = totalSpent.add(BRKeyStore.getSpendLimit(app, wm.getIso(app)));
                    BRKeyStore.putTotalLimit(app, totalLimit, wm.getIso(app));
                }
            });

        }
    }

    @WorkerThread
    public void initLastWallet(Context app) {
        if (app == null) {
            app = YentenApp.getBreadContext();
            if (app == null) {
                Log.e(TAG, "initLastWallet: FAILED, app is null");
                return;
            }
        }
        BaseWalletManager wallet = getWalletByIso(app, BRSharedPrefs.getCurrentWalletIso(app));
        if (wallet == null) wallet = getWalletByIso(app, "YTN");
        wallet.connect(app);
    }

    @WorkerThread
    public void updateFixedPeer(Context app, BaseWalletManager wm) {
        String node = BRSharedPrefs.getTrustNode(app, wm.getIso(app));
        if (!Utils.isNullOrEmpty(node)) {
            String host = TrustedNode.getNodeHost(node);
            int port = TrustedNode.getNodePort(node);
//        Log.e(TAG, "trust onClick: host:" + host);
//        Log.e(TAG, "trust onClick: port:" + port);
            boolean success = wm.useFixedNode(host, port);
            if (!success) {
                Log.e(TAG, "updateFixedPeer: Failed to updateFixedPeer with input: " + node);
            } else {
                Log.d(TAG, "updateFixedPeer: succeeded");
            }
        }
        wm.connect(app);

    }

    public void startTheWalletIfExists(final Activity app) {
        final WalletsMaster m = WalletsMaster.getInstance(app);
        if (!m.isPasscodeEnabled(app)) {
            //Device passcode/password should be enabled for the app to work
            BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.Prompts_NoScreenLock_body_android),
                    app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            app.finish();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            app.finish();
                        }
                    }, 0);
        } else {
            if (!m.noWallet(app)) {
                BRAnimator.startBreadActivity(app, true);
            }
            //else just sit in the intro screen

        }
    }

}

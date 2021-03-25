package com.yentenandroidwallet;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.IntentFilter;
import android.graphics.Point;
import android.hardware.fingerprint.FingerprintManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import com.bugsnag.android.Bugsnag;
import com.yentenandroidwallet.presenter.activities.util.BRActivity;
import com.yentenandroidwallet.tools.crypto.Base32;
import com.yentenandroidwallet.tools.crypto.CryptoHelper;
import com.yentenandroidwallet.tools.listeners.SyncReceiver;
import com.yentenandroidwallet.tools.manager.InternetManager;
import com.yentenandroidwallet.tools.threads.executor.BRExecutor;
import com.yentenandroidwallet.tools.util.BRConstants;
import com.yentenandroidwallet.tools.util.Utils;
import com.yentenandroidwallet.wallet.WalletsMaster;
import com.yentenandroidwallet.wallet.abstracts.BaseWalletManager;
//import com.google.firebase.crash.FirebaseCrash;
//import com.google.firebase.crash.FirebaseCrash;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.ytnplatform.APIClient.BREAD_POINT;
import static org.apache.commons.compress.utils.CharsetNames.ISO_8859_1;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
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

public class YentenApp extends Application {
    private static final String TAG = YentenApp.class.getName();
    public static int DISPLAY_HEIGHT_PX;
    FingerprintManager mFingerprintManager;
    // host is the server(s) on which the API is hosted
    public static String HOST = "deactivated.com";
    private static List<OnAppBackgrounded> listeners;
    private static Timer isBackgroundChecker;
    public static AtomicInteger activityCounter = new AtomicInteger();
    public static long backgroundedTime;
    public static boolean appInBackground;
    private static Context mContext;

    private static final String PACKAGE_NAME = YentenApp.getBreadContext() == null ? null : YentenApp.getBreadContext().getApplicationContext().getPackageName();

    static {
        try {
            System.loadLibrary(BRConstants.NATIVE_LIB_NAME);
        } catch (UnsatisfiedLinkError e) {
            e.printStackTrace();
            Log.d(TAG, "Native code library failed to load.\\n\" + " + e);
            Log.d(TAG, "Installer Package Name -> " + (PACKAGE_NAME == null ? "null" : YentenApp.getBreadContext().getPackageManager().getInstallerPackageName(PACKAGE_NAME)));
        }
    }

    public static final boolean IS_ALPHA = false;

    public static final Map<String, String> mHeaders = new HashMap<>();

    private static Activity currentActivity;

    private static FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Utils.isEmulatorOrDebug(this)) {
//          BRKeyStore.putFailCount(0, this);
//          HOST = "stage2.breadwallet.com";
            HOST = "deactivated.com";
        }

        Bugsnag.init(this);
//            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
//                @Override
//                public void run() {
//                    FirebaseCrash.setCrashCollectionEnabled(false);
//
//                }
//            });

        mContext = this;

        // [START shared_app_measurement]
        // Obtain the FirebaseAnalytics instance.
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        // [END shared_app_measurement]

        if (!Utils.isEmulatorOrDebug(this) && IS_ALPHA)
            throw new RuntimeException("can't be alpha for release");

        boolean isTestVersion = BREAD_POINT.contains("staging") || BREAD_POINT.contains("stage");
        boolean isTestNet = BuildConfig.BITCOIN_TESTNET;
        String lang = getCurrentLocale(this);

        mHeaders.put("X-Is-Internal", IS_ALPHA ? "true" : "false");
        mHeaders.put("X-Testflight", isTestVersion ? "true" : "false");
        mHeaders.put("X-Bitcoin-Testnet", isTestNet ? "true" : "false");
        mHeaders.put("Accept-Language", lang);


        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Display display = wm.getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int DISPLAY_WIDTH_PX = size.x;
        DISPLAY_HEIGHT_PX = size.y;
        mFingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);

        registerReceiver(InternetManager.getInstance(), new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));

//        addOnBackgroundedListener(new OnAppBackgrounded() {
//            @Override
//            public void onBackgrounded() {
//
//            }
//        });

    }


    @TargetApi(Build.VERSION_CODES.N)
    public String getCurrentLocale(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return ctx.getResources().getConfiguration().getLocales().get(0).getLanguage();
        } else {
            //noinspection deprecation
            return ctx.getResources().getConfiguration().locale.getLanguage();
        }
    }

    public static Map<String, String> getBreadHeaders() {
        return mHeaders;
    }

    public static FirebaseAnalytics getFirebaseInstance() {
        return mFirebaseAnalytics;
    }

    public static Context getBreadContext() {
        Context app = currentActivity;
        if (app == null) app = SyncReceiver.app;
        if (app == null) app = mContext;
        return app;
    }

    public static void setBreadContext(Activity app) {
        currentActivity = app;
    }

    public static synchronized void fireListeners() {
        if (listeners == null) return;
        List<OnAppBackgrounded> copy = listeners;
        for (OnAppBackgrounded lis : copy) if (lis != null) lis.onBackgrounded();
    }

    public static void addOnBackgroundedListener(OnAppBackgrounded listener) {
        if (listeners == null) listeners = new ArrayList<>();
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public static boolean isAppInBackground(final Context context) {
        return context == null || activityCounter.get() <= 0;
    }

    //call onStop on every activity so
    public static void onStop(final BRActivity app) {

        if (isBackgroundChecker != null) isBackgroundChecker.cancel();
        isBackgroundChecker = new Timer();
        TimerTask backgroundCheck = new TimerTask() {
            @Override
            public void run() {
                if (isAppInBackground(app)) {
                    backgroundedTime = System.currentTimeMillis();
                    Log.e(TAG, "App went in background!");
                    // APP in background, do something
                    fireListeners();
                    isBackgroundChecker.cancel();
                }
                // APP in foreground, do something else
            }
        };

        isBackgroundChecker.schedule(backgroundCheck, 500, 500);
    }

    public interface OnAppBackgrounded {
        void onBackgrounded();
    }
}

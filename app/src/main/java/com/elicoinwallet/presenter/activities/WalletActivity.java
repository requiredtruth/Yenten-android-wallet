package com.elicoinwallet.presenter.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ViewFlipper;

import com.elicoinwallet.R;
import com.elicoinwallet.presenter.activities.settings.WebViewActivity;
import com.elicoinwallet.presenter.activities.util.BRActivity;
import com.elicoinwallet.presenter.customviews.BRButton;
import com.elicoinwallet.presenter.customviews.BRNotificationBar;
import com.elicoinwallet.presenter.customviews.BRSearchBar;
import com.elicoinwallet.presenter.customviews.BRText;
import com.elicoinwallet.tools.animation.BRAnimator;
import com.elicoinwallet.tools.animation.BRDialog;
import com.elicoinwallet.tools.manager.BRSharedPrefs;
import com.elicoinwallet.tools.manager.FontManager;
import com.elicoinwallet.tools.manager.InternetManager;
import com.elicoinwallet.tools.manager.TxManager;
import com.elicoinwallet.tools.services.SyncService;
import com.elicoinwallet.tools.sqlite.CurrencyDataSource;
import com.elicoinwallet.tools.threads.executor.BRExecutor;
import com.elicoinwallet.tools.util.CurrencyUtils;
import com.elicoinwallet.tools.util.Utils;
import com.elicoinwallet.wallet.WalletsMaster;
import com.elicoinwallet.wallet.abstracts.BaseWalletManager;
import com.elicoinwallet.wallet.abstracts.OnBalanceChangedListener;
import com.elicoinwallet.wallet.abstracts.OnTxListModified;
import com.elicoinwallet.wallet.abstracts.SyncListener;
import com.elicoinwallet.wallet.util.CryptoUriParser;
import com.eliplatform.HTTPServer;

import java.math.BigDecimal;

import static com.elicoinwallet.tools.animation.BRAnimator.t1Size;
import static com.elicoinwallet.tools.animation.BRAnimator.t2Size;

/**
 * Created by byfieldj on 1/16/18.
 * <p>
 * <p>
 * This activity will display pricing and transaction information for any currency the user has access to
 * (BTC, BCH, ETH)
 */

public class WalletActivity extends BRActivity implements InternetManager.ConnectionReceiverListener, OnTxListModified {
    private static final String TAG = WalletActivity.class.getName();
    BRText mCurrencyTitle;
    BRText mCurrencyPriceUsd;
    BRText mBalancePrimary;
    BRText mBalanceSecondary;
    Toolbar mToolbar;
    ImageButton mBackButton;
    BRButton mSendButton;
    BRButton mReceiveButton;
    BRButton mBuyButton;
    BRText mBalanceLabel;
    BRText mProgressLabel;
    ProgressBar mProgressBar;

    public ViewFlipper barFlipper;
    private BRSearchBar searchBar;
    private ImageButton mSearchIcon;
    private ImageButton mSwap;
    private ConstraintLayout toolBarConstraintLayout;

    private BRNotificationBar mNotificationBar;

    private static WalletActivity app;

    private InternetManager mConnectionReceiver;
    private SyncNotificationBroadcastReceiver mSyncNotificationBroadcastReceiver;
    private String mCurrentWalletIso;

    private TestLogger logger;

    public static WalletActivity getApp() {
        return app;
    }


    private BaseWalletManager mWallet;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_wallet);

        mCurrencyTitle = findViewById(R.id.currency_label);
        mCurrencyPriceUsd = findViewById(R.id.currency_usd_price);
        mBalancePrimary = findViewById(R.id.balance_primary);
        mBalanceSecondary = findViewById(R.id.balance_secondary);
        mToolbar = findViewById(R.id.bread_bar);
        mBackButton = findViewById(R.id.back_icon);
        mSendButton = findViewById(R.id.send_button);
        mReceiveButton = findViewById(R.id.receive_button);
        mBuyButton = findViewById(R.id.buy_button);
        barFlipper = findViewById(R.id.tool_bar_flipper);
        searchBar = findViewById(R.id.search_bar);
        mSearchIcon = findViewById(R.id.search_icon);
        toolBarConstraintLayout = findViewById(R.id.bread_toolbar);
        mSwap = findViewById(R.id.swap);
        mBalanceLabel = findViewById(R.id.balance_label);
        mProgressLabel = findViewById(R.id.syncing_label);
        mProgressBar = findViewById(R.id.sync_progress);
        mNotificationBar = findViewById(R.id.notification_bar);

        if (Utils.isEmulatorOrDebug(this)) {
            if (logger != null) logger.interrupt();
            logger = new TestLogger(); //Sync logger
            logger.start();
        }

        setUpBarFlipper();

        BRAnimator.init(this);
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);//make it the size it should be after animation to get the X
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);//make it the size it should be after animation to get the X

        BRAnimator.init(this);
        mBalancePrimary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 28);//make it the size it should be after animation to get the X
        mBalanceSecondary.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);//make it the size it should be after animation to get the X


        mSendButton.setHasShadow(false);
        mSendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showSendFragment(WalletActivity.this, null);

            }
        });

        mSendButton.setHasShadow(false);
        mReceiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                BRAnimator.showReceiveFragment(WalletActivity.this, true);

            }
        });

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
                finish();
            }
        });

        mSearchIcon.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!BRAnimator.isClickAllowed()) return;
                barFlipper.setDisplayedChild(1); //search bar
                searchBar.onShow(true);
            }
        });


        mBalancePrimary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });
        mBalanceSecondary.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                swap();
            }
        });

        TxManager.getInstance().init(this);

        onConnectionChanged(InternetManager.getInstance().isConnected(this));

        updateUi();

        // Check if the "Twilight" screen altering app is currently running
        if (Utils.checkIfScreenAlteringAppIsRunning(this, "com.urbandroid.lux")) {
            BRDialog.showSimpleDialog(this, "Screen Altering App Detected", getString(R.string.Android_screenAlteringMessage));
        }

        mWallet = WalletsMaster.getInstance(this).getCurrentWallet(this);

        boolean cryptoPreferred = BRSharedPrefs.isCryptoPreferred(this);

        setPriceTags(cryptoPreferred, false);


    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mConnectionReceiver != null)
            unregisterReceiver(mConnectionReceiver);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        //since we have one instance of activity at all times, this is needed to know when a new intent called upon this activity
        handleUrlClickIfNeeded(intent);
    }

    private void handleUrlClickIfNeeded(Intent intent) {
        Uri data = intent.getData();
        if (data != null && !data.toString().isEmpty()) {
            //handle external click with crypto scheme
            CryptoUriParser.processRequest(this, data.toString(), WalletsMaster.getInstance(this).getCurrentWallet(this));
        }
    }

    private void updateUi() {
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        if (wallet == null) {
            Log.e(TAG, "updateUi: wallet is null");
            return;
        }

        if (wallet.getUiConfiguration().buyVisible) {
            mBuyButton.setHasShadow(false);
            mBuyButton.setVisibility(View.VISIBLE);
            mBuyButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.buy_link)));
                    startActivity(browserIntent);
                    app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
                }
            });

        } else {
            LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Utils.getPixelsFromDps(this, 65), 1.5f
            );

            LinearLayout.LayoutParams param2 = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    Utils.getPixelsFromDps(this, 65), 1.5f
            );
            param.gravity = Gravity.CENTER;
            param2.gravity = Gravity.CENTER;

            param.setMargins(Utils.getPixelsFromDps(this, 8), Utils.getPixelsFromDps(this, 8), Utils.getPixelsFromDps(this, 8), 0);
            param2.setMargins(0, Utils.getPixelsFromDps(this, 8), Utils.getPixelsFromDps(this, 8), 0);

            mSendButton.setLayoutParams(param);
            mReceiveButton.setLayoutParams(param2);
            mBuyButton.setVisibility(View.GONE);

        }

        String fiatExchangeRate = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), wallet.getFiatExchangeRate(this));
        String fiatBalance = CurrencyUtils.getFormattedAmount(this, BRSharedPrefs.getPreferredFiatIso(this), wallet.getFiatBalance(this));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(this, wallet.getIso(this), wallet.getCachedBalance(this));

        mCurrencyTitle.setText(wallet.getName(this));
        mCurrencyPriceUsd.setText(String.format("%s per %s", fiatExchangeRate, wallet.getIso(this)));
        mBalancePrimary.setText(fiatBalance);
        mBalanceSecondary.setText(cryptoBalance);
        mToolbar.setBackgroundColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        mSendButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        mBuyButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));
        mReceiveButton.setColor(Color.parseColor(wallet.getUiConfiguration().colorHex));

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                TxManager.getInstance().updateTxList(WalletActivity.this);
            }
        });

    }

    private void swap() {
        if (!BRAnimator.isClickAllowed()) return;
        BRSharedPrefs.setIsCryptoPreferred(WalletActivity.this, !BRSharedPrefs.isCryptoPreferred(WalletActivity.this));
        setPriceTags(BRSharedPrefs.isCryptoPreferred(WalletActivity.this), true);
    }


    private void setPriceTags(final boolean cryptoPreferred, boolean animate) {
        ConstraintSet set = new ConstraintSet();
        set.clone(toolBarConstraintLayout);
        if (animate)
            TransitionManager.beginDelayedTransition(toolBarConstraintLayout);
        int px8 = Utils.getPixelsFromDps(this, 8);

        // CRYPTO on RIGHT
        if (cryptoPreferred) {
            // Align crypto balance to the right parent
            set.connect(R.id.balance_secondary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px8);
            mBalancePrimary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 22), 0, 0);
            mBalanceSecondary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 12), 0, 0);

            // Align swap icon to left of crypto balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_secondary, ConstraintSet.START, px8);

            // Align usd balance to left of swap icon
            set.connect(R.id.balance_primary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);

            mBalanceSecondary.setTextSize(t1Size);
            mBalancePrimary.setTextSize(t2Size);

            set.applyTo(toolBarConstraintLayout);

        }

        // CRYPTO on LEFT
        else {
            // Align primary to right of parent
            set.connect(R.id.balance_primary, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END, px8);

            // Align swap icon to left of usd balance
            set.connect(R.id.swap, ConstraintSet.END, R.id.balance_primary, ConstraintSet.START, px8);


            // Align secondary currency to the left of swap icon
            set.connect(R.id.balance_secondary, ConstraintSet.END, R.id.swap, ConstraintSet.START, px8);
            mBalancePrimary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 12), 0, 0);
            mBalanceSecondary.setPadding(0, Utils.getPixelsFromDps(WalletActivity.this, 22), 0, 0);


            mBalanceSecondary.setTextSize(t2Size);
            mBalancePrimary.setTextSize(t1Size);

            set.applyTo(toolBarConstraintLayout);
        }
        mBalanceSecondary.setTextColor(getResources().getColor(cryptoPreferred ? R.color.white : R.color.currency_subheading_color, null));
        mBalancePrimary.setTextColor(getResources().getColor(cryptoPreferred ? R.color.currency_subheading_color : R.color.white, null));
        mBalanceSecondary.setTypeface(FontManager.get(this, cryptoPreferred ? "CircularPro-Bold.otf" : "CircularPro-Book.otf"));
        mBalancePrimary.setTypeface(FontManager.get(this, !cryptoPreferred ? "CircularPro-Bold.otf" : "CircularPro-Book.otf"));

        new Handler().postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                }, 300);
    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                WalletsMaster.getInstance(app).initWallets(app);
            }
        });

        setupNetworking();

        TxManager.getInstance().onResume(this);

        CurrencyDataSource.getInstance(this).addOnDataChangedListener(new CurrencyDataSource.OnDataChanged() {
            @Override
            public void onChanged() {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });
            }
        });
        final BaseWalletManager wallet = WalletsMaster.getInstance(this).getCurrentWallet(this);
        wallet.addTxListModifiedListener(this);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                wallet.refreshCachedBalance(app);
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });
                if (wallet.getConnectStatus() != 2)
                    wallet.connect(WalletActivity.this);

            }
        });
        wallet.addBalanceChangedListener(new OnBalanceChangedListener() {
            @Override
            public void onBalanceChanged(String iso, BigDecimal newBalance) {
                wallet.refreshCachedBalance(WalletActivity.this);
                updateUi();
            }
        });

        mCurrentWalletIso = wallet.getIso(WalletActivity.this);

        wallet.addSyncListeners(new SyncListener() {
            @Override
            public void syncStopped(String err) {

            }

            @Override
            public void syncStarted() {
                SyncService.startService(WalletActivity.this.getApplicationContext(), SyncService.ACTION_START_SYNC_PROGRESS_POLLING, mCurrentWalletIso);
            }
        });

        mSyncNotificationBroadcastReceiver = new SyncNotificationBroadcastReceiver();
        SyncService.registerSyncNotificationBroadcastReceiver(WalletActivity.this.getApplicationContext(), mSyncNotificationBroadcastReceiver);
        SyncService.startService(this.getApplicationContext(), SyncService.ACTION_START_SYNC_PROGRESS_POLLING, mCurrentWalletIso);

        handleUrlClickIfNeeded(getIntent());

    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.removeConnectionListener(this);
        SyncService.unregisterSyncNotificationBroadcastReceiver(WalletActivity.this.getApplicationContext(), mSyncNotificationBroadcastReceiver);
    }

    private void setUpBarFlipper() {
        barFlipper.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_enter));
        barFlipper.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.flipper_exit));
    }

    public void resetFlipper() {
        barFlipper.setDisplayedChild(0);
    }

    private void setupNetworking() {
        if (mConnectionReceiver == null)
            mConnectionReceiver = InternetManager.getInstance();
        IntentFilter mNetworkStateFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(mConnectionReceiver, mNetworkStateFilter);
        InternetManager.addConnectionListener(this);
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged");
        if (isConnected) {
            if (barFlipper != null && barFlipper.getDisplayedChild() == 2) {
                barFlipper.setDisplayedChild(0);
            }
            SyncService.startService(this.getApplicationContext(), SyncService.ACTION_START_SYNC_PROGRESS_POLLING, mCurrentWalletIso);

        }
//        Deactivate notification bar with Broken internet connection alert
//        else {
//            if (barFlipper != null)
//                barFlipper.setDisplayedChild(2);
//        }
    }

    @Override
    public void onBackPressed() {
        int c = getFragmentManager().getBackStackEntryCount();
        if (c > 0) {
            super.onBackPressed();
            return;
        }
        Intent intent = new Intent(this, HomeActivity.class);
        startActivity(intent);
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        if (!isDestroyed()) {
            finish();
        }
    }

    @Override
    public void txListModified(String hash) {
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateUi();
            }
        });
    }

    public void updateSyncProgress(double progress) {
        mProgressBar.setProgress((int) (progress * 100));
        if (progress == 1) {
            mProgressBar.setVisibility(View.GONE);
            mProgressLabel.setVisibility(View.GONE);
            mBalanceLabel.setVisibility(View.VISIBLE);
            mProgressBar.invalidate();
        } else {
            mProgressBar.setVisibility(View.VISIBLE);
            mProgressLabel.setVisibility(View.VISIBLE);
            mBalanceLabel.setVisibility(View.GONE);
            mProgressBar.invalidate();
        }
    }

    /**
     * The {@link SyncNotificationBroadcastReceiver} is responsible for receiving updates from the
     * {@link SyncService} and updating the UI accordingly.
     */
    private class SyncNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncService.ACTION_SYNC_PROGRESS_UPDATE.equals(intent.getAction())) {
                String intentWalletIso = intent.getStringExtra(SyncService.EXTRA_WALLET_ISO);
                double progress = intent.getDoubleExtra(SyncService.EXTRA_PROGRESS, SyncService.PROGRESS_NOT_DEFINTED);
                if (mCurrentWalletIso.equals(intentWalletIso)) {
                    if (progress >= SyncService.PROGRESS_START) {
                        WalletActivity.this.updateSyncProgress(progress);
                    } else {
                        Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Progress not set:" + progress);
                    }
                } else {
                    Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Wrong wallet. Expected:" + mCurrentWalletIso + " Actual:" + intentWalletIso + " Progress:" + progress);
                }
            }
        }
    }

    //test logger
    class TestLogger extends Thread {
        private static final String TAG = "TestLogger";

        @Override
        public void run() {
            super.run();

            while (true) {
                boolean needsLog = false;
                StringBuilder builder = new StringBuilder();
                for (BaseWalletManager w : WalletsMaster.getInstance(WalletActivity.this).getAllWallets()) {
                    builder.append("   " + w.getIso(WalletActivity.this));
                    String connectionStatus = "";
                    if (w.getConnectStatus() == 2)
                        connectionStatus = "Connected";
                    else if (w.getConnectStatus() == 0)
                        connectionStatus = "Disconnected";
                    else if (w.getConnectStatus() == 1)
                        connectionStatus = "Connecting";

                    double progress = w.getSyncProgress(BRSharedPrefs.getStartHeight(WalletActivity.this, w.getIso(WalletActivity.this)));
                    if (progress != 1) needsLog = true;
                    builder.append(" - " + connectionStatus + " " + progress * 100 + "%     ");

                }
                if (needsLog)
                    Log.e(TAG, "testLog: " + builder.toString());

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}

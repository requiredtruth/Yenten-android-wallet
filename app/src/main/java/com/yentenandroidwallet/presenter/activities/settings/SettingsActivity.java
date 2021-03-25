package com.yentenandroidwallet.presenter.activities.settings;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;

import com.yentenandroidwallet.R;
import com.yentenandroidwallet.presenter.activities.CurrencySettingsActivity;
import com.yentenandroidwallet.presenter.activities.UpdatePinActivity;
import com.yentenandroidwallet.presenter.activities.util.BRActivity;
import com.yentenandroidwallet.presenter.entities.BRSettingsItem;
import com.yentenandroidwallet.tools.adapter.SettingsAdapter;
import com.yentenandroidwallet.tools.manager.BRSharedPrefs;
import com.yentenandroidwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.yentenandroidwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.yentenandroidwallet.wallet.wallets.etherium.WalletEthManager;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends BRActivity {
    private static final String TAG = SettingsActivity.class.getName();
    private ListView listView;
    public List<BRSettingsItem> items;
    public static boolean appVisible = false;
    private static SettingsActivity app;

    public static SettingsActivity getApp() {
        return app;
    }

    private ImageButton mBackButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        listView = findViewById(R.id.settings_list);

        mBackButton = findViewById(R.id.back_button);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

    }


    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        if (items == null)
            items = new ArrayList<>();
        items.clear();

        populateItems();
        listView.addFooterView(new View(this), null, true);
        listView.setAdapter(new SettingsAdapter(this, R.layout.settings_list_item, items));
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void populateItems() {

        items.add(new BRSettingsItem(getString(R.string.Settings_wallet), "", null, true, 0));


        items.add(new BRSettingsItem(getString(R.string.Settings_wipe), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, UnlinkActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_preferences), "", null, true, 0));

        items.add(new BRSettingsItem(getString(R.string.UpdatePin_updateTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, UpdatePinActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        items.add(new BRSettingsItem(getString(R.string.Settings_currency), BRSharedPrefs.getPreferredFiatIso(this), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, DisplayCurrencyActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_currencySettings), "", null, true, 0));

        final WalletBitcoinManager btcWallet = WalletBitcoinManager.getInstance(app);
        if (btcWallet.getSettingsConfiguration().mSettingList.size() > 0)
            items.add(new BRSettingsItem(btcWallet.getName(app), "", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(SettingsActivity.this, CurrencySettingsActivity.class);
                    BRSharedPrefs.putCurrentWalletIso(app, btcWallet.getIso(app)); //change the current wallet to the one they enter settings to
                    startActivity(intent);
                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }, false, R.drawable.chevron_right_light));
//        final WalletBchManager bchWallet = WalletBchManager.getInstance(app);
//        if (bchWallet.getSettingsConfiguration().mSettingList.size() > 0)
//            items.add(new BRSettingsItem(WalletBchManager.getInstance(app).getName(app), "", new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Intent intent = new Intent(SettingsActivity.this, CurrencySettingsActivity.class);
//                    BRSharedPrefs.putCurrentWalletIso(app, bchWallet.getIso(app));//change the current wallet to the one they enter settings to
//                    startActivity(intent);
//                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//                }
//            }, false, R.drawable.chevron_right_light));
//        final WalletEthManager ethWallet = WalletEthManager.getInstance(app);
//        if (ethWallet.getSettingsConfiguration().mSettingList.size() > 0)
//            items.add(new BRSettingsItem(WalletEthManager.getInstance(app).getName(app), "", new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    Intent intent = new Intent(SettingsActivity.this, CurrencySettingsActivity.class);
//                    BRSharedPrefs.putCurrentWalletIso(app, ethWallet.getIso(app));//change the current wallet to the one they enter settings to
//                    startActivity(intent);
//                    overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//                }
//            }, false, R.drawable.chevron_right_light));


        items.add(new BRSettingsItem(getString(R.string.Settings_other), "", null, true, 0));

//        String shareAddOn = BRSharedPrefs.getShareData(SettingsActivity.this) ? getString(R.string.PushNotifications_on) : getString(R.string.PushNotifications_off);

//        items.add(new BRSettingsItem(getString(R.string.Settings_shareData), shareAddOn, new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(SettingsActivity.this, ShareDataActivity.class);
//                startActivity(intent);
//                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
//            }
//        }, false, R.drawable.chevron_right_light));

        items.add(new BRSettingsItem(getString(R.string.Settings_review), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    Intent appStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.yentenandroidwallet"));
                    appStoreIntent.setPackage("com.android.vending");

                    startActivity(appStoreIntent);
                } catch (android.content.ActivityNotFoundException exception) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.yentenandroidwallet")));
                }
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.arrow_leave));

        items.add(new BRSettingsItem(getString(R.string.About_title), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AboutActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));

        items.add(new BRSettingsItem(getString(R.string.Settings_advancedTitle), "", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SettingsActivity.this, AdvancedActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }, false, R.drawable.chevron_right_light));


    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }
}

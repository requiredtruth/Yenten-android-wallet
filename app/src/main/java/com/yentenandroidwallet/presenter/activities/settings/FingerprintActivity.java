package com.yentenandroidwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.yentenandroidwallet.R;
import com.yentenandroidwallet.presenter.activities.util.BRActivity;
import com.yentenandroidwallet.presenter.customviews.BRDialogView;
import com.yentenandroidwallet.presenter.interfaces.BRAuthCompletion;
import com.yentenandroidwallet.tools.animation.BRAnimator;
import com.yentenandroidwallet.tools.animation.BRDialog;
import com.yentenandroidwallet.tools.manager.BRSharedPrefs;
import com.yentenandroidwallet.tools.security.AuthManager;
import com.yentenandroidwallet.tools.security.BRKeyStore;
import com.yentenandroidwallet.tools.util.BRConstants;
import com.yentenandroidwallet.tools.util.CurrencyUtils;
import com.yentenandroidwallet.tools.util.Utils;
import com.yentenandroidwallet.wallet.WalletsMaster;
import com.yentenandroidwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;


public class FingerprintActivity extends BRActivity {
    private static final String TAG = FingerprintActivity.class.getName();

    public RelativeLayout layout;
    public static boolean appVisible = false;
    private static FingerprintActivity app;
    private TextView limitExchange;
    private TextView limitInfo;

    private ToggleButton toggleButton;

    public static FingerprintActivity getApp() {
        return app;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fingerprint);
        toggleButton = findViewById(R.id.toggleButton);
        limitExchange = findViewById(R.id.limit_exchange);
        limitInfo = findViewById(R.id.limit_info);

        ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(FingerprintActivity.this).getCurrentWallet(FingerprintActivity.this);
                BRAnimator.showSupportFragment(FingerprintActivity.this, BRConstants.enableFingerprint, wm);
            }
        });

        toggleButton.setChecked(BRSharedPrefs.getUseFingerprint(this));

        limitExchange.setText(getLimitText());

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Activity app = FingerprintActivity.this;
                if (isChecked && !Utils.isFingerprintEnrolled(app)) {
                    BRDialog.showCustomDialog(app, getString(R.string.TouchIdSettings_disabledWarning_title_android),
                            getString(R.string.TouchIdSettings_disabledWarning_body_android), getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                    buttonView.setChecked(false);
                } else {
                    BRSharedPrefs.putUseFingerprint(app, isChecked);
                }

            }
        });
        SpannableString ss = new SpannableString(getString(R.string.TouchIdSettings_customizeText_android));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {

                AuthManager.getInstance().authPrompt(FingerprintActivity.this, null, getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        Intent intent = new Intent(FingerprintActivity.this, SpendLimitActivity.class);
                        overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                        startActivity(intent);
                        finish();
                    }

                    @Override
                    public void onCancel() {

                    }
                });


            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        //start index of the last space (beginning of the last word)
        int indexOfSpace = limitInfo.getText().toString().lastIndexOf(" ");
        // make the whole text clickable if failed to select the last word
        ss.setSpan(clickableSpan, indexOfSpace == -1 ? 0 : indexOfSpace, limitInfo.getText().length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        limitInfo.setText(ss);
        limitInfo.setMovementMethod(LinkMovementMethod.getInstance());
        limitInfo.setHighlightColor(Color.TRANSPARENT);

    }

    private String getLimitText() {
        String iso = BRSharedPrefs.getPreferredFiatIso(this);
        //amount in satoshis

        BaseWalletManager wm = WalletsMaster.getInstance(this).getCurrentWallet(this);
        BigDecimal cryptoLimit = BRKeyStore.getSpendLimit(this, wm.getIso(this));
        //amount in smallest crypto amount (satoshis, wei)
//        BigDecimal amount = wm.getFiatForSmallestCrypto(this, cryptoLimit, null);
        BigDecimal amount = cryptoLimit;
        //amount in user preferred ISO (e.g. USD)
        BigDecimal curAmount = wm.getFiatForSmallestCrypto(this, cryptoLimit, null);
        //formatted string for the label
        return String.format(getString(R.string.TouchIdSettings_spendingLimit),
                CurrencyUtils.getFormattedAmount(this, wm.getIso(this), amount), CurrencyUtils.getFormattedAmount(this, iso, curAmount));
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    public void onPause() {
        super.onPause();
        appVisible = false;
    }

}

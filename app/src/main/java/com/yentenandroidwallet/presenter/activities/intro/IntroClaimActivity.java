package com.yentenandroidwallet.presenter.activities.intro;


import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import com.yentenandroidwallet.R;
import com.yentenandroidwallet.presenter.activities.util.BRActivity;
import com.yentenandroidwallet.tools.manager.BRSharedPrefs;

public class IntroClaimActivity extends BRActivity {
    private Button acceptButton;
    private Button declineButton;
    public static boolean appVisible = false;
    private static IntroClaimActivity app;

    public static IntroClaimActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro_claim);
        acceptButton = (Button) findViewById(R.id.btnAccept);
        declineButton = (Button) findViewById(R.id.btnDecline);
        acceptButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BRSharedPrefs.putClaimApproved(IntroClaimActivity.this, true);
                IntroClaimActivity.this.finish();
            }
        });
        declineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                IntroClaimActivity.this.finishAffinity();
                moveTaskToBack(true);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }
    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

//    @Override
//    public void onBackPressed() {
//        super.onBackPressed();
//        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
//    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }
}

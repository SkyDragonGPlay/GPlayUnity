package com.unity3d.player;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;


public class UnityPlayerProxyActivity extends Activity {
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        Intent intent = new Intent(this, UnityPlayerActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        Bundle extras = this.getIntent().getExtras();
        if (extras != null) {
            intent.putExtras(extras);
        }
        this.startActivity(intent);
    }
}


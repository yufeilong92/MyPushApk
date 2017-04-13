package com.lawyee.mypushapk;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.huawei.android.pushagent.api.PushManager;
import com.huawei.hms.api.ConnectionResult;
import com.huawei.hms.api.HuaweiApiAvailability;
import com.huawei.hms.api.HuaweiApiClient;
import com.huawei.hms.support.api.hwid.HuaweiId;
import com.huawei.hms.support.api.hwid.HuaweiIdSignInOptions;
import com.lawyee.mypushapk.Huawei.receiver.MyApplication;
import com.lawyee.mypushapk.Xiaomi.XiaoMiApplication;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class MainActivity extends AppCompatActivity implements HuaweiApiAvailability.OnUpdateListener {

    public static List<String> logList = new CopyOnWriteArrayList<String>();
    public static final String TAG = "测试结果";
    private HuaweiIdSignInOptions signInOptions = new
            HuaweiIdSignInOptions.Builder(HuaweiIdSignInOptions.DEFAULT_SIGN_IN).build();
    private HuaweiApiClient mClient;
    private boolean mResolvingError = false;
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    private String model;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        XGPushConfig.enableDebug(this, true);
        setContentView(R.layout.activity_main);
        XiaoMiApplication.setMainActivity(this);//小米推送测试
        MyApplication.instance().setMainActivity(this);//华为测试
        model = getModel();
        if (!model.equals(getResources().getString(R.string.Mi))
                && !model.equals(getResources().getString(R.string.HuaWei))) {
            Log.d(TAG, "华为推送");
            XGPushManager.registerPush(this, callback);
        }
    }

    /**
     * 信鸽推送
     */
    private XGIOperateCallback callback = new XGIOperateCallback() {
        @Override
        public void onSuccess(Object o, int i) {
            Log.d("TPush", "注册成功，设备token为：" + o);
        }

        @Override
        public void onFail(Object o, int i, String s) {
            Log.d("TPush", "注册失败，错误码：" + i + ",错误信息：" + s);
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        if (getModel().equals(R.string.HuaWei)) {
            Log.d(TAG, "==华为测试==");
            //申请token
            PushManager.requestToken(this);
            mClient = new HuaweiApiClient.Builder(this)
                    .addApi(HuaweiId.SIGN_IN_API, signInOptions)
                    .addConnectionCallbacks(callbacks)
                    .addOnConnectionFailedListener(listener)
                    .build();
            mClient.connect();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshLogInfo();//小米
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        XiaoMiApplication.setMainActivity(null);
    }


    /**
     * 华为申请token失败回调
     */

    private HuaweiApiClient.OnConnectionFailedListener listener = new HuaweiApiClient.OnConnectionFailedListener() {
        @Override
        public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
            if (mResolvingError) {
                return;
            }
            int errorCode = connectionResult.getErrorCode();
            HuaweiApiAvailability availability = HuaweiApiAvailability.getInstance();
            if (availability.isUserResolvableError(errorCode)) {
                mResolvingError = true;
                availability.resolveError(MainActivity.this, errorCode, REQUEST_RESOLVE_ERROR, MainActivity.this);
            }
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_RESOLVE_ERROR) {
            mResolvingError = false;
            int errorcode = HuaweiApiAvailability.getInstance().isHuaweiMobileServicesAvailable(this);
            if (errorcode == ConnectionResult.SUCCESS) {
                if (!mClient.isConnecting() && !mClient.isConnected()) {
                    mClient.connect();
                }
            } else {
                Log.d(TAG, "onActivityResult:回调错误 " + errorcode);
            }

        }
    }

    /**
     * 华为申请token
     */
    private HuaweiApiClient.ConnectionCallbacks callbacks = new HuaweiApiClient.ConnectionCallbacks() {
        @Override
        public void onConnected() {
            Log.d(TAG, "api 连接成功");
        }

        @Override
        public void onConnectionSuspended(int i) {
            Log.d(TAG, "onConnectionSuspended: 连接中失败错误代码" + i);
        }
    };

    @Override
    public void onUpdateFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "错误代码" + connectionResult.getErrorCode());
    }

    public void refreshLogInfo() {
        String AllLog = "";
        for (String log : logList) {
            AllLog = AllLog + log + "\n\n";
        }
        Log.d(TAG, "=========: " + AllLog);
    }

    private String getModel() {
        return Build.MANUFACTURER;

    }
}

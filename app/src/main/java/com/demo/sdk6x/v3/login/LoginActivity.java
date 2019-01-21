package com.demo.sdk6x.v3.login;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.wifi.WifiManager;
import android.nfc.Tag;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.demo.sdk6x.v3.R;
import com.demo.sdk6x.v3.app.DemoApp;
import com.demo.sdk6x.v3.constants.Constants;
import com.demo.sdk6x.v3.data.Config;
import com.demo.sdk6x.v3.data.TempData;
import com.demo.sdk6x.v3.live.LiveActivity;
import com.demo.sdk6x.v3.resource.ResourceListActivity;
import com.demo.sdk6x.v3.utils.Base64Utils;
import com.demo.sdk6x.v3.utils.CheckUtil;
import com.demo.sdk6x.v3.utils.PrintLog;
import com.demo.sdk6x.v3.utils.ThreadUtil;
import com.demo.sdk6x.v3.utils.UIUtil;
import com.hikvision.vmsnetsdk.ServInfo;
import com.hikvision.vmsnetsdk.ServerConfig;
import com.hikvision.vmsnetsdk.VMSNetSDK;

import java.util.concurrent.Executors;

public class LoginActivity extends Activity {
    public static final String TAG = "LoginActivity";

    /**
     * 发送消息的对象
     */
    private MsgHandler handler;

    /**
     * 用户名输入框
     */
    private EditText username;

    /**
     * 密码输入框
     */
    private EditText passwd;

    /**
     * 验证码输入框
     */
    private EditText verifCode;

    /**
     * 验证码图片
     */
    private ImageView verifCodeImg;

    /**
     * 登录按钮
     */
    private Button loginBtn;

    /**
     * 登录平台地址
     */
    private String servAddr = "";
    private String usertext="",pwdtext="";

    /**
     * 登录返回的数据
     */
    private ServInfo servInfo;

    /**
     * 服务器地址输入框
     */
    private EditText serverAddrEt;

    /**
     * 是否需要验证码
     */
    private boolean isNeedVerifCode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);

        initUI();

        initData();
    }

    /**
     * 初始化控件
     */
    private void initUI() {
        // TODO Auto-generated method stub
        username = (EditText) findViewById(R.id.username);
        passwd = (EditText) findViewById(R.id.passwd);
        verifCode = (EditText) findViewById(R.id.verifCode);
        verifCodeImg = (ImageView) findViewById(R.id.iv_verifCode);
        loginBtn = (Button) findViewById(R.id.loginbtn);

        serverAddrEt = (EditText) findViewById(R.id.server_addr_et);

        loginBtn.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                login();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNeedVerifCode) {
            refreshVerifCode();
        }
    }

    public void getData(){
        Intent intent=getIntent();
        if (intent!=null){
            usertext= intent.getStringExtra(Constants.IntentKey.USERNAME);
            usertext= CheckUtil.isEmpty(usertext)?Config.DEF_USERNAME:usertext;

            pwdtext= intent.getStringExtra(Constants.IntentKey.PASSWORD);
            pwdtext= CheckUtil.isEmpty(pwdtext)?Config.DEF_PASSWORD:pwdtext;

            servAddr= intent.getStringExtra(Constants.IntentKey.SERVERADDRESS);
        }
    }

    /**
     * 初始化数据
     */
    private void initData() {
        //获取外部传进来的登陆信息
        getData();

        // TODO Auto-generated method stub
        handler = new MsgHandler();
        servInfo = new ServInfo();

        // 为了方便测试，设置默认用户名密码
        username.setText(usertext);
        passwd.setText(pwdtext);
        // 登录平台地址
        servAddr = Config.getIns().getServerAddr();
        serverAddrEt.setText(servAddr);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                loginBtn.performClick();
            }
        },500);
    }

    protected void login() {
        // TODO Auto-generated method stub
        servAddr = serverAddrEt.getText().toString().trim();
        if (servAddr.length() <= 0) {
            UIUtil.showToast(this, R.string.serveraddr_empty_tip);
            return;
        }
        Config.getIns().setServerAddr(servAddr);
        final String userName = username.getText().toString().trim();
        final String password = passwd.getText().toString().trim();
        final String code = verifCode.getText().toString().trim();

        if (isNeedVerifCode && code.length() <= 0) {
            UIUtil.showToast(LoginActivity.this, R.string.verifCode_empty_tip);
            return;
        }

        if (userName.length() <= 0) {
            UIUtil.showToast(LoginActivity.this, R.string.username_empty_tip);
            return;
        }
        if (password.length() <= 0) {
            UIUtil.showToast(LoginActivity.this, R.string.password_empty_tip);
            return;
        }

        final String macAddress = DemoApp.getIns().getMacAddr();
        if (macAddress.length() == 0) {
            UIUtil.showToast(LoginActivity.this, R.string.macaddr_empty_tip);
            return;
        }
        PrintLog.log(LoginActivity.this, "login","测试！");
        handler.sendEmptyMessage(Constants.Login.SHOW_LOGIN_PROGRESS);
        // 新线程进行登录操作
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                //1、先获取服务器配置信息ServerConfig
                ServerConfig serverConfig = new ServerConfig();
                boolean getServerConfigSuccess = VMSNetSDK.getInstance().getServerConfig(servAddr, serverConfig, true);
                if (getServerConfigSuccess) {
                    boolean isSuccess = VMSNetSDK.getInstance().safeLoginOldPlatform(servAddr, userName, password,
                            macAddress, 3, "5060", code, servInfo.getVerifCodeKey() == null ? "" : servInfo.getVerifCodeKey(), servInfo, serverConfig);
                    if (servInfo != null) {
                        TempData.getInstance().setLoginData(servInfo);
                    }
                    dealOldLoginResult(servInfo, isSuccess);
                } else {
                    // 登录请求
                    boolean ret = VMSNetSDK.getInstance().login(servAddr, userName, password, 1, macAddress, 3, servInfo);
                    Log.d(TAG, "login:" + ret);
                    if (ret) {
                        TempData.getInstance().setLoginData(servInfo);
                        handler.sendEmptyMessage(Constants.Login.LOGIN_SUCCESS);
                    } else {
                        handler.sendEmptyMessage(Constants.Login.LOGIN_FAILED);
                    }
                }
            }
        });
    }

    public void dealOldLoginResult(ServInfo servInfo, boolean isSuccess) {
        if (servInfo == null) {
            handler.sendEmptyMessage(Constants.Login.LOGIN_FAILED);
            return;
        }

        if (isSuccess) {
            int code = servInfo.getSrcCode();
            handleLoginSuccess(code, servInfo);
            TempData.getInstance().setLoginData(servInfo);
        } else {
            handler.sendEmptyMessage(Constants.Login.LOGIN_FAILED);
        }
    }

    private void handleLoginSuccess(int successCode, ServInfo servInfo) {
        String errorDec = "";
        switch (successCode) {
            case Constants.LoginCode.SUCCESS_LOGIN:
            case Constants.LoginCode.SUCCESS_CODE_FIRST_LOGIN:
            case Constants.LoginCode.SUCCESS_CODE_PWD_WEAK:
            case Constants.LoginCode.SUCCESS_CODE_PWD_STALE:
                handler.sendEmptyMessage(Constants.Login.LOGIN_SUCCESS);
                break;
            case VMSNetSDK.VMSNETSDK_MSP_USEER_HAS_LOCK:
                errorDec = getApplication().getResources().getString(R.string.login_user_has_lock);
                sendHandleMessage(Constants.Login.LOGIN_FAIL_WITH_CODE, successCode, errorDec);
                break;
            case VMSNetSDK.VMSNETSDK_MSP_NEED_VERIFCODE:
                isNeedVerifCode = true;
                sendHandleMessage(Constants.Login.LOGIN_NEED_VERIFCODE, successCode, servInfo);
                break;
            case VMSNetSDK.VMSNETSDK_MSP_VERIFCODE_ERR:
                isNeedVerifCode = true;
                sendHandleMessage(Constants.Login.LOGIN_CHECK_FAIL_VERIFYCODE_ERR, successCode, servInfo);
                break;
            case VMSNetSDK.VMSNETSDK_MSP_VERIFCODE_INVALIDE:
                isNeedVerifCode = true;
                sendHandleMessage(Constants.Login.LOGIN_CHECK_FAIL_VERIFYCODE_INVALIDE, successCode, servInfo);
                break;
            default:
                handler.sendEmptyMessage(Constants.Login.LOGIN_FAILED);
                break;
        }
    }

    public void sendHandleMessage(int what, int arg1, Object obj) {
        if (null == handler) {
            Log.e(TAG, "the handler is null.");
            return;
        }
        Message message = Message.obtain();
        message.what = what;
        message.arg1 = arg1;
        message.obj = obj;
        handler.sendMessage(message);
    }

    /**
     * 登录失败
     */
    public void onLoginFailed() {
        cancelProgress();
        UIUtil.showToast(this, getString(R.string.login_failed, UIUtil.getErrorDesc()));
    }

    /**
     * 登录成功
     */
    public void onLoginSuccess() {
        cancelProgress();
        UIUtil.showToast(this, R.string.login_suc_tip);
        // 跳转到获取控制中心列表界面
        // gotoResourceListActivity();
        Intent intent = new Intent(LoginActivity.this, ResourceListActivity.class);
        startActivity(intent);
    }

    /**
     * 登录进度条
     */
    private void showLoginProgress() {
        UIUtil.showProgressDialog(this, R.string.login_process_tip);
    }

    /**
     * 取消进度条
     */
    private void cancelProgress() {
        UIUtil.cancelProgressDialog();
    }


    private final class MsgHandler extends Handler {

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case Constants.Login.SHOW_LOGIN_PROGRESS:
                    showLoginProgress();
                    break;
                case Constants.Login.CANCEL_LOGIN_PROGRESS:
                    cancelProgress();
                    break;
                case Constants.Login.LOGIN_SUCCESS:
                    // 登录成功
                    onLoginSuccess();
                    break;
                case Constants.Login.LOGIN_FAILED:
                    // 登录失败
                    onLoginFailed();
                    break;
                case Constants.Login.LOGIN_FAIL_WITH_CODE:
                    cancelProgress();
                    UIUtil.showToast(LoginActivity.this, msg.obj.toString());
                    break;
                case Constants.Login.LOGIN_NEED_VERIFCODE:
                    cancelProgress();
                    UIUtil.showToast(LoginActivity.this, R.string.login_user_password_error);
                    passwd.setText("");//清空密码
                    ServInfo servInfotemp = (ServInfo) msg.obj;
                    if (TextUtils.isEmpty(servInfotemp.getVerifCode()) || TextUtils.isEmpty(servInfotemp.getVerifCodeKey())) {
                        refreshVerifCode();
                    } else {
                        showVerifCode(servInfotemp);
                    }
                    break;
                case Constants.Login.LOGIN_CHECK_FAIL_VERIFYCODE_ERR:
                    cancelProgress();
                    UIUtil.showToast(LoginActivity.this, R.string.login_verifcode_err);
                    passwd.setText("");//清空密码
                    refreshVerifCode();
                    break;
                case Constants.Login.LOGIN_CHECK_FAIL_VERIFYCODE_INVALIDE:
                    cancelProgress();
                    UIUtil.showToast(LoginActivity.this, R.string.login_verifcode_invalide);
                    passwd.setText("");//清空密码
                    refreshVerifCode();
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * 展示验证码
     *
     * @param servInfo
     */
    private void showVerifCode(ServInfo servInfo) {
        this.servInfo = servInfo;

        if (verifCode.getVisibility() != View.VISIBLE) {
            verifCode.setVisibility(View.VISIBLE);
            verifCodeImg.setVisibility(View.VISIBLE);
        } else {
            verifCode.setText("");
        }
        if (servInfo != null) {
            verifCodeImg.setImageBitmap(getBitmap(Base64Utils.decode(servInfo.getVerifCode())));
        }
    }

    /**
     * 刷新验证码
     */
    public void refreshVerifCode() {
        // 执行登录方法
        ThreadUtil.execute(new Runnable() {
            @Override
            public void run() {
                // TODO Auto-generated method stub
                final ServInfo servInfo = new ServInfo();
                if (VMSNetSDK.getInstance().getVerifCode(servAddr, servInfo)) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showVerifCode(servInfo);
                        }
                    });
                } else {
                    UIUtil.showToast(LoginActivity.this, R.string.verifcode_refresh_fail);
                }
            }
        });
    }

    private Bitmap getBitmap(byte[] decode) {
        if (decode == null) {
            return null;
        }
        return decode.length != 0 ? BitmapFactory.decodeByteArray(decode, 0, decode.length) : null;
    }
}

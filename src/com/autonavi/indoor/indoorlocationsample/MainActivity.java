package com.autonavi.indoor.indoorlocationsample;

import com.autonavi.indoor.constant.Configuration;
import com.autonavi.indoor.constant.MessageCode;
import com.autonavi.indoor.entity.LocationResult;
import com.autonavi.indoor.location.ILocationManager;
import com.autonavi.indoor.onlinelocation.OnlineLocator;
import com.autonavi.indoor.util.PackageHelper;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;

import java.lang.ref.WeakReference;


public class MainActivity extends Activity {
    boolean mIsLocating = false;
    TextView mTextView1;

    // 当前要进行定位的建筑物
    public String strBuildNameId = "";
    // 初始化定位引擎的时候需要的回调handler
    SDKInitHandler mSDKInitHandler = null;
    // 定位引擎接口
    ILocationManager mLocationManager;
    // 配置信息
    Configuration mConfiguration;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView1 = ((TextView)findViewById(R.id.textView1));
        mTextView1.setText("请先初始化引擎");

        // 使用com.autonavi.indoor.onlinelocation.LocationManager或者com.autonavi.indoor.onlinelocation.OnlineLocator
        mLocationManager = OnlineLocator.getInstance();
        mSDKInitHandler = new SDKInitHandler(this);
        mConfiguration = createConfigBuilder(this).build();

        (findViewById(R.id.btnInit)).setOnClickListener(new ButtonClick());
        (findViewById(R.id.btnRegist)).setOnClickListener(new ButtonClick());
        (findViewById(R.id.btnUnregist)).setOnClickListener(new ButtonClick());
        (findViewById(R.id.btnUninit)).setOnClickListener(new ButtonClick());
    }    

    class ButtonClick implements View.OnClickListener
    {
        public void onClick(View v)
        {
            switch (v.getId()) {
                case R.id.btnInit:
                    // 初始化定位引擎，在使用定位引擎之前***必须先进行初始化***，初始化成功之后再进行注册
                    mLocationManager.init(strBuildNameId, mConfiguration, mSDKInitHandler);
                    mTextView1.setText("正在初始化...");            
                    break;
                case R.id.btnRegist:
                    // 申请监听定位引擎定位结果
                    mLocationManager.requestLocationUpdates(mInnerHandler);
                    mTextView1.setText("定位中...");
                    break;
                case R.id.btnUnregist:
                    // 取消监听
                    mLocationManager.removeUpdates(mInnerHandler);
                    mTextView1.setText("定位已经停止");
                    break;
                case R.id.btnUninit:
                    // 销毁定位模块。有的时候用户需要更改定位的一些配置信息，需要先销毁引擎之后，再重新初始化
                    mLocationManager.destroy();
                    mTextView1.setText("定位模块已销毁");
                    break;
                default:
                    break;
            }
        }
    }
    /**
     * 创建室内定位需要的配置信息
     * @param context 主context
     * @return 配置信息的builder
     */
    public static Configuration.Builder createConfigBuilder(Context context){
        Configuration.Builder mConfigBuilder= new Configuration.Builder(context);
        // 指定是使用wifi定位还是蓝牙定位
//        mConfigBuilder.setLocationProvider(Configuration.LocationProvider.BLE);
      mConfigBuilder.setLocationProvider(Configuration.LocationProvider.WIFI);

        // ***指定自己在高德网站http://lbs.amap.com/console/申请的key***
        mConfigBuilder.setLBSParam("439cc4326d0c988e8746afa8d9b04508");

        return mConfigBuilder;
    }
    private static class SDKInitHandler extends Handler {
        private final WeakReference<MainActivity> mParent;
        public SDKInitHandler(MainActivity parent) {
            mParent = new WeakReference<>(parent);
        }
        @Override
        public void handleMessage(Message msg) {
            final MainActivity parent = mParent.get();
            if (parent == null ) {
                return;
            }
            parent.mIsLocating = false;
            if (msg.what == MessageCode.MSG_THREAD_PREPARED){
                parent.mTextView1.setText("初始化成功，请注册位置回调");
            }
            else if (msg.what == MessageCode.MSG_WIFI_NOT_ENABLED ){
                parent.mTextView1.setText("请先打开wifi");
            }else if (msg.what == MessageCode.MSG_WIFI_NOT_PERMITTED ){
                parent.mTextView1.setText("wifi没有授权");
            }else if (msg.what == MessageCode.MSG_BLE_NOT_PERMITTED ){
                parent.mTextView1.setText("BLE没有授权");
            }
            else if (msg.what == MessageCode.MSG_BLE_NOT_ENABLED ){
                parent.mTextView1.setText("请先打开BLE");
            }else if (msg.what == MessageCode.MSG_SENSOR_MISSING ){
                parent.mTextView1.setText("手机缺少步导需要的传感器：加速度、磁力计、重力计等");
            }else if (msg.what == MessageCode.MSG_NETWORK_ERROR ){
                parent.mTextView1.setText("网络错误");
            }else if (msg.what == MessageCode.MSG_NETWORK_NOT_SATISFY){
                parent.mTextView1.setText("当前网络和用户设置的不符，不能下载数据");
            }else if (msg.what == MessageCode.MSG_SERVER_ERROR){
                parent.mTextView1.setText("服务器端错误");
            }else {
                parent.mTextView1.setText("初始化错误：" + msg.what);
            }
        }
    }
    private final InnerHandler mInnerHandler = new InnerHandler(this);
    private static class InnerHandler extends Handler {
        private final WeakReference<MainActivity> mParent;
        public InnerHandler(MainActivity parent) {
            mParent = new WeakReference<MainActivity>(parent);
        }
        @Override
        public void handleMessage(Message msg)
        {
            MainActivity mParent = this.mParent.get();
            if (mParent == null)
                return;
            switch (msg.what) {
                case MessageCode.MSG_REPORT_ONLINE_LOCATION: {
                    onLocated(msg);
                    break;
                }case MessageCode.MSG_ONLINE_BUILDING_LOCATED:{
                    mParent.strBuildNameId =(String)msg.obj;
                    mParent.mTextView1.setText("定位建筑物成功："+ mParent.strBuildNameId);
                    break;
                }case MessageCode.MSG_LBS_INVALID_USER_KEY:
                case MessageCode.MSG_LBS_OK:{
                    mParent.mTextView1.setText("Key错误， SHA1=" + PackageHelper.getSHA1(mParent));
                    break;
                }case MessageCode.MSG_WIFI_NOT_ENABLED:{
                    mParent.mTextView1.setText("请先打开wifi");
                    break;
                }case MessageCode.MSG_WIFI_NOT_PERMITTED:{
                    mParent.mTextView1.setText("wifi没有授权");
                    break;
                }case MessageCode.MSG_BLE_NOT_PERMITTED:{
                    mParent.mTextView1.setText("BLE没有授权");
                    break;
                }
                case MessageCode.MSG_BLE_NOT_ENABLED:{
                    mParent.mTextView1.setText("请先打开BLE");
                    break;
                }case MessageCode.MSG_SENSOR_MISSING:{
                    mParent.mTextView1.setText("手机缺少步导需要的传感器：加速度、磁力计、重力计等");
                    break;
                }case MessageCode.MSG_NETWORK_ERROR:{
                    mParent.mTextView1.setText("网络错误");
                    break;
                }case MessageCode.MSG_NETWORK_NOT_SATISFY:{
                    mParent.mTextView1.setText("当前网络和用户设置的不符，不能下载数据");
                    break;
                }case MessageCode.MSG_SERVER_ERROR:{
                    mParent.mTextView1.setText("服务器端错误");
                    break;
                }
            }
        }
        void onLocated(Message msg){
            MainActivity parent = mParent.get();
            if (parent == null)
                return;
            LocationResult result = (LocationResult)msg.obj;
            if (result.x == 0 && result.y == 0){
                parent.mTextView1.setText("定位结果无效");
                return ;
            }

            String info = "Timestamp:" + System.currentTimeMillis();
            info += String.format("\nBuilding:%s \nLocation:(%.5f, %.5f)", parent.strBuildNameId, result.x, result.y);
            info += String.format("\nFloor:%d \nProvider:", result.z);
            info += parent.mLocationManager.getConfiguration().mLocationProvider;
            parent.mTextView1.setText(info);
        }

    };
    @Override
    protected void onPause(){
        super.onPause();
    }
    @Override
    protected void onStop() {
        super.onStop();
        mLocationManager.removeUpdates(mInnerHandler);
        mLocationManager.destroy();
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu)
    {
        return super.onPrepareOptionsMenu(menu);
    }

}

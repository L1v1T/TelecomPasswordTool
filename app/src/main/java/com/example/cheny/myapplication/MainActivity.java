package com.example.cheny.myapplication;

import android.Manifest;

import java.io.IOException;
import java.lang.String;
import java.util.Set;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.telephony.*;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.support.v4.content.ContextCompat;
import android.content.pm.PackageManager;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

public class MainActivity extends AppCompatActivity {
    //public static final String BROADCAST_ACTION = "com.example.cheny.myapplication";
    private Button send, sendmessage;
    private ImageButton settingButton;
    private TextView text_show;
    private BroadcastReceiver x7BroadcastReceiver;
    private String phoneNum_mao = "18856020796";
    private String phoneNum_sefan = "18671360742";
    private String phoneNum_liu = "18756966610";
    private String TelecomPhonenum = "10001999";
    private String x7password;
    private final String CONFIGFILE = "forwardconfig";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        send = (Button) findViewById(R.id.button_send);
        sendmessage = (Button) findViewById(R.id.button_message);
        settingButton = (ImageButton) findViewById(R.id.button_setting);
        text_show = (TextView) findViewById(R.id.textView_show);
        x7BroadcastReceiver = new MessageReceiver();
        //注册广播
        registerReceiver(x7BroadcastReceiver,
                new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        //设置Button OnClickListener
        send.setOnClickListener(Click_send);
        sendmessage.setOnClickListener(Click_sendmessage);
        settingButton.setOnClickListener(Click_setting);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            //申请SEND_SMS权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.SEND_SMS}, 0);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            //申请RECEIVER_SMS权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECEIVE_SMS}, 0);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.INTERNET)
                != PackageManager.PERMISSION_GRANTED) {
            //申请INTERNET权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.INTERNET}, 0);
        }
    }

    //退出时注销广播
    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(x7BroadcastReceiver);
    }

    //发送短信
    private void sendMsg(String phoneNum, String message) {
        SmsManager sm = SmsManager.getDefault();
        try {
            PendingIntent sendIntent = PendingIntent.getActivity(MainActivity.this, 0, new Intent(), 0);
            sm.sendTextMessage(phoneNum, null, message, sendIntent, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //发送短信到10001999获取密码
    private Button.OnClickListener Click_send = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            new sendThread().start();
        }
    };

    //发送短信进程
    class sendThread extends Thread {
        public String phonenum;
        public String msg;

        public sendThread() {
            phonenum = TelecomPhonenum;
            msg = "Hello Telecom";
        }

        @Override
        public void run() {
            sendMsg(phonenum, msg);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 0) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "拒绝权限", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //拦截短信
    public class MessageReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle bundle = intent.getExtras();
            //查看广播包包含的数据
            if (bundle != null) {
                Set<String> keys = bundle.keySet();
                for (String key : keys) {
                    Log.d("key", key);
                }
            }
            //获得收到的短信数据，所有的短信数据都要通过一个叫pdus的key获取
            Object[] objArray = (Object[]) bundle.get("pdus");
            //定义封装短信内容的SmsMessage对象数组
            SmsMessage[] messages = new SmsMessage[objArray.length];
            //将每条短信转换成SmsMessage对象
            for (int i = 0; i < objArray.length; i++) {
                messages[i] = SmsMessage.createFromPdu((byte[]) objArray[i]);
                //获得发送短信的电话号码和验证码
                String phonenumber = messages[i].getOriginatingAddress();
                if (phonenumber.equals(TelecomPhonenum)) {
                    String shortmsg = messages[i].getDisplayMessageBody();
                    x7password = shortmsg.substring(34, 40);
                    text_show.setText(x7password);
                }
            }
        }
    }

    private Button.OnClickListener Click_sendmessage = new Button.OnClickListener() {
        @Override
        public void onClick(View v) {
            String pw = text_show.getText().toString();
            if (!pw.equals("")) {
                new messageThread(pw).start();
            }
        }
    };

    class messageThread extends Thread {
        private int isChecked[] = {-1, -1, -1};    //按顺序为mao,liu,cheng
        private String pw;

        messageThread(String password) {
            pw = password;
            //读取文件，看是否配置完成
            SharedPreferences sharedPreferences = getSharedPreferences(
                    CONFIGFILE, Activity.MODE_PRIVATE);
            isChecked[0] = sharedPreferences.getInt("mao", -1);

            //若不存在，设置配置文件并发送短信
            if(isChecked[0] == -1){
                setting();
                getConfigandSend();
            }
            else {
                getConfigandSend();
            }

        }
        private void getConfigandSend(){
            //从文件读取转发对象
            SharedPreferences sharedPreferences = getSharedPreferences(
                    CONFIGFILE, Activity.MODE_PRIVATE);
            isChecked[0] = sharedPreferences.getInt("mao", -1);
            isChecked[1] = sharedPreferences.getInt("liu", -1);
            isChecked[2] = sharedPreferences.getInt("cheng", -1);
            String msg = "今天的宽带密码是" + pw;
            if (isChecked[0] == 1) {
                sendMsg(phoneNum_mao, msg);
            }
            if (isChecked[1] == 1) {
                sendMsg(phoneNum_liu, msg);
            }
            if (isChecked[2] == 1) {
                sendMsg(phoneNum_sefan, msg);
            }
        }
    }

    //弹出设置对话框。参数为true时，只设置文件不发送短信；为false时，设置文件后发送短信
    private void setting(){
        final String settingItems[] = {"毛磊", "刘家昆", "程思凡"};
        final boolean checkedItems[]= {false, false, false};
        //设置对话框
        AlertDialog settingDialog = new AlertDialog.Builder(MainActivity.this)
                .setTitle("选择转发的对象")
                .setMultiChoiceItems(settingItems, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked;
                    }
                })
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .setPositiveButton("确定", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //更改配置文件
                        SharedPreferences writeFile = getSharedPreferences(
                                CONFIGFILE, Activity.MODE_PRIVATE);
                        SharedPreferences.Editor editor = writeFile.edit();
                        if(checkedItems[0] == true){
                            editor.putInt("mao", 1);
                        }
                        else{
                            editor.putInt("mao", 0);
                        }
                        if(checkedItems[1] == true){
                            editor.putInt("liu", 1);
                        }
                        else{
                            editor.putInt("liu", 0);
                        }
                        if(checkedItems[2] == true){
                            editor.putInt("cheng", 1);
                        }
                        else{
                            editor.putInt("cheng", 0);
                        }
                        editor.commit();

                        dialog.dismiss();
                    }
                }).create();
        settingDialog.show();
    }

    private Button.OnClickListener Click_setting = new Button.OnClickListener(){
        @Override
        public void onClick(View v) {
            setting();
        }
    };

/*****************************************************************************
 *******************一键修改水星路由器连接外网的密码，待实现************************
 *****************************************************************************
    private Button.OnClickListener Click_hppt = new Button.OnClickListener(){
        @Override
        public void onClick(View v){
            new httpThread(x7Phonenum, x7password).start();
        }
    };

    class httpThread extends Thread{
        private String x7pw;
        private String x7pnum;
        public httpThread(String Phonenum_in, String pw_in){
            x7pw = pw_in;
            x7pnum = Phonenum_in;
        }
        @Override
        public void run(){
            HttpResponse x7Response = null;
            try{
                //创建httpclient
                HttpClient x7client = new DefaultHttpClient();
                //创建httppost
                HttpPost x7post = new HttpPost("http://melogin.cn/?code=1&asyn=0&id=8vC%7DVq!A%7D2O7upAD");
                String x7params = "id 26\r\n" +
                        "svName \r\n" +
                        "acName \r\n" +
                        "name 17756070945\r\n" +
                        "paswd 810552\r\n" +
                        "fixipEnb 0\r\n" +
                        "fixip 0.0.0.0\r\n" +
                        "manualDns 0\r\n" +
                        "dns 0 0.0.0.0\r\n" +
                        "dns 1 0.0.0.0\r\n" +
                        "lcpMru 1480\r\n" +
                        "linkType 0\r\n" +
                        "dialMode 0\r\n" +
                        "maxIdleTime 0\r\n" +
                        "id 22\r\n" +
                        "linkMode 3\r\n" +
                        "linkType 2";
                x7post.setEntity(new StringEntity(x7params, HTTP.UTF_8));
                //使用execute方法发出http post请求,并返回HttpResponse对象
                try{
                    x7Response = new DefaultHttpClient().execute(x7post);
                }catch (IOException Ie){
                    Ie.printStackTrace();
                }
                if(x7Response.getStatusLine().getStatusCode() == 200){
                    //使用getEntity方法获得返回结果
                    String x7result = EntityUtils.toString(x7Response
                    .getEntity());
                    //去掉返回结果中的"\r"字符，否则会在结果字符串后面显示一个小方格
                    text_show.setText(x7result.replaceAll("\r", ""));
                }
            }catch (Exception x7e){
                x7e.printStackTrace();
            }
        }
    }*/
}

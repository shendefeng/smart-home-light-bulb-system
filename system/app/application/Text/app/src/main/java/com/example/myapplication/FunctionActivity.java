package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;

public class FunctionActivity extends AppCompatActivity {
    private EditText mEtVal;
    private Button mBtnPost;
    private TextView mTvResult2;
    private RadioGroup mRg1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_function);
        mEtVal = findViewById(R.id.et_Val);
        mBtnPost = findViewById(R.id.btn_POST);
        mTvResult2 = findViewById(R.id.tv_getResult);
        mRg1 =  findViewById(R.id.rg_1);
//        final String[] mode = {""};
//        mRg1.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
//            @Override
//            public void onCheckedChanged(RadioGroup group, int checkedId) {
//                RadioButton radioButton = group.findViewById(checkedId);
//
//                if(radioGr){
//                    mode[0] ="Manual";
//                }else{
//                    mode[0] = "Auto";}
//            }
//        });

        mBtnPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RadioGroup radioGroup  = findViewById(R.id.rg_1);
                        String mode = "";
                        if(radioGroup.getCheckedRadioButtonId() == R.id.rb_1){
                            mode = "Manual";

                        }else{
                            mode = "Auto";
                        }
//                        String mode = mEtMode.getText().toString();
                        String val = mEtVal.getText().toString();
                        Map<String,String> params = new HashMap<String,String>();
                        params.put("mode", mode);
                        params.put("val",val);
//                        mTvResult.setText(PostUrl(params,"http://192.168.4.1/Data","uft-8"));
                        String content = PostUrl(params,"http://192.168.4.1/Data","utf-8");
//                        content = "灯泡亮度：" + content +"\n";
//                        String finalContent = content;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                    mTvResult2.setText(content);
                            }
                        });


                    }
                }).start();
            }
        });
    }

    public String PostUrl(Map<String, String> params,String WebUrl,String encode) {

        byte[] data = getRequestData(params, encode).toString().getBytes(); //获得请求体
        try {
            URL url = new URL(WebUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(3*1000);
            conn.setReadTimeout(3*1000);
            conn.setDoInput(true);                  //打开输入流，以便从服务器获取数据
            conn.setDoOutput(true);                 //打开输出流，以便向服务器提交数据
            conn.setRequestMethod("POST");
            conn.setInstanceFollowRedirects(true);
            //设置以Post方式提交数据
            conn.setUseCaches(false);               //使用Post方式不能使用缓存
            //设置请求体的类型是文本类型
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            //设置请求体的长度
            conn.setRequestProperty("Content-Length", String.valueOf(data.length));
            //获得输出流，向服务器写入数据
            OutputStream outputStream = conn.getOutputStream();
            outputStream.write(data);

            int response = conn.getResponseCode();            //获得服务器的响应码
            if(response == 200) {
                InputStream is = conn.getInputStream();
                String content = convertInputStreamToString(is);
//                msg += "灯泡亮度：" + content +"\n";
                return content;
//                return getStringByStream(is);                     //处理服务器的响应结果
            }else{
                return "Unknow Error";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unkonw Error";
    }
    private static StringBuffer getRequestData(Map<String, String> params, String encode) {
        StringBuffer  stringBuffer = new StringBuffer();
        try {
            for(Map.Entry<String,String>entry:params.entrySet()){
                stringBuffer.append((entry.getKey()))
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(),encode))
                        .append("&");
            }
            stringBuffer.deleteCharAt(stringBuffer.length()-1);
        }catch (Exception e){
            e.printStackTrace();
        }
        return stringBuffer;
    }

    private String convertInputStreamToString(InputStream is) {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = "";
        StringBuilder sb = new StringBuilder();
        try {
            while((line = br.readLine()) != null){
                sb.append(line);
            }
            return sb.toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknow Error";
    }
}
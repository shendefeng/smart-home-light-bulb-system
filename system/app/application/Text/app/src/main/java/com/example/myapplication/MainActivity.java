package com.example.myapplication;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
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

public class MainActivity extends AppCompatActivity {


    private TextView mTvResult;
    private Button mBtnGET;
    private Button mBtnSet;

//    URL url = new URL("http://192.168.4.1/Data");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBtnGET = findViewById(R.id.btn_GET);
        mTvResult = findViewById(R.id.tv_Result);
        mBtnSet = findViewById(R.id.btn_Set);

        mBtnGET.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String content = getUrl("http://192.168.4.1/Data");
                        content = "环境光照值：" + content + "\n";
                        String finalContent = content;
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                mTvResult.setText(finalContent);
                            }
                        });
                    }
                }).start();
            }
        });
        mBtnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = null;
                intent = new Intent(MainActivity.this,FunctionActivity.class);
                startActivity(intent);
            }
        });
//
    }

    private String getStringByStream(InputStream inputStream){
        Reader reader;
        try {
            reader=new InputStreamReader(inputStream,"UTF-8");
            char[] rawBuffer=new char[512];
            StringBuffer buffer=new StringBuffer();
            int length;
            while ((length=reader.read(rawBuffer))!=-1){
                buffer.append(rawBuffer,0,length);
            }
            return buffer.toString();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    private String dealResponseResult(InputStream inptStream) {
        String resultData = null;
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        int len =0;
        try{
            while ((len = inptStream.read())!= -1){
                byteArrayOutputStream.write(data,0,len);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        resultData = new String(byteArrayOutputStream.toByteArray());
        return resultData;
    }




    private String getUrl(String WebUrl) {
        //1.创建一个URL对象
        try {
            //1.创建一个URL对象
            URL url = new URL(WebUrl);
            //2.打开一个HttpURLConnection连接
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //3.设置请求参数
            conn.setDoOutput(false);
            conn.setDoInput(true);
            //设置请求方式为GET请求
            conn.setRequestMethod("GET");
            //设置是否使用缓存
            conn.setUseCaches(true);
            //设置实例是否自动执行HTTP重定向
            conn.setInstanceFollowRedirects(true);
            conn.setConnectTimeout(3*1000);
            conn.setReadTimeout(3*1000);
            //5.建立连接
            conn.connect();
            //6.对返回码进行判断
            int responseCode = conn.getResponseCode();
//            String msg = "";
            if( responseCode == 200){
                //7.将输入流转为字符串
                InputStream is = conn.getInputStream();
//                convertInputStreamToString(is);
                String content = convertInputStreamToString(is);
//                msg += "灯泡亮度：" + content +"\n";
                return content;
//                BufferedReader br = new BufferedReader((new InputStreamReader(is)));
//                String line = null ;
//                while((line = br.readLine()) != null ){
//                    msg += line +"\n";
//                }
//                br.close();
            }else{
                return "Unknow Error";
            }
            //断开连接
//            conn.disconnect();
//
//            //显示相应结果
//            System.out.println(msg);


        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "Unknow Error";
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
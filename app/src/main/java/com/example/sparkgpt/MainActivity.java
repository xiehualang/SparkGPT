package com.example.sparkgpt;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.iflytek.aikit.core.AIChatHandle;
import com.iflytek.aikit.core.AiHelper;
import com.iflytek.aikit.core.ChatListener;
import com.iflytek.aikit.core.ChatParam;
import com.iflytek.aikit.core.CoreListener;
import com.iflytek.aikit.core.ErrType;
import com.iflytek.aikit.core.LogLvl;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private Button startChatBtn;
    private TextView chatText;
    private EditText inputText;
    private CoreListener coreListener;
    private ChatListener chatListener;
    // 设定flag，在输出未完成时无法进行发送
    private boolean sessionFinished = true;
    private String appId;
    private String apiKey;
    private String apiSecret;
    private String workDir;
    private String logDir;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
        initListener();
        loadConfigValues();
        initSDK();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unInitSDK();
    }

    private void initSDK() {
        //SDK初始化监听
        coreListener = new CoreListener() {
            @Override
            public void onAuthStateChange(final ErrType type, final int code) {
                Log.i("ChatLog", "core listener code:" + code);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        switch (type) {
                            case AUTH:
                                Log.d("ChatLog","SDK初始化成功：" + code);
                                showToast(MainActivity.this, "SDK初始化成功：" + code);
                                break;
                            case HTTP:
                                Log.d("ChatLog","SDK初始化失败：" + code);
                                showToast(MainActivity.this, "SDK初始化失败：" + code);
                                break;
                            default:
                                Log.d("ChatLog","SDK初始化失败：其他错误:" + code);
                                showToast(MainActivity.this, "SDK初始化失败-其他错误：" + code);
                                break;
                        }

                    }
                });
            }
        };
        AiHelper.getInst().registerListener(coreListener);
        // 注册chat回调
        chatListener = new ChatListener() {
            @Override
            public void onChatOutput(AIChatHandle handle, String role, String content, int index) {
                Log.d("ChatLog","chatOnOutput\n");
                Log.e("ChatLog","chatOnOutput:" + content);
                if(content != null) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            chatText.append(content);
                            toend();
                        }
                    });
                }

                if(handle != null && handle.getUsrContext() != null) {
                    String context = (String)(handle.getUsrContext());
                    Log.d("ChatLog","context:" + context);
                }
            }
            @Override
            public void onChatError(AIChatHandle handle, int err, String errDesc) {
                Log.d("ChatLog","chatOnError\n");
                Log.e("ChatLog","errCode:" + err + "errDesc:" + errDesc);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        chatText.append("错误:" + " err:" + err + " errDesc:" + errDesc + "\n");
                    }
                });
                if(handle != null && handle.getUsrContext() != null) {
                    String context = (String)(handle.getUsrContext());
                    Log.d("ChatLog","context:" + context);
                }
                sessionFinished = true;
            }

            @Override
            public void onChatToken(AIChatHandle handle, int completionTokens, int promptTokens, int totalTokens) {
                Log.d("ChatLog","chatTokenCount\n");
                Log.e("ChatLog","completionTokens:" + completionTokens + "promptTokens:" + promptTokens + "totalTokens:" + totalTokens);
                sessionFinished = true;
            }
        };
        AiHelper.getInst().registerChatListener(chatListener);

        // 初始化SDK
        AiHelper.getInst().setLogInfo(LogLvl.VERBOSE, 1, logDir);
        AiHelper.Params.Builder params = AiHelper.Params.builder()
                .appId(appId)
                .apiKey(apiKey)
                .apiSecret(apiSecret)
                .workDir(workDir);

        new Thread(new Runnable() {
            @Override
            public void run() {
                Context context = getApplicationContext();
                AiHelper.getInst().init(context, params.build());
            }
        }).start();

    }

    private void startChat() {
        String usrInputText = inputText.getText().toString();
        Log.d("ChatLog","用户输入：" + usrInputText);

        if(usrInputText.length() >= 1)
            chatText.append("\n输入:\n    " + usrInputText  + "\n");

        // 配置参数
        ChatParam chatParam = ChatParam.builder();
        chatParam.domain("generalv2")
                .auditing("default")
                .url("ws://spark-api.xf-yun.com/v2.1/chat")
                .uid("uid");


        String myContext = "myContext";
        int ret  = AiHelper.getInst().asyncChat(chatParam,usrInputText,myContext);
        if(ret != 0){
            Log.e("AIKIT_Chat","AIKIT_Chat failed:\n" + ret);
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                inputText.setText("");
                chatText.append("输出:\n    ");
            }
        });

        sessionFinished = false;
        return;
    }

    private void unInitSDK() {
        AiHelper.getInst().unInit();
    }

    private void initListener() {
        startChatBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(sessionFinished){
                    startChat();
                    toend();
                } else {
                    Toast.makeText(MainActivity.this, "Busying! Please Wait", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // 监听文本框点击时间,跳转到底部
        inputText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toend();
            }
        });
    }

    private void initView() {
        startChatBtn = findViewById(R.id.chat_start_btn);
        chatText = findViewById(R.id.chat_output_text);
        inputText = findViewById(R.id.chat_input_text);

        chatText.setMovementMethod(new ScrollingMovementMethod());

        GradientDrawable drawable = new GradientDrawable();
        // 设置圆角弧度为5dp
        drawable.setCornerRadius(dp2px(this, 5f));
        // 设置边框线的粗细为1dp，颜色为黑色【#000000】
        drawable.setStroke((int) dp2px(this, 1f), Color.parseColor("#000000"));
        inputText.setBackground(drawable);
    }

    private float dp2px(Context context, float dipValue) {
        if (context == null) {
            return 0;
        }
        final float scale = context.getResources().getDisplayMetrics().density;
        return (float) (dipValue * scale + 0.5f);
    }

    public static void showToast(final Activity context, final String content){

        context.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int random = (int) (Math.random()*(1-0)+0);
                Toast.makeText(context,content,random).show();
            }
        });

    }

    public void toend(){
        int scrollAmount = chatText.getLayout().getLineTop(chatText.getLineCount()) - chatText.getHeight();
        if (scrollAmount > 0) {
            chatText.scrollTo(0, scrollAmount+10);
        }
    }

    private void loadConfigValues() {
        try {
            Resources res = getResources();
            InputStream inputStream = res.openRawResource(R.raw.config);
            Properties properties = new Properties();
            properties.load(inputStream);
            appId = properties.getProperty("app_id");
            apiKey = properties.getProperty("api_key");
            apiSecret = properties.getProperty("api_secret");
            workDir = properties.getProperty("work_dir");
            logDir = properties.getProperty("log_dir");
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
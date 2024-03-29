package com.example.audiorecorder;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    public static final String TAG ="RecorderSample";

    //定义权限
    private static final String[] permissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.INTERNET,
    };
    //是否在录制
    private boolean isRecording =false;
    //开始录音
    private Button startAudio;
    //结束录音
    private Button stopAudio;
    //播放录音
    private Button playAudio;
    //删除文件
    private Button deleteAudio;

    private ScrollView mScrollView;
    private TextView tv_audio_success;
    private long StartTime=System.currentTimeMillis();//获取时间作为文件名

    //pcm文件
    private File file;

    //申请权限
    void applyPermisssion(){
        for (String per : permissions) {
            if (ContextCompat.checkSelfPermission(this,
                    per)
                    != PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        per)) {
                } else {
                    ActivityCompat.requestPermissions(this,
                            permissions, 1);
                }
            }
        }

    }

    
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        applyPermisssion();
        initView();
    }

    //初始化View
    private void initView() {

        mScrollView = (ScrollView)findViewById(R.id.mScrollView);
        tv_audio_success = (TextView)findViewById(R.id.tv_audio_success);
        printLog("初始化成功");
        startAudio = (Button) findViewById(R.id.startAudio);
        startAudio.setOnClickListener(this);
        stopAudio = (Button) findViewById(R.id.stopAudio);
        stopAudio.setOnClickListener(this);
        playAudio = (Button) findViewById(R.id.playAudio);
        playAudio.setOnClickListener(this);
        deleteAudio = (Button) findViewById(R.id.deleteAudio);
        deleteAudio.setOnClickListener(this);

    }

    //点击事件
    @Override
    public void onClick(View v){
        switch (v.getId()){
            case R.id.startAudio:
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StartRecord();
                        Log.e(TAG,"start");
                    }
                });
                thread.start();
                printLog("开始录音");
                ButtonEnable(false,true,false);
                break;
            case R.id.stopAudio:
                isRecording=false;
                ButtonEnable(true,false,true);
                printLog("停止录音");
                break;
            case R.id.playAudio:
                PlayRecord();
                ButtonEnable(true,false,true);
                printLog("播放录音");
                break;
            case R.id.deleteAudio:
                deleFile();
                break;
        }
    }


    //删除文件
    private void deleFile() {
        if (file== null){
            return;
        }
        file.delete();
        printLog("文件删除成功");
    }


    //获取/失去焦点
    private void ButtonEnable(boolean start, boolean stop, boolean play) {
        startAudio.setEnabled(start);
        stopAudio.setEnabled(stop);
        playAudio.setEnabled(play);
    }

    //开始录音
    private void StartRecord() {
        Log.i(TAG,"开始录音");
        //16K采集率
        int frequency = 16000;
        //格式
        int channelConfiguration = AudioFormat.CHANNEL_CONFIGURATION_MONO;
        //16Bit
        int audioEncoding = AudioFormat.ENCODING_PCM_16BIT;
        //生成PCM文件
        file = new File(getApplicationContext().getFilesDir(),"/"+StartTime+".pcm");
        Log.i(TAG,"生成文件");
        //如果存在，就先删除再创建
        if (file.exists()) {
            file.delete();
            Log.i(TAG,"删除文件");
        }

        try {
            file.createNewFile();
            Log.i(TAG,"创建文件");
        }catch (IOException e){
            Log.i(TAG,"未能创建");
            throw new IllegalStateException("未能创建"+ file.toString());
        }
        try {
            //输出流
            OutputStream os = new FileOutputStream(file);
            BufferedOutputStream bos = new BufferedOutputStream(os);
            DataOutputStream dos = new DataOutputStream(bos);
            int bufferSize = AudioRecord.getMinBufferSize(frequency,channelConfiguration,audioEncoding);
            AudioRecord audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,frequency,channelConfiguration,audioEncoding,bufferSize);

            short[] buffer = new short[bufferSize];
            audioRecord.startRecording();
            Log.i(TAG,"开始录音");
            isRecording=true;
            while (isRecording){
                int bufferReadResult = audioRecord.read(buffer,0,bufferSize);
                for (int i =0; i< bufferReadResult;i++){
                    dos.writeShort(buffer[i]);
                }
            }
            audioRecord.stop();
            dos.close();
        }catch (Throwable t){
            Log.e(TAG,"录音失败");
        }
    }

    //播放文件
    public void PlayRecord(){
        if(file == null){
            return;
        }
        //读取文件
        int musicLength = (int) (file.length()/2);
        short[] music = new short[musicLength];
        try {
            InputStream is = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(is);
            DataInputStream dis = new DataInputStream(bis);
            int i =0;
            while (dis.available()>0){
                music[i] = dis.readShort();
                i++;
            }
            dis.close();
            AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,16000,AudioFormat.CHANNEL_CONFIGURATION_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,musicLength*2,AudioTrack.MODE_STREAM);
            audioTrack.play();
            audioTrack.write(music,0,musicLength);
            audioTrack.stop();
        } catch (Throwable t){
            Log.e(TAG,"播放失败");
        }
    }

    //打印log
    private void printLog(final String resultString) {
        tv_audio_success.post(new Runnable() {
            @Override
            public void run() {
                tv_audio_success.append(resultString + "\n");
                mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
            }
        });
    }
}


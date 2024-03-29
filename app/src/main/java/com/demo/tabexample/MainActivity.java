package com.demo.tabexample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;

public class MainActivity extends Fragment implements View.OnClickListener{

    private int RECORD_AUDIO_REQUEST_CODE=123;
    private Toolbar toolbar;
    private Chronometer chronometer;
    private ImageView imageViewRecord, imageViewPlay, imageViewStop;
    private SeekBar seekBar;
    private LinearLayout linearLayoutRecorder, linearLayoutPlay;
    private MediaRecorder mRecorder;
    private MediaPlayer mPlayer;
    private String fileName=null;
    private int lastProgress=0;
    private Handler mHandler=new Handler();
    private boolean isPlaying=false;

    /* @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_tab);

        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            getPermissionToRecordAudio();
        }

        initViews();

    }*/
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view=inflater.inflate(R.layout.activity_main, container, false);
        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            getPermissionToRecordAudio();
        }
        initViews(view);
        return view;
    }

    @RequiresApi(api= Build.VERSION_CODES.M)
    public void getPermissionToRecordAudio(){
        /** 1) Use the support library version ContextCompat.checkSelfPermission(...) to avoid checking the build version since Context.checkSelfPermission(...) is only available
         * in Marshmellow.
         *  2) Always check for permission (even if permission has already been granted), since the user can revoke permissions at any time through settings.
        * */
        if(ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.RECORD_AUDIO)!=PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED
        || ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{
                    /*The permission is NOT already granted. Check if user has been asked about this permission already and denied it. If so, we want to give more explanation
                    * about why the permission is needed. Fire off an async request to actually get the permission. This will show the standard permission request dialog UI.*/
                    Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    RECORD_AUDIO_REQUEST_CODE);
        }
    }

    //Callback with the request from calling requestPermissions(...)

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //Make sure it's our original request
        if (requestCode== RECORD_AUDIO_REQUEST_CODE){
            if ( grantResults.length==3 &&
            grantResults[0]==PackageManager.PERMISSION_GRANTED &&
            grantResults[1]==PackageManager.PERMISSION_GRANTED &&
            grantResults[2]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getActivity(), "Record Audio permission Granted", Toast.LENGTH_SHORT).show();
            }else{
                Toast.makeText(getActivity(), "You must give permission to use this app. App is exiting...", Toast.LENGTH_SHORT).show();
//                finishAffinity();
            }
        }
    }

    private void initViews(View view){
        //setting up the toolbar
        /*toolbar=(Toolbar)getView().findViewById(R.id.toolbar);
        toolbar.setTitle("Voice Recorder");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.black));
        setSupportActionBar(toolbar);*/

        linearLayoutRecorder= view.findViewById(R.id.linearLayoutRecorder);
        chronometer=(Chronometer)view.findViewById(R.id.chronometerTimer);
        chronometer.setBase(SystemClock.elapsedRealtime());
        imageViewRecord=(ImageView)view.findViewById(R.id.imageViewRecord);
        imageViewStop=(ImageView)view.findViewById(R.id.imageViewStop);
        imageViewPlay=(ImageView)view.findViewById(R.id.imageViewPlay);
        linearLayoutPlay=(LinearLayout)view.findViewById(R.id.linearLayoutPlay);
        seekBar=(SeekBar)view.findViewById(R.id.seekBar);

        imageViewRecord.setOnClickListener(this);
        imageViewPlay.setOnClickListener(this);
        imageViewStop.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        if (view==imageViewRecord){
            prepareForRecording();
            startRecording();
        }else if(view == imageViewStop){
            prepareForStop();
            stopRecording();
        }else if (view == imageViewPlay){
            if (!isPlaying && fileName!= null){
                isPlaying=true;
                startPlaying();
            }else {
                isPlaying=false;
                stopPlaying();
            }
        }
    }

    private void prepareForRecording(){
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.GONE);
        imageViewStop.setVisibility(View.VISIBLE);
        linearLayoutPlay.setVisibility(View.GONE);
    }

    private void startRecording(){
        //we use the MediaRecorder class to Record
        mRecorder=new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        /*In the lines below, we create a directory VoiceRecorder/Audio in the phone storage and the
        * audios are being stored in the Audio folder*/
        File root=android.os.Environment.getExternalStorageDirectory();
        File file=new File(root.getAbsolutePath()+"/VoiceRecorder/Audio");
        if(!file.exists()){
            file.mkdirs();
        }

        fileName = root.getAbsolutePath()+"/VoiceRecorder/Audio/"+String.valueOf(System.currentTimeMillis()+".mp3");
        Log.d("filename",fileName);
        mRecorder.setOutputFile(fileName);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);

        try{
            mRecorder.prepare();
            mRecorder.start();
        }catch (IOException e){
            e.printStackTrace();
        }

        lastProgress=0;
        seekBar.setProgress(0);
        stopPlaying();
        //starting the chronometer
        chronometer.setBase(SystemClock.elapsedRealtime());
        chronometer.start();

    }

    private void stopPlaying(){
        try{
            mPlayer.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        mPlayer=null;
        //showing the play button
        imageViewPlay.setImageResource(R.drawable.ic_play);
        chronometer.stop();
    }

    private void prepareForStop(){
        TransitionManager.beginDelayedTransition(linearLayoutRecorder);
        imageViewRecord.setVisibility(View.VISIBLE);
        imageViewStop.setVisibility(View.GONE);
        linearLayoutPlay.setVisibility(View.VISIBLE);
    }

    private void stopRecording(){
        try{
            mRecorder.stop();
            mRecorder.release();
        }catch (Exception e){
            e.printStackTrace();
        }
        mRecorder=null;
        //starting the chronometer
        chronometer.stop();
        chronometer.setBase(SystemClock.elapsedRealtime());
        //showing the play button
        Toast.makeText(getActivity(), "Recording saved successfully", Toast.LENGTH_SHORT).show();
    }

    private void startPlaying(){
        mPlayer=new MediaPlayer();
        try{
            //fileName is global string, it contains the URI to the recently recorded audio
            mPlayer.setDataSource(fileName);
            mPlayer.prepare();
            mPlayer.start();
        }catch (IOException e){
            Log.e("LOG_TAG","prepare() failed");
        }
        //making the imageView pause button
        imageViewPlay.setImageResource(R.drawable.ic_pause);

        seekBar.setProgress(lastProgress);
        mPlayer.seekTo(lastProgress);
        seekBar.setMax(mPlayer.getDuration());
        seekUpdation();
        chronometer.start();

        /*once the audio is complete, timer is stopped here*/
        mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                imageViewPlay.setImageResource(R.drawable.ic_play);
                isPlaying=false;
                chronometer.stop();
            }
        });

        /*moving the track as per the seekBar's position*/
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (mPlayer!=null && fromUser){
                    //here the track's progress is being changed as per the progress bar
                    mPlayer.seekTo(progress);
                    //timer is being updated as per the progress of the seekbar
                    chronometer.setBase(SystemClock.elapsedRealtime()-mPlayer.getCurrentPosition());
                    lastProgress=progress;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    Runnable runnable=new Runnable() {
        @Override
        public void run() {
            seekUpdation();
        }
    };

    private void seekUpdation(){
        if(mPlayer!=null){
            int mCurrentPosition=mPlayer.getCurrentPosition();
            seekBar.setProgress(mCurrentPosition);
            lastProgress=mCurrentPosition;
        }
        mHandler.postDelayed(runnable,100);
    }

   /* @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater=getMenuInflater();
        inflater.inflate(R.menu.list_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.item_list:
                gotoRecordingListActivity();
                return true;
                default:
                    return super.onOptionsItemSelected(item);
        }
    }

    private void gotoRecordingListActivity(){
        Intent intent=new Intent(this, RecordingListActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }*/
}


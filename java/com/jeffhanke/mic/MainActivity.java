package com.jeffhanke.mic;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.media.audiofx.AcousticEchoCanceler;
import android.media.audiofx.AutomaticGainControl;
import android.media.audiofx.BassBoost;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;

import com.jeffhanke.mic.views.Microphone;

public class MainActivity extends AppCompatActivity {

    //Audio
    private Button mOn;
    private boolean isOn;
    private boolean isRecording;
    private AudioRecord record;
    private AudioTrack player;
    private AudioManager manager;
    private Microphone mic;
    private int recordState, playerState;
    private int minBuffer;

    //Popup Window
    private Button setting;
    private PopupWindow popupWindow;
    private LayoutInflater layoutInflater;
    private RelativeLayout relativeLayout;
    private int btnTextColor;

    //Variables for Setting
    private int seekR, seekG, seekB;
    private AudioManager audioManager;
    private String color;
    private Button colorChange1, colorChange2;
    private Switch speaker;
    private boolean speakerOn;

    //Audio Settings
    private final int source = MediaRecorder.AudioSource.VOICE_RECOGNITION;
    private final int channel_in = AudioFormat.CHANNEL_IN_MONO;
    private final int channel_out = AudioFormat.CHANNEL_OUT_MONO;
    private final int format = AudioFormat.ENCODING_PCM_16BIT;

    private final static int REQUEST_ENABLE_BT = 1;

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //https://stackoverflow.com/questions/27067273/play-back-audio-from-mic-in-real-time (Some ideas)
        setVolumeControlStream(AudioManager.MODE_IN_COMMUNICATION);

        //Needs permission both from manifest and runtime
        requestRecordAudioPermission();

        mic = (Microphone) findViewById(R.id.mMicrophone);
        mOn = (Button) findViewById(R.id.button);
        isOn = false;
        isRecording = false;

        manager = (AudioManager) this.getSystemService(Context.AUDIO_SERVICE);
        manager.setMode(AudioManager.MODE_IN_COMMUNICATION);

        //Bluetooth
        //https://stackoverflow.com/questions/7672334/how-to-check-if-bluetooth-is-enabled-programmatically
        BluetoothAdapter bluetooth = BluetoothAdapter.getDefaultAdapter();
        if (bluetooth == null){
            //No Bluetooth
        } else if (!bluetooth.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        initAudio();

        mOn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                mOn.getBackground().setColorFilter(getResources().getColor(!isOn ? R.color.colorOn : R.color.colorOff), PorterDuff.Mode.SRC_ATOP);
                isOn = !isOn;
                if(isOn) {
                    (new Thread() {
                        @Override
                        public void run()
                        {
                            startAudio();
                        }
                    }).start();
                } else {
                    endAudio();
                }
            }
        });

        //Setup Settings Menu
        setting = (Button) findViewById(R.id.settings);
        relativeLayout = (RelativeLayout) findViewById(R.id.main);
        color = "#545454";
        seekR = 84;
        seekG = 84;
        seekB = 84;
        speakerOn = false;
        btnTextColor = Color.WHITE;

        setting.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view){
                layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(LAYOUT_INFLATER_SERVICE);
                ViewGroup container = (ViewGroup) layoutInflater.inflate(R.layout.settings, null);

                popupWindow = new PopupWindow(container, 800, 1350, true);
                popupWindow.showAtLocation(relativeLayout, Gravity.CENTER,0,0);
                View popContainer = (View) popupWindow.getContentView().getRootView();
                Context context = popupWindow.getContentView().getContext();
                WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                WindowManager.LayoutParams layout = (WindowManager.LayoutParams) popContainer.getLayoutParams();
                layout.flags = WindowManager.LayoutParams.FLAG_DIM_BEHIND;
                layout.dimAmount = 0.3f;
                wm.updateViewLayout(popContainer, layout);

                //Color Changer
                colorChange1 = (Button) popContainer.findViewById(R.id.colorBtn1);
                colorChange1.setBackgroundColor(Color.parseColor(color));
                colorChange1.setTextColor(btnTextColor);

                colorChange2 = (Button) popContainer.findViewById(R.id.colorBtn2);
                colorChange2.setBackgroundColor(Color.parseColor(color));
                colorChange2.setTextColor(btnTextColor);

                colorChange1.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mic.setBaseColor(color);
                    }
                });

                colorChange2.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        relativeLayout.setBackgroundColor(Color.parseColor(color));
                    }
                });

                //Speaker Setup
                speaker = (Switch) popContainer.findViewById(R.id.speakerSwitch);
                speaker.setChecked(speakerOn);
                speaker.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        speakerOn = !speakerOn;
                        manager.setSpeakerphoneOn(speakerOn);
                    }
                });

                //Color Bar
                SeekBar sbR = (SeekBar) popContainer.findViewById(R.id.redSeekBar);
                SeekBar sbG = (SeekBar) popContainer.findViewById(R.id.greenSeekBar);
                SeekBar sbB = (SeekBar) popContainer.findViewById(R.id.blueSeekBar);

                sbR.setProgress(seekR);
                sbG.setProgress(seekG);
                sbB.setProgress(seekB);

                SeekBar.OnSeekBarChangeListener onSeekBarChangeListener = new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                        switch (arg0.getId()) {
                            case R.id.redSeekBar:
                                seekR = arg1;
                                break;
                            case R.id.greenSeekBar:
                                seekG = arg1;
                                break;
                            case R.id.blueSeekBar:
                                seekB = arg1;
                                break;
                        }
                        getStrCol(seekR, seekG, seekB);
                        colorChange1.setBackgroundColor(Color.parseColor(color));
                        colorChange2.setBackgroundColor(Color.parseColor(color));
                        btnTextColor = seekR + seekG + seekB < 250 ? Color.WHITE : Color.BLACK;
                        colorChange1.setTextColor(btnTextColor);
                        colorChange2.setTextColor(btnTextColor);
                    }
                };

                sbR.setOnSeekBarChangeListener(onSeekBarChangeListener);
                sbG.setOnSeekBarChangeListener(onSeekBarChangeListener);
                sbB.setOnSeekBarChangeListener(onSeekBarChangeListener);

                //Volume Bar
                audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
                int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                int curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                SeekBar volControl = (SeekBar) popContainer.findViewById(R.id.volbar);
                volControl.setMax(speakerOn ? 10 : maxVolume);
                volControl.setProgress(speakerOn && curVolume > 10 ? 10 : curVolume);
                volControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onStopTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar arg0) {
                    }

                    @Override
                    public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, 0);
                    }
                });

                relativeLayout.setOnTouchListener(new View.OnTouchListener(){
                    @Override
                    public boolean onTouch(View view, MotionEvent motionEvent){
                        popupWindow.dismiss();
                        return true;
                    }
                });
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
    public void initAudio() {
        //Tests all sample rates before selecting one that works
        int sample_rate = getSampleRate();
        minBuffer = AudioRecord.getMinBufferSize(sample_rate, channel_in, format);

        record = new AudioRecord(source, sample_rate, channel_in, format, minBuffer);
        recordState = record.getState();
        int id = record.getAudioSessionId();
        Log.d("Record", "ID: " + id);
        playerState = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            player = new AudioTrack(
                    new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build(),
                    new AudioFormat.Builder().setEncoding(format).setSampleRate(sample_rate).setChannelMask(channel_out).build(),
                    minBuffer,
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            playerState = player.getState();
            if(AcousticEchoCanceler.isAvailable()) {
                AcousticEchoCanceler echo = AcousticEchoCanceler.create(id);
                echo.setEnabled(true);
                Log.d("Echo", "Off");
            }
            if(NoiseSuppressor.isAvailable()) {
                NoiseSuppressor noise = NoiseSuppressor.create(id);
                noise.setEnabled(true);
                Log.d("Noise", "Off");
            }
            if(AutomaticGainControl.isAvailable()) {
                AutomaticGainControl gain = AutomaticGainControl.create(id);
                gain.setEnabled(false);
                Log.d("Gain", "Off");
            }
            BassBoost base = new BassBoost(1, player.getAudioSessionId());
            base.setStrength((short) 1000);
        }
    }

    public void startAudio() {
        int read = 0, write = 0;
        if(recordState == AudioRecord.STATE_INITIALIZED && playerState == AudioTrack.STATE_INITIALIZED) {
            record.startRecording();
            player.play();
            isRecording = true;
            Log.d("Record", "Recording...");
        }
        while(isRecording) {
            short[] audioData = new short[minBuffer];
            if(record != null)
                read = record.read(audioData, 0, minBuffer);
            else
                break;
            Log.d("Record", "Read: " + read);
            if(player != null)
                write = player.write(audioData, 0, read);
            else
                break;
            Log.d("Record", "Write: " + write);
        }
    }

    public void endAudio() {
        if(record != null) {
            if(record.getRecordingState() == AudioRecord.RECORDSTATE_RECORDING)
                record.stop();
            isRecording = false;
            Log.d("Record", "Stopping...");
        }
        if(player != null) {
            if(player.getPlayState() == AudioTrack.PLAYSTATE_PLAYING)
                player.stop();
            isRecording = false;
            Log.d("Player", "Stopping...");
        }
    }

    public int getSampleRate() {
        //Find a sample rate that works with the device
        for (int rate : new int[] {8000, 11025, 16000,  22050, 44100, 48000}) {
            int buffer = AudioRecord.getMinBufferSize(rate, channel_in, format);
            if (buffer > 0)
                return rate;
        }
        return -1;
    }

    //https://stackoverflow.com/questions/38033068/android-audiorecord-wont-initialize
    private void requestRecordAudioPermission() {
        //Check API version, do nothing if API version < 23
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.LOLLIPOP) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }
    }

    //https://stackoverflow.com/questions/38033068/android-audiorecord-wont-initialize
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 1: {
                //If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    //Permission was granted
                    Log.d("Activity", "Granted!");
                } else {
                    //Permission was denied
                    Log.d("Activity", "Denied!");
                    finish();
                }
                return;
            }
        }
    }

    public void getStrCol(int r, int g, int b) {
        color = "#";
        String colR = Integer.toHexString(r);
        color += colR.length() < 2 ? "0" + colR : colR;
        String colG = Integer.toHexString(g);
        color += colG.length() < 2 ? "0" + colG : colG;
        String colB = Integer.toHexString(b);
        color += colB.length() < 2 ? "0" + colB : colB;
    }
}
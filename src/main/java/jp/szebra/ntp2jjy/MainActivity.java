package jp.szebra.ntp2jjy;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements NTPSyncCallback, WFGenerateCallback {
  public static final String TAG = "MainActivity";
  private WaveformData wf;
  private long offset = 0;
  private Calendar cal; // Accurate synced calendar
  private Map<JJY.Signal, AudioTrack> sigMap;
  private JJY.Signal[] signals;
  private ByteBuffer[] bufferArray;
  private boolean isStarted = false;
  private boolean ntpReady, audioReady;
  
  private TextView logLabel;
  private ScrollView logContainer;
  
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    logLabel = findViewById(R.id.logLabel);
    logContainer = findViewById(R.id.logContainer);
    
    loadSoundData();
    
    sigMap = new HashMap<>();
    for (JJY.Signal sig : JJY.Signal.values()) {
      AudioTrack trk = new AudioTrack(AudioManager.STREAM_MUSIC,
        44100,
        AudioFormat.CHANNEL_OUT_MONO,
        AudioFormat.ENCODING_PCM_8BIT,
        sig == JJY.Signal.MORSE ? 44100 * 8 : 44100,
        AudioTrack.MODE_STATIC);
      if (sig == JJY.Signal.HIGH) trk.write(wf.bitHi, 0, wf.bitHi.length);
      else if (sig == JJY.Signal.LOW) trk.write(wf.bitLo, 0, wf.bitLo.length);
      else if (sig == JJY.Signal.MARKER) trk.write(wf.marker, 0, wf.marker.length);
      else if (sig == JJY.Signal.MORSE) trk.write(wf.jjyMorse, 0, wf.jjyMorse.length);
      sigMap.put(sig, trk);
    }
    new NTPSyncTask("ntp.nict.jp", MainActivity.this).execute();
  }
  
  void appendLog(String line) {
    logLabel.setText(logLabel.getText().toString() + line + "\n");
    logContainer.fullScroll(View.FOCUS_DOWN);
  }
  
  @Override
  public void onSynced(long offset) {
    appendLog("Synced, offset: " + offset);
    this.offset = offset;
    // Add >+-1s offset
    cal = Calendar.getInstance();
    long syncedMs = System.currentTimeMillis();
    cal.setTimeInMillis(syncedMs + offset);
    
    signals = JJY.generateMinuteData(cal);
    
    Handler h = new Handler(getMainLooper());
    int sec = cal.get(Calendar.SECOND);
//    h.postDelayed(generateNext, (59 - sec) * 1000);
    ntpReady = true;
//    if (audioReady)
    h.postDelayed(play, Math.abs(offset) % 1000 - (System.currentTimeMillis() - syncedMs));
  }
  
  private Runnable play = new Runnable() {
    @Override
    public void run() {
      long startTime = System.currentTimeMillis();
      cal.setTimeInMillis(startTime + offset);
      int sec = cal.get(Calendar.SECOND);
//    ByteBuffer buf = bufferArray[];
      for (AudioTrack t : sigMap.values())
        if (t.getState() == AudioTrack.STATE_INITIALIZED) t.stop();
      JJY.Signal sig = signals[sec];
      AudioTrack trk = sigMap.get(sig);
      assert trk != null;
      trk.setPlaybackHeadPosition(0);
      appendLog("Playing " + sec);
      trk.play();
//    track.write(bufArr, 0, bufArr.length);
      
      Handler h = new Handler(getMainLooper());
//    long delay = (long) (bufLen / 44.1) - (System.currentTimeMillis() - startTime);
      long delay = (long) (sig == JJY.Signal.MORSE ? 8000 : 1000) - (System.currentTimeMillis() - startTime);
      appendLog("Play next in " + delay);
      h.postDelayed(this, delay);
      if (sec == 59) generateNext.run();
    }
  };
  
  private Runnable generateNext = new Runnable() {
    @Override
    public void run() {
      Calendar calNext = (Calendar) cal.clone();
      calNext.setTimeInMillis(System.currentTimeMillis() + offset);
      calNext.add(Calendar.MINUTE, 1);
      calNext.set(Calendar.SECOND, 0);
//      new WaveformGenerateTask(MainActivity.this, waveformData).execute(cal);
      signals = JJY.generateMinuteData(cal);
      StringBuilder sb = new StringBuilder("[");
      for (int i = 0; i < signals.length; i++) {
        sb.append(i + ": " + signals[i] + ", ");
      }
      appendLog(sb.append("]").toString());
//      new Handler(getMainLooper()).postDelayed(this, 60 * 1000);
    }
  };
  
  @Override
  public void onGenerated(ByteBuffer[] array) {
    if (ntpReady && !isStarted) {
      new Handler(getMainLooper()).postDelayed(play, Math.abs(offset) % 1000);
    }
    appendLog("Generated " + array.length + " secs");
    bufferArray = array;
    audioReady = true;
  }
  
  private void loadSoundData() {
    try {
      byte[] jjyMorse, bitLo, bitHi, marker;
      jjyMorse = new byte[44100 * 8];
      getResources().openRawResource(R.raw.jjy_jjy_u).read(jjyMorse);
      bitLo = new byte[44100];
      getResources().openRawResource(R.raw.bit_lo_u).read(bitLo);
      bitHi = new byte[44100];
      getResources().openRawResource(R.raw.bit_hi_u).read(bitHi);
      marker = new byte[44100];
      getResources().openRawResource(R.raw.marker_u).read(marker);
      wf = new WaveformData(jjyMorse, bitHi, bitLo, marker);
    } catch (IOException e) {
      Log.e(TAG, "onCreate: loadSoundData", e);
      e.printStackTrace();
    }
  }
}
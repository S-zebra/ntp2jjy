package jp.szebra.ntp2jjy;

import android.os.AsyncTask;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Calendar;

/**
 * Created by s-zebra on 8/6/20.
 */
class WaveformGenerateTask extends AsyncTask<Calendar, Void, ByteBuffer[]> {
  private WeakReference<WFGenerateCallback> callbackWR;
  private WaveformData waveformData;
  
  public WaveformGenerateTask(WFGenerateCallback callback, WaveformData waveformData) {
    this.callbackWR = new WeakReference<>(callback);
    this.waveformData = waveformData;
  }
  
  @Override
  protected ByteBuffer[] doInBackground(Calendar... calendars) {
    if (waveformData == null) return null;
    Calendar c = calendars[0];
    JJY.Signal[] signals = JJY.generateMinuteData(c == null ? Calendar.getInstance() : c);
    ByteBuffer[] array = new ByteBuffer[60];
    for (int i = 60 - signals.length; i < signals.length; i++) {
      ByteBuffer buffer = ByteBuffer.allocate(signals[i] == JJY.Signal.MORSE ? 44100 * 8 : 44100);
      switch (signals[i]) {
        case MORSE:
//          newTrack.write(waveformData.jjyMorse, 0, waveformData.jjyMorse.length);
          buffer.put(waveformData.jjyMorse);
          break;
        case LOW:
//          newTrack.write(waveformData.bitLo, 0, waveformData.bitLo.length);
          buffer.put(waveformData.bitLo);
          break;
        case HIGH:
//          newTrack.write(waveformData.bitHi, 0, waveformData.bitHi.length);
          buffer.put(waveformData.bitHi);
          break;
        case MARKER:
//          newTrack.write(waveformData.marker, 0, waveformData.marker.length);
          buffer.put(waveformData.marker);
          break;
        default:
          break;
      }
      array[i] = buffer;
    }
    Log.d(getClass().getSimpleName(), "doInBackground(): Generated " + signals.length + " signals");
    return array;
  }
  
  @Override
  protected void onPostExecute(ByteBuffer[] array) {
    if (callbackWR.get() != null) callbackWR.get().onGenerated(array);
  }
}

interface WFGenerateCallback {
  void onGenerated(ByteBuffer[] array);
}

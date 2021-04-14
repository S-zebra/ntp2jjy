package jp.szebra.ntp2jjy;

import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.InetAddress;

/**
 * Created by s-zebra on 8/6/20.
 */
class NTPSyncTask extends AsyncTask<Void, Void, Long> {
  private WeakReference<NTPSyncCallback> callback;
  private String addr;
  public static final String TAG = "NTPSyncTask";
  
  public NTPSyncTask(String ntpAddress, NTPSyncCallback callback) {
    this.callback = new WeakReference<>(callback);
    this.addr = ntpAddress;
  }
  
  @Override
  protected Long doInBackground(Void... voids) {
    NTPUDPClient client = new NTPUDPClient();
    try {
      client.open();
      TimeInfo info = client.getTime(InetAddress.getByName(addr));
      info.computeDetails();
      return info.getOffset();
    } catch (IOException e) {
      Log.e(TAG, "doInBackground: ", e);
      return 0L;
    }
  }
  
  @Override
  protected void onPostExecute(Long aLong) {
    if (callback != null) callback.get().onSynced(aLong);
  }
}

interface NTPSyncCallback {
  void onSynced(long offset);
}
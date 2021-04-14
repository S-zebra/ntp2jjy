package jp.szebra.ntp2jjy;

import android.util.Log;

import java.util.Calendar;

/**
 * Created by s-zebra on 8/6/20.
 */
public class JJY {
  
  private static final int[] DIGITS = new int[]{200, 100, 0, 80, 40, 20, 10, 0, 8, 4, 2, 1};
  private static final int[] DIGITS_YEAR = new int[]{80, 40, 20, 10, 8, 4, 2, 1};
  
  private JJY() {
  }
  
  public static Signal[] generateMinuteData(Calendar c) {
    long beginTime = System.currentTimeMillis();
    Signal[] minuteData = new Signal[60];
    for (DataElem elem : DataElem.values()) {
      int id = elem.getCalendarElemID();
      Signal[] data = generateDigits(elem, id == Calendar.YEAR ? c.get(id) % 100 : c.get(id));
      System.arraycopy(data, 0, minuteData, elem.getOffsetInMin(), data.length);
    }
    minuteData[36] = evenParity(minuteData, 12, 18);
    minuteData[37] = evenParity(minuteData, 1, 8);
    
    int min = c.get(Calendar.MINUTE);
    if (min == 15 || min == 45) {
      //40-48: "JJY" in moles
      minuteData[40] = Signal.MORSE;
      for (int i = 41; i <= 48; i++) {
        minuteData[i] = Signal.NONE;
      }
      // [50]-[55]: Stop info
    }
    // [38]: SU1: Start or end DST in 6 days?
    // [40]: SU2: DST ongoing?
    // [53]: Leap second in this month?
    // [54]: Leap second type; Add: 1, Subtract: 0
    for (int i = 0; i < minuteData.length; i++) {
      if (i == 0 || i % 10 == 9) minuteData[i] = Signal.MARKER;
      else if (minuteData[i] == null) minuteData[i] = Signal.LOW; // Unused fields
    }
    Log.d("JJY", "generateMinuteData: took " + (System.currentTimeMillis() - beginTime) + " ms");
    return minuteData;
  }
  
  private static Signal evenParity(Signal[] data, int offset, int end) {
    int count = 0;
    for (int i = offset; i <= end; i++) {
      if (data[i] == Signal.HIGH) count++;
    }
    return count % 2 == 0 ? Signal.HIGH : Signal.LOW;
  }
  
  private static Signal[] generateDigits(int num, int[] digits, int nDigits) {
    int digOffset = digits.length - nDigits;
    Signal[] bits = new Signal[nDigits];
    if (num == 0) return bits;
    for (int i = digOffset; i < bits.length; i++) {
      bits[i - digOffset] = (digits[i]!=0 && num >= digits[i]) ? Signal.HIGH : Signal.LOW;
      Log.d("JJY", "generateDigits: num=" + num + ", digits[" + (i - digOffset) + "]=" + digits[i] + ", res=" + bits[i - digOffset]);
      if (bits[i - digOffset] == Signal.HIGH) num -= digits[i];
      if (num == 0) break;
    }
    return bits;
  }
  
  private static Signal[] generateDigits(DataElem elem, int num) {
    return generateDigits(num, elem == DataElem.YEAR ? DIGITS_YEAR : DIGITS, elem.getLength());
  }
  
  private enum DataElem {
    MINUTE(1, 8, Calendar.MINUTE),
    HOUR(12, 7, Calendar.HOUR_OF_DAY),
    TOTAL_DAYS(22, 12, Calendar.DAY_OF_YEAR),
    YEAR(41, 8, Calendar.YEAR),
    DAY_OF_WEEK(50, 3, Calendar.DAY_OF_WEEK);
    
    private int length;
    private int offset;
    private int calendarElemID;
    
    DataElem(int offset, int length, int calendarElemID) {
      this.length = length;
      this.offset = offset;
      this.calendarElemID = calendarElemID;
    }
    
    public int getLength() {
      return length;
    }
    
    public int getOffsetInMin() {
      return offset;
    }
    
    public int getCalendarElemID() {
      return calendarElemID;
    }
  }
  
  public enum Signal {
    LOW, HIGH, MARKER, MORSE, NONE;
  }
}


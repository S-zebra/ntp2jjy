package jp.szebra.ntp2jjy;

/**
 * Created by s-zebra on 8/6/20.
 */
final class WaveformData {
  final byte[] jjyMorse, bitHi, bitLo, marker;
  
  public WaveformData(byte[] jjyMorse, byte[] bitHi, byte[] bitLo, byte[] marker) {
    this.jjyMorse = jjyMorse;
    this.bitHi = bitHi;
    this.bitLo = bitLo;
    this.marker = marker;
  }
}

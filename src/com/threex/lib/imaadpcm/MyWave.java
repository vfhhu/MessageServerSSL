package com.threex.lib.imaadpcm;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by leo on 2017/6/26.
 */

public class MyWave {
    byte []RIFF=new byte[4];
    byte []LENGTH=new byte[4];
    byte []WAVE=new byte[4];
    byte []FMT=new byte[4];
    byte []LENGTH2=new byte[4];
    byte []FORMAT=new byte[2];
    byte []CHANNEL=new byte[2];
    byte []SAMPLE_RATE=new byte[4];
    byte []BYTE_S=new byte[4];
    byte []BLOCK=new byte[2];
    byte []DEEP=new byte[2];

    byte []DATA_LABEL=new byte[4];
    byte []LENGTH3=new byte[4];

    public MyWave(InputStream mFStream)
    {
        byte[] mBuffer = null;
        mBuffer = new byte[4];
// Read RIFF Symbol
        try {
            mFStream.read(RIFF);
            mFStream.read(LENGTH);// Read File Length
            mFStream.read(WAVE);// Read Wave Symbol
            mFStream.read(FMT);// Read Fmt Symbol
            mFStream.read(LENGTH2);// Read unknowed
            mFStream.read(FORMAT);// 格式类型
            mFStream.read(CHANNEL);// 通道数
            mFStream.read(SAMPLE_RATE);// 采样率
            mFStream.read(BYTE_S);// 波形音频数据传送率
            mFStream.read(BLOCK);// 数据块调整数
            mFStream.read(DEEP);// 样本数据位数
            mFStream.read(DATA_LABEL);// 数据标记符
            mFStream.read(LENGTH3);// 语音数据长度
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public String toString(){
        String retStr="";
        retStr += new String(RIFF, StandardCharsets.UTF_8)+"\n";
        retStr += byteToInt(LENGTH)+"\n";
        retStr += new String(WAVE, StandardCharsets.UTF_8)+"\n";
        retStr += new String(FMT, StandardCharsets.UTF_8)+"\n";
        retStr += byteToInt(LENGTH2)+"\n";
        retStr += byteToInt2(FORMAT)+"\n";
        retStr += byteToInt2(CHANNEL)+"\n";
        retStr += byteToInt(SAMPLE_RATE)+"\n";
        retStr += byteToInt(BYTE_S)+"\n";
        retStr += byteToInt2(BLOCK)+"\n";
        retStr += byteToInt2(DEEP)+"\n";
        retStr += new String(DATA_LABEL, StandardCharsets.UTF_8)+"\n";
        retStr += byteToInt(LENGTH3)+"\n";

        return retStr;
    }
    public static byte[] get_write_header(long totalAudioLen, long longSampleRate, int channels, long byteRate){
        long totalDataLen=totalAudioLen+36;
        byte[] header = new byte[44];
        header[0] = 'R'; // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';  //WAVE
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f'; // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;   // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
//        header[32] = (byte) (2 * 16 / 8); // block align
        header[32] = (byte) (1 * 16 / 8); // block align
        header[33] = 0;
        header[34] = 16;  // bits per sample
        header[35] = 0;
        header[36] = 'd'; //data
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);
        return header;
    }
    public int byteToInt(byte[] b){
        int MASK = 0xFF;
        int result = 0;
        result = b[0] & MASK;
        result = result + ((b[1] & MASK) << 8);
        result = result + ((b[2] & MASK) << 16);
        result = result + ((b[3] & MASK) << 24);
        return result;
    }
    public int byteToInt2(byte[] b){
        int MASK = 0xFF;
        int result = 0;
        result = b[0] & MASK;
        result = result + ((b[1] & MASK) << 8);
        return result;
    }
}

package com.threex.lib.imaadpcm;


import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by leo on 2017/6/26.
 */

public class MyAdpcm {

    byte []RIFF=new byte[4];
    byte []LENGTH=new byte[4];
    byte []WAVE=new byte[4];
    byte []FMT=new byte[4];
    byte []LENGTH2=new byte[4];
    byte []FORMAT=new byte[2];
    byte []CHANNEL=new byte[2];
    byte []SAMPLE_RATE=new byte[4];
    byte []BYTES_PER_SEC=new byte[4];
    byte []BLOCK=new byte[2];
    byte []BITS_PER_SAMPLE=new byte[2];
    public MyAdpcm(InputStream input){
        try {
            input.read(RIFF);
            input.read(LENGTH);
            input.read(WAVE);
            input.read(FMT);
            input.read(LENGTH2);
            input.read(FORMAT);
            input.read(CHANNEL);
            input.read(SAMPLE_RATE);
            input.read(BYTES_PER_SEC);
            input.read(BLOCK);
            input.read(BITS_PER_SAMPLE);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getRIFF() {
        return RIFF;
    }

    public byte[] getLENGTH() {
        return LENGTH;
    }

    public byte[] getWAVE() {
        return WAVE;
    }

    public byte[] getFMT() {
        return FMT;
    }

    public byte[] getLENGTH2() {
        return LENGTH2;
    }

    public byte[] getFORMAT() {
        return FORMAT;
    }

    public byte[] getCHANNEL() {
        return CHANNEL;
    }

    public byte[] getSAMPLE_RATE() {
        return SAMPLE_RATE;
    }

    public byte[] getBYTES_PER_SEC() {
        return BYTES_PER_SEC;
    }

    public byte[] getBLOCK() {
        return BLOCK;
    }

    public byte[] getBITS_PER_SAMPLE() {
        return BITS_PER_SAMPLE;
    }

    public byte[] decodeToPcm(InputStream input, int length){
        byte retB[]=new byte[length];
        int index=0,cur_sample=0;

        int block_length=byteToInt2(BLOCK);

        try {
            byte block[]=new byte[block_length];
            int count=0;
            int read=input.read(block,0,block_length);
            String hex1=byteToHexString(block);

            block=new byte[block_length];
            read=input.read(block,0,block_length);
            String hex2=byteToHexString(block);

//            Log.d("HEX1",hex1);
//            Log.d("HEX2",hex1);
//            while(read>-1){
//                count++;
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return retB;
    }

    public int get_header_length_all(){
        return byteToInt(LENGTH2)+20;
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
        retStr += byteToInt(BYTES_PER_SEC)+"\n";
        retStr += byteToInt2(BLOCK)+"\n";
        retStr += byteToInt2(BITS_PER_SAMPLE)+"\n";



        return retStr;
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

    public byte[] hexToBytes(String hexString) {
        hexString=hexString.trim().replace(" ","");
        char[] hex = hexString.toCharArray();
        int length = hex.length / 2;
        byte[] rawData = new byte[length];
        for (int i = 0; i < length; i++) {
            int high = Character.digit(hex[i * 2], 16);
            int low = Character.digit(hex[i * 2 + 1], 16);
            int value = (high << 4) | low;
            if (value > 127)
                value -= 256;
            rawData[i] = (byte) value;
        }
        return rawData;
    }
    public String byteToHexString(byte []data) {
        String str = "";
        for (int i = 0; i < data.length; i++) {
            str += String.format("%02X", data[i]) + " ";
        }
        return str.trim();
    }
}

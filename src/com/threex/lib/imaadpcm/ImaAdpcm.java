package com.threex.lib.imaadpcm;

/**
 * Created by leo on 2017/6/26.
 */

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/*
	A simple, good-enough-in-1995 quality audio compression algorithm.
	The basic algorithm encodes the difference between each sample into
	4 bits, increasing or decreasing the quantization step size depending
	on the size of the difference. A 16-bit stereo sample is neatly packed
	into 1 byte.
*/
public class ImaAdpcm {
    public static final String VERSION = "20101025 (c)2010 mumart@gmail.com";

//    private static final byte[] stepIdxTable = {
//            8, 6, 4, 2, -1, -1, -1, -1, -1, -1, -1, -1, 2, 4, 6, 8
//    };

    private static final byte[] stepIdxTable = {
            -1, -1, -1, -1, 2, 4, 6, 8
    };
    private static final short[] stepTable = {
            7, 8, 9, 10, 11, 12, 13, 14, 16, 17,
            19, 21, 23, 25, 28, 31, 34, 37, 41, 45,
            50, 55, 60, 66, 73, 80, 88, 97, 107, 118,
            130, 143, 157, 173, 190, 209, 230, 253, 279, 307,
            337, 371, 408, 449, 494, 544, 598, 658, 724, 796,
            876, 963, 1060, 1166, 1282, 1411, 1552, 1707, 1878, 2066,
            2272, 2499, 2749, 3024, 3327, 3660, 4026, 4428, 4871, 5358,
            5894, 6484, 7132, 7845, 8630, 9493, 10442, 11487, 12635, 13899,
            15289, 16818, 18500, 20350, 22385, 24623, 27086, 29794, 32767
    };

    private int lStepIdx, rStepIdx, lPredicted, rPredicted;

    /*
        Reset the ADPCM predictor.
        Call when encoding or decoding a new stream.
    */
    public void reset() {
        lStepIdx = rStepIdx = 0;
        lPredicted = rPredicted = 0;
    }

    /*
        Encode count samples of 16-bit stereo
        little-endian PCM audio data to count bytes of ADPCM.
        The output buffer is used for temporary data and must be at least count * 4 in size.
    */
    public void encode( InputStream input, byte[] output, int count ) throws IOException {
        readFully( input, output, 0, count * 4 );
        int inputIdx = 0, outputIdx = 0;
        while( outputIdx < count ) {
            int lSam  = ( output[ inputIdx++ ] & 0xFF ) | ( output[ inputIdx++ ] << 8 );
            int rSam  = ( output[ inputIdx++ ] & 0xFF ) | ( output[ inputIdx++ ] << 8 );
            int lStep = stepTable[ lStepIdx ];
            int rStep = stepTable[ rStepIdx ];
            int lCode = ( ( lSam - lPredicted ) * 4 + lStep * 8 ) / lStep;
            int rCode = ( ( rSam - rPredicted ) * 4 + rStep * 8 ) / rStep;
            if( lCode > 15 ) lCode = 15;
            if( rCode > 15 ) rCode = 15;
            if( lCode <  0 ) lCode =  0;
            if( rCode <  0 ) rCode =  0;
            lPredicted += ( ( lCode * lStep ) >> 2 ) - ( ( 15 * lStep ) >> 3 );
            rPredicted += ( ( rCode * rStep ) >> 2 ) - ( ( 15 * rStep ) >> 3 );
            if( lPredicted >  32767 ) lPredicted =  32767;
            if( rPredicted >  32767 ) rPredicted =  32767;
            if( lPredicted < -32768 ) lPredicted = -32768;
            if( rPredicted < -32768 ) rPredicted = -32768;
            lStepIdx += stepIdxTable[ lCode ];
            rStepIdx += stepIdxTable[ rCode ];
            if( lStepIdx > 88 ) lStepIdx = 88;
            if( rStepIdx > 88 ) rStepIdx = 88;
            if( lStepIdx <  0 ) lStepIdx =  0;
            if( rStepIdx <  0 ) rStepIdx =  0;
            output[ outputIdx++ ] = ( byte ) ( ( lCode << 4 ) | rCode );
        }
    }

    /*
        Decode count samples of ADPCM to 16-bit stereo little-endian PCM audio data.
    */
//    public void decode( InputStream input, byte[] output, int count ) throws IOException {
//        int read_length=readFully( input, output, count * 3, count );
//        int inputIdx = count * 3, outputIdx = 0, outputEnd = count * 4;
////        int inputIdx = count * 3, outputIdx = 0, outputEnd = read_length;
//        while( outputIdx < outputEnd ) {
//            int lCode = output[ inputIdx++ ] & 0xFF;
//            int rCode = lCode & 0xF;
//            lCode = lCode >> 4;
//            int lStep = stepTable[ lStepIdx ];
//            int rStep = stepTable[ rStepIdx ];
//            lPredicted += ( ( lCode * lStep ) >> 2 ) - ( ( 15 * lStep ) >> 3 );
//            rPredicted += ( ( rCode * rStep ) >> 2 ) - ( ( 15 * rStep ) >> 3 );
//            if( lPredicted >  32767 ) lPredicted =  32767;
//            if( rPredicted >  32767 ) rPredicted =  32767;
//            if( lPredicted < -32768 ) lPredicted = -32768;
//            if( rPredicted < -32768 ) rPredicted = -32768;
//            output[ outputIdx++ ] = ( byte )   lPredicted;
//            output[ outputIdx++ ] = ( byte ) ( lPredicted >> 8 );
//            output[ outputIdx++ ] = ( byte )   rPredicted;
//            output[ outputIdx++ ] = ( byte ) ( rPredicted >> 8 );
//            lStepIdx += stepIdxTable[ lCode ];
//            rStepIdx += stepIdxTable[ rCode ];
//            if( lStepIdx > 88 ) lStepIdx = 88;
//            if( rStepIdx > 88 ) rStepIdx = 88;
//            if( lStepIdx <  0 ) lStepIdx =  0;
//            if( rStepIdx <  0 ) rStepIdx =  0;
//        }
//    }



int tmp_cnt=0;   
    
    public void decode( InputStream input, byte[] output, int count ) throws IOException {
        byte codes[]=new byte[count];
        int read_length=readFully( input, codes, 0, count );
        int inputIdx = 0, outputIdx = 0, outputEnd = output.length-1;


        byte codeSample1=codes[ inputIdx++ ];
        byte codeSample2=codes[ inputIdx++ ];
//        System.out.println("=====decode");        
//        System.out.println("====="+String.format("%02X", codeSample1)+","+String.format("%02X", codeSample2));
        //System.out.println("====="+byteToInt2(new byte[]{codeSample1,codeSample2}));
        byte codeIndex=codes[ inputIdx++ ];
        byte codeReserved=codes[ inputIdx++ ];

        output[outputIdx++]=codeSample1;
        output[outputIdx++]=codeSample2;

        //int index=(codeIndex & 0xFF),cur_sample=(codeSample2 & 0xFF);
        int index=(codeIndex & 0xFF);
        int cur_sample=this.byteToShort(new byte[]{codeSample1,codeSample2});
        while( outputIdx < outputEnd && inputIdx<codes.length ) {
//            Log.d("INDEX",outputIdx+","+inputIdx+","+outputEnd);

            byte codeB=codes[ inputIdx++ ];
            int Code1=((codeB >> 7) & 0x1)*8+((codeB >> 6) & 0x1)*4+((codeB >> 5) & 0x1)*2+((codeB >> 4) & 0x1)*1;
            int Code2=((codeB >> 3) & 0x1)*8+((codeB >> 2) & 0x1)*4+((codeB >> 1) & 0x1)*2+((codeB ) & 0x1)*1;
            
            int Code = Code2 & 0xFF;
            int fg=0;
            if ((Code & 8) != 0) fg=1 ;
            Code&=7;
            
            if (index<0) index=0;
            if (index>88) index=88;
            int diff = (int)((stepTable[index]*Code) /4.0+ stepTable[index] / 8.0);            
            if (fg==1) diff=-diff;
            cur_sample+=diff;
            
            if (cur_sample>32767) cur_sample=32767;
            else if (cur_sample<-32768) cur_sample= -32768;
            byte byteSample[]=ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) cur_sample).array();            
//            if(cur_sample>32767 || cur_sample<-32768)System.out.println("block"+tmp_cnt+" diff:"+diff+",cur_sample:"+cur_sample);   
            
            output[outputIdx++]=byteSample[0];
            output[outputIdx++]=byteSample[1];

            index+=stepIdxTable[Code];
            if (index<0) index=0;
            if (index>88) index=88;



            Code = Code1 & 0xFF;
            fg=0;
            if ((Code & 8) != 0) fg=1 ;
            Code&=7;
            diff = (int)((stepTable[index]*Code) /4.0 + stepTable[index] / 8.0);
            if (fg==1) diff=-diff;
            cur_sample+=diff;
            if (cur_sample>32767) cur_sample=32767;
            else if (cur_sample<-32768) cur_sample= -32768;
            byteSample=ByteBuffer.allocate(2).order(ByteOrder.LITTLE_ENDIAN).putShort((short) cur_sample).array();
//            if(cur_sample>32767 || cur_sample<-32768)System.out.println("block"+tmp_cnt+" diff:"+diff+",cur_sample:"+cur_sample);
            
            output[outputIdx++]=byteSample[0];
            output[outputIdx++]=byteSample[1];


            index+=stepIdxTable[Code];
            if (index<0) index=0;
            if (index>88) index=88;

        }
        tmp_cnt++;
        
    }


    /*
        Convert a 16-bit stereo Wav stream to an IMA ADPCM stream.
    */
    public static void convertWavToAdpcm( InputStream input, OutputStream output ) throws IOException {
        final int BUF_SAMPLES = 16384;
        byte[] buf = new byte[ BUF_SAMPLES * 4 ];
        ImaAdpcm imaAdpcm = new ImaAdpcm();
        int samples = readWav( input );
        while( samples > 0 ) {
            int count = samples > BUF_SAMPLES ? BUF_SAMPLES : samples;
            imaAdpcm.encode( input, buf, count );
            output.write( buf, 0, count );
            samples -= count;
        }
    }

    public static byte[] convertAdpcmToWav( InputStream input,int count ) throws IOException {
//        final int BUF_SAMPLES = 16384;
        final int BUF_SAMPLES = 256;
        byte []tmp=new byte[0];
        byte []out=new byte[0];
        ImaAdpcm imaAdpcm = new ImaAdpcm();
        byte[] buffer = new byte[ 1010 ];
        while( count > 0 ) {
            int samples = count > BUF_SAMPLES ? BUF_SAMPLES : count;
            imaAdpcm.decode( input, buffer, samples );

            out=new byte[tmp.length+buffer.length];
            System.arraycopy(tmp,0,out,0,tmp.length);
            System.arraycopy(buffer,0,out,tmp.length,buffer.length);
            tmp=out.clone();

            count -= samples;
        }
        return out;




//        final int BUF_SAMPLES = 16384;
//        try {
//            ImaAdpcm imaAdpcm = new ImaAdpcm();
//            byte[] buffer = new byte[ BUF_SAMPLES * 4 ];
//            while( count > 0 ) {
//                int samples = count > BUF_SAMPLES ? BUF_SAMPLES : count;
//                imaAdpcm.decode( input, buffer, samples );
//
//                output.write( buffer, 0, samples * 4 );
////                sourceLine.write( buffer, 0, samples * 4 );
//                count -= samples;
//            }
////            sourceLine.drain();
//        } finally {
////            sourceLine.close();
//        }



    }

    /*
        Read the header of a 16-bit stereo WAV file.
        The InputStream is positioned at the start of the data.
        The number of samples in the file are returned.
    */
    public static int readWav( InputStream input ) throws IOException {
		/*
			CHAR[4] "RIFF"
			UINT32  Size of following data. Sample data length+36. Must be even.
			  CHAR[4] "WAVE"
				CHAR[4] "fmt "
				UINT32  PCM Header chunk size = 16
				  UINT16 0x0001 (PCM)
				  UINT16 NumChannels
				  UINT32 SampleRate
				  UINT32 BytesPerSec = samplerate*frame size
				  UINT16 frame Size (eg 4 bytes for stereo PCM16)
				  UINT16 BitsPerSample
				CHAR[4] "data"
				UINT32 Length of sample data.
				<Samples>
		*/
        if( !"RIFF".equals( readASCII( input, 4 ) ) ) throw new IOException( "RIFF header not found." );
        int dataSize = readInt32( input );
        if( !"WAVE".equals( readASCII( input, 4 ) ) ) throw new IOException( "WAVE header not found." );
        if( !"fmt ".equals( readASCII( input, 4 ) ) ) throw new IOException( "'fmt' header not found." );
        int chunkSize = readInt32( input );
        int format = readInt16( input );
        if( format != 1 ) throw new IOException( "Format is not PCM." );
        int channels = readInt16( input );
        if( channels != 2 ) throw new IOException( "Number of channels must be 2." );
        int sampleRate = readInt32( input );
        int bytesPerSec = readInt32( input );
        int frameSize = readInt16( input );
        if( frameSize != 4 ) throw new IOException( "Frame size must be 4." );
        int bits = readInt16( input );
        if( bits != 16 ) throw new IOException( "PCM data must be 16 bit." );
        if( !"data".equals( readASCII( input, 4 ) ) ) throw new IOException( "'data' header not found." );
        int dataLen = readInt32( input );
        return dataLen / 4;
    }

    /* Read a 16-bit little-endian unsigned integer from input.*/
    public static int readInt16( InputStream input ) throws IOException {
        return ( input.read() & 0xFF ) | ( ( input.read() & 0xFF ) << 8 );
    }

    /* Read a 32-bit little-endian signed integer from input.*/
    public static int readInt32( InputStream input ) throws IOException {
        return ( input.read() & 0xFF ) | ( ( input.read() & 0xFF ) << 8 )
                | ( ( input.read() & 0xFF ) << 16 ) | ( ( input.read() & 0xFF ) << 24 );
    }

    /* Return a String containing count characters of ASCII/ISO-8859-1 text from input. */
    public static String readASCII( InputStream input, int count ) throws IOException {
        byte[] chars = new byte[ count ];
        readFully( input, chars, 0, count );
        return new String( chars, "ISO-8859-1" );
    }

    /* Read no less than count bytes from input into the output array. */
    public static int readFully( InputStream input, byte[] output, int offset, int count ) throws IOException {
        int ret_length=0;
        int end = offset + count;
        while( offset < end ) {
            int read = input.read( output, offset, end - offset );
//            if( read < 0 ) throw new java.io.EOFException();
            if( read < 0 ) break;
            offset += read;
            ret_length+=read;
        }
        return ret_length;
    }

//    public static void main( String[] args ) {
//        try {
//            if( args.length != 2 ) {
//                System.out.println( "IMA ADPCM Converter " + VERSION );
//                System.out.println( "Usage: java " + ImaAdpcm.class.getName() + " input.wav output.ima" );
//                System.exit( 0 );
//            }
//            String wavFileName = args[ 0 ];
//            String imaFileName = args[ 1 ];
//            System.out.println( "Converting " + wavFileName + " to " + imaFileName );
//            InputStream input = new java.io.FileInputStream( wavFileName );
//            OutputStream output = new java.io.FileOutputStream( imaFileName );
//            try {
//                convertWavToAdpcm( input, output );
//            } finally {
//                output.close();
//            }
//        } catch( Exception e ) {
//            //e.printStackTrace();
//            System.err.println( e );
//            System.exit( 1 );
//        }
//    }
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
    public short byteToShort(byte[] b){
        int MASK = 0xFF;
        short result = 0;
        result = (short) (b[0] & MASK);
        result = (short) (result + ((b[1] & MASK) << 8));
        return result;
        //return (short)((b[0]<<8) | (b[1]));
    

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

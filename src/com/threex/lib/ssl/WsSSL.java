package com.threex.lib.ssl;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.xml.bind.DatatypeConverter;
import java.io.*;
import java.math.BigInteger;
import java.nio.channels.FileChannel;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAPrivateCrtKeySpec;

public class WsSSL {
    public static SSLContext getContext(String path_key, String path_crt, String path_ca_bundle,String target_path) {
        SSLContext context;
        String password = "";
        String pathname = "pem";
        try {
            String c_path = path_crt;
            if(path_ca_bundle!=null && path_ca_bundle.trim().length()>0 && target_path!=null){
                c_path=target_path+"ca.txt";
                File resultFile = new File(c_path);
                try {
                    FileChannel resultFileChannel = new FileOutputStream(resultFile,true).getChannel();

                    FileChannel blk1 = new FileInputStream(path_crt).getChannel();
                    resultFileChannel.transferFrom(blk1,resultFileChannel.size(),blk1.size());
                    blk1.close();

                    FileChannel blk2 = new FileInputStream(path_ca_bundle).getChannel();
                    resultFileChannel.transferFrom(blk2,resultFileChannel.size(),blk2.size());
                    blk2.close();

                    resultFileChannel.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    c_path = path_crt;
                } catch (IOException e) {
                    e.printStackTrace();
                    c_path = path_crt;
                }
            }


            X509Certificate cert = (X509Certificate) CertificateFactory.getInstance("X.509").generateCertificate(new FileInputStream(c_path));
            Certificate[] certA = parseDERFromPEMtoArr(getBytes(new File(c_path)),"-----BEGIN CERTIFICATE-----", "-----END CERTIFICATE-----");
//			X509Certificate cert = generateCertificateFromDER(certBytes);

            context = SSLContext.getInstance("TLS");//
            byte[] keyBytes = parseDERFromPEM(getBytes(new File(path_key)),"-----BEGIN RSA PRIVATE KEY-----", "-----END RSA PRIVATE KEY-----");
//			byte[] keyBytes = getBytes(new File(path_key));//
            RSAPrivateKey key = generatePrivateKeyFromDER(keyBytes);





            KeyStore keystore = KeyStore.getInstance("JKS");
            keystore.load(null);
            keystore.setCertificateEntry("cert-alias", cert);
            keystore.setKeyEntry("key-alias", key, password.toCharArray(), certA);
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            kmf.init(keystore, password.toCharArray());

            KeyManager[] km = kmf.getKeyManagers();

            context.init(km, null, null);
        } catch (Exception e) {
            context = null;
            e.printStackTrace();
        }
        return context;
    }
    private static Certificate[] parseDERFromPEMtoArr(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        Certificate[] ret=new Certificate[tokens.length-1];
        for(int i=1;i<tokens.length;i++){
            String _tmp=tokens[i].split(endDelimiter)[0];

            try {
                ret[i-1]=generateCertificateFromDER(DatatypeConverter.parseBase64Binary(_tmp));
            } catch (CertificateException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }
    private static byte[] parseDERFromPEM(byte[] pem, String beginDelimiter, String endDelimiter) {
        String data = new String(pem);
        String[] tokens = data.split(beginDelimiter);
        tokens = tokens[1].split(endDelimiter);
        return DatatypeConverter.parseBase64Binary(tokens[0]);
    }

    private static RSAPrivateKey generatePrivateKeyFromDER(byte[] keyBytes)
//			throws InvalidKeySpecException, NoSuchAlgorithmException
            throws IOException, GeneralSecurityException
    {

        DerParser parser = new DerParser(keyBytes);

        Asn1Object sequence = parser.read();
        if (sequence.getType() != DerParser.SEQUENCE)
            throw new IOException("Invalid DER: not a sequence"); //$NON-NLS-1$

        // Parse inside the sequence
        parser = sequence.getParser();

        parser.read(); // Skip version
        BigInteger modulus = parser.read().getInteger();
        BigInteger publicExp = parser.read().getInteger();
        BigInteger privateExp = parser.read().getInteger();
        BigInteger prime1 = parser.read().getInteger();
        BigInteger prime2 = parser.read().getInteger();
        BigInteger exp1 = parser.read().getInteger();
        BigInteger exp2 = parser.read().getInteger();
        BigInteger crtCoef = parser.read().getInteger();

        RSAPrivateCrtKeySpec keySpec = new RSAPrivateCrtKeySpec(
                modulus, publicExp, privateExp, prime1, prime2,
                exp1, exp2, crtCoef);

        KeyFactory factory = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) factory.generatePrivate(keySpec);




//		PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
//
//		KeyFactory factory = KeyFactory.getInstance("RSA");
//
//		return (RSAPrivateKey) factory.generatePrivate(spec);
    }
    static class DerParser {

        // Classes
        public final static int UNIVERSAL = 0x00;
        public final static int APPLICATION = 0x40;
        public final static int CONTEXT = 0x80;
        public final static int PRIVATE = 0xC0;

        // Constructed Flag
        public final static int CONSTRUCTED = 0x20;

        // Tag and data types
        public final static int ANY = 0x00;
        public final static int BOOLEAN = 0x01;
        public final static int INTEGER = 0x02;
        public final static int BIT_STRING = 0x03;
        public final static int OCTET_STRING = 0x04;
        public final static int NULL = 0x05;
        public final static int OBJECT_IDENTIFIER = 0x06;
        public final static int REAL = 0x09;
        public final static int ENUMERATED = 0x0a;
        public final static int RELATIVE_OID = 0x0d;

        public final static int SEQUENCE = 0x10;
        public final static int SET = 0x11;

        public final static int NUMERIC_STRING = 0x12;
        public final static int PRINTABLE_STRING = 0x13;
        public final static int T61_STRING = 0x14;
        public final static int VIDEOTEX_STRING = 0x15;
        public final static int IA5_STRING = 0x16;
        public final static int GRAPHIC_STRING = 0x19;
        public final static int ISO646_STRING = 0x1A;
        public final static int GENERAL_STRING = 0x1B;

        public final static int UTF8_STRING = 0x0C;
        public final static int UNIVERSAL_STRING = 0x1C;
        public final static int BMP_STRING = 0x1E;

        public final static int UTC_TIME = 0x17;
        public final static int GENERALIZED_TIME = 0x18;

        protected InputStream in;

        /**
         * Create a new DER decoder from an input stream.
         *
         * @param in
         *            The DER encoded stream
         */
        public DerParser(InputStream in) throws IOException {
            this.in = in;
        }

        /**
         * Create a new DER decoder from a byte array.
         *
         * @param The
         *            encoded bytes
         * @throws IOException
         */
        public DerParser(byte[] bytes) throws IOException {
            this(new ByteArrayInputStream(bytes));
        }

        /**
         * Read next object. If it's constructed, the value holds
         * encoded content and it should be parsed by a new
         * parser from <code>Asn1Object.getParser</code>.
         *
         * @return A object
         * @throws IOException
         */
        public Asn1Object read() throws IOException {
            int tag = in.read();

            if (tag == -1)
                throw new IOException("Invalid DER: stream too short, missing tag"); //$NON-NLS-1$

            int length = getLength();

            byte[] value = new byte[length];
            int n = in.read(value);
            if (n < length)
                throw new IOException("Invalid DER: stream too short, missing value"); //$NON-NLS-1$

            Asn1Object o = new Asn1Object(tag, length, value);

            return o;
        }

        /**
         * Decode the length of the field. Can only support length
         * encoding up to 4 octets.
         *
         * <p/>In BER/DER encoding, length can be encoded in 2 forms,
         * <ul>
         * <li>Short form. One octet. Bit 8 has value "0" and bits 7-1
         * give the length.
         * <li>Long form. Two to 127 octets (only 4 is supported here).
         * Bit 8 of first octet has value "1" and bits 7-1 give the
         * number of additional length octets. Second and following
         * octets give the length, base 256, most significant digit first.
         * </ul>
         * @return The length as integer
         * @throws IOException
         */
        private int getLength() throws IOException {

            int i = in.read();
            if (i == -1)
                throw new IOException("Invalid DER: length missing"); //$NON-NLS-1$

            // A single byte short length
            if ((i & ~0x7F) == 0)
                return i;

            int num = i & 0x7F;

            // We can't handle length longer than 4 bytes
            if ( i >= 0xFF || num > 4)
                throw new IOException("Invalid DER: length field too big (" //$NON-NLS-1$
                        + i + ")"); //$NON-NLS-1$

            byte[] bytes = new byte[num];
            int n = in.read(bytes);
            if (n < num)
                throw new IOException("Invalid DER: length too short"); //$NON-NLS-1$

            return new BigInteger(1, bytes).intValue();
        }

    }
    static class Asn1Object {

        protected final int type;
        protected final int length;
        protected final byte[] value;
        protected final int tag;

        /**
         * Construct a ASN.1 TLV. The TLV could be either a
         * constructed or primitive entity.
         *
         * <p/>The first byte in DER encoding is made of following fields,
         * <pre>
         *-------------------------------------------------
         *|Bit 8|Bit 7|Bit 6|Bit 5|Bit 4|Bit 3|Bit 2|Bit 1|
         *-------------------------------------------------
         *|  Class    | CF  |     +      Type             |
         *-------------------------------------------------
         * </pre>
         * <ul>
         * <li>Class: Universal, Application, Context or Private
         * <li>CF: Constructed flag. If 1, the field is constructed.
         * <li>Type: This is actually called tag in ASN.1. It
         * indicates data type (Integer, String) or a construct
         * (sequence, choice, set).
         * </ul>
         *
         * @param tag Tag or Identifier
         * @param length Length of the field
         * @param value Encoded octet string for the field.
         */
        public Asn1Object(int tag, int length, byte[] value) {
            this.tag = tag;
            this.type = tag & 0x1F;
            this.length = length;
            this.value = value;
        }

        public int getType() {
            return type;
        }

        public int getLength() {
            return length;
        }

        public byte[] getValue() {
            return value;
        }

        public boolean isConstructed() {
            return  (tag & DerParser.CONSTRUCTED) == DerParser.CONSTRUCTED;
        }

        /**
         * For constructed field, return a parser for its content.
         *
         * @return A parser for the construct.
         * @throws IOException
         */
        public DerParser getParser() throws IOException {
            if (!isConstructed())
                throw new IOException("Invalid DER: can't parse primitive entity"); //$NON-NLS-1$

            return new DerParser(value);
        }

        /**
         * Get the value as integer
         *
         * @return BigInteger
         * @throws IOException
         */
        public BigInteger getInteger() throws IOException {
            if (type != DerParser.INTEGER)
                throw new IOException("Invalid DER: object is not integer"); //$NON-NLS-1$

            return new BigInteger(value);
        }

        /**
         * Get value as string. Most strings are treated
         * as Latin-1.
         *
         * @return Java string
         * @throws IOException
         */
        public String getString() throws IOException {

            String encoding;

            switch (type) {

                // Not all are Latin-1 but it's the closest thing
                case DerParser.NUMERIC_STRING:
                case DerParser.PRINTABLE_STRING:
                case DerParser.VIDEOTEX_STRING:
                case DerParser.IA5_STRING:
                case DerParser.GRAPHIC_STRING:
                case DerParser.ISO646_STRING:
                case DerParser.GENERAL_STRING:
                    encoding = "ISO-8859-1"; //$NON-NLS-1$
                    break;

                case DerParser.BMP_STRING:
                    encoding = "UTF-16BE"; //$NON-NLS-1$
                    break;

                case DerParser.UTF8_STRING:
                    encoding = "UTF-8"; //$NON-NLS-1$
                    break;

                case DerParser.UNIVERSAL_STRING:
                    throw new IOException("Invalid DER: can't handle UCS-4 string"); //$NON-NLS-1$

                default:
                    throw new IOException("Invalid DER: object is not a string"); //$NON-NLS-1$
            }

            return new String(value, encoding);
        }
    }




    private static X509Certificate generateCertificateFromDER(byte[] certBytes)
            throws CertificateException {
        CertificateFactory factory = CertificateFactory.getInstance("X.509");

        return (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
    }

    private static byte[] getBytes(File file) {
        byte[] bytesArray = new byte[(int) file.length()];

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            fis.read(bytesArray); //read file into bytes[]
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytesArray;
    }
}

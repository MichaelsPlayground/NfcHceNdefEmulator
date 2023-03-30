package de.androidcrypto.nfchcendefemulator;

import android.os.Build;

import java.lang.reflect.Array;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;

public class Utils {

    public static String getTimestamp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return ZonedDateTime
                    .now(ZoneId.systemDefault())
                    .format(DateTimeFormatter.ofPattern("uuuu.MM.dd HH:mm:ss"));
        } else {
            return new SimpleDateFormat("yyyy.MM.dd HH:mm:ss").format(new Date());
        }
    }

    public static String removeAllNonAlphaNumeric(String s) {
        if (s == null) {
            return null;
        }
        return s.replaceAll("[^A-Za-z0-9]", "");
    }

    // position is 0 based starting from right to left
    public static byte setBitInByte(byte input, int pos) {
        return (byte) (input | (1 << pos));
    }

    // position is 0 based starting from right to left
    public static byte unsetBitInByte(byte input, int pos) {
        return (byte) (input & ~(1 << pos));
    }

    // https://stackoverflow.com/a/29396837/8166854
    public static boolean testBit(byte b, int n) {
        int mask = 1 << n; // equivalent of 2 to the nth power
        return (b & mask) != 0;
    }

    // https://stackoverflow.com/a/29396837/8166854
    public static boolean testBit(byte[] array, int n) {
        int index = n >>> 3; // divide by 8
        int mask = 1 << (n & 7); // n modulo 8
        return (array[index] & mask) != 0;
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuffer result = new StringBuffer();
        for (byte b : bytes)
            result.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
        return result.toString();
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String getDec(byte[] bytes) {
        long result = 0;
        long factor = 1;
        for (int i = 0; i < bytes.length; ++i) {
            long value = bytes[i] & 0xffl;
            result += value * factor;
            factor *= 256l;
        }
        return result + "";
    }

    public static String printByteBinary(byte bytes){
        byte[] data = new byte[1];
        data[0] = bytes;
        return printByteArrayBinary(data);
    }

    public static String printByteArrayBinary(byte[] bytes){
        String output = "";
        for (byte b1 : bytes){
            String s1 = String.format("%8s", Integer.toBinaryString(b1 & 0xFF)).replace(' ', '0');
            //s1 += " " + Integer.toHexString(b1);
            //s1 += " " + b1;
            output = output + " " + s1;
            //System.out.println(s1);
        }
        return output;
    }

    public static byte[] convertIntToByteArray(int value, int numberOfBytes) {
        byte b[] = new byte[numberOfBytes];
        int i, shift;
        for (i = 0, shift = (b.length - 1) * 8; i < b.length; i++, shift -= 8) {
            b[i] = (byte) (0xFF & (value >> shift));
        }
        return b;
    }

    public static String parseTextrecordPayload(byte[] ndefPayload) {
        int languageCodeLength = Array.getByte(ndefPayload, 0);
        int ndefPayloadLength = ndefPayload.length;
        byte[] languageCode = new byte[languageCodeLength];
        System.arraycopy(ndefPayload, 1, languageCode, 0, languageCodeLength);
        byte[] message = new byte[ndefPayloadLength - 1 - languageCodeLength];
        System.arraycopy(ndefPayload, 1 + languageCodeLength, message, 0, ndefPayloadLength - 1 - languageCodeLength);
        return new String(message, StandardCharsets.UTF_8);
    }

    /**
     * NFC Forum "URI Record Type Definition"<p>
     * This is a mapping of "URI Identifier Codes" to URI string prefixes,
     * per section 3.2.2 of the NFC Forum URI Record Type Definition document.
     */
    // source: https://github.com/skjolber/ndef-tools-for-android
    private static final String[] URI_PREFIX_MAP = new String[] {
            "", // 0x00
            "http://www.", // 0x01
            "https://www.", // 0x02
            "http://", // 0x03
            "https://", // 0x04
            "tel:", // 0x05
            "mailto:", // 0x06
            "ftp://anonymous:anonymous@", // 0x07
            "ftp://ftp.", // 0x08
            "ftps://", // 0x09
            "sftp://", // 0x0A
            "smb://", // 0x0B
            "nfs://", // 0x0C
            "ftp://", // 0x0D
            "dav://", // 0x0E
            "news:", // 0x0F
            "telnet://", // 0x10
            "imap:", // 0x11
            "rtsp://", // 0x12
            "urn:", // 0x13
            "pop:", // 0x14
            "sip:", // 0x15
            "sips:", // 0x16
            "tftp:", // 0x17
            "btspp://", // 0x18
            "btl2cap://", // 0x19
            "btgoep://", // 0x1A
            "tcpobex://", // 0x1B
            "irdaobex://", // 0x1C
            "file://", // 0x1D
            "urn:epc:id:", // 0x1E
            "urn:epc:tag:", // 0x1F
            "urn:epc:pat:", // 0x20
            "urn:epc:raw:", // 0x21
            "urn:epc:", // 0x22
    };

    public static String parseUrirecordPayload(byte[] ndefPayload) {
        int uriPrefix = Array.getByte(ndefPayload, 0);
        int ndefPayloadLength = ndefPayload.length;
        byte[] message = new byte[ndefPayloadLength - 1];
        System.arraycopy(ndefPayload, 1, message, 0, ndefPayloadLength - 1);
        return URI_PREFIX_MAP[uriPrefix] + new String(message, StandardCharsets.UTF_8);
    }

    private static final byte[] SW_9000 = {
            (byte)0x90,  // SW1	Status byte 1 - Command processing status
            (byte)0x00   // SW2	Status byte 2 - Command processing qualifier
    };

    /**
     * Method used to check if the last command return SW1SW2 == 9000
     *
     * @param pByte
     *            response to the last command
     * @return true if the status is 9000 false otherwise
     */
    public static boolean isSucceed(final byte[] pByte) {
        byte[] resultValue = Arrays.copyOfRange(pByte, pByte.length - 2, pByte.length);
        if (Arrays.equals(resultValue, SW_9000)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Constant-time Byte Array Comparison
     * Less overheard, safer. Originally from: http://codahale.com/a-lesson-in-timing-attacks/
     *
     * @param a yourByteArrayA
     * @param b yourByteArrayB
     * @return boolean
     *
     */
    public static boolean isEqual(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        return result == 0;
    }
}

package de.androidcrypto.nfchcendefemulator;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.util.Arrays;

/**
 * This class emulates a NFC Forum Tag Type 4 containing a NDEF message
 * The class uses the AID D2760000850101
 */
public class MyHostApduService extends HostApduService {

    // source: https://github.com/TechBooster/C85-Android-4.4-Sample/blob/master/chapter08/NdefCard/src/com/example/ndefcard/NdefHostApduService.java
    
    private final static String TAG = "MyHostApduService";

    private static final byte[] SELECT_APPLICATION = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xA4, // INS	- Instruction - Instruction code
            (byte) 0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x07, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x01, // NDEF Tag Application name D2 76 00 00 85 01 01
            (byte) 0x00  // Le field	- Maximum number of bytes expected in the data field of the response to the command
    };

    private static final byte[] SELECT_CAPABILITY_CONTAINER = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xe1, (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] SELECT_NDEF_FILE = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1, (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private final static byte[] CAPABILITY_CONTAINER_FILE = new byte[] {
            0x00, 0x0f, // CCLEN
            0x20, // Mapping Version
            0x00, 0x3b, // Maximum R-APDU data size
            0x00, 0x34, // Maximum C-APDU data size
            0x04, 0x06, // Tag & Length
            (byte)0xe1, 0x04, // NDEF File Identifier
            (byte) 0x00, (byte) 0xff, // Maximum NDEF size, do NOT extend this value
            0x00, // NDEF file read access granted
            (byte)0xff, // NDEF File write access denied
    };

    // Status Word success
    private final static byte[] SUCCESS_SW = new byte[] {
            (byte)0x90,
            (byte)0x00,
    };
    // Status Word failure
    private final static byte[] FAILURE_SW = new byte[] {
            (byte)0x6a,
            (byte)0x82,
    };

    private byte[] mNdefRecordFile;

    private boolean mAppSelected; // true when SELECT_APPLICATION detected

    private boolean mCcSelected; // true when SELECT_CAPABILITY_CONTAINER detected

    private boolean mNdefSelected; // true when SELECT_NDEF_FILE detected

    @Override
    public void onCreate() {
        super.onCreate();

        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;

        // default NDEF-message
        final String DEFAULT_MESSAGE = "This is the default message from NfcHceNdelEmulator. If you want to change the message use the tab 'Send' to enter an individual message.";
        NdefMessage ndefDefaultMessage = getNdefMessage(DEFAULT_MESSAGE);
        // the maximum length is 246 so do not extend this value
        int nlen = ndefDefaultMessage.getByteArrayLength();
        mNdefRecordFile = new byte[nlen + 2];
        mNdefRecordFile[0] = (byte)((nlen & 0xff00) / 256);
        mNdefRecordFile[1] = (byte)(nlen & 0xff);
        System.arraycopy(ndefDefaultMessage.toByteArray(), 0, mNdefRecordFile, 2, ndefDefaultMessage.getByteArrayLength());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            // intent contains a text message
            if (intent.hasExtra("ndefMessage")) {
                NdefMessage ndefMessage = getNdefMessage(intent.getStringExtra("ndefMessage"));
                if (ndefMessage != null) {
                    int nlen = ndefMessage.getByteArrayLength();
                    mNdefRecordFile = new byte[nlen + 2];
                    mNdefRecordFile[0] = (byte) ((nlen & 0xff00) / 256);
                    mNdefRecordFile[1] = (byte) (nlen & 0xff);
                    System.arraycopy(ndefMessage.toByteArray(), 0, mNdefRecordFile, 2, ndefMessage.getByteArrayLength());
                }
            }
            // intent contains an URL
            if (intent.hasExtra("ndefUrl")) {
                NdefMessage ndefMessage = getNdefUrlMessage(intent.getStringExtra("ndefUrl"));
                if (ndefMessage != null) {
                    int nlen = ndefMessage.getByteArrayLength();
                    mNdefRecordFile = new byte[nlen + 2];
                    mNdefRecordFile[0] = (byte) ((nlen & 0xff00) / 256);
                    mNdefRecordFile[1] = (byte) (nlen & 0xff);
                    System.arraycopy(ndefMessage.toByteArray(), 0, mNdefRecordFile, 2, ndefMessage.getByteArrayLength());
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private NdefMessage getNdefMessage(String ndefData) {
        if (ndefData.length() == 0) {
            return null;
        }
        NdefRecord ndefRecord;
        ndefRecord = NdefRecord.createTextRecord("en", ndefData);
        return new NdefMessage(ndefRecord);
    }

    private NdefMessage getNdefUrlMessage(String ndefData) {
        if (ndefData.length() == 0) {
            return null;
        }
        NdefRecord ndefRecord;
        ndefRecord = NdefRecord.createUri(ndefData);
        return new NdefMessage(ndefRecord);
    }

    /**
     * emulates an NFC Forum Tag Type 4
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.d((TAG), "commandApdu: " + Utils.bytesToHex(commandApdu)); 
        //if (Arrays.equals(SELECT_APP, commandApdu)) {
        // check if commandApdu qualifies for SELECT_APPLICATION
        if (Arrays.equals(SELECT_APPLICATION, commandApdu)) {
            mAppSelected = true;
            mCcSelected = false;
            mNdefSelected = false;
            Log.d((TAG), "responseApdu: " + Utils.bytesToHex(SUCCESS_SW));
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_CAPABILITY_CONTAINER
        } else if (mAppSelected && Arrays.equals(SELECT_CAPABILITY_CONTAINER, commandApdu)) {
            mCcSelected = true;
            mNdefSelected = false;
            Log.d((TAG), "responseApdu: " + Utils.bytesToHex(SUCCESS_SW));
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_NDEF_FILE
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            // NDEF
            mCcSelected = false;
            mNdefSelected = true;
            Log.d((TAG), "responseApdu: " + Utils.bytesToHex(SUCCESS_SW));
            return SUCCESS_SW;
            // check if commandApdu qualifies for // READ_BINARY
        } else if (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {
            // READ_BINARY
            // get the offset an le (length) data
            //System.out.println("** " + Utils.bytesToHex(commandApdu) + " in else if (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {");
            int offset = (0x00ff & commandApdu[2]) * 256 + (0x00ff & commandApdu[3]);
            int le = 0x00ff & commandApdu[4];

            byte[] responseApdu = new byte[le + SUCCESS_SW.length];

            if (mCcSelected && offset == 0 && le == CAPABILITY_CONTAINER_FILE.length) {
                System.arraycopy(CAPABILITY_CONTAINER_FILE, offset, responseApdu, 0, le);
                System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                Log.d((TAG), "responseApdu: " + Utils.bytesToHex(responseApdu));
                return responseApdu;
            } else if (mNdefSelected) {
                if (offset + le <= mNdefRecordFile.length) {
                    System.arraycopy(mNdefRecordFile, offset, responseApdu, 0, le);
                    System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                    Log.d((TAG), "responseApdu: " + Utils.bytesToHex(responseApdu));
                    return responseApdu;
                }
            }
        }

        // The tag should return different errors for different reasons
        // this emulation just returns the general error message
        Log.d((TAG), "responseApdu: " + Utils.bytesToHex(FAILURE_SW));
        return FAILURE_SW;
    }
    
/*
complete sequence:
commandApdu: 00a4040007d276000085010100
responseApdu: 9000
commandApdu: 00a4000c02e103
responseApdu: 9000
commandApdu: 00b000000f
responseApdu: 000f20003b00340406e10400ff00ff9000
commandApdu: 00a4000c02e104
responseApdu: 9000
commandApdu: 00b0000002
responseApdu: 002e9000
commandApdu: 00b000022e
responseApdu: d1012a55046769746875622e636f6d2f416e64726f696443727970746f3f7461623d7265706f7369746f726965739000    
 */

    /**
     * onDeactivated is called when reading ends
     * reset the status boolean values
     */
    @Override
    public void onDeactivated(int reason) {
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
    }
}


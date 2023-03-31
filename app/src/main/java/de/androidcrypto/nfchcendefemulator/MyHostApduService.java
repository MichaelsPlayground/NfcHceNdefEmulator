package de.androidcrypto.nfchcendefemulator;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import java.util.Arrays;

/**
 * This class emulates a NFC Forum Tag Type 4 containing a NDEF message
 * The class uses the AID D2760000850101
 */
public class MyHostApduService extends HostApduService {

    // source: https://github.com/TechBooster/C85-Android-4.4-Sample/blob/master/chapter08/NdefCard/src/com/example/ndefcard/NdefHostApduService.java
    // status: working with long messages

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

    private static final byte[] READ_CAPABILITY_CONTAINER = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xb0, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x0f  // Lc field	- Number of bytes present in the data field of the command
    };

    // In the scenario that we have done a CC read, the same byte[] match
    // for ReadBinary would trigger and we don't want that in succession
    private boolean READ_CAPABILITY_CONTAINER_CHECK = false;

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE = {
            (byte) 0x00, (byte) 0x0F, // CCLEN length of the CC file
            (byte) 0x20, // Mapping Version 2.0
            (byte) 0x00, (byte) 0x3B, // MLe maximum 59 bytes R-APDU data size
            (byte) 0x00, (byte) 0x34, // MLc maximum 52 bytes C-APDU data size
            (byte) 0x04, // T field of the NDEF File Control TLV
            (byte) 0x06, // L field of the NDEF File Control TLV
            (byte) 0xE1, (byte) 0x04, // File Identifier of NDEF file
            (byte) 0x00, (byte) 0x32, // Maximum NDEF file size of 50 bytes
            (byte) 0x00, // Read access without any security
            (byte) 0x00, // Write access without any security
            (byte) 0x90, (byte) 0x00 // A_OKAY
    };

    private static final byte[] SELECT_NDEF_FILE = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1, (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
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

    /**
     * emulates a NFC Forum Tag Type 4
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        //if (Arrays.equals(SELECT_APP, commandApdu)) {
        // check if commandApdu qualifies for SELECT_APPLICATION
        if (Arrays.equals(SELECT_APPLICATION, commandApdu)) {
            mAppSelected = true;
            mCcSelected = false;
            mNdefSelected = false;
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_CAPABILITY_CONTAINER
        } else if (mAppSelected && Arrays.equals(SELECT_CAPABILITY_CONTAINER, commandApdu)) {
            mCcSelected = true;
            mNdefSelected = false;
            return SUCCESS_SW;
            // check if commandApdu qualifies for SELECT_NDEF_FILE
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            // NDEF
            mCcSelected = false;
            mNdefSelected = true;
            return SUCCESS_SW;
            // check if commandApdu qualifies for // READ_BINARY
        } else if (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {
            // READ_BINARY
            // get the offset an le (length) data
            System.out.println("** " + Utils.bytesToHex(commandApdu) + " in else if (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {");
            int offset = (0x00ff & commandApdu[2]) * 256 + (0x00ff & commandApdu[3]);
            int le = 0x00ff & commandApdu[4];

            byte[] responseApdu = new byte[le + SUCCESS_SW.length];

            if (mCcSelected && offset == 0 && le == CAPABILITY_CONTAINER_FILE.length) {
                System.arraycopy(CAPABILITY_CONTAINER_FILE, offset, responseApdu, 0, le);
                System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                return responseApdu;
            } else if (mNdefSelected) {
                if (offset + le <= mNdefRecordFile.length) {
                    System.arraycopy(mNdefRecordFile, offset, responseApdu, 0, le);
                    System.arraycopy(SUCCESS_SW, 0, responseApdu, le, SUCCESS_SW.length);
                    return responseApdu;
                }
            }
        }

        // The tag should return a different error for different reasons
        // this emulation just return the general error message
        return FAILURE_SW;
    }

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


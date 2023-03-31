package de.androidcrypto.nfchcendefemulator;

import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;

import java.util.Arrays;

public class NdefHostApduService extends HostApduService {
    // アプリケーション選択のC-APDU
    private final static byte[] SELECT_APP = new byte[] {(byte)0x00, (byte)0xa4, (byte)0x04, (byte)0x00,
            (byte)0x07, (byte)0xd2, (byte)0x76, (byte)0x00, (byte)0x00, (byte)0x85, (byte)0x01, (byte)0x01,
            (byte)0x00,
    };
    // CCファイル選択のC-APDU)
    private final static byte[] SELECT_CC_FILE = new byte[] {(byte)0x00, (byte)0xa4, (byte)0x00, (byte)0x0c,
            (byte)0x02, (byte)0xe1, (byte)0x03,
    };
    // NDEFレコードファイル選択のC-APDU
    private final static byte[] SELECT_NDEF_FILE = new byte[] {(byte)0x00, (byte)0xa4, (byte)0x00, (byte)0x0c,
            (byte)0x02, (byte)0xe1, (byte)0x04,
    };

    // 成功時の Status Word (レスポンスで使用する)
    private final static byte[] SUCCESS_SW = new byte[] {
            (byte)0x90, (byte)0x00,
    };
    // 失敗時の Status Word (レスポンスで使用する)
    private final static byte[] FAILURE_SW = new byte[] {
            (byte)0x6a, (byte)0x82,
    };

    // CCファイルのデータ
    private final static byte[] CC_FILE = new byte[] {
            0x00, 0x0f, // CCLEN
            0x20, // Mapping Version
            0x00, 0x3b, // Maximum R-APDU data size
            0x00, 0x34, // Maximum C-APDU data size
            0x04, 0x06, // Tag & Length
            (byte)0xe1, 0x04, // NDEF File Identifier
            //0x00, 0x32, // Maximum NDEF size, original
            (byte) 0x00, (byte) 0xff, // Maximum NDEF size
            0x00, // NDEF file read access granted
            (byte)0xff, // NDEF File write access denied
    };

    // NDEFレコードファイル用変数
    private byte[] mNdefRecordFile;

    // アプリが選択されているかどうかのフラグ
    private boolean mAppSelected;

    // CCファイルが選択されているかどうかのフラグ
    private boolean mCcSelected;

    // NDEFレコードファイルが選択されているかどうかのフラグ
    private boolean mNdefSelected;

    @Override
    public void onCreate() {
        super.onCreate();

        // 状態のクリア
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;

        // default NDEF-message
        final String DEFAULT_MESSAGE = "This is the default message from NfcHceNdelEmulator. If you want to change the message use the tab 'Send' to enter an individual message.";
        // the maximum length is 246 so do not extend this value
        NdefMessage ndefDefaultMessage = getNdefMessage(DEFAULT_MESSAGE);
        int nlen = ndefDefaultMessage.getByteArrayLength();
        mNdefRecordFile = new byte[nlen + 2];
        mNdefRecordFile[0] = (byte)((nlen & 0xff00) / 256);
        mNdefRecordFile[1] = (byte)(nlen & 0xff);
        System.arraycopy(ndefDefaultMessage.toByteArray(), 0, mNdefRecordFile, 2, ndefDefaultMessage.getByteArrayLength());
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
     * NFC Forum Tag Type 4として振る舞う処理を行う。
     * C-APDUを受け取り、対応するR-APDUを返す。
     */
    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        if (Arrays.equals(SELECT_APP, commandApdu)) {
            // アプリ選択
            mAppSelected = true;
            mCcSelected = false;
            mNdefSelected = false;
            return SUCCESS_SW; // 成功
        } else if (mAppSelected && Arrays.equals(SELECT_CC_FILE, commandApdu)) {
            // CCファイル選択
            mCcSelected = true;
            mNdefSelected = false;
            return SUCCESS_SW; // 成功
        } else if (mAppSelected && Arrays.equals(SELECT_NDEF_FILE, commandApdu)) {
            // NDEFファイル選択
            mCcSelected = false;
            mNdefSelected = true;
            return SUCCESS_SW; // 成功
        } else if (commandApdu[0] == (byte)0x00 && commandApdu[1] == (byte)0xb0) {
            // READ_BINARY (ファイル読み出し)

            // オフセットと長さを取り出す
            int offset = (0x00ff & commandApdu[2]) * 256 + (0x00ff & commandApdu[3]);
            int le = 0x00ff & commandApdu[4];

            // R-APDU用のバッファを生成する
            byte[] responseApdu = new byte[le + SUCCESS_SW.length];

            if (mCcSelected && offset == 0 && le == CC_FILE.length) {
                // CC選択時はオフセットが0、長さがファイル長(15)と一致していなければならない
                System.arraycopy(CC_FILE, offset, responseApdu, 0, le);
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

        // エラーを返す
        // 本来、ICカードアプリではエラー種別にあわせてエラーの値を変えなければ
        // ならないが、ここでは省略して一種類のみを返している。
        return FAILURE_SW;
    }

    /**
     * カードアプリが非選択状態になった時に呼ばれる。
     * 本アプリでは状態をリセットして初期状態に戻します。
     */
    @Override
    public void onDeactivated(int reason) {
        mAppSelected = false;
        mCcSelected = false;
        mNdefSelected = false;
    }
}

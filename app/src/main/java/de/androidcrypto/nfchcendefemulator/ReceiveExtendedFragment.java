package de.androidcrypto.nfchcendefemulator;

import static android.content.Context.VIBRATOR_SERVICE;

import android.content.Context;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReceiveExtendedFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReceiveExtendedFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReceiveExtendedFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReceiveExtendedFragment newInstance(String param1, String param2) {
        ReceiveExtendedFragment fragment = new ReceiveExtendedFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    TextView readResult;
    private NfcAdapter mNfcAdapter;
    String dumpExportString = "";
    String tagIdString = "";
    String tagTypeString = "";
    private static final int REQUEST_PERMISSION_WRITE_EXTERNAL_STORAGE = 100;
    Context contextSave;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        readResult = getView().findViewById(R.id.tvReceiveReadResult);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_receive_extended, container, false);
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an Ndef Technology Type

        System.out.println("NFC tag discovered");
        getActivity().runOnUiThread(() -> {
            readResult.setText("");
        });

        IsoDep isoDep = null;
        writeToUiAppend(readResult, "Tag found");
        String[] techList = tag.getTechList();
        for (int i = 0; i < techList.length; i++) {
            writeToUiAppend(readResult, "TechList: " + techList[i]);
        }
        String tagId = Utils.bytesToHex(tag.getId());
        writeToUiAppend(readResult, "TagId: " + tagId);

        try {
            isoDep = IsoDep.get(tag);

            if (isoDep != null) {
                getActivity().runOnUiThread(() -> {
                    Toast.makeText(this.getContext(),
                            "NFC tag is IsoDep compatible",
                            Toast.LENGTH_SHORT).show();
                });

                isoDep.connect();
                dumpExportString = "";
                getActivity().runOnUiThread(() -> {
                    //readResult.setText("");
                });


                writeToUiAppend(readResult, "IsoDep reading");
                String nfcaContent = "IsoDep reading" + "\n";

                // now we run the select command with AID
                String nfcHceNdefAid = "D2760000850101";
                byte[] aid = Utils.hexStringToByteArray(nfcHceNdefAid);

                byte[] command = selectApdu(aid);
                byte[] responseSelect = isoDep.transceive(command);
                writeToUiAppend(readResult, "selectApdu with AID: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "selectApdu response: " + Utils.bytesToHex(responseSelect));

                if (responseSelect == null) {
                    writeToUiAppend(readResult, "selectApdu with AID fails (null)");
                } else {
                    writeToUiAppend(readResult, "responseSelect length: " + responseSelect.length + " data: " + Utils.bytesToHex(responseSelect));
                    System.out.println("responseSelect: " + Utils.bytesToHex(responseSelect));
                }

                if (!Utils.isSucceed(responseSelect)) {
                    writeToUiAppend(readResult, "responseSelect is not 90 00 - aborted");
                    System.out.println("responseSelect is not 90 00 - aborted ");
                    return;
                }

                // sending cc select = get the capability container
                String selectCapabilityContainer = "00a4000c02e103";
                command = Utils.hexStringToByteArray(selectCapabilityContainer);
                byte[] responseSelectCc = isoDep.transceive(command);
                writeToUiAppend(readResult, "select CC: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "select CC response: " + Utils.bytesToHex(responseSelectCc));
                writeToUiAppend(readResult, "responseSelect length: " + responseSelectCc.length + " data: " + Utils.bytesToHex(responseSelectCc));
                System.out.println("responseSelectCc: " + Utils.bytesToHex(responseSelectCc));

                if (!Utils.isSucceed(responseSelectCc)) {
                    writeToUiAppend(readResult, "responseSelectCc is not 90 00 - aborted");
                    System.out.println("responseSelectCc is not 90 00 - aborted ");
                    return;
                }

                // Sending ReadBinary from CC...
                String sendBinareFromCc = "00b000000f";
                command = Utils.hexStringToByteArray(sendBinareFromCc);
                byte[] responseSendBinaryFromCc = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendBinaryFromCc: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendBinaryFromCc response: " + Utils.bytesToHex(responseSendBinaryFromCc));
                writeToUiAppend(readResult, "sendBinaryFromCc response length: " + responseSendBinaryFromCc.length + " data: " + Utils.bytesToHex(responseSendBinaryFromCc));
                System.out.println("sendBinaryFromCc response: " + Utils.bytesToHex(responseSendBinaryFromCc));

                if (!Utils.isSucceed(responseSendBinaryFromCc)) {
                    writeToUiAppend(readResult, "responseSendBinaryFromCc is not 90 00 - aborted");
                    System.out.println("responseSendBinaryFromCc is not 90 00 - aborted ");
                    return;
                }

                // Capability Container header:
                byte[] capabilityContainerHeader = Arrays.copyOfRange(responseSendBinaryFromCc, 0, responseSendBinaryFromCc.length - 2);
                writeToUiAppend(readResult, "capabilityContainerHeader length: " + capabilityContainerHeader.length + " data: " + Utils.bytesToHex(capabilityContainerHeader));
                System.out.println("capabilityContainerHeader: " + Utils.bytesToHex(capabilityContainerHeader));
                System.out.println("capabilityContainerHeader: " + new String(capabilityContainerHeader));

                // Sending NDEF Select...
                String sendNdefSelect = "00a4000c02e104";
                command = Utils.hexStringToByteArray(sendNdefSelect);
                byte[] responseSendNdefSelect = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendNdefSelect: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendNdefSelect response: " + Utils.bytesToHex(responseSendNdefSelect));
                writeToUiAppend(readResult, "sendNdefSelect response length: " + responseSendNdefSelect.length + " data: " + Utils.bytesToHex(responseSendNdefSelect));
                System.out.println("sendNdefSelect response: " + Utils.bytesToHex(responseSendNdefSelect));

                if (!Utils.isSucceed(responseSendNdefSelect)) {
                    writeToUiAppend(readResult, "responseSendNdefSelect is not 90 00 - aborted");
                    System.out.println("responseSendNdefSelect is not 90 00 - aborted ");
                    return;
                }

                // Sending ReadBinary NLEN...
                String sendReadBinaryNlen = "00b0000002";
                command = Utils.hexStringToByteArray(sendReadBinaryNlen);
                byte[] responseSendBinaryNlen = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendBinaryNlen: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendBinaryNlen response: " + Utils.bytesToHex(responseSendBinaryNlen));
                writeToUiAppend(readResult, "sendBinaryNlen response length: " + responseSendBinaryNlen.length + " data: " + Utils.bytesToHex(responseSendBinaryNlen));
                System.out.println("sendBinaryNlen response: " + Utils.bytesToHex(responseSendBinaryNlen));

                if (!Utils.isSucceed(responseSendBinaryNlen)) {
                    writeToUiAppend(readResult, "responseSendBinaryNlen is not 90 00 - aborted");
                    System.out.println("responseSendBinaryNlen is not 90 00 - aborted ");
                    return;
                }

                // Sending ReadBinary, get NDEF data...
                byte[] ndefLen = Arrays.copyOfRange(responseSendBinaryNlen, 0, 2);
                byte[] cmdLen = Utils.hexStringToByteArray(sendReadBinaryNlen);
                int ndefLenInt = new BigInteger(ndefLen).intValue();
                writeToUiAppend(readResult,"ndefLen: " + Utils.bytesToHex(ndefLen) + " len (dec): " + ndefLenInt);
                int ndefLenIntRequest = ndefLenInt + 2;
                //byte[] cmdLenNew = BigInteger.valueOf(ndefLenIntRequest).toByteArray();
                byte[] cmdLenNew = Utils.convertIntToByteArray(ndefLenIntRequest, 2);
                writeToUiAppend(readResult,"ndefLen new (dec): " + ndefLenIntRequest + " data: " + Utils.bytesToHex(cmdLenNew) );

                String sendReadBinaryNdefData = "00b000" + Utils.bytesToHex(cmdLenNew);
                //String sendReadBinaryNdefData = "00b000000f";
                //String sendReadBinaryNdefData = "00b0000092";
                command = Utils.hexStringToByteArray(sendReadBinaryNdefData);
                byte[] responseSendBinaryNdefData = isoDep.transceive(command);
                writeToUiAppend(readResult, "sendBinaryNdefData: " + Utils.bytesToHex(command));
                writeToUiAppend(readResult, "sendBinaryNdefData response: " + Utils.bytesToHex(responseSendBinaryNdefData));
                writeToUiAppend(readResult, "sendBinaryNdefData response length: " + responseSendBinaryNdefData.length + " data: " + Utils.bytesToHex(responseSendBinaryNdefData));
                writeToUiAppend(readResult, "sendBinaryNdefData response: " + new String(responseSendBinaryNdefData));
                System.out.println("sendBinaryNdefData response: " + Utils.bytesToHex(responseSendBinaryNdefData));
                System.out.println("sendBinaryNdefData response: " + new String(responseSendBinaryNdefData));

                if (!Utils.isSucceed(responseSendBinaryNdefData)) {
                    writeToUiAppend(readResult, "responseSendBinaryNdefData is not 90 00 - aborted");
                    System.out.println("responseSendBinaryNdefData is not 90 00 - aborted ");
                    return;
                }

                byte[] ndefMessage = Arrays.copyOfRange(responseSendBinaryNdefData, 0, responseSendBinaryNdefData.length - 2);
                writeToUiAppend(readResult, "ndefMessage length: " + ndefMessage.length + " data: " + Utils.bytesToHex(ndefMessage));
                writeToUiAppend(readResult, "ndefMessage: " + new String(ndefMessage));
                System.out.println("ndefMessage: " + new String(ndefMessage));

                // strip off the first 2 bytes
                byte[] ndefMessageStrip = Arrays.copyOfRange(ndefMessage, 9, ndefMessage.length);

                //String ndefMessageParsed = Utils.parseTextrecordPayload(ndefMessageStrip);
                String ndefMessageParsed = new String(ndefMessageStrip);
                writeToUiAppend(readResult, "ndefMessage parsed: " + ndefMessageParsed);
                System.out.println("ndefMessage parsed: " + ndefMessageParsed);

                // try to get a NdefMessage from the byte array
                byte[] ndefMessageByteArray = Arrays.copyOfRange(ndefMessage, 2, ndefMessage.length);
                try {
                    NdefMessage ndefMessageFromTag = new NdefMessage(ndefMessageByteArray);
                    NdefRecord[] ndefRecords = ndefMessageFromTag.getRecords();
                    NdefRecord ndefRecord;
                    int ndefRecordsCount = ndefRecords.length;
                    if (ndefRecordsCount > 0) {
                        for (int i = 0; i < ndefRecordsCount; i++) {
                            short ndefTnf = ndefRecords[i].getTnf();
                            byte[] ndefType = ndefRecords[i].getType();
                            byte[] ndefPayload = ndefRecords[i].getPayload();
                            // here we are trying to parse the content
                            // Well known type - Text
                            if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                                    Arrays.equals(ndefType, NdefRecord.RTD_TEXT)) {
                                writeToUiAppend(readResult, "rec: " + i +
                                        " Well known Text payload\n" + new String(ndefPayload) + " \n");
                                writeToUiAppend(readResult, Utils.parseTextrecordPayload(ndefPayload));
                            }
                            // Well known type - Uri
                            if (ndefTnf == NdefRecord.TNF_WELL_KNOWN &&
                                    Arrays.equals(ndefType, NdefRecord.RTD_URI)) {
                                writeToUiAppend(readResult, "rec: " + i +
                                        " Well known Uri payload\n" + new String(ndefPayload) + " \n");
                                writeToUiAppend(readResult, Utils.parseUrirecordPayload(ndefPayload) + " \n");
                            }
                        }
                        dumpExportString = readResult.getText().toString();
                    }
                    //dumpExportString = readResult.getText().toString();

                } catch (FormatException e) {
                    e.printStackTrace();
                }
                doVibrate();
            } else {
                writeToUiAppend(readResult, "IsoDep == null");
            }
        } catch (IOException e) {
            writeToUiAppend(readResult, "ERROR IOException: " + e);
            e.printStackTrace();
        }
    }

    // https://stackoverflow.com/a/51338700/8166854
    private byte[] selectApdu(byte[] aid) {
        byte[] commandApdu = new byte[6 + aid.length];
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xA4;  // INS
        commandApdu[2] = (byte) 0x04;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) (aid.length & 0x0FF);       // Lc
        System.arraycopy(aid, 0, commandApdu, 5, aid.length);
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }

    private void doVibrate() {
        if (getActivity() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ((Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(150, 10));
            } else {
                Vibrator v = (Vibrator) getActivity().getSystemService(VIBRATOR_SERVICE);
                v.vibrate(200);
            }
        }
    }

    private void writeToUiAppend(TextView textView, String message) {
        getActivity().runOnUiThread(() -> {
            String newString = textView.getText().toString() + "\n" + message;
            textView.setText(newString);
            dumpExportString = newString;
        });
    }

    private void writeToUiAppendReverse(TextView textView, String message) {
        getActivity().runOnUiThread(() -> {
            String newString = message + "\n" + textView.getText().toString();
            textView.setText(newString);
        });
    }

    private void writeToUiToast(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(this.getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for all types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag afer reading
            mNfcAdapter.enableReaderMode(this.getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NFC_B |
                            NfcAdapter.FLAG_READER_NFC_F |
                            NfcAdapter.FLAG_READER_NFC_V |
                            NfcAdapter.FLAG_READER_NFC_BARCODE |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this.getActivity());
    }

}
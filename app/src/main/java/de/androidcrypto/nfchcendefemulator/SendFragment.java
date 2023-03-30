package de.androidcrypto.nfchcendefemulator;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.TextView;

import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SendFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class SendFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    RadioButton rbTimestamp, rbMessage;
    TextView tvTimestamp;
    com.google.android.material.textfield.TextInputLayout dataToSendLayout;
    com.google.android.material.textfield.TextInputEditText dataToSend;

    public SendFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SendFragment newInstance(String param1, String param2) {
        SendFragment fragment = new SendFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    // AID is setup in apduservice.xml
    // original AID: F0394148148100

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_send, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        //com.google.android.material.textfield.TextInputLayout dataToSendLayout;
        //com.google.android.material.textfield.TextInputEditText dataToSend;
        tvTimestamp = getView().findViewById(R.id.tvTimestamp);
        rbTimestamp = getView().findViewById(R.id.rbTimestamp);
        rbMessage = getView().findViewById(R.id.rbMessage);

        dataToSendLayout = getView().findViewById(R.id.etMainDataToSendsLayout);
        dataToSend = getView().findViewById(R.id.etMainDataToSend);
        dataToSendLayout.setEndIconOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String dataToSendString = dataToSend.getText().toString();


            }
        });


        Button setNdef = (Button) getView().findViewById(R.id.set_ndef_button);
        setNdef.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                // Technically, if this is past our byte limit,
                // it will cause issues.
                //
                // TODO: add validation
                //
                TextView getNdefString = (TextView) getView().findViewById(R.id.ndef_text);
                String test = getNdefString.getText().toString() + " on " +
                        Utils.getTimestamp();

                Intent intent = new Intent(view.getContext(), MyHostApduService.class);
                intent.putExtra("ndefMessage", test);
                System.out.println("*** start ***");
                getActivity().startService(intent);
            }
        });

        Button setNdef100 = (Button) getView().findViewById(R.id.set_ndef100_button);
        setNdef100.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                // Technically, if this is past our byte limit,
                // it will cause issues.
                //
                // TODO: add validation
                //
                String characters66 = "";
                for (int i = 0; i < 66; i++) {
                    characters66 = characters66 + "A";
                }
                String test = characters66 + " on " +
                        Utils.getTimestamp();
                Intent intent = new Intent(view.getContext(), MyHostApduService.class);
                intent.putExtra("ndefMessage", test);
                System.out.println("*** start ***");
                getActivity().startService(intent);
            }
        });

        Button setNdef500 = (Button) getView().findViewById(R.id.set_ndef500_button);
        setNdef500.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //
                // Technically, if this is past our byte limit,
                // it will cause issues.
                //
                // TODO: add validation
                //
                String characters66 = "";
                for (int i = 0; i < 466; i++) {
                    characters66 = characters66 + "A";
                }
                String test = characters66 + " on " +
                        Utils.getTimestamp();
                Intent intent = new Intent(view.getContext(), MyHostApduService.class);
                intent.putExtra("ndefMessage", test);
                System.out.println("*** start ***");
                getActivity().startService(intent);
            }
        });

        // start with timestamp
        ndefWithTimestamp(view.getContext());
    }

    private void ndefWithTimestamp(Context context) {
        PackageManager pm = context.getPackageManager();
        Timer t = new Timer();

        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                Date dt = Calendar.getInstance().getTime();
                //Log.d(TAG, "Set time as " + dt.toString());
                tvTimestamp.setText(dt.toString());
                /*
                if (t != null) {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            tvTimestamp.setText(dt.toString());
                        }
                    });
                }*/

                if (pm.hasSystemFeature(PackageManager.FEATURE_NFC_HOST_CARD_EMULATION)) {
                    //Intent intent = new Intent(context, CardService.class);
                    Intent intent = new Intent(context, MyHostApduService.class);
                    intent.putExtra("ndefMessage", dt.toString());
                    //intent.putExtra("ndefMessage", test);
                    // Log.d(TAG, intent.toString());
                    context.startService(intent);
                }
            }

        };
        //t.scheduleAtFixedRate(task, 0, 1000); // every second
        //t.scheduleAtFixedRate(task, 0, 60000); // every minute
        t.scheduleAtFixedRate(task, 0, 2000); // every minute
    }


    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

}
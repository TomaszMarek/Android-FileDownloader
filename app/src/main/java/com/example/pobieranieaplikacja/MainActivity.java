package com.example.pobieranieaplikacja;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MainActivity extends AppCompatActivity {
    private static final int WRITE_EXTERNAL_STORAGE_REQUEST_CODE = 1;

    private EditText editTextUrl;
    private TextView textView5;
    private TextView textView7;
    private TextView textView8;
    private TextView textView9;
    private int mRozmiar;
    private String mTyp;
    private ProgressBar progressBar;
    private Button buttonDownload;
    private Button buttonInformation;
    private static final String BUTTON_ENABLED_KEY = "buttonEnabled";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextUrl = findViewById(R.id.editTextUrl);
        textView5 = findViewById(R.id.textView5);
        textView7 = findViewById(R.id.textView7);
        textView8 = findViewById(R.id.textView8);
        textView9 = findViewById(R.id.textView9);
        progressBar = findViewById(R.id.progressBar);
        buttonDownload = findViewById(R.id.buttonPobierzPlik);
        buttonInformation = findViewById(R.id.buttonPobierzInformacje);


        if (savedInstanceState != null) {
            editTextUrl.setText(savedInstanceState.getString("editTextUrl"));
            textView5.setText(savedInstanceState.getString("textView5"));
            textView7.setText(savedInstanceState.getString("textView7"));
            textView8.setText(savedInstanceState.getString("textView8"));
            textView9.setText(savedInstanceState.getString("textView9"));
            mRozmiar = savedInstanceState.getInt("mRozmiar", 0);
            mTyp = savedInstanceState.getString("mTyp");
            //przywracanie stanu przycisku
            buttonDownload.setEnabled(savedInstanceState.getBoolean(BUTTON_ENABLED_KEY, true));

        } else {
            Intent intent = getIntent();
            if (intent != null) {
                mRozmiar = intent.getIntExtra("rozmiar", 0);
                mTyp = intent.getStringExtra("typ");
                //pobranie stanu wartosci przycisku!
                //wyłączony - false, włączony - true
                boolean buttonDisabled = intent.getBooleanExtra("buttonDisabled", false);
                buttonDownload.setEnabled(!buttonDisabled);

            }
        }

        textView5.setText(String.valueOf(mRozmiar));
        textView7.setText(mTyp);

        buttonInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = editTextUrl.getText().toString();
                ZadanieAsynchroniczne zadanie = new ZadanieAsynchroniczne(url);
                zadanie.execute(10);
            }
        });
        buttonDownload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // jesli ma przyznane uprawnienia
                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                    buttonDownload.setEnabled(false);
                    String url = editTextUrl.getText().toString();
                    MojaIntentService.uruchomUsluge(MainActivity.this, url, true);
                } else {
                    //żądanie uprawnien
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, WRITE_EXTERNAL_STORAGE_REQUEST_CODE);
                }
            }
        });
        //tworzenie nasluchiwacza, informacje o pobieraniu i zakonczeniu
        LocalBroadcastManager.getInstance(this).registerReceiver(mOdbiorca, new IntentFilter(MojaIntentService.POWIADOMIENIE));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("editTextUrl", editTextUrl.getText().toString());
        outState.putString("textView5", textView5.getText().toString());
        outState.putString("textView7", textView7.getText().toString());
        outState.putString("textView8", textView8.getText().toString());
        outState.putString("textView9", textView9.getText().toString());
        outState.putInt("mRozmiar", mRozmiar);
        outState.putString("mTyp", mTyp);
        outState.putBoolean(BUTTON_ENABLED_KEY, buttonDownload.isEnabled());
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        editTextUrl.setText(savedInstanceState.getString("editTextUrl"));
        textView5.setText(savedInstanceState.getString("textView5"));
        textView7.setText(savedInstanceState.getString("textView7"));
        textView8.setText(savedInstanceState.getString("textView8"));
        textView9.setText(savedInstanceState.getString("textView9"));
        mRozmiar = savedInstanceState.getInt("mRozmiar", 0);
        mTyp = savedInstanceState.getString("mTyp");
        buttonDownload.setEnabled(savedInstanceState.getBoolean(BUTTON_ENABLED_KEY, true));
    }
    //odbiorca rozgloszen
    //oczekuje na odpowiedz od wyslijbroadcast
    private BroadcastReceiver mOdbiorca = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                //pobieranie postep info z intenjci
                PostepInfo postepInfo = intent.getParcelableExtra(MojaIntentService.INFO); //
                if (postepInfo != null) {
                    // Aktualizacja UI na podstawie postepInfo
                    textView9.setText(postepInfo.mPobranychBajtow + " / " + postepInfo.mRozmiar);
                    textView8.setText(postepInfo.mStatus);
                    //setProgress oczekuje warrtosci procentowej
                    progressBar.setProgress((int) ((postepInfo.mPobranychBajtow / (float) postepInfo.mRozmiar) * 100));

                    // Sprawdzenie czy pobieranie zakończone
                    if (postepInfo.mPobranychBajtow == postepInfo.mRozmiar) {
                        textView8.setText("Pobieranie zakończone"); // Aktualizacja TextView
                        findViewById(R.id.buttonPobierzPlik).setEnabled(true); // Odblokuj przycisk
                    }
                }
            }
        }
    };
    //Przy pierwszym uruchomieniu
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                // Otrzymaliśmy uprawnienia, możemy uruchomić usługę pobierania pliku
                buttonDownload.setEnabled(false);
                String url = editTextUrl.getText().toString();
                MojaIntentService.uruchomUsluge(MainActivity.this, url, true);
            } else {
                // Nie otrzymaliśmy uprawnień
                Toast.makeText(this, "Brak uprawnień do zapisu na zewnętrznym nośniku", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // AsyncTask do pobierania informacji o pliku

//Pozwala na wykonywanie zadania w tle i aktualizowanie interfejsu uzytkownika
    private class ZadanieAsynchroniczne extends AsyncTask<Integer, Integer, Integer> {
        //dane przekazywane, dane do postepu i dane koncowe.
        private int mRozmiar;
        private String mTyp;
        private String adresUrl;

        public ZadanieAsynchroniczne(String adresUrl) {
            this.adresUrl = adresUrl;
        }

        @Override
        protected Integer doInBackground(Integer... params) {
            HttpsURLConnection polaczenie = null;
            try {
                URL url = new URL(adresUrl); //UTWORZENIE
                polaczenie = (HttpsURLConnection) url.openConnection(); //OTWORZENIE
                polaczenie.setRequestMethod("GET"); //WYBRANIE METODY
                mRozmiar = polaczenie.getContentLength();
                mTyp = polaczenie.getContentType();
                //bez ponizszego nie aktualizuje
                publishProgress(mRozmiar); // wysyłanie informacji o postępie
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (polaczenie != null) polaczenie.disconnect();
            }
            return 42;
        }
        @Override
        protected void onProgressUpdate(Integer... values) {
            // Aktualizacja informacji o postępie POBIERZ INFORMACJE
            textView5.setText(String.valueOf(mRozmiar));
            textView7.setText(mTyp);
        }
        }

    }



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

import java.net.HttpURLConnection;
import java.net.URL;

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
            buttonDownload.setEnabled(savedInstanceState.getBoolean(BUTTON_ENABLED_KEY, true));
        } else {
            Intent intent = getIntent();
            if (intent != null) {
                mRozmiar = intent.getIntExtra("rozmiar", 0);
                mTyp = intent.getStringExtra("typ");
                boolean buttonDisabled = intent.getBooleanExtra("buttonDisabled", false);
                buttonDownload.setEnabled(!buttonDisabled);
            }
        }

        textView5.setText(String.valueOf(mRozmiar));
        textView7.setText(mTyp);

        buttonInformation.setOnClickListener(v -> {
            String url = editTextUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(MainActivity.this, "Wprowadź poprawny adres URL", Toast.LENGTH_SHORT).show();
                return;
            }
            ZadanieAsynchroniczne zadanie = new ZadanieAsynchroniczne(url);
            zadanie.execute();
        });

        buttonDownload.setOnClickListener(v -> checkPermissionAndDownload());

        LocalBroadcastManager.getInstance(this).registerReceiver(
                mOdbiorca,
                new IntentFilter(MojaIntentService.POWIADOMIENIE)
        );
    }

    private void checkPermissionAndDownload() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startDownloadService();
        } else {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                startDownloadService();
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        WRITE_EXTERNAL_STORAGE_REQUEST_CODE
                );
            }
        }
    }

    private void startDownloadService() {
        String url = editTextUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Wprowadź poprawny adres URL", Toast.LENGTH_SHORT).show();
            return;
        }
        buttonDownload.setEnabled(false);
        MojaIntentService.uruchomUsluge(MainActivity.this, url, true);
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

    private final BroadcastReceiver mOdbiorca = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent != null) {
                PostepInfo postepInfo = intent.getParcelableExtra(MojaIntentService.INFO);
                if (postepInfo != null) {
                    textView9.setText(postepInfo.mPobranychBajtow + " / " + postepInfo.mRozmiar);
                    textView8.setText(postepInfo.mStatus);

                    if (postepInfo.mRozmiar > 0) {
                        int progress = (int) (((float) postepInfo.mPobranychBajtow / postepInfo.mRozmiar) * 100);
                        progressBar.setProgress(progress);
                    }

                    if (postepInfo.mPobranychBajtow == postepInfo.mRozmiar && postepInfo.mRozmiar > 0) {
                        textView8.setText("Pobieranie zakończone");
                        buttonDownload.setEnabled(true);
                    } else if ("Błąd pobierania".equals(postepInfo.mStatus)) {
                        buttonDownload.setEnabled(true);
                    }
                }
            }
        }
    };

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startDownloadService();
            } else {
                Toast.makeText(this, "Brak uprawnień do zapisu danych", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mOdbiorca);
    }

    private class ZadanieAsynchroniczne extends AsyncTask<Void, Void, Boolean> {
        private final String adresUrl;
        private int pobranyRozmiar;
        private String pobranyTyp;

        public ZadanieAsynchroniczne(String adresUrl) {
            this.adresUrl = adresUrl;
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            HttpURLConnection polaczenie = null;
            try {
                URL url = new URL(adresUrl);
                polaczenie = (HttpURLConnection) url.openConnection();
                polaczenie.setRequestMethod("GET");
                polaczenie.setConnectTimeout(10000);
                polaczenie.setReadTimeout(10000);

                pobranyRozmiar = polaczenie.getContentLength();
                pobranyTyp = polaczenie.getContentType();
                return true;
            } catch (Exception e) {
                return false;
            } finally {
                if (polaczenie != null) {
                    polaczenie.disconnect();
                }
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mRozmiar = pobranyRozmiar;
                mTyp = pobranyTyp != null ? pobranyTyp : "Nieznany";
                textView5.setText(String.valueOf(mRozmiar));
                textView7.setText(mTyp);
            } else {
                Toast.makeText(MainActivity.this, "Nie udało się pobrać informacji o pliku", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

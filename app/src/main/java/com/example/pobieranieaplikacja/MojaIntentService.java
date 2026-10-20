package com.example.pobieranieaplikacja;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MojaIntentService extends IntentService {
    private static final String AKCJA_ZADANIE1 = "com.example.intent_service.action.zadanie1";
    private static final String PARAMETR1 = "com.example.intent_service.extra.parametr1";
    private static final int ID_POWIADOMIENIA = 1;
    private static final String ID_KANALU = "kanal_powiadomien";

    private NotificationManager mManagerPowiadomien;
    private int mRozmiar;
    private int mPobranychBajtow = 0;
    private String mAdres;
    private String mTyp;
    private boolean isButtonDisabled;

    public static final String POWIADOMIENIE = "com.example.intent_service.odbiornik";
    public static final String INFO = "info";

    public static void uruchomUsluge(Context context, String adres, boolean buttonDisabled) {
        Intent zamiar = new Intent(context, MojaIntentService.class);
        zamiar.setAction(AKCJA_ZADANIE1);
        zamiar.putExtra(PARAMETR1, adres);
        zamiar.putExtra("buttonDisabled", buttonDisabled);
        context.startService(zamiar);
    }

    public MojaIntentService() {
        super("MojaIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mManagerPowiadomien = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        przygotujKanalPowiadomien();
        startForeground(ID_POWIADOMIENIA, utworzPowiadomienie());

        if (intent != null) {
            final String action = intent.getAction();
            if (AKCJA_ZADANIE1.equals(action)) {
                mAdres = intent.getStringExtra(PARAMETR1);
                isButtonDisabled = intent.getBooleanExtra("buttonDisabled", false);
                pobierzPlik(mAdres);
            }
        }
    }

    private void pobierzPlik(String adres) {
        HttpURLConnection polaczenie = null;
        InputStream strumienZSieci = null;
        FileOutputStream strumienDoPliku = null;

        try {
            URL url = new URL(adres);
            String sciezkaUrl = url.getPath();
            String nazwaPliku = sciezkaUrl.substring(sciezkaUrl.lastIndexOf('/') + 1);
            if (nazwaPliku.isEmpty()) {
                nazwaPliku = "pobrany_plik_" + System.currentTimeMillis();
            }

            File katalogPobierania = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
            if (katalogPobierania == null) {
                katalogPobierania = getFilesDir();
            }
            File plikWyjsciowy = new File(katalogPobierania, nazwaPliku);

            if (plikWyjsciowy.exists()) {
                plikWyjsciowy.delete();
            }

            polaczenie = (HttpURLConnection) url.openConnection();
            polaczenie.setRequestMethod("GET");
            polaczenie.setConnectTimeout(15000);
            polaczenie.setReadTimeout(15000);

            mRozmiar = polaczenie.getContentLength();
            mTyp = polaczenie.getContentType();
            strumienZSieci = polaczenie.getInputStream();
            strumienDoPliku = new FileOutputStream(plikWyjsciowy);

            byte[] bufor = new byte[8192];
            int pobrano;

            while ((pobrano = strumienZSieci.read(bufor)) != -1) {
                strumienDoPliku.write(bufor, 0, pobrano);
                mPobranychBajtow += pobrano;

                PostepInfo postepInfo = new PostepInfo();
                postepInfo.mPobranychBajtow = mPobranychBajtow;
                postepInfo.mRozmiar = mRozmiar;
                postepInfo.mStatus = "Pobieranie trwa";

                wyslijBroadcast(postepInfo);
                mManagerPowiadomien.notify(ID_POWIADOMIENIA, utworzPowiadomienie());
            }

            PostepInfo postepInfo = new PostepInfo();
            postepInfo.mPobranychBajtow = mPobranychBajtow;
            postepInfo.mRozmiar = mRozmiar;
            postepInfo.mStatus = "Pobieranie zakończone";
            wyslijBroadcast(postepInfo);

        } catch (Exception e) {
            PostepInfo postepInfo = new PostepInfo();
            postepInfo.mPobranychBajtow = 0;
            postepInfo.mRozmiar = 0;
            postepInfo.mStatus = "Błąd pobierania";
            wyslijBroadcast(postepInfo);
        } finally {
            if (strumienZSieci != null) {
                try {
                    strumienZSieci.close();
                } catch (IOException ignored) {}
            }
            if (strumienDoPliku != null) {
                try {
                    strumienDoPliku.close();
                } catch (IOException ignored) {}
            }
            if (polaczenie != null) {
                polaczenie.disconnect();
            }
        }
    }

    private void przygotujKanalPowiadomien() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            NotificationChannel kanal = new NotificationChannel(
                    ID_KANALU,
                    name,
                    NotificationManager.IMPORTANCE_LOW
            );
            mManagerPowiadomien.createNotificationChannel(kanal);
        }
    }

    private Notification utworzPowiadomienie() {
        Intent intencjaPowiadomienia = new Intent(this, MainActivity.class);
        intencjaPowiadomienia.putExtra("rozmiar", mRozmiar);
        intencjaPowiadomienia.putExtra("typ", mTyp);
        intencjaPowiadomienia.putExtra("buttonDisabled", isButtonDisabled);

        TaskStackBuilder budowniczyStosu = TaskStackBuilder.create(this);
        budowniczyStosu.addParentStack(MainActivity.class);
        budowniczyStosu.addNextIntent(intencjaPowiadomienia);

        int flagiPendingIntent = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flagiPendingIntent |= PendingIntent.FLAG_IMMUTABLE;
        }

        PendingIntent intencjaOczekujaca = budowniczyStosu.getPendingIntent(0, flagiPendingIntent);
        Notification.Builder budowniczyPowiadomien = new Notification.Builder(this);

        budowniczyPowiadomien.setContentTitle(getString(R.string.powiadomienie_tytul))
                .setProgress(mRozmiar > 0 ? mRozmiar : 0, mPobranychBajtow, mRozmiar <= 0)
                .setContentIntent(intencjaOczekujaca)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setWhen(System.currentTimeMillis());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            budowniczyPowiadomien.setChannelId(ID_KANALU);
        } else {
            budowniczyPowiadomien.setPriority(Notification.PRIORITY_HIGH);
        }

        return budowniczyPowiadomien.build();
    }

    private void wyslijBroadcast(PostepInfo postepInfo) {
        Intent zamiar = new Intent(POWIADOMIENIE);
        zamiar.putExtra(INFO, postepInfo);
        LocalBroadcastManager.getInstance(this).sendBroadcast(zamiar);
    }
}

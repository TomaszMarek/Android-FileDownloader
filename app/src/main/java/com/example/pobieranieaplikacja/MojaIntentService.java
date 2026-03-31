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
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class MojaIntentService extends IntentService {
    private static final String AKCJA_ZADANIE1 = "com.example.intent_service.action.zadanie1";
    private static final String PARAMETR1 = "com.example.intent_service.extra.parametr1";
    private static final int ID_POWIADOMIENIA = 1;
    private NotificationManager mManagerPowiadomien;
    private static final String ID_KANALU = "kanal_powiadomien";
    private int mRozmiar;
    private int mPobranychBajtow = 0;
    private String mAdres;
    private String mTyp;

    private boolean isButtonDisabled;



    public static final String POWIADOMIENIE = "com.example.intent_service.odbiornik";
    public static final String INFO = "info";

    public static void uruchomUsluge(Context context, String adres,boolean buttonDisabled) {
        Intent zamiar = new Intent(context, MojaIntentService.class);
        //urucjomienie uslugi MojaIntentservice z odpowiedniki parametrami.
        zamiar.setAction(AKCJA_ZADANIE1);
        zamiar.putExtra(PARAMETR1, adres); //przechowywanie adresu pobierania
        zamiar.putExtra("buttonDisabled", buttonDisabled);
        context.startService(zamiar); //uruchomienie uslugi z argumentem zamiar
    }

    public MojaIntentService() {
        super("MojaIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mManagerPowiadomien = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        //tworzenie kanalu powiadomien
        przygotujKanalPowiadomien();
        //miała wyższy priorytet i nie będzie łatwo zakończona przez system.
        startForeground(ID_POWIADOMIENIA, utworzPowiadomienie());

        if (intent != null) {
            final String action = intent.getAction();
            if (AKCJA_ZADANIE1.equals(action)) {
                //jesli akcja jest rowna naszemu zadaniu
                //pobieramy adres, stan przycisku i pobieramy plik
                mAdres = intent.getStringExtra(PARAMETR1);
                isButtonDisabled = intent.getBooleanExtra("buttonDisabled", false);
                pobierzPlik(mAdres);
            }
        }

    }

    private void pobierzPlik(String adres) {
        HttpsURLConnection polaczenie = null;
        InputStream strumienZSieci = null; //strumien wejscia
        FileOutputStream strumienDoPliku = null; //strumien wyjscia
        try {
            URL url = new URL(adres);
            File plikRoboczy = new File(url.getFile()); // na podstawie nazwy url
            File plikWyjsciowy = new File(Environment.getExternalStorageDirectory() + File.separator + plikRoboczy.getName());
            if (plikWyjsciowy.exists()) {
                plikWyjsciowy.delete();
            }
            polaczenie = (HttpsURLConnection) url.openConnection(); //nawiazanie polaczenia
            polaczenie.setRequestMethod("GET");
            mRozmiar = polaczenie.getContentLength(); //rozmiar pliku
            mTyp = polaczenie.getContentType(); // typ pliku
            strumienZSieci = polaczenie.getInputStream(); //Strumień wejściowy do odczytu danych z połączenia.
            strumienDoPliku = new FileOutputStream(plikWyjsciowy.getPath()); //Strumień wyjściowy do zapisu danych do pliku.

            byte bufor[] = new byte[8192]; // Zwiększ rozmiar bufora do 8192 bajtów
            int pobrano = strumienZSieci.read(bufor, 0, 8192);
            while (pobrano != -1) {
                //gdy dane zostana odczytane, zostanie zwrocone -1, petla sie skonczy
                strumienDoPliku.write(bufor, 0, pobrano);
                mPobranychBajtow += pobrano;
                pobrano = strumienZSieci.read(bufor, 0, 8192);


                // Aktualizacja statusu i powiadomień za każdym razem
                PostepInfo postepInfo = new PostepInfo();
                postepInfo.mPobranychBajtow = mPobranychBajtow;
                postepInfo.mRozmiar = mRozmiar;
                postepInfo.mStatus = "Pobieranie trwa";

                wyslijBroadcast(postepInfo); //przekazywane do mainactivity

                // Aktualizacja powiadomienia o postępie
                mManagerPowiadomien.notify(ID_POWIADOMIENIA, utworzPowiadomienie());
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (strumienZSieci != null) {
                try {
                    strumienZSieci.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (strumienDoPliku != null) {
                try {
                    strumienDoPliku.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (polaczenie != null) {
                polaczenie.disconnect();
            }
        }

    }

    private void przygotujKanalPowiadomien() {
        mManagerPowiadomien = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { //wersja Android 26
            CharSequence name = getString(R.string.app_name); //mazwa powidomienia
            NotificationChannel kanal = new NotificationChannel(ID_KANALU, name, NotificationManager.IMPORTANCE_LOW);
            mManagerPowiadomien.createNotificationChannel(kanal);
        }
    }

    private Notification utworzPowiadomienie() {
        Intent intencjaPowiadomienia = new Intent(this, MainActivity.class);

        // Dodanie mRozmiar i mTyp do intencji powiadomienia
        intencjaPowiadomienia.putExtra("rozmiar", mRozmiar);
        intencjaPowiadomienia.putExtra("typ", mTyp);
        intencjaPowiadomienia.putExtra("buttonDisabled", isButtonDisabled);

        TaskStackBuilder budowniczyStosu = TaskStackBuilder.create(this); // tworzenie stosu
        budowniczyStosu.addParentStack(MainActivity.class); // mozliwosc powrotu do ekranow na stosie
        budowniczyStosu.addNextIntent(intencjaPowiadomienia);
        PendingIntent intencjaOczekujaca = budowniczyStosu.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder budowniczyPowiadomien = new Notification.Builder(this);

        budowniczyPowiadomien.setContentTitle(getString(R.string.powiadomienie_tytul)) // konfigurowanie powiadomienia
                .setProgress(mRozmiar, mPobranychBajtow, false)
                .setContentIntent(intencjaOczekujaca)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setWhen(System.currentTimeMillis())
                .setPriority(Notification.PRIORITY_HIGH);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            budowniczyPowiadomien.setChannelId(ID_KANALU);
        }
        return budowniczyPowiadomien.build();
    }

    private void wyslijBroadcast(PostepInfo postepInfo) {
        Intent zamiar = new Intent(POWIADOMIENIE);
        zamiar.putExtra(INFO, postepInfo);
        //bezpieczny sposob wysylania informacji z akcją POWIADOMIENIE
        LocalBroadcastManager.getInstance(this).sendBroadcast(zamiar);
    }
}

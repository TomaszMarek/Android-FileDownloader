package com.example.pobieranieaplikacja;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class PostepInfo implements Parcelable {

    //przekazanie informacj miedzy MainActivity a MojaIntentService za pomoca
    //LocalBroadcastManager
    public int mPobranychBajtow;
    public int mRozmiar;
    public String mStatus;
    public PostepInfo() {
    }

    protected PostepInfo(Parcel in) {
        mPobranychBajtow = in.readInt();
        mRozmiar = in.readInt();
        mStatus = in.readString();
    }

    public static final Creator<PostepInfo> CREATOR = new Creator<PostepInfo>() {
        @Override
        public PostepInfo createFromParcel(Parcel in) {
            return new PostepInfo(in);
        }

        @Override
        public PostepInfo[] newArray(int size) {
            return new PostepInfo[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        //zapisanie stanu obiektu do paczki
        //otrzymanej jako parametr.
        dest.writeInt(mPobranychBajtow);
        dest.writeInt(mRozmiar);
        dest.writeString(mStatus);
    }
}

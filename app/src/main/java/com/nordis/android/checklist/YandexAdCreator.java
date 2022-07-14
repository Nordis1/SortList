package com.nordis.android.checklist;

import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.yandex.mobile.ads.common.AdRequest;
import com.yandex.mobile.ads.common.AdRequestError;
import com.yandex.mobile.ads.common.ImpressionData;
import com.yandex.mobile.ads.rewarded.Reward;
import com.yandex.mobile.ads.rewarded.RewardedAd;
import com.yandex.mobile.ads.rewarded.RewardedAdEventListener;

public class YandexAdCreator extends MainActivity {
    private static final String TAG = "YandexAdCreator";
    final String myAdId = "R-M-1759251-1";
    final String testAdId = "R-M-338228-6";
    final Context context;
    RewardedAd mYandexRewardedAd;
    AdRequest adRequest;

    public YandexAdCreator(Context context) {
        this.context = context;
    }

    public void yandexRewardAdCreate(){
        mYandexRewardedAd = new RewardedAd(context);
        mYandexRewardedAd.setAdUnitId(myAdId);

        adRequest = new AdRequest.Builder().build();
        mYandexRewardedAd.setRewardedAdEventListener(new RewardedAdEventListener() {

            @Override
            public void onAdLoaded() {
                Log.d(TAG, "onAdLoaded: ");
                mYRewardedAd = mYandexRewardedAd;
            }

            @Override
            public void onAdFailedToLoad(@NonNull AdRequestError adRequestError) {
                Log.d(TAG, "onAdFailedToLoad: ");
                mYRewardedAd = null;
            }

            @Override
            public void onAdShown() {
                Log.d(TAG, "onAdShown: ");
                mYRewardedAd = null;

            }

            @Override
            public void onAdDismissed() {
                Log.d(TAG, "onAdDismissed: ");
            }

            @Override
            public void onRewarded(@NonNull Reward reward) {
                Log.d(TAG, "onRewarded: ");
                Log.d(TAG, "The user earned the reward: " + reward.getAmount());

                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("DiamondIncrementing", reward.getAmount());
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            @Override
            public void onAdClicked() {
                Log.d(TAG, "onAdClicked: ");
            }

            @Override
            public void onLeftApplication() {
                Log.d(TAG, "onLeftApplication: ");
            }

            @Override
            public void onReturnedToApplication() {
                Log.d(TAG, "onReturnedToApplication: ");
            }

            @Override
            public void onImpression(@Nullable ImpressionData impressionData) {
                Log.d(TAG, "onImpression: ");
            }
        });

        mYandexRewardedAd.loadAd(adRequest);


    }

    public void yandexRewardAdStart(){
        mYandexRewardedAd.show();
        yandexRewardAdCreate();
    }
}

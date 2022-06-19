package com.nordis.android.checklist;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

public class AdMobCreator extends MainActivity {
    private String myAdAppNative = "ca-app-pub-6564886494367745/6806905091";
    private String myAdNumber = "ca-app-pub-6564886494367745/7174186976";
    private String testAdRewarded = "ca-app-pub-3940256099942544/5224354917";
    private Context context;

    public AdMobCreator(Context context) {
        this.context = context;
    }

    private static final String TAG = "AdMobCreator";

    public void startRewardedAdCreate() {

        AdRequest adRequest = new AdRequest.Builder().build();


        MobileAds.initialize(context, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });

        RewardedAd.load(context, testAdRewarded,
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, "onAdFailedToLoad: error");
                        mRewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        Log.d(TAG, "onAdLoaded: created");
                        mRewardedAd = rewardedAd;
                    }
                });

    }

    public void startRewardedAdExecute(Activity activityContext){
        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "Ad was shown.");
                mRewardedAd = null;
                startRewardedAdCreate();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when ad fails to show.
                Log.d(TAG, "Ad failed to show.");
                mRewardedAd = null;
                startRewardedAdCreate();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "Ad was dismissed.");
            }
        });
        mRewardedAd.show(activityContext, new OnUserEarnedRewardListener() {
            @Override
            public void onUserEarnedReward(@NonNull RewardItem rewardItem) {
                // Handle the reward.
                Log.d(TAG, "The user earned the reward.");
                int rewardAmount = rewardItem.getAmount();

                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("DiamondIncrementing", rewardAmount);
                msg.setData(bundle);
                handler.sendMessage(msg);


            }
        });
    }
}

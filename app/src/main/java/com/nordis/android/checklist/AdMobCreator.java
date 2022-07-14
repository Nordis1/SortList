package com.nordis.android.checklist;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Random;

public class AdMobCreator extends MainActivity {
    private static final String TAG = "AdMobCreator";
    private String myAdRewarded = "ca-app-pub-6564886494367745/7174186976";
    private String testAdRewarded = "ca-app-pub-3940256099942544/5224354917";
    private String testAdBanner = "ca-app-pub-3940256099942544/6300978111";
    private Context context;
    private Random random = new Random();

    public AdMobCreator(Context context) {
        this.context = context;
    }

    public void startRewardedAdCreate() {

        AdRequest adRequest = new AdRequest.Builder().build();


        MobileAds.initialize(context, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {

            }
        });

        RewardedAd.load(context, myAdRewarded,
                adRequest, new RewardedAdLoadCallback() {

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, "RewardedOnAdFailedToLoad: error");
                        mRewardedAd = null;

                        /** Admob is not loaded, start creating YandexAds*/
                        Log.d(TAG, "onAdFailedToLoad: Admob is not loaded, start creating YandexAds");
                        handler.sendEmptyMessage(hSetYandexAdCreator);

                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd rewardedAd) {
                        Log.d(TAG, "RewardedOnAdLoaded: created");
                        mRewardedAd = rewardedAd;
                    }
                });

    }

    public void startRewardedAdExecute(Activity activityContext){
        mRewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdShowedFullScreenContent() {
                // Called when ad is shown.
                Log.d(TAG, "rewarded Ad was shown.");
                mRewardedAd = null;
                startRewardedAdCreate();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(AdError adError) {
                // Called when ad fails to show.
                Log.d(TAG, "rewarded Ad failed to show.");
                mRewardedAd = null;
                startRewardedAdCreate();
            }

            @Override
            public void onAdDismissedFullScreenContent() {
                // Called when ad is dismissed.
                // Set the ad reference to null so you don't show the ad a second time.
                Log.d(TAG, "rewarded Ad was dismissed.");
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

    public void startAdBannerCreate(){
        MobileAds.initialize(context, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(@NonNull InitializationStatus initializationStatus) {
            }
        });

        AdRequest adRequest = new AdRequest.Builder().build();
        binding.adBannerView.loadAd(adRequest);
        binding.adBannerView.setAdListener(new AdListener() {
            @Override
            public void onAdClicked() {
                Log.d(TAG, "Banner onAdClicked: ");
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putInt("DiamondIncrementing", 3);
                msg.setData(bundle);
                handler.sendMessage(msg);
            }

            @Override
            public void onAdClosed() {
                Log.d(TAG, "Banner onAdClosed: ");
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                handler.sendEmptyMessage(hSetAdBannerFailedLoad);
                Log.d(TAG, "Banner onAdFailedToLoad: ");
            }

            @Override
            public void onAdLoaded() {
                if (diamondValue > 500){
                    if (random.nextInt(3) == 1) {
                        Log.d(TAG, "Banner onAdLoaded: Пошла прибавка +1 кристалл за Баннер > 500");
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putInt("DiamondIncrementing", 1);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                }else if (diamondValue > 120) {
                    if (random.nextInt(2) == 1) {
                        Log.d(TAG, "Banner onAdLoaded: Пошла прибавка +1 кристалл за Баннер > 120");
                        Message msg = handler.obtainMessage();
                        Bundle bundle = new Bundle();
                        bundle.putInt("DiamondIncrementing", 1);
                        msg.setData(bundle);
                        handler.sendMessage(msg);
                    }
                }else if (diamondValue < 30){
                    Log.d(TAG, "Banner onAdLoaded: Пошла прибавка +2 кристалл за Баннер < 30");
                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("DiamondIncrementing", 2);
                    msg.setData(bundle);
                    handler.sendMessage(msg);

                }else {
                    Log.d(TAG, "Banner onAdLoaded: Пошла прибавка +1 кристалл за Баннер < 120");
                    Message msg = handler.obtainMessage();
                    Bundle bundle = new Bundle();
                    bundle.putInt("DiamondIncrementing", 1);
                    msg.setData(bundle);
                    handler.sendMessage(msg);
                }
            }

            @Override
            public void onAdOpened() {
                Log.d(TAG, "Banner onAdOpened: ");
            }
        });
        //mAdView.loadAd(adRequest);
    }
}

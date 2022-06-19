package com.nordis.android.checklist;

public class MyRunnable extends MainActivity implements Runnable {

    @Override
    public void run() {
        binding.downloadBar.setProgress(mProgresscounter);
    }
}

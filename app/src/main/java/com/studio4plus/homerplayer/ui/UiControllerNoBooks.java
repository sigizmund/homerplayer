package com.studio4plus.homerplayer.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.google.common.base.Preconditions;
import com.studio4plus.homerplayer.service.DemoSamplesInstallerService;
import com.studio4plus.homerplayer.events.DemoSamplesInstallationStartedEvent;

import javax.inject.Inject;
import javax.inject.Named;

import de.greenrobot.event.EventBus;

public class UiControllerNoBooks {

    public static class Factory {
        private final @NonNull Activity activity;
        private final @NonNull Uri samplesDownloadUrl;
        private final @NonNull EventBus eventBus;

        @Inject
        public Factory(@NonNull Activity activity,
                       @NonNull @Named("SAMPLES_DOWNLOAD_URL") Uri samplesDownloadUrl,
                       @NonNull EventBus eventBus) {
            this.activity = activity;
            this.samplesDownloadUrl = samplesDownloadUrl;
            this.eventBus = eventBus;
        }

        public UiControllerNoBooks create(@NonNull NoBooksUi ui) {
            return new UiControllerNoBooks(activity, ui, samplesDownloadUrl, eventBus);
        }
    }

    private final @NonNull Activity activity;
    private final @NonNull NoBooksUi ui;
    private final @NonNull Uri samplesDownloadUrl;
    private final @NonNull EventBus eventBus;

    private @Nullable DownloadProgressReceiver progressReceiver;

    private UiControllerNoBooks(@NonNull Activity activity,
                                @NonNull NoBooksUi ui,
                                @NonNull Uri samplesDownloadUrl,
                                @NonNull EventBus eventBus) {
        this.activity = activity;
        this.ui = ui;
        this.samplesDownloadUrl = samplesDownloadUrl;
        this.eventBus = eventBus;

        ui.initWithController(this);

        boolean isInstalling = DemoSamplesInstallerService.isInstalling();
        if (DemoSamplesInstallerService.isDownloading() || isInstalling)
            showInstallProgress(isInstalling);
    }

    public void startSamplesInstallation() {
        eventBus.post(new DemoSamplesInstallationStartedEvent());
        showInstallProgress(false);
        activity.startService(DemoSamplesInstallerService.createDownloadIntent(
                activity, samplesDownloadUrl));
    }

    public void abortSamplesInstallation() {
        Preconditions.checkState(DemoSamplesInstallerService.isDownloading()
                || DemoSamplesInstallerService.isInstalling());
        // Can't cancel installation.
        if (DemoSamplesInstallerService.isDownloading()) {
            activity.startService(DemoSamplesInstallerService.createCancelIntent(
                    activity));
            stopProgressReceiver();
        }
    }

    void shutdown() {
        if (progressReceiver != null)
            stopProgressReceiver();
        ui.shutdown();
    }

    private void showInstallProgress(boolean isAlreadyInstalling) {
        Preconditions.checkState(progressReceiver == null);
        NoBooksUi.InstallProgressObserver uiProgressObserver =
                ui.showInstallProgress(isAlreadyInstalling);
        progressReceiver = new DownloadProgressReceiver(uiProgressObserver);
        IntentFilter filter = new IntentFilter();
        filter.addAction(DemoSamplesInstallerService.BROADCAST_DOWNLOAD_PROGRESS_ACTION);
        filter.addAction(DemoSamplesInstallerService.BROADCAST_INSTALL_STARTED_ACTION);
        filter.addAction(DemoSamplesInstallerService.BROADCAST_FAILED_ACTION);
        filter.addAction(DemoSamplesInstallerService.BROADCAST_INSTALL_FINISHED_ACTION);
        LocalBroadcastManager.getInstance(activity).registerReceiver(progressReceiver, filter);
    }

    private void stopProgressReceiver() {
        Preconditions.checkState(progressReceiver != null);
        LocalBroadcastManager.getInstance(activity).unregisterReceiver(progressReceiver);
        progressReceiver.stop();
        progressReceiver = null;
    }

    private class DownloadProgressReceiver extends BroadcastReceiver {

        private @Nullable NoBooksUi.InstallProgressObserver observer;

        DownloadProgressReceiver(@NonNull NoBooksUi.InstallProgressObserver observer) {
            this.observer = observer;
        }

        public void stop() {
            this.observer = null;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            Preconditions.checkNotNull(intent.getAction());
            // Workaround for intents being sent after the receiver is unregistered:
            // https://code.google.com/p/android/issues/detail?id=191546
            if (observer == null)
                return;

            if (DemoSamplesInstallerService.BROADCAST_DOWNLOAD_PROGRESS_ACTION.equals(
                    intent.getAction())) {
                int transferredBytes = intent.getIntExtra(
                        DemoSamplesInstallerService.PROGRESS_BYTES_EXTRA, 0);
                int totalBytes = intent.getIntExtra(
                        DemoSamplesInstallerService.TOTAL_BYTES_EXTRA, -1);
                observer.onDownloadProgress(transferredBytes, totalBytes);
            } else if (DemoSamplesInstallerService.BROADCAST_INSTALL_STARTED_ACTION.equals(
                    intent.getAction())) {
                observer.onInstallStarted();
            } else if (DemoSamplesInstallerService.BROADCAST_INSTALL_FINISHED_ACTION.equals(
                    intent.getAction())) {
                stopProgressReceiver();
            } else if (DemoSamplesInstallerService.BROADCAST_FAILED_ACTION.equals(
                    intent.getAction())) {
                observer.onFailure();
                stopProgressReceiver();
            } else {
                Preconditions.checkState(false,
                        "Unexpected intent action: " + intent.getAction());
            }
        }
    }
}

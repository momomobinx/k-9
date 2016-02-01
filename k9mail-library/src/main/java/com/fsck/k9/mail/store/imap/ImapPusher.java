package com.fsck.k9.mail.store.imap;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.fsck.k9.mail.K9MailLib;
import com.fsck.k9.mail.PushReceiver;
import com.fsck.k9.mail.Pusher;

import static com.fsck.k9.mail.K9MailLib.LOG_TAG;


class ImapPusher implements Pusher {
    private final ImapStore mStore;
    final PushReceiver mReceiver;
    private long lastRefresh = -1;

    final Map<String, ImapFolderPusher> folderPushers = new HashMap<String, ImapFolderPusher>();

    public ImapPusher(ImapStore store, PushReceiver receiver) {
        mStore = store;
        mReceiver = receiver;
    }

    @Override
    public void start(List<String> folderNames) {
        stop();
        synchronized (folderPushers) {
            setLastRefresh(System.currentTimeMillis());
            for (String folderName : folderNames) {
                ImapFolderPusher pusher = folderPushers.get(folderName);
                if (pusher == null) {
                    pusher = new ImapFolderPusher(mStore, folderName, mReceiver);
                    folderPushers.put(folderName, pusher);
                    pusher.start();
                }
            }
        }
    }

    @Override
    public void refresh() {
        synchronized (folderPushers) {
            for (ImapFolderPusher folderPusher : folderPushers.values()) {
                try {
                    folderPusher.refresh();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Got exception while refreshing for " + folderPusher.getName(), e);
                }
            }
        }
    }

    @Override
    public void stop() {
        if (K9MailLib.isDebug())
            Log.i(LOG_TAG, "Requested stop of IMAP pusher");

        synchronized (folderPushers) {
            for (ImapFolderPusher folderPusher : folderPushers.values()) {
                try {
                    if (K9MailLib.isDebug())
                        Log.i(LOG_TAG, "Requesting stop of IMAP folderPusher " + folderPusher.getName());
                    folderPusher.stop();
                } catch (Exception e) {
                    Log.e(LOG_TAG, "Got exception while stopping " + folderPusher.getName(), e);
                }
            }
            folderPushers.clear();
        }
    }

    @Override
    public int getRefreshInterval() {
        return (mStore.getStoreConfig().getIdleRefreshMinutes() * 60 * 1000);
    }

    @Override
    public long getLastRefresh() {
        return lastRefresh;
    }

    @Override
    public void setLastRefresh(long lastRefresh) {
        this.lastRefresh = lastRefresh;
    }
}

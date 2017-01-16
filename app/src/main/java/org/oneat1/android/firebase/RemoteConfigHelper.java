package org.oneat1.android.firebase;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import org.oneat1.android.R;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by parthpadgaonkar on 1/16/17.
 */

public class RemoteConfigHelper {
    private final static Logger LOG = LoggerFactory.getLogger(RemoteConfigHelper.class);

    private static final long FIREBASE_FETCH_THRESHOLD = TimeUnit.MINUTES.toSeconds(30L); //playing with fire, because server throttles at ~5+ requests per hour.
    private static final String KEY_YOUTUBE_ID = "LiveVideoUrl";

    private static RemoteConfigHelper sInstance;
    private final FirebaseRemoteConfig remoteConfigInstance;

    public interface CompletionListener {
        void onComplete(boolean wasSuccessful, @Nullable String youtubeID);
    }

    public synchronized static RemoteConfigHelper get() {
        if (sInstance == null) {
            sInstance = new RemoteConfigHelper();
        }
        return sInstance;
    }

    private RemoteConfigHelper() {
        remoteConfigInstance = FirebaseRemoteConfig.getInstance();
        remoteConfigInstance.setDefaults(R.xml.firebase_defaults);
    }

    public void fetch(boolean bustCache, final CompletionListener listener) {
        remoteConfigInstance
              .fetch(bustCache ? 0 : FIREBASE_FETCH_THRESHOLD)
              .addOnCompleteListener(new OnCompleteListener<Void>() {
                  @Override
                  public void onComplete(@NonNull Task<Void> task) {
                      remoteConfigInstance.activateFetched();
                      final boolean success;
                      String id = null;
                      if (success = task.isSuccessful()) {
                          LOG.debug("successful fetch!");
                          id = remoteConfigInstance.getString(KEY_YOUTUBE_ID);
                          if (remoteConfigInstance.getInfo()
                                    .getLastFetchStatus() == FirebaseRemoteConfig.LAST_FETCH_STATUS_THROTTLED) {
                              LOG.warn("fetch was from cache");
                          }
                      } else {
                          LOG.error("Error while fetching remote config: ", task.getException());
                      }
                      final String finalId = id;
                      listener.onComplete(success, finalId);
                  }
              });
    }

    public String getYoutubeID() {
        return remoteConfigInstance.getString(KEY_YOUTUBE_ID);
    }
}

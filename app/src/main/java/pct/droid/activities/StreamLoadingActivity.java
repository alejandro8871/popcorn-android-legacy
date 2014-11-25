package pct.droid.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.FileObserver;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.okhttp.Callback;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import butterknife.InjectView;
import pct.droid.R;
import pct.droid.providers.media.MediaProvider;
import pct.droid.providers.subs.SubsProvider;
import pct.droid.streamer.Status;
import pct.droid.utils.FileUtils;
import pct.droid.utils.LogUtils;

public class StreamLoadingActivity extends BaseActivity {

    public final static String STREAM_URL = "stream_url";
    public final static String DATA = "video_data";
    public final static String QUALITY = "quality";
    public final static String SUBTITLES = "subs";

    private FileObserver mFileObserver;
    private Boolean mIntentStarted = false, mHasSubs = false;

    private enum SubsStatus { SUCCESS, FAILURE, DOWNLOADING };
    private SubsStatus mSubsStatus = SubsStatus.DOWNLOADING;

    @InjectView(R.id.progressIndicator)
    ProgressBar progressIndicator;
    @InjectView(R.id.progressText)
    TextView progressText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState, R.layout.activity_streamloading);

        while (!getApp().isServiceBound()) {
            getApp().startService();
        }

        String action = getIntent().getAction();
        if (action == null || !action.equals(Intent.ACTION_VIEW)) {
            if (!getIntent().hasExtra(STREAM_URL) && !getIntent().hasExtra(DATA)) {
                finish();
            }

            String streamUrl = getIntent().getStringExtra(STREAM_URL);
            MediaProvider.Video data = getIntent().getParcelableExtra(DATA);

            if(getIntent().hasExtra(SUBTITLES)) {
                mHasSubs = true;
                String subtitleLanguage = getIntent().getStringExtra(SUBTITLES);
                if(!subtitleLanguage.equals("no-subs")) {
                    SubsProvider.download(this, data, subtitleLanguage, new Callback() {
                        @Override
                        public void onFailure(Request request, IOException e) {
                            mSubsStatus = SubsStatus.FAILURE;
                        }

                        @Override
                        public void onResponse(Response response) throws IOException {
                            mSubsStatus = SubsStatus.SUCCESS;
                        }
                    });
                }
            }

            getApp().startStreamer(streamUrl);
        } else {
            Uri data = getIntent().getData();
            String url = data.toString();
            try {
                url = URLDecoder.decode(url, "utf-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            getApp().startStreamer(url);
        }

        String directory = getApp().getStreamDir();
        mFileObserver = new FileObserver(directory) {
            @Override
            public void onEvent(int event, String path) {
                if(path == null) return;
                if(path.contains("streamer.json")) {
                    switch (event) {
                        case CREATE:
                            LogUtils.d("Streamer file created");
                            break;
                        case MODIFY:
                            LogUtils.d("Streamer file modified");
                            startPlayer();
                            break;
                    }
                } else if(path.contains("status.json")) {
                    switch (event) {
                        case CREATE:
                        case MODIFY:
                            LogUtils.d("Status file changed");
                            updateStatus();
                            break;
                    }
                }
            }
        };

        mFileObserver.startWatching();
    }

    private void startPlayer() {
        if(mHasSubs && mSubsStatus == SubsStatus.DOWNLOADING) {
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressText.setText("Waiting for subtitles"); // TODO: translation (by sv244)
                }
            });
            return;
        }

        if(!mIntentStarted && progressIndicator.getProgress() == progressIndicator.getMax()) {
            mIntentStarted = true;
            Intent i = new Intent(StreamLoadingActivity.this, VideoPlayerActivity.class);
            if(getIntent().hasExtra(DATA)) {
                i.putExtra(VideoPlayerActivity.DATA, getIntent().getParcelableExtra(DATA));
            }
            if(getIntent().hasExtra(QUALITY)) {
                i.putExtra(VideoPlayerActivity.QUALITY, getIntent().getStringExtra(QUALITY));
            }
            if(mSubsStatus == SubsStatus.SUCCESS) {
                i.putExtra(VideoPlayerActivity.SUBTITLES, getIntent().getStringExtra(SUBTITLES));
            }
            i.putExtra(VideoPlayerActivity.LOCATION, "http://localhost:9999");
            startActivity(i);
            finish();
        }
    }

    private void updateStatus() {
        try {
            final Status status = Status.parseJSON(FileUtils.getContentsAsString(getApp().getStreamDir() + "/status.json"));
            if(status == null) return;
            LogUtils.d(status.toString());
            int calculateProgress = (int)Math.floor(status.progress * 20
            );
            if(calculateProgress > 100) calculateProgress = 100;
            final int progress = calculateProgress;
            if(progressIndicator.getProgress() < 100) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressIndicator.setIndeterminate(false);
                        progressIndicator.setProgress(progress);
                        progressText.setText(progress + "%");
                    }
                });
            } else {
                startPlayer();
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        progressText.setText("Streaming"); // TODO: translation (by sv244)
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mFileObserver.startWatching();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mFileObserver.stopWatching();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        getApp().stopStreamer();
    }
}
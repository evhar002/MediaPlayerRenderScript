package gungoren.com.mediaplayerrenderscript;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.net.Uri;
import android.renderscript.RenderScript;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.PlaybackParameters;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.extractor.ExtractorsFactory;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelectionArray;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.upstream.BandwidthMeter;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.util.Util;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Uri mPath = null;
    private Button button;
    private TextureView mainTextureView, destTextureView;
    private SurfaceView mainSurfaceView;
    private Surface mainSurface, destSurface;

    private SimpleExoPlayer exoPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        button = findViewById(R.id.render);
        button.setVisibility(View.VISIBLE);
        if (mPath == null) {
            button.setText("Video Seç");
        } else {
            button.setText("Render");
        }
        button.setOnClickListener(new Button.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mPath == null) {
                    Intent intent = new Intent();
                    intent.setType("video/*");
                    intent.setAction(Intent.ACTION_GET_CONTENT);
                    try {
                        startActivityForResult(Intent.createChooser(intent, "Select Video"), 201);
                    } catch (android.content.ActivityNotFoundException ex) {
                        Toast.makeText(getBaseContext(), "No video on gallery", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    setUpRenderScript();
                }
            }
        });

        mainTextureView = findViewById(R.id.main_view);
        mainTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int i, int i1) {
                mainSurfaceView = new SurfaceView(getBaseContext());
                mainSurface = mainSurfaceView.getHolder().getSurface();
            }

            @Override
            public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture, int i, int i1) {

            }

            @Override
            public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {

            }
        });

        destTextureView = findViewById(R.id.dest_view);
    }

    private void setUpRenderScript() {

        mainSurfaceView.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(mainSurfaceView.getDrawingCache());
        mainSurfaceView.setDrawingCacheEnabled(false);

        RenderScript renderScript = RenderScript.create(getBaseContext());
        final RenderscriptProcessor processor = new RenderscriptProcessor(renderScript, destTextureView.getWidth(), destTextureView.getHeight());
        Bitmap bitmap_scriptIntrinsicBlur = processor.createBitmap_ScriptIntrinsicBlur(renderScript, bitmap, destTextureView.getWidth(), destTextureView.getHeight(), 5f);

        Canvas canvas = destTextureView.lockCanvas();
        canvas.drawBitmap(bitmap_scriptIntrinsicBlur, 0, 0, new Paint());
        bitmap_scriptIntrinsicBlur.recycle();
        destTextureView.unlockCanvasAndPost(canvas);

    }

    private void initExoPlayer() {
        BandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
        TrackSelection.Factory videoTrackSelectionFactory =
                new AdaptiveTrackSelection.Factory(bandwidthMeter);
        TrackSelector trackSelector = new DefaultTrackSelector(videoTrackSelectionFactory);

        // Create the player
        exoPlayer = ExoPlayerFactory.newSimpleInstance(this, trackSelector);

        // Measures bandwidth during playback. Can be null if not required.
        DefaultBandwidthMeter defaultBandwidthMeter = new DefaultBandwidthMeter();
        // Produces DataSource instances through which media data is loaded.
        DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "Video Editor"), defaultBandwidthMeter);

        // Produces Extractor instances for parsing the media data.
        ExtractorsFactory extractorsFactory = new DefaultExtractorsFactory();

        // This is the MediaSource representing the media to be played.
        MediaSource mediaSource = new ExtractorMediaSource(mPath, dataSourceFactory, extractorsFactory, null, null);
        exoPlayer.prepare(mediaSource);
        exoPlayer.setVideoSurface(mainSurface);
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.addListener(new Player.EventListener() {
            @Override
            public void onLoadingChanged(boolean isLoading) {
                Log.i(TAG, "onloadingChanged [isLoading = " + isLoading + "]");
            }

            @Override
            public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
                Log.i(TAG, "onPlayerStateChanged [playWhenReady = " + playWhenReady + ", playbackState = " + playbackState + "]");
            }

            @Override
            public void onRepeatModeChanged(int i) {
                Log.i(TAG, "onRepeatModeChanged [i = " + i + "]");
            }

            @Override
            public void onTimelineChanged(Timeline timeline, Object manifest) {
                Log.i(TAG, "onTimelineChanged [timeline = " + timeline + ", manifest = " + manifest + "]");
            }

            @Override
            public void onTracksChanged(TrackGroupArray trackGroupArray, TrackSelectionArray trackSelectionArray) {
                Log.i(TAG, "onTracksChanged [trackGroupArray = " + trackGroupArray + ", trackSelectionArray = " + trackSelectionArray + "]");
            }

            @Override
            public void onPlayerError(ExoPlaybackException error) {
                Log.i(TAG, "onPlayerError [ExoPlaybackException = " + error.getLocalizedMessage() + "]");
            }

            @Override
            public void onPositionDiscontinuity() {
                Log.i(TAG, "onPositionDiscontinuity");
            }

            @Override
            public void onPlaybackParametersChanged(PlaybackParameters onPlaybackParametersChanged) {
                Log.i(TAG, "onPlaybackParametersChanged [onPlaybackParametersChanged = " + onPlaybackParametersChanged + "]");
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Uri selectedImageUri = null;
        if (requestCode == 201) {
            if (resultCode == RESULT_OK) {
                selectedImageUri = data.getData();
                if (selectedImageUri == null) {
                    Bundle bundle = data.getExtras();
                    if (bundle == null) return;
                    selectedImageUri = (Uri) bundle.get(Intent.EXTRA_STREAM);
                }
            } else {
                return;
            }
        }
        mPath = selectedImageUri;
        button.setText("Render");
        initExoPlayer();
    }
}
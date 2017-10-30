package gungoren.com.mediaplayerrenderscript;


import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.util.Log;
import android.view.Surface;


public class RenderscriptProcessor {

    private final static String TAG = RenderscriptProcessor.class.getSimpleName();

    private Allocation mInputAllocation;
    private Allocation mInterAllocation;
    private Allocation mOutputAllocation;

    private HandlerThread mProcessingThread;
    private Handler mProcessingHandler;

    private ScriptIntrinsicBlur mScriptBlur;
    private ScriptIntrinsicYuvToRGB mScriptYuvToRGB;
    public ProcessingTask mTask;

    private Surface outSurface;

    private boolean mNeedYuvConversion = false;

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public RenderscriptProcessor(RenderScript rs, int width, int height) {
        /*String model = Build.MODEL;
        Log.d("MODEL", model);
        if (model.contains("SM-N920") || model.contains("SM-G920") || model.contains("SM-G925") || model.contains("SM-G928")
                || model.contains("SM-G93") || model.contains("SM-N9208") || model.contains("SM-G95")) {
            mNeedYuvConversion = true;
        }

        Type.Builder yuvTypeBuilder = new Type.Builder(rs, Element.U8_4(rs));
        yuvTypeBuilder.setX(width);
        yuvTypeBuilder.setY(height);
        yuvTypeBuilder.setYuvFormat(ImageFormat.YUV_420_888);

        Type.Builder rgbTypeBuilder = new Type.Builder(rs, Element.RGBA_8888(rs));
        rgbTypeBuilder.setX(width);
        rgbTypeBuilder.setY(height);

        if (mNeedYuvConversion) {
            mInputAllocation = Allocation.createTyped(rs, yuvTypeBuilder.create(),
                    Allocation.USAGE_IO_INPUT | Allocation.USAGE_SCRIPT);

            mInterAllocation = Allocation.createTyped(rs, rgbTypeBuilder.create(),
                    Allocation.USAGE_SCRIPT);

            mScriptYuvToRGB = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        } else {
            mInputAllocation = Allocation.createTyped(rs, rgbTypeBuilder.create(),
                    Allocation.USAGE_IO_INPUT | Allocation.USAGE_SCRIPT);
        }

        mOutputAllocation = Allocation.createTyped(rs, rgbTypeBuilder.create(),
                Allocation.USAGE_IO_OUTPUT | Allocation.USAGE_SCRIPT);
        mScriptBlur = ScriptIntrinsicBlur.create(rs, mOutputAllocation.getElement());

        mProcessingThread = new HandlerThread("EffectProcessor");
        mProcessingThread.start();
        mProcessingHandler = new Handler(mProcessingThread.getLooper());

        mTask = new ProcessingTask(mInputAllocation, mNeedYuvConversion);*/
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Bitmap createBitmap_ScriptIntrinsicBlur(RenderScript renderScript, Bitmap src, int width, int height, float r) {

        //Radius range (0 < r <= 25)
        if(r <= 0){
            r = 0.1f;
        }else if(r > 25){
            r = 25.0f;
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        Allocation blurInput = Allocation.createFromBitmap(renderScript, src);
        Allocation blurOutput = Allocation.createFromBitmap(renderScript, bitmap);

        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(renderScript,Element.U8_4(renderScript));
        blur.setInput(blurInput);
        blur.setRadius(r);
        blur.forEach(blurOutput);

        blurOutput.copyTo(bitmap);
        return bitmap;
    }


    public void release() {
        mTask.release();
        mProcessingHandler.removeCallbacks(mTask);
        mProcessingThread.quit();
    }

    public Surface getInputSurface() {
        return mInputAllocation.getSurface();
    }

    public void setOutputSurface(Surface output) {
        outSurface = output;
        mOutputAllocation.setSurface(output);
    }


    @TargetApi(Build.VERSION_CODES.KITKAT)
    class ProcessingTask implements Runnable, Allocation.OnBufferAvailableListener {

        private final String TAG = ProcessingTask.class.getSimpleName();

        private int mPendingFrames = 0;
        private Allocation mInputAllocation;
        private boolean mNeedYuvConversion;

        public ProcessingTask(Allocation input, boolean needYuvConversion) {
            mInputAllocation = input;
            mInputAllocation.setOnBufferAvailableListener(this);
            mNeedYuvConversion = needYuvConversion;
        }

        public void release() {
            mInputAllocation.setOnBufferAvailableListener(null);
        }

        @Override
        public void onBufferAvailable(Allocation a) {
            synchronized (this) {
                Log.i(TAG, "onBufferAvailable mPendingFrames : " + mPendingFrames);
                mPendingFrames++;
                mProcessingHandler.post(this);
            }
        }

        @Override
        public void run() {
            // Find out how many frames have arrived
            int pendingFrames;
            synchronized (this) {
                pendingFrames = mPendingFrames;
                mPendingFrames = 0;
                mProcessingHandler.removeCallbacks(this);
            }

            Log.i(TAG, "run : pendingFrames : " + pendingFrames);

            // Get to newest input
            for (int i = 0; i < pendingFrames; i++) {
                Log.i(TAG, "run2 : pendingFrames : " + pendingFrames + ", i : " + i);
                mInputAllocation.ioReceive();
            }

            Log.i(TAG, "run2 : pendingFrames : " + pendingFrames);
            if (mNeedYuvConversion) {
                Log.i(TAG, "run31 : pendingFrames : " + pendingFrames);
                mScriptYuvToRGB.setInput(mInputAllocation);
                Log.i(TAG, "run32 : pendingFrames : " + pendingFrames);
                mScriptYuvToRGB.forEach(mInterAllocation);
                Log.i(TAG, "run33 : pendingFrames : " + pendingFrames);

                mScriptBlur.setInput(mInterAllocation);
                Log.i(TAG, "run34 : pendingFrames : " + pendingFrames);
                mScriptBlur.setRadius(10.0f);
                Log.i(TAG, "run35 : pendingFrames : " + pendingFrames);
                mScriptBlur.forEach(mOutputAllocation);
            } else {
                mScriptBlur.setInput(mInputAllocation);
                Log.i(TAG, "run41 : pendingFrames : " + pendingFrames);
                mScriptBlur.setRadius(10.0f);
                Log.i(TAG, "run42 : pendingFrames : " + pendingFrames);
                Log.i(TAG, "surface is valid.");
                mScriptBlur.forEach(mOutputAllocation);
            }

            Log.i(TAG, "run99 : pendingFrames : " + pendingFrames);
            mOutputAllocation.ioSend();
        }
    }
}



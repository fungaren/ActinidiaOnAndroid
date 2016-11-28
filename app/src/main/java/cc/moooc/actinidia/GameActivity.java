package cc.moooc.actinidia;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Typeface;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends Activity {
    private SurfaceView sfv;
    private SurfaceHolder sfh;

    static {
        System.loadLibrary("lua-lib");
        System.loadLibrary("native-lib");
    }

    private File gameDir;
    private boolean vertical=false;

    private Timer timer;     // produce refresh message
    private SoundPool sp;
    private Map<Integer,Boolean> sound_list;
    private Handler hRefresh = new Handler();   // redraw

    /**
     * Make preparations.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        if(i != null){
            gameDir = (File)i.getSerializableExtra("gameDir");
            vertical = i.getBooleanExtra("vertical", false);
        } else if (savedInstanceState != null){
            gameDir = (File)savedInstanceState.getSerializable("gameDir");
            vertical = savedInstanceState.getBoolean("vertical", false);
        }
        int toset = vertical?ActivityInfo.SCREEN_ORIENTATION_PORTRAIT:ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        if(getRequestedOrientation()!=toset){
            if(!vertical){
                setRequestedOrientation(toset);
                return;
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        sfv = new SurfaceView(this);
        sfh = sfv.getHolder();
        sfh.addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) { }
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                // Load game !
                sp = new SoundPool.Builder().setMaxStreams(255).build();
                sound_list = new HashMap<>();
                sp.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                    @Override
                    public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                        if(sound_list.get(i)) sp.play(i,1,1,1,0,1); // if loop, auto-play
                    }
                });

                String err = OnCreate();            // launch game
                if (!err.isEmpty()){
                    Toast.makeText(GameActivity.this, err, Toast.LENGTH_LONG).show();
                    GameActivity.this.finish();
                }

                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                    hRefresh.post(new Runnable() {
                        @Override
                        public void run() {
                            Canvas c = sfh.lockCanvas();
                            OnPaint(c);                 // invoke OnPaint() in script
                            sfh.unlockCanvasAndPost(c);
                        }
                    });
                    }
                }, 300, 20);       // period: 16.6667ms -> 60fps
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) { }
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) { }
        });
        // register message handler
        sfv.setOnTouchListener(new View.OnTouchListener() {
            float oldX = 0, oldY = 0;
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // int i = motionEvent.getActionIndex();    // do not support multi-touch now
                switch (motionEvent.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                            OnLButtonDown(motionEvent.getX(),motionEvent.getY());
                        break;
                    case MotionEvent.ACTION_UP:
                            OnLButtonUp(motionEvent.getX(),motionEvent.getY());
                        break;
                    case MotionEvent.ACTION_MOVE:{
                            float newX = motionEvent.getX();
                            float newY = motionEvent.getY();
                            // suppress
                            if(Math.abs(newX-oldX)>12||Math.abs(newY-oldY)>12)
                                OnMouseMove(newX,newY);
                            oldX = newX;
                            oldY = newY;
                        break;}
                    case MotionEvent.ACTION_POINTER_DOWN:
                        break;
                    case MotionEvent.ACTION_POINTER_UP:
                        break;
                }
                return true;
            }
        });
        setContentView(sfv);
    }

    private boolean backPressed =false;
    @Override
    public void onBackPressed() {
        timer.cancel();
        timer = null;
        OnClose();  // !
        sp.release();
        sp = null;
        backPressed = true;
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean("vertical", vertical);
        outState.putSerializable("gameDir", gameDir);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        if(timer != null) {
            timer.cancel();
            timer = null;
            if (!backPressed) OnClose();    // !
            sp.release();
            sp = null;
        }
        super.onDestroy();
    }

    /**
     * Interfaces for script
     */
    public int getScreenWidth(){
        return sfv.getWidth();
    }
    public int getScreenHeight(){
        return sfv.getHeight();
    }
    public Bitmap createImage(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.BLACK);
        return bmp;
    }
    public Bitmap createImageEx(int width, int height, int r, int g, int b){
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawColor(Color.rgb(r,g,b));
        return bmp;
    }
    public Bitmap createTransImage(int width, int height) {
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        return bmp;
    }
    public void printText(Bitmap bmp, float x, float y, String str, String fontName, float fontSize, int r, int g, int b) {
        Canvas c = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.rgb(r,g,b));
        p.setTextSize(fontSize);
        p.setTypeface(Typeface.DEFAULT);    // fontName is discarded currently
        c.drawText(str, x, y-p.getFontMetrics().ascent, p);
    }
    public int getWidth(Bitmap bmp){
        return bmp.getWidth();
    }
    public int getHeight(Bitmap bmp){
        return bmp.getHeight();
    }
    public String getText(String pathname) {
        String out = new String();
        try
        {
            File f = new File(gameDir, pathname.substring(4).replace('\\','/'));      // trim "res/" at the beginning
            BufferedReader r = new BufferedReader(new FileReader(f));
            String line;

            while((line = r.readLine())!=null) {
                out+=line+"\n";
            }
        }
        catch(IOException e){}
        return out;
    }
    public Bitmap getImage(String pathname) {
        try {
            File f = new File(gameDir, pathname.substring(4).replace('\\','/'));      // trim "res/" at the beginning
            return BitmapFactory.decodeStream(new FileInputStream(f));
        } catch (IOException e) {
            return null;
        }
    }
    public void pasteToImage(Bitmap gDest, Bitmap gSrc, float xDest, float yDest) {
        Canvas c = new Canvas(gDest);
        c.drawBitmap(gSrc, xDest, yDest, new Paint(Paint.ANTI_ALIAS_FLAG));
    }
    public void pasteToImageEx(Bitmap gDest, Bitmap gSrc,
                               int xDest, int yDest, int DestWidth, int DestHeight,
                               int xSrc, int ySrc, int SrcWidth, int SrcHeight) {
        Canvas c = new Canvas(gDest);
        c.drawBitmap(gSrc,
                new Rect(xSrc,ySrc,SrcWidth+xSrc,SrcHeight+ySrc),
                new Rect(xDest,yDest,DestWidth+xDest,DestHeight+yDest),
                new Paint(Paint.ANTI_ALIAS_FLAG));
    }
    public void alphaBlend(Bitmap gDest, Bitmap gSrc, int xDest, int yDest, int SrcAlpha) {
        Canvas c = new Canvas(gDest);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setAlpha(SrcAlpha);
        c.drawBitmap(gSrc, (float)xDest, (float)yDest, p);
    }
    public void alphaBlendEx(Bitmap gDest, Bitmap gSrc,
                             int xDest, int yDest, int DestWidth, int DestHeight,
                             int xSrc, int ySrc, int SrcWidth, int SrcHeight, int SrcAlpha) {
        Canvas c = new Canvas(gDest);
        Paint p = new Paint(new Paint(Paint.ANTI_ALIAS_FLAG));
        p.setAlpha(SrcAlpha);
        c.drawBitmap(gSrc,
                new Rect(xSrc,ySrc,SrcWidth+xSrc,SrcHeight+ySrc),
                new Rect(xDest,yDest,DestWidth+xDest,DestHeight+yDest),
                p);
    }
    public void pasteToWnd(Canvas WndGraphic, Bitmap g){
        WndGraphic.drawBitmap(g, 0, 0, null);
    }
    public void pasteToWndEx(Canvas WndGraphic, Bitmap g,
                             int xDest, int yDest, int DestWidth, int DestHeight,
                             int xSrc, int ySrc, int SrcWidth, int SrcHeight) {
        WndGraphic.drawBitmap(g,
                new Rect(xSrc,ySrc,SrcWidth+xSrc,SrcHeight+ySrc),
                new Rect(xDest,yDest,DestWidth+xDest,DestHeight+yDest),
                new Paint(Paint.ANTI_ALIAS_FLAG));
    }
    public int getSound(String pathname, boolean bLoop) {
        int sound = sp.load(gameDir.toString()+pathname.substring(3).replace('\\','/'),1);
        sound_list.put(sound, bLoop);
        if (bLoop)
            sp.setLoop(sound, -1);
        return sound;
    }
    public void stopSound(int sound) {
        sp.stop(sound);
        sp.unload(sound);
    }
    public void setVolume(int sound, float volume){
        sp.setVolume(sound,volume,volume);
    }
    public void playSound(int sound) {
        if(!sound_list.get(sound))
            sp.play(sound,1,1,1,0,1); // loop will auto-play
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String OnCreate();
    public native int OnPaint(Canvas c);
    public native int OnClose();
    public native int OnLButtonDown(float x, float y);
    public native int OnLButtonUp(float x, float y);
    public native int OnMouseMove(float x, float y);
}

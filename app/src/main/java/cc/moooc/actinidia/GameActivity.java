package cc.moooc.actinidia;

import android.app.Activity;
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;

public class GameActivity extends Activity {
    private SurfaceView sfv;
    private SurfaceHolder sfh;
    private Boolean initialized = false;

    static {
        System.loadLibrary("lua-lib");
        System.loadLibrary("native-lib");
    }

    private File gameDir;
    private boolean vertical = false; // in "res/config.ini"

    private Timer timer;     // Refresh the surface view. Any tasks will be processed here.
    private TimerTask paint_loop;
    private SoundPool sp;
    private Map<Integer,Boolean> sound_list;

    private File data_file;        // user data ("res/data")
    private Properties prop;        // load settings at beginning, save setting on destroy

    /**
     * Make preparations.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = getIntent();
        if(i != null) {
            gameDir = (File)i.getSerializableExtra("gameDir");
            vertical = i.getBooleanExtra("vertical", false);    // horizontal for default
            if(!vertical) {                  // a new activity is vertical
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // Load settings
        data_file = new File(gameDir, "data");
        prop = new Properties();
        try {
            Reader data_reader = new FileReader(data_file);
            prop.load(data_reader);
            data_reader.close();
        }catch(IOException e){}

        // init SoundPool
        sp = new SoundPool.Builder().setMaxStreams(255).build();
        sound_list = new HashMap<>();
        sp.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i1) {
                if(sound_list.get(i)) sp.play(i,1,1,1,0,1); // if loop, auto-play
            }
        });

        // set SurfaceView
        sfv = new SurfaceView(this);
        sfh = sfv.getHolder();
        sfh.addCallback(new SurfaceHolder.Callback2() {
            @Override
            public void surfaceRedrawNeeded(SurfaceHolder surfaceHolder) {}
            @Override
            public void surfaceCreated(SurfaceHolder surfaceHolder) {
                if (!initialized) {
                    // Lua init & Launch
                    String err = OnCreate();

                    if (!err.isEmpty()) {
                        Toast.makeText(GameActivity.this, err, Toast.LENGTH_LONG).show();
                        GameActivity.this.finish();     // display error and exit
                    }
                    initialized = true;
                    setTimer();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {}
            @Override
            public void surfaceDestroyed(SurfaceHolder surfaceHolder) {}
        });

        // set message handler
        sfv.setOnTouchListener(new View.OnTouchListener() {
            float oldX = 0, oldY = 0;
            float p1, p2;

            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                // int i = motionEvent.getActionIndex();
                switch (motionEvent.getActionMasked()) {
                    // Note that we MUST NOT invoke callbacks directly.
                    // As we shouldn't do that on main thread.
                    case MotionEvent.ACTION_DOWN:   // A primary pointer has gone down.
                        p1 = motionEvent.getX(0);
                        p2 = motionEvent.getY(0);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                OnLButtonDown(p1,p2);
                            }
                        }, 0);
                        break;
                    case MotionEvent.ACTION_UP:     // A primary pointer has gone up.
                        p1 = motionEvent.getX(0);
                        p2 = motionEvent.getY(0);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                OnLButtonUp(p1,p2);
                            }
                        }, 0);
                        break;
                    case MotionEvent.ACTION_POINTER_DOWN:   // A non-primary pointer has gone down.
                        p1 = motionEvent.getX(1);
                        p2 = motionEvent.getY(1);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                OnLButtonDown(p1,p2);
                            }
                        }, 0);
                        break;
                    case MotionEvent.ACTION_POINTER_UP:     // A non-primary pointer has gone up.
                        p1 = motionEvent.getX(1);
                        p2 = motionEvent.getY(1);
                        timer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                OnLButtonUp(p1,p2);
                            }
                        }, 0);
                        break;
                    case MotionEvent.ACTION_MOVE:
                    {
                        float newX = motionEvent.getX(0);
                        float newY = motionEvent.getY(0);
                        p1 = newX; p2 = newY;
                        // Suppress slight shiver
                        if (Math.abs(newX-oldX)>4 || Math.abs(newY-oldY)>4)
                            timer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    OnMouseMove(p1,p2);
                                }
                            }, 0);
                        oldX = newX;
                        oldY = newY;
                        break;
                    }
                }
                return true;
            }
        });
        setContentView(sfv);
    }

    private void setTimer()
    {
        timer = new Timer();
        paint_loop = new TimerTask() {
            @Override
            public void run() {
                Canvas c = sfh.lockCanvas();
                // invoke OnPaint() in script
                OnPaint(c);
                sfh.unlockCanvasAndPost(c);
            }
        };
        // period: 16.6667ms -> 60fps
        timer.schedule(paint_loop, 300, 17);
    }

    private void saveUserData()
    {
        try {
            Writer data_writer = new FileWriter(data_file);
            prop.store(data_writer, null);   // save user data
            data_writer.close();
        }catch (IOException e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        if (initialized) {
            OnClose(); // !!!
            saveUserData();

            if(sp != null) {
                sp.release();
                sp = null;
            }
        }
        super.onDestroy();
    }

    @Override
    protected void onPause() {
        if (initialized) {
            paint_loop.cancel();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    OnKillFocus();  // !
                }
            },0);
            timer.cancel();
            timer = null;
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        if (initialized) {
            setTimer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    OnSetFocus();  // !
                }
            },0);
        }
        super.onResume();
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
    public String getSetting(String key) {
        return prop.getProperty(key);   // return null if not found
    }
    public void saveSetting(String key, String value) {
        prop.setProperty(key,value);
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
    public native int OnSetFocus();
    public native int OnKillFocus();
}

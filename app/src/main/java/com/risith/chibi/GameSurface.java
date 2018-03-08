package com.risith.chibi;

/**
 * Created by RISITH-PC on 2018-03-07.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


public class GameSurface extends SurfaceView implements SurfaceHolder.Callback {


    private GameThread gameThread;

    private final List<ChibiCharacter> chibiList = new ArrayList<ChibiCharacter>();
    private final List<Explosion> explosionList = new ArrayList<Explosion>();

    private static final int MAX_STREAMS=100;
    private int soundIdExplosion;
    private int soundIdBackground;

    private boolean soundPoolLoaded;
    private SoundPool soundPool;

    public GameSurface(Context context)  {
        super(context);

        // Make Game Surface focusable so it can handle events. .
        this.setFocusable(true);

        // Sét callback.
        this.getHolder().addCallback(this);

        this.initSoundPool();
    }

    //-----------------------------------------------------------Sounds------------------------------------------------------------//

    private void initSoundPool()  {
        // With Android API >= 21.
        if (Build.VERSION.SDK_INT >= 21 ) {

            AudioAttributes audioAttrib = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();

            SoundPool.Builder builder= new SoundPool.Builder();
            builder.setAudioAttributes(audioAttrib).setMaxStreams(MAX_STREAMS);

            this.soundPool = builder.build();
        }
        // With Android API < 21
        else {
            // SoundPool(int maxStreams, int streamType, int srcQuality)
            this.soundPool = new SoundPool(MAX_STREAMS, AudioManager.STREAM_MUSIC, 0);
        }

        // When SoundPool load complete.
        this.soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                soundPoolLoaded = true;

                // Playing background sound.
                playSoundBackground();
            }
        });

        // Load the sound background.mp3 into SoundPool
        this.soundIdBackground= this.soundPool.load(this.getContext(), R.raw.backgroundmusic,1);

        // Load the sound explosion.wav into SoundPool
        this.soundIdExplosion = this.soundPool.load(this.getContext(), R.raw.explosionmusic,1);


    }

    public void playSoundExplosion()  {
        if(this.soundPoolLoaded) {
            float leftVolumn = 0.8f;
            float rightVolumn =  0.8f;
            // Play sound explosion.wav
            int streamId = this.soundPool.play(this.soundIdExplosion,leftVolumn, rightVolumn, 1, 0, 1f);
        }
    }

    public void playSoundBackground()  {
        if(this.soundPoolLoaded) {
            float leftVolumn = 0.8f;
            float rightVolumn =  0.8f;
            // Play sound background.mp3
            int streamId = this.soundPool.play(this.soundIdBackground,leftVolumn, rightVolumn, 1, -1, 1f);
        }
    }

    //-----------------------------------------------------------------------------------------------------------------------//

    public void update()  {
        for(ChibiCharacter chibi: chibiList) {
            chibi.update();
        }

        for(Explosion explosion: this.explosionList)  {
            explosion.update();
        }

        Iterator<Explosion> iterator= this.explosionList.iterator();
        while(iterator.hasNext())  {
            Explosion explosion = iterator.next();

            if(explosion.isFinish()) {
                // If explosion finish, Remove the current element from the iterator & list.
                iterator.remove();
                continue;
            }
        }
    }

    @Override
    public void draw(Canvas canvas)  {
        super.draw(canvas);
        canvas.drawColor(Color.GREEN);

        for(ChibiCharacter chibi: chibiList) {
            chibi.draw(canvas);
        }

        for(Explosion explosion: this.explosionList)  {
            explosion.draw(canvas);
        }
    }

    // Implements method of SurfaceHolder.Callback
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        Bitmap chibiBitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.chibi);
        ChibiCharacter chibi = new ChibiCharacter(this,chibiBitmap,100,50);

        Bitmap zoniBitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.zoni);
        ChibiCharacter zoni = new ChibiCharacter(this,zoniBitmap,300,150);

        this.chibiList.add(chibi);
        this.chibiList.add(zoni);

        this.gameThread = new GameThread(this,holder);
        this.gameThread.setRunning(true);
        this.gameThread.start();
    }

    // Implements method of SurfaceHolder.Callback
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

    }

    // Implements method of SurfaceHolder.Callback
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry= true;
        while(retry) {
            try {
                this.gameThread.setRunning(false);

                // Parent thread must wait until the end of GameThread.
                this.gameThread.join();
            }catch(InterruptedException e)  {
                e.printStackTrace();
            }
            retry= true;
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            int x=  (int)event.getX();
            int y = (int)event.getY();

            Iterator<ChibiCharacter> iterator= this.chibiList.iterator();

            while(iterator.hasNext()) {
                ChibiCharacter chibi = iterator.next();
                if( chibi.getX() < x && x < chibi.getX() + chibi.getWidth()
                        && chibi.getY() < y && y < chibi.getY()+ chibi.getHeight())  {
                    // Remove the current element from the iterator and the list.
                    iterator.remove();

                    // Create Explosion object.
                    Bitmap bitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.explosion);
                    Explosion explosion = new Explosion(this, bitmap,chibi.getX(),chibi.getY());

                    this.explosionList.add(explosion);
                }
            }

            Bitmap chibiBitmap = BitmapFactory.decodeResource(this.getResources(),R.drawable.chibi);
            ChibiCharacter cha = new ChibiCharacter(this,chibiBitmap,100,50);
            this.chibiList.add(cha);

            for(ChibiCharacter chibi: chibiList) {
                int movingVectorX =x-  chibi.getX() ;
                int movingVectorY =y-  chibi.getY() ;
                chibi.setMovingVector(movingVectorX, movingVectorY);
            }

            return true;
        }
        return false;
    }

}
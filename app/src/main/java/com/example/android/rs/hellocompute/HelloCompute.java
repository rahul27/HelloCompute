/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.rs.hellocompute;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.Type;
import android.view.Surface;
import android.view.Surface.OutOfResourcesException;
import android.view.TextureView;
import android.widget.ImageView;

public class HelloCompute extends Activity {
    private Bitmap mBitmapIn;
    private static final String TAG = "HelloCompute";

    private RenderScript mRS;
    private ScriptC_mono mScript;
    private Allocation mSurfaceAllocation;
    private Allocation mCanvasAllocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mBitmapIn = loadBitmap(R.drawable.data);
        int x = mBitmapIn.getWidth();
        int y = mBitmapIn.getHeight();

        ImageView in = (ImageView) findViewById(R.id.displayin);
        in.setImageBitmap(mBitmapIn);

        mRS = RenderScript.create(this);

        mScript = new ScriptC_mono(mRS, getResources(), R.raw.mono);

        mCanvasAllocation = Allocation.createTyped(mRS,
                new Type.Builder(mRS, Element.RGBA_8888(mRS))
                        .setX(x).setY(y).create(),
                Allocation.USAGE_IO_INPUT | Allocation.USAGE_SCRIPT);

        mSurfaceAllocation = Allocation.createTyped(mRS,
                new Type.Builder(mRS, Element.RGBA_8888(mRS))
                        .setX(x).setY(y).create(),
                Allocation.USAGE_IO_OUTPUT | Allocation.USAGE_SCRIPT);

        final TextureView mTextureView = (TextureView) findViewById(R.id.displayout);
        mTextureView.setSurfaceTextureListener(
                new TextureView.SurfaceTextureListener() {
                    @Override
                    public void onSurfaceTextureAvailable(SurfaceTexture s,
                                                          int w, int h) {
                        if (s != null) mSurfaceAllocation.setSurface(new Surface(s));
                        mScript.forEach_filter(mCanvasAllocation, mSurfaceAllocation);
                        mSurfaceAllocation.ioSend();
                    }

                    @Override
                    public void onSurfaceTextureUpdated(SurfaceTexture s) {
                    }

                    @Override
                    public void onSurfaceTextureSizeChanged(SurfaceTexture s,
                                                            int w, int h) {
                        if (s != null) mSurfaceAllocation.setSurface(new Surface(s));
                    }

                    @Override
                    public boolean onSurfaceTextureDestroyed(SurfaceTexture s) {
                        mSurfaceAllocation.setSurface(null);
                        return true;
                    }
                });

        try {
            Surface surface = mCanvasAllocation.getSurface();
            Canvas canvas = surface.lockCanvas(new Rect(0, 0, 100, 100));
            canvas.drawBitmap(mBitmapIn, 0, 0, new Paint());
            surface.unlockCanvasAndPost(canvas);
            mCanvasAllocation.ioReceive();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (OutOfResourcesException e) {
            e.printStackTrace();
        }
    }

    private Bitmap loadBitmap(int resource) {
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        return BitmapFactory.decodeResource(getResources(), resource, options);
    }
}
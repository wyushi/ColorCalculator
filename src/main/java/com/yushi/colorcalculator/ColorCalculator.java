package com.yushi.colorcalculator;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;

/**
 * Created by yushi on 2017-06-16.
 */

public class ColorCalculator {

    public interface ResultListener {
        void done(float luminance);
        void fail(Error error);
    }

    public interface ColorAlgorithmPlugin {
        void calculate(Bitmap bitmap);
    }

    private View mRoot;
    private View mFrontView;
    private ImageView mBackView;
    private ResultListener mListener;
    private ColorAlgorithmPlugin mColorAlgorithm;

    public void setColorCalculationListener(ResultListener mListener) {
        this.mListener = mListener;
    }

    public void setColorAlgorithmPlugin(ColorAlgorithmPlugin mColorAlgorithm) {
        this.mColorAlgorithm = mColorAlgorithm;
    }

    public ColorCalculator(View root, View frontView, ImageView backView) {
        mRoot = root;
        mFrontView = frontView;
        mBackView = backView;
        mColorAlgorithm = new ColorAlgorithmPlugin() {
            @Override
            public void calculate(Bitmap bitmap) {
                if (mListener == null) return;
                if (bitmap == null) {
                    mListener.fail(new Error("The front view and the back view do not have any overlap"));
                }
                int averageColor = getAverageColor(bitmap);
                float luminance = luminance(averageColor);
                mListener.done(luminance);
            }
        };
    }

    public void setup() {
        mRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                calculateTextColor();
            }
        });
    }

    public void calculateTextColor() {
        int[] position, size;
        position = getRelativePosition(mBackView, mFrontView);
        size =  getSize(mFrontView);
        Bitmap part = cropSubImageBasedOnViewCoordinates(mBackView, position, size);
        mColorAlgorithm.calculate(part);
    }

    private static int[] getLocation(View v) {
        int[] position = new int[2];
        v.getLocationOnScreen(position);
        return position;
    }

    private static int[] getSize(View v) {
        int[] size = new int[2];
        size[0] = v.getMeasuredWidth();
        size[1] = v.getMeasuredHeight();
        return size;
    }

    private static int[] getRelativePosition(View ref, View tar) {
        int[] rPos = getLocation(ref);
        int[] tPos = getLocation(tar);
        return getRelativePosition(rPos, tPos);
    }

    private static int[] getRelativePosition(int[] r, int[] t) {
        int[] rp = new int[2];
        rp[0] = t[0] - r[0];
        rp[1] = t[1] - r[1];
        return rp;
    }

    private static Bitmap cropSubImageBasedOnViewCoordinates(ImageView v, int[] pos, int[] size) {
        if (v.getDrawable() == null) return null;

        Rect bounds = v.getDrawable().getBounds();
        if (bounds.width() * bounds.height() == 0) {
            return null;
        }

        float[] ratio = {
                (float) v.getDrawable().getIntrinsicWidth() / v.getDrawable().getBounds().width(),
                (float) v.getDrawable().getIntrinsicHeight() / v.getDrawable().getBounds().height()
        };

        int[] bPos = {
                (int)(pos[0] * ratio[0]),
                (int)(pos[1] * ratio[1])
        };

        int[] bSize = {
                (int)(size[0] * ratio[0]),
                (int)(size[1] * ratio[1])
        };

        Bitmap bitmap = ((BitmapDrawable)v.getDrawable()).getBitmap();

        int left, top, right, bottom;
        left = bPos[0];
        top = bPos[1];
        right = bPos[0] + bSize[0];
        bottom = bPos[1] + bSize[1];

        if (right < 0 || left > bitmap.getWidth() || bottom < 0 || top > bitmap.getHeight()) {
            return null;
        }

        left = Math.max(left, 0);
        top = Math.max(top, 0);
        right = Math.min(right, bitmap.getWidth());
        bottom = Math.min(bottom, bitmap.getHeight());

        return bitmap.createBitmap(bitmap, left, top, right - left, bottom - top);
    }

    private static int getAverageColor(Bitmap bitmap) {
        int r = 0, g = 0, b = 0;
        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                int color = bitmap.getPixel(i, j);
                r += Color.red(color);
                g += Color.green(color);
                b += Color.blue(color);
            }
        }
        int numOfPixel = bitmap.getWidth() * bitmap.getHeight();
        r /= numOfPixel;
        g /= numOfPixel;
        b /= numOfPixel;
        return Color.rgb(r, g, b);
    }

    private static float luminance(int color) {
        double red = Color.red(color) / 255.0;
        red = red < 0.03928 ? red / 12.92 : Math.pow((red + 0.055) / 1.055, 2.4);
        double green = Color.green(color) / 255.0;
        green = green < 0.03928 ? green / 12.92 : Math.pow((green + 0.055) / 1.055, 2.4);
        double blue = Color.blue(color) / 255.0;
        blue = blue < 0.03928 ? blue / 12.92 : Math.pow((blue + 0.055) / 1.055, 2.4);
        return (float) ((0.2126 * red) + (0.7152 * green) + (0.0722 * blue));
    }

}

package com.google.zxing.ocr.util;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.util.Log;

import com.googlecode.leptonica.android.Pixa;
import com.googlecode.tesseract.android.ResultIterator;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.tesseract.android.TessBaseAPI.PageIteratorLevel;

public class TessOCRUtils {
	private static final String TAG = TessOCRUtils.class.getSimpleName();
	private static final String TESSBASE_PATH = "/mnt/sdcard/tesseract/";
    private static final String DEFAULT_LANGUAGE = "eng";
    private static final String EXPECTED_FILE = TESSBASE_PATH + "tessdata/" + DEFAULT_LANGUAGE
            + ".traineddata";
    
    public static String getOcrUTF8Text(Bitmap bitMap) {
        // First, make sure the eng.traineddata file exists.
        Log.i(TAG, "Make sure that you've copied " + DEFAULT_LANGUAGE + ".traineddata to "+ EXPECTED_FILE );
        final String inputText = "11876.897";
        // Attempt to initialize the API.
        final Bitmap bmp = getTextImage(inputText, 640, 480);
        // Attempt to initialize the API.
        final TessBaseAPI baseApi = new TessBaseAPI();
        baseApi.init(TESSBASE_PATH, DEFAULT_LANGUAGE);
        //设置识别白名单， 识别为数字和小数点。 
        baseApi.setVariable(TessBaseAPI.VAR_CHAR_WHITELIST, "0123456789.");
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_SINGLE_LINE);
        baseApi.setImage(bmp);
        // Ensure that the result is correct.
        final String outputText = baseApi.getUTF8Text();
        // Attempt to shut down the API.
        baseApi.end();
        bmp.recycle();
        Log.i(TAG, "outputText= " + outputText );
        return outputText;
    }
    
    private static Bitmap getTextImage(String text, int width, int height) {
        final Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        final Paint paint = new Paint();
        final Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.WHITE);
        paint.setColor(Color.BLACK);
        paint.setStyle(Style.FILL);
        paint.setAntiAlias(true);
        paint.setTextAlign(Align.CENTER);
        paint.setTextSize(24.0f);
        canvas.drawText(text, width / 2, height / 2, paint);

        return bmp;
    }

}

/*
 * Copyright 2007 ZXing authors
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

package com.google.zxing;

import android.util.Log;

import com.google.zxing.aztec.AztecReader;
import com.google.zxing.datacolumn.DataColumnReader;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.maxicode.MaxiCodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.multi.datacolumn.DataColumnAndOnedMultiReader;
import com.google.zxing.oned.MultiFormatOneDReader;
import com.google.zxing.pdf417.PDF417Reader;
import com.google.zxing.qrcode.QRCodeReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

/**
 * MultiFormatReader is a convenience class and the main entry point into the library for most uses.
 * By default it attempts to decode all barcode formats that the library supports. Optionally, you
 * can provide a hints object to request different behavior, for example only decoding QR codes.
 *
 * @author Sean Owen
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class MultiFormatReader implements Reader {

  private Map<DecodeHintType,?> hints;
  private Reader[] readers;
  private MultipleBarcodeReader[] multiReaders;

  /**
   * This version of decode honors the intent of Reader.decode(BinaryBitmap) in that it
   * passes null as a hint to the decoders. However, that makes it inefficient to call repeatedly.
   * Use setHints() followed by decodeWithState() for continuous scan applications.
   *
   * @param image The pixel data to decode
   * @return The contents of the image
   * @throws NotFoundException Any errors which occurred
   */
  @Override
  public Result decode(BinaryBitmap image) throws NotFoundException {
    setHints(null);
    return decodeInternal(image);
  }

  /**
   * Decode an image using the hints provided. Does not honor existing state.
   *
   * @param image The pixel data to decode
   * @param hints The hints to use, clearing the previous state.
   * @return The contents of the image
   * @throws NotFoundException Any errors which occurred
   */
  @Override
  public Result decode(BinaryBitmap image, Map<DecodeHintType,?> hints) throws NotFoundException {
    setHints(hints);
    return decodeInternal(image);
  }

  /**
   * Decode an image using the state set up by calling setHints() previously. Continuous scan
   * clients will get a <b>large</b> speed increase by using this instead of decode().
   *
   * @param image The pixel data to decode
   * @return The contents of the image
   * @throws NotFoundException Any errors which occurred
   */
  public Result decodeWithState(BinaryBitmap image) throws NotFoundException {
    // Make sure to set up the default state so we don't crash
	Log.i("MultiFormatReader", "start into single reader");
    if (readers == null) {
      setHints(null);
    }
    return decodeInternal(image);
  }
  /**
   * 解码符合类型的图像 
   * @param image
   * @return
   * @throws NotFoundException
   */
  public Result[] decodeMultiWithState(BinaryBitmap image) throws NotFoundException {
	    // Make sure to set up the default state so we don't crash
	    Log.i("MultiFormatReader", "start into multi reader");
	    if (multiReaders == null) {
	    	setHints(null);
	    }
	    if (multiReaders != null) {
	    	Log.i("MultiFormatReader", "found "+multiReaders.length+" multiple readers!");
	        for (MultipleBarcodeReader reader : multiReaders) {
	          try {
	            return reader.decodeMultiple(image, hints);
	          } catch (ReaderException re) {
	            // continue
	          }
	        }
	    }
	    
	    throw NotFoundException.getNotFoundInstance();
	  }
	  
  

  /**
   * This method adds state to the MultiFormatReader. By setting the hints once, subsequent calls
   * to decodeWithState(image) can reuse the same set of readers without reallocating memory. This
   * is important for performance in continuous scan clients.
   *
   * @param hints The set of hints to use for subsequent calls to decode(image)
   */
  public void setHints(Map<DecodeHintType,?> hints) {
    this.hints = hints;

    //首先解析条形码， 还是后解析条形码。 
    boolean tryHarder = hints != null && hints.containsKey(DecodeHintType.TRY_HARDER);
    @SuppressWarnings("unchecked")
    Collection<BarcodeFormat> formats =
        hints == null ? null : (Collection<BarcodeFormat>) hints.get(DecodeHintType.POSSIBLE_FORMATS);
    Collection<Reader> readers = new ArrayList<Reader>();
    Collection<MultipleBarcodeReader> multireaders = new ArrayList<MultipleBarcodeReader>();
    
    if (formats != null) { // 添加一维码 
      boolean addOneDReader =
          formats.contains(BarcodeFormat.UPC_A) ||
          formats.contains(BarcodeFormat.UPC_E) ||
          formats.contains(BarcodeFormat.EAN_13) ||
          formats.contains(BarcodeFormat.EAN_8) ||
          formats.contains(BarcodeFormat.CODABAR) ||
          formats.contains(BarcodeFormat.CODE_39) ||
          formats.contains(BarcodeFormat.CODE_93) ||
          formats.contains(BarcodeFormat.CODE_128) ||
          formats.contains(BarcodeFormat.ITF) ||
          formats.contains(BarcodeFormat.RSS_14) ||
          formats.contains(BarcodeFormat.RSS_EXPANDED);
      // Put 1D readers upfront in "normal" mode
      if (addOneDReader && !tryHarder) { // 一维码的解码器 
        readers.add(new MultiFormatOneDReader(hints));
      }
      if (formats.contains(BarcodeFormat.QR_CODE)) {
        readers.add(new QRCodeReader());
      }
      if (formats.contains(BarcodeFormat.DATA_MATRIX)) {
        readers.add(new DataMatrixReader());
      }
      if (formats.contains(BarcodeFormat.AZTEC)) {
        readers.add(new AztecReader());
      }
      if (formats.contains(BarcodeFormat.PDF_417)) {
         readers.add(new PDF417Reader());
      }
      if (formats.contains(BarcodeFormat.MAXICODE)) {
         readers.add(new MaxiCodeReader());
      }
      // just for wisedu //这个用来解析成绩登分册表单 
      if (formats.contains(BarcodeFormat.DATA_COLUMN)) { //如果同时启用了条形码， 则使用条形码的复合
          readers.add(new DataColumnReader());
      }
      
      if(formats.contains(BarcodeFormat.DATA_COLUMN_MULTI)){
    	  //Log.i("MultiFormatReader", "start to add DataColumnAndOnedMultiReader.");
    	  multireaders.add(new DataColumnAndOnedMultiReader(hints)); //条形码和datacolumn的复合阅读器。 
      }
      // At end in "try harder" mode
      if (addOneDReader && tryHarder) {
        readers.add(new MultiFormatOneDReader(hints));
      }
    }
    //未进行任何设置， 则使用默认的方式. 
    if (readers.isEmpty()) {
      if (!tryHarder) {
        readers.add(new MultiFormatOneDReader(hints));
      }
      //readers.add(new DataColumnReader());
      readers.add(new QRCodeReader());
      readers.add(new DataMatrixReader());
      readers.add(new AztecReader());
      readers.add(new PDF417Reader());
      readers.add(new MaxiCodeReader());
      if (tryHarder) {
        readers.add(new MultiFormatOneDReader(hints));
      }
      //默认支持 dataColumn和条形码的阅读器
      //multireaders.add(new DataColumnAndOnedMultiReader(hints));
    }
    this.readers = readers.toArray(new Reader[readers.size()]);
    this.multiReaders = multireaders.toArray(new MultipleBarcodeReader[multireaders.size()]);
    
  }

  @Override
  public void reset() {
    if (readers != null) {
      for (Reader reader : readers) {
        reader.reset();
        
      }
    }
    if (multiReaders != null) {
    	  for (MultipleBarcodeReader reader : multiReaders) {
    	    	 reader.reset();
    	  }
      }
  }

  /**
   * 循环所有的reader， 尝试进行解析，知道解析成功。 
   * @param image
   * @return
   * @throws NotFoundException
   */
  private Result decodeInternal(BinaryBitmap image) throws NotFoundException {
    if (readers != null) {
      Log.i("MultiFormatReader", "found "+readers.length+" readers!");
      for (Reader reader : readers) {
        try {
          return reader.decode(image, hints);
        } catch (ReaderException re) {
          // continue
        }
      }
    }
    throw NotFoundException.getNotFoundInstance();
  }

 
  
  
}

package com.google.zxing.multi.datacolumn;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.ReaderException;
import com.google.zxing.Result;
import com.google.zxing.aztec.AztecReader;
import com.google.zxing.datacolumn.DataColumnReader;
import com.google.zxing.datamatrix.DataMatrixReader;
import com.google.zxing.maxicode.MaxiCodeReader;
import com.google.zxing.multi.MultipleBarcodeReader;
import com.google.zxing.oned.MultiFormatOneDReader;
import com.google.zxing.pdf417.PDF417Reader;
import com.google.zxing.qrcode.QRCodeReader;

/**
 * DataColumn 和 1D 条形码的混合读取
 * 
 * @author stzhang
 * 
 */
public class DataColumnAndOnedMultiReader implements MultipleBarcodeReader {

	private Reader[] readers;
	private Map<DecodeHintType, ?> multiHints = null;

	public DataColumnAndOnedMultiReader(Map<DecodeHintType, ?> hints) {
		multiHints = hints;
	}

	@Override
	public Result[] decodeMultiple(BinaryBitmap image) throws NotFoundException {
		return this.decodeMultiple(image, multiHints);
	}

	@Override
	public Result[] decodeMultiple(BinaryBitmap image,
			Map<DecodeHintType, ?> hints) throws NotFoundException {
		this.setHints(hints);
		Collection<Result> results = new ArrayList<Result>();
		if (readers != null) {
		      for (Reader reader : readers) {
		        try {
		          Result r= reader.decode(image, hints);
		          results.add(r);
		        } catch (ReaderException re) {
		          // continue
		        }
		      }
		      return results.toArray(new Result[results.size()]);
		}
		throw NotFoundException.getNotFoundInstance();

	}

	/**
	 * 初始化hints
	 * @param hints
	 */
	@SuppressWarnings("unchecked")
	private void setHints (Map<DecodeHintType, ?> hints){
		Collection<BarcodeFormat> formats = hints == null ? null
				: (Collection<BarcodeFormat>) hints
						.get(DecodeHintType.POSSIBLE_FORMATS);
		Collection<Reader> readers = new ArrayList<Reader>();
		if (formats != null) { // 添加一维码
			boolean addOneDReader = formats.contains(BarcodeFormat.UPC_A)
					|| formats.contains(BarcodeFormat.UPC_E)
					|| formats.contains(BarcodeFormat.EAN_13)
					|| formats.contains(BarcodeFormat.EAN_8)
					|| formats.contains(BarcodeFormat.CODABAR)
					|| formats.contains(BarcodeFormat.CODE_39)
					|| formats.contains(BarcodeFormat.CODE_93)
					|| formats.contains(BarcodeFormat.CODE_128)
					|| formats.contains(BarcodeFormat.ITF)
					|| formats.contains(BarcodeFormat.RSS_14)
					|| formats.contains(BarcodeFormat.RSS_EXPANDED);
			// just for wisedu //这个用来解析成绩登分册表单
			if (formats.contains(BarcodeFormat.DATA_COLUMN)) { // 如果同时启用了条形码，
				readers.add(new DataColumnReader());
			}
			// Put 1D readers upfront in "normal" mode
			if (addOneDReader) { // 一维码的解码器
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
			
			
			
		}else{ //默认添加多个代码
			readers.add(new DataColumnReader());
			readers.add(new MultiFormatOneDReader(hints));
		}
		
		this.readers = readers.toArray(new Reader[readers.size()]);
	}
	
	@Override
	public void reset() {
		// TODO Auto-generated method stub
	}

}

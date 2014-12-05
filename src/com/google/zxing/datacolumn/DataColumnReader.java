package com.google.zxing.datacolumn;

import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.Reader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.DecoderResult;
import com.google.zxing.ocr.util.TessOCRUtils;

public class DataColumnReader implements Reader {
	private static final ResultPoint[] NO_POINTS = new ResultPoint[0];
	 
	@Override
	public Result decode(BinaryBitmap image) throws NotFoundException, ChecksumException, FormatException {
		return decode(image, null);
	}
	
	/**解码图像文件， 解码的时候， 返回Result
	 * 返回的Result， 作为回调参数到CaptureActivity-decodeOrStoreSavedBitmap
	 * 
	 * */
	@Override
	public Result decode(BinaryBitmap image, Map<DecodeHintType, ?> hints)
			throws NotFoundException, ChecksumException, FormatException {
		// TODO Auto-generated method stub
		DecoderResult decoderResult;
		ResultPoint[] points;
		points = NO_POINTS;
		Result result = new Result("", null, points, BarcodeFormat.DATA_COLUMN);
		TessOCRUtils.getOcrUTF8Text(null);
		throw NotFoundException.getNotFoundInstance();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub

	}

}

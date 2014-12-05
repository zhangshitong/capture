package com.google.zxing.client.result;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.Result;

public class DataColumnResultParser extends ResultParser {
	@Override
	public ParsedResult parse(Result result) {
		BarcodeFormat format = result.getBarcodeFormat();
	    if (!(format == BarcodeFormat.DATA_COLUMN)
	    		|| format == BarcodeFormat.DATA_COLUMN_MULTI) {
	      return null;
	    }
	    String rawText = getMassagedText(result);
	    return new DataColumnParsedResult(rawText, "") ;
	}
}

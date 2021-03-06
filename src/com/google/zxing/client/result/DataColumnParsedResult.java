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

package com.google.zxing.client.result;

/**
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class DataColumnParsedResult extends ParsedResult {

	private final String rawDataColumn;
	private String productID;

	DataColumnParsedResult(String rawDataColumn, String productID) {
		super(ParsedResultType.DATA_COLUMN);
		this.rawDataColumn = rawDataColumn;
		this.productID = productID;
	}

	DataColumnParsedResult(String rawDataColumn) {
		super(ParsedResultType.DATA_COLUMN);
		this.rawDataColumn = rawDataColumn;
	}

	public String getProductID() {
		return productID;
	}

	public void setProductID(String productID) {
		this.productID = productID;
	}

	public String getRawDataColumn() {
		return rawDataColumn;
	}

	@Override
	public String getDisplayResult() {
		return rawDataColumn;
	}

}

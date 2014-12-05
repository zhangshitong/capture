/*
 * Copyright 2010 ZXing authors
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

package com.google.zxing.common.detector;

import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.BitMatrix;

/**
 * <p>
 * 专门为Dataculumn识别使用。 
 * 成绩单按图形内容为  左上角一个黑色矩形，垂直放置。 
 *                   右上角一个黑色矩形，水平放置。
 *                   左下角一个黑色矩形， 水平放置。
 *                   右下角一个条形码， 水平放置。
 * Detects a candidate barcode-like rectangular region within an image. It
 * starts around the center of the image, increases the size of the candidate
 * region until it finds a white rectangular region. By keeping track of the
 * last black points it encountered, it determines the corners of the barcode.
 * </p>
 *
 * @author David Olivier
 */
public final class BlackRectangleDetector {

  private static final int INIT_SIZE_H = 230; //去向面积的最小像素 -水平方向
  private static final int INIT_SIZE_V = 460; //去向面积的最小像素 -垂直方向
  
  
  private static final int CORR = 1;
  private final BitMatrix image;
  private final int height;
  private final int width;
  private final int leftInit;
  private final int rightInit;
  private final int downInit;
  private final int upInit;

  /**
   * @throws NotFoundException if image is too small
   */
  public BlackRectangleDetector(BitMatrix image) throws NotFoundException {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    leftInit = (width - INIT_SIZE_H) >> 1;
    rightInit = (width + INIT_SIZE_H) >> 1;
    upInit = (height - INIT_SIZE_V) >> 1;
    downInit = (height + INIT_SIZE_V) >> 1;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  /**
   * @throws NotFoundException if image is too small
   */
  public BlackRectangleDetector(BitMatrix image, int initSize, int x, int y) throws NotFoundException {
    this.image = image;
    height = image.getHeight();
    width = image.getWidth();
    int halfsize = initSize >> 1;
    leftInit = x - halfsize;
    rightInit = x + halfsize;
    upInit = y - halfsize;
    downInit = y + halfsize;
    if (upInit < 0 || leftInit < 0 || downInit >= height || rightInit >= width) {
      throw NotFoundException.getNotFoundInstance();
    }
  }

  /**
   * <p>
   * Detects a candidate barcode-like rectangular region within an image. It
   * starts around the center of the image, increases the size of the candidate
   * region until it finds a white rectangular region.
   * </p>
   *
   * @return {@link ResultPoint}[] describing the corners of the rectangular
   *         region. The first and last points are opposed on the diagonal, as
   *         are the second and third. The first point will be the topmost
   *         point and the last, the bottommost. The second point will be
   *         leftmost and the third, the rightmost
   * @throws NotFoundException if no Data Matrix Code can be found
   */
  public ResultPoint[] detect() throws NotFoundException {

    int left = leftInit;
    int right = rightInit;
    int up = upInit;
    int down = downInit;
    boolean sizeExceeded = false;
    boolean aBlackPointFoundOnBorder = true;
    boolean atLeastOneBlackPointFoundOnBorder = false;

    while (aBlackPointFoundOnBorder) {

      aBlackPointFoundOnBorder = false;

      // .....
      // .   |
      // .....
      // 找到图形区域的右边位置。
      boolean rightBorderNotWhite = true;
      while (rightBorderNotWhite && right < width) {
        rightBorderNotWhite = containsBlackPoint(up, down, right, false);
        if (rightBorderNotWhite) {
          right++;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (right >= width) {
        sizeExceeded = true;
        break;
      }
      
      // .....
      // .   .
      // .___.
      // 找到图形区域的下边位置。
      boolean bottomBorderNotWhite = true;
      while (bottomBorderNotWhite && down < height) {
        bottomBorderNotWhite = containsBlackPoint(left, right, down, true);
        if (bottomBorderNotWhite) {
          down++;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (down >= height) {
        sizeExceeded = true;
        break;
      }

      // .....
      // |   .
      // .....
     // 找到图形区域的左边位置。
      boolean leftBorderNotWhite = true;
      while (leftBorderNotWhite && left >= 0) {
        leftBorderNotWhite = containsBlackPoint(up, down, left, false);
        if (leftBorderNotWhite) {
          left--;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (left < 0) {
        sizeExceeded = true;
        break;
      }

      // .___.
      // .   .
      // .....
      //找到图形区域的上边框边位置。
      boolean topBorderNotWhite = true;
      while (topBorderNotWhite && up >= 0) {
        topBorderNotWhite = containsBlackPoint(left, right, up, true);
        if (topBorderNotWhite) {
          up--;
          aBlackPointFoundOnBorder = true;
        }
      }

      if (up < 0) {
        sizeExceeded = true;
        break;
      }

      if (aBlackPointFoundOnBorder) {
        atLeastOneBlackPointFoundOnBorder = true;
      }

    }
    // sizeExceeded 四个边线均找到无异常
    // 在整个图形中 ，至少有一个黑点。图像。 
    
    if (!sizeExceeded && atLeastOneBlackPointFoundOnBorder) {
      int maxSizeHorizontal = right - left; //水平距离最大值
      int maxSizeVertical = down - up; //垂直距离， 最大值.
      int maxSize = Math.min(maxSizeHorizontal, maxSizeVertical);
      
      //找到最下方的数据点 ；坐下放为水平放置的矩形黑色。 长度必须大与高端的两倍。 高度大于20px;
      //BLACK_BLOCK_SIZE = 30, 宽度。 
      ResultPoint z = null;
      for (int i = 1; i < maxSizeHorizontal; i++) {
        z = getBlackPointOnSegment(left, down - i, left + i, down);
        if (z != null) {
          break;
        }
      }

      if (z == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      ResultPoint t = null;
      //go down right
      for (int i = 1; i < maxSize; i++) {
        t = getBlackPointOnSegment(left, up + i, left + i, up);
        if (t != null) {
          break;
        }
      }

      if (t == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      ResultPoint x = null;
      //go down left
      for (int i = 1; i < maxSize; i++) {
        x = getBlackPointOnSegment(right, up + i, right - i, up);
        if (x != null) {
          break;
        }
      }

      if (x == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      ResultPoint y = null;
      //go up left
      for (int i = 1; i < maxSize; i++) {
        y = getBlackPointOnSegment(right, down - i, right - i, down);
        if (y != null) {
          break;
        }
      }

      if (y == null) {
        throw NotFoundException.getNotFoundInstance();
      }

      float yi = y.getX();
      float zi = z.getX();
      float xi = x.getX();
      float ti = t.getX();
      float invalidX = width / 2.0f;
      if (yi < invalidX || zi > invalidX || xi < invalidX || ti > invalidX ) { //如果四个角，大多数黑点，则没有数据，直接返回。
    	  throw NotFoundException.getNotFoundInstance(); //如果四个角落，图像缺失太多。 
      }
      return new ResultPoint[]{y, z, x ,t};

    } else {
      throw NotFoundException.getNotFoundInstance();
    }
  }
  /**
   * 在一个区域的对角线附近找黑点。
   * @param aX
   * @param aY
   * @param bX
   * @param bY
   * @return
   */
  //  
  private ResultPoint getBlackPointOnSegment(float aX, float aY, float bX, float bY) {
    int dist = MathUtils.round(MathUtils.distance(aX, aY, bX, bY));
    float xStep = (bX - aX) / dist;
    float yStep = (bY - aY) / dist;

    for (int i = 0; i < dist; i++) {
      int x = MathUtils.round(aX + i * xStep);
      int y = MathUtils.round(aY + i * yStep);
      if (image.get(x, y)) {
        return new ResultPoint(x, y);
      }
    }
    return null;
  }

 

  /**
   * Determines whether a segment contains a black point
   *
   * @param a          min value of the scanned coordinate
   * @param b          max value of the scanned coordinate
   * @param fixed      value of fixed coordinate
   * @param horizontal set to true if scan must be horizontal, false if vertical
   * @return true if a black point has been found, else false.
   */
  private boolean containsBlackPoint(int a, int b, int fixed, boolean horizontal) {

    if (horizontal) {
      for (int x = a; x <= b; x++) {
        if (image.get(x, fixed)) {
          return true;
        }
      }
    } else {
      for (int y = a; y <= b; y++) {
        if (image.get(fixed, y)) {
          return true;
        }
      }
    }

    return false;
  }

}
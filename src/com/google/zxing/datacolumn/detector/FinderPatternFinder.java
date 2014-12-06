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

package com.google.zxing.datacolumn.detector;

import com.google.zxing.DecodeHintType;
import com.google.zxing.NotFoundException;
import com.google.zxing.ResultPoint;
import com.google.zxing.ResultPointCallback;
import com.google.zxing.common.BitMatrix;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * <p>This class attempts to find finder patterns in a QR Code. Finder patterns are the square
 * markers at three corners of a QR Code.</p>
 *
 * <p>This class is thread-safe but not reentrant. Each thread must allocate its own object.
 *
 * @author Sean Owen
 */
public class FinderPatternFinder {
  private static final int BLACK_BLOCK_SIZE = 15; //黑色方块， 最小边长 的最小值。
  private static final int CENTER_QUORUM = 2; // 
  protected static final int MIN_SKIP = 3; // 1 pixel/module times 3 modules/center
  protected static final int MAX_MODULES = 57; // support up to version 10 for mobile clients
  private static final int INTEGER_MATH_SHIFT = 8;

  private final BitMatrix image;
  private final List<FinderPattern> possibleCenters;
  private boolean hasSkipped;
  private final int[] crossCheckStateCount;
  private final ResultPointCallback resultPointCallback;

  /**
   * <p>Creates a finder that will search the image for three finder patterns.</p>
   *
   * @param image image to search
   */
  public FinderPatternFinder(BitMatrix image) {
    this(image, null);
  }

  public FinderPatternFinder(BitMatrix image, ResultPointCallback resultPointCallback) {
    this.image = image;
    this.possibleCenters = new ArrayList<FinderPattern>();
    this.crossCheckStateCount = new int[5];
    this.resultPointCallback = resultPointCallback;
  }

  protected final BitMatrix getImage() {
    return image;
  }

  protected final List<FinderPattern> getPossibleCenters() {
    return possibleCenters;
  }

  final FinderPatternInfo find(Map<DecodeHintType,?> hints) throws NotFoundException {
    int maxI = image.getHeight();
    int maxJ = image.getWidth();
    //检查长方形模块  
    int iSkip = MIN_SKIP; //扫描间隔， 不必每行像素都扫描。 
    
    boolean done = false;
    int[] stateCount = new int[1];
    for (int i = iSkip - 1; i < maxI && !done; i += iSkip) {
      // Get a row of black/white values
      stateCount[0] = 0;
      for (int j = 0; j < maxJ; j++) {
        if (image.get(j, i)) { //如果发现了黑色， 则 
        
          stateCount[0]++;
        } else { // White pixel
              if (foundPatternCross(stateCount)) { //检查有没有构成条件， 必须大于最小宽度。  Yes
                boolean confirmed = handlePossibleCenter(stateCount, i, j);
                if (confirmed) {
                  // Start examining every other line. Checking each line turned out to be too
                  // expensive and didn't improve performance.
                  iSkip = 2;
                  if (hasSkipped) {
                    done = haveMultiplyConfirmedCenters();
                  } else {
                    int rowSkip = findRowSkip();
                    if (rowSkip > BLACK_BLOCK_SIZE ) {
                      i += rowSkip - BLACK_BLOCK_SIZE - iSkip;
                      j = maxJ - 1;
                    }
                  }
                } 
             }
             stateCount[0] = 0;
         }
        
      }
      //循环外， 再进行一次判断. 
      if (foundPatternCross(stateCount)) {
        boolean confirmed = handlePossibleCenter(stateCount, i, maxJ);
        if (confirmed) {
          iSkip = MIN_SKIP;
          if (hasSkipped) {
            // Found a third one
            done = haveMultiplyConfirmedCenters();
          }
        }
      }
    }

    FinderPattern[] patternInfo = selectBestPatterns();
    ResultPoint.orderBestPatterns(patternInfo);

    return new FinderPatternInfo(patternInfo);
  }

  /**
   * Given a count of black pixels just seen and an end position,
   * figures the location of the center of this run.
   * 计算方块的中间点的位置。 
   */
  private static float centerFromEnd(int[] stateCount, int end) {
    return (float) end - stateCount[0] / 2.0f;
  }

  /** 检查横向的像素，是否具备构成长方形黑色像素的最低条件: pixels  >= BLACK_BLOCK_SIZE
   * @param stateCount count of black  pixels just read
   * @return true if the proportions of the counts is  long enough to build a black_block. 
   */
  protected static boolean foundPatternCross(int[] stateCount) {
    int totalModuleSize = 0;
    for (int i = 0; i < stateCount.length; i++) {
      int count = stateCount[i];
      if (count == 0) {
        return false;
      }
      totalModuleSize += count;
    }
    if (totalModuleSize < BLACK_BLOCK_SIZE) {
      return false;
    }
    return true;
  }

  private int[] getCrossCheckStateCount() {
    crossCheckStateCount[0] = 0;
    return crossCheckStateCount;
  }

  /**
   * <p>After a horizontal scan finds a potential finder pattern, this method
   * "cross-checks" by scanning down vertically through the center of the possible
   * finder pattern to see if the same proportion is detected.</p>
   * 在水平扫描并且找到特征的像素后，在中间位置进行垂直扫描。 以判断在垂直维度上是否具备相同的特征。 
   * 返回垂直方向上的中间点。 
   */
  private float crossCheckVertical(int startI, int centerJ, int originalStateCountTotal) {
    BitMatrix image = this.image;
    int maxCount = BLACK_BLOCK_SIZE;
    if(originalStateCountTotal < (BLACK_BLOCK_SIZE << 1)){
    	maxCount = (BLACK_BLOCK_SIZE << 1) + 2 ;
    }
    
    int maxI = image.getHeight();
    int[] stateCount = getCrossCheckStateCount();

    // Start counting up from center
    int i = startI;
    while (i >= 0 && image.get(centerJ, i)) {
      i--;
    }
    if (i < 0) {
      return Float.NaN;
    }
    while (i < maxI && image.get(centerJ, i) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      i++;
    }
    // If we found a finder-pattern-like section, but its size is more than 40% different than
    // the original, assume it's a false positive
    int stateCountTotal = stateCount[0] ;
    // 计算水平方向，垂直方向上的长度和宽度。 当两个长度之差 太小， 不足以2倍时。返回notfounnd。 
    if (Math.abs(stateCountTotal - originalStateCountTotal) < 2 * Math.min(stateCountTotal, originalStateCountTotal)) {
      return Float.NaN;
    }
    // 如果好到， 则返回中垂直维度的中间点。 中间值。 
    return foundPatternCross(stateCount) ? centerFromEnd(stateCount, i) : Float.NaN;
  }

  /**
   * <p>Like {@link #crossCheckVertical(int, int, int, int)}, and in fact is basically identical,
   * except it reads horizontally instead of vertically. This is used to cross-cross
   * check a vertical cross check and locate the real center of the alignment pattern.</p>
   */
  private float crossCheckHorizontal(int startJ, int centerI,  int originalStateCountTotal) {
    BitMatrix image = this.image;
    int maxJ = image.getWidth();
    int[] stateCount = getCrossCheckStateCount();
    int maxCount = BLACK_BLOCK_SIZE;
    if(originalStateCountTotal > (BLACK_BLOCK_SIZE + 2)){
    	maxCount = (BLACK_BLOCK_SIZE << 1) + 2 ;
    }
    int j = startJ;
    while (j >= 0 && image.get(j, centerI)) {
      j--;
    }
    if (j < 0) {
      return Float.NaN;
    }
    while (j < maxJ && image.get(j, centerI) && stateCount[0] <= maxCount) {
      stateCount[0]++;
      j++;
    }
    // If we found a finder-pattern-like section, but its size is significantly different than
    // the original, assume it's a false positive
    int stateCountTotal = stateCount[0];
    if (Math.abs(stateCountTotal - originalStateCountTotal) > (BLACK_BLOCK_SIZE >> 1)) {
        return Float.NaN;
    }
    return foundPatternCross(stateCount) ? centerFromEnd(stateCount, j) : Float.NaN;
  }

  /**
   * <p>This is called when a horizontal scan finds a possible alignment pattern. It will
   * cross check with a vertical scan, and if successful, will, ah, cross-cross-check
   * with another horizontal scan. This is needed primarily to locate the real horizontal
   * center of the pattern in cases of extreme skew.</p>
   *
   * <p>If that succeeds the finder pattern location is added to a list that tracks
   * the number of times each location has been nearly-matched as a finder pattern.
   * Each additional find is more evidence that the location is in fact a finder
   * pattern center
   *
   * @param stateCount reading state module counts from horizontal scan
   * @param i row where finder pattern may be found
   * @param j end of possible finder pattern in row
   * @return true if a finder pattern candidate was found this time
   */
  protected final boolean handlePossibleCenter(int[] stateCount, int i, int j) {
    int stateCountTotal = stateCount[0] ; 
    float centerJ = centerFromEnd(stateCount, j);
    float centerI = crossCheckVertical(i, (int) centerJ, stateCountTotal);
    int centerStateCountVertical = crossCheckStateCount[0];
    
    
    if (!Float.isNaN(centerI)) {
      // Re-cross check
      centerJ = crossCheckHorizontal((int) centerJ, (int) centerI, stateCountTotal);
      int centerStateCountHorizontal = crossCheckStateCount[0];
      //获取方向 
      int direction =  (centerStateCountHorizontal > centerStateCountVertical) ? FinderPattern.HORIZONTAL : FinderPattern.VERTICAL;
      if (!Float.isNaN(centerJ)) {
        float estimatedModuleSize = (float) stateCountTotal / 4.0f;
        if(stateCountTotal < (BLACK_BLOCK_SIZE << 1)){
        	estimatedModuleSize = (float) stateCountTotal / 2.0f;
        }
        boolean found = false;
        for (int index = 0; index < possibleCenters.size(); index++) {
          FinderPattern center = possibleCenters.get(index);
          // Look for about the same center and module size:
          if (center.aboutEquals(estimatedModuleSize, centerI, centerJ, direction)) {
            possibleCenters.set(index, center.combineEstimate(centerI, centerJ, estimatedModuleSize));
            found = true;
            break;
          }
        }
        if (!found) {
          FinderPattern point = new FinderPattern(centerJ, centerI, estimatedModuleSize);
          point.setDirection(direction);
          possibleCenters.add(point);
          if (resultPointCallback != null) {
            resultPointCallback.foundPossibleResultPoint(point);
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * @return number of rows we could safely skip during scanning, based on the first
   *         two finder patterns that have been located. In some cases their position will
   *         allow us to infer that the third pattern must lie below a certain point farther
   *         down in the image.
   */
  private int findRowSkip() {
    int max = possibleCenters.size();
    if (max <= 1) {
      return 0;
    }
    FinderPattern firstConfirmedCenter = null;
    for (FinderPattern center : possibleCenters) {
      if (center.getCount() >= CENTER_QUORUM) {
        if (firstConfirmedCenter == null) {
          firstConfirmedCenter = center;
        } else {
          // We have two confirmed centers
          // How far down can we skip before resuming looking for the next
          // pattern? In the worst case, only the difference between the
          // difference in the x / y coordinates of the two centers.
          // This is the case where you find top left last.
          hasSkipped = true;
          return (int) (Math.abs(firstConfirmedCenter.getX() - center.getX()) -
              Math.abs(firstConfirmedCenter.getY() - center.getY())) / 2;
        }
      }
    }
    return 0;
  }

  /**
   * @return true iff we have found at least 3 finder patterns that have been detected
   *         at least {@link #CENTER_QUORUM} times each, and, the estimated module size of the
   *         candidates is "pretty similar"
   *         
   *         
   * 判断我们是否已经得到了三个以上的黑色方块。 
   */
  private boolean haveMultiplyConfirmedCenters() {
    int confirmedCount = 0;
    float totalModuleSize = 0.0f;
    int max = possibleCenters.size();
    for (FinderPattern pattern : possibleCenters) {
      if (pattern.getCount() >= CENTER_QUORUM) {
        confirmedCount++;
        totalModuleSize += pattern.getEstimatedModuleSize();
      }
    }
    if (confirmedCount < 3) {
      return false;
    }
    // OK, we have at least 3 confirmed centers, but, it's possible that one is a "false positive"
    // and that we need to keep looking. We detect this by asking if the estimated module sizes
    // vary too much. We arbitrarily say that when the total deviation from average exceeds
    // 5% of the total module size estimates, it's too much.
    float average = totalModuleSize / (float) max;
    float totalDeviation = 0.0f;
    for (FinderPattern pattern : possibleCenters) {
      totalDeviation += Math.abs(pattern.getEstimatedModuleSize() - average);
    }
    return totalDeviation <= 0.05f * totalModuleSize;
  }

  /**
   * @return the 3 best {@link FinderPattern}s from our list of candidates. The "best" are
   *         those that have been detected at least {@link #CENTER_QUORUM} times, and whose module
   *         size differs from the average among those patterns the least
   * @throws NotFoundException if 3 such finder patterns do not exist
   * 从中找到质量最好的三个颜色方块。 判断标准： 尺寸最相似的三个方块。 
   * 
   */
  private FinderPattern[] selectBestPatterns() throws NotFoundException {

    int startSize = possibleCenters.size();
    if (startSize < 3) {
      // Couldn't find enough finder patterns
      throw NotFoundException.getNotFoundInstance();
    }
    // Filter outlier possibilities whose module size is too different
    if (startSize > 3) {
      // But we can only afford to do so if we have at least 4 possibilities to choose from
      float totalModuleSize = 0.0f;
      float square = 0.0f;
      for (FinderPattern center : possibleCenters) {
        float size = center.getEstimatedModuleSize();
        totalModuleSize += size;
        square += size * size;
      }
      float average = totalModuleSize / (float) startSize;
      float stdDev = (float) Math.sqrt(square / startSize - average * average);
      Collections.sort(possibleCenters, new FurthestFromAverageComparator(average));
      float limit = Math.max(0.2f * average, stdDev);
      for (int i = 0; i < possibleCenters.size() && possibleCenters.size() > 3; i++) {
        FinderPattern pattern = possibleCenters.get(i);
        if (Math.abs(pattern.getEstimatedModuleSize() - average) > limit) {
          possibleCenters.remove(i);
          i--;
        }
      }
    }

    if (possibleCenters.size() > 3) {
      // Throw away all but those first size candidate points we found.

      float totalModuleSize = 0.0f;
      for (FinderPattern possibleCenter : possibleCenters) {
        totalModuleSize += possibleCenter.getEstimatedModuleSize();
      }

      float average = totalModuleSize / (float) possibleCenters.size();

      Collections.sort(possibleCenters, new CenterComparator(average));

      possibleCenters.subList(3, possibleCenters.size()).clear();
    }

    return new FinderPattern[]{
        possibleCenters.get(0),
        possibleCenters.get(1),
        possibleCenters.get(2)
    };
  }

  /**
   * <p>Orders by furthest from average</p>
   */
  private static final class FurthestFromAverageComparator implements Comparator<FinderPattern>, Serializable {
    private final float average;
    private FurthestFromAverageComparator(float f) {
      average = f;
    }
    @Override
    public int compare(FinderPattern center1, FinderPattern center2) {
      float dA = Math.abs(center2.getEstimatedModuleSize() - average);
      float dB = Math.abs(center1.getEstimatedModuleSize() - average);
      return dA < dB ? -1 : dA == dB ? 0 : 1;
    }
  }

  /**
   * <p>Orders by {@link FinderPattern#getCount()}, descending.</p>
   */
  private static final class CenterComparator implements Comparator<FinderPattern>, Serializable {
    private final float average;
    private CenterComparator(float f) {
      average = f;
    }
    @Override
    public int compare(FinderPattern center1, FinderPattern center2) {
      if (center2.getCount() == center1.getCount()) {
        float dA = Math.abs(center2.getEstimatedModuleSize() - average);
        float dB = Math.abs(center1.getEstimatedModuleSize() - average);
        return dA < dB ? 1 : dA == dB ? 0 : -1;
      } else {
        return center2.getCount() - center1.getCount();
      }
    }
  }

}

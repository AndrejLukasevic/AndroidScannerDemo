package com.scanlibrary;


import android.graphics.Bitmap;
import android.os.Environment;

import org.opencv.core.Core;
import org.opencv.core.CvException;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class ScanUtils {

    // ===========================================================
    // Constants
    // ===========================================================

    static {
        System.loadLibrary("opencv_java3");
        System.loadLibrary("Scanner");
    }

    private static final String LOG_TAG = ScanUtils.class.getSimpleName();
    private static final boolean DEBUG = false;

    // ===========================================================
    // Fields
    // ===========================================================

    // ===========================================================
    // Constructors
    // ===========================================================

    // ===========================================================
    // Getters & Setters
    // ===========================================================

    public static MatOfPoint2f computePoint(int p1, int p2) {

        MatOfPoint2f pt = new MatOfPoint2f();
        pt.fromArray(new Point(p1, p2));
        return pt;
    }

    //
    public static List<Point> getPoints(Bitmap bitmap) {
        List<MatOfPoint> squares = findSquares(bitmap);
        return findBiggestSquere(squares, bitmap);
    }

    public static List<Point> findBiggestSquere(List<MatOfPoint> squares, Bitmap image) {
        double width = image.getWidth();
        double height = image.getHeight();

        double largest_area = -1;
        int largest_contour_index = 0;
        for (int i = 0; i < squares.size(); i++) {
            double a = Imgproc.contourArea(squares.get(i), false);
            if (a > largest_area) {
                largest_area = a;
                largest_contour_index = i;
            }
        }

        List<Point> points = new ArrayList<>();
        if (squares.size() > 0) {
            points = squares.get(largest_contour_index).toList();
        } else {
            points.add(new Point(0, 0));
            points.add(new Point(width, 0));
            points.add(new Point(0, height));
            points.add(new Point(width, height));
        }
        return points;
    }

    public static List<MatOfPoint> findSquares(Bitmap bitmap) {
        Mat image = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        org.opencv.android.Utils.bitmapToMat(bmp32, image);

        if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/mat.png", image);

        Mat image_proc = image.clone();
        List<MatOfPoint> squares = new ArrayList<>();
        // blur will enhance edge detection
        Mat blurred = image_proc.clone();

        if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blured.png", blurred);

        // Bluring Image
        Imgproc.medianBlur(image_proc, blurred, 9);
        Mat gray0 = new Mat(blurred.size(), CvType.CV_8U);
        Mat gray = new Mat();

        if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray0-1.png", gray0);
        if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/blured-0.png", blurred);

        List<MatOfPoint> contours = new ArrayList<>();

        // find squares in every color plane of the image
        for (int c = 0; c < 3; c++) {
            int ch[] = {c, 0};
            List<Mat> bluredList = new ArrayList<>(1);
            bluredList.add(blurred);
            List<Mat> grayList = new ArrayList<>(1);
            grayList.add(gray0);
            Core.mixChannels(bluredList, grayList, new MatOfInt(ch));
            if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray0-2-" + c + ".png", gray0);

            // try several threshold levels
            final int threshold_level = 2;

            for (int l = 0; l < threshold_level; l++) {
                // Use Canny instead of zero threshold level!
                // Canny helps to catch squares with gradient shading
                if (l == 0) {
                    Imgproc.Canny(gray0, gray, 10, 30, 3, true);
                    if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray-1-" + c + ".png", gray);
                    Imgproc.dilate(gray, gray, new Mat(), new Point(-1, -1), 1);
                    if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray-2-" + c + ".png", gray);
                } else {
                    double val = (l + 1) * 255 / threshold_level;
                    Core.compare(gray0, new Scalar(val), gray, Core.CMP_GE);
                }

                // Find contours and store them in a list
                Imgproc.findContours(gray, contours, new Mat(), Imgproc.RETR_LIST, Imgproc.CHAIN_APPROX_SIMPLE);

                if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray-4-" + c + ".png", gray);
                if (DEBUG) Highgui.imwrite(Environment.getExternalStorageDirectory().getAbsolutePath() + "/gray0-3-" + c + ".png", gray0);

                // Test contours
                MatOfPoint2f approx = new MatOfPoint2f();
                for (int i = 0; i < contours.size(); i++) {
                    // approximate contour with accuracy proportional
                    // to the contour perimeter
                    MatOfPoint2f curve = new MatOfPoint2f(contours.get(i).toArray());
                    double epsilon = Imgproc.arcLength(curve, true) * 0.02;
                    Imgproc.approxPolyDP(curve, approx, epsilon, true);

                    // Note: absolute value of an area is used because
                    // area may be positive or negative - in accordance with the
                    // contour orientation

                    double val1 = Math.abs(Imgproc.contourArea(approx));
                    boolean val2 = Imgproc.isContourConvex(new MatOfPoint(approx.toArray()));
                    if (approx.toArray().length == 4 && val1 > 1000 && val2) {
                        double maxCosine = 0;

                        for (int j = 2; j < 5; j++) {
                            Point[] approxArray = approx.toArray();
                            double cosine = Math.abs(angle(approxArray[j % 4], approxArray[j - 2], approxArray[j - 1]));
                            maxCosine = Math.max(maxCosine, cosine);
                        }

                        if (maxCosine < 0.3) {
                            squares.add(new MatOfPoint(approx.toArray()));
                        }
                    }
                }
            }
        }
        return squares;
    }

    public static Bitmap getScannedBitmap(Bitmap bitmap, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {
        Mat mbgra = bitmapToMat(bitmap);
        // init our output image
        Mat dst = scan(mbgra, x1, y1, x2, y2, x3, y3, x4, y4);
        Bitmap result = matToBitmap(dst);
        return result;
    }

    public static double angle(Point pt1, Point pt2, Point pt0) {
        double dx1 = pt1.x - pt0.x;
        double dy1 = pt1.y - pt0.y;
        double dx2 = pt2.x - pt0.x;
        double dy2 = pt2.y - pt0.y;

        return (dx1 * dx2 + dy1 * dy2) / Math.sqrt((dx1 * dx1 + dy1 * dy1) * (dx2 * dx2 + dy2 * dy2) + 1e-10);
    }

    private static Mat scan(Mat img, float x1, float y1, float x2, float y2, float x3, float y3, float x4, float y4) {

        // define the destination image size:

        float w1 = (float) Math.sqrt(Math.pow(x4 - x3, 2) + Math.pow(x4 - x3, 2));
        float w2 = (float) Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(x2 - x1, 2));
        float h1 = (float) Math.sqrt(Math.pow(y2 - y4, 2) + Math.pow(y2 - y4, 2));
        float h2 = (float) Math.sqrt(Math.pow(y1 - y3, 2) + Math.pow(y1 - y3, 2));

        float maxWidth = (w1 < w2) ? w1 : w2;
        float maxHeight = (h1 < h2) ? h1 : h2;

        Mat dst = Mat.zeros(new Size(maxWidth, maxHeight), CvType.CV_8UC3);

        // corners of destination image with the sequence [tl, tr, bl, br]
        List<Point> dst_pts = new ArrayList<>();
        List<Point> img_pts = new ArrayList<>();
        dst_pts.add(new Point(0, 0));
        dst_pts.add(new Point(maxWidth - 1, 0));
        dst_pts.add(new Point(0, maxHeight - 1));
        dst_pts.add(new Point(maxWidth - 1, maxHeight - 1));

        img_pts.add(new Point(x1, y1));
        img_pts.add(new Point(x2, y2));
        img_pts.add(new Point(x3, y3));
        img_pts.add(new Point(x4, y4));

        // get transformation matrix
        MatOfPoint2f source = new MatOfPoint2f(img_pts.get(0), img_pts.get(1), img_pts.get(2), img_pts.get(3));
        MatOfPoint2f dest = new MatOfPoint2f(dst_pts.get(0), dst_pts.get(1), dst_pts.get(2), dst_pts.get(3));
        Mat transmtx = Imgproc.getPerspectiveTransform(source, dest);
        // apply perspective transformation
        Imgproc.warpPerspective(img, dst, transmtx, dst.size());
//        warpPerspective(img, dst, transmtx, dst.size());

        return dst;
    }

    private static Mat bitmapToMat(Bitmap bitmap) {
        System.gc();
        Mat image = new Mat();
        Bitmap bmp32 = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        org.opencv.android.Utils.bitmapToMat(bmp32, image);
        return image;
    }

    private static Bitmap matToBitmap(Mat mat) {
        try {
            Bitmap bmp = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
            org.opencv.android.Utils.matToBitmap(mat, bmp);
            return bmp;
        } catch (CvException e) {
            throw new RuntimeException("Not able to convert mat to bitmap", e);
        }
    }

    public static Bitmap getGrayBitmap(Bitmap bitmap) {
        Mat mbgra = bitmapToMat(bitmap);
        Mat dst = mbgra.clone();

        Imgproc.cvtColor(mbgra, dst, Imgproc.COLOR_RGB2GRAY);

        return matToBitmap(dst);
    }

    public static Bitmap getMagicColorBitmap(Bitmap bitmap) {
        Mat mbgra = bitmapToMat(bitmap);
        Mat dst = mbgra.clone();
        // init our output image
        float alpha = 1.9f;
        float beta = -80;
        dst.convertTo(dst, -1, alpha, beta);

        return matToBitmap(dst);
    }

    // ===========================================================
    // Methods for/from SuperClass/Interfaces
    // ===========================================================

    // ===========================================================
    // Methods
    // ===========================================================

    // ===========================================================
    // Inner and Anonymous Classes
    // ===========================================================

}

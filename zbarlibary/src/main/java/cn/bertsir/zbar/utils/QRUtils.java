package cn.bertsir.zbar.utils;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorInt;
import androidx.annotation.Nullable;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.FormatException;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.GlobalHistogramBinarizer;
import com.google.zxing.qrcode.QRCodeReader;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.util.Hashtable;

import cn.bertsir.zbar.Qr.Config;
import cn.bertsir.zbar.Qr.Image;
import cn.bertsir.zbar.Qr.ImageScanner;
import cn.bertsir.zbar.Qr.Symbol;
import cn.bertsir.zbar.Qr.SymbolSet;

/**
 * Created by Bert on 2017/9/20.
 */

public class QRUtils {

    private static QRUtils instance;
    private Bitmap scanBitmap;
    private Context mContext;


    public static QRUtils getInstance() {
        if (instance == null)
            instance = new QRUtils();
        return instance;
    }



    /**
     * 识别本地二维码
     *
     * @param url
     * @return
     */
    public String decodeQRcode(String url) throws Exception {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        Bitmap qrbmp = BitmapFactory.decodeFile(url,options);
        if (qrbmp != null) {
            return decodeQRcode(qrbmp);
        } else {
            return "";
        }

    }

    public String decodeQRcode(ImageView iv) throws Exception {
        Bitmap qrbmp = ((BitmapDrawable) (iv).getDrawable()).getBitmap();
        if (qrbmp != null) {
            return decodeQRcode(qrbmp);
        } else {
            return "";
        }
    }

    public String decodeQRcode(Bitmap barcodeBmp) throws Exception {
        int width = barcodeBmp.getWidth();
        int height = barcodeBmp.getHeight();
        int[] pixels = new int[width * height];
        barcodeBmp.getPixels(pixels, 0, width, 0, 0, width, height);
        Image barcode = new Image(width, height, "RGB4");
        barcode.setData(pixels);
        ImageScanner reader = new ImageScanner();
        reader.setConfig(Symbol.NONE, Config.ENABLE, 0);
        reader.setConfig(Symbol.QRCODE, Config.ENABLE, 1);
        int result = reader.scanImage(barcode.convert("Y800"));
        String qrCodeString = null;
        if (result != 0) {
            SymbolSet syms = reader.getResults();
            for (Symbol sym : syms) {
                qrCodeString = sym.getData();
            }
        }
        return qrCodeString;
    }


    /**
     * 扫描二维码图片的方法
     * @param path
     * @return
     */
    public String decodeQRcodeByZxing(String path) {
        if (TextUtils.isEmpty(path)) {
            return null;

        }
        Hashtable<DecodeHintType, String> hints = new Hashtable();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8"); // 设置二维码内容的编码
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true; // 先获取原大小
        scanBitmap = BitmapFactory.decodeFile(path,options);
        options.inJustDecodeBounds = false;
        int sampleSize = (int) (options.outHeight / (float) 200);
        if (sampleSize <= 0)
            sampleSize = 1;
        options.inSampleSize = sampleSize;
        scanBitmap = BitmapFactory.decodeFile(path, options);
        int[] data = new int[scanBitmap.getWidth() * scanBitmap.getHeight()];
        scanBitmap.getPixels(data, 0, scanBitmap.getWidth(), 0, 0, scanBitmap.getWidth(), scanBitmap.getHeight());
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(scanBitmap.getWidth(),scanBitmap.getHeight(),data);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(rgbLuminanceSource));
        QRCodeReader reader = new QRCodeReader();
        Result result = null;
        try {
            result = reader.decode(binaryBitmap, hints);
        } catch (NotFoundException e) {

        }catch (ChecksumException e){

        }catch(FormatException e){

        }
        if(result == null){
            return "";
        }else {
            return result.getText();
        }
    }

    /**
     * 扫描二维码图片的方法
     * @return
     */
    public String decodeQRcodeByZxing(Bitmap bitmap) {
        Hashtable<DecodeHintType, String> hints = new Hashtable();
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8"); // 设置二维码内容的编码
        scanBitmap =bitmap;
        int[] data = new int[scanBitmap.getWidth() * scanBitmap.getHeight()];
        scanBitmap.getPixels(data, 0, scanBitmap.getWidth(), 0, 0, scanBitmap.getWidth(), scanBitmap.getHeight());
        RGBLuminanceSource rgbLuminanceSource = new RGBLuminanceSource(scanBitmap.getWidth(),scanBitmap.getHeight(),data);
        BinaryBitmap binaryBitmap = new BinaryBitmap(new GlobalHistogramBinarizer(rgbLuminanceSource));
        QRCodeReader reader = new QRCodeReader();
        Result result = null;
        try {
            result = reader.decode(binaryBitmap, hints);
        } catch (NotFoundException e) {

        }catch (ChecksumException e){

        }catch(FormatException e){

        }
        if(result == null){
            return "";
        }else {
            return result.getText();
        }

    }


    /**
     * 识别本地条形码
     *
     * @param url
     * @return
     */
    public String decodeBarcode(String url) throws Exception {
        Bitmap qrbmp = BitmapFactory.decodeFile(url);
        if (qrbmp != null) {
            return decodeBarcode(qrbmp);
        } else {
            return "";
        }

    }

    public String decodeBarcode(ImageView iv) throws Exception {
        Bitmap qrbmp = ((BitmapDrawable) (iv).getDrawable()).getBitmap();
        if (qrbmp != null) {
            return decodeBarcode(qrbmp);
        } else {
            return "";
        }
    }

    public String decodeBarcode(Bitmap barcodeBmp) throws Exception {
        int width = barcodeBmp.getWidth();
        int height = barcodeBmp.getHeight();
        int[] pixels = new int[width * height];
        barcodeBmp.getPixels(pixels, 0, width, 0, 0, width, height);
        Image barcode = new Image(width, height, "RGB4");
        barcode.setData(pixels);
        ImageScanner reader = new ImageScanner();
        reader.setConfig(Symbol.NONE, Config.ENABLE, 0);
        reader.setConfig(Symbol.CODE128, Config.ENABLE, 1);
        reader.setConfig(Symbol.CODE39, Config.ENABLE, 1);
        reader.setConfig(Symbol.EAN13, Config.ENABLE, 1);
        reader.setConfig(Symbol.EAN8, Config.ENABLE, 1);
        reader.setConfig(Symbol.UPCA, Config.ENABLE, 1);
        reader.setConfig(Symbol.UPCE, Config.ENABLE, 1);
        reader.setConfig(Symbol.UPCE, Config.ENABLE, 1);
        int result = reader.scanImage(barcode.convert("Y800"));
        String qrCodeString = null;
        if (result != 0) {
            SymbolSet syms = reader.getResults();
            for (Symbol sym : syms) {
                qrCodeString = sym.getData();
            }
        }
        return qrCodeString;
    }


    /**
     * 生成二维码
     *
     * @param content
     * @return
     */
    public Bitmap createQRCode(String content) {
        return createQRCode(content, 300, 300);
    }

    /**
     * 生成二维码
     *
     * @param content
     * @return
     */
    public Bitmap createQRCode(String content, int width, int height) {
        Bitmap bitmap = null;
        BitMatrix result = null;
        MultiFormatWriter multiFormatWriter = new MultiFormatWriter();
        try {
            Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);//这里调整二维码的容错率
            hints.put(EncodeHintType.MARGIN, 1);   //设置白边取值1-4，值越大白边越大
            result = multiFormatWriter.encode(new String(content.getBytes("UTF-8"), "ISO-8859-1"), BarcodeFormat
                    .QR_CODE, width, height, hints);
            int w = result.getWidth();
            int h = result.getHeight();
            int[] pixels = new int[w * h];
            for (int y = 0; y < h; y++) {
                int offset = y * w;
                for (int x = 0; x < w; x++) {
                    pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
                }
            }
            bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bitmap;
    }


    /**
     * 生成带logo的二维码
     *
     * @param content
     * @param logo
     * @return
     */
    public Bitmap createQRCodeAddLogo(String content, Bitmap logo) {
        Bitmap qrCode = createQRCode(content);
        int qrheight = qrCode.getHeight();
        int qrwidth = qrCode.getWidth();
        int waterWidth = (int) (qrwidth * 0.3);//0.3为logo占二维码大小的倍数 建议不要过大，否则二维码失效
        float scale = waterWidth / (float) logo.getWidth();
        Bitmap waterQrcode = createWaterMaskCenter(qrCode, zoomImg(logo, scale));
        return waterQrcode;
    }


    public Bitmap createQRCodeAddLogo(String content, int width, int height, Bitmap logo) {
        Bitmap qrCode = createQRCode(content, width, height);
        int qrheight = qrCode.getHeight();
        int qrwidth = qrCode.getWidth();
        int waterWidth = (int) (qrwidth * 0.3);//0.3为logo占二维码大小的倍数 建议不要过大，否则二维码失效
        float scale = waterWidth / (float) logo.getWidth();
        Bitmap waterQrcode = createWaterMaskCenter(qrCode, zoomImg(logo, scale));
        return waterQrcode;
    }

    /**
     * 创建二维码位图 (带Logo小图片)
     *
     * @param content 字符串内容
     * @param size 位图宽&高(单位:px)
     * @param logoBitmap logo图片
     * @param logoPercent logo小图片在二维码图片中的占比大小,范围[0F,1F]。超出范围->默认使用0.2F
     * @param logoRoundRadius logo小图片圆角半径
     * @param logoStrokeWidth logo小图片边框线，<=0时不显示边框线
     * @return
     */
    @Nullable
    public static Bitmap createQRCodeBitmap(String content, int size, @Nullable Bitmap logoBitmap,
                                            float logoPercent, int logoRoundRadius, int logoStrokeWidth){
        return createQRCodeBitmap(content, size, "UTF-8", "H", "0",
                Color.BLACK, Color.WHITE, null, logoBitmap, logoPercent, logoRoundRadius, logoStrokeWidth);
    }

    /**
     * 创建二维码位图 (Bitmap颜色代替黑色) 注意!!!注意!!!注意!!! 选用的Bitmap图片一定不能有白色色块,否则会识别不出来!!!
     *
     * @param content 字符串内容
     * @param size 位图宽&高(单位:px)
     * @param targetBitmap 目标图片 (如果targetBitmap != null, 黑色色块将会被该图片像素色值替代)
     * @return
     */
    @Nullable
    public static Bitmap createQRCodeBitmap(String content, int size, Bitmap targetBitmap){
        return createQRCodeBitmap(content, size, "UTF-8", "H", "4",
                Color.BLACK, Color.WHITE, targetBitmap, null, 0F,0,0);
    }

    /**
     * 创建二维码位图 (支持自定义配置和自定义样式)
     *
     * @param content 字符串内容
     * @param size 位图宽&高(单位:px)
     * @param character_set 字符集/字符转码格式 (支持格式:{@link com.google.zxing.common.CharacterSetECI })。传null时,zxing源码默认使用 "ISO-8859-1"
     * @param error_correction 容错级别 (支持级别:{@link com.google.zxing.qrcode.decoder.ErrorCorrectionLevel })。传null时,zxing源码默认使用 "L"
     * @param margin 空白边距 (可修改,要求:整型且>=0), 传null时,zxing源码默认使用"4"。
     * @param color_black 黑色色块的自定义颜色值
     * @param color_white 白色色块的自定义颜色值
     * @param targetBitmap 目标图片 (如果targetBitmap != null, 黑色色块将会被该图片像素色值替代)
     * @param logoBitmap logo小图片
     * @param logoPercent logo小图片在二维码图片中的占比大小,范围[0F,1F],超出范围->默认使用0.2F。
     * @param logoRoundRadius   logo小图片圆角半径
     * @param logoStrokeWidth logo小图片边框线，<=0时不显示边框线
     * @return
     */
    @Nullable
    public static Bitmap createQRCodeBitmap(@Nullable String content, int size, @Nullable String character_set,
                                            @Nullable String error_correction, @Nullable String margin,
                                            @ColorInt int color_black, @ColorInt int color_white,
                                            @Nullable Bitmap targetBitmap, @Nullable Bitmap logoBitmap,
                                            float logoPercent, int logoRoundRadius, int logoStrokeWidth){

        /** 1.参数合法性判断 */
        if(TextUtils.isEmpty(content)){ // 字符串内容判空
            return null;
        }

        if(size <= 0){ // 宽&高都需要>0
            return null;
        }

        try {
            /** 2.设置二维码相关配置,生成BitMatrix(位矩阵)对象 */
            Hashtable<EncodeHintType, String> hints = new Hashtable<>();

            if(!TextUtils.isEmpty(character_set)) {
                hints.put(EncodeHintType.CHARACTER_SET, character_set); // 字符转码格式设置
            }

            if(!TextUtils.isEmpty(error_correction)){
                hints.put(EncodeHintType.ERROR_CORRECTION, error_correction); // 容错级别设置
            }

            if(!TextUtils.isEmpty(margin)){
                hints.put(EncodeHintType.MARGIN, margin); // 空白边距设置
            }
            BitMatrix bitMatrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, size, size, hints);

            /** 3.根据BitMatrix(位矩阵)对象为数组元素赋颜色值 */
            if(targetBitmap != null){
                targetBitmap = Bitmap.createScaledBitmap(targetBitmap, size, size, false);
            }
            int[] pixels = new int[size * size];
            for(int y = 0; y < size; y++){
                for(int x = 0; x < size; x++){
                    if(bitMatrix.get(x, y)){ // 黑色色块像素设置
                        if(targetBitmap != null) {
                            pixels[y * size + x] = targetBitmap.getPixel(x, y);
                        } else {
                            pixels[y * size + x] = color_black;
                        }
                    } else { // 白色色块像素设置
                        pixels[y * size + x] = color_white;
                    }
                }
            }

            /** 4.创建Bitmap对象,根据像素数组设置Bitmap每个像素点的颜色值,之后返回Bitmap对象 */
            Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size);

            /** 5.为二维码添加logo小图标 */
            if(logoBitmap != null){
                return addLogo(bitmap, logoBitmap, logoPercent, logoRoundRadius, logoStrokeWidth);
            }

            return bitmap;
        } catch (WriterException e) {
            e.printStackTrace();
        }

        return null;
    }

    /**
     * 向一张图片中间添加logo小图片(图片合成)
     *
     * @param srcBitmap 原图片
     * @param logoBitmap logo图片
     * @param logoPercent 百分比 (用于调整logo图片在原图片中的显示大小, 取值范围[0,1], 传值不合法时使用0.2F)
     *                    原图片是二维码时,建议使用0.2F,百分比过大可能导致二维码扫描失败。
     * @param logoRoundRadius   logo小图片圆角半径
     * @param logoStrokeWidth logo小图片边框线，<=0时不显示边框线
     * @return
     */
    @Nullable
    private static Bitmap addLogo(@Nullable Bitmap srcBitmap, @Nullable Bitmap logoBitmap,
                                  float logoPercent, int logoRoundRadius, int logoStrokeWidth){

        /** 1. 参数合法性判断 */
        if(srcBitmap == null){
            return null;
        }

        if(logoBitmap == null){
            return srcBitmap;
        }

        if(logoPercent < 0F || logoPercent > 1F){
            logoPercent = 0.2F;
        }

        if(logoRoundRadius < 0){
            logoRoundRadius = 0;
        }

        if(logoStrokeWidth < 0){
            logoStrokeWidth = 0;
        }

        Paint paint = new Paint();
        paint.setDither(true);
        paint.setAntiAlias(true);
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.FILL);

        /** 2. 获取原图片和Logo图片各自的宽、高值 */
        int srcWidth = srcBitmap.getWidth();
        int srcHeight = srcBitmap.getHeight();
        int logoWidth = logoBitmap.getWidth();
        int logoHeight = logoBitmap.getHeight();

        /** 3. 计算画布缩放的宽高比 */
        float scaleWidth = srcWidth * logoPercent / logoWidth;
        float scaleHeight = srcHeight * logoPercent / logoHeight;

        /** 4. 使用Canvas绘制,合成图片 */
        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        canvas.drawBitmap(srcBitmap, 0, 0, null);
        canvas.scale(scaleWidth, scaleHeight, srcWidth / 2, srcHeight / 2);

        //绘制矩形
        RectF rectF = new RectF();
        rectF.left = srcWidth / 2 - logoWidth / 2 - logoStrokeWidth;
        rectF.top = srcHeight / 2 - logoHeight / 2 - logoStrokeWidth;
        rectF.right = srcWidth / 2 + logoWidth / 2 + logoStrokeWidth;
        rectF.bottom = srcHeight / 2 + logoHeight / 2 + logoStrokeWidth;

        paint.setShadowLayer(15,0,10,0x88000000);
        canvas.drawRoundRect(rectF, logoRoundRadius + logoStrokeWidth / 2, logoRoundRadius + logoStrokeWidth / 2, paint);
        paint.setShadowLayer(0,0,0,Color.TRANSPARENT);

        canvas.drawBitmap(logoBitmap, srcWidth / 2 - logoWidth / 2, srcHeight / 2 - logoHeight / 2, paint);

        return bitmap;
    }

    private static void clipTopLeft(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect block = new Rect(0, 0, offset, offset);
        canvas.drawRect(block, paint);
    }

    private static void clipTopRight(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect block = new Rect(width - offset, 0, width, offset);
        canvas.drawRect(block, paint);
    }

    private static void clipBottomLeft(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect block = new Rect(0, height - offset, offset, height);
        canvas.drawRect(block, paint);
    }

    private static void clipBottomRight(final Canvas canvas, final Paint paint, int offset, int width, int height) {
        final Rect block = new Rect(width - offset, height - offset, width, height);
        canvas.drawRect(block, paint);
    }

    /**
     * 生成条形码
     *
     * @param context
     * @param contents
     * @param desiredWidth
     * @param desiredHeight
     * @return
     */
    @Deprecated
    public Bitmap createBarcode(Context context, String contents, int desiredWidth, int desiredHeight) {
        if (TextUtils.isEmpty(contents)) {
            throw new NullPointerException("contents not be null");
        }
        if (desiredWidth == 0 || desiredHeight == 0) {
            throw new NullPointerException("desiredWidth or desiredHeight not be null");
        }
        Bitmap resultBitmap;
        /**
         * 条形码的编码类型
         */
        BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;

        resultBitmap = encodeAsBitmap(contents, barcodeFormat,
                desiredWidth, desiredHeight);
        return resultBitmap;
    }

    /**
     * 生成条形码
     *
     * @param context
     * @param contents
     * @param desiredWidth
     * @param desiredHeight
     * @return
     */
    public Bitmap createBarCodeWithText(Context context, String contents, int desiredWidth,
                                        int desiredHeight) {
        return createBarCodeWithText(context, contents, desiredWidth, desiredHeight, null);
    }

    public Bitmap createBarCodeWithText(Context context, String contents, int desiredWidth,
                                        int desiredHeight, TextViewConfig config) {
        if (TextUtils.isEmpty(contents)) {
            throw new NullPointerException("contents not be null");
        }
        if (desiredWidth == 0 || desiredHeight == 0) {
            throw new NullPointerException("desiredWidth or desiredHeight not be null");
        }
        Bitmap resultBitmap;

        /**
         * 条形码的编码类型
         */
        BarcodeFormat barcodeFormat = BarcodeFormat.CODE_128;

        Bitmap barcodeBitmap = encodeAsBitmap(contents, barcodeFormat,
                desiredWidth, desiredHeight);

        Bitmap codeBitmap = createCodeBitmap(contents, barcodeBitmap.getWidth(),
                barcodeBitmap.getHeight(), context, config);

        resultBitmap = mixtureBitmap(barcodeBitmap, codeBitmap, new PointF(
                0, desiredHeight));
        return resultBitmap;
    }

    private Bitmap encodeAsBitmap(String contents, BarcodeFormat format, int desiredWidth, int desiredHeight) {
        final int WHITE = 0xFFFFFFFF;
        final int BLACK = 0xFF000000;

        MultiFormatWriter writer = new MultiFormatWriter();
        BitMatrix result = null;
        try {
            result = writer.encode(contents, format, desiredWidth,
                    desiredHeight, null);
        } catch (WriterException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        // All are 0, or black, by default
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;

    }


    private Bitmap createCodeBitmap(String contents, int width, int height, Context context,
                                    TextViewConfig config) {
        if (config == null) {
            config = new TextViewConfig();
        }
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        tv.setLayoutParams(layoutParams);
        tv.setText(contents);
        tv.setTextSize(config.size == 0 ? tv.getTextSize() : config.size);
        tv.setHeight(height);
        tv.setGravity(config.gravity);
        tv.setMaxLines(config.maxLines);
        tv.setWidth(width);
        tv.setDrawingCacheEnabled(true);
        tv.setTextColor(config.color);
        tv.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        tv.layout(0, 0, tv.getMeasuredWidth(), tv.getMeasuredHeight());

        tv.buildDrawingCache();
        return tv.getDrawingCache();
    }

    public static class TextViewConfig {

        private int gravity = Gravity.CENTER;
        private int maxLines = 1;
        private int color = Color.BLACK;
        private float size;

        public TextViewConfig() {
        }

        public void setGravity(int gravity) {
            this.gravity = gravity;
        }

        public void setMaxLines(int maxLines) {
            this.maxLines = maxLines;
        }

        public void setColor(int color) {
            this.color = color;
        }

        public void setSize(float size) {
            this.size = size;
        }
    }

    /**
     * 将两个Bitmap合并成一个
     *
     * @param first
     * @param second
     * @param fromPoint 第二个Bitmap开始绘制的起始位置（相对于第一个Bitmap）
     * @return
     */
    private Bitmap mixtureBitmap(Bitmap first, Bitmap second, PointF fromPoint) {
        if (first == null || second == null || fromPoint == null) {
            return null;
        }

        int width = Math.max(first.getWidth(), second.getWidth());
        Bitmap newBitmap = Bitmap.createBitmap(
                width,
                first.getHeight() + second.getHeight(), Bitmap.Config.ARGB_4444);
        Canvas cv = new Canvas(newBitmap);
        cv.drawBitmap(first, 0, 0, null);
        cv.drawBitmap(second, fromPoint.x, fromPoint.y, null);
        cv.save();
        cv.restore();

        return newBitmap;
    }

    /**
     * 设置水印图片到中间
     *
     * @param src
     * @param watermark
     * @return
     */
    private Bitmap createWaterMaskCenter(Bitmap src, Bitmap watermark) {
        return createWaterMaskBitmap(src, watermark,
                (src.getWidth() - watermark.getWidth()) / 2,
                (src.getHeight() - watermark.getHeight()) / 2);
    }

    private Bitmap createWaterMaskBitmap(Bitmap src, Bitmap watermark, int paddingLeft, int paddingTop) {
        if (src == null) {
            return null;
        }
        int width = src.getWidth();
        int height = src.getHeight();
        Bitmap newb = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);// 创建一个新的和SRC长度宽度一样的位图
        Canvas canvas = new Canvas(newb);
        canvas.drawBitmap(src, 0, 0, null);
        canvas.drawBitmap(watermark, paddingLeft, paddingTop, null);
        canvas.save();
        canvas.restore();
        return newb;
    }

    /**
     * 缩放Bitmap
     *
     * @param bm
     * @param f
     * @return
     */
    private Bitmap zoomImg(Bitmap bm, float f) {

        int width = bm.getWidth();
        int height = bm.getHeight();

        float scaleWidth = f;
        float scaleHeight = f;

        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);

        Bitmap newbm = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, true);
        return newbm;
    }


    public boolean isMIUI() {
        String manufacturer = Build.MANUFACTURER;
        if ("xiaomi".equalsIgnoreCase(manufacturer)) {
            return true;
        }
        return false;
    }

    /**
     * Return the width of screen, in pixel.
     *
     * @return the width of screen, in pixel
     */
    public int getScreenWidth(Context mContext) {
        WindowManager wm = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //noinspection ConstantConditions
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            //noinspection ConstantConditions
            wm.getDefaultDisplay().getSize(point);
        }
        return point.x;
    }

    /**
     * Return the height of screen, in pixel.
     *
     * @return the height of screen, in pixel
     */
    public int getScreenHeight(Context mContext) {
        WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        Point point = new Point();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            //noinspection ConstantConditions
            wm.getDefaultDisplay().getRealSize(point);
        } else {
            //noinspection ConstantConditions
            wm.getDefaultDisplay().getSize(point);
        }
        return point.y;
    }

    /**
     * 返回当前屏幕是否为竖屏。
     * @param context
     * @return 当且仅当当前屏幕为竖屏时返回true,否则返回false。
     */
    public  boolean isScreenOriatationPortrait(Context context) {
        return context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
    }

    public float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }


}

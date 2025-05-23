/*
 * aTalk, ohos VoIP and Instant Messaging client
 * Copyright 2024 Eng Chong Meng
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.atalk.ohos.util;

import ohos.agp.components.element.PixelMapElement;
import ohos.agp.render.Canvas;
import ohos.agp.render.Paint;
import ohos.agp.render.Texture;
import ohos.agp.utils.Color;
import ohos.agp.utils.Rect;
import ohos.agp.utils.RectFloat;
import ohos.app.Context;
import ohos.global.resource.NotExistException;
import ohos.global.resource.Resource;
import ohos.media.image.ImageSource;
import ohos.media.image.PixelMap;
import ohos.media.image.common.PixelFormat;
import ohos.media.image.common.Size;
import ohos.utils.net.Uri;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Class containing utility methods for Android's Displayable and Bitmap
 *
 * @author Eng Chong Meng
 */
public class AppImageUtil {
    /**
     * Converts given array of bytes to {@link PixelMap}
     *
     * @param imageBlob array of bytes with raw image data
     *
     * @return {@link PixelMap} created from <code>imageBlob</code>
     */
    static public PixelMap pixelMapFromBytes(byte[] imageBlob) {
        if (imageBlob != null) {
            ImageSource.SourceOptions sourceOptions = new ImageSource.SourceOptions();
            sourceOptions.formatHint = "image/png";
            ImageSource imageSource = ImageSource.create(imageBlob, sourceOptions);

//            ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
//            // decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;
//            decodingOpts.desiredSize = new Size(0, 0);
//            decodingOpts.rotateDegrees = 0;
//            return imageSource.createPixelmap(decodingOpts);
            return imageSource.createPixelmap(null);
        }
        return null;
    }

    /**
     * Creates a <code>Bitmap</code> from the given image byte array and scales it to the given
     * <code>width</code> and <code>height</code>.
     *
     * @param imageBytes the raw image data
     * @param reqWidth the width to which to scale the image
     * @param reqHeight the height to which to scale the image
     *
     * @return the newly created <code>PixelMap</code>
     */
    static public PixelMap scaledPixelMapFromBytes(byte[] imageBytes, int reqWidth, int reqHeight) {
        if (imageBytes != null) {
            ImageSource.SourceOptions sourceOptions = new ImageSource.SourceOptions();
            sourceOptions.formatHint = "image/png";
            ImageSource imageSource = ImageSource.create(imageBytes, sourceOptions);

            ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
            decodingOpts.desiredSize = new Size(reqWidth, reqHeight);
            decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;
            decodingOpts.rotateDegrees = 0;
            return imageSource.createPixelmap(decodingOpts);
        }
        return null;
    }

    static public PixelMap scaledPixelMap(PixelMap pixelMap, int reqWidth, int reqHeight) {
        if (pixelMap != null) {
            final Paint paint = new Paint();
            paint.setColor(new Color(0xff424242));
            paint.setAntiAlias(true);

            Texture texture = new Texture(pixelMap);
            Canvas canvas = new Canvas(texture);

            final Rect rect = new Rect(0, 0, reqWidth, reqHeight);
            canvas.drawRect(rect, paint);
            return texture.getPixelMap();
        }
        return null;
    }

    /**
     * Decodes <code>Bitmap</code> identified by given <code>resId</code> scaled to requested width and height.
     *
     * @param ctx the <code>Context</code> object.
     * @param resId bitmap resource id.
     * @param reqWidth requested width.
     * @param reqHeight requested height.
     *
     * @return <code>Bitmap</code> identified by given <code>resId</code> scaled to requested width and height.
     */
    public static PixelMap scaledPixelMapFromResource(Context ctx, int resId, int reqWidth, int reqHeight) {
        ImageSource.SourceOptions srcOptions = new ImageSource.SourceOptions();
        srcOptions.formatHint = "image/png";
        try {
            Resource resource = ctx.getResourceManager().getResource(resId);
            ImageSource source = ImageSource.create(resource, srcOptions);
            ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
            decodingOpts.desiredSize = new Size(reqWidth, reqHeight);
            decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;
            decodingOpts.rotateDegrees = 0;

            return source.createPixelmap(decodingOpts);
        } catch (IOException | NotExistException e) {
            LogUtil.error("AppImageUtil", "read error");
        }
        return null;
    }

    /**
     * Reads <code>Bitmap</code> from given <code>uri</code> using <code>ContentResolver</code>. Output image is scaled
     * to given <code>reqWidth</code> and <code>reqHeight</code>. Output size is not guaranteed to match exact
     * given values, because only powers of 2 are used as scale factor. Algorithm tries to scale image down
     * as long as the output size stays larger than requested value.
     *
     * @param uri the <code>Uri</code> that points to the image.
     * @param reqWidth requested width.
     * @param reqHeight requested height.
     *
     * @return <code>Bitmap</code> from given <code>uri</code> retrieved using <code>ContentResolver</code>
     * and down sampled as close as possible to match requested width and height.
     */
    public static PixelMap scaledPixelMapFromContentUri(Uri uri, int reqWidth, int reqHeight) {
        try {
            File file = new File(uri.getDecodedPath());
            ImageSource.SourceOptions srcOptions = new ImageSource.SourceOptions();
            ImageSource imageSrc = ImageSource.create(file, srcOptions);

            ImageSource.DecodingOptions decodingOpts = new ImageSource.DecodingOptions();
            decodingOpts.desiredSize = new Size(reqWidth, reqHeight);
            decodingOpts.desiredPixelFormat = PixelFormat.ARGB_8888;
            decodingOpts.rotateDegrees = 0;

            // Decode pixelMap with image Size set
            // decOptions.sampleSize = calculateInSampleSize(imageSrc.getImageInfo(), reqWidth, reqHeight);
            return imageSrc.createPixelmap(decodingOpts);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Encodes given <code>Bitmap</code> to array of bytes using given compression <code>quality</code> in PNG format.
     *
     * @param pixelMap the bitmap to encode.
     *
     * @return raw bitmap data PNG encoded using given <code>quality</code>.
     */
    public static byte[] convertPixelMapToBytes(PixelMap pixelMap) {
        Size size = pixelMap.getImageInfo().size;
        ByteBuffer byteBuffer = ByteBuffer.allocate(size.width * size.height * 4);
        pixelMap.readPixels(byteBuffer);
        byteBuffer.flip();
        return byteBuffer.array();
    }

    /**
     * Loads an image from a given image identifier and return bytes of the image.
     *
     * @param resId The identifier of the image i.e. ResourceTable.Media_
     *
     * @return The image bytes for the given identifier.
     */
    public static byte[] getImageBytes(Context ctx, int resId) {
        PixelMap pixelMap = getPixelMap(ctx, resId);
        return (pixelMap != null) ? convertPixelMapToBytes(pixelMap) : new byte[0];
    }

    public static PixelMap getPixelMap(Context ctx, int resId) {
        try {
            Resource resource = ctx.getResourceManager().getResource(resId);
            PixelMapElement pixelMapElement = new PixelMapElement(resource);
            return pixelMapElement.getPixelMap();
        } catch (IOException | NotExistException e) {
            LogUtil.error("AppImageUtil", e.getMessage());
        }
        return null;
    }

    /**
     * Creates a <code>PixelMap</code> with rounded corners.
     *
     * @param pixelMap the bitmap that will have it's corners rounded.
     * @param factor factor used to calculate corners radius based on width and height of the image.
     *
     * @return a <code>PixelMap</code> with rounded corners created from given <code>pixelmap</code>.
     */
    public static PixelMap getRoundedCornerPixelMap(PixelMap pixelMap, float factor) {
        final Paint paint = new Paint();
        paint.setColor(new Color(0xff424242));
        paint.setAntiAlias(true);
        // paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Size size = pixelMap.getImageInfo().size;
        final Rect rect = new Rect(0, 0, size.width, size.height);
        final RectFloat rectF = new RectFloat(rect);

        Texture texture = new Texture(pixelMap);
        Canvas canvas = new Canvas(texture);

        canvas.drawRoundRect(rectF, factor, factor, paint);

//        canvas.drawARGB(0, 0, 0, 0);
//        PixelMap output = PixelMap.createBitmap(size.width, size.height, PixelFormat.ARGB_8888);
//        canvas.drawBitmap(pixelmap, rect, rect, paint);
        return texture.getPixelMap();
    }

    /**
     * Creates <code>PixelMap</code> with rounded corners from raw image data.
     *
     * @param rawData raw bitmap data
     *
     * @return <code>PixelMap</code> with rounded corners from raw image data.
     */
    public static PixelMap getRoundedCornerPixelMapFromBytes(byte[] rawData) {
        PixelMap pixelMap = pixelMapFromBytes(rawData);
        if (pixelMap == null)
            return null;
        return getRoundedCornerPixelMap(pixelMap, 0.10f);
    }

    /**
     * Creates a rounded corner scaled image.
     *
     * @param imageBytes The bytes of the image to be scaled.
     * @param width The maximum width of the scaled image.
     * @param height The maximum height of the scaled image.
     *
     * @return The rounded corner scaled image.
     */
    public static PixelMap getScaledRoundedIcon(byte[] imageBytes, int width, int height) {
        return getRoundedCornerPixelMap(scaledPixelMapFromBytes(imageBytes, width, height), 0.1f);
    }

    /**
     * Creates a circular <code>PixelMap</code>.
     *
     * @param pixelmap the bitmap that will have circular mack.
     *
     * @return a circular <code>PixelMap</code> created from given <code>pixelmap</code>.
     */
    public static PixelMap getCircularPixelMap(PixelMap pixelmap) {
        final Paint paint = new Paint();
        paint.setColor(new Color(0xff424242));
        paint.setAntiAlias(true);
        // paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));

        Size size = pixelmap.getImageInfo().size;
        final float rX = (float) size.width / 2.0f;
        final float rY = (float) size.height / 2.0f;

        Texture texture = new Texture(pixelmap);
        Canvas canvas = new Canvas(texture);
        canvas.drawCircle(rX, rY, rX, paint);

//        canvas.drawARGB(0, 0, 0, 0);
//        PixelMap output = PixelMap.createBitmap(size.width, size.height, PixelFormat.ARGB_8888);
//        canvas.drawBitmap(pixelmap, rect, rect, paint);
        return texture.getPixelMap();
    }

    /**
     * Creates a circular <code>PixelMap</code> from raw image data.
     *
     * @param rawData raw bitmap data
     *
     * @return a circular <code>PixelMap</code> from raw image data.
     */
    public static PixelMap getCircularPixelMapFromBytes(byte[] rawData) {
        PixelMap pixelMap = pixelMapFromBytes(rawData);
        if (pixelMap == null)
            return null;
        return getCircularPixelMap(pixelMap);
    }

    /**
     * Creates a circular scaled image.
     *
     * @param imageBytes The bytes of the image to be scaled.
     * @param width The maximum width of the scaled image.
     * @param height The maximum height of the scaled image.
     *
     * @return The circular scaled image.
     */
    public static PixelMap getScaledCircularIcon(byte[] imageBytes, int width, int height) {
        return getCircularPixelMap(scaledPixelMapFromBytes(imageBytes, width, height));
    }

//    /**
//     * Calculates <code>options.inSampleSize</code> for requested width and height.
//     *
//     * @param imgInfo the <code>ImageInfo</code> object that contains image <code>size</code>.
//     * @param reqWidth requested width.
//     * @param reqHeight requested height.
//     *
//     * @return <code>options.inSampleSize</code> for requested width and height.
//     */
//    public static int calculateInSampleSize(ImageInfo imgInfo, int reqWidth, int reqHeight) {
//        // Raw height and width of image
//        Size size = imgInfo.size;
//        final int height = size.height;
//        final int width = size.width;
//        int inSampleSize = 1;
//
//        if (height > reqHeight || width > reqWidth) {
//            final int halfHeight = height / 2;
//            final int halfWidth = width / 2;
//
//            // Calculate the largest inSampleSize value that is a power of 2
//            // and keeps both height and width larger than the requested height
//            // and width.
//            while ((halfHeight / inSampleSize) > reqHeight
//                    && (halfWidth / inSampleSize) > reqWidth) {
//                inSampleSize *= 2;
//            }
//        }
//        return inSampleSize;
//    }
}

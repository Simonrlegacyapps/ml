package com.example.demomlkit.utils

import android.content.Context
import android.graphics.*
import android.media.Image.Plane
import android.os.Build.VERSION_CODES
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.common.primitives.Floats
import com.google.mlkit.vision.common.PointF3D
import com.google.mlkit.vision.pose.PoseLandmark
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.atan2

fun getAngle(firstPoint: PoseLandmark?, midPoint: PoseLandmark?, lastPoint: PoseLandmark?): Double {
    var result : Double = if (firstPoint != null && midPoint != null && lastPoint != null) {
        Math.toDegrees(
            atan2(
                lastPoint.position.y - midPoint.position.y,
                lastPoint.position.x - midPoint.position.x
            ) - atan2(
                firstPoint.position.y - midPoint.position.y,
                firstPoint.position.x - midPoint.position.x
            ).toDouble()
        )
    } else 0.0

    result = abs(result) // Angle should never be negative
    if (result > 180) result = 360.0 - result
    return result
}

fun toast(ctx : Context, msg :String) {
    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show()
}

/** Converts a YUV_420_888 image from CameraX API to a bitmap.  */
@RequiresApi(VERSION_CODES.LOLLIPOP)
@ExperimentalGetImage
fun getBitmap(image: ImageProxy): Bitmap? {
    val frameMetadata: FrameMetadata = FrameMetadata.Builder()
        .setWidth(image.width)
        .setHeight(image.height)
        .setRotation(image.imageInfo.rotationDegrees)
        .build()
    val nv21Buffer: ByteBuffer? = yuv420ThreePlanesToNV21(
        image.image?.planes, image.width, image.height
    )

    return getBitmap(nv21Buffer, frameMetadata)
}

/** Converts NV21 format byte buffer to bitmap.  */
fun getBitmap(data: ByteBuffer?, metadata: FrameMetadata): Bitmap? {
    data?.rewind()
    val imageInBuffer = data?.limit()?.let { ByteArray(it) }
    if (imageInBuffer != null)
        data?.get(imageInBuffer, 0, imageInBuffer.size)

    try {
        val image = YuvImage(
            imageInBuffer, ImageFormat.NV21, metadata.width, metadata.height, null
        )
        val stream = ByteArrayOutputStream()
        image.compressToJpeg(Rect(0, 0, metadata.width, metadata.height), 80, stream)
        val bmp = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size())
        stream.close()
        return rotateBitmap(
            bmp,
            metadata.rotation,
            false,
            false
        )
    } catch (e: Exception) {
        Log.e("VisionProcessorBase", "Error: " + e.message)
    }
    return null
}

/** Rotates a bitmap if it is converted from a bytebuffer.  */
private fun rotateBitmap(
    bitmap: Bitmap, rotationDegrees: Int, flipX: Boolean, flipY: Boolean
): Bitmap? {
    val matrix = Matrix()

    // Rotate the image back to straight.
    matrix.postRotate(rotationDegrees.toFloat())

    // Mirror the image along the X or Y axis.
    matrix.postScale(if (flipX) -1.0f else 1.0f, if (flipY) -1.0f else 1.0f)
    val rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

    // Recycle the old bitmap if it has changed.
    if (rotatedBitmap != bitmap) bitmap.recycle()
    return rotatedBitmap
}

private fun yuv420ThreePlanesToNV21(
    yuv420888planes: Array<Plane>?, width: Int, height: Int
): ByteBuffer? {
    val imageSize = width * height

    val out = ByteArray(imageSize + 2 * (imageSize / 4))
    if (areUVPlanesNV21(yuv420888planes, width, height)) {
        // Copy the Y values.
        yuv420888planes?.get(0)?.buffer?.get(out, 0, imageSize)
        val uBuffer = yuv420888planes?.get(1)?.buffer
        val vBuffer = yuv420888planes?.get(2)?.buffer
        // Get the first V value from the V buffer, since the U buffer does not contain it.
        vBuffer?.get(out, imageSize, 1)
        // Copy the first U value and the remaining VU values from the U buffer.
        uBuffer?.get(out, imageSize + 1, 2 * imageSize / 4 - 1)
    } else {
        // Fallback to copying the UV values one by one, which is slower but also works.
        // Unpack Y.
        unpackPlane(
            yuv420888planes?.get(0),
            width,
            height,
            out,
            0,
            1
        )
        // Unpack U.
        unpackPlane(
            yuv420888planes?.get(1),
            width,
            height,
            out,
            imageSize + 1,
            2
        )
        // Unpack V.
        unpackPlane(
            yuv420888planes?.get(2),
            width,
            height,
            out,
            imageSize,
            2
        )
    }
    return ByteBuffer.wrap(out)
}

/**
 * Unpack an image plane into a byte array.
 * The input plane data will be copied in 'out', starting at 'offset' and every pixel will be
 * spaced by 'pixelStride'. Note that there is no row padding on the output.
 */
private fun unpackPlane(
    plane: Plane?, width: Int, height: Int, out: ByteArray, offset: Int, pixelStride: Int
) {
    val buffer = plane?.buffer
    buffer?.rewind()

    // Compute the size of the current plane.
    // We assume that it has the aspect ratio as the original image.
    //val numRow = (buffer.limit() + plane.rowStride - 1) / plane.rowStride
    val numRow = ((buffer?.limit())?.plus(plane.rowStride)?.minus(1))?.div(plane.rowStride)
    if (numRow == 0 || numRow == null) return

    val scaleFactor = height / numRow
    val numCol = width / scaleFactor

    // Extract the data in the output buffer.
    var outputPos = offset
    var rowStart = 0
    for (row in 0 until numRow) {
        var inputPos = rowStart
        for (col in 0 until numCol) {
            out[outputPos] = buffer[inputPos]
            outputPos += pixelStride
            inputPos += plane.pixelStride
        }
        rowStart += plane.rowStride
    }
}

/** Checks if the UV plane buffers of a YUV_420_888 image are in the NV21 format.  */
private fun areUVPlanesNV21(planes: Array<Plane>?, width: Int, height: Int): Boolean {
    val imageSize = width * height
    val uBuffer = planes?.get(1)?.buffer
    val vBuffer = planes?.get(2)?.buffer

    // Backup buffer properties.
    val vBufferPosition = vBuffer?.position()
    val uBufferLimit = uBuffer?.limit()

    // Advance the V buffer by 1 byte, since the U buffer will not contain the first V value.
    vBufferPosition?.plus(1)?.let { vBuffer.position(it) }
    // Chop off the last byte of the U buffer, since the V buffer will not contain the last U value.
    if (uBufferLimit != null)
        uBuffer.limit(uBufferLimit - 1)

    // Check that the buffers are equal and have the expected number of elements.
    val areNV21 = vBuffer?.remaining() == 2 * imageSize / 4 - 2 && vBuffer.compareTo(uBuffer) == 0

    // Restore buffers to their initial state.
    if (vBufferPosition != null)
        vBuffer.position(vBufferPosition)

    if (uBufferLimit != null)
        uBuffer.limit(uBufferLimit)

    return areNV21
}

fun average(a: PointF3D, b: PointF3D) = PointF3D.from((a.x + b.x) * 0.5f, (a.y + b.y) * 0.5f, (a.z + b.z) * 0.5f)

fun subtractAll(p: PointF3D, pointsList: MutableList<PointF3D>) {
    val iterator = pointsList.listIterator()
    while (iterator.hasNext()) {
        iterator.set(
            subtract(
                p,
                iterator.next()
            )
        )
    }
}

fun subtract(b: PointF3D, a: PointF3D) = PointF3D.from(a.x - b.x, a.y - b.y, a.z - b.z)

fun multiplyAll(pointsList: MutableList<PointF3D>, multiple: Float) {
    val iterator = pointsList.listIterator()
    while (iterator.hasNext()) {
        iterator.set(
           multiply(
                iterator.next(),
                multiple
            )
        )
    }
}

fun multiply(a: PointF3D, multiple: Float) = PointF3D.from(a.x * multiple, a.y * multiple, a.z * multiple)

fun l2Norm2D(point: PointF3D) = Math.hypot(point.x.toDouble(), point.y.toDouble()).toFloat()

fun multiplyAll(pointsList: MutableList<PointF3D>, multiple: PointF3D) {
    val iterator = pointsList.listIterator()
    while (iterator.hasNext()) {
        iterator.set(
           multiply(
                iterator.next(),
                multiple
            )
        )
    }
}

fun multiply(a: PointF3D, multiple: PointF3D) = PointF3D.from(a.x * multiple.x, a.y * multiple.y, a.z * multiple.z)

fun maxAbs(point: PointF3D) = Floats.max(Math.abs(point.x), Math.abs(point.y), Math.abs(point.z))

fun sumAbs(point: PointF3D) = Math.abs(point.x) + Math.abs(point.y) + Math.abs(point.z)

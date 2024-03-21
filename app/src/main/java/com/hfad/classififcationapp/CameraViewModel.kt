package com.hfad.classififcationapp

import android.location.Location
import android.media.ExifInterface
import androidx.lifecycle.ViewModel

class CameraViewModel:ViewModel() {

    // 設置 GPS 相關的 EXIF 屬性
    fun setLocationInExif(exifInterface: ExifInterface, location: Location) {
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, convert(location.latitude))
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, convert(location.longitude))
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (location.latitude >= 0) "N" else "S")
        exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (location.longitude >= 0) "E" else "W")
    }

    // 將經緯度轉換為度分秒形式
    private fun convert(coordinate: Double): String {
        val absCoordinate = Math.abs(coordinate)
        val degrees = Math.floor(absCoordinate).toInt()
        val minutesNotTruncated = (absCoordinate - degrees) * 60
        val minutes = Math.floor(minutesNotTruncated).toInt()
        val seconds = (minutesNotTruncated - minutes) * 60
        return "$degrees/1,$minutes/1,$seconds/1000"
    }
}
package com.example.eaties

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class NotificationHelper(var co:Context,var msg:String) {
    private val CHANNEL_ID = "massage id"
    private val NOTIFICATION_ID=123
    /**set Notification*/
    @SuppressLint("MissingPermission")
    fun Notification(){
        createNotificationChannel()
        val senInt = Intent(co,CartActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingInt = PendingIntent.getActivities(co,0, arrayOf(senInt),PendingIntent.FLAG_IMMUTABLE)
        /**set notification Dialog*/
        val isnotification = NotificationCompat.Builder(co,CHANNEL_ID)
            .setSmallIcon(R.drawable.baseline_fastfood_24)
            .setContentTitle("Eaties")
            .setContentText(msg)
            .setContentIntent(pendingInt)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(co)
            .notify(NOTIFICATION_ID,isnotification)
    }
    /**create createNotificationChannel*/
    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT>= Build.VERSION_CODES.O){
            val name = CHANNEL_ID
            val descrip = "Channel descrip"
            val imports = NotificationManager.IMPORTANCE_DEFAULT
            val cannels = NotificationChannel(CHANNEL_ID,name,imports).apply {
                description = descrip
            }
            val notificationManger = co.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManger.createNotificationChannel(cannels)
        }
    }
}
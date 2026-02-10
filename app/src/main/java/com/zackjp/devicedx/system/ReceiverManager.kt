package com.zackjp.devicedx.system

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject


class ReceiverManager @Inject constructor(
    @param:ApplicationContext private val appContext: Context,
) {

    fun registerReceiver(
        intentFilter: IntentFilter,
        receiveBlock: (Context?, Intent?) -> Unit,
    ): BroadcastReceiver {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                receiveBlock(context, intent)
            }
        }

        ContextCompat.registerReceiver(
            appContext,
            receiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED,
        )

        return receiver
    }

    fun unregisterReceiver(receiver: BroadcastReceiver) {
        appContext.unregisterReceiver(receiver)
    }

}

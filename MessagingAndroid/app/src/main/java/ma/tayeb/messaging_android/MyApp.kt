package ma.tayeb.messaging_android

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen
import ma.tayeb.messaging_android.websocket.StompManager

class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
        
        // Initialize STOMP manager
        StompManager.getInstance()
    }
}
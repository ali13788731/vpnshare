package ir.mrpc.hotshare

import android.app.Application
import ir.mrpc.hotshare.client.Connector
import ir.mrpc.hotshare.core.Prefs

class HotShareApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        Connector.init(this)
    }
}

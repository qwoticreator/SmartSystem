package home.api

import android.content.Context
import android.util.Log
import com.android.volley.Request
import com.android.volley.toolbox.JsonObjectRequest
import home.data.UnifiedRequestCallback
import home.helpers.Global
import home.interfaces.HomeRecyclerViewHelperInterface

class EspEasyAPI(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    private val parser = EspEasyAPIParser(c.resources, this)

    override fun loadList(callback: CallbackInterface) {
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "json", null,
            { infoResponse ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(
                        parser.parseResponse(infoResponse),
                        deviceId
                    ),
                    recyclerViewInterface
                )
            },
            { error ->
                callback.onItemsLoaded(
                    UnifiedRequestCallback(null, deviceId,
                    Global.volleyError(c, error)
                ), null)
            }
        )
        queue.add(jsonObjectRequest)
    }

    override fun loadStates(callback: RealTimeStatesCallback, offset: Int) {
        val jsonObjectRequest = JsonObjectRequest(
            Request.Method.GET, url + "json", null,
            { infoResponse ->
                callback.onStatesLoaded(
                    parser.parseStates(infoResponse),
                    offset,
                    dynamicSummaries
                )
            }, { }
        )
        queue.add(jsonObjectRequest)
    }

    override fun changeSwitchState(id: String, state: Boolean) {
        val switchUrl = url + "control?cmd=GPIO," + id + "," + (if (state) "1" else "0")
        val jsonObjectRequest = JsonObjectRequest(
            switchUrl,
            { },
            { e -> Log.e(Global.LOG_TAG, e.toString()) }
        )
        queue.add(jsonObjectRequest)
    }
}

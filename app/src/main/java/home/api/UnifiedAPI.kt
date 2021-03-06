package home.api

import android.content.Context
import android.content.res.Resources
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import home.data.ListViewItem
import home.data.UnifiedRequestCallback
import home.helpers.Devices
import home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONObject

open class UnifiedAPI(
    protected val c: Context,
    val deviceId: String,
    protected val recyclerViewInterface: HomeRecyclerViewHelperInterface?
) {

    interface CallbackInterface {
        fun onItemsLoaded(holder: UnifiedRequestCallback, recyclerViewInterface: HomeRecyclerViewHelperInterface?)
        fun onExecuted(result: String, shouldRefresh: Boolean = false)
    }

    interface RealTimeStatesCallback {
        fun onStatesLoaded(states: ArrayList<Boolean?>, offset: Int, dynamicSummary: Boolean)
    }

    var dynamicSummaries: Boolean = true
    var needsRealTimeData: Boolean = false

    protected val url: String = Devices(c).getDeviceById(deviceId).address
    protected val queue: RequestQueue = Volley.newRequestQueue(c)

    open fun loadList(callback: CallbackInterface) {}
    open fun loadStates(callback: RealTimeStatesCallback, offset: Int) {}
    open fun execute(path: String, callback: CallbackInterface) {}
    open fun changeSwitchState(id: String, state: Boolean) {}

    open class Parser(protected val resources: Resources, protected val api: UnifiedAPI? = null) {
        open fun parseResponse(response: JSONObject): ArrayList<ListViewItem> = arrayListOf()
        open fun parseStates(response: JSONObject): ArrayList<Boolean?> = arrayListOf()
    }
}
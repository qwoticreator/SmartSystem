package home.api

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.preference.PreferenceManager
import com.android.volley.Request
import com.android.volley.toolbox.StringRequest
import com.example.smartsystem.R
import org.json.JSONArray
import org.json.JSONObject
import home.data.ListViewItem
import home.data.UnifiedRequestCallback
import home.helpers.Global
import home.helpers.TasmotaHelper
import home.interfaces.HomeRecyclerViewHelperInterface
import org.json.JSONException

class Tasmota(
    c: Context,
    deviceId: String,
    recyclerViewInterface: HomeRecyclerViewHelperInterface?
) : UnifiedAPI(c, deviceId, recyclerViewInterface) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(c)

    override fun loadList(callback: CallbackInterface) {
        val list = JSONArray(prefs.getString(deviceId, TasmotaHelper.EMPTY_ARRAY))
        val listItems: ArrayList<ListViewItem> = ArrayList(list.length())
        if (list.length() == 0) {
            listItems += ListViewItem(
                title = c.resources.getString(R.string.tasmota_empty_list),
                summary = c.resources.getString(R.string.tasmota_empty_list_summary),
                icon = R.drawable.ic_warning
            )
        } else {
            var currentItem: JSONObject
            for (i in 0 until list.length()) {
                try {
                    currentItem = list.optJSONObject(i) ?: JSONObject()
                    listItems += ListViewItem(
                        title = currentItem.optString("title"),
                        summary = currentItem.optString("command"),
                        hidden = "tasmota_command#$i",
                        icon = R.drawable.ic_do
                    )
                } catch (e: JSONException) {
                    Log.e(Global.LOG_TAG, e.toString())
                }
            }
        }

        listItems += ListViewItem(
            title = c.resources.getString(R.string.tasmota_add_command),
            summary = c.resources.getString(R.string.tasmota_add_command_summary),
            icon = R.drawable.ic_add,
            hidden = "add"
        )

        listItems += ListViewItem(
            title = c.resources.getString(R.string.tasmota_execute_once),
            summary = c.resources.getString(R.string.tasmota_execute_once_summary),
            icon = R.drawable.ic_edit,
            hidden = "execute_once"
        )

        callback.onItemsLoaded(UnifiedRequestCallback(listItems, deviceId, ""), recyclerViewInterface)
    }

    override fun execute(path: String, callback: CallbackInterface) {
        val request = StringRequest(Request.Method.GET, url + path,
            { response ->
                callback.onExecuted(response)
            },
            { error ->
                Toast.makeText(c, Global.volleyError(c, error), Toast.LENGTH_LONG).show()
            }
        )
        queue.add(request)
    }
}
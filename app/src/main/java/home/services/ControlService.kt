package home.services

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.controls.Control
import android.service.controls.ControlsProviderService
import android.service.controls.actions.BooleanAction
import android.service.controls.actions.ControlAction
import android.service.controls.templates.ControlButton
import android.service.controls.templates.ToggleTemplate
import androidx.annotation.RequiresApi
import com.example.smartsystem.R
import home.activities.MainActivity
import home.api.UnifiedAPI
import home.data.DeviceItem
import home.data.UnifiedRequestCallback
import home.helpers.Devices
import home.helpers.Global
import home.interfaces.HomeRecyclerViewHelperInterface
import java.util.concurrent.Flow
import java.util.function.Consumer

@RequiresApi(Build.VERSION_CODES.R)
class ControlService : ControlsProviderService() {

    private var updateSubscriber: Flow.Subscriber<in Control>? = null

    override fun createPublisherForAllAvailable(): Flow.Publisher<Control> {
        return Flow.Publisher { subscriber ->
            updateSubscriber = subscriber
            if (!Global.checkNetwork(this)) {
                subscriber.onComplete()
                return@Publisher
            }
            val devices = Devices(this)
            val relevantDevices = arrayListOf<DeviceItem>()
            for (i in 0 until devices.length) {
                val currentDevice = devices.getDeviceByIndex(i)
                if (
                    !currentDevice.hide
                    && Global.POWER_MENU_MODES.contains(currentDevice.mode)
                ) relevantDevices.add(currentDevice)
            }
            val pi = PendingIntent.getActivity(
                baseContext, 0, Intent(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            var finishedRequests = 0
            for (i in 0 until relevantDevices.size) {
                Global.getCorrectAPI(this, relevantDevices[i].mode, relevantDevices[i].id)
                    .loadList(object : UnifiedAPI.CallbackInterface {
                        override fun onItemsLoaded(
                            holder: UnifiedRequestCallback,
                            recyclerViewInterface: HomeRecyclerViewHelperInterface?
                        ) {
                            if (holder.response != null) {
                                holder.response.forEach {
                                    subscriber.onNext(
                                        Control.StatelessBuilder(
                                            relevantDevices[i].id + '@' + it.hidden,
                                            pi
                                        )
                                            .setTitle(it.title)
                                            .setZone(relevantDevices[i].name)
                                            .setStructure(resources.getString(R.string.app_name))
                                            .setDeviceType(Global.getDeviceType(relevantDevices[i].iconName))
                                            .build()
                                    )
                                }
                            }
                            finishedRequests++
                            if (finishedRequests == relevantDevices.size) subscriber.onComplete()
                        }

                        override fun onExecuted(
                            result: String,
                            shouldRefresh: Boolean
                        ) {
                        }
                    })
            }
        }
    }

    internal fun getUnreachableControl(id: String, device: DeviceItem, pi: PendingIntent): Control {
        return Control.StatefulBuilder(id, pi)
            .setTitle(device.name)
            .setZone(device.name)
            .setStructure(resources.getString(R.string.app_name))
            .setDeviceType(Global.getDeviceType(device.iconName))
            .setStatus(Control.STATUS_DISABLED)
            .setStatusText(resources.getString(R.string.str_unreachable))
            .build()
    }

    private fun loadStatefulControl(subscriber: Flow.Subscriber<in Control>?, id: String) {
        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val device = Devices(this).getDeviceById(id.substring(0, id.indexOf('@')))
        if (Global.checkNetwork(this)) {
            Global.getCorrectAPI(this, device.mode, device.id)
                .loadList(object : UnifiedAPI.CallbackInterface {
                    override fun onItemsLoaded(
                        holder: UnifiedRequestCallback,
                        recyclerViewInterface: HomeRecyclerViewHelperInterface?
                    ) {
                        if (holder.response != null) {
                            holder.response.forEach {
                                if (device.id + '@' + it.hidden == id) {
                                    val controlBuilder = Control.StatefulBuilder(id, pi)
                                        .setTitle(it.title)
                                        .setZone(device.name)
                                        .setStructure(resources.getString(R.string.app_name))
                                        .setDeviceType(Global.getDeviceType(device.iconName))
                                        .setStatus(Control.STATUS_OK)
                                    if (it.state != null) {
                                        controlBuilder.setControlTemplate(
                                            ToggleTemplate(
                                                id,
                                                ControlButton(
                                                    it.state ?: false,
                                                    it.state.toString()
                                                )
                                            )
                                        )
                                        controlBuilder.setStatusText(
                                            if (it.state == true) resources.getString(R.string.str_on)
                                            else resources.getString(R.string.str_off)
                                        )
                                    }
                                    subscriber?.onNext(controlBuilder.build())
                                }
                            }
                        } else {
                            subscriber?.onNext(getUnreachableControl(id, device, pi))
                        }
                    }

                    override fun onExecuted(
                        result: String,
                        shouldRefresh: Boolean
                    ) {
                    }
                })
        } else {
            subscriber?.onNext(getUnreachableControl(id, device, pi))
        }
    }

    override fun createPublisherFor(controlIds: MutableList<String>): Flow.Publisher<Control> {
        return Flow.Publisher { subscriber ->
            updateSubscriber = subscriber
            subscriber.onSubscribe(object : Flow.Subscription {
                override fun request(n: Long) {}
                override fun cancel() {}
            })
            controlIds.forEach { id ->
                loadStatefulControl(subscriber, id)
            }
        }
    }

    override fun performControlAction(
        controlId: String,
        action: ControlAction,
        consumer: Consumer<Int>
    ) {
        if (Global.checkNetwork(this)) {
            val device = Devices(this)
                .getDeviceById(controlId.substring(0, controlId.indexOf('@')))
            if (action is BooleanAction) {
                Global.getCorrectAPI(this, device.mode, device.id)
                    .changeSwitchState(controlId.substring(device.id.length + 1), action.newState)
            }
            consumer.accept(ControlAction.RESPONSE_OK)
            loadStatefulControl(updateSubscriber, controlId)
        } else {
            consumer.accept(ControlAction.RESPONSE_FAIL)
        }
    }
}
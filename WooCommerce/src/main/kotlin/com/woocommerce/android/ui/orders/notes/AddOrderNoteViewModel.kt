package com.woocommerce.android.ui.orders.notes

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_NOTE_ADD
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.OrderNote
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.AnalyticsUtils
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.store.WCOrderStore

class AddOrderNoteViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispathers: CoroutineDispatchers,
    private val orderStore: WCOrderStore,
    private val resourceProvider: ResourceProvider,
    private val networkStatus: NetworkStatus,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispathers) {

    final val addOrderNoteViewStateData = LiveDataDelegate(savedState, ViewState())
    private var addOrderNoteViewState by addOrderNoteViewStateData

    private val navArgs: AddOrderNoteFragmentArgs by savedState.navArgs()

    private val orderId: OrderIdentifier
        get() = navArgs.orderId

    private val orderNumber: String
        get() = navArgs.orderNumber

    val screenTitle: String
        get() = resourceProvider.getString(R.string.orderdetail_orderstatus_ordernum, orderNumber)

    init {
        if (orderId.isEmpty() || orderNumber.isEmpty()) {
            triggerEvent(Exit)
        }
        checkIfHasBillingMail()
    }

    fun onOrderTextEntered(text: String) {
        val draftNote = addOrderNoteViewState.draftNote.copy(note = text)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote)
    }

    fun onIsCustomerCheckboxChanged(isChecked: Boolean){
        AnalyticsTracker.track(
            ADD_ORDER_NOTE_EMAIL_NOTE_TO_CUSTOMER_TOGGLED,
            mapOf(AnalyticsTracker.KEY_STATE to AnalyticsUtils.getToggleStateLabel(isChecked))
        )
        val draftNote = addOrderNoteViewState.draftNote.copy(isCustomerNote = isChecked)
        addOrderNoteViewState = addOrderNoteViewState.copy(draftNote = draftNote)
    }

    private fun checkIfHasBillingMail() {
        val email = orderStore.getOrderByIdentifier(orderId)?.billingEmail
        addOrderNoteViewState = addOrderNoteViewState.copy(hasBillingEmail = email?.isNotEmpty() == true)
    }

    fun pushOrderNote() {
        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(R.string.offline_error))
            return
        }

        val order = orderStore.getOrderByIdentifier(orderId)
        if (order == null) {
            triggerEvent(ShowSnackbar(R.string.add_order_note_error))
            return
        }
        AnalyticsTracker.track(ORDER_NOTE_ADD, mapOf(AnalyticsTracker.KEY_PARENT_ID to order.remoteOrderId))

        addOrderNoteViewState.copy(isProgressDialogShown = true)

        val note = addOrderNoteViewState.draftNote
        launch {
            if(orderDetailRepository.addOrderNote(order.id, order.remoteOrderId, note))
            {
                triggerEvent(ShowSnackbar(R.string.add_order_note_added))
                triggerEvent(ExitWithResult(note))
            } else {
                triggerEvent(ShowSnackbar(R.string.add_order_note_error))
            }
        }
    }


    @Parcelize
    data class ViewState(
        val draftNote: OrderNote = OrderNote(note = "", isCustomerNote = false),
        val hasBillingEmail: Boolean = false,
        val isProgressDialogShown: Boolean = false
    ) : Parcelable


    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddOrderNoteViewModel>
}
package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.util.Log
import com.tinder.StateMachine
import com.woocommerce.android.model.Address
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ExperimentalCoroutinesApi
class ShippingLabelStateMachine @Inject constructor() {
    companion object {
        private val TAG = ShippingLabelStateMachine::class.simpleName
    }

    private val _effects = MutableStateFlow<SideEffect>(SideEffect.NoOp)
    val effects: StateFlow<SideEffect> = _effects

    private val stateMachine = StateMachine.create<State, Event, SideEffect> {
        initialState(State.Idle)

        state<State.Idle> {
            on<Event.FlowStarted> { event ->
                transitionTo(State.DataLoading, SideEffect.LoadData(event.orderId))
            }
        }

        state<State.DataLoading> {
            on<Event.DataLoaded> { event ->
                val data = Data(event.originAddress, event.shippingAddress, setOf(FlowStep.ORIGIN_ADDRESS))
                transitionTo(State.WaitingForUser(data), SideEffect.UpdateViewState(data))
            }
            on<Event.DataLoadingFailed> {
                transitionTo(State.DataLoadingFailure, SideEffect.ShowError(Error.DataLoadingError))
            }
        }

        state<State.WaitingForUser> {
            on<Event.OriginAddressValidationStarted> {
                transitionTo(State.OriginAddressValidation(data), SideEffect.ValidateAddress(data.originAddress))
            }
            on<Event.ShippingAddressValidationStarted> {
                transitionTo(State.ShippingAddressValidation(data), SideEffect.ValidateAddress(data.shippingAddress))
            }
            on<Event.PackageSelectionStarted> {
                transitionTo(State.PackageSelection(data), SideEffect.ShowPackageOptions)
            }
            on<Event.CustomsDeclarationStarted> {
                transitionTo(State.CustomsDeclaration(data), SideEffect.ShowCustomsForm)
            }
            on<Event.ShippingCarrierSelectionStarted> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions)
            }
            on<Event.PaymentSelectionStarted> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentDetails)
            }
            on<Event.EditOriginAddressRequested> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress))
            }
            on<Event.EditShippingAddressRequested> {
                transitionTo(State.ShippingAddressEditing(data), SideEffect.OpenAddressEditor(data.shippingAddress))
            }
            on<Event.EditPackagingRequested> {
                transitionTo(State.PackageSelection(data), SideEffect.ShowPackageOptions)
            }
            on<Event.EditCustomsRequested> {
                transitionTo(State.CustomsDeclaration(data), SideEffect.ShowCustomsForm)
            }
            on<Event.EditShippingCarrierRequested> {
                transitionTo(State.ShippingCarrierSelection(data), SideEffect.ShowCarrierOptions)
            }
            on<Event.EditPaymentRequested> {
                transitionTo(State.PaymentSelection(data), SideEffect.ShowPaymentDetails)
            }
        }

        state<State.OriginAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    stepsDone = data.stepsDone + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.OriginAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(data.originAddress, event.suggested)
                )
            }
            on<Event.AddressNotRecognized> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress))
            }
        }

        state<State.OriginAddressSuggestion> {
            on<Event.SuggestedAddressSelected> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    stepsDone = data.stepsDone + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.EditOriginAddressRequested> {
                transitionTo(State.OriginAddressEditing(data), SideEffect.OpenAddressEditor(data.originAddress))
            }
        }

        state<State.OriginAddressEditing> {
            on<Event.AddressEditFinished> { event ->
                transitionTo(State.OriginAddressValidation(data), SideEffect.ValidateAddress(event.address))
            }
            on<Event.AddressUsedAsIs> { event ->
                val newData = data.copy(
                    originAddress = event.address,
                    stepsDone = data.stepsDone + FlowStep.SHIPPING_ADDRESS
                )
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.ShippingAddressValidation> {
            on<Event.AddressValidated> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    stepsDone = data.stepsDone + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.AddressInvalid> { event ->
                transitionTo(
                    State.ShippingAddressSuggestion(data),
                    SideEffect.ShowAddressSuggestion(data.originAddress, event.suggested)
                )
            }
            on<Event.AddressNotRecognized> {
                transitionTo(State.ShippingAddressEditing(data), SideEffect.OpenAddressEditor(data.shippingAddress))
            }
        }

        state<State.OriginAddressSuggestion> {
            on<Event.SuggestedAddressSelected> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    stepsDone = data.stepsDone + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
            on<Event.EditShippingAddressRequested> {
                transitionTo(State.ShippingAddressEditing(data), SideEffect.OpenAddressEditor(data.shippingAddress))
            }
        }

        state<State.ShippingAddressEditing> {
            on<Event.AddressEditFinished> { event ->
                transitionTo(State.ShippingAddressValidation(data), SideEffect.ValidateAddress(event.address))
            }
            on<Event.AddressUsedAsIs> { event ->
                val newData = data.copy(
                    shippingAddress = event.address,
                    stepsDone = data.stepsDone + FlowStep.PACKAGING
                )
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.PackageSelection> {
            on<Event.PackagesSelected> {
                val newData = data.copy(stepsDone = data.stepsDone + FlowStep.CUSTOMS)
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.CustomsDeclaration> {
            on<Event.CustomsFormFilledOut> {
                val newData = data.copy(stepsDone = data.stepsDone + FlowStep.CARRIER)
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.ShippingCarrierSelection> {
            on<Event.ShippingCarrierSelected> {
                val newData = data.copy(stepsDone = data.stepsDone + FlowStep.PAYMENT)
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
        }

        state<State.PaymentSelection> {
            on<Event.PaymentSelected> {
                val newData = data.copy(stepsDone = data.stepsDone + FlowStep.DONE)
                transitionTo(State.WaitingForUser(newData), SideEffect.UpdateViewState(newData))
            }
        }

        onTransition { transition ->
            if (transition is StateMachine.Transition.Valid) {
                Log.d(TAG, transition.toState.toString())
                transition.sideEffect?.let { sideEffect ->
                    _effects.value = sideEffect
                }
            } else {
                throw InvalidStateException("Unexpected event ${transition.event} passed from ${transition.fromState}")
            }
        }
    }

    fun start(orderId: String) {
        stateMachine.transition(Event.FlowStarted(orderId))
    }

    fun handleEvent(event: Event) {
        Log.d(TAG, event.toString())
        stateMachine.transition(event)
    }

    data class Data(
        val originAddress: Address,
        val shippingAddress: Address,
        val stepsDone: Set<FlowStep>
    )

    enum class FlowStep {
        ORIGIN_ADDRESS, SHIPPING_ADDRESS, PACKAGING, CUSTOMS, CARRIER, PAYMENT, DONE
    }

    sealed class Error {
        object DataLoadingError : Error()
    }

    sealed class State {
        object Idle : State()
        object DataLoadingFailure : State()
        object DataLoading : State()
        data class WaitingForUser(val data: Data) : State()

        data class OriginAddressValidation(val data: Data) : State()
        data class OriginAddressSuggestion(val data: Data) : State()
        data class OriginAddressEditing(val data: Data) : State()

        data class ShippingAddressValidation(val data: Data) : State()
        data class ShippingAddressSuggestion(val data: Data) : State()
        data class ShippingAddressEditing(val data: Data) : State()

        data class PackageSelection(val data: Data) : State()
        data class CustomsDeclaration(val data: Data) : State()
        data class ShippingCarrierSelection(val data: Data) : State()
        data class PaymentSelection(val data: Data) : State()
    }

    sealed class Event {
        data class FlowStarted(val orderId: String) : Event()
        data class DataLoaded(val originAddress: Address, val shippingAddress: Address) : Event()
        object DataLoadingFailed : Event()

        object AddressNotRecognized : Event()
        data class AddressValidated(val address: Address) : Event()
        data class AddressInvalid(val suggested: Address) : Event()
        data class AddressUsedAsIs(val address: Address) : Event()
        data class AddressEditFinished(val address: Address) : Event()
        data class SuggestedAddressSelected(val address: Address) : Event()

        object OriginAddressValidationStarted : Event()
        object EditOriginAddressRequested : Event()

        object ShippingAddressValidationStarted : Event()
        object EditShippingAddressRequested : Event()

        object PackageSelectionStarted : Event()
        object EditPackagingRequested : Event()
        object PackagesSelected : Event()

        object CustomsDeclarationStarted : Event()
        object EditCustomsRequested : Event()
        object CustomsFormFilledOut : Event()

        object ShippingCarrierSelectionStarted : Event()
        object EditShippingCarrierRequested : Event()
        object ShippingCarrierSelected : Event()

        object PaymentSelectionStarted : Event()
        object EditPaymentRequested : Event()
        object PaymentSelected : Event()
    }

    sealed class SideEffect {
        object NoOp : SideEffect()
        data class LoadData(val orderId: String) : SideEffect()
        data class ShowError(val error: Error) : SideEffect()
        data class UpdateViewState(val data: Data) : SideEffect()

        data class ValidateAddress(val address: Address) : SideEffect()
        data class ShowAddressSuggestion(val entered: Address, val suggested: Address) : SideEffect()
        data class OpenAddressEditor(val address: Address) : SideEffect()

        object ShowPackageOptions : SideEffect()
        object ShowCustomsForm : SideEffect()
        object ShowCarrierOptions : SideEffect()
        object ShowPaymentDetails : SideEffect()
    }

    class InvalidStateException(message: String) : Exception(message)
}

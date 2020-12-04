package com.woocommerce.android.ui.orders.notes

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.observe
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ADD_ORDER_NOTE_ADD_BUTTON_TAPPED
import com.woocommerce.android.databinding.FragmentAddOrderNoteBinding
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.takeIfNotEqualTo
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.dialog.WooDialog
import com.woocommerce.android.ui.main.MainActivity.Companion.BackPressListener
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ViewModelFactory
import org.wordpress.android.util.ActivityUtils
import javax.inject.Inject

class AddOrderNoteFragment : BaseFragment(), BackPressListener {
    companion object {
        const val TAG = "AddOrderNoteFragment"
        private const val FIELD_NOTE_TEXT = "note_text"
        private const val FIELD_IS_CUSTOMER_NOTE = "is_customer_note"
        private const val FIELD_IS_CONFIRMING_DISCARD = "is_confirming_discard"
        const val KEY_ADD_NOTE_RESULT = "key_add_note_result"
    }

    @Inject lateinit var viewModelFactory: ViewModelFactory
    @Inject lateinit var uiMessageResolver: UIMessageResolver

    private val viewModel: AddOrderNoteViewModel by viewModels { viewModelFactory }

    private var isConfirmingDiscard = false
    private var shouldShowDiscardDialog = true

    private var _binding: FragmentAddOrderNoteBinding? = null
    private val binding get() = _binding!!

    private val enteredText: String
        get() = binding.addNoteEditor.text.toString().trim()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentAddOrderNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUi()
        setupObservers()

        if (savedInstanceState == null) {
            binding.addNoteEditor.requestFocus()
            ActivityUtils.showKeyboard(binding.addNoteEditor)
        }
    }

    private fun initUi() {
        binding.addNoteEditor.doOnTextChanged { text, _, _, _ ->
            viewModel.onOrderTextEntered(text.toString())
        }

        binding.addNoteSwitch.setOnCheckedChangeListener { _, isChecked ->
            viewModel.onIsCustomerCheckboxChanged(isChecked)
        }
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is ExitWithResult<*> -> navigateBackWithResult(KEY_ADD_NOTE_RESULT, event.data)
                is Exit -> findNavController().navigateUp()
                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }

        viewModel.addOrderNoteViewStateData.observe(viewLifecycleOwner) { old, new ->
            new.draftNote.takeIfNotEqualTo(old?.draftNote) {
                if (binding.addNoteEditor.text.toString() != it.note) {
                    binding.addNoteEditor.setText(it.note)
                }
                binding.addNoteSwitch.isChecked = it.isCustomerNote

                if(new.hasBillingEmail) {
                    binding.addNoteSwitch.isVisible = true
                    val noteIcon = if (it.isCustomerNote) R.drawable.ic_note_public else R.drawable.ic_note_private
                    binding.addNoteIcon.setImageResource(noteIcon)
                } else {
                    binding.addNoteSwitch.isVisible = false
                }
            }
        }
    }

    override fun getFragmentTitle() = viewModel.screenTitle

    override fun onResume() {
        super.onResume()
        AnalyticsTracker.trackViewShown(this)
    }

    override fun onStop() {
        super.onStop()
        WooDialog.onCleared()
        activity?.let {
            ActivityUtils.hideKeyboard(it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_add, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_add -> {
                AnalyticsTracker.track(ADD_ORDER_NOTE_ADD_BUTTON_TAPPED)
                viewModel.pushOrderNote()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(FIELD_NOTE_TEXT, enteredText)
        outState.putBoolean(FIELD_IS_CUSTOMER_NOTE, binding.addNoteSwitch.isChecked)
        outState.putBoolean(FIELD_IS_CONFIRMING_DISCARD, isConfirmingDiscard)
        super.onSaveInstanceState(outState)
    }

    /**
     * Prevent back press in the main activity if the user entered a note so we can confirm the discard
     */
    override fun onRequestAllowBackPress(): Boolean {
        return true
    }
}

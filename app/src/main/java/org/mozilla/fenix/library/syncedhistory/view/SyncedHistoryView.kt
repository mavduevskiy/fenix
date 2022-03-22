/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory.view

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import mozilla.components.support.base.feature.UserInteractionHandler
import org.mozilla.fenix.R
import org.mozilla.fenix.databinding.ComponentSyncedHistoryBinding
import org.mozilla.fenix.library.LibraryPageView
import org.mozilla.fenix.library.history.*
import org.mozilla.fenix.theme.FirefoxTheme

class SyncedHistoryView(
    container: ViewGroup,
    val interactor: HistoryInteractor
) : LibraryPageView(container), UserInteractionHandler {

    val binding = ComponentSyncedHistoryBinding.inflate(
        LayoutInflater.from(container.context), container, true
    )

    var mode: HistoryFragmentState.Mode = HistoryFragmentState.Mode.Normal
        private set

    private var adapterItemCount: Int? = null
    private var swipeEnabled = true

    fun setData(
        data: Flow<PagingData<History>>,
        store: HistoryFragmentStore
    ) {
        binding.composeView.setContent {
            FirefoxTheme {
                HistoryList(
                    data,
                    store,
                    interactor,
                    onRefresh = { interactor.onRequestSync() },
                    swipeEnabled = { swipeEnabled }
                )
            }
        }
    }

    fun update(state: HistoryFragmentState) {
        val oldMode = mode

        binding.progressBar.isVisible = state.isDeletingItems
        swipeEnabled = state.mode === HistoryFragmentState.Mode.Normal || state.mode === HistoryFragmentState.Mode.Syncing
        mode = state.mode

        updateEmptyState(state.pendingDeletionIds.size != adapterItemCount)

        if (state.mode::class != oldMode::class) {
            interactor.onModeSwitched()
        }

        when (val mode = state.mode) {
            is HistoryFragmentState.Mode.Normal -> {
                setUiForNormalMode(
                    context.getString(R.string.synced_history)
                )
            }
            is HistoryFragmentState.Mode.Editing -> {
                setUiForSelectingMode(
                    context.getString(R.string.history_multi_select_title, mode.selectedItems.size)
                )
            }
        }
    }

    fun updateEmptyState(userHasHistory: Boolean) {
        binding.composeView.isVisible = userHasHistory
        binding.historyEmptyView.isVisible = !userHasHistory

        if (!userHasHistory) {
            binding.historyEmptyView.announceForAccessibility(context.getString(R.string.history_empty_message))
        }
    }

    override fun onBackPressed(): Boolean {
        return interactor.onBackPressed()
    }
}

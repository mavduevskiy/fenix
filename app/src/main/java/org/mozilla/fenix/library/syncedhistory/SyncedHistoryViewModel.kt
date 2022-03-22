/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import org.mozilla.fenix.components.history.PagedHistoryProvider
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryDataSource

class SyncedHistoryViewModel(historyProvider: PagedHistoryProvider) : ViewModel() {
    var userHasHistory = MutableLiveData(true)
    private lateinit var dataSource: HistoryDataSource
    var history: Flow<PagingData<History>> = Pager(
        PagingConfig(PAGE_SIZE),
        null
    ) {
        dataSource = HistoryDataSource(
            historyProvider = historyProvider,
            isRemote = true,
            onZeroItemsLoaded = { userHasHistory.value = false }
        )
        dataSource
    }.flow

    fun refreshData() {
        dataSource.invalidate()
    }

    companion object {
        private const val PAGE_SIZE = 25
    }
}
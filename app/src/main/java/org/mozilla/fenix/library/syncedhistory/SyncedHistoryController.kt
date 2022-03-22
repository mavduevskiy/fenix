/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory

import androidx.navigation.NavController
import kotlinx.coroutines.CoroutineScope
import org.mozilla.fenix.R
import org.mozilla.fenix.components.metrics.MetricController
import org.mozilla.fenix.ext.navigateSafe
import org.mozilla.fenix.library.history.DefaultHistoryController
import org.mozilla.fenix.library.history.History
import org.mozilla.fenix.library.history.HistoryFragmentDirections
import org.mozilla.fenix.library.history.HistoryFragmentStore

class SyncedHistoryController(
    store: HistoryFragmentStore,
    private val navController: NavController,
    scope: CoroutineScope,
    openToBrowser: (item: History.Regular) -> Unit,
    displayDeleteAll: () -> Unit,
    invalidateOptionsMenu: () -> Unit,
    deleteHistoryItems: (Set<History>) -> Unit,
    syncHistory: suspend () -> Unit,
    metrics: MetricController
) : DefaultHistoryController(
    store,
    navController,
    scope,
    openToBrowser,
    displayDeleteAll,
    invalidateOptionsMenu,
    deleteHistoryItems,
    syncHistory,
    metrics
) {
    override fun handleSearch() {
        val directions = HistoryFragmentDirections.actionGlobalHistorySearchDialog()
        navController.navigateSafe(R.id.syncedHistoryFragment, directions)
    }
}

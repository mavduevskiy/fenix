/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.library.syncedhistory.view

import android.graphics.Color
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import kotlinx.coroutines.flow.Flow
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.PrimaryText
import org.mozilla.fenix.ext.hideAndDisable
import org.mozilla.fenix.ext.showAndEnable
import org.mozilla.fenix.library.LibrarySiteItemView
import org.mozilla.fenix.library.history.*
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.theme.FirefoxTheme

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryList(
    history: Flow<PagingData<History>>,
    historyStore: HistoryFragmentStore,
    interactor: HistoryInteractor,
    onRefresh: () -> Unit,
    swipeEnabled: () -> Boolean
) {
    val historyItems: LazyPagingItems<History> = history.collectAsLazyPagingItems()
    val state = historyStore.observeAsComposableState { state ->
        state
    }
    val pendingDeletionIds = state.value?.pendingDeletionIds
    val selectedItems = state.value?.mode?.selectedItems
    val mode = state.value?.mode

    val itemsWithHeaders: MutableMap<HistoryItemTimeGroup, Int> = mutableMapOf()
    val collapsedHeaders = remember {
        mutableStateMapOf(
            HistoryItemTimeGroup.Today to false,
            HistoryItemTimeGroup.Yesterday to false,
            HistoryItemTimeGroup.ThisWeek to false,
            HistoryItemTimeGroup.ThisMonth to false,
            HistoryItemTimeGroup.Older to false
        )
    }
    val context = LocalContext.current

    SwipeRefresh(
        onRefresh = { onRefresh.invoke() },
        swipeEnabled = swipeEnabled.invoke(),
        state = rememberSwipeRefreshState(false),
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                contentColor = FirefoxTheme.colors.textPrimary // check the color is black not blue
            )
        }
    ) {
        LazyColumn {
            val itemCount = historyItems.itemCount
            for (index in 0 until itemCount) {
                val historyItem = historyItems.peek(index)
                if (historyItem != null) {
                    var timeGroup: HistoryItemTimeGroup? = null
                    val isPendingDeletion = false
                    if (itemsWithHeaders.containsKey(historyItem.historyTimeGroup)) {
                        if (isPendingDeletion && itemsWithHeaders[historyItem.historyTimeGroup] == index) {
                            itemsWithHeaders.remove(historyItem.historyTimeGroup)
                        } else if (isPendingDeletion && itemsWithHeaders[historyItem.historyTimeGroup] != index) {
                            // do nothing
                        } else {
                            if (index <= itemsWithHeaders[historyItem.historyTimeGroup] as Int) {
                                itemsWithHeaders[historyItem.historyTimeGroup] = index
                                timeGroup = historyItem.historyTimeGroup
                            }
                        }
                    } else if (!isPendingDeletion) {
                        itemsWithHeaders[historyItem.historyTimeGroup] = index
                        timeGroup = historyItem.historyTimeGroup
                    }
                    timeGroup?.humanReadable(context)?.let { text ->
                        this@LazyColumn.stickyHeader(key = index) {
                            HistorySectionHeader(
                                text,
                                expanded = collapsedHeaders[historyItem.historyTimeGroup]
                            ) {
                                val currentValue = collapsedHeaders[historyItem.historyTimeGroup]!!
                                collapsedHeaders[historyItem.historyTimeGroup] = !currentValue
                            }
                        }
                    }

                    item {
                        // Gets item, triggering page loads if needed
                        val historyItem = historyItems[index]!!
                        val collapsedHeader = collapsedHeaders[historyItem.historyTimeGroup] == true
                        val pendingDeletion =
                            pendingDeletionIds?.contains(historyItem.visitedAt) == true
                        val shouldHide = collapsedHeader || pendingDeletion
                        if (!shouldHide) {
                            val bodyText = when (historyItem) {
                                is History.Regular -> historyItem.url
                                is History.Metadata -> historyItem.url
                                is History.Group -> {
                                    val numChildren = historyItem.items.size
                                    val stringId = if (numChildren == 1) {
                                        R.string.history_search_group_site
                                    } else {
                                        R.string.history_search_group_sites
                                    }
                                    String.format(LocalContext.current.getString(stringId), numChildren)
                                }
                            }

                            val url = when (historyItem) {
                                is History.Regular -> historyItem.url
                                is History.Metadata -> historyItem.url
                                is History.Group -> null
                            }

                            HistoryItem(
                                historyItem,
                                interactor,
                                selectedItems!!,
                                mode!!,
                                historyItem.title,
                                bodyText,
                                url
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySectionHeader(
    text: String,
    expanded: Boolean? = null,
    onClick: () -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(FirefoxTheme.colors.layer1)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = 24.dp,
                    bottom = 16.dp,
                    start = 16.dp, end
                    = 16.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryText(
                modifier = Modifier.weight(1f),
                text = text,
                fontSize = 16.sp,
                fontFamily = FontFamily(Font(R.font.metropolis_semibold)),
                maxLines = 1,
            )

            expanded?.let {
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.ic_chevron_up else R.drawable.ic_chevron_down
                    ),
                    contentDescription = stringResource(
                        if (expanded) R.string.synced_tabs_collapse_group else R.string.synced_tabs_expand_group,
                    ),
                    tint = FirefoxTheme.colors.textPrimary,
                )
            }
        }

        Divider(color = FirefoxTheme.colors.borderPrimary)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryItem(
    historyItem: History,
    historyInteractor: HistoryInteractor,
    selectedItems: Set<History>,
    mode: HistoryFragmentState.Mode,
    titleText: String,
    bodyText: String,
    url: String?
) {
    if (mode is HistoryFragmentState.Mode.Editing) {
        historyInteractor.onModeSwitched()
    }
    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            LibrarySiteItemView(it).apply {
                url?.let {
                        url -> loadFavicon(url)
                        urlView.text = url
                } ?: run {
                    iconView.setImageResource(R.drawable.ic_multiple_tabs)
                    urlView.text = bodyText
                }
                titleView.text = titleText

                overflowView.apply {
                    setImageResource(R.drawable.ic_close)
                    contentDescription = it.getString(R.string.history_delete_item)
                    setOnClickListener {
                        val item = historyItem
                        historyInteractor.onDeleteSome(setOf(item))
                    }
                }
                setSelectionInteractor(
                    historyItem,
                    object : SelectionHolder<History> {
                        override val selectedItems: Set<History>
                            get() = selectedItems

                    }, historyInteractor
                )

                val isSelected = historyItem in selectedItems
//                Log.d("kalabak", "isSelected = $isSelected")
                changeSelected(isSelected)

                if (mode is HistoryFragmentState.Mode.Editing) {
                    overflowView.hideAndDisable()
                } else {
                    overflowView.showAndEnable()
                }
            }
        }, update = {
            val isSelected = historyItem in selectedItems
            it.changeSelected(isSelected)
            if (isSelected && mode is HistoryFragmentState.Mode.Editing) {
                it.overflowView.hideAndDisable()
            } else {
                it.overflowView.showAndEnable()
            }
            it.setSelectionInteractor(
                historyItem,
                object : SelectionHolder<History> {
                    override val selectedItems: Set<History>
                        get() = selectedItems

                }, historyInteractor
            )
        }
    )
}

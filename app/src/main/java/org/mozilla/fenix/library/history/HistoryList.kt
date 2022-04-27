package org.mozilla.fenix.library.history

import android.view.ViewGroup
import android.widget.ImageView
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import kotlinx.coroutines.flow.Flow
import mozilla.components.lib.state.ext.observeAsComposableState
import org.mozilla.fenix.R
import org.mozilla.fenix.compose.Favicon
import org.mozilla.fenix.compose.PrimaryText
import org.mozilla.fenix.compose.SecondaryText
import org.mozilla.fenix.ext.components
import org.mozilla.fenix.ext.hideAndDisable
import org.mozilla.fenix.ext.loadIntoView
import org.mozilla.fenix.ext.showAndEnable
import org.mozilla.fenix.library.LibrarySiteItemView
import org.mozilla.fenix.selection.SelectionHolder
import org.mozilla.fenix.theme.FirefoxTheme
import java.util.*

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HistoryList(
    history: Flow<PagingData<History>>,
    historyStore: HistoryFragmentStore,
    interactor: HistoryInteractor
) {
    val historyItems: LazyPagingItems<History> = history.collectAsLazyPagingItems()
    // TODO somewhere here we should save the state to compare it with the old one
    // in case it has changed, we switch to modify
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

    LazyColumn {

        val itemCount = historyItems.itemCount
        val headerMap = mutableMapOf<Int, HeaderViewItem>()
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
                    headerMap.put(index, HeaderViewItem(text, collapsedHeaders[historyItem.historyTimeGroup]!!))
                }
            }
        }

        for (index in 0 until itemCount) {
            val historyItem = historyItems.peek(index)
            if (historyItem != null) {

                if (headerMap.containsKey(index)) {
                    val headerItem = headerMap[index]!!
                    stickyHeader(key = index) {
                        HistorySectionHeader(
                            headerItem.text,
                            expanded = headerItem.isExpanded
                        ) {
                            collapsedHeaders[historyItem.historyTimeGroup]?.let {
                                collapsedHeaders[historyItem.historyTimeGroup] = !it
                            }
                        }
                    }
                }
                item {
                    val historyItem = historyItems[index]!!

                    val collapsedHeader = collapsedHeaders[historyItem.historyTimeGroup] == true
                    val pendingDeletion = pendingDeletionIds?.contains(historyItem.visitedAt) == true
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
                            url,
                            onClick = {

                            },
                            onLongClick = {

                            },
                            onDelete = {
                                interactor.onDeleteSome(setOf(historyItem))
                            }
                        )
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
                .padding(top = 16.dp, bottom = 8.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PrimaryText(
                text = text,
                fontSize = 14.sp,
                fontFamily = FontFamily(Font(R.font.metropolis_semibold)),
                maxLines = 1,
            )

            expanded?.let {
                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(
                        if (expanded) R.drawable.ic_chevron_down else R.drawable.ic_chevron_up
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
    url: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit
) {
    if (mode is HistoryFragmentState.Mode.Editing) {
        historyInteractor.onModeSwitched()
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = {
            LibrarySiteItemView(it).apply {
                url?.let { url -> loadFavicon(url) }
                titleView.text = titleText
                urlView.text = url
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
        })
}

data class HeaderViewItem(val text: String, val isExpanded: Boolean)

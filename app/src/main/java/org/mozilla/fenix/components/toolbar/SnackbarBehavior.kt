/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.components.toolbar

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.Snackbar
import mozilla.components.browser.toolbar.BrowserToolbar
import mozilla.components.browser.toolbar.behavior.ToolbarPosition

private const val SMALL_ELEVATION_CHANGE = 0.01f

class SnackbarBehavior(
    val context: Context?,
    attrs: AttributeSet?,
    private val toolbarPosition: ToolbarPosition
) : CoordinatorLayout.Behavior<BrowserToolbar>(context, attrs) {

    override fun layoutDependsOn(parent: CoordinatorLayout, child: BrowserToolbar, dependency: View): Boolean {
        if (toolbarPosition == ToolbarPosition.BOTTOM && dependency is Snackbar.SnackbarLayout) {
            positionSnackbar(child, dependency)
        }

        return super.layoutDependsOn(parent, child, dependency)
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun positionSnackbar(child: View, snackbarLayout: Snackbar.SnackbarLayout) {
        val params = snackbarLayout.layoutParams as CoordinatorLayout.LayoutParams

        // Position the snackbar above the toolbar so that it doesn't overlay the toolbar.
        params.anchorId = child.id
        params.anchorGravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        params.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

        snackbarLayout.layoutParams = params

        // In order to avoid the snackbar casting a shadow on the toolbar we adjust the elevation of the snackbar here.
        // We still place it slightly behind the toolbar so that it will not animate over the toolbar but instead pop
        // out from under the toolbar.
        snackbarLayout.elevation = child.elevation - SMALL_ELEVATION_CHANGE
    }
}

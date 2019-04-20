package com.hse.vasiliy.bcrecognition.gallery

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.selection.SelectionTracker
import com.hse.vasiliy.bcrecognition.R

class ActionBarOverlay (
    private val tracker: SelectionTracker<*>
) : ActionMode.Callback {

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mode.menuInflater.inflate(R.menu.gallery_context_menu, menu)
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        tracker.clearSelection()
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean = true

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = when (item.itemId) {
        R.id.context_delete -> {
            mode.finish()
            true
        }
        else -> false
    }
}
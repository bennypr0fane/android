/*
 * Copyright © 2013 – 2016 Ricki Hirner (bitfire web engineering).
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 */

package com.etesync.syncadapter.ui.journalviewer

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import com.etesync.syncadapter.App
import com.etesync.syncadapter.R
import com.etesync.syncadapter.model.*
import com.etesync.syncadapter.ui.JournalItemActivity
import io.requery.Persistable
import io.requery.sql.EntityDataStore

class ListEntriesFragment : ListFragment(), AdapterView.OnItemClickListener {

    private lateinit var data: EntityDataStore<Persistable>
    private lateinit var info: CollectionInfo
    private var journalEntity: JournalEntity? = null
    private var asyncTask: AsyncTask<*, *, *>? = null

    private var emptyTextView: TextView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        data = (context!!.applicationContext as App).data
        info = arguments!!.getSerializable(EXTRA_COLLECTION_INFO) as CollectionInfo
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        activity!!.title = info.displayName
        val view = inflater.inflate(R.layout.journal_viewer_list, container, false)

        //This is instead of setEmptyText() function because of Google bug
        //See: https://code.google.com/p/android/issues/detail?id=21742
        emptyTextView = view.findViewById<View>(android.R.id.empty) as TextView
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        asyncTask = JournalFetch().execute()

        listView.onItemClickListener = this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (asyncTask != null)
            asyncTask!!.cancel(true)
    }

    override fun onItemClick(parent: AdapterView<*>, view: View, position: Int, id: Long) {
        val entry = listAdapter.getItem(position) as EntryEntity
        startActivity(JournalItemActivity.newIntent(context!!, info, entry.content))
    }

    internal inner class EntriesListAdapter(context: Context) : ArrayAdapter<EntryEntity>(context, R.layout.journal_viewer_list_item) {

        override fun getView(position: Int, v: View?, parent: ViewGroup): View {
            var v = v
            if (v == null)
                v = LayoutInflater.from(context).inflate(R.layout.journal_viewer_list_item, parent, false)

            val entryEntity = getItem(position)

            val tv = v!!.findViewById<View>(R.id.title) as TextView

            // FIXME: hacky way to make it show sensible info
            val info = journalEntity!!.info
            setJournalEntryView(v, info, entryEntity!!.content)

            return v
        }
    }

    private inner class JournalFetch : AsyncTask<Void, Void, List<EntryEntity>>() {

        override fun doInBackground(vararg voids: Void): List<EntryEntity> {
            journalEntity = JournalModel.Journal.fetch(data, info.getServiceEntity(data), info.uid)
            return data.select(EntryEntity::class.java).where(EntryEntity.JOURNAL.eq(journalEntity)).orderBy(EntryEntity.ID.desc()).get().toList()
        }

        override fun onPostExecute(result: List<EntryEntity>) {
            val listAdapter = EntriesListAdapter(context!!)
            setListAdapter(listAdapter)

            listAdapter.addAll(result)

            emptyTextView!!.text = getString(R.string.journal_entries_list_empty)
        }
    }

    companion object {
        protected val EXTRA_COLLECTION_INFO = "collectionInfo"

        fun newInstance(info: CollectionInfo): ListEntriesFragment {
            val frag = ListEntriesFragment()
            val args = Bundle(1)
            args.putSerializable(EXTRA_COLLECTION_INFO, info)
            frag.arguments = args
            return frag
        }

        private fun getLine(content: String?, prefix: String): String? {
            var content: String? = content ?: return null

            val start = content!!.indexOf(prefix)
            if (start >= 0) {
                val end = content.indexOf("\n", start)
                content = content.substring(start + prefix.length, end)
            } else {
                content = null
            }
            return content
        }

        fun setJournalEntryView(v: View, info: CollectionInfo, syncEntry: SyncEntry) {

            var tv = v.findViewById<View>(R.id.title) as TextView

            // FIXME: hacky way to make it show sensible info
            val fullContent = syncEntry.content
            val prefix: String
            if (info.type == CollectionInfo.Type.CALENDAR) {
                prefix = "SUMMARY:"
            } else {
                prefix = "FN:"
            }
            var content = getLine(fullContent, prefix)
            content = content ?: "Not found"
            tv.text = content

            tv = v.findViewById<View>(R.id.description) as TextView
            content = getLine(fullContent, "UID:")
            content = "UID: " + (content ?: "Not found")
            tv.text = content

            val action = v.findViewById<View>(R.id.action) as ImageView
            when (syncEntry.action) {
                SyncEntry.Actions.ADD -> action.setImageResource(R.drawable.action_add)
                SyncEntry.Actions.CHANGE -> action.setImageResource(R.drawable.action_change)
                SyncEntry.Actions.DELETE -> action.setImageResource(R.drawable.action_delete)
            }
        }
    }
}
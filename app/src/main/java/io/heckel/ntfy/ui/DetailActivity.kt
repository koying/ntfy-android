package io.heckel.ntfy.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import io.heckel.ntfy.R
import io.heckel.ntfy.app.Application
import io.heckel.ntfy.data.Notification
import io.heckel.ntfy.data.topicShortUrl

class DetailActivity : AppCompatActivity() {
    private val viewModel by viewModels<DetailViewModel> {
        DetailViewModelFactory((application as Application).repository)
    }
    private var subscriptionId: Long = 0L // Set in onCreate()
    private var subscriptionTopic: String = "" // Set in onCreate()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.detail_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show 'Back' button

        // Get extras required for the return to the main activity
        subscriptionId = intent.getLongExtra(MainActivity.EXTRA_SUBSCRIPTION_ID, 0)
        subscriptionTopic = intent.getStringExtra(MainActivity.EXTRA_SUBSCRIPTION_TOPIC) ?: return

        // Set title
        val subscriptionBaseUrl = intent.getStringExtra(MainActivity.EXTRA_SUBSCRIPTION_BASE_URL) ?: return
        title = topicShortUrl(subscriptionBaseUrl, subscriptionTopic)

        // Update main list based on viewModel (& its datasource/livedata)
        val noEntriesText: View = findViewById(R.id.detail_no_notifications_text)
        val adapter = DetailAdapter { notification -> onNotificationClick(notification) }
        val mainList: RecyclerView = findViewById(R.id.detail_notification_list)
        mainList.adapter = adapter

        viewModel.list(subscriptionId).observe(this) {
            it?.let {
                adapter.submitList(it as MutableList<Notification>)
                if (it.isEmpty()) {
                    mainList.visibility = View.GONE
                    noEntriesText.visibility = View.VISIBLE
                } else {
                    mainList.visibility = View.VISIBLE
                    noEntriesText.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.detail_action_bar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.detail_menu_delete -> {
                onDeleteClick()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onDeleteClick() {
        val builder = AlertDialog.Builder(this)
        builder
            .setMessage(R.string.detail_delete_dialog_message)
            .setPositiveButton(R.string.detail_delete_dialog_permanently_delete) { _, _ ->
                // Return to main activity
                val result = Intent()
                    .putExtra(MainActivity.EXTRA_SUBSCRIPTION_ID, subscriptionId)
                    .putExtra(MainActivity.EXTRA_SUBSCRIPTION_TOPIC, subscriptionTopic)
                setResult(RESULT_OK, result)
                finish()

                // Delete notifications
                viewModel.removeAll(subscriptionId)
            }
            .setNegativeButton(R.string.detail_delete_dialog_cancel) { _, _ -> /* Do nothing */ }
            .create()
            .show()
    }

    private fun onNotificationClick(notification: Notification) {
        println("clicked " + notification.id)
    }
}
package app.septs.nfctextbeam

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.NfcEvent
import android.os.Bundle
import android.provider.Settings
import android.support.v4.content.FileProvider
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.widget.EditText
import android.widget.Toast
import java.io.File


class MainActivity : AppCompatActivity(), NfcAdapter.CreateNdefMessageCallback {
    private lateinit var mContent: EditText
    private var mNFCAdapter: NfcAdapter? = null

    companion object {
        private const val SAVED_TEXT = "SAVED_TEXT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mContent = this.findViewById(R.id.content)
        mContent.addTextChangedListener(PlainTextWatcher())
        mNFCAdapter = NfcAdapter.getDefaultAdapter(this)

        if (mNFCAdapter == null) {
            mContent.setText(R.string.nfc_unavailable)
            mContent.gravity = Gravity.CENTER
            mContent.isEnabled = false
        } else {
            mNFCAdapter?.setNdefPushMessageCallback(this, this)
        }
    }

    override fun createNdefMessage(event: NfcEvent?): NdefMessage {
        val packageName = applicationContext.packageName
        val payload = mContent.text.toString()
        val mimeType = "application/$packageName.payload"
        return NdefMessage(
            arrayOf<NdefRecord>(
                NdefRecord(
                    NdefRecord.TNF_MIME_MEDIA,
                    mimeType.toByteArray(Charsets.UTF_8),
                    ByteArray(0),
                    payload.toByteArray(Charsets.UTF_8)
                ),
                NdefRecord.createApplicationRecord(packageName)
            )
        )
    }

    override fun onNewIntent(intent: Intent?) {
        setIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        val editor = getPreferences(Context.MODE_PRIVATE).edit()
        editor.putString(SAVED_TEXT, mContent.text.toString())
        editor.apply()
    }

    override fun onResume() {
        super.onResume()
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type == "text/plain") {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    mContent.setText(sharedText)
                }
            }
            NfcAdapter.ACTION_NDEF_DISCOVERED -> {
                val messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES)
                val payload = (messages[0] as NdefMessage).records[0].payload
                mContent.setText(payload.toString(Charsets.UTF_8))
            }
        }
        val prefs = getPreferences(Context.MODE_PRIVATE)
        val restoredText = prefs.getString(SAVED_TEXT, "")
        restoredText?.let {
            if (it.isEmpty()) {
                return@let
            }
            val editable = mContent.text
            val content = editable.toString()
            if (editable.isEmpty()) {
                editable.append(it)
            } else if (it.trim() != content.trim()) {
                editable.append("\n\n")
                editable.append(it)
            }
        }
        mContent.setSelection(mContent.text.length)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (mNFCAdapter == null) {
            return super.onCreateOptionsMenu(menu)
        }
        menuInflater.inflate(R.menu.options, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_clear -> {
                mContent.setText("")
            }
            R.id.menu_share -> {
                if (mContent.text.isNotEmpty()) {
                    val intent = Intent().apply {
                        this.type = "text/plain"
                        this.action = Intent.ACTION_SEND
                        this.putExtra(Intent.EXTRA_TEXT, mContent.text.toString())
                    }
                    startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
                }
            }
            R.id.menu_copy_to_clipboard -> {
                if (mContent.text.isNotEmpty()) {
                    (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager).apply {
                        val label = "nfc-beam"
                        val payload = mContent.text.toString()
                        this.primaryClip = ClipData.newPlainText(label, payload)
                    }
                    Toast.makeText(this, getString(R.string.copies_to_clipboard), Toast.LENGTH_LONG).show()
                }
            }
            R.id.menu_beam_settings -> {
                startActivity(Intent(Settings.ACTION_NFCSHARING_SETTINGS))
            }
            R.id.menu_share_self -> let {
                val app = applicationContext.applicationInfo
                val appName = getString(app.labelRes)
                    .replace("[ /-]".toRegex(), "_")
                val clonedFile = File(applicationContext.cacheDir, "$appName.apk")
                if (clonedFile.exists()) {
                    clonedFile.delete()
                }
                File(app.sourceDir).copyTo(clonedFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    val target = FileProvider.getUriForFile(
                        applicationContext,
                        "${app.packageName}.FileProvider",
                        clonedFile
                    )
                    this.type = "*/*"
                    this.putExtra(Intent.EXTRA_STREAM, target)
                }
                startActivity(Intent.createChooser(intent, getString(R.string.share_via)))
            }
            R.id.menu_open_homepage -> {
                startActivity(Intent(Intent.ACTION_VIEW).apply {
                    this.data = Uri.parse(getString(R.string.project_link))
                })
            }
            else -> {
                return super.onOptionsItemSelected(item)
            }
        }
        return true
    }
}

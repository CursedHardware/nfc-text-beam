package app.septs.nfctextbeam

import android.text.Editable
import android.text.TextWatcher
import android.text.style.CharacterStyle

class PlainTextWatcher : TextWatcher {
    override fun afterTextChanged(editable: Editable?) {
        editable?.let {
            val spans = it.getSpans(
                0,
                it.length,
                CharacterStyle::class.java
            )
            spans.forEach(it::removeSpan)
        }
    }

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
    }
}
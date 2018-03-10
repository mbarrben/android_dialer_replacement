package com.mbarrben.dialer

import android.os.Bundle
import android.util.Log
import android.view.View
import com.mbarrben.dialer.R.id.buttonAnswer
import com.mbarrben.dialer.R.id.buttonHangup
import com.mbarrben.dialer.R.id.textDisplayName
import com.mbarrben.dialer.R.id.textDuration
import com.mbarrben.dialer.R.id.textStatus
import io.reactivex.Observable
import io.reactivex.disposables.Disposables
import java.util.concurrent.TimeUnit

class CallActivity : AppCompatActivity() {

  companion object {
    private const val LOG_TAG = "CallActivity"
  }

  private var updatesDisposable = Disposables.empty()
  private var timerDisposable = Disposables.empty()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_call)

    hideBottomNavigationBar()

    buttonHangup.setOnClickListener { CallManager.cancelCall() }
    buttonAnswer.setOnClickListener { CallManager.acceptCall() }
  }

  override fun onResume() {
    super.onResume()
    updatesDisposable = CallManager.updates()
        .doOnEach { Log.i(LOG_TAG, "updated call: $it") }
        .doOnError { throwable -> Log.e(LOG_TAG, "Error processing call", throwable) }
        .subscribe { updateView(it) }
  }

  private fun updateView(gsmCall: GsmCall) {
    textStatus.visibility = when (gsmCall.status) {
      GsmCall.Status.ACTIVE -> View.GONE
      else                  -> View.VISIBLE
    }
    textStatus.text = when (gsmCall.status) {
      GsmCall.Status.CONNECTING   -> "Connecting…"
      GsmCall.Status.DIALING      -> "Calling…"
      GsmCall.Status.RINGING      -> "Incoming call"
      GsmCall.Status.ACTIVE       -> ""
      GsmCall.Status.DISCONNECTED -> "Finished call"
      GsmCall.Status.UNKNOWN      -> ""
    }
    textDuration.visibility = when (gsmCall.status) {
      GsmCall.Status.ACTIVE -> View.VISIBLE
      else                  -> View.GONE
    }
    buttonHangup.visibility = when (gsmCall.status) {
      GsmCall.Status.DISCONNECTED -> View.GONE
      else                        -> View.VISIBLE
    }

    if (gsmCall.status == GsmCall.Status.DISCONNECTED) {
      buttonHangup.postDelayed({ finish() }, 3000)
    }

    when (gsmCall.status) {
      GsmCall.Status.ACTIVE       -> startTimer()
      GsmCall.Status.DISCONNECTED -> stopTimer()
      else                        -> Unit
    }

    textDisplayName.text = gsmCall.displayName ?: "Unknown"

    buttonAnswer.visibility = when (gsmCall.status) {
      GsmCall.Status.RINGING -> View.VISIBLE
      else                   -> View.GONE
    }
  }

  override fun onPause() {
    super.onPause()
    updatesDisposable.dispose()
  }

  private fun hideBottomNavigationBar() {
    window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
  }

  private fun startTimer() {
    timerDisposable = Observable.interval(1, TimeUnit.SECONDS)
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { textDuration.text = it.toDurationString() }
  }

  private fun stopTimer() {
    timerDisposable.dispose()
  }

  private fun Long.toDurationString() = String.format("%02d:%02d:%02d", this / 3600, (this % 3600) / 60, (this % 60))
}

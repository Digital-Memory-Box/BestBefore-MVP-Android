package com.dmb.bestbefore.ui.screens.profile

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.DecoratedBarcodeView

/**
 * QR reader activity — wraps ZXing's camera scanner and returns the scanned text
 * as the "SCAN_RESULT" string extra on RESULT_OK. Handles both HTTPS URLs and
 * legacy bestbefore:// custom scheme QR codes.
 */
class QrScannerActivity : AppCompatActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val rootLayout = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
        }

        barcodeView = DecoratedBarcodeView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            // Hide the default low-positioned status view
            statusView.visibility = View.GONE
        }
        rootLayout.addView(barcodeView)

        // Top Status Header Pill: "Scan a BestBefore room QR code"
        val topStatusText = TextView(this).apply {
            text = "Scan a BestBefore room QR code"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            setTypeface(typeface, Typeface.BOLD)
            gravity = Gravity.CENTER

            val hPad = (20 * resources.displayMetrics.density).toInt()
            val vPad = (10 * resources.displayMetrics.density).toInt()
            setPadding(hPad, vPad, hPad, vPad)

            background = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 24 * resources.displayMetrics.density
                setColor(Color.parseColor("#CC121212"))
            }

            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                topMargin = (72 * resources.displayMetrics.density).toInt()
            }
        }
        rootLayout.addView(topStatusText)

        // Top Close Button ("✕")
        val closeButton = TextView(this).apply {
            text = "✕"
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            gravity = Gravity.CENTER

            val size = (40 * resources.displayMetrics.density).toInt()
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#AA1C1C1E"))
            }

            layoutParams = FrameLayout.LayoutParams(size, size).apply {
                gravity = Gravity.TOP or Gravity.START
                val margin = (24 * resources.displayMetrics.density).toInt()
                topMargin = (68 * resources.displayMetrics.density).toInt()
                marginStart = margin
            }

            setOnClickListener { finish() }
        }
        rootLayout.addView(closeButton)

        setContentView(rootLayout)

        barcodeView.decodeContinuous(object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                // Return the raw scanned text to the caller
                val intent = android.content.Intent().apply {
                    putExtra("SCAN_RESULT", result.text)
                }
                setResult(android.app.Activity.RESULT_OK, intent)
                finish()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        barcodeView.resume()
    }

    override fun onPause() {
        super.onPause()
        barcodeView.pause()
    }
}


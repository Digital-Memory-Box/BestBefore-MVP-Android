package com.dmb.bestbefore.ui.screens.profile

import android.os.Bundle
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

        barcodeView = DecoratedBarcodeView(this)
        setContentView(barcodeView)

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

        barcodeView.statusView.text = "Scan a BestBefore room QR code"
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

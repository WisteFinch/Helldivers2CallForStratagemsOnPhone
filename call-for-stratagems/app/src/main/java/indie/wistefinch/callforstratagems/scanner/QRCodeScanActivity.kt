package indie.wistefinch.callforstratagems.scanner

import android.content.Intent
import com.google.zxing.Result
import com.king.camera.scan.AnalyzeResult
import com.king.camera.scan.CameraScan
import com.king.camera.scan.analyze.Analyzer
import com.king.zxing.BarcodeCameraScanActivity
import com.king.zxing.DecodeConfig
import com.king.zxing.DecodeFormatManager
import com.king.zxing.analyze.QRCodeAnalyzer
import indie.wistefinch.callforstratagems.R

class QRCodeScanActivity : BarcodeCameraScanActivity() {
    override fun initCameraScan(cameraScan: CameraScan<Result>) {
        super.initCameraScan(cameraScan)
    }

    override fun createAnalyzer(): Analyzer<Result> {
        // Init decoder.
        val decodeConfig = DecodeConfig()
        decodeConfig.setHints(DecodeFormatManager.QR_CODE_HINTS)
            .setAreaRectRatio(1f)
        return QRCodeAnalyzer(decodeConfig)
    }

    override fun getLayoutId(): Int {
        return R.layout.activity_qrcode_scan
    }

    override fun onScanResultCallback(result: AnalyzeResult<Result>) {
        // Stop analyze.
        cameraScan.setAnalyzeImage(false)
        // Return result.
        val intent = Intent()
        intent.putExtra(CameraScan.SCAN_RESULT, result.result.text)
        setResult(RESULT_OK, intent)
        finish()
    }
}
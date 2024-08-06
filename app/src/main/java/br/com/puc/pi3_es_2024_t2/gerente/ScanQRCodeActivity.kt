package br.com.puc.pi3_es_2024_t2.gerente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityScanQrcodeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity responsável por escanear o QR Code
 *
 * REFERÊNCIAS UTILIZADAS:
 * https://developers.google.com/ml-kit/vision/barcode-scanning/android?hl=pt-br
 * https://www.simplifiedcoding.net/android-qr-code-scanner-example/
 * @author Guilherme
 * */
class ScanQRCodeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanQrcodeBinding

    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private lateinit var processCameraProvider: ProcessCameraProvider
    private lateinit var cameraPreview: Preview
    private lateinit var imgCaptureExecutor: ExecutorService
    private lateinit var imageAnalysis: ImageAnalysis
    private var qrCodeDetected = false
    private val codeRequest = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanQrcodeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        // clicou em voltar no botão voltar da activity
        binding.btnArrowBack.setOnClickListener {
            finish()
        }

        // ciclou em escanear QR Code - iniciar a câmera
        binding.btnScanQrCode.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

                // A permissão não foi concedida, solicite ao usuário.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    codeRequest)

            } else {
                binding.btnScanQrCode.visibility = View.GONE
                startCamera()
            }
        }

        // navegação da bottom bar
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    finish()
                    true
                }

                else -> false
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            codeRequest -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    binding.tvScan.visibility = View.GONE
                    binding.btnScanQrCode.visibility = View.GONE
                    startCamera()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // Usuário negou com "Não perguntar novamente" ou é a primeira vez que negou.
                        binding.tvScan.visibility = View.VISIBLE
                        binding.tvScan.text =
                            getString(R.string.liberar_nas_configuracoes_preview)
                    } else {
                        // Usuário negou sem marcar "Não perguntar novamente".
                        binding.tvScan.visibility = View.VISIBLE
                        binding.tvScan.text =
                            getString(R.string.permissao_para_mostrar_a_preview)
                    }
                }
                return
            }
        }
    }


    /**
     * Função responsável por inicializar o preview da câmera e capturar a imagem.
     * */
    private fun startCamera() {
        cameraProviderFuture.addListener(
            {
                processCameraProvider = cameraProviderFuture.get()
                bindCameraPreview()
                bindInputAnalyser()

                try {
                    // abrir o preview
                } catch (e: Exception) {
                    binding.btnScanQrCode.visibility = View.VISIBLE
                    Log.e("CameraPreview", "Falha ao abrir a câmera.")
                }
            },
            // quem vai cuidar das threads de execução da câmera
            ContextCompat.getMainExecutor(this)
        )
    }

    /**
     * Função responsável por vincular o preview da câmera ao ciclo de vida da câmera.
     * */
    private fun bindCameraPreview() {
        // construindo o objeto responsável pelo preview da câmera
        cameraPreview = Preview.Builder()
            .setTargetRotation(binding.cameraPreview.display.rotation)
            .build()

        // definindo a superfície que o câmera preview vai ser hospedado
        cameraPreview.setSurfaceProvider(binding.cameraPreview.surfaceProvider)

        // vinculando camera preview ao ciclo de vida da câmera
        processCameraProvider.bindToLifecycle(this, cameraSelector, cameraPreview)
    }

    /**
     * Função responsável por vincular o objeto do tipo ImageAnalysis - que têm como objetivo
     * analisar determinada imagem - ao ciclo de vida da câmera
     * */
    private fun bindInputAnalyser() {
        // inicializando o leitor de código de barras para receber o formato QR Code
        val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )

        // construindo o objeto ImageAnalysis
        imageAnalysis = ImageAnalysis.Builder()
            .setTargetRotation(binding.cameraPreview.display.rotation)
            .build()

        // definindo um analisador para receber e processar o QR Code
        imageAnalysis.setAnalyzer(imgCaptureExecutor) { imageProxy ->
            processImageProxy(barcodeScanner, imageProxy)
        }

        try {
            // vinculando o analisador de imagem ao ciclo de vida da câmera
            processCameraProvider.bindToLifecycle(this, cameraSelector, imageAnalysis)
        } catch (illegalStateException: IllegalStateException) {
            // COLOCAR UMA SNACKBAR INFORMANDO O ERRO AQUI
            binding.btnScanQrCode.visibility = View.VISIBLE
            val snackbar = Snackbar.make(binding.root, "${illegalStateException.message}", Snackbar.LENGTH_SHORT)
            snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
            val view = snackbar.view
            val params = view.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP
            view.layoutParams = params
            snackbar.show()
        } catch (illegalArgumentException: IllegalArgumentException) {
            // COLOCAR UMA SNACKBAR INFORMANDO O ERRO AQUI
            binding.btnScanQrCode.visibility = View.VISIBLE
            val snackbar = Snackbar.make(binding.root, "${illegalArgumentException.message}", Snackbar.LENGTH_SHORT)
            snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
            val view = snackbar.view
            val params = view.layoutParams as FrameLayout.LayoutParams
            params.gravity = Gravity.TOP
            view.layoutParams = params
            snackbar.show()
        }
    }

    /**
     * Função responsável por processar as imagens, e partir daí verificar se alguma delas são o
     * QR Code. Caso seja um QR Code irá iniciar a Activity para escolher o número de pessoas que
     * irão utilizar aquele armário.
     * @param barcodeScanner leitor código de barras
     * @param imageProxy imagem recebida no analisador
     * */
    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->

                // Variável de controle é true e o barcodes não é vazio
                if (!qrCodeDetected && barcodes.isNotEmpty()) {
                    for (barcode in barcodes) {

                        // método que retorna o formato do barcode
                        val format = barcode.format

                        // Verifica se o formato do código de barras é QR code
                        if (format == Barcode.FORMAT_QR_CODE) {

                            qrCodeDetected = true
                            startActivity(
                                Intent(this, PessoasNumberActivity::class.java)
                                    .putExtra("uid", barcode.rawValue)
                            )
                            finish()
                            break
                        }
                    }
                }
            }
            .addOnFailureListener {
                // COLOCAR UMA SNACKBAR INFORMANDO O ERRO AQUI
                binding.btnScanQrCode.visibility = View.VISIBLE
                Log.e("Failed to process", it.message ?: it.toString())
                val snackbar = Snackbar.make(binding.root, "Erro ao escanear. Por favor tente novamente.", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(getResources().getColor(R.color.red_error))
                val view = snackbar.view
                val params = view.layoutParams as FrameLayout.LayoutParams
                params.gravity = Gravity.TOP
                view.layoutParams = params
                snackbar.show()
            }.addOnCompleteListener {
                imageProxy.close()
            }
    }
}
package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivityCameraBinding
import com.google.common.util.concurrent.ListenableFuture
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Activity responsável por tirar as fotos. Nela há o controle de ciclo de vida da câmera,
 * qual câmera será usada, será o exbido o preview da câmera.
 * ```
 * Além disso, ao tirar a foto será enviada para NfcHostActivity:
 * - 1) Uma variável typeAction com o valor 1 que é para indicar que o Fragment
 * WriteTagFragment será acionado, ou seja, escreverá na tag NFC;
 * -2) A variável dir-photo que é onde a imagem foi guardada no celular do gerente.
 * ```
 * */
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding

    // Processamento de imagem (não permitir ou controlar melhor o estado do driver da camera)
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>

    // Seleciona se deseja a camera frontal ou traseira
    private lateinit var cameraSelector: CameraSelector

    //Imagem capturada
    private var imageCapture : ImageCapture? = null

    // Executor de thread separado
    private lateinit var imgCaptureExecutor: ExecutorService

    private var uid: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        uid = intent.getStringExtra("uid").toString()

        //chamar metodo startCamera()
        startCamera()

        // evento do click no botão para chamar o metodo takePhoto
        binding.btnTakePhoto.setOnClickListener {
            takePhoto()
        }
    }

    private fun startCamera() {

        cameraProviderFuture.addListener({

            // definindo o objeto que contém a captura da imagem
            imageCapture = ImageCapture.Builder().build()

            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.cameraPreview.surfaceProvider)
            }
            try {
                //Abrir o preview
                // desvinculando tudo do ciclo de vida da camerax
                cameraProvider.unbindAll()

                // vincunlando ao ciclo de vida da camera: a camera que será usada, a superfície
                // do preview da camera, e a imagem capturada
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            }catch (e: Exception) {
                Log.e("CameraPreview", "Falha ao abrir a camera")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        //Codigo para tirar foto
        imageCapture?.let {
            // nome do arquivo para gravar a foto
            val fileName = "JPEG_${System.currentTimeMillis()}.jpeg"
            val file = File(externalMediaDirs[0], fileName)


            val outPutFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()

            it.takePicture(
                outPutFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Log.i("CameraPreview", "A imagem foi salva no diretorio: ${file.toURI()}")
                        val intent = Intent(this@CameraActivity, NfcHostActivity::class.java)
                        intent.putExtra("dir-photo", "$file")
                        intent.putExtra("typeAction", 1)
                        intent.putExtra("uid", uid)
                        startActivity(intent)
                        finish()
                    }

                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(binding.root.context, "Error ao salvar foto. ", Toast.LENGTH_LONG).show()
                        Log.e("CameraPreview", "Execeção ao gravar o arquivo da foto: $exception")
                    }
                })
        }
    }

}
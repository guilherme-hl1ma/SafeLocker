package br.com.puc.pi3_es_2024_t2.gerente

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityTakePhotoBinding
import com.google.android.material.snackbar.Snackbar

/**
 * Activity que precede a CameraActivity. Isto é, ela é responsável por exibir ao usuário o botão
 * TIRAR FOTO que na prática é para abrir a CameraActivity
 * */
class TakePhotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTakePhotoBinding
    private var codeRequest = 100
    private var uid : String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakePhotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        uid = intent.getStringExtra("uid").toString()

        binding.btnTakePhoto.setOnClickListener {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

                // A permissão não foi concedida, solicite ao usuário.
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.CAMERA),
                    codeRequest)

            } else {
                startActivity(Intent(this, CameraActivity::class.java).putExtra("uid", uid))
                finish()
            }
        }

        binding.btnArrowBack.setOnClickListener {
            finish()
        }

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    // iniciar uma nova task a partir da GerenteActivity
                    // limpar a fila de tarefas
                    startActivity(Intent(this, GerenteActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    })
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
                    startActivity(Intent(this, CameraActivity::class.java).putExtra("uid", uid))
                    finish()
                } else {
                    if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                        // Usuário negou com "Não perguntar novamente" ou é a primeira vez que negou.
                        val snackbar = Snackbar.make(binding.root, "Você deverá liberar a permissão nas configurações para poder visualizar a camera.", Snackbar.LENGTH_SHORT)
                        snackbar.setBackgroundTint(getResources().getColor(R.color.azul_primary))
                        val view = snackbar.view
                        val params = view.layoutParams as FrameLayout.LayoutParams
                        params.gravity = Gravity.TOP
                        view.layoutParams = params
                        snackbar.show()
                    } else {
                        // Usuário negou sem marcar "Não perguntar novamente".
                        val snackbar = Snackbar.make(binding.root, "Permissão necessária para mostrar a camera.", Snackbar.LENGTH_SHORT)
                        snackbar.setBackgroundTint(getResources().getColor(R.color.azul_primary))
                        val view = snackbar.view
                        val params = view.layoutParams as FrameLayout.LayoutParams
                        params.gravity = Gravity.TOP
                        view.layoutParams = params
                        snackbar.show()
                    }
                }
                return
            }
        }
    }
}
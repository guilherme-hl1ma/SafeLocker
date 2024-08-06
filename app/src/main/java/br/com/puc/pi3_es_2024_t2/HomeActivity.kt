package br.com.puc.pi3_es_2024_t2

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivityHomeBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var functionsNuvem: FunctionsNuvem
    private lateinit var functions: FirebaseFunctions
    private var markerJson: String? = null
    private lateinit var rentLockerFragment: RentLockerFragment

    private var storedFlagNotificationQr: String? = null
    private var dataQrCode: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth
        functions = Firebase.functions("southamerica-east1")
        functionsNuvem = FunctionsNuvem()

        binding.progressBarHome.visibility = View.VISIBLE

        // mensagem que deve ser exibida na snackbar, quando o cartão for cadastrado
        val message = intent.getStringExtra("EXTRA_SNACKBAR_MESSAGE")

        // cor que vai ser definida como background da snackbar
        val color = intent.getIntExtra("EXTRA_SNACKBAR_COLOR", -1)

        // cartão cadastrado com sucesso
        successRegisterCard(message, color)

        // gerenciar a bottom bar
        navegationHome()

        verificationCard()

        markerJson = intent.getStringExtra("lockerPosition")
    }

    override fun onStart() {
        super.onStart()

        storedFlagNotificationQr = intent.getStringExtra("isQRFragment")
        dataQrCode = intent.getStringExtra("dataQrCode")

        // Verifica se há uma intenção da notificação e se é para exibir a QRActivity
        if (storedFlagNotificationQr == "true") {
            startActivity(Intent(this, QRActivity::class.java)
                .putExtra("dataQrCode", dataQrCode))
            // Remove a StringExtra do Intent
            intent.removeExtra("isQRFragment")
            intent.removeExtra("dataQrCode")
        }

        if (auth.currentUser == null) {
            Toast.makeText(this, "Faça login para acessar essa tela.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, InitialActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

    }

    private fun verificationCard() {
        functionsNuvem.buscaCartao(auth, functions) { possuiCartao ->
            binding.progressBarHome.visibility = View.GONE
            if (possuiCartao) {
                // Limpa a intenção para evitar que a lógica seja executada novamente na
                // próxima vez que onStart() for chamado
                intent.removeExtra("isQRFragment")
                rentLockerFragment = RentLockerFragment().apply {
                    arguments = Bundle().apply {
                        putString("lockerPosition", markerJson)
                    }
                }

                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, rentLockerFragment)
                    .commitAllowingStateLoss()

            } else {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, CardNotFoundFragment())
                    .commitAllowingStateLoss()
            }
        }
    }

    private fun navegationHome() {
        binding.bottomNavigationView.selectedItemId = R.id.bottom_home
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    true
                }

                R.id.bottom_location -> {
                    startActivity(Intent(applicationContext, LocationActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                R.id.bottom_person -> {
                    startActivity(Intent(applicationContext, AccountActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    true
                }

                else -> false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        // Verificar permissões
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
        }
    }

    /**
     * Função responsável por exibir uma snackbar contendo o feedback que o cartão foi cadastrado
     * com sucesso.
     * @author Guilherme
     * */
    private fun successRegisterCard(message: String?, color: Int) {
        if (message != null && color != -1) {
            val rootView: View = findViewById(android.R.id.content)
            val snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_LONG)

            // definindo a cor do background
            snackbar.setBackgroundTint(ContextCompat.getColor(this, color))

            val view = snackbar.view
            val params = view.layoutParams as FrameLayout.LayoutParams

            // definido o parâmetro gravity como top
            params.gravity = Gravity.TOP

            // definindo que a snackbar vai aparecer no topo
            view.layoutParams = params

            snackbar.show()
        }
    }
}
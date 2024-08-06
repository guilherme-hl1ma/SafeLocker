package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import br.com.puc.pi3_es_2024_t2.gerente.GerenteActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions

class InitialActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var btnSignIn: AppCompatButton
    private lateinit var btnSignUp: AppCompatButton
    private lateinit var btnConsult: AppCompatButton

    private lateinit var functionsNuvem: FunctionsNuvem
    private lateinit var functions: FirebaseFunctions
    private lateinit var typeUser: String

    private lateinit var splashScreen: SplashScreen

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Instalar a SplashScreen e mantê-la ativa
        splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { !::typeUser.isInitialized }

        setContentView(R.layout.activity_initial)

        // Inicializar Firebase e funções da nuvem
        functions = Firebase.functions("southamerica-east1")
        auth = Firebase.auth
        functionsNuvem = FunctionsNuvem()

        // Configurar views
        btnSignIn = findViewById(R.id.btnEntrar)
        btnSignUp = findViewById(R.id.btnCadastrar)
        btnConsult = findViewById(R.id.btnConsultarLocais)

        btnSignIn.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        btnSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        btnConsult.setOnClickListener {
            startActivity(Intent(this, LocationActivity::class.java))
        }

        // Buscar tipo de usuário
        functionsNuvem.buscarTipoUsuario(auth, functions) {
            typeUser = it
            verificarTipo()
        }
    }

    private fun verificarTipo() {
        val currentUser = auth.currentUser
        if (currentUser != null && (currentUser.isEmailVerified || (currentUser.phoneNumber != null && currentUser.phoneNumber != ""))) {
            if (typeUser == "cliente") {
                startActivity(Intent(this, HomeActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            } else {
                startActivity(Intent(this, GerenteActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }
        } else if (currentUser == null) {
            // Se não houver instância auth.currentUser
            return

        } else {
            // Se o usuário não está logado ou email/telefone não confirmado, redirecione para a página de login
            startActivity(Intent(this, SignInActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}

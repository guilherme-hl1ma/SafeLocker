package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import br.com.puc.pi3_es_2024_t2.InitialActivity
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityGerenteBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class GerenteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityGerenteBinding
    private lateinit var alertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var inflater: LayoutInflater
    private lateinit var dialogView: View
    private lateinit var message: AppCompatTextView
    private lateinit var title: AppCompatTextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityGerenteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = Firebase.auth

        // gerente vai liberar a locação
        binding.btnLiberar.setOnClickListener{
            startActivity(Intent(this, ScanQRCodeActivity::class.java))
        }

        // gerente clicou em abrir armário
        binding.btnAbrirArmario.setOnClickListener{
            val intent = Intent(this, NfcHostActivity::class.java)
            intent.putExtra("typeAction", 2)
            startActivity(intent)
        }

        // navegação da bottom bar
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    true
                }

                else -> false
            }
        }

        // evento de clique no botão de sair do app
        binding.btnExit.setOnClickListener {
            // definindo o tema do dialog
            alertDialogBuilder = MaterialAlertDialogBuilder(this, R.style.ThemeOverlay_App_MaterialAlertDialogSairApp)

            // obtendo o layout inflater de exibição
            inflater = LayoutInflater.from(this)

            // inflando o layout do dialog para atual atividade
            dialogView = inflater.inflate(R.layout.dialog_layout, null)

            message = dialogView.findViewById(R.id.tvMessage)
            title = dialogView.findViewById(R.id.tvTitle)

            title.text = "Sair do Aplicativo"
            message.text = "Você realmente deseja sair do aplicativo?"

            alertDialogBuilder.setView(dialogView)

            // definindo a ação do botão para ação positiva
            alertDialogBuilder.setPositiveButton("Não") { dialog, _ ->

                // fecha o dialog
                dialog.dismiss()
            }

            // definindo a ação do botão para ação negativa
            alertDialogBuilder.setNegativeButton("Sim") { _, _ ->

                // deslogar a atual instância de Firabase.atuh
                Firebase.auth.signOut()

                /**
                 * Iniciando uma nova task a partir de InitialActivity.
                 * Zerando a fila de atividades.
                 * Adicionando a InitialActivity como atividade raiz da fila de atividades.
                 * */
                startActivity(Intent(this, InitialActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                })
            }

            // cria o dialog
            alertDialogBuilder.create()

            // mostra o dialog
            alertDialogBuilder.show()
        }
        
    }
}
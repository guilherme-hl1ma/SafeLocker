package br.com.puc.pi3_es_2024_t2

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatTextView
import br.com.puc.pi3_es_2024_t2.databinding.ActivityAccountBinding
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.firebase.functions.functions

class AccountActivity: AppCompatActivity() {

    private lateinit var binding : ActivityAccountBinding
    private lateinit var functions: FirebaseFunctions
    private lateinit var auth: FirebaseAuth
    private lateinit var alertDialogBuilder: MaterialAlertDialogBuilder
    private lateinit var inflater: LayoutInflater
    private lateinit var dialogView: View
    private lateinit var message: AppCompatTextView
    private lateinit var title: AppCompatTextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAccountBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navegationAccount()

        auth = Firebase.auth

        if (auth.currentUser == null) {
            Toast.makeText(this, "Faça login para acessar essa tela.", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, InitialActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }

        functions = Firebase.functions("southamerica-east1")
        getUserByUid()

        // evento de clique no botão de sair do app
        binding.btnSair.setOnClickListener {
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

    private fun navegationAccount(){
        binding.bottomNavigationView.selectedItemId = R.id.bottom_person
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.bottom_home -> {
                    startActivity(Intent(applicationContext, HomeActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_location -> {
                    startActivity(Intent(applicationContext, LocationActivity::class.java))
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
                    finish()
                    true
                }
                R.id.bottom_person -> {
                    true
                }
                else -> false
            }
        }
    }

    private fun getUserByUid(): Task<HttpsCallableResult> {
        val data = hashMapOf(
            "userUid" to auth.currentUser!!.uid
        )
        return functions
            .getHttpsCallable("getUserByUid")
            .call(data)
            .addOnSuccessListener { result ->
                val dados = result.data as? Map<*, *>
                val payload = dados?.get("payload") as? List<*>

                payload?.forEach { rentalUnit ->
                    if (rentalUnit is Map<*, *>) {
                        binding.tvEmail.text = rentalUnit["email"] as String
                        binding.tvNome.text = rentalUnit["nome"] as String
                        binding.tvCpf.text = rentalUnit["cpf"] as String
                        binding.tvDataDeNascimento.text = rentalUnit["dataNascimento"] as String
                        binding.tvTelefone.text = rentalUnit["telefone"] as String
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Error to read the rental units", exception.toString())
                }
        }
}
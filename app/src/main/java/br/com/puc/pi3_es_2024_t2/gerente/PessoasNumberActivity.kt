package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.puc.pi3_es_2024_t2.FunctionsNuvem
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityPessoasNumberBinding
import com.google.firebase.Firebase
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.functions

class PessoasNumberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPessoasNumberBinding
    private lateinit var functions: FirebaseFunctions
    private lateinit var functionsNuvem: FunctionsNuvem

    private var qtdTags : Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val uid = intent.getStringExtra("uid").toString()
        functions = Firebase.functions("southamerica-east1")
        functionsNuvem = FunctionsNuvem()

        binding = ActivityPessoasNumberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnArrowBack.setOnClickListener {
            startActivity(Intent(this, ScanQRCodeActivity::class.java))
            finish()
        }

        binding.radioGroup.setOnCheckedChangeListener{ _, checkedId ->
            when (checkedId) {
                binding.radioBtn1Person.id -> qtdTags = 1
                binding.radioBtn2People.id -> qtdTags = 2
            }
        }

        binding.btnProsseguir.setOnClickListener{
            if(binding.radioBtn2People.isChecked || binding.radioBtn1Person.isChecked) {
                functionsNuvem.inserirTagLocacao(functions, qtdTags, uid) {
                    startActivity(Intent(this, TakePhotoActivity::class.java).putExtra("uid", uid))
                    finish()
                }
            }
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
}

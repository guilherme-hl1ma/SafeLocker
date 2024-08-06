package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityOpenLockerConfirmationBinding

class OpenLockerConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityOpenLockerConfirmationBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityOpenLockerConfirmationBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // navegação da bottom bar
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    // Inicia uma nova tarefa a partir da GerenteActivity
                    // Ao mesmo tempo limpa a fila de tarefas atual
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
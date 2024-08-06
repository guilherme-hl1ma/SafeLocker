package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import androidx.appcompat.app.AppCompatActivity
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityRentalEndedBinding

/**
 * Activity resposável por fornecer um feedback para o usuário de que a locação foi encerrada.
 * Dessa forma, o usuário pode voltar à tela inicial.
 * */
class RentalEndedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRentalEndedBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRentalEndedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val refoundPrice = intent.getStringExtra("valorReembolso")

        binding.refoundPrice.text = "O valor a ser estornado é R$ $refoundPrice"

        // navegação da bottom bar
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

    /**
     * Quando a tecla de voltar do sistema android for pressionada iniciar uma nova Task a partir
     * da GerenteActivity
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(Intent(this, GerenteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }
}
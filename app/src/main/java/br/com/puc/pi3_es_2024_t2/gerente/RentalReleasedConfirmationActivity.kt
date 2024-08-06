package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import br.com.puc.pi3_es_2024_t2.R

/**
 * Activity para mostrar que a locação foi liberada exibindo o armário alugado e a respectiva
 * porta.
 * */
class RentalReleasedConfirmationActivity : AppCompatActivity() {

    private lateinit var btnGoToHome: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rental_released_confirmation)

        btnGoToHome = findViewById(R.id.btnGoToHome)

        btnGoToHome.setOnClickListener {
            startActivity(Intent(this, GerenteActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
    }
}
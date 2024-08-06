package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityOptionCustomerRentalStateBinding

class OptionCustomerRentalState : AppCompatActivity() {

    private lateinit var binding : ActivityOptionCustomerRentalStateBinding
    override fun onCreate(savedInstanceState: Bundle?) {

        binding = ActivityOptionCustomerRentalStateBinding.inflate(layoutInflater)

        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.btnAbrirMoment.setOnClickListener{
            startActivity(Intent(this, OpenLockerConfirmationActivity::class.java))
        }

        binding.btnEncerrarLocal.setOnClickListener {
            val intent = Intent(this, NfcHostActivity::class.java)
            intent.putExtra("typeAction", 3)
            startActivity(intent)
        }

        // navegação da bottom bar
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when(item.itemId) {
                R.id.bottom_home -> {
                    finish()
                    true
                }

                else -> false
            }
        }
    }
}
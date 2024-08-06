package br.com.puc.pi3_es_2024_t2.gerente

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import br.com.puc.pi3_es_2024_t2.R
import br.com.puc.pi3_es_2024_t2.databinding.ActivityPhotoClienteBinding

/**
 * Activity responsável por exibir a foto do cliente, para assim o gerente decidir se deve ou não
 * prosseguir. Nesse contexto, o gerente pode **PROSSEGUIR** ou **CANCELAR**.
 * */
class PhotoClienteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPhotoClienteBinding
    private lateinit var endImageView : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPhotoClienteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        endImageView = intent.getStringExtra("endImage").toString()

        // definindo a ImageView com a foto que está no endereço contido em endImageView
        binding.ivUserPhoto.setImageURI(endImageView.toUri())

        // usuário clicou em prosseguir
        // ir para a OptionCustomerRentalState
        binding.btnProsseguir.setOnClickListener{
            startActivity(Intent(this, OptionCustomerRentalState::class.java))
            finish()
        }

        // usuário clicou em cancelar
        // voltar para a GerenteActivity
        binding.btnCancelar.setOnClickListener {
            startActivity(Intent(this, GerenteActivity::class.java))
            finish()
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
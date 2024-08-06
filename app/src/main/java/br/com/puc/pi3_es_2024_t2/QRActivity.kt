package br.com.puc.pi3_es_2024_t2

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import br.com.puc.pi3_es_2024_t2.databinding.ActivityQractivityBinding
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix

class QRActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQractivityBinding
    private val PRIMARY_CHANEL_ID = "primary_channel_id"
    private val NOTIFICATION_ID = 0
    private var notificationManager: NotificationManager? = null
    private lateinit var dataQrCode: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQractivityBinding.inflate(layoutInflater)
        setContentView(binding.root)

        dataQrCode = intent.getStringExtra("data").toString()

        /**
         * Quando a tecla o botão de voltar da Action Bar for pressionado iniciar uma nova Task a
         * partir a HomeActivity
         * */
        binding.btnArrowBack.setOnClickListener{
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        /**
         * Quando a tecla o botão de ir para a tela inicial da Action Bar for pressionado iniciar
         * uma nova Task a partir a HomeActivity
         * */
        binding.btnGoToHome.setOnClickListener {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        try{
            val bitmap = generateQR(dataQrCode)
            binding.ivQrCode.setImageBitmap(bitmap)
        }catch (e: WriterException){
            e.printStackTrace()
            Toast.makeText(this, "Erro ao gerar QRCode", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Gerador de QR code baseado no data passado
     * @author Thiago
     */

    private fun generateQR(data: String): Bitmap{
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(
            data,
            BarcodeFormat.QR_CODE,
            500, 500, null
        )
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for(x in 0 until width){
            for(y in 0 until height){
                bmp.setPixel(x, y, if(bitMatrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
            }
        }
        return bmp
    }

    /**
     * Quando a tecla de voltar do sistema android for pressionada iniciar uma nova Task a partir
     * da HomeActivty
     * */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            startActivity(Intent(this, HomeActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }
        return super.onKeyDown(keyCode, event)
    }

    /**
     * Quando QRActivity não estiver mais visível, a aplicação acionará a notificação.
     * */
    override fun onPause() {
        super.onPause()
        createChannel()
        sendNotification()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        dataQrCode = intent?.getStringExtra("dataQrCode").toString()
        try {
            val bitmap = generateQR(dataQrCode)
            binding.ivQrCode.setImageBitmap(bitmap)
        } catch (e: WriterException) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao gerar QRCode", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Criar o canal da notificação habilitando ao usuário a escolha de desativar ou não a
     * notificação que aparece nesse canal.
     * */
    private fun createChannel() {
        notificationManager = applicationContext
            .getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                PRIMARY_CHANEL_ID,
                applicationContext.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            )

            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.enableVibration((true))
            notificationChannel.description =
                applicationContext.getString(R.string.notification_channel_description)

            notificationManager?.createNotificationChannel(notificationChannel)
        }
    }

    /**
     * Obter a notificação construída e entregá-la à aplicação.
     * */
    private fun sendNotification() {
        val notificationBuilder: NotificationCompat.Builder = getNotificationBuilder()

        // entregar nortificação
        notificationManager?.notify(NOTIFICATION_ID, notificationBuilder.build())
    }

    /**
     * Construir a notificação: definir o ícone(obrigatório), o título da notificação, o texto da
     * notificação e a prioridade.
     * */
    private fun getNotificationBuilder(): NotificationCompat.Builder {
        val notificationIntent = Intent(applicationContext, HomeActivity::class
            .java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        notificationIntent.putExtra("isQRFragment", "true")
        notificationIntent.putExtra("dataQrCode", dataQrCode)

        val notificationPendingIntent: PendingIntent = PendingIntent.getActivity(
            applicationContext, NOTIFICATION_ID, notificationIntent, PendingIntent
                .FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(applicationContext, PRIMARY_CHANEL_ID)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentTitle("Locação Pendente")
            .setContentText("Existe uma locação a ser efetivada.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(notificationPendingIntent)
            .setAutoCancel(true)
    }
}
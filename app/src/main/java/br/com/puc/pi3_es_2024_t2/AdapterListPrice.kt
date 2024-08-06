package br.com.puc.pi3_es_2024_t2

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatButton
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.util.Calendar
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class AdapterListPrice(
    private val precos: List<Double>,
    private val listener: OnRentLocker
    ):
    RecyclerView.Adapter<AdapterListPrice.ItemViewHolder>(){

        private var aux: Number? = null
        inner class ItemViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
            val tvTempo: AppCompatTextView = itemView.findViewById(R.id.tvTempo)
            val tvPreco: AppCompatTextView = itemView.findViewById(R.id.tvPreco)
            val btnConfirmarLocacao: AppCompatButton = itemView.findViewById(R.id.btnConfirmarLocacao)

            init{
                btnConfirmarLocacao.setOnClickListener {
                    val aux = precos.size -1
                    listener.rentLocker(precos[adapterPosition], precos[aux])
                }
            }
        }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.price_list_item, parent, false)
        return ItemViewHolder(itemView)
    }

    override fun getItemCount(): Int {
        return precos.size
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val currentItem = precos[position]

        holder.tvPreco.text = "R$%.2f".format(currentItem.toDouble())

        val now = Calendar.getInstance()
        val currentHour = now.get(Calendar.HOUR_OF_DAY).hours
        val currentMinute = now.get(Calendar.MINUTE).minutes
        val startDuration = currentHour + currentMinute
        val baseTime = 18.hours
        val duration: Duration

        when (position) {
            0 -> {
                holder.tvTempo.text = "30min"
                duration = 30.minutes
            }
            4 -> {
                holder.tvTempo.text = "Até as 18h"
                duration = if(startDuration < 8.hours){
                    0.seconds
                }else{
                    1.days
                }
            }
            else -> {
                holder.tvTempo.text = "${position}h"
                duration = position.hours
            }
        }

        val endDuration = startDuration.plus(duration)
        val difference = baseTime.minus(endDuration)

        if(difference < 0.seconds){
            holder.btnConfirmarLocacao.text = "Indisponível"
            holder.btnConfirmarLocacao.setTextColor(ContextCompat.getColor(holder.btnConfirmarLocacao.context, R.color.red_error))
            holder.btnConfirmarLocacao.isEnabled = false
        }
    }
}
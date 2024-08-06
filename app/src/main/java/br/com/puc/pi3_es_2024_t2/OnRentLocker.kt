package br.com.puc.pi3_es_2024_t2

/**
 * Interface responsável por fornecer uma função que vai obrigar a classe implementadora a
 * sobreescrever a função de alugar um armário.
 * @author Guilherme e Thiago
 * */
interface OnRentLocker {
    fun rentLocker(precos: Number, valorDiaria: Number)
}
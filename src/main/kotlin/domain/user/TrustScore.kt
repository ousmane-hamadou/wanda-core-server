package com.github.ousmane_hamadou.domain.user

@JvmInline
value class TrustScore(val value: Int) {
    init {
        require(value in 0..100) { "Le score de confiance doit être entre 0 et 100" }
    }

    fun isHighReliability(): Boolean = value >= 80

    companion object {
        val DEFAULT = TrustScore(50)
        val MIN = TrustScore(0)
        val MAX = TrustScore(100)
    }
}
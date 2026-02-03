package com.github.ousmane_hamadou.domain.social

class ExternalIntegrationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

class InboundDataPersistenceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
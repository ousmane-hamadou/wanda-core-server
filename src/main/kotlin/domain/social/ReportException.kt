package com.github.ousmane_hamadou.domain.social

class ReportNotFoundException(message: String) : RuntimeException(message)
class ModerationActionException(message: String, cause: Throwable?) : RuntimeException(message, cause)
class PostNotFoundException(message: String) : RuntimeException(message)
class ReportPersistenceException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
class DuplicateReportException(message: String) : RuntimeException(message)

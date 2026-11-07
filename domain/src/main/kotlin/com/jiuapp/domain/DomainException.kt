package com.jiuapp.domain

/** Base de todas as violações de regra de negócio. Nunca engolida silenciosamente. */
sealed class DomainException(message: String) : RuntimeException(message)

class InvalidRankException(message: String) : DomainException(message)

class InvalidSessionException(message: String) : DomainException(message)

class InvalidGoalException(message: String) : DomainException(message)

class InvalidAttachmentException(message: String) : DomainException(message)

class InvalidHealthRecordException(message: String) : DomainException(message)

class InvalidCompetitionException(message: String) : DomainException(message)

class InvalidSettingsException(message: String) : DomainException(message)

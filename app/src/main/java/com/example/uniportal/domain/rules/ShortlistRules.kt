package com.example.uniportal.domain.rules

enum class ShortlistError { NAME_EMPTY, NAME_TOO_LONG, NAME_DUPLICATE, DESCRIPTION_TOO_LONG, NOT_FOUND }

sealed interface ShortlistNameValidation {
    data class Valid(val name: String) : ShortlistNameValidation
    data class Invalid(val error: ShortlistError) : ShortlistNameValidation
}

sealed interface ShortlistValidation {
    data class Valid(val name: String, val description: String) : ShortlistValidation
    data class Invalid(val error: ShortlistError) : ShortlistValidation
}

sealed interface ShortlistSaveResult {
    data class Saved(val id: Long) : ShortlistSaveResult
    data class Invalid(val error: ShortlistError) : ShortlistSaveResult
}

object ShortlistRules {
    const val NAME_MAX_LENGTH = 40
    const val DESCRIPTION_MAX_LENGTH = 200
    const val DEFAULT_EMOJI = "⭐"
    val EMOJI_OPTIONS: List<String> = listOf("⭐", "🎯", "🎓", "🏛️", "🔬", "💡", "🌍", "🚀", "❤️", "📚", "🏆", "🧭")

    fun validateName(name: String, existingNames: Collection<String>): ShortlistNameValidation {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> ShortlistNameValidation.Invalid(ShortlistError.NAME_EMPTY)
            trimmed.length > NAME_MAX_LENGTH -> ShortlistNameValidation.Invalid(ShortlistError.NAME_TOO_LONG)
            existingNames.any { it.trim().equals(trimmed, ignoreCase = true) } ->
                ShortlistNameValidation.Invalid(ShortlistError.NAME_DUPLICATE)
            else -> ShortlistNameValidation.Valid(trimmed)
        }
    }

    fun validate(name: String, description: String, existingNames: Collection<String>): ShortlistValidation {
        val validName = when (val nameResult = validateName(name, existingNames)) {
            is ShortlistNameValidation.Invalid -> return ShortlistValidation.Invalid(nameResult.error)
            is ShortlistNameValidation.Valid -> nameResult.name
        }
        val trimmedDescription = description.trim()
        if (trimmedDescription.length > DESCRIPTION_MAX_LENGTH) {
            return ShortlistValidation.Invalid(ShortlistError.DESCRIPTION_TOO_LONG)
        }
        return ShortlistValidation.Valid(validName, trimmedDescription)
    }

    fun normalizeEmoji(emoji: String): String = emoji.trim().ifEmpty { DEFAULT_EMOJI }
}

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HistoryItemDto(
    @SerialName("attempt_id") val attemptId: String,
    @SerialName("test_id") val testId: String,
    @SerialName("test_title") val testTitle: String,
    @SerialName("is_pair_activity") val isPairActivity: Boolean,
    @SerialName("user_completed_at") val userCompletedAt: String?,
    @SerialName("partner_id") val partnerId: String?,
    @SerialName("partner_completed_at") val partnerCompletedAt: String?
)

data class HistoryItem(
    val attemptId: String,
    val testId: String,
    val title: String,
    val completedAt: String, // Дата прохождения
    val userStatus: CompletionStatus,
    val partnerStatus: CompletionStatus
)

enum class CompletionStatus {
    COMPLETED,
    NOT_COMPLETED,
    NOT_APPLICABLE // Для не-парных активностей
}
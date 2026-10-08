package com.z23u184.studymate.app.util

import com.z23u184.studymate.app.R
import com.z23u184.studymate.domain.DomainError

fun DomainError.toUiText(): UiText = UiText.StringResource(
    when (this) {
        DomainError.EmptyTopicTitle -> R.string.domain_empty_topic_title
        DomainError.TopicTitleTooLong -> R.string.domain_topic_title_too_long
        DomainError.TopicTitleAlreadyExists -> R.string.domain_topic_title_already_exists
        DomainError.TopicNotFound -> R.string.domain_topic_not_found
        DomainError.TopicHasTasks -> R.string.domain_topic_has_tasks
        DomainError.EmptyTaskTitle -> R.string.domain_empty_task_title
        DomainError.TaskTitleTooLong -> R.string.domain_task_title_too_long
        DomainError.TaskDescriptionTooLong -> R.string.domain_task_description_too_long
        DomainError.TaskNotFound -> R.string.domain_task_not_found
        DomainError.InvalidStatusTransition -> R.string.domain_invalid_status_transition
        DomainError.SolutionNotFound -> R.string.domain_solution_not_found
        DomainError.EmptyAttachmentFileName -> R.string.domain_empty_attachment_file_name
        DomainError.InvalidAttachmentSize -> R.string.domain_invalid_attachment_size
        DomainError.AttachmentTooLarge -> R.string.domain_attachment_too_large
        DomainError.UnsupportedAttachmentType -> R.string.domain_unsupported_attachment_type
        DomainError.AttachmentOwnerNotFound -> R.string.domain_attachment_owner_not_found
        DomainError.AttachmentNotFound -> R.string.domain_attachment_not_found
    }
)

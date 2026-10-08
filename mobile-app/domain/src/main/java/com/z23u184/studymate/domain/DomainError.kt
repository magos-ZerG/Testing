package com.z23u184.studymate.domain

sealed interface DomainError {
    data object EmptyTopicTitle : DomainError
    data object TopicTitleTooLong : DomainError
    data object TopicTitleAlreadyExists : DomainError
    data object TopicNotFound : DomainError
    data object TopicHasTasks : DomainError

    data object EmptyTaskTitle : DomainError
    data object TaskTitleTooLong : DomainError
    data object TaskDescriptionTooLong : DomainError
    data object TaskNotFound : DomainError
    data object InvalidStatusTransition : DomainError

    data object SolutionNotFound : DomainError

    data object EmptyAttachmentFileName : DomainError
    data object InvalidAttachmentSize : DomainError
    data object AttachmentTooLarge : DomainError
    data object UnsupportedAttachmentType : DomainError
    data object AttachmentOwnerNotFound : DomainError
    data object AttachmentNotFound : DomainError
}

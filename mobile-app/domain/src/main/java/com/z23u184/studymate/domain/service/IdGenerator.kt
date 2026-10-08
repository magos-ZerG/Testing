package com.z23u184.studymate.domain.service

import com.z23u184.studymate.domain.model.AttachmentId
import com.z23u184.studymate.domain.model.TaskId
import com.z23u184.studymate.domain.model.TaskSolutionId
import com.z23u184.studymate.domain.model.TopicId

interface IdGenerator {
    fun newTopicId(): TopicId
    fun newTaskId(): TaskId
    fun newTaskSolutionId(): TaskSolutionId
    fun newAttachmentId(): AttachmentId
}

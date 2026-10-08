package com.z23u184.studymate.app.mapper

import com.z23u184.studymate.app.ui.task.taskdetails.AttachmentUi
import com.z23u184.studymate.domain.model.Attachment

class SolutionUiMapper {
    fun mapAttachment(attachment: Attachment): AttachmentUi = AttachmentUi(
        id = attachment.id.value,
        remoteId = attachment.remoteId,
        fileName = attachment.fileName,
        mimeType = attachment.mimeType,
        sizeText = formatSize(attachment.sizeBytes),
        localPath = attachment.localPath,
    )

    private fun formatSize(sizeBytes: Long): String {
        if (sizeBytes < 1024) return "$sizeBytes B"
        val kb = sizeBytes / 1024.0
        if (kb < 1024) return "%.1f KB".format(kb)
        val mb = kb / 1024.0
        return "%.1f MB".format(mb)
    }
}

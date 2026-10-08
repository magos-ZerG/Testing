package com.z23u184.studymate.app.ui.common.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AttachmentChip(
    fileName: String,
    sizeText: String,
    onClick: () -> Unit,
    onDeleteClick: (() -> Unit)? = null,
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        AssistChip(
            onClick = onClick,
            label = { Text(text = "$fileName • $sizeText") },
        )
        if (onDeleteClick != null) {
            TextButton(onClick = onDeleteClick) {
                Text(stringResource(R.string.delete_attachment))
            }
        }
    }
}

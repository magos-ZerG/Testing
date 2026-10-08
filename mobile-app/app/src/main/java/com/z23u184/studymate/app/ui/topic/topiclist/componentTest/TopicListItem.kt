package com.z23u184.studymate.app.ui.topic.topiclist.componentTest

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.topic.topiclist.*

@Composable
fun TopicListItem(
    item: TopicListItemUi,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(onClick = onClick),
                ) {
                    Text(item.title)
                    item.updatedAtText?.let { Text(it) }
                }
                TextButton(onClick = onDeleteClick) {
                    Text(stringResource(R.string.delete))
                }
            }
        }
    }
}

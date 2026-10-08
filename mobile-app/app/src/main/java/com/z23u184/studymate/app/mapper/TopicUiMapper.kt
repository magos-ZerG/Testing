package com.z23u184.studymate.app.mapper

import com.z23u184.studymate.app.ui.topic.topiclist.TopicListItemUi
import com.z23u184.studymate.domain.model.Topic

class TopicUiMapper {
    fun map(topic: Topic): TopicListItemUi = TopicListItemUi(
        id = topic.id.value,
        title = topic.title,
        updatedAtText = topic.updatedAt.toString(),
    )
}

package com.z23u184.studymate.app.ui.topic.topiclist.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Login
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.z23u184.studymate.app.R
import com.z23u184.studymate.app.ui.topic.topiclist.*

@Composable
fun TopicListTopBar(
    isAuthorized: Boolean,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit,
    onLoginClick: () -> Unit,
) {
    IconButton(onClick = onProfileClick) {
        Icon(Icons.Default.AccountCircle, contentDescription = stringResource(R.string.profile))
    }
    if (isAuthorized) {
        IconButton(onClick = onLogoutClick) {
            Icon(Icons.Default.ExitToApp, contentDescription = stringResource(R.string.logout_from_account))
        }
    } else {
        IconButton(onClick = onLoginClick) {
            Icon(Icons.Default.Login, contentDescription = stringResource(R.string.login))
        }
    }
}

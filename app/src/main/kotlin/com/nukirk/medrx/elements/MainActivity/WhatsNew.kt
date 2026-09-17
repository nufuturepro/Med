@file:OptIn(ExperimentalMaterial3Api::class, androidx.compose.ui.text.ExperimentalTextApi::class)

package com.nukirk.medrx.elements.MainActivity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.nukirk.medrx.R
import com.nukirk.medrx.ui.theme.GoogleSansFlex

private const val RELEASES_URL = "https://github.com/nufuturepro/MedRX/releases"
private const val DISCUSSIONS_URL = "https://github.com/nufuturepro/MedRX/discussions"

/**
 * Shown once per app version. Lists what the fork adds compared to
 * upstream Med and points at Releases and Discussions.
 */
@Composable
fun WhatsNewDialog(
    onDismiss: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.whatsnew_title),
                fontFamily = GoogleSansFlex
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.whatsnew_body),
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = GoogleSansFlex
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.whatsnew_community_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = GoogleSansFlex,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { uriHandler.openUri(RELEASES_URL) }) {
                Text(
                    text = stringResource(R.string.whatsnew_releases),
                    fontFamily = GoogleSansFlex
                )
            }
        },
        dismissButton = {
            TextButton(onClick = { uriHandler.openUri(DISCUSSIONS_URL) }) {
                Text(
                    text = stringResource(R.string.whatsnew_community),
                    fontFamily = GoogleSansFlex
                )
            }
        }
    )
}

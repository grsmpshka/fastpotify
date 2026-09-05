package rocks.fastpotify.android.update

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height

@Composable
fun UpdatePrompt(
    state: UpdateUiState,
    onDownload: (AndroidRelease) -> Unit,
    onInstall: (UpdateUiState.Ready) -> Unit,
    onDismiss: () -> Unit,
) {
    if (state is UpdateUiState.Idle) return

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when (state) {
                    is UpdateUiState.Available -> "Доступно обновление"
                    is UpdateUiState.Downloading -> "Загрузка обновления"
                    is UpdateUiState.Ready -> "Обновление готово"
                    is UpdateUiState.Current -> "Установлена новая версия"
                    is UpdateUiState.Error -> "Не удалось обновиться"
                    is UpdateUiState.Checking -> "Проверка обновлений"
                    is UpdateUiState.Idle -> ""
                },
                fontWeight = FontWeight.Bold,
            )
        },
        text = {
            Column {
                Text(
                    when (state) {
                        is UpdateUiState.Available ->
                            "Версия ${state.release.versionName} готова к скачиванию."
                        is UpdateUiState.Downloading ->
                            "Fastpotify скачивает и проверяет APK."
                        is UpdateUiState.Ready ->
                            "Нажмите «Установить». Android может попросить разрешить установку из Fastpotify."
                        is UpdateUiState.Current ->
                            "Обновлений пока нет."
                        is UpdateUiState.Error -> state.message
                        is UpdateUiState.Checking -> "Запрашиваем список релизов GitHub."
                        is UpdateUiState.Idle -> ""
                    },
                )
                if (state is UpdateUiState.Checking || state is UpdateUiState.Downloading) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            when (state) {
                is UpdateUiState.Available -> Button(onClick = { onDownload(state.release) }) {
                    Text("Скачать")
                }
                is UpdateUiState.Ready -> Button(onClick = { onInstall(state) }) {
                    Text("Установить")
                }
                is UpdateUiState.Current, is UpdateUiState.Error ->
                    Button(onClick = onDismiss) { Text("Готово") }
                else -> Unit
            }
        },
        dismissButton = {
            if (state is UpdateUiState.Available || state is UpdateUiState.Ready) {
                TextButton(onClick = onDismiss) { Text("Позже") }
            }
        },
    )
}

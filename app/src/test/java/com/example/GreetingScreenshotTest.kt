package com.example

import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.example.data.local.PaperEntity
import com.example.data.local.PaperStatus
import com.example.ui.PaperCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val samplePaper = PaperEntity(
        id = 1,
        uriString = "file:///dummy/downloaded_paper_1706.03762.pdf",
        folderUriString = "file:///dummy",
        originalFileName = "downloaded_paper_1706.03762.pdf",
        currentFileName = "downloaded_paper_1706.03762.pdf",
        suggestedFileName = "2017_Vaswani_Attention_Is_All_You_Need.pdf",
        doi = "10.48550/arXiv.1706.03762",
        title = "Attention Is All You Need",
        firstAuthor = "Vaswani",
        authorsFormatted = "Vaswani et al.",
        publicationYear = "2017",
        journal = "NeurIPS",
        status = PaperStatus.READY_TO_RENAME,
        fileSizeBytes = 2200000L
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        PaperCard(
            paper = samplePaper,
            onRename = {},
            onRevert = {},
            onEdit = {},
            modifier = Modifier.padding(16.dp)
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}

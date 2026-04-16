# Story 2.6: Item Photos (Camera & Gallery)

Status: ready-for-dev

## Story

As a user,
I want to attach a photo of my supplement bottle or pill,
So that I can recognise items instantly by their actual appearance.

## Acceptance Criteria

1. **Given** the `ItemEditSheet` is open **When** the sheet renders **Then** a photo option row is visible below the icon picker section, showing Camera and Gallery buttons.

2. **Given** the user taps Camera for the first time **When** the system camera permission prompt appears **Then** if granted, the camera opens and the captured photo is stored in app-private storage (iOS: `Application Support/<app>/images/`; Android: `context.filesDir/images/`) and set as the item's `image_path`.

3. **Given** the user taps Gallery for the first time **When** the system photo library permission prompt appears **Then** if granted, the photo picker opens and the selected image is copied to app-private storage and set as the item's `image_path`.

4. **Given** camera permission is permanently denied **When** the `ItemEditSheet` is displayed **Then** the Camera button is hidden; Gallery remains if its permission is granted.

5. **Given** photo library permission is permanently denied **When** the `ItemEditSheet` is displayed **Then** the Gallery button is hidden; Camera remains if its permission is granted.

6. **Given** both camera and gallery permissions are permanently denied **When** any screen displays items **Then** the photo option row is hidden in `ItemEditSheet`; the icon thumbnail in `ItemRow` still shows icon/placeholder as normal (the "no image placeholder" FR10 interpretation: no *photo*-based placeholder — icon placeholder is unaffected).

7. **Given** a photo has been captured or selected **When** the item appears in the items list **Then** the photo is shown inside the 40dp rounded-square thumbnail in `ItemRow`, taking visual priority over any icon (photo shown instead of icon when both are set).

8. **Given** an item with an existing photo is opened for editing **When** the `ItemEditSheet` opens **Then** the existing photo thumbnail is displayed in the leading content of the photo row, and camera/gallery options remain available to replace it.

---

## Tasks / Subtasks

- [ ] Task 1: Create `AppImageStorage` expect/actual (AC: 2, 3)
  - [ ] Create `shared/src/commonMain/kotlin/com/sonja/tracker/data/db/AppImageStorage.kt` — expect class
  - [ ] Create `shared/src/androidMain/kotlin/com/sonja/tracker/data/db/AppImageStorage.android.kt` — actual
  - [ ] Create `shared/src/iosMain/kotlin/com/sonja/tracker/data/db/AppImageStorage.ios.kt` — actual
  - [ ] See Dev Notes Task 1 for exact code

- [ ] Task 2: Register `AppImageStorage` in Koin DI (AC: 2, 3)
  - [ ] Add `single { AppImageStorage(get()) }` to `SharedModule.kt`
  - [ ] Verify Android and iOS module DI wiring — see Dev Notes Task 2

- [ ] Task 3: Add Coil 3.x for image display (AC: 7, 8)
  - [ ] Add `coil` version `"3.1.0"` to `[versions]` in `gradle/libs.versions.toml`
  - [ ] Add `coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }` to `[libraries]`
  - [ ] Add `implementation(libs.coil.compose)` to `commonMain.dependencies` in `shared/build.gradle.kts`
  - [ ] Run `./gradlew :shared:compileKotlinIosSimulatorArm64` to confirm resolution
  - [ ] See Dev Notes Task 3 for version compatibility notes

- [ ] Task 4: Create `rememberImagePickerActions` expect/actual Composable (AC: 1–6)
  - [ ] Create `shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ImagePickerActions.kt` — expect
  - [ ] Create `shared/src/androidMain/kotlin/com/sonja/tracker/ui/components/ImagePickerActions.android.kt` — actual
  - [ ] Create `shared/src/iosMain/kotlin/com/sonja/tracker/ui/components/ImagePickerActions.ios.kt` — actual
  - [ ] See Dev Notes Task 4 for complete code

- [ ] Task 5: Add permissions declarations (AC: 2–6)
  - [ ] Android: add `CAMERA` and `READ_MEDIA_IMAGES` / `READ_EXTERNAL_STORAGE` to `androidApp/src/main/AndroidManifest.xml` — see Dev Notes Task 5
  - [ ] Android: add `FileProvider` to `AndroidManifest.xml` and `res/xml/file_paths.xml` for camera URI — see Dev Notes Task 5
  - [ ] iOS: add `NSCameraUsageDescription` and `NSPhotoLibraryUsageDescription` to `iosApp/iosApp/Info.plist` — see Dev Notes Task 5

- [ ] Task 6: Update `ItemEditSheet` to add photo row (AC: 1, 4, 5, 6, 7, 8)
  - [ ] Add `initialImagePath: String? = null` parameter
  - [ ] Change `onSave` lambda signature to include `imagePath: String?` as 5th parameter
  - [ ] Add `var selectedImagePath by remember { mutableStateOf(initialImagePath) }` state
  - [ ] Add photo row section after the icon picker Column, before the `when` block — see Dev Notes Task 6
  - [ ] Update Save button `onClick` to pass `selectedImagePath`
  - [ ] Run `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

- [ ] Task 7: Update `ItemRow` to render photo (AC: 7)
  - [ ] When `item.imagePath != null`: render Coil `AsyncImage` at 24dp inside the thumbnail box
  - [ ] Photo takes priority over icon: `if (imagePath != null) AsyncImage(...) else if (iconId != null) ItemIconContent(...)`
  - [ ] `contentDescription`: `"${item.name} photo"` when imagePath set, else existing logic
  - [ ] See Dev Notes Task 7

- [ ] Task 8: Fix `ItemsViewModel.editItem` to pass `imagePath` directly (deferred-work fix)
  - [ ] Add `imagePath: String?` parameter to `editItem`
  - [ ] Pass it directly to `repository.updateItem(imagePath = imagePath, ...)` instead of reading from stale `uiState`
  - [ ] Remove the `// still read from uiState` comment
  - [ ] See Dev Notes Task 8

- [ ] Task 9: Update `ItemRepository.addItem` to accept `imagePath` (AC: 2, 3)
  - [ ] Change signature: `suspend fun addItem(name: String, weekdayTime: String, weekendTime: String?, iconId: String? = null, imagePath: String? = null)`
  - [ ] Pass `imagePath` to `insertItem` instead of hardcoded `null`
  - [ ] Existing tests compile unchanged (default `= null` keeps them valid)

- [ ] Task 10: Update `ItemsScreen` to wire `imagePath` through `onSave` (AC: 7)
  - [ ] Add `initialImagePath = item.imagePath` to edit sheet invocation
  - [ ] Update both `onSave` lambdas to 5-parameter form forwarding `imagePath`
  - [ ] See Dev Notes Task 10

- [ ] Task 11: Add repository and storage tests (AC: 2, 3)
  - [ ] `addItem_withImagePath_storesImagePath` — pass non-null imagePath, verify stored
  - [ ] `addItem_withNullImagePath_storesNull` — verify default null behaviour unchanged
  - [ ] Run `./gradlew :shared:testDebugUnitTest` — all tests pass
  - [ ] See Dev Notes Task 11

- [ ] Task 12: Final build verification (AC: all)
  - [ ] `./gradlew :shared:testDebugUnitTest` — BUILD SUCCESSFUL
  - [ ] `./gradlew :shared:assembleDebug` — BUILD SUCCESSFUL
  - [ ] `./gradlew :androidApp:assembleDebug` — BUILD SUCCESSFUL
  - [ ] `./gradlew :shared:compileKotlinIosSimulatorArm64` — BUILD SUCCESSFUL

---

## Dev Notes

### Don't Break List (from Stories 2.1–2.5)

- `ItemRepository.updateItem`, `deleteItem`, `observeItems()` — do NOT modify signatures (except as in Task 8/9)
- `ItemsViewModel.deleteItem` — do NOT touch
- `ItemEditSheet` `showTimePicker`/`showWeekendTimePicker` inline `when` block — preserve exactly
- `ItemEditSheet` `hideNavBar` / `awaitCancellation` `LaunchedEffect` — preserve exactly
- `ItemEditSheet` icon picker section (collapsible `ListItem` + `IconPickerGrid`) — preserve exactly
- `ItemIcons.kt`, `IconPickerGrid.kt` — do NOT modify
- `AppNavigation.kt`, `LocalHideNavBar`, `SharedModule.kt` — do NOT modify
- `DatabaseDriverFactory` expect/actual — do NOT touch
- `PlatformTimePickerDialog` expect/actual — do NOT touch
- `TrackerDatabase.sq` — do NOT modify (schema already has `image_path` TEXT NULL)

### Current `onSave` signature (before this story):
```kotlin
onSave: (name: String, weekdayTime: String, weekendTime: String?, iconId: String?) -> Unit
```
After this story:
```kotlin
onSave: (name: String, weekdayTime: String, weekendTime: String?, iconId: String?, imagePath: String?) -> Unit
```

### Current `editItem` in `ItemsViewModel` (before this story):
```kotlin
fun editItem(id: Long, name: String, weekdayTime: String, weekendTime: String?, iconId: String?) {
    viewModelScope.launch {
        val current = (uiState.value as? ItemsUiState.Success)?.items?.find { it.id == id }
        repository.updateItem(
            id = id,
            name = name,
            weekdayTime = weekdayTime,
            weekendTime = weekendTime,
            imagePath = current?.imagePath,  // stale-state risk — FIXED in this story
            iconId = iconId
        )
    }
}
```

---

### Task 1: `AppImageStorage` — Full Code

**`commonMain/data/db/AppImageStorage.kt`:**
```kotlin
package com.sonja.tracker.data.db

expect class AppImageStorage(context: Any? = null) {
    /** Returns the directory path where item images are stored. Creates it if absent. */
    fun getImagesDir(): String

    /** Generates a unique file path for a new image (does not create the file). */
    fun generateImagePath(): String
}
```

**`androidMain/data/db/AppImageStorage.android.kt`:**
```kotlin
package com.sonja.tracker.data.db

import android.content.Context
import java.io.File
import java.util.UUID

actual class AppImageStorage actual constructor(private val context: Any?) {
    private val ctx get() = requireNotNull(context as? Context) {
        "AppImageStorage requires a non-null Context on Android"
    }

    actual fun getImagesDir(): String {
        val dir = File(ctx.filesDir, "images")
        if (!dir.exists()) dir.mkdirs()
        return dir.absolutePath
    }

    actual fun generateImagePath(): String =
        File(getImagesDir(), "${UUID.randomUUID()}.jpg").absolutePath
}
```

**`iosMain/data/db/AppImageStorage.ios.kt`:**
```kotlin
package com.sonja.tracker.data.db

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUUID

actual class AppImageStorage actual constructor(private val context: Any?) {
    @OptIn(ExperimentalForeignApi::class)
    actual fun getImagesDir(): String {
        val paths = NSSearchPathForDirectoriesInDomains(
            NSApplicationSupportDirectory,
            NSUserDomainMask,
            true
        )
        val supportDir = paths.firstOrNull() as? String
            ?: error("NSApplicationSupportDirectory not found")
        val imagesDir = "$supportDir/images"
        NSFileManager.defaultManager.createDirectoryAtPath(
            imagesDir,
            withIntermediateDirectories = true,
            attributes = null,
            error = null
        )
        return imagesDir
    }

    actual fun generateImagePath(): String =
        "${getImagesDir()}/${NSUUID().UUIDString()}.jpg"
}
```

---

### Task 2: Koin Registration

`AppImageStorage` follows the same Koin pattern as `DatabaseDriverFactory`. Check the existing `SharedModule.kt`, `AndroidModule.kt`, and `IosModule.kt` for the `DatabaseDriverFactory` binding and mirror it exactly for `AppImageStorage`.

In `SharedModule.kt` (commonMain Koin module), add:
```kotlin
single { AppImageStorage(get()) }
```

`get()` resolves to the context parameter — this mirrors the `DatabaseDriverFactory` pattern already in place. If `DatabaseDriverFactory` is registered as `single { DatabaseDriverFactory(androidContext()) }` in `AndroidModule.kt`, do the same for `AppImageStorage`.

---

### Task 3: Coil 3.x Dependency

Add to `gradle/libs.versions.toml`:
```toml
[versions]
coil = "3.1.0"

[libraries]
coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
```

Add to `shared/build.gradle.kts` inside `commonMain.dependencies { }`:
```kotlin
implementation(libs.coil.compose)
```

Coil 3.x is native CMP-compatible (Android + iOS). No additional platform-specific additions needed.

**Usage in `ItemRow`:**
```kotlin
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale

AsyncImage(
    model = item.imagePath,
    contentDescription = "${item.name} photo",
    modifier = Modifier.size(24.dp),   // inside the 40dp box
    contentScale = ContentScale.Crop
)
```

**Important:** Coil 3.x uses `coil3.compose.AsyncImage`, not `coil.compose.AsyncImage`. Import from `coil3`.

---

### Task 4: `rememberImagePickerActions` — Full Code

This follows the **exact same pattern** as `PlatformTimePickerDialog` — `expect @Composable fun` in `commonMain/ui/components/`.

**`commonMain/ui/components/ImagePickerActions.kt`:**
```kotlin
package com.sonja.tracker.ui.components

import androidx.compose.runtime.Composable
import com.sonja.tracker.data.db.AppImageStorage

data class ImagePickerActions(
    val cameraGranted: Boolean,
    val galleryGranted: Boolean,
    val launchCamera: () -> Unit,
    val launchGallery: () -> Unit
)

@Composable
expect fun rememberImagePickerActions(
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
): ImagePickerActions
```

**`androidMain/ui/components/ImagePickerActions.android.kt`:**
```kotlin
package com.sonja.tracker.ui.components

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import com.sonja.tracker.data.db.AppImageStorage
import java.io.File
import java.io.FileOutputStream

@Composable
actual fun rememberImagePickerActions(
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
): ImagePickerActions {
    val context = LocalContext.current

    // Permission state
    var cameraGranted by remember {
        mutableStateOf(
            context.checkSelfPermission(Manifest.permission.CAMERA) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var galleryGranted by remember {
        mutableStateOf(hasGalleryPermission(context))
    }

    // Temp file holder for camera
    var cameraImagePath by remember { mutableStateOf<String?>(null) }
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraGranted = granted
        if (granted) {
            // Permission just granted — user must tap Camera again
        }
    }

    val galleryPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        galleryGranted = granted
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) onResult(cameraImagePath) else onResult(null)
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val destPath = appImageStorage.generateImagePath()
            copyUriToFile(context, uri, destPath)
            onResult(destPath)
        } else {
            onResult(null)
        }
    }

    return ImagePickerActions(
        cameraGranted = cameraGranted,
        galleryGranted = galleryGranted,
        launchCamera = {
            if (cameraGranted) {
                val path = appImageStorage.generateImagePath()
                val file = File(path)
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                cameraImagePath = path
                cameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
        launchGallery = {
            if (galleryGranted) {
                galleryLauncher.launch(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            } else {
                val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    Manifest.permission.READ_MEDIA_IMAGES
                else
                    Manifest.permission.READ_EXTERNAL_STORAGE
                galleryPermissionLauncher.launch(perm)
            }
        }
    )
}

private fun hasGalleryPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        context.checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    } else {
        context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
    }
}

private fun copyUriToFile(context: Context, uri: Uri, destPath: String) {
    context.contentResolver.openInputStream(uri)?.use { input ->
        FileOutputStream(destPath).use { output ->
            input.copyTo(output)
        }
    }
}
```

**`iosMain/ui/components/ImagePickerActions.ios.kt`:**

iOS camera/gallery requires UIKit interop. The approach uses `UIImagePickerController` presented on the root `UIViewController`. CMP exposes this via `UIApplication.sharedApplication.keyWindow?.rootViewController`.

```kotlin
package com.sonja.tracker.ui.components

import androidx.compose.runtime.*
import com.sonja.tracker.data.db.AppImageStorage
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFoundation.AVAuthorizationStatus
import platform.AVFoundation.AVAuthorizationStatusAuthorized
import platform.AVFoundation.AVCaptureDevice
import platform.AVFoundation.AVMediaTypeVideo
import platform.AVFoundation.requestAccessForMediaType
import platform.Photos.PHAuthorizationStatus
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIApplication
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerEditedImage
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.UIKit.UIViewController
import platform.darwin.NSObject
import platform.UIKit.UIImageJPEGRepresentation
import platform.Foundation.NSData
import platform.Foundation.writeToFile

@OptIn(ExperimentalForeignApi::class)
@Composable
actual fun rememberImagePickerActions(
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
): ImagePickerActions {
    var cameraGranted by remember {
        mutableStateOf(
            AVCaptureDevice.authorizationStatusForMediaType(AVMediaTypeVideo) ==
                    AVAuthorizationStatusAuthorized
        )
    }
    var galleryGranted by remember {
        mutableStateOf(
            PHPhotoLibrary.authorizationStatus() == PHAuthorizationStatusAuthorized
        )
    }

    return ImagePickerActions(
        cameraGranted = cameraGranted,
        galleryGranted = galleryGranted,
        launchCamera = {
            if (cameraGranted) {
                presentImagePicker(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                    appImageStorage = appImageStorage,
                    onResult = onResult
                )
            } else {
                AVCaptureDevice.requestAccessForMediaType(AVMediaTypeVideo) { granted ->
                    cameraGranted = granted
                }
            }
        },
        launchGallery = {
            if (galleryGranted) {
                presentImagePicker(
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
                    appImageStorage = appImageStorage,
                    onResult = onResult
                )
            } else {
                PHPhotoLibrary.requestAuthorization { status ->
                    galleryGranted = (status == PHAuthorizationStatusAuthorized)
                }
            }
        }
    )
}

@OptIn(ExperimentalForeignApi::class)
private fun presentImagePicker(
    sourceType: UIImagePickerControllerSourceType,
    appImageStorage: AppImageStorage,
    onResult: (imagePath: String?) -> Unit
) {
    val rootVC = UIApplication.sharedApplication.keyWindow?.rootViewController
        ?: return

    val picker = UIImagePickerController()
    picker.sourceType = sourceType
    picker.allowsEditing = false

    val delegate = object : NSObject(), UIImagePickerControllerDelegateProtocol,
        UINavigationControllerDelegateProtocol {
        override fun imagePickerController(
            picker: UIImagePickerController,
            didFinishPickingMediaWithInfo: Map<Any?, *>
        ) {
            val image = (didFinishPickingMediaWithInfo[UIImagePickerControllerEditedImage]
                ?: didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage])
                    as? platform.UIKit.UIImage
            val path = image?.let {
                val destPath = appImageStorage.generateImagePath()
                val data: NSData? = UIImageJPEGRepresentation(it, 0.85)
                if (data?.writeToFile(destPath, atomically = true) == true) destPath else null
            }
            picker.dismissViewControllerAnimated(true, completion = null)
            onResult(path)
        }

        override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
            picker.dismissViewControllerAnimated(true, completion = null)
            onResult(null)
        }
    }

    picker.delegate = delegate
    // Retain delegate to prevent GC — store as associated object or in a stable holder
    picker.setAssociatedObject(delegate) // helper — see note below
    rootVC.presentViewController(picker, animated = true, completion = null)
}
```

**⚠️ iOS delegate retention note:** The `NSObject` delegate will be garbage-collected if not retained. Use a stable holder (e.g., a `StableRef` or a stored property). The simplest approach: create a companion object or `object` delegate and clear it after dismissal. Alternatively, keep `delegate` in a `remember { }` at the call site and pass it into `presentImagePicker` as a retained holder.

**Alternative iOS approach:** Use `PHPickerViewController` (iOS 14+, preferred over `UIImagePickerController` for gallery — no gallery permission needed for basic access). `UIImagePickerController` for camera is fine.

**Note on `keyWindow`:** `keyWindow` is deprecated in iOS 13+. For iOS 16+ compatibility use:
```kotlin
UIApplication.sharedApplication.connectedScenes
    .filterIsInstance<UIWindowScene>()
    .firstOrNull()?.windows?.firstOrNull { it.isKeyWindow() }?.rootViewController
```

---

### Task 5: Permissions Declarations

**Android `AndroidManifest.xml`** — add inside `<manifest>` before `<application>`:
```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES"
    android:minSdkVersion="33" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE"
    android:maxSdkVersion="32" />
```

Add inside `<application>` (alongside `<activity>`):
```xml
<provider
    android:name="androidx.core.content.FileProvider"
    android:authorities="${applicationId}.fileprovider"
    android:exported="false"
    android:grantUriPermissions="true">
    <meta-data
        android:name="android.support.FILE_PROVIDER_PATHS"
        android:resource="@xml/file_paths" />
</provider>
```

Create `androidApp/src/main/res/xml/file_paths.xml`:
```xml
<?xml version="1.0" encoding="utf-8"?>
<paths xmlns:android="http://schemas.android.com/apk/res/android">
    <files-path name="images" path="images/" />
</paths>
```

**iOS `iosApp/iosApp/Info.plist`** — add inside the `<dict>`:
```xml
<key>NSCameraUsageDescription</key>
<string>Take a photo of your supplement or medication bottle for easy identification.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Select a photo of your supplement or medication bottle from your library.</string>
```

---

### Task 6: `ItemEditSheet` — Photo Row Addition

Add `initialImagePath: String? = null` parameter and update `onSave`:

```kotlin
fun ItemEditSheet(
    onSave: (name: String, weekdayTime: String, weekendTime: String?, iconId: String?, imagePath: String?) -> Unit,
    onDismiss: () -> Unit,
    initialName: String = "",
    initialWeekdayTime: String = "08:00",
    initialWeekendTime: String? = null,
    initialIconId: String? = null,
    initialImagePath: String? = null,     // NEW
    onDelete: (() -> Unit)? = null
)
```

Add state near other state declarations:
```kotlin
var selectedImagePath by remember { mutableStateOf(initialImagePath) }
```

Add `appImageStorage` via Koin injection at the top of the composable:
```kotlin
val appImageStorage: AppImageStorage = koinInject()
```

Add image picker actions:
```kotlin
val imagePicker = rememberImagePickerActions(
    appImageStorage = appImageStorage,
    onResult = { path -> if (path != null) selectedImagePath = path }
)
```

Add photo row section **after the icon picker Column and before the `when` block**:
```kotlin
// Photo row — shown only when at least one permission is available
if (imagePicker.cameraGranted || imagePicker.galleryGranted) {
    Column {
        ListItem(
            headlineContent = {
                Text(if (selectedImagePath != null) "Photo set" else "Add photo")
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    val path = selectedImagePath
                    if (path != null) {
                        AsyncImage(
                            model = path,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            },
            trailingContent = null
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (imagePicker.cameraGranted) {
                OutlinedButton(
                    onClick = imagePicker.launchCamera,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.PhotoCamera, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Camera")
                }
            }
            if (imagePicker.galleryGranted) {
                OutlinedButton(
                    onClick = imagePicker.launchGallery,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Outlined.Photo, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("Gallery")
                }
            }
        }
    }
}
```

Update Save button `onClick`:
```kotlin
onSave(name.trim(), weekdayTime, if (weekendToggleExpanded) weekendTime else null, selectedIconId, selectedImagePath)
```

New imports needed:
```kotlin
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.Photo
import androidx.compose.material3.OutlinedButton
import org.koin.compose.koinInject
```

---

### Task 7: `ItemRow` — Photo Rendering

Replace the box body in `ItemRow.kt`:
```kotlin
Box(
    modifier = Modifier
        .size(40.dp)
        .clip(RoundedCornerShape(8.dp))
        .background(MaterialTheme.colorScheme.primaryContainer)
        .semantics {
            contentDescription = when {
                item.imagePath != null -> "${item.name} photo"
                item.iconId != null -> "${item.name} icon"
                else -> "No icon"
            }
        },
    contentAlignment = Alignment.Center
) {
    when {
        item.imagePath != null -> AsyncImage(
            model = item.imagePath,
            contentDescription = null,
            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        item.iconId != null -> ItemIconContent(
            iconId = item.iconId,
            modifier = Modifier.size(24.dp)
        )
        // else: empty primaryContainer box (placeholder)
    }
}
```

New imports:
```kotlin
import coil3.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.layout.fillMaxSize
```

---

### Task 8: `ItemsViewModel.editItem` — Fix Stale-State (deferred-work resolution)

```kotlin
fun editItem(id: Long, name: String, weekdayTime: String, weekendTime: String?, iconId: String?, imagePath: String?) {
    viewModelScope.launch {
        repository.updateItem(
            id = id,
            name = name,
            weekdayTime = weekdayTime,
            weekendTime = weekendTime,
            imagePath = imagePath,   // now passed directly — fixes stale-state risk
            iconId = iconId
        )
        // TODO Epic 4: NotificationScheduler.rescheduleForSlot(weekdayTime, weekendTime)
    }
}
```

Remove the `val current = ...` lookup entirely — no longer needed.

---

### Task 9: `ItemRepository.addItem` — Accept `imagePath`

```kotlin
suspend fun addItem(
    name: String,
    weekdayTime: String,
    weekendTime: String?,
    iconId: String? = null,
    imagePath: String? = null
) {
    withContext(Dispatchers.Default) {
        database.trackerDatabaseQueries.insertItem(
            name = name,
            reminder_weekday_time = weekdayTime,
            reminder_weekend_time = weekendTime,
            image_path = imagePath,
            icon_id = iconId
        )
    }
}
```

Default `= null` for both `iconId` and `imagePath` preserves all existing test calls.

---

### Task 10: `ItemsScreen` — Updated Callbacks

Add sheet (5-parameter `onSave`):
```kotlin
if (showAddSheet) {
    ItemEditSheet(
        onSave = { name, weekdayTime, weekendTime, iconId, imagePath ->
            viewModel.addItem(name, weekdayTime, weekendTime, iconId, imagePath)
        },
        onDismiss = { showAddSheet = false }
    )
}
```

Edit sheet:
```kotlin
selectedItem?.let { item ->
    ItemEditSheet(
        initialName = item.name,
        initialWeekdayTime = item.reminderWeekdayTime ?: "08:00",
        initialWeekendTime = item.reminderWeekendTime,
        initialIconId = item.iconId,
        initialImagePath = item.imagePath,
        onSave = { name, weekdayTime, weekendTime, iconId, imagePath ->
            viewModel.editItem(item.id, name, weekdayTime, weekendTime, iconId, imagePath)
        },
        onDismiss = { selectedItem = null },
        onDelete = { viewModel.deleteItem(item.id) }
    )
}
```

---

### Task 11: Repository Test Pattern

```kotlin
@Test
fun addItem_withImagePath_storesImagePath() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin D3", "08:00", null, imagePath = "/some/path/img.jpg")

    val items = repo.observeItems().first()
    assertEquals(1, items.size)
    assertEquals("/some/path/img.jpg", items[0].imagePath)
}

@Test
fun addItem_withNullImagePath_storesNull() = runTest {
    val db = TrackerDatabase(createDriver())
    val repo = ItemRepository(db)

    repo.addItem("Vitamin C", "09:00", null)  // default imagePath = null

    val items = repo.observeItems().first()
    assertNull(items[0].imagePath)
}
```

---

### New Files Summary

```
shared/src/commonMain/kotlin/com/sonja/tracker/data/db/AppImageStorage.kt          (NEW)
shared/src/androidMain/kotlin/com/sonja/tracker/data/db/AppImageStorage.android.kt  (NEW)
shared/src/iosMain/kotlin/com/sonja/tracker/data/db/AppImageStorage.ios.kt          (NEW)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ImagePickerActions.kt  (NEW)
shared/src/androidMain/kotlin/com/sonja/tracker/ui/components/ImagePickerActions.android.kt (NEW)
shared/src/iosMain/kotlin/com/sonja/tracker/ui/components/ImagePickerActions.ios.kt (NEW)
androidApp/src/main/res/xml/file_paths.xml                                           (NEW)
```

### Modified Files Summary

```
gradle/libs.versions.toml                                           (coil version + lib)
shared/build.gradle.kts                                             (coil dependency)
androidApp/src/main/AndroidManifest.xml                             (permissions + FileProvider)
iosApp/iosApp/Info.plist                                            (NSCamera/PhotoLibrary usage)
shared/src/commonMain/kotlin/com/sonja/tracker/data/repository/ItemRepository.kt    (imagePath in addItem)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsViewModel.kt           (editItem + addItem)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemEditSheet.kt            (photo row)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/items/ItemsScreen.kt              (onSave callbacks)
shared/src/commonMain/kotlin/com/sonja/tracker/ui/components/ItemRow.kt             (photo display)
shared/src/commonTest/.../ItemRepositoryTest.kt                                      (2 new tests)
_bmad-output/implementation-artifacts/deferred-work.md                               (resolve imagePath stale-state item)
_bmad-output/implementation-artifacts/sprint-status.yaml                             (status update)
```

### References

- Architecture: `AppImageStorage` expect/actual — iOS `Application Support/images/`, Android `context.filesDir/images/` [architecture.md, "Image & Icon Storage"]
- Architecture: Platform actual rule — zero business logic in `actual` implementations [architecture.md]
- Architecture: `DatabaseDriverFactory` expect/actual — exact pattern to mirror for `AppImageStorage` [architecture.md + source files]
- Architecture: File structure — `data/db/AppImageStorage.kt` location [architecture.md, "Project Structure"]
- UX: Photo option row below icon picker; camera/gallery buttons [ux-design-specification.md, `ItemEditSheet` anatomy]
- UX: Photo takes priority over icon in thumbnail display [ux-design-specification.md, "Item Image / Icon Strategy"]
- Deferred-work: `imagePath` stale-state in `editItem` — resolved by this story [deferred-work.md, code review of 2-4]
- Story 2.5: icon picker collapsible ListItem + `IconPickerGrid` pattern — photo row follows same ListItem style [2-5-icon-picker.md, Task 7]
- `PlatformTimePickerDialog` expect pattern — exact template for `rememberImagePickerActions` [source: `ui/components/PlatformTimePickerDialog.kt`]

---

## Dev Agent Record

### Agent Model Used

### Debug Log References

### Completion Notes List

### File List

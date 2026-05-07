import os
import glob
import shutil

base_dir = "/Users/adrianopassos/gut-trace/android-source"
app_dir = os.path.join(base_dir, "app")
src_java_dir = os.path.join(app_dir, "src/main/java/com/example/guttrace")

# 1. Move folders and update package names
folders_to_move = ["data", "sync", "ui", "viewmodel"]
for folder in folders_to_move:
    src_folder = os.path.join(base_dir, folder)
    if not os.path.exists(src_folder): continue
    
    tgt_folder = os.path.join(src_java_dir, folder)
    os.makedirs(tgt_folder, exist_ok=True)
    
    for filename in os.listdir(src_folder):
        if filename.endswith(".kt"):
            # skip MainActivity.kt from ui folder for now, it's special
            if filename == "MainActivity.kt" and folder == "ui":
                continue
                
            src_file = os.path.join(src_folder, filename)
            tgt_file = os.path.join(tgt_folder, filename)
            
            with open(src_file, "r") as f:
                content = f.read()
                
            # Replace package and imports
            content = content.replace("package com.guttrace", "package com.example.guttrace")
            content = content.replace("import com.guttrace", "import com.example.guttrace")
            
            with open(tgt_file, "w") as f:
                f.write(content)
                
# 2. Handle MainActivity (merge my navigation with the generated one)
my_main_path = os.path.join(base_dir, "ui", "MainActivity.kt")
generated_main_path = os.path.join(src_java_dir, "MainActivity.kt")

if os.path.exists(my_main_path):
    with open(my_main_path, "r") as f:
        my_main = f.read()
    
    # We replace the package and import, but we also want to use the generated theme if possible.
    # Actually, we can just replace the whole file, use our theme, and ignore the generated theme,
    # OR we can just use the generated theme package name!
    my_main = my_main.replace("package com.guttrace", "package com.example.guttrace")
    my_main = my_main.replace("import com.guttrace", "import com.example.guttrace")
    my_main = my_main.replace("import com.example.guttrace.ui.theme.GutTraceTheme", "import com.example.guttrace.ui.theme.GutTraceTheme")
    
    with open(generated_main_path, "w") as f:
        f.write(my_main)

# 3. Handle file_paths.xml
xml_dir = os.path.join(app_dir, "src/main/res/xml")
os.makedirs(xml_dir, exist_ok=True)
shutil.copy(os.path.join(base_dir, "file_paths.xml"), os.path.join(xml_dir, "file_paths.xml"))

# 4. Update AndroidManifest.xml
manifest_path = os.path.join(app_dir, "src/main/AndroidManifest.xml")
with open(manifest_path, "r") as f:
    manifest = f.read()

# Add permissions and file provider if not present
if "android.permission.CAMERA" not in manifest:
    perms = """
    <uses-permission android:name="android.permission.CAMERA" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-feature android:name="android.hardware.camera" android:required="false" />
"""
    manifest = manifest.replace("<application", perms + "\n    <application")
    
if "FileProvider" not in manifest:
    provider = """
        <provider
            android:name="androidx.core.content.FileProvider"
            android:authorities="${applicationId}.fileprovider"
            android:exported="false"
            android:grantUriPermissions="true">
            <meta-data
                android:name="android.support.FILE_PROVIDER_PATHS"
                android:resource="@xml/file_paths" />
        </provider>
"""
    manifest = manifest.replace("</application>", provider + "\n    </application>")
    
with open(manifest_path, "w") as f:
    f.write(manifest)

# 5. Append dependencies to app/build.gradle.kts
gradle_path = os.path.join(app_dir, "build.gradle.kts")
with open(gradle_path, "r") as f:
    gradle = f.read()
    
if "androidx.camera" not in gradle:
    deps = """
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // ViewModel + LiveData
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Room (local SQLite database)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager (background sync)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // CameraX (photo capture)
    implementation("androidx.camera:camera-camera2:1.3.2")
    implementation("androidx.camera:camera-lifecycle:1.3.2")
    implementation("androidx.camera:camera-view:1.3.2")

    // Coil (async image loading)
    implementation("io.coil-kt:coil-compose:2.6.0")
    
    // Icons
    implementation("androidx.compose.material:material-icons-extended:1.6.7")
"""
    gradle = gradle.replace("dependencies {", "dependencies {" + deps)
    
    # Also add ksp plugin to the top plugins block
    if "com.google.devtools.ksp" not in gradle:
        gradle = gradle.replace('alias(libs.plugins.kotlin.compose)', 'alias(libs.plugins.kotlin.compose)\n    id("com.google.devtools.ksp") version "1.9.22-1.0.17"')
        
    with open(gradle_path, "w") as f:
        f.write(gradle)

# Setting Up GitHub Repository

This guide will help you push your DroidPad project to GitHub.

## Prerequisites

- Git installed on your system
- GitHub account
- Repository created on GitHub (https://github.com/Tolstoyj/MacTrack)

## Steps to Push to GitHub

### 1. Initialize Git Repository (if not already initialized)

```bash
cd /Users/lzycdrtj/AndroidStudioProjects/DroidPadMacOS
git init
```

### 2. Add All Files

```bash
git add .
```

### 3. Create Initial Commit

```bash
git commit -m "Initial commit: DroidPad - Android Trackpad for macOS

- Bluetooth HID device implementation
- Multi-touch gesture support
- Air Mouse mode with gyroscope
- macOS shortcuts integration
- Modern Jetpack Compose UI
- Device history and auto-reconnect
- Full documentation and open-source setup"
```

### 4. Add Remote Repository

```bash
git remote add origin https://github.com/Tolstoyj/MacTrack.git
```

### 5. Verify Remote

```bash
git remote -v
```

You should see:
```
origin  https://github.com/Tolstoyj/MacTrack.git (fetch)
origin  https://github.com/Tolstoyj/MacTrack.git (push)
```

### 6. Push to GitHub

```bash
git branch -M main
git push -u origin main
```

If you encounter authentication issues, you may need to:
- Use a Personal Access Token instead of password
- Set up SSH keys
- Use GitHub CLI

### 7. Set Up GitHub Repository Settings

After pushing, go to your GitHub repository settings and:

1. **Add Repository Description:**
   ```
   Transform your Android device into a wireless trackpad and mouse for macOS. Built with Kotlin and Jetpack Compose.
   ```

2. **Add Topics:**
   - `android`
   - `kotlin`
   - `jetpack-compose`
   - `bluetooth-hid`
   - `trackpad`
   - `macos`
   - `open-source`

3. **Add Website (if applicable):**
   - Leave blank or add your website

4. **Enable Issues and Discussions:**
   - Go to Settings → General
   - Enable Issues
   - Enable Discussions (optional)

5. **Add License:**
   - GitHub should automatically detect the MIT license

## Creating Your First Release

### 1. Create a Tag

```bash
git tag -a v1.0.0 -m "DroidPad v1.0.0 - Initial Release"
git push origin v1.0.0
```

### 2. Create Release on GitHub

1. Go to your repository on GitHub
2. Click "Releases" → "Create a new release"
3. Choose tag: `v1.0.0`
4. Release title: `DroidPad v1.0.0 - Initial Release`
5. Description:
   ```markdown
   ## 🎉 Initial Release

   First public release of DroidPad!

   ### Features
   - Bluetooth HID trackpad functionality
   - Multi-touch gesture support
   - Air Mouse mode
   - macOS shortcuts integration
   - Modern UI with Jetpack Compose

   ### Installation
   Download the APK from the assets below and install on your Android device.

   ### Requirements
   - Android 10+ (API 29)
   - Bluetooth 4.0+
   - macOS 10.12+
   ```
6. Upload APK file (if available)
7. Click "Publish release"

## Adding Badges to README

You can add badges to your README by including them at the top. The README already includes some badges, but you can customize them at [shields.io](https://shields.io/).

## Next Steps

1. **Add Screenshots**: Take screenshots of your app and add them to a `screenshots/` folder
2. **Create Issues Template**: Add `.github/ISSUE_TEMPLATE/` for better issue management
3. **Add Pull Request Template**: Create `.github/pull_request_template.md`
4. **Set Up CI/CD**: Consider GitHub Actions for automated builds
5. **Add Code of Conduct**: Create `CODE_OF_CONDUCT.md` if needed

## Troubleshooting

### Authentication Issues

If you get authentication errors:

**Option 1: Use Personal Access Token**
1. Go to GitHub Settings → Developer settings → Personal access tokens
2. Generate a new token with `repo` permissions
3. Use token as password when pushing

**Option 2: Use SSH**
```bash
git remote set-url origin git@github.com:Tolstoyj/MacTrack.git
```

**Option 3: Use GitHub CLI**
```bash
gh auth login
gh repo create MacTrack --public --source=. --remote=origin
```

### Large Files

If you have large files, consider:
- Using Git LFS for APK files
- Adding them to `.gitignore` if not needed in repo
- Using GitHub Releases for binary files

## Congratulations! 🎉

Your project is now open source on GitHub! Share it with the community and welcome contributions.


@echo off
chcp 65001 >nul
set PATH=%PATH%;C:\Users\NLSync\AppData\Local\Programs\Git\cmd
echo ====================================================================
echo             CÔNG CỤ ĐẨY DỰ ÁN LÊN GITHUB (ĐỀ TÀI 2508)
echo ====================================================================
echo.

git --version >nul 2>&1
if errorlevel 1 (
    echo [LỖI] Không tìm thấy Git trên máy!
    pause
    exit /b
)

echo Dự án hiện đã sẵn sàng đẩy lên GitHub.
echo Vui lòng dán (Paste) đường link GitHub Repository của bạn vào bên dưới.
echo Ví dụ: https://github.com/ten-tai-khoan/DigitalLibraryClustering.git
echo.
set /p REPO_URL="Link GitHub Repository: "

if "%REPO_URL%"=="" (
    echo.
    echo [THÔNG BÁO] Bạn chưa nhập link GitHub. Vui lòng chạy lại khi có link!
    pause
    exit /b
)

echo.
echo [*] Đang thiết lập remote origin: %REPO_URL%
git remote remove origin 2>nul
git remote add origin %REPO_URL%
git branch -M main

echo [*] Đang đẩy toàn bộ mã nguồn lên GitHub...
git push -u origin main

if errorlevel 1 (
    echo.
    echo [!] Quá trình đẩy gặp trục trặc (có thể do chưa đăng nhập hoặc link chưa đúng).
    echo [!] Nếu hiện cửa sổ đăng nhập GitHub trên trình duyệt, bạn hãy bấm "Sign in with your browser".
) else (
    echo.
    echo ====================================================================
    echo [THÀNH CÔNG] Toàn bộ dự án đã được đẩy lên GitHub thành công!
    echo ====================================================================
)
echo.
pause

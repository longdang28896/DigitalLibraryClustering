@echo off
chcp 65001 >nul
set PATH=C:\Users\NLSync\AppData\Local\Programs\Git\cmd;C:\Users\NLSync\AppData\Local\Programs\Git\mingw64\bin;%PATH%
cd /d "%~dp0"

echo ====================================================================
echo             CÔNG CỤ ĐẨY DỰ ÁN LÊN GITHUB (ĐỀ TÀI 2508)
echo ====================================================================
echo.
echo Repo GitHub của bạn:
echo https://github.com/longdang28896/DigitalLibraryClustering.git
echo.
echo Đang tiến hành đẩy toàn bộ mã nguồn lên nhánh main...
echo.

git remote remove origin 2>nul
git remote add origin https://github.com/longdang28896/DigitalLibraryClustering.git
git branch -M main
git push -u origin main

if errorlevel 1 (
    echo.
    echo ====================================================================
    echo [LƯU Ý]: Nếu đây là lần đầu tiên bạn đẩy code lên GitHub từ máy này:
    echo 1. Trình duyệt hoặc cửa sổ Git sẽ hiện lên yêu cầu xác thực.
    echo 2. Bạn chỉ cần chọn "Sign in with your browser" và bấm Authorize.
    echo 3. Sau khi đăng nhập thành công, code sẽ tự động được tải lên GitHub!
    echo ====================================================================
) else (
    echo.
    echo ====================================================================
    echo [THÀNH CÔNG] Dự án đã được đẩy lên GitHub thành công 100%!
    echo Xem tại: https://github.com/longdang28896/DigitalLibraryClustering
    echo ====================================================================
)

echo.
pause

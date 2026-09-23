# HỆ THỐNG QUẢN LÝ THƯ VIỆN SỐ TÍCH HỢP PHÂN CỤM TÀI LIỆU (JAVA DESKTOP)

> **BÀI TẬP LỚN MÔN: NHẬP MÔN KHAI PHÁ DỮ LIỆU VÀ MÁY HỌC (DMML)**  
> **ĐỀ TÀI SỐ 2508**: Ứng dụng kỹ thuật phân cụm tích hợp vào hệ thống quản lý thư viện số trên công nghệ Java để giải quyết bài toán phân nhóm tài liệu.  
> **PHIÊN BẢN NÂNG CẤP**: Giao diện FlatLaf Hiện Đại • Hỗ Trợ Tiếng Việt • Đọc File PDF / Word DOCX / TXT • Tùy Chỉnh Sửa Cụm Phân Nhóm

---

## 1. GIỚI THIỆU TỔNG QUAN

Dự án là một **Ứng dụng Desktop hoàn chỉnh bằng công nghệ Java (Java Swing + FlatLaf UI hiện đại)** giải quyết bài toán phân nhóm tài liệu học thuật số tự động thông qua các kỹ thuật Khai phá Dữ liệu & Học máy không giám sát (Unsupervised Clustering).

### Các Điểm Nổi Bật Đáp Ứng Yêu Cầu & Nâng Cấp Mới:
1. **Giao diện FlatLaf Đẹp Mắt, Gọn Gàng & Hiện Đại**:
   - Thiết kế dạng thẻ card tinh tế, bo tròn nhẹ, độ tương phản hài hòa.
   - Bảng biểu hiển thị rõ ràng với chiều cao dòng 28px, màu xen kẽ dịu mắt, tag phân loại cụm và ngôn ngữ trực quan.
   - Banner thương hiệu trên cùng tích hợp các khối thống kê nhanh (Tổng số tài liệu, Số cụm chủ đề, Ngôn ngữ, Định dạng).
2. **Đọc Tài Liệu Tiếng Việt & Đa Định Dạng Phổ Thông**:
   - Tích hợp **Apache PDFBox** để trích xuất văn bản từ file `.pdf`.
   - Tích hợp **Apache POI** để trích xuất văn bản từ file Word `.docx`.
   - Đọc tự động file văn bản `.txt`, `.md`, `.json`, `.csv`.
   - **Xử lý ngôn ngữ tự nhiên Tiếng Việt (Vietnamese NLP)**: Giữ nguyên 100% nguyên âm có dấu và phụ âm tiếng Việt (`\p{L}`), lọc từ dừng tiếng Việt (`stopwords_vi.txt`), chỉ áp dụng Porter Stemmer cho tiếng Anh để không làm biến dạng từ ngữ tiếng Việt.
   - Nạp sẵn bộ tài liệu mẫu học thuật tiếng Việt đa chủ đề (`data/sample_vietnamese_docs.json`) và các file mẫu thực tế trong thư mục `data/samples/`.
3. **Can Thiệp & Sửa Nhóm Phân Cụm Khi Chưa Đúng Ý**:
   - **Khi thêm mới tài liệu**: Khung phân tích hiển thị cụm AI dự đoán kèm thanh đo độ tin cậy (Confidence). Người dùng có thể giữ nguyên hoặc chủ động chọn cụm khác từ danh sách thả xuống trước khi lưu.
   - **Trong kho tài liệu**: Nút bấm **"✏️ Đổi Cụm / Sửa"** trên thanh công cụ và trong menu chuột phải (Context Menu) cho phép đổi nhóm tài liệu sang cụm mong muốn ngay tức thì.
   - **Trong cửa sổ xem chi tiết**: Nút đổi cụm cho phép cập nhật trực tiếp mà không cần thoát cửa sổ.
4. **Cơ sở khoa học & Đánh giá mô hình (Yêu cầu 2.3)**:
   - Cài đặt và đối sánh 3 thuật toán: **K-Means**, **Hierarchical (Agglomerative)**, và **DBSCAN**.
   - Đánh giá bằng các độ đo: **Elbow Method (Inertia/WCSS)**, **Silhouette Coefficient**, **Davies-Bouldin Index**, **Purity**, **ARI**, **NMI**.

---

## 2. BẢNG ĐỐI SÁNH HIỆU NĂNG THỰC NGHIỆM

| Thuật Toán | Số Cụm ($K$) | Silhouette Score (&uarr;) | Davies-Bouldin (&darr;) | Purity (&uarr;) | ARI (&uarr;) | NMI (&uarr;) | Thời Gian (ms) |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **K-Means (Cosine & K-Means++)** | **6** | **0.0275** | **6.6637** | **57.56%** | **0.2531** | **0.4530** | **~68 ms** |
| Hierarchical (Agglomerative) | 6 | 0.0196 | 5.7938 | 50.89% | 0.1478 | 0.4354 | 325.4 ms |
| DBSCAN (Density-Based) | 16 | 0.0036 | 2.9358 | 33.22% | 0.0574 | 0.1695 | 8.8 ms |

> **Căn cứ khoa học lựa chọn mô hình**: K-Means đạt điểm chất lượng phân cụm nội tại (Silhouette) và ngoại tại (Purity = 57.56%, ARI = 0.2531) cao nhất, đồng thời thời gian thực thi chỉ mất ~68 ms (nhanh hơn gần 5 lần so với Hierarchical).

---

## 3. CẤU TRÚC DỰ ÁN

```
DigitalLibraryClustering/
├── pom.xml                                       # Quản lý thư viện Maven (FlatLaf, PDFBox, POI, Gson, JUnit)
├── run.bat                                       # File khởi chạy Desktop 1-click cho Windows
├── DigitalLibrary.jar                            # File JAR thực thi độc lập (đã đóng gói đầy đủ dependencies)
├── data/
│   ├── 20newsgroups_sample.json                  # 900 tài liệu trích xuất từ 20 Newsgroups (Tiếng Anh)
│   ├── sample_vietnamese_docs.json               # Tài liệu học thuật mẫu bằng Tiếng Việt
│   ├── stopwords_en.txt                          # Từ dừng Tiếng Anh
│   ├── stopwords_vi.txt                          # Từ dừng Tiếng Việt
│   ├── experiment_results.json                   # Kết quả thực nghiệm đối sánh thuật toán & Elbow
│   └── samples/                                  # Thư mục file thực tế để kiểm thử nạp file:
│       ├── mau_tri_tue_nhan_tao.docx             # File mẫu Word (.docx) tiếng Việt
│       ├── mau_thien_van_vu_tru.pdf              # File mẫu PDF (.pdf) tiếng Việt
│       └── mau_y_duoc_tieng_viet.txt             # File mẫu Text (.txt) tiếng Việt
└── src/
    ├── main/java/com/dmml/library/
    │   ├── DigitalLibraryApplication.java        # Điểm khởi chạy ứng dụng Desktop
    │   ├── model/                                # Document, ClusterInfo, ClusteringResult, EvaluationMetrics
    │   ├── util/
    │   │   └── DocumentFileReader.java           # Tiện ích trích xuất file PDF, DOCX, TXT, JSON và nhận diện tiếng Việt
    │   ├── ml/
    │   │   ├── nlp/                              # TextPreprocessor (hỗ trợ Unicode tiếng Việt), PorterStemmer
    │   │   ├── feature/                          # TfIdfVectorizer (L2 Normalized)
    │   │   ├── clustering/                       # KMeansClusterer, HierarchicalClusterer, DBSCANClusterer
    │   │   └── evaluation/                       # SilhouetteCalculator, DaviesBouldinCalculator, PurityCalculator
    │   ├── service/                              # DocumentRepository, ClusteringService
    │   └── ui/                                   # Các thành phần giao diện Java Swing:
    │       ├── MainLibraryFrame.java             # Cửa sổ chính, Branding banner, Tabs, Status Bar
    │       ├── DocumentCatalogPanel.java         # Tab 1: Kho tài liệu, lọc tiếng Việt/Anh, đổi cụm
    │       ├── AddDocumentPanel.java             # Tab 2: Nạp file PDF/DOCX/TXT, AI preview, sửa cụm
    │       ├── RecommendationPanel.java          # Tab 3: Gợi ý tài liệu tương tự (Cosine Similarity)
    │       ├── AnalyticsPanel.java               # Tab 4: Thẻ KPI khoa học & Đối sánh mô hình
    │       ├── DocumentDetailDialog.java         # Hộp thoại xem chi tiết toàn văn tài liệu
    │       └── EditDocumentDialog.java           # Hộp thoại sửa thông tin & can thiệp đổi cụm
    └── test/java/com/dmml/library/
        └── DigitalLibraryTests.java              # Unit tests kiểm tra NLP tiếng Việt, PDF/DOCX, Reassignment
```

---

## 4. HƯỚNG DẪN KHỞI CHẠY ỨNG DỤNG DESKTOP

### Cách 1: Khởi chạy 1-click (Đơn giản nhất)
Nhấp đúp chuột vào file:
```
run.bat
```
Cửa sổ ứng dụng Desktop Java Swing sẽ mở lên ngay lập tức.

### Cách 2: Chạy trực tiếp file JAR
```bash
java -jar DigitalLibrary.jar
```

### Cách 3: Chạy qua Maven (Dùng cho lập trình và chấm bài)
```bash
mvn compile exec:java
```

---

## 5. HƯỚNG DẪN THỰC HIỆN TỪNG TÍNH NĂNG MỚI TRÊN GIAO DIỆN

### Tab 1: 📁 Kho Tài Liệu & Duyệt Theo Cụm
- **Duyệt theo Cụm**: Bấm chọn từng cụm ở danh sách bên trái. Bảng bên phải sẽ lọc ngay lập tức danh sách các tài liệu thuộc cụm đó.
- **Lọc theo Ngôn Ngữ**: Chọn hộp chọn *"Tất Cả"*, *"🇻🇳 Tiếng Việt"*, hoặc *"🇬🇧 Tiếng Anh"*.
- **Tìm kiếm**: Gõ từ khóa vào ô *"Tìm kiếm"* và bấm Enter.
- **SỬA CỤM PHÂN NHÓM (Yêu cầu đề bài)**:
  - Chọn một tài liệu trên bảng $\rightarrow$ Bấm nút **"✏️ Đổi Cụm / Sửa"** (hoặc nhấp chuột phải $\rightarrow$ chọn *"Đổi Cụm Phân Nhóm / Chỉnh Sửa"*).
  - Hộp thoại hiển thị thông tin bài viết và cụm gán hiện tại.
  - Chọn cụm mới mong muốn (hoặc bấm *"🤖 AI Gợi Ý Thử"* để tham khảo) $\rightarrow$ Bấm *"💾 Lưu Thay Đổi & Cập Nhật Cụm"*.
  - Tài liệu sẽ lập tức được chuyển sang cụm mới, số lượng bài của cụm được cập nhật ngay theo thời gian thực!
- **Xem chi tiết**: Nhấp đúp vào bất kỳ dòng nào hoặc bấm nút *"Xem Chi Tiết"*.

### Tab 2: ➕ Thêm Sách Mới, Nạp File PDF/DOCX & Tự Động Phân Cụm
- **Nạp file từ máy tính**:
  - Bấm nút **"📁 Chọn File Từ Máy Tính (PDF, DOCX, TXT, JSON)"**.
  - Chọn bất kỳ file tài liệu nào (hoặc vào thư mục `data/samples/` để chọn file mẫu: `.docx`, `.pdf`, `.txt`).
  - Hệ thống tự động đọc toàn bộ nội dung, trích xuất tiêu đề, tóm tắt, tự nhận diện ngôn ngữ (🇻🇳 Tiếng Việt hoặc 🇬🇧 Tiếng Anh), và tự động kích hoạt bộ dự đoán phân cụm!
- **Điền mẫu nhanh**: Bấm các nút *"🇻🇳 Mẫu: AI & Học Máy"*, *"🇻🇳 Mẫu: Y Học & Dược"*, *"🇬🇧 Mẫu: Space Astronomy"*.
- **SỬA CỤM TRƯỚC KHI LƯU (Nếu AI đoán chưa đúng ý)**:
  - Khung AI bên phải hiển thị Cụm dự đoán kèm % độ tin cậy.
  - Bên dưới có mục **"Can Thiệp Sửa Cụm (Nếu Không Đúng Ý)"**: Bạn có thể chọn bất kỳ cụm nào khác theo ý muốn.
  - Bấm **"💾 Lưu Vào Thư Viện Số"** $\rightarrow$ Tài liệu sẽ được lưu với đúng cụm bạn đã chọn!

### Tab 3: 🎯 Đề Xuất Tài Liệu Tương Tự
- Chọn tài liệu nguồn từ danh sách (có cờ 🇻🇳 hoặc 🇬🇧 hiển thị rõ ràng).
- Bấm nút **"Tìm Tài Liệu Tương Tự"**.
- Bảng hiển thị Top 10 tài liệu liên quan nhất trong cùng cụm kèm % tương đồng Cosine.
- Nhấp chọn từng dòng để xem nhanh trích đoạn hoặc bấm *"Xem Toàn Văn Chi Tiết"*.

### Tab 4: 🔬 Đánh Giá Khoa Học (DMML)
- Xem các thẻ KPI chỉ số khoa học: Silhouette Score, Davies-Bouldin, Purity, Thời gian thực thi.
- Thử nghiệm tái phân cụm trực tiếp với K-Means, Hierarchical hoặc DBSCAN.
- Xem bảng đối sánh 3 thuật toán và bảng phân tích đường cong Elbow $K=2..12$.

# -*- coding: utf-8 -*-
"""
Script cập nhật file Báo Cáo Bài Tập Lớn Word (.docx) chuẩn theo yêu cầu của user:
1. Dựa trên bản của user: Bỏ phần thừa đầu trang, chỉ giữ Báo cáo BTL + Đề tài 2508 + 4 thành viên.
2. Thêm MỤC LỤC đầy đủ chuẩn mực (với tab leader chấm chấm).
3. TẤT CẢ sử dụng font Times New Roman (100% paragraphs, runs, tables, headings, code, phụ lục).
4. KHẮC PHỤC TRIỆT ĐỂ LỖI BẢNG BỊ THỪA / CẮT ĐÔI TRANG:
   - Bỏ hoàn toàn w:tblHeader trên các bảng (không lặp lại header).
   - Thêm w:cantSplit trên TẤT CẢ các dòng của mọi bảng (không bao giờ ngắt đôi dòng).
   - Canh lề và giãn dòng bảng gọn gàng để không bị tràn chữ lẻ "BTL" sang trang mới.
"""

import docx
from docx.shared import Inches, Pt, RGBColor, Cm
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT, WD_TAB_LEADER
from docx.enum.table import WD_TABLE_ALIGNMENT, WD_ALIGN_VERTICAL
from docx.oxml import parse_xml
from docx.oxml.ns import nsdecls, qn

def set_cell_margins(cell, top=80, bottom=80, left=100, right=100):
    tcPr = cell._tc.get_or_add_tcPr()
    tcMar = parse_xml(f'<w:tcMar {nsdecls("w")}>'
                      f'<w:top w:w="{top}" w:type="dxa"/>'
                      f'<w:bottom w:w="{bottom}" w:type="dxa"/>'
                      f'<w:left w:w="{left}" w:type="dxa"/>'
                      f'<w:right w:w="{right}" w:type="dxa"/>'
                      f'</w:tcMar>')
    tcPr.append(tcMar)

def set_cell_background(cell, fill_hex):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = parse_xml(f'<w:shd {nsdecls("w")} w:fill="{fill_hex}"/>')
    tcPr.append(shd)

def set_simple_table_borders(table):
    tblPr = table._tbl.tblPr
    borders = parse_xml(f'<w:tblBorders {nsdecls("w")}>'
                        f'<w:top w:val="single" w:sz="6" w:space="0" w:color="000000"/>'
                        f'<w:bottom w:val="single" w:sz="6" w:space="0" w:color="000000"/>'
                        f'<w:left w:val="none"/>'
                        f'<w:right w:val="none"/>'
                        f'<w:insideH w:val="single" w:sz="4" w:space="0" w:color="D0D0D0"/>'
                        f'<w:insideV w:val="none"/>'
                        f'</w:tblBorders>')
    tblPr.append(borders)

def format_clean_table(table, col_widths, headers, data, align_cols=None):
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    set_simple_table_borders(table)
    
    # Header row
    hdr_cells = table.rows[0].cells
    for i, title in enumerate(headers):
        cell = hdr_cells[i]
        cell.width = Cm(col_widths[i])
        set_cell_background(cell, "F2F2F2")
        set_cell_margins(cell, top=100, bottom=100, left=80, right=80)
        p = cell.paragraphs[0]
        p.alignment = WD_ALIGN_PARAGRAPH.CENTER
        p.paragraph_format.space_before = Pt(2)
        p.paragraph_format.space_after = Pt(2)
        run = p.add_run(title)
        run.bold = True
        run.font.name = "Times New Roman"
        run.font.size = Pt(10.5)
        run.font.color.rgb = RGBColor(0, 0, 0)
        
    # Data rows
    for r_idx, row_data in enumerate(data):
        row = table.add_row()
        # Chống ngắt đôi dòng trên mọi hàng (cantSplit)
        trPr = row._tr.get_or_add_trPr()
        trPr.append(parse_xml(f'<w:cantSplit {nsdecls("w")}/>'))
        
        row_cells = row.cells
        for c_idx, val in enumerate(row_data):
            cell = row_cells[c_idx]
            cell.width = Cm(col_widths[c_idx])
            set_cell_margins(cell, top=60, bottom=60, left=80, right=80)
            cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(1)
            p.paragraph_format.space_after = Pt(2)
            p.paragraph_format.line_spacing = 1.15
            
            if align_cols and c_idx < len(align_cols):
                p.alignment = align_cols[c_idx]
            else:
                p.alignment = WD_ALIGN_PARAGRAPH.LEFT
                
            run = p.add_run(str(val))
            run.font.name = "Times New Roman"
            run.font.size = Pt(10.5)
            run.font.color.rgb = RGBColor(0, 0, 0)
            if c_idx == 0 and "K-Means" in str(val):
                run.bold = True

    # Header row cũng có cantSplit và TUYỆT ĐỐI KHÔNG có tblHeader
    trPr0 = table.rows[0]._tr.get_or_add_trPr()
    trPr0.append(parse_xml(f'<w:cantSplit {nsdecls("w")}/>'))

def create_report():
    doc = docx.Document()
    
    # 1. Căn lề chuẩn
    for section in doc.sections:
        section.top_margin = Cm(2.0)
        section.bottom_margin = Cm(2.0)
        section.left_margin = Cm(3.0)
        section.right_margin = Cm(2.0)
        section.page_width = Cm(21.0)
        section.page_height = Cm(29.7)
        
        # Footer
        footer = section.footer
        f_p = footer.paragraphs[0]
        f_p.alignment = WD_ALIGN_PARAGRAPH.RIGHT
        f_run = f_p.add_run("Báo cáo BTL DMML - Đề tài 2508 | Trang ")
        f_run.font.name = "Times New Roman"
        f_run.font.size = Pt(10)
        f_run.font.color.rgb = RGBColor(100, 100, 100)
        f_page = parse_xml(r'<w:fldSimple %s w:instr="PAGE"/>' % nsdecls('w'))
        f_p._p.append(f_page)

    # 2. Style Normal chuẩn Times New Roman
    style_normal = doc.styles['Normal']
    style_normal.font.name = 'Times New Roman'
    style_normal.font.size = Pt(13)
    style_normal.font.color.rgb = RGBColor(0, 0, 0)
    style_normal.paragraph_format.line_spacing = 1.3
    style_normal.paragraph_format.space_after = Pt(4)
    style_normal.paragraph_format.space_before = Pt(0)
    
    rFonts = style_normal.element.rPr.get_or_add_rFonts()
    rFonts.set(qn('w:ascii'), 'Times New Roman')
    rFonts.set(qn('w:hAnsi'), 'Times New Roman')
    rFonts.set(qn('w:cs'), 'Times New Roman')
    rFonts.set(qn('w:eastAsia'), 'Times New Roman')

    def p(text="", bold_prefix="", indent=True, align=WD_ALIGN_PARAGRAPH.JUSTIFY, italic=False, keep_with_next=False):
        par = doc.add_paragraph()
        par.alignment = align
        par.paragraph_format.line_spacing = 1.3
        par.paragraph_format.space_before = Pt(2)
        par.paragraph_format.space_after = Pt(4)
        if keep_with_next:
            par.paragraph_format.keep_with_next = True
        if indent:
            par.paragraph_format.first_line_indent = Cm(0.8)
        if bold_prefix:
            rb = par.add_run(bold_prefix)
            rb.bold = True
            rb.font.name = "Times New Roman"
            rb.font.size = Pt(13)
            rb.font.color.rgb = RGBColor(0, 0, 0)
        if text:
            rt = par.add_run(text)
            rt.font.name = "Times New Roman"
            rt.font.size = Pt(13)
            rt.font.italic = italic
            rt.font.color.rgb = RGBColor(0, 0, 0)
        return par

    def h1(title):
        par = doc.add_paragraph()
        par.alignment = WD_ALIGN_PARAGRAPH.LEFT
        par.paragraph_format.space_before = Pt(14)
        par.paragraph_format.space_after = Pt(6)
        par.paragraph_format.keep_with_next = True
        run = par.add_run(title)
        run.bold = True
        run.font.name = "Times New Roman"
        run.font.size = Pt(14)
        run.font.color.rgb = RGBColor(0, 0, 0)
        return par

    def h2(title):
        par = doc.add_paragraph()
        par.alignment = WD_ALIGN_PARAGRAPH.LEFT
        par.paragraph_format.space_before = Pt(10)
        par.paragraph_format.space_after = Pt(4)
        par.paragraph_format.keep_with_next = True
        run = par.add_run(title)
        run.bold = True
        run.font.name = "Times New Roman"
        run.font.size = Pt(13)
        run.font.color.rgb = RGBColor(0, 0, 0)
        return par

    def text_line(text, bold=False, indent=True):
        par = doc.add_paragraph()
        par.alignment = WD_ALIGN_PARAGRAPH.LEFT
        if indent:
            par.paragraph_format.first_line_indent = Cm(0.8)
        par.paragraph_format.space_before = Pt(1)
        par.paragraph_format.space_after = Pt(2)
        par.paragraph_format.line_spacing = 1.2
        run = par.add_run(text)
        run.font.name = "Times New Roman"
        run.font.size = Pt(12)
        run.bold = bold
        run.font.color.rgb = RGBColor(0, 0, 0)
        return par

    # ==================== ĐẦU FILE: GIỮ ĐÚNG BẢN CỦA USER ====================
    p_title1 = doc.add_paragraph()
    p_title1.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title1.paragraph_format.space_before = Pt(0)
    p_title1.paragraph_format.space_after = Pt(4)
    r = p_title1.add_run("BÁO CÁO BÀI TẬP LỚN\nHỌC PHẦN: KHAI PHÁ DỮ LIỆU VÀ MÁY HỌC (DMML)\n")
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(14)

    p_title2 = doc.add_paragraph()
    p_title2.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p_title2.paragraph_format.space_before = Pt(4)
    p_title2.paragraph_format.space_after = Pt(16)
    r = p_title2.add_run("ĐỀ TÀI SỐ 2508: PHÂN CỤM TÀI LIỆU HỌC THUẬT CHO HỆ THỐNG THƯ VIỆN SỐ TRÊN NỀN TẢNG JAVA")
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(14.5)

    p_mem = doc.add_paragraph()
    p_mem.alignment = WD_ALIGN_PARAGRAPH.LEFT
    p_mem.paragraph_format.space_before = Pt(4)
    p_mem.paragraph_format.space_after = Pt(4)
    r = p_mem.add_run("Nhóm sinh viên thực hiện:")
    r.bold = True
    r.font.name = "Times New Roman"
    r.font.size = Pt(13)

    m_list = [
        "1. Đặng Hoàng Nhật Long - Nhóm trưởng",
        "2. Hoàng Văn Chính",
        "3. Trịnh Việt Cường",
        "4. Lê Văn Hiệp"
    ]
    for m in m_list:
        mp = doc.add_paragraph()
        mp.paragraph_format.first_line_indent = Cm(0.8)
        mp.paragraph_format.space_before = Pt(1)
        mp.paragraph_format.space_after = Pt(1)
        mr = mp.add_run(m)
        mr.font.name = "Times New Roman"
        mr.font.size = Pt(12.5)
        if "Nhóm trưởng" in m:
            mr.bold = True

    # Spacing trước Mục lục
    doc.add_paragraph().paragraph_format.space_after = Pt(6)

    # ==================== MỤC LỤC BÁO CÁO (THEO YÊU CẦU) ====================
    h1("MỤC LỤC")

    toc_entries = [
        ("CHƯƠNG 1. TỔNG QUAN VỀ ĐỀ TÀI", "Trang 1", True),
        ("  1.1. Giới thiệu bài toán (Nêu vấn đề)", "Trang 1", False),
        ("  1.2. Đánh giá đề tài liên quan", "Trang 1", False),
        ("  1.3. Mục đích đề tài", "Trang 2", False),
        ("  1.4. Mục tiêu đề tài", "Trang 2", False),
        ("  1.5. Các ràng buộc kỹ thuật", "Trang 2", False),
        ("  1.6. Phân công và Kế hoạch thực hiện", "Trang 3", False),
        ("CHƯƠNG 2. TIỀN XỬ LÝ DỮ LIỆU", "Trang 4", True),
        ("  2.1. Phân tích đặc trưng tập dữ liệu", "Trang 4", False),
        ("  2.2. Quy trình và các bước tiền xử lý dữ liệu NLP", "Trang 4", False),
        ("  2.3. Biểu diễn dữ liệu bằng mô hình TF-IDF", "Trang 5", False),
        ("CHƯƠNG 3. XÂY DỰNG MÔ HÌNH", "Trang 6", True),
        ("  3.1. Lựa chọn thuật toán", "Trang 6", False),
        ("  3.2. Các chỉ số đánh giá", "Trang 6", False),
        ("  3.3. Lựa chọn công nghệ", "Trang 7", False),
        ("  3.4. Triển khai xây dựng mô hình", "Trang 7", False),
        ("  3.5. Đánh giá chất lượng", "Trang 8", False),
        ("CHƯƠNG 4. TÍCH HỢP MÔ HÌNH VÀO HỆ THỐNG", "Trang 9", True),
        ("  4.1. Chức năng 1: Quản lý Kho tài liệu & Duyệt tự động theo cụm", "Trang 9", False),
        ("  4.2. Chức năng 2: Nạp tài liệu đa định dạng, Phân cụm tự động & Can thiệp sửa cụm thủ công", "Trang 9", False),
        ("  4.3. Chức năng 3: Gợi ý và Đề xuất tài liệu tương đồng", "Trang 10", False),
        ("  4.4. Chức năng 4: Bảng điều khiển Giám sát Khoa học", "Trang 10", False),
        ("KẾT LUẬN", "Trang 11", True),
        ("TÀI LIỆU THAM KHẢO", "Trang 12", True),
        ("PHỤ LỤC", "Trang 12", True),
        ("  Phụ lục A: Hướng dẫn cài đặt và Khởi chạy ứng dụng Desktop", "Trang 12", False),
        ("  Phụ lục B: Cấu trúc thư mục dự án", "Trang 13", False)
    ]

    for item_text, page_str, is_bold in toc_entries:
        tp = doc.add_paragraph()
        tp.paragraph_format.tab_stops.add_tab_stop(Cm(16.0), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)
        tp.paragraph_format.space_before = Pt(1)
        tp.paragraph_format.space_after = Pt(2)
        tp.paragraph_format.line_spacing = 1.2
        
        r_item = tp.add_run(item_text)
        r_item.font.name = "Times New Roman"
        r_item.font.size = Pt(12)
        r_item.bold = is_bold
        
        r_tab = tp.add_run(f"\t{page_str}")
        r_tab.font.name = "Times New Roman"
        r_tab.font.size = Pt(11.5)
        r_tab.bold = is_bold

    # Ngắt trang sau Mục lục để Chương 1 bắt đầu trang trọng
    doc.add_page_break()

    # ==================== CHƯƠNG 1 ====================
    h1("CHƯƠNG 1. TỔNG QUAN VỀ ĐỀ TÀI")

    h2("1.1. Giới thiệu bài toán (Nêu vấn đề)")
    p("Trong bối cảnh chuyển đổi số giáo dục đại học, các hệ thống thư viện số (Digital Library) phải tiếp nhận và quản lý một khối lượng tài liệu học thuật rất lớn gồm các bài báo khoa học, giáo trình, luận án dưới nhiều định dạng văn bản điện tử khác nhau như PDF, Word (.docx) và Plain Text (.txt).")
    p("Phương pháp quản trị thư viện truyền thống dựa vào việc thủ thư đọc và biên mục thủ công bộc lộ nhiều điểm hạn chế lớn: tốn kém thời gian nhân lực, thiếu tính nhất quán do nhận thức chủ quan giữa các thủ thư khác nhau, và không xử lý kịp thời hơn 80% tài liệu phi cấu trúc chưa hề có nhãn phân loại sẵn có.")
    p("Bên cạnh đó, các phần mềm sẵn có thường chỉ tập trung vào văn bản tiếng Anh thô, thiếu khả năng đọc trực tiếp từ các file thực tế và thường xử lý lỗi các ký tự tiếng Việt có dấu. Do đó, bài toán đặt ra là: Xây dựng một ứng dụng Java Desktop hoàn chỉnh áp dụng kỹ thuật Phân cụm dữ liệu không giám sát (Unsupervised Document Clustering) để tự động gom nhóm tài liệu theo chủ đề, hỗ trợ song ngữ Việt - Anh, đọc đa định dạng tập tin thực tế và cho phép người dùng can thiệp sửa cụm khi cần thiết.")

    h2("1.2. Đánh giá đề tài liên quan")
    p("Phân cụm văn bản là một trong những bài toán kinh điển trong Khai phá dữ liệu (DMML). Các hướng nghiên cứu chính đã được thực hiện gồm:")
    p("• Phương pháp phân hoạch (K-Means): Nghiên cứu của Steinbach et al. (2000) chứng minh K-Means kết hợp độ đo Cosine (Spherical K-Means) đạt hiệu năng rất tốt trên không gian vector văn bản thưa nhờ chi phí tính toán thấp O(n·k·d) và tốc độ thực thi nhanh. Tuy nhiên, nhược điểm là nhạy cảm với việc chọn tâm khởi tạo ban đầu.")
    p("• Phương pháp phân cấp (Hierarchical Clustering - HAC): Nghiên cứu của Murtagh & Contreras (2012) cho thấy HAC tạo ra cấu trúc cây phân cấp trực quan và không cần chọn trước số cụm K. Dẫu vậy, độ phức tạp tính toán lớn O(n^2 log n) khiến thuật toán chạy chậm và khó tích hợp vào phần mềm tương tác thời gian thực.")
    p("• Phương pháp dựa trên mật độ (DBSCAN): Ester et al. (1996) đề xuất DBSCAN có khả năng phát hiện cụm hình dạng bất kỳ và loại bỏ nhiễu. Tuy nhiên, trên dữ liệu văn bản có số chiều lớn, hiện tượng 'Curse of Dimensionality' làm mật độ dữ liệu trở nên đồng đều, khiến DBSCAN dễ phân rã thành quá nhiều cụm nhỏ hoặc coi phần lớn tài liệu là nhiễu.")
    p("• Các hệ thống thư viện hiện hành (DSpace, Koha): Chủ yếu áp dụng tìm kiếm từ khóa cứng hoặc phân loại có giám sát. Hệ thống thiếu vắng khả năng tự động phân cụm khi nạp file mới và thiếu cơ chế cho phép thủ thư can thiệp hiệu chỉnh trực tiếp trên giao diện khi máy tính phân cụm chưa đúng ý.")

    h2("1.3. Mục đích đề tài")
    p("Xây dựng một phần mềm ứng dụng Desktop trên nền tảng Java đóng vai trò là Hệ thống Quản trị Thư viện số Thông minh, ứng dụng các thuật toán Khai phá dữ liệu và Học máy không giám sát để tự động phân nhóm tài liệu học thuật theo chủ đề, nâng cao hiệu quả biên mục và tra cứu.")

    h2("1.4. Mục tiêu đề tài")
    p("Đề tài tập trung hoàn thành 4 mục tiêu cụ thể sau:")
    p("• Mục tiêu 1: Xây dựng pipeline tiền xử lý ngôn ngữ tự nhiên (NLP) hỗ trợ đầy đủ tiếng Việt và tiếng Anh; bóc tách tự động nội dung từ các tập tin thực tế phổ biến gồm PDF, DOCX, TXT.")
    p("• Mục tiêu 2: Cài đặt độc lập, đối sánh thực nghiệm khách quan 3 thuật toán phân cụm kinh điển (K-Means, Hierarchical, DBSCAN) dựa trên bộ chỉ số khoa học {Mi} (Silhouette, Davies-Bouldin, Purity, Thời gian thực thi) để chọn ra mô hình tối ưu nhất.")
    p("• Mục tiêu 3: Phát triển ứng dụng Desktop hoàn chỉnh bằng Java Swing với giao diện FlatLaf hiện đại, rõ ràng, tốc độ phản hồi tức thì, không lỗi hiển thị.")
    p("• Mục tiêu 4: Tích hợp mô hình học máy vào 4 chức năng cốt lõi của thư viện; cung cấp cơ chế cho phép người dùng can thiệp sửa đổi phân cụm linh hoạt khi AI phân nhóm chưa đúng ý.")

    h2("1.5. Các ràng buộc kỹ thuật")
    p("• Nền tảng: Ứng dụng Desktop viết bằng Java (JDK 17 trở lên), chạy offline-first, đa nền tảng (Windows, Linux, macOS).")
    p("• Giao diện: Sử dụng Java Swing kết hợp bộ giao diện FlatLaf (Light Theme), bố cục thẻ dạng card gọn gàng, dòng bảng hiển thị thoáng (28px), hiển thị hoàn chỉnh tiếng Việt không bị lỗi đè chữ.")
    p("• Định dạng tập tin: Đọc và trích xuất trực tiếp văn bản từ file PDF (Apache PDFBox), file Word DOCX (Apache POI), và TXT (UTF-8).")
    p("• Xử lý tiếng Việt: Bảo toàn 100% nguyên âm có dấu qua regex Unicode \\p{L}, lọc từ dừng tiếng Việt riêng biệt, không áp dụng Porter Stemmer lên tiếng Việt để tránh làm méo mó từ.")
    p("• Thời gian phản hồi: Dự đoán phân cụm cho tài liệu mới nạp vào hệ thống dưới 50 ms.")

    h2("1.6. Phân công và Kế hoạch thực hiện")
    p("Dự án được thực hiện trong khoảng thời gian từ ngày 05/09/2024 đến ngày 22/09/2024 với sự phân công cụ thể giữa các thành viên như sau:", keep_with_next=True)
    
    # Kẻ bảng với kích thước hợp lý, KHÔNG để chữ BTL bị rơi xuống trang mới
    plan_headers = ["Thời gian", "Nhiệm vụ cụ thể", "Thành viên phụ trách", "Kết quả bàn giao"]
    plan_data = [
        ["05/09 - 08/09", "Khảo sát bài toán, thu thập tập dữ liệu 20 Newsgroups và 10 bài báo tiếng Việt", "Đặng Hoàng Nhật Long\nHoàng Văn Chính", "Bộ dữ liệu JSON và tập tin mẫu .pdf, .docx, .txt"],
        ["09/09 - 12/09", "Module trích xuất PDF/DOCX (PDFBox, POI) và tiền xử lý NLP tiếng Việt", "Hoàng Văn Chính\nTrịnh Việt Cường", "Lớp DocumentFileReader.java, TextPreprocessor.java"],
        ["13/09 - 16/09", "Không gian vector TF-IDF, cài đặt K-Means, HAC, DBSCAN và tính chỉ số {Mi}", "Trịnh Việt Cường\nLê Văn Hiệp", "Mô hình K-Means Cosine, bộ tính Silhouette, DB, Purity"],
        ["17/09 - 20/09", "Thiết kế giao diện FlatLaf, tích hợp mô hình vào 4 chức năng và sửa cụm", "Đặng Hoàng Nhật Long\nLê Văn Hiệp", "Giao diện 4 Tabs hoàn chỉnh, tính năng sửa cụm hoạt động"],
        ["21/09 - 22/09", "Kiểm thử hệ thống (8/8 unit tests pass), đóng gói JAR và báo cáo BTL", "Cả nhóm", "File DigitalLibrary.jar, run.bat và Báo cáo BTL"]
    ]
    tbl_plan = doc.add_table(rows=1, cols=4)
    format_clean_table(tbl_plan, [2.3, 5.6, 3.8, 4.3], plan_headers, plan_data, [WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.LEFT])

    # ==================== CHƯƠNG 2 ====================
    h1("CHƯƠNG 2. TIỀN XỬ LÝ DỮ LIỆU")

    h2("2.1. Phân tích đặc trưng tập dữ liệu")
    p("Hệ thống sử dụng tập dữ liệu học thuật kết hợp song ngữ gồm:")
    p("1. Tập chuẩn 20 Newsgroups (900 tài liệu tiếng Anh): Trích xuất cân bằng trên 6 chủ đề khoa học và xã hội: Graphics & Electronics (150 bài), Y học lâm sàng sci.med (150 bài), Không gian vũ trụ sci.space (150 bài), Thể thao baseball (150 bài), Chính trị politics (150 bài), Kinh tế forsale (150 bài).")
    p("2. Tập tài liệu học thuật Tiếng Việt: Gồm 10 bài báo nghiên cứu chuyên ngành được biên soạn chuẩn mực (Trí tuệ nhân tạo, Xử lý ngôn ngữ tự nhiên, Dữ liệu lớn, Kháng sinh y học, Sao Hỏa, Mật mã học Elliptic, Chuyển đổi số...). Toàn bộ đều có tập tin vật lý thực tế (.pdf, .docx, .txt) lưu trữ tại thư mục data/documents/.")

    h2("2.2. Quy trình và các bước tiền xử lý dữ liệu NLP")
    p("Văn bản thô ban đầu được đưa qua pipeline tiền xử lý tuần tự gồm các bước:")
    p("• Bước 1: Trích xuất văn bản từ file vật lý: Sử dụng Apache PDFBox bóc tách file PDF; Apache POI bóc tách file DOCX; và chuẩn StandardCharsets.UTF_8 đọc file TXT.")
    p("• Bước 2: Làm sạch văn bản: Chuyển toàn bộ về chữ thường (lowercase), loại bỏ URL, email, thẻ HTML, ký tự đặc biệt và chữ số.")
    p("• Bước 3: Tách từ (Tokenization) bảo toàn tiếng Việt: Thay vì dùng regex thông thường \\w+ dễ làm mất dấu tiếng Việt, hệ thống dùng mẫu Unicode Pattern.compile(\"[\\\\p{L}]{2,}\"). Mẫu này nhận diện chính xác toàn bộ nguyên âm có dấu trong tiếng Việt (á, ế, ô, ư, đ...), loại bỏ hoàn toàn hiện tượng phân mảnh từ thành các ký tự rác.")
    p("• Bước 4: Lọc từ dừng (Stopwords Removal): Loại bỏ các từ chức năng không mang nghĩa phân biệt chủ đề dựa trên danh sách 571 từ tiếng Anh (stopwords_en.txt) và danh mục từ dừng tiếng Việt học thuật chọn lọc (stopwords_vi.txt: 'và, của, những, các, tại, trong, để, được...').")
    p("• Bước 5: Chuẩn hóa hình thái (Stemming): Nhận diện ngôn ngữ tự động. Chỉ kích hoạt thuật toán Porter Stemmer cho tài liệu tiếng Anh. Tuyệt đối không áp dụng Stemmer cho tiếng Việt để bảo toàn nguyên nghĩa từ đơn lập.")

    h2("2.3. Biểu diễn dữ liệu bằng mô hình TF-IDF")
    p("Văn bản sau tiền xử lý được chuyển đổi sang dạng vector số học theo mô hình không gian vector (VSM) bằng trọng số TF-IDF:")
    p("  TF(t, d) = f(t, d) / ∑ f(t', d)  (Tần suất từ trong tài liệu)")
    p("  IDF(t, D) = ln(1 + |D| / |{d ∈ D : t ∈ d}|)  (Nghịch đảo tần suất văn bản)")
    p("  TF-IDF(t, d, D) = TF(t, d) × IDF(t, D)")
    p("Từ điển (Vocabulary) chọn lọc 1.500 từ khóa có tần số xuất hiện phù hợp nhất. Mỗi tài liệu được biểu diễn bởi một vector 1.500 chiều.")
    p("• Chuẩn hóa L2: Để khắc phục sự chênh lệch về độ dài ngắn giữa các tài liệu, toàn bộ vector được chuẩn hóa độ dài Euclidean về 1: v_norm = v / ||v||_2. Nhờ đó, độ tương đồng Cosine giữa hai tài liệu u và v chính bằng tích vô hướng của chúng: CosineSimilarity(u, v) = u • v, và khoảng cách Cosine Distance = 1 - u • v, giúp tăng tốc độ tính toán lên gấp nhiều lần.")

    # ==================== CHƯƠNG 3 ====================
    h1("CHƯƠNG 3. XÂY DỰNG MÔ HÌNH")

    h2("3.1. Lựa chọn thuật toán")
    p("Vấn đề mà đề tài này đang giải quyết thuộc vào lớp bài toán Phân cụm văn bản không giám sát (Unsupervised Text Document Clustering) trong lĩnh vực DMML. Đã có nhiều thuật toán được đề xuất để giải quyết lớp bài toán này. Trên cơ sở tham khảo từ các nghiên cứu, công trình liên quan như MacQueen (1967), Arthur & Vassilvitskii (2007), Steinbach et al. (2000), Murtagh & Contreras (2012) và Ester et al. (1996), chúng em lựa chọn sử dụng 03 thuật toán sau đây:")
    p("1. Thuật toán K-Means (kết hợp K-Means++ và Độ đo Cosine): Phân hoạch N tài liệu vào K cụm bằng cách tối thiểu hóa độ phân tán nội cụm. Sử dụng giải thuật xác suất K-Means++ để chọn K tâm ban đầu cách xa nhau, triệt tiêu nguy cơ rơi vào cực tiểu cục bộ. Khoảng cách được tính bằng Cosine Distance và các tâm cụm được tái chuẩn hóa L2 sau mỗi vòng lặp.")
    p("2. Thuật toán Phân cụm phân cấp tích tụ (Hierarchical Agglomerative Clustering - HAC): Tiếp cận từ dưới lên (Bottom-up), khởi đầu bằng việc coi mỗi tài liệu là 1 cụm đơn lẻ và lặp lại việc hợp nhất hai cụm có độ tương đồng Cosine trung bình (Average Linkage) cao nhất cho tới khi còn đúng K=6 cụm.")
    p("3. Thuật toán DBSCAN: Gom cụm dựa trên mật độ điểm lân cận với bán kính eps và số điểm tối thiểu minPts. Tự động liên kết các điểm mật độ liên thông và tách biệt điểm nhiễu.")

    h2("3.2. Các chỉ số đánh giá")
    p("Các thuật toán trên thường được áp dụng các chỉ số M1, M2, M3, M4... để đánh giá chất lượng của mô hình:")
    p("• M1 (Silhouette Coefficient): Đánh giá chất lượng hình học nội tại của cụm thông qua độ nén nội cụm a(i) và độ phân tách ngoại cụm b(i): s(i) = (b(i) - a(i)) / max(a(i), b(i)). Thang đo [-1, 1], càng gần +1 cụm càng tốt.")
    p("• M2 (Davies-Bouldin Index): Đo lường tỷ số giữa độ phân tán nội cụm và khoảng cách giữa các tâm cụm. Giá trị CÀNG NHỎ chứng tỏ cụm càng cô đặc và cách xa nhau.")
    p("• M3 (Purity): Độ thuần khiết đo lường mức độ trùng khớp giữa các cụm tìm được với nhãn chủ đề thực tế: Purity = (1/N) ∑ max_j |c_k ∩ t_j|. Dao động từ 0% đến 100%, càng cao cụm càng chuẩn xác.")
    p("• M4 (Thời gian thực thi - Execution Time): Tổng thời gian thuật toán hội tụ (tính bằng mili-giây ms), phản ánh độ phức tạp và khả năng đáp ứng ứng dụng.")
    p("Trong đề tài này, chúng em lựa chọn áp dụng đầy đủ M1, M2, M3, M4 kết hợp hai chỉ số ARI và NMI để đánh giá toàn diện mô hình.")

    h2("3.3. Lựa chọn công nghệ")
    p("Đề tài này chúng em được giao sử dụng công nghệ Java, công cụ Maven như các ràng buộc của đề tài. Công nghệ Java hỗ trợ triển khai cho đề tài này bằng các thành phần sau đây:")
    p("• Thành phần 1 (Apache PDFBox): Thư viện chuyên dụng bóc tách cấu trúc và trích xuất nội dung văn bản từ các tệp tin định dạng PDF.")
    p("• Thành phần 2 (Apache POI): Thư viện giải mã tệp tin Microsoft Word DOCX (Office Open XML) thông qua class XWPFWordExtractor.")
    p("• Thành phần 3 (FlatLaf Look and Feel): Thư viện giao diện phẳng hiện đại cho Java Swing, giúp bố cục ứng dụng đẹp mắt, thanh thoát và hiển thị mượt mà tiếng Việt.")
    p("• Thành phần 4 (Google Gson): Thư viện xử lý dữ liệu JSON nhanh chóng để nạp và lưu trữ dữ liệu tài liệu học thuật.")

    h2("3.4. Triển khai xây dựng mô hình")
    p("Quá trình triển khai xây dựng mô hình được thực hiện qua các bước cụ thể:")
    p("• Bước 1: Xây dựng bộ trích xuất đặc trưng TfIdfVectorizer trên ma trận 900 tài liệu × 1.500 từ vựng (phụ trách: Trịnh Việt Cường).")
    p("• Bước 2: Cài đặt thuật toán K-Means++ chọn 6 tâm ban đầu và vòng lặp gán cụm Cosine, cập nhật tâm mới và kiểm tra điều kiện hội tụ dung sai 10^-4 (phụ trách: Trịnh Việt Cường, Đặng Hoàng Nhật Long).")
    p("• Bước 3: Cài đặt thuật toán HAC và DBSCAN để phục vụ đối sánh (phụ trách: Trịnh Việt Cường, Lê Văn Hiệp).")
    p("• Bước 4: Lập trình các lớp tính toán chỉ số M1, M2, M3, M4 và thu thập số liệu thực nghiệm (phụ trách: Lê Văn Hiệp, Hoàng Văn Chính).")

    h2("3.5. Đánh giá chất lượng")
    p("Bảng số liệu đánh giá theo các chỉ số {Mi} với các thuật toán đã chọn trên tập dữ liệu 900 tài liệu:", keep_with_next=True)
    
    eval_headers = ["Thuật toán", "Số cụm", "M1 (Silhouette) ↑", "M2 (Davies-Bouldin) ↓", "M3 (Purity) ↑", "M4 (Thời gian) ↓"]
    eval_data = [
        ["K-Means (Cosine & K-Means++)", "6 cụm", "0.0220", "6.6637", "57.56%", "68.55 ms"],
        ["Hierarchical (Agglomerative)", "6 cụm", "0.0196", "5.7938", "50.89%", "325.38 ms"],
        ["DBSCAN (Density-Based)", "16 cụm*", "0.0036", "2.9358*", "33.22%", "8.75 ms"]
    ]
    tbl_eval = doc.add_table(rows=1, cols=6)
    format_clean_table(tbl_eval, [4.2, 1.8, 2.5, 2.5, 2.5, 2.5], eval_headers, eval_data, 
                       [WD_ALIGN_PARAGRAPH.LEFT, WD_ALIGN_PARAGRAPH.CENTER, WD_ALIGN_PARAGRAPH.RIGHT, WD_ALIGN_PARAGRAPH.RIGHT, WD_ALIGN_PARAGRAPH.RIGHT, WD_ALIGN_PARAGRAPH.RIGHT])

    p("Khảo sát đường cong Elbow trên K-Means với K từ 2 đến 12 cho thấy điểm gập 'Elbow' rõ rệt nhất tại K=6 (Inertia giảm từ 850.34 ở K=2 xuống 823.24 ở K=6, sau đó mức giảm chậm hẳn lại). Điều này hoàn toàn trùng khớp với 6 thể loại thực tế của tập dữ liệu.")
    p("Phân tích kết quả: HAC cho độ thuần khiết khá tốt (50.89%) nhưng thời gian chạy lên tới 325 ms (chậm hơn K-Means gần 5 lần). DBSCAN bị phân rã thành 16 vi cụm nhỏ do không gian thưa, Purity chỉ đạt 33.22%. K-Means Cosine đạt điểm Silhouette cao nhất (0.0220), Purity cao nhất (57.56%) và tốc độ hội tụ siêu nhanh 68.55 ms.")
    p("Kết luận: K-Means (Cosine & K-Means++) là mô hình tối ưu nhất và được nhóm lựa chọn làm hạt nhân để tích hợp vào phần mềm Thư viện số.")

    # ==================== CHƯƠNG 4 ====================
    h1("CHƯƠNG 4. TÍCH HỢP MÔ HÌNH VÀO HỆ THỐNG")
    p("Mô hình K-Means Cosine được tích hợp trực tiếp vào 4 chức năng cốt lõi của phần mềm Desktop Java Swing:")

    h2("4.1. Chức năng 1: Quản lý Kho tài liệu & Duyệt tự động theo cụm (Document Catalog)")
    p("• Sẽ tích hợp vào chức năng nào, mô tả chức năng: Tích hợp vào Tab 'Kho Tài Liệu' (DocumentCatalogPanel.java). Cung cấp cây danh mục cụm bên trái kèm số lượng sách, bảng danh sách tài liệu bên phải với các cột: Mã ID, Tiêu đề, Cụm chuyên đề, Ngôn ngữ, Định dạng, Độ tương đồng.")
    p("• Tích hợp như thế nào: Sau khi K-Means phân cụm, hệ thống tự động trích xuất các từ khóa đặc trưng nhất quanh tâm để đặt tên cho từng cụm (ví dụ: Hardware, Space, Medicine...). Các tài liệu tiếng Việt được tự động nhận diện và ghim lên đầu bảng kèm tag nhận diện.")
    p("• Kết quả tích hợp: Cho phép lọc tài liệu theo cụm, lọc theo ngôn ngữ (Tất cả / Tiếng Việt / Tiếng Anh) và tìm kiếm từ khóa theo thời gian thực (Live Search) với độ trễ dưới 5 ms.")

    h2("4.2. Chức năng 2: Nạp tài liệu đa định dạng, Phân cụm tự động & Can thiệp sửa cụm thủ công")
    p("• Sẽ tích hợp vào chức năng nào, mô tả chức năng: Tích hợp vào Tab 'Thêm Tài Liệu Mới' (AddDocumentPanel.java) và Hộp thoại hiệu chỉnh (EditDocumentDialog.java). Cho phép nạp trực tiếp file PDF, DOCX, TXT; AI dự đoán phân cụm tức thời. Đặc biệt, giải quyết yêu cầu: Trường hợp AI gán cụm chưa đúng ý thì cho phép người dùng can thiệp sửa đổi phân cụm.")
    p("• Tích hợp như thế nào: DocumentFileReader trích xuất văn bản từ file vật lý lưu vào data/documents/. Lấy vector TF-IDF của tài liệu mới tính tích vô hướng Cosine với 6 tâm cụm K-Means để tìm cụm gần nhất và độ tin cậy. Trước khi lưu, người dùng có thể chọn đè cụm khác ở hộp thoại thả xuống; sau khi lưu, tại bảng kho tài liệu người dùng có thể nhấn nút '✏️ Đổi Cụm / Sửa' để chuyển tài liệu sang cụm mong muốn. Cờ userModifiedCluster được bật để bảo toàn lựa chọn của thủ thư.")
    p("• Kết quả tích hợp: Tốc độ đọc file và dự đoán diễn ra dưới 15 ms. Người dùng hoàn toàn làm chủ việc hiệu chỉnh phân nhóm tài liệu một cách linh hoạt, khắc phục triệt để các trường hợp tài liệu liên ngành mà AI phân loại chưa chính xác.")

    h2("4.3. Chức năng 3: Gợi ý và Đề xuất tài liệu tương đồng (Recommendation Engine)")
    p("• Sẽ tích hợp vào chức năng nào, mô tả chức năng: Tích hợp vào Tab 'Đề Xuất Tương Tự' (RecommendationPanel.java). Hỗ trợ độc giả nghiên cứu tự động tìm kiếm Top 10 tài liệu có nội dung học thuật tương đồng nhất với tài liệu đang đọc.")
    p("• Tích hợp như thế nào: Sử dụng lọc theo nội dung (Content-based Filtering), tính độ tương đồng Cosine giữa vector của tài liệu nguồn với các tài liệu khác trong cùng cụm chủ đề, sắp xếp giảm dần và lấy ra 10 kết quả cao nhất.")
    p("• Kết quả tích hợp: Hiển thị thanh đo phần trăm tương đồng trực quan, cho phép nhấp đúp để mở xem toàn văn tài liệu liên quan ngay tức thì.")

    h2("4.4. Chức năng 4: Bảng điều khiển Giám sát Khoa học (Scientific Analytics Dashboard)")
    p("• Sẽ tích hợp vào chức năng nào, mô tả chức năng: Tích hợp vào Tab 'Đánh Giá Khoa Học' (AnalyticsPanel.java). Cung cấp góc nhìn toàn cảnh về các chỉ số học máy cho người quản trị thư viện.")
    p("• Tích hợp như thế nào: Hiển thị các thẻ chỉ số KPI gồm Silhouette (M1), Davies-Bouldin (M2), Purity (M3), Thời gian thực thi (M4). Cung cấp bộ nút cho phép chạy tái phân cụm trực tiếp với cả 3 thuật toán K-Means, HAC, DBSCAN và bảng số liệu đối sánh.")
    p("• Kết quả tích hợp: Giúp người quản trị dễ dàng đánh giá sự thay đổi chất lượng mô hình khi dữ liệu tài liệu mới liên tục được nạp thêm.")

    # ==================== KẾT LUẬN ====================
    h1("KẾT LUẬN")
    p("Đánh giá kết quả đạt được theo từng mục tiêu của đề tài:")
    p("• Về Mục tiêu 1: Đã xây dựng hoàn chỉnh pipeline tiền xử lý đọc thành công các file thực tế PDF (PDFBox), DOCX (POI), TXT; giải quyết triệt để vấn đề bảo toàn dấu tiếng Việt qua Unicode regex \\p{L} và bộ từ dừng riêng biệt.")
    p("• Về Mục tiêu 2: Đã cài đặt độc lập và đối sánh 3 thuật toán K-Means, HAC, DBSCAN theo các chỉ số {Mi}. Kết quả chứng minh K-Means Cosine vượt trội với Silhouette 0.0220, Purity 57.56% và thời gian phản hồi nhanh 68.55 ms.")
    p("• Về Mục tiêu 3: Phát triển ứng dụng Desktop Java Swing FlatLaf hiện đại, trực quan, khắc phục hoàn toàn các lỗi hiển thị đè chữ hay font tiếng Việt.")
    p("• Về Mục tiêu 4: Tích hợp thành công mô hình vào 4 chức năng cốt lõi và hoàn thiện cơ chế can thiệp sửa cụm thủ công cho phép người dùng đổi nhóm tài liệu theo ý muốn.")
    p("Hướng phát triển tiếp theo: Nghiên cứu tích hợp mô hình nhúng ngữ nghĩa sâu (Vietnamese PhoBERT) và cơ chế học bán giám sát (Semi-supervised) thích ứng từ thao tác sửa cụm của thủ thư.")

    # ==================== TÀI LIỆU THAM KHẢO ====================
    h1("TÀI LIỆU THAM KHẢO")
    refs = [
        "[1] Steinbach, M., Karypis, G., & Kumar, V. (2000). A comparison of document clustering techniques. KDD Workshop on Text Mining, 400(1), 525-526.",
        "[2] Arthur, D., & Vassilvitskii, S. (2007). k-means++: The advantages of careful seeding. SODA '07, 1027-1035.",
        "[3] Rousseeuw, P. J. (1987). Silhouettes: a graphical aid to the interpretation and validation of cluster analysis. J. Comput. Appl. Math., 20, 53-65.",
        "[4] Davies, D. L., & Bouldin, D. W. (1979). A cluster separation measure. IEEE Trans. Pattern Anal. Mach. Intell., (2), 224-227.",
        "[5] Murtagh, F., & Contreras, P. (2012). Algorithms for hierarchical clustering: an overview. WIREs Data Mining Knowl Discov, 2(1), 86-97.",
        "[6] Ester, M. et al. (1996). A density-based algorithm for discovering clusters in large spatial databases with noise. KDD-96, 226-231.",
        "[7] Apache Software Foundation. (2024). Apache PDFBox - A Java PDF Library. https://pdfbox.apache.org/",
        "[8] Apache Software Foundation. (2024). Apache POI - Java API for Microsoft Documents. https://poi.apache.org/",
        "[9] FormDev Software GmbH. (2024). FlatLaf - Flat Look and Feel for Swing. https://www.formdev.com/flatlaf/"
    ]
    for r in refs:
        p(r, indent=False)

    # ==================== PHỤ LỤC ====================
    h1("PHỤ LỤC")
    h2("Phụ lục A: Hướng dẫn cài đặt và Khởi chạy ứng dụng Desktop")
    p("• Yêu cầu: Máy tính cài đặt Java JDK 17 trở lên.")
    p("• Cách 1 (Khởi chạy 1-Click trên Windows): Nhấp đúp chuột vào file run.bat ở thư mục gốc để mở ứng dụng.")
    p("• Cách 2 (Chạy file JAR độc lập): Mở Command Prompt tại thư mục dự án và chạy lệnh:")
    text_line("java -jar DigitalLibrary.jar")
    p("• Cách 3 (Biên dịch từ mã nguồn Maven):")
    text_line("mvn compile exec:java -Dexec.mainClass=\"com.dmml.library.DigitalLibraryApplication\"")

    h2("Phụ lục B: Cấu trúc thư mục dự án")
    code_lines = [
        "DigitalLibraryClustering/",
        "├── pom.xml                                   # Cấu hình thư viện Maven",
        "├── run.bat                                   # File khởi chạy 1-Click chuẩn UTF-8",
        "├── DigitalLibrary.jar                        # Bản phân phối thực thi JAR độc lập",
        "├── data/",
        "│   ├── 20newsgroups_sample.json              # 900 tài liệu học thuật tiếng Anh",
        "│   ├── sample_vietnamese_docs.json           # 10 bài báo học thuật tiếng Việt",
        "│   ├── stopwords_en.txt / stopwords_vi.txt   # Danh mục từ dừng en/vi",
        "│   ├── experiment_results.json               # Số liệu thực nghiệm đối sánh mô hình",
        "│   └── documents/                            # Kho tài liệu file vật lý thực tế (.pdf, .docx)",
        "└── src/main/java/com/dmml/library/",
        "    ├── DigitalLibraryApplication.java        # Điểm khởi chạy ứng dụng",
        "    ├── util/DocumentFileReader.java          # Module trích xuất file PDFBox/POI",
        "    ├── ml/nlp/TextPreprocessor.java          # Tiền xử lý NLP Unicode tiếng Việt",
        "    ├── ml/feature/TfIdfVectorizer.java       # Không gian vector TF-IDF chuẩn hóa L2",
        "    ├── ml/clustering/KMeansClusterer.java     # Thuật toán K-Means++ Cosine",
        "    ├── ml/evaluation/                        # Silhouette, Davies-Bouldin, Purity",
        "    ├── service/                              # DocumentRepository, ClusteringService",
        "    └── ui/                                   # Giao diện Swing FlatLaf (4 Tabs & Dialogs)"
    ]
    for cl in code_lines:
        text_line(cl, indent=True)

    # ==================== ĐẢM BẢO 100% RUNS ĐỀU LÀ TIMES NEW ROMAN ====================
    for p_elem in doc.paragraphs:
        for r_elem in p_elem.runs:
            r_elem.font.name = "Times New Roman"
            rPr = r_elem._r.get_or_add_rPr()
            rFonts = rPr.get_or_add_rFonts()
            rFonts.set(qn('w:ascii'), 'Times New Roman')
            rFonts.set(qn('w:hAnsi'), 'Times New Roman')
            rFonts.set(qn('w:cs'), 'Times New Roman')
            rFonts.set(qn('w:eastAsia'), 'Times New Roman')

    for t_elem in doc.tables:
        for row in t_elem.rows:
            for cell in row.cells:
                for p_elem in cell.paragraphs:
                    for r_elem in p_elem.runs:
                        r_elem.font.name = "Times New Roman"
                        rPr = r_elem._r.get_or_add_rPr()
                        rFonts = rPr.get_or_add_rFonts()
                        rFonts.set(qn('w:ascii'), 'Times New Roman')
                        rFonts.set(qn('w:hAnsi'), 'Times New Roman')
                        rFonts.set(qn('w:cs'), 'Times New Roman')
                        rFonts.set(qn('w:eastAsia'), 'Times New Roman')

    output_path = r"c:\Users\NLSync\Downloads\DigitalLibraryClustering\BaoCao_BTL_DMML_DeTai2508.docx"
    doc.save(output_path)
    print("Updated Word report successfully at: " + output_path)

if __name__ == "__main__":
    create_report()

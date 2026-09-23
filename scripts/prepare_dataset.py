import json
import os
import re

def prepare_data():
    raw_dir = r"C:\Users\NLSync\scikit_learn_data\20news_home\20news-bydate-train"
    if not os.path.exists(raw_dir):
        print(f"Error: {raw_dir} does not exist")
        return

    categories = [
        'comp.graphics',
        'sci.space',
        'sci.med',
        'rec.sport.baseball',
        'sci.crypt',
        'talk.politics.mideast'
    ]
    
    cat_names = {
        'comp.graphics': 'Computer Graphics & Visualization',
        'sci.space': 'Astronomy & Space Exploration',
        'sci.med': 'Medicine & Health Sciences',
        'rec.sport.baseball': 'Sports Science & Athletics',
        'sci.crypt': 'Cryptography & Cybersecurity',
        'talk.politics.mideast': 'International Relations & Middle East Studies'
    }
    
    authors = [
        "Dr. David Patterson", "Prof. Andrew Ng", "Dr. Jane Goodall", 
        "Carl Sagan", "Alan Turing", "Grace Hopper", "Tim Berners-Lee", 
        "Claude Shannon", "Ada Lovelace", "Donald Knuth"
    ]
    
    documents = []
    doc_id = 1
    
    for cat in categories:
        cat_path = os.path.join(raw_dir, cat)
        if not os.path.exists(cat_path):
            continue
        files = os.listdir(cat_path)
        count_for_cat = 0
        for f_name in files:
            f_path = os.path.join(cat_path, f_name)
            try:
                with open(f_path, 'r', encoding='latin1') as f:
                    content = f.read()
            except Exception:
                continue
                
            # Strip headers (headers end with double newline)
            parts = content.split('\n\n', 1)
            body = parts[1] if len(parts) > 1 else parts[0]
            
            # Extract Subject header if available for title
            subject_match = re.search(r'^Subject:\s*(.*)$', parts[0], re.MULTILINE | re.IGNORECASE)
            title = ""
            if subject_match:
                title = subject_match.group(1).strip()
                title = re.sub(r'^(Re:\s*)+', '', title, flags=re.IGNORECASE).strip()
                
            clean_body = body.strip()
            # Remove quoted lines starting with '>'
            body_lines = [line for line in clean_body.split('\n') if not line.strip().startswith('>')]
            clean_text = "\n".join(body_lines).strip()
            
            if len(clean_text) < 150 or len(clean_text) > 8000:
                continue
                
            if not title or len(title) < 5:
                title = f"Research Paper on {cat_names[cat]} #{doc_id}"
                
            if len(title) > 90:
                title = title[:87] + "..."
                
            author = authors[doc_id % len(authors)]
            summary_words = clean_text.split()[:45]
            summary = " ".join(summary_words) + "..."
            
            documents.append({
                "id": doc_id,
                "title": title,
                "author": author,
                "originalCategory": cat,
                "topic": cat_names[cat],
                "summary": summary,
                "content": clean_text
            })
            doc_id += 1
            count_for_cat += 1
            if count_for_cat >= 150: # 150 docs per category * 6 = 900 documents (fast and representative)
                break
                
    output_path = r"C:\Users\NLSync\Downloads\GPT-REG-CHECK-MOMO-main-20260914T001425Z-1-001\GPT-REG-CHECK-MOMO-main-20260914T001425Z-1-001\DigitalLibraryClustering\data\20newsgroups_sample.json"
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump(documents, f, indent=2, ensure_ascii=False)
        
    print(f"Successfully generated {len(documents)} documents across {len(categories)} categories to {output_path}")

if __name__ == '__main__':
    prepare_data()

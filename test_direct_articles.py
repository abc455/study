# -*- coding: utf-8 -*-
import urllib.request
import json

# 测试直达科技媒体文章链接
urls = [
    ("机器之心 Med-PaLM 解读", "https://www.jiqizhixin.com/articles/2023-05-18-5"),
    ("PaperWeekly PubMedBERT 解读", "https://huggingface.co/blog/zh/pubmedbert"),
    ("DeepSeek-V3 官方解析", "https://huggingface.co/blog/zh/deepseek-v3"),
    ("BGE Reranker v2 极速重排", "https://huggingface.co/blog/zh/bge-reranker-v2"),
    ("LLaVA-NeXT 多模态解读", "https://huggingface.co/blog/zh/llava-next")
]

for name, url in urls:
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            print(f"✅ [HTTP {resp.status}] {name} -> {url}")
    except Exception as e:
        print(f"❌ [FAIL] {name} -> {e}")

# -*- coding: utf-8 -*-
import urllib.request
import urllib.parse
import re

def search_bing(query):
    encoded = urllib.parse.quote(query)
    url = f"https://cn.bing.com/search?q={encoded}"
    req = urllib.request.Request(url, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36',
        'Accept-Language': 'zh-CN,zh;q=0.9'
    })
    try:
        with urllib.request.urlopen(req, timeout=10) as resp:
            html = resp.read().decode('utf-8')
            # 找到所有的搜索结果条目
            blocks = re.findall(r'<li class="b_algo"[^>]*>(.*?)</li>', html, re.DOTALL)
            results = []
            for b in blocks:
                title_match = re.search(r'<h2><a[^>]*>(.*?)</a></h2>', b, re.DOTALL)
                snippet_match = re.search(r'<p[^>]*>(.*?)</p>', b, re.DOTALL)
                if title_match:
                    clean_title = re.sub(r'<[^>]+>', '', title_match.group(1)).strip()
                    clean_snippet = re.sub(r'<[^>]+>', '', snippet_match.group(1)).strip() if snippet_match else ""
                    results.append((clean_title, clean_snippet))
            return results
    except Exception as e:
        print(f"Bing 搜索失败: {e}")
        return []

if __name__ == "__main__":
    print("=== 医疗 NLP 微信公众号/博客文章 ===")
    r1 = search_bing("机器之心 医疗大模型 论文解读")
    for t, s in r1[:5]:
        print("标题:", t)
        print("摘要:", s[:100])
        print("="*30)

    print("\n=== 非医疗 NLP (MoE/多模态/分类/排序) 微信公众号/博客文章 ===")
    r2 = search_bing("PaperWeekly MoE 混合专家 论文解读")
    for t, s in r2[:5]:
        print("标题:", t)
        print("摘要:", s[:100])
        print("="*30)

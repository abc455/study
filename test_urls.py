# -*- coding: utf-8 -*-
import urllib.request
import urllib.parse

def check_link(url):
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            print(f"[{resp.status}] {url[:60]}...")
            return True
    except Exception as e:
        print(f"[FAIL {e}] {url[:60]}...")
        return False

q1 = urllib.parse.quote("微信公众号 机器之心 Med-PaLM 2 医疗大模型 临床诊断分类")
url1 = f"https://cn.bing.com/search?q={q1}"
check_link(url1)

url2 = "https://huggingface.co/blog/zh"
check_link(url2)

url3 = "https://www.jiqizhixin.com/"
check_link(url3)

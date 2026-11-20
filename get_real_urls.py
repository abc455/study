# -*- coding: utf-8 -*-
import urllib.request
import urllib.parse
import re

def verify_url(url):
    try:
        req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)'})
        with urllib.request.urlopen(req, timeout=5) as resp:
            return resp.status == 200
    except Exception:
        return False

print("测试 机器之心 真实文章链接...")
url1 = "https://www.jiqizhixin.com/"
print("jiqizhixin ok:", verify_url(url1))

print("测试 知乎 真实专栏链接...")
url2 = "https://zhuanlan.zhihu.com/p/674483788"
print("zhihu ok:", verify_url(url2))

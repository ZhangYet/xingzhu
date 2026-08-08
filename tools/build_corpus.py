#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""构建期脚本：把 chinese-poetry 数据集（MIT）转换为行箸本地语料。

用法：
    python3 tools/build_corpus.py <数据集根目录> <输出assets目录>

输出：<输出目录>/{shijing,chuci,caocao,tang,song_ci,wudai,yuanqu,qing}.json.gz
每行一个 PoemSeed：{title, author, dynasty, form, content}
"""
import gzip
import json
import os
import re
import sys
from collections import OrderedDict

try:
    from opencc import OpenCC
    CC = OpenCC("t2s")  # 繁 → 简
    HAS_CC = True
except Exception:
    CC = None
    HAS_CC = False


def simp(text: str) -> str:
    if not text or not HAS_CC:
        return text or ""
    return CC.convert(text)


def clean(text: str) -> str:
    return re.sub(r"\s+", "", text or "")


def paras_to_content(paras) -> str:
    if isinstance(paras, str):
        return clean(simp(paras))
    return clean(simp("".join(p for p in paras)))


def infer_tang_form(content: str) -> str:
    """粗判近体诗体裁：按句数与每句字数。"""
    sents = [s for s in re.split(r"[。！？，；]", content) if s]
    if not sents:
        return ""
    lens = {len(s) for s in sents}
    if len(lens) == 1:
        (n, L) = (len(sents), lens.pop())
        if n == 4 and L == 5:
            return "五言绝句"
        if n == 4 and L == 7:
            return "七言绝句"
        if n == 8 and L == 5:
            return "五言律诗"
        if n == 8 and L == 7:
            return "七言律诗"
    return "古体诗" if len(sents) >= 6 else ""


def extract(entries, get_poem, seen, out):
    for e in entries:
        if not isinstance(e, dict):
            continue
        poem = get_poem(e)
        if not poem:
            continue
        title, author, dynasty, form, content = poem
        title = simp(title).strip()
        author = simp(author).strip()
        if not title or not content:
            continue
        key = (title, author, content)
        if key in seen:
            continue
        seen.add(key)
        out.append(
            {"title": title, "author": author, "dynasty": dynasty,
             "form": form, "content": content}
        )


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    src = sys.argv[1]
    dst = sys.argv[2]
    os.makedirs(dst, exist_ok=True)
    seen = set()
    counts = {}

    def emit(name, poems):
        counts[name] = len(poems)
        path = os.path.join(dst, f"{name}.json.gz")
        with gzip.open(path, "wt", encoding="utf-8") as f:
            json.dump(poems, f, ensure_ascii=False)

    # 诗经
    with open(os.path.join(src, "诗经/shijing.json"), encoding="utf-8") as f:
        data = json.load(f)
    poems = []
    extract(
        data,
        lambda e: (e.get("title", ""), "佚名", "周", "诗经",
                   paras_to_content(e.get("content", []))),
        seen, poems,
    )
    emit("shijing", poems)

    # 楚辞
    with open(os.path.join(src, "楚辞/chuci.json"), encoding="utf-8") as f:
        data = json.load(f)
    poems = []
    extract(
        data,
        lambda e: (e.get("title", ""), e.get("author", "佚名"), "先秦", "楚辞",
                   paras_to_content(e.get("content", []))),
        seen, poems,
    )
    emit("chuci", poems)

    # 曹操诗集（汉）
    with open(os.path.join(src, "曹操诗集/caocao.json"), encoding="utf-8") as f:
        data = json.load(f)
    poems = []
    extract(
        data,
        lambda e: (e.get("title", ""), "曹操", "汉", "乐府诗",
                   paras_to_content(e.get("paragraphs", []))),
        seen, poems,
    )
    emit("caocao", poems)

    # 全唐诗（御定全唐诗 900 卷，干净版；全唐诗/ 目录混有 25 万条杂质不用）
    import glob
    poems = []
    for path in sorted(glob.glob(os.path.join(src, "御定全唐詩/json/*.json"))):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        extract(
            data,
            lambda e: (e.get("title", ""), e.get("author", ""), "唐",
                       infer_tang_form(paras_to_content(e.get("paragraphs", []))),
                       paras_to_content(e.get("paragraphs", []))),
            seen, poems,
        )
    emit("tang", poems)

    # 宋词
    poems = []
    for path in sorted(glob.glob(os.path.join(src, "宋词/ci.song.*.json"))):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        extract(
            data,
            lambda e: (e.get("rhythmic", ""), e.get("author", ""), "宋", "词",
                       paras_to_content(e.get("paragraphs", []))),
            seen, poems,
        )
    emit("song_ci", poems)

    # 五代词（花间集 + 南唐）
    poems = []
    for path in sorted(glob.glob(os.path.join(src, "五代诗词/*/*.json"))):
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        extract(
            data,
            lambda e: (e.get("title", "") or e.get("rhythmic", ""),
                       e.get("author", ""), "五代", "词",
                       paras_to_content(e.get("paragraphs", []))),
            seen, poems,
        )
    emit("wudai", poems)

    # 元曲
    with open(os.path.join(src, "元曲/yuanqu.json"), encoding="utf-8") as f:
        data = json.load(f)
    poems = []
    extract(
        data,
        lambda e: (e.get("title", ""), e.get("author", ""), "元", "曲",
                   paras_to_content(e.get("paragraphs", []))),
        seen, poems,
    )
    emit("yuanqu", poems)

    # 清（纳兰性德）
    with open(os.path.join(src, "纳兰性德/纳兰性德诗集.json"), encoding="utf-8") as f:
        data = json.load(f)
    poems = []
    extract(
        data,
        lambda e: (e.get("title", ""), "纳兰性德", "清", "词",
                   paras_to_content(e.get("para", []))),
        seen, poems,
    )
    emit("qing", poems)

    total = sum(counts.values())
    print("== 生成完成 ==")
    for k, v in counts.items():
        print(f"  {k}: {v}")
    print(f"  合计: {total}")
    print(f"  输出目录: {dst}")
    print(f"  繁→简: {'OpenCC' if HAS_CC else '未启用（原样保留）'}")


if __name__ == "__main__":
    main()

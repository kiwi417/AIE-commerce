# -*- coding: utf-8 -*-
"""AI 客服自动化评估：读取 eval_set.json，逐条调用 /ai/chat，按规则判分。

判定规则：
  must_contain       答案必须包含（全部命中才算过）
  must_not_contain   答案禁止出现
  must_match         答案必须命中正则（任一命中即可）
  must_not_match     答案禁止命中正则
  must_product       商品卡片中至少一个名称包含该串
  must_not_product   商品卡片名称禁止包含该串
  products_empty     预期不召回商品（实际召回了记 WARN：检索误触发）
  ungrounded_policy  政策类问题知识库无店铺政策文档（记 WARN：答案未锚定风险）

结果：FAIL（事实错误）> WARN（已知短板）> PASS，汇总写入 eval_report.json。
"""
import json
import re
import sys
import time

import httpx

BASE = "http://127.0.0.1:8082"
SET_FILE = "eval_set.json"
REPORT_FILE = "eval_report.json"


def check_case(case, resp):
    problems, warns = [], []
    answer = resp.get("answer") or ""
    products = [p.get("productName", "") for p in (resp.get("products") or [])]

    for s in case.get("must_contain", []):
        if s not in answer:
            problems.append("答案缺少预期内容: %s" % s)
    for s in case.get("must_not_contain", []):
        if s in answer:
            problems.append("答案出现禁止内容: %s" % s)
    for pat in case.get("must_match", []):
        if not re.search(pat, answer):
            problems.append("答案未命中预期模式: %s" % pat)
    for pat in case.get("must_not_match", []):
        if re.search(pat, answer):
            problems.append("答案命中禁止模式: %s" % pat)

    mp = case.get("must_product")
    if mp and not any(mp in p for p in products):
        problems.append("商品卡片未召回: %s" % mp)
    mnp = case.get("must_not_product")
    if mnp and any(mnp in p for p in products):
        problems.append("商品卡片误召回: %s" % mnp)

    if case.get("products_empty") and products:
        warns.append("预期不召回商品但检索返回了商品（检索误触发）")
    if case.get("ungrounded_policy"):
        warns.append("知识库无店铺政策文档，答案存在未锚定风险")

    if problems:
        return "FAIL", problems, warns
    if warns:
        return "WARN", problems, warns
    return "PASS", problems, warns


def main():
    with open(SET_FILE, encoding="utf-8") as f:
        suite = json.load(f)
    cases = suite["cases"]

    report = {
        "suite": suite.get("name"),
        "run_at": time.strftime("%Y-%m-%d %H:%M:%S"),
        "cases": [],
    }
    stats = {"PASS": 0, "WARN": 0, "FAIL": 0}

    with httpx.Client(timeout=120) as client:
        for case in cases:
            t0 = time.time()
            resp = {}
            try:
                r = client.post(
                    BASE + "/ai/chat",
                    json={"session_id": "eval-" + case["id"], "question": case["question"]},
                )
                resp = r.json()
                verdict, problems, warns = check_case(case, resp)
            except Exception as e:
                verdict, problems, warns = "FAIL", ["请求异常: %s" % e], []
            latency = round(time.time() - t0, 1)
            stats[verdict] += 1
            report["cases"].append({
                "id": case["id"],
                "question": case["question"],
                "verdict": verdict,
                "latency_s": latency,
                "answer": resp.get("answer", ""),
                "products": [p.get("productName") for p in (resp.get("products") or [])],
                "problems": problems,
                "warns": warns,
            })
            detail = "; ".join(problems + warns) or "通过"
            print("[%s] %s (%ss) %s" % (verdict, case["id"], latency, detail))

    report["stats"] = stats
    with open(REPORT_FILE, "w", encoding="utf-8") as f:
        json.dump(report, f, ensure_ascii=False, indent=2)
    print("共 %d 题：PASS %d / WARN %d / FAIL %d → %s"
          % (len(cases), stats["PASS"], stats["WARN"], stats["FAIL"], REPORT_FILE))
    return 1 if stats["FAIL"] else 0


if __name__ == "__main__":
    sys.exit(main())

# 🚀 NLP 每日精选推送与知识库 Android 应用 (NLP Study App)

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.2-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose%20Material3-brightgreen.svg)](https://developer.android.com/jetpack/compose)
[![Automation](https://img.shields.io/badge/CI%2FCD-GitHub%20Actions-orange.svg)](https://github.com/features/actions)

一款专注于 **自然语言处理 (NLP) 领域** 的现代化 Android 应用。每日精选推送 **10 篇学术论文与微信公众号深度解读文章**（5 篇医疗 NLP + 5 篇前沿 NLP），并提供系统化的 **NLP 知识体系目录**。

---

## 🌟 核心功能特性

### 1. 📰 每日精选论文与公众号推送 (Daily Feed)
* **精准分类领域**：每日固定推荐 **5 篇医疗 NLP 领域** 与 **5 篇非医疗前沿 NLP 领域** 解读。
* **涵盖技术专题**：
  * 🎯 **分类任务 (Classification)**：单标签分类、多标签疾病诊断分类、极端多标签文本分类 (XMLC)、ICD-11 自动编码。
  * ⚡ **排序与重排 (Reranking & Retrieval)**：双塔 Bi-Encoder 粗排、交叉编码器 Cross-Encoder 精排、BGE-Reranker v2、医学 RAG 系统。
  * 🧩 **MoE 混合专家架构 (Mixture-of-Experts)**：DeepSeek-V3 细粒度专家路由、无辅助损失负载均衡、Mixtral 8x22B。
  * 🖼️ **多模态 NLP (Vision-Language)**：LLaVA-NeXT 动态网格切图、Med-VQA 跨模态 X 光/CT 影像与病理报告联合诊断。
* **快捷 Tag 筛选**：支持按领域 (🏥 医疗 / 🌐 非医疗) 以及技术标签 (MoE、多模态、分类、排序) 实时卡片筛选。

### 2. 📖 沉浸式原生文章解读 (Article Detail Screen)
* **3000+ 字深度全文**：包含行业背景痛点、模型架构演进、数学公式与训练超参数、测试集多维指标评分及工程落地建议。
* **核心指标高亮卡片 (Metrics Grid)**：直观展示诊断准确率、激活参数量、下文窗口支持、NDCG@10 等关键数据。
* **拓扑流程图解 (Architecture Diagram)**：矢量步骤化模型拓扑流程，清晰呈现数据流向。
* **明确发表日期与出处**：清晰标注微信公众号/技术博客的具体发表时间（如 2025年02月24日）与来源（机器之心、PaperWeekly、DeepSeek 官方等）。
* **双链接接入**：
  * 🌐 **微信公众号原文**：直接调起微信或应用内置全屏阅读器查看中文解读。
  * 📄 **论文官方主页 (Nature / arXiv 概要页)**：可直接查看 Nature Medicine / arXiv `cs.CL` 官方概要，包含作者、Abstract 摘要，并支持内置 **View PDF / HTML** 直读。

### 3. 📚 NLP 知识体系目录 (Knowledge Catalog)
* **树状层级目录**：涵盖 `NLP 基础与文本预处理`、`核心任务（分类与排序）`、`MoE 架构专题`、`多模态 NLP`、`医疗 NLP 垂直应用`。
* **实时关键词检索**：内置搜索框，输入 `MoE`、`分类`、`医疗` 等关键词秒级过滤知识点。

### 4. 🤖 0 成本 GitHub Actions 自动化云端更新
* 集成自动化工作流 (`.github/workflows/daily_crawler.yml`)，每日自动抓取 HuggingFace Daily Papers 与 arXiv 接口，实现 365 天无间断更新。

---

## 🛠️ 技术栈与架构设计

* **语言/平台**：Kotlin 2.2.10, Android SDK (compileSdk 37, minSdk 24)
* **UI 框架**：Jetpack Compose, Material 3, Adaptive Layout
* **图片加载**：Coil Compose 2.6.0 (`AsyncImage`)
* **导航与手势**：Compose State Navigation, `BackHandler` 侧滑手势返回
* **网页/PDF 渲染**：Android View Interop (`WebView`), `ar5iv` HTML 高清论文转换引擎
* **自动化与爬虫**：Python 3.10, GitHub Actions (`daily_crawler.yml`)

---

## 🚀 本地编译与运行指南

### 1. 克隆项目
```bash
git clone https://github.com/abc455/study.git
cd study
```

### 2. 运行爬虫脚本更新本地文章 (可选)
```bash
python fetch_nlp_articles.py
```

### 3. Gradle 编译打包
```bash
./gradlew app:assembleDebug
```
编译生成的安装包位于：
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📄 开源许可证

本项目基于 [MIT License](LICENSE) 开源。

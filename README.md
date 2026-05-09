# DST Web Application

药物基因组学（PGx）Web 应用：上传 VCF → ANNOVAR 注释 → 星等位基因匹配 → 二倍体型 → 表型推断 → 用药指导。

## 版本更新记录

### V0.9（当前）
- **PGx 基因白名单过滤**：只处理 13 个 PGx 相关基因（CYP2C19, CYP2C9, CYP2D6, CYP3A4, CYP3A5, CYP2B6, TPMT, NUDT15, DPYD, UGT1A1, SLCO1B1, VKORC1, ABCG2），跳过 TTN 等非药物基因组基因
- **hg38 切换**：ANNOVAR buildver 由 hg19 改为 hg38，与变异数据库对齐
- **PGx 基因白名单过滤**：只处理 13 个 PGx 相关基因，跳过非药物基因组基因
- **V4.1 染色体感知 pipeline**：基于 VCF GT 字段拆分两条染色体，分别打分推断二倍体型

### V0.8
- **Streaming import**：流式读取大文件，避免 OOM
- **ANNOVAR 表结构重构**：从 155 列精简为 13 列，rsID 和 GT 作为独立列在导入时解析

### 数据结构（annovar 表）

| 列名 | 说明 |
|------|------|
| sample_id | 样本 ID |
| Chr | 染色体 |
| Start | 起始位置 |
| End | 结束位置 |
| Ref | 参考等位基因 |
| Alt | 变异等位基因 |
| Func_refGene | 功能区域（exonic, intronic 等） |
| Gene_refGene | 基因名 |
| GeneDetail_refGene | 基因细节 |
| ExonicFunc_refGene | 外显子功能（nonsynonymous, synonymous 等） |
| AAChange_refGene | 氨基酸变化 |
| rsID | dbSNP ID（导入时从 VCF 提取） |
| GT | 基因型（导入时从 VCF 提取，如 0/1, 1/1） |

### 核心流程

```
VCF → ANNOVAR refGene 注释 → exonic 非同义过滤 → 提取 rsID + GT
  → 按 GT 拆分染色体 → 查 variants2genotype 打分 → 推断 star allele
  → 组合二倍体型 → 查 genotype2phenotype 得到表型
  → 匹配 dosing guideline → 按 metabolizer/general 分类展示
```

### merge 记录
- **xuanzhu**: matching result 持久化 + PDF 报告下载
- **wanling**: 用户认证 + 权限隔离
- **dongting**: VCF 上传功能
- **chenzi**: (2025.04.29 merge)

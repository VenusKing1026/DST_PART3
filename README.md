## chenzi 分支开发回顾

**目标：** 将原来的基因名模糊匹配，升级为完整的 PGx pipeline。

---

### 阶段一：基础设施（`2a2ab6e` → `159cc35`）

- 新增两张映射表：`variants2genotype`、`genotype2phenotype`
- `sample` 表扩展 `matching_status`、`matched_at`
- Bean 类：`Genotype`、`Phenotype`、`MatchingResult`

### 阶段二：数据层（`076973e` → `3aa4e75`）

- 新增 `GenotypeDao`、`PhenotypeDao`
- 扩展 `AnnovarDao`（按基因分组取 rsID）、`DosingGuidelineDao`（按基因模糊查询）、`SampleDao`（写回状态）
- Crawler 导入两份 CSV，数据进库

### 阶段三：Bug Fix（`b66727c` → `74e8ade`）

- CSV 引号解析问题修复
- 多列宽度不足（`activity_score`、`star_allele`、`diplotype` 等）
- `GROUP BY` 兼容 MySQL `ONLY_FULL_GROUP_BY` 模式

### 阶段四：Pipeline + 验证（`9737009` → `43241cf`）

- `MatchingController` 新 5 步 pipeline：
  `rsID → star allele → diplotype → phenotype → dosing guideline`
- 结果拆分 `metabolizerMatches` / `generalMatches`
- 步骤日志，后端验证通过
- JSP 视图按基因分组展示两类结果

### 阶段五：打分策略 V2（`da97601`）

- 新增 `ScoredAllele` bean
- `GenotypeDao.findScoredAlleles()`：对每个候选 star allele 统计患者 rsID 命中数
- 取得分最高的两个 allele 组成 diplotype，替代 V1 first-match
- 边界处理：score=0 → `*1/*1`，仅1个命中 → `*1/top1`

### 阶段六：多基因 + Bug Fix（`7b7b335` → `a53e9d7`）

- `AnnovarDao`：跳过空行、表头行、列数不足的行
- `AnnovarDao`：修复批处理 bug（最后一批未执行导致数据丢失）
- 多基因 pipeline 验证通过（CYP2C9 + CYP2C19 同时处理）

---

## 里程碑

| Tag                 | 内容                              |
| ------------------- | ------------------------------- |
| `v0.1-pgx-pipeline` | Pipeline 后端验证通过（V1 first-match） |
| `v0.2-scoring`      | V2 打分策略替换 first-match           |
| `v0.3-multiGene`    | 多基因 pipeline 跑通，批处理 bug 修复      |

---

## 已知问题 / TODO

| 优先级 | 问题                 | 说明                                      |
| --- | ------------------ | --------------------------------------- |
| 高   | 两条染色体未区分           | 当前混合两条染色体的 rsID 打分，需要用 VCF GT 字段区分杂合/纯合 |
| 中   | 多基因最严重 phenotype   | 同一药物涉及多基因时，应取最严重 phenotype 作为用药建议依据     |
| 中   | Sample info 页面     | 同学负责                                    |
| 低   | `Gene.refGene` 含分号 | 多基因注释（如 `GENE1;GENE2`）暂跳过，待拆分处理         |

---

## 部署说明

### 数据库初始化

```sql
-- 1. 执行 src/main/sql/schema.sql
-- 2. 运行 crawler/Main.java 导入两份 CSV
--    Step 4: VariantGenotypeCrawler  → variants2genotype（1095 行）
--    Step 5: GenotypePhenotypeCrawler → genotype2phenotype（27424 行）
```

### 构建

```powershell
$env:JAVA_HOME = "E:\JAVA\jdk-11.0.2"
mvn package -f pom.xml
```

### 测试数据

`scripts/test_annovar.txt`：3 行测试变异（CYP2C19 × 2 + CYP2C9 × 1），可直接上传验证 pipeline。
